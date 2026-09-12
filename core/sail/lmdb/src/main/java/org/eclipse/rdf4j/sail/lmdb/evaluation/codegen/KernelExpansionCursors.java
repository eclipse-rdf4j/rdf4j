/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.rdf4j.sail.lmdb.evaluation.NativeLmdbQuerySource;
import org.eclipse.rdf4j.sail.lmdb.evaluation.NativeLmdbQuerySource.NativeAdjacency;
import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedFactorBatch;

/**
 * Resumable primitive algorithms shared by interpreted and generated kernels. These cursors know neither IR nodes nor
 * scalar-register layouts. Each instance is owned by one operator activation; the enclosing context retains the
 * snapshot. Close releases activation state but never closes an adjacency owned by that context.
 */
public final class KernelExpansionCursors {
	private KernelExpansionCursors() {
	}

	public abstract static class Values implements AutoCloseable {
		protected final KernelCancellation cancellation;
		protected long value;
		protected int tick;
		protected boolean done;

		protected Values(KernelCancellation cancellation) {
			this.cancellation = cancellation;
		}

		public final long value() {
			return value;
		}

		protected final void poll() {
			if ((++tick & 1023) == 0)
				KernelRuntime.checkCancelled(cancellation);
		}

		public abstract boolean next();

		@Override
		public void close() {
			done = true;
		}
	}

	/** A partition cursor remains positioned while its downstream continuation is suspended. */
	public static final class Domains extends Values {
		private NativeLmdbQuerySource.NodeDomainIntersection view;
		private NativeLmdbQuerySource.NodeDomainIntersection.Cursor cursor;
		private int partition;

		public Domains(NativeLmdbQuerySource.NodeDomainIntersection view, KernelCancellation cancellation) {
			super(cancellation);
			this.view = Objects.requireNonNull(view);
		}

		@Override
		public boolean next() {
			if (done)
				return false;
			KernelRuntime.checkCancelled(cancellation);
			while (true) {
				if (cursor != null && cursor.next()) {
					value = cursor.nodeId();
					return true;
				}
				if (partition == view.partitionCount()) {
					close();
					return false;
				}
				poll();
				cursor = view.cursor(partition++);
			}
		}

		@Override
		public void close() {
			super.close();
			cursor = null;
			view = null;
		}
	}

	/** Subject/object DISTINCT with bounded quad staging and demand-driven source opening. */
	public static final class Terms extends Values {
		private KernelScanner scanner;
		private final int scan;
		private KernelQuadCursor cursor;
		private KernelRuntime.LongHashSet seen;
		private long[] buffer;
		private int position, count;

		public Terms(KernelScanner scanner, int scan, KernelCancellation cancellation) {
			super(cancellation);
			this.scanner = Objects.requireNonNull(scanner);
			this.scan = scan;
		}

		@Override
		public boolean next() {
			if (done)
				return false;
			try {
				if (cursor == null) {
					KernelRuntime.checkCancelled(cancellation);
					cursor = scanner.open(scan, -1L, -1L, -1L, -1L);
					seen = new KernelRuntime.LongHashSet();
					buffer = new long[KernelRuntime.SCAN_BATCH_ROWS * 4];
				}
				while (true) {
					if (position == count * 2) {
						KernelRuntime.checkCancelled(cancellation);
						count = cursor.fill(buffer, KernelRuntime.SCAN_BATCH_ROWS);
						KernelRuntime.checkCancelled(cancellation);
						if (count < 0 || count > KernelRuntime.SCAN_BATCH_ROWS)
							throw new IllegalStateException("quad cursor exceeded window bounds");
						position = 0;
						if (count == 0) {
							close();
							return false;
						}
					}
					poll();
					int at = position++;
					long id = buffer[(at >>> 1) * 4 + ((at & 1) == 0 ? 0 : 2)];
					if (id != -1L && seen.add(id)) {
						value = id;
						return true;
					}
				}
			} catch (RuntimeException | Error failure) {
				KernelRuntime.closeCursor(this, failure);
				throw failure;
			}
		}

		@Override
		public void close() {
			super.close();
			KernelQuadCursor owned = cursor;
			cursor = null;
			buffer = null;
			seen = null;
			scanner = null;
			if (owned != null)
				KernelRuntime.closeScanCursor(owned, null);
		}
	}

	/**
	 * Lazy p* / p+ reachability. A node is queued at most once, so duplicate edges do not make the pending stack
	 * proportional to edge count. The start of p* is returned before any adjacency lookup. This is reachability, not
	 * path-counting; graph-context restrictions apply to every traversed edge.
	 */
	public static final class Path extends Values {
		private NativeAdjacency adjacency;
		private KernelRuntime.LongHashSet emitted = new KernelRuntime.LongHashSet();
		private KernelRuntime.LongHashSet queued = new KernelRuntime.LongHashSet();
		private long[] stack = new long[16];
		private long[] contexts;
		private int top;
		private long handle, position, end;
		private boolean initial;

		public Path(NativeAdjacency adjacency, long start, int minHops, long[] contexts,
				KernelCancellation cancellation) {
			super(cancellation);
			if (minHops != 0 && minHops != 1)
				throw new IllegalArgumentException("only p* / p+ is supported");
			this.adjacency = Objects.requireNonNull(adjacency);
			this.contexts = Objects.requireNonNull(contexts).clone();
			if (start == -1L) {
				done = true;
				return;
			}
			stack[top++] = start;
			queued.add(start);
			if (minHops == 0) {
				initial = true;
				value = start;
				emitted.add(start);
			}
		}

		@Override
		public boolean next() {
			if (done)
				return false;
			KernelRuntime.checkCancelled(cancellation);
			if (initial) {
				initial = false;
				return true;
			}
			while (true) {
				while (position < end) {
					poll();
					long at = position++;
					if (contexts.length != 0) {
						long context = adjacency.contextAt(handle, at);
						boolean accepted = false;
						for (long allowed : contexts)
							if (allowed == context) {
								accepted = true;
								break;
							}
						if (!accepted)
							continue;
					}
					long neighbor = adjacency.neighborAt(handle, at);
					if (queued.add(neighbor)) {
						if (top == stack.length)
							stack = Arrays.copyOf(stack, Math.multiplyExact(top, 2));
						stack[top++] = neighbor;
					}
					if (emitted.add(neighbor)) {
						value = neighbor;
						return true;
					}
				}
				if (top == 0) {
					close();
					return false;
				}
				poll();
				long node = stack[--top];
				handle = adjacency.find(node);
				position = 0;
				end = handle > 0L ? adjacency.size(handle) : 0L;
			}
		}

		@Override
		public void close() {
			super.close();
			adjacency = null;
			emitted = null;
			queued = null;
			stack = contexts = null;
		}
	}

	/**
	 * Sorted unsigned multiset intersection. A cached frontier retains the value already read at each source position.
	 * In particular, the first value after a duplicate group is not read again by the next group. Equal runs remain
	 * factorized as duplicate ranges; row replay never has to multiply those ranges.
	 */
	public static final class Intersection extends Values {
		private NativeAdjacency[] views;
		private long[] handles, positions, sizes, heads, duplicates, digits, keys;
		private boolean group, initialized, borrowedUnavailable;
		private BorrowedPair borrowed;

		public Intersection(NativeAdjacency[] views, KernelCancellation cancellation) {
			super(cancellation);
			if (views.length < 1)
				throw new IllegalArgumentException("intersection arity");
			this.views = views.clone();
			int n = views.length;
			handles = new long[n];
			positions = new long[n];
			sizes = new long[n];
			heads = new long[n];
			duplicates = new long[n];
			digits = new long[n];
			keys = new long[n];
			for (NativeAdjacency view : this.views)
				Objects.requireNonNull(view);
			done = true;
		}

		public Intersection(NativeAdjacency[] views, long[] keys, KernelCancellation cancellation) {
			this(views, cancellation);
			bind(keys);
		}

		/** Set one input key, then call bind(). Generated fixed-arity code needs no temporary key array. */
		public void key(int index, long key) {
			if (views == null)
				throw new IllegalStateException("closed intersection");
			keys[index] = key;
		}

		public void bind(long[] sourceKeys) {
			if (views == null)
				throw new IllegalStateException("closed intersection");
			if (sourceKeys.length != keys.length)
				throw new IllegalArgumentException("intersection arity");
			System.arraycopy(sourceKeys, 0, keys, 0, keys.length);
			bind();
		}

		/** Rebind one non-reentrant activation, retaining primitive scratch but no previous frontier state. */
		public void bind() {
			if (views == null)
				throw new IllegalStateException("closed intersection");
			done = true;
			group = false;
			initialized = false;
			tick = 0;
			KernelRuntime.checkCancelled(cancellation);
			for (int i = 0; i < views.length; i++) {
				positions[i] = 0;
				if (keys[i] == -1L)
					return;
				handles[i] = views[i].find(keys[i]);
				if (handles[i] <= 0L)
					return;
				sizes[i] = views[i].size(handles[i]);
				if (sizes[i] == 0L)
					return;
				if (sizes[i] < 0L)
					throw new IllegalStateException("negative adjacency run size");
			}
			done = false;
		}

		@Override
		public boolean next() {
			if (done)
				return false;
			poll();
			if (group) {
				for (int i = digits.length - 1; i >= 0; i--) {
					if (digits[i] < duplicates[i] - 1) {
						digits[i]++;
						return true;
					}
					digits[i] = 0;
				}
				group = false;
			}
			return nextGroup();
		}

		/** Advances past the previous duplicate group without enumerating its remaining combinations. */
		public boolean nextGroup() {
			if (done)
				return false;
			KernelRuntime.checkCancelled(cancellation);
			return advanceGroup();
		}

		private boolean advanceGroup() {
			if (done)
				return false;
			poll();
			group = false;
			if (!initialized) {
				for (int i = 0; i < views.length; i++)
					heads[i] = views[i].neighborAt(handles[i], 0L);
				initialized = true;
			}
			return views.length == 2 ? nextPair() : nextMany();
		}

		/** Fixed two-input merge: advance only the smaller frontier, without scanning an arity loop. */
		private boolean nextPair() {
			while (positions[0] < sizes[0] && positions[1] < sizes[1]) {
				poll();
				long left = heads[0], right = heads[1];
				if (left == right) {
					value = left;
					consumeGroup(0, left);
					consumeGroup(1, left);
					group = true;
					return true;
				}
				if (Long.compareUnsigned(left, right) < 0)
					advance(0, right, false);
				else
					advance(1, left, false);
			}
			done = true;
			return false;
		}

		private boolean nextMany() {
			while (true) {
				poll();
				if (positions[0] >= sizes[0]) {
					done = true;
					return false;
				}
				long maximum = heads[0];
				boolean equal = true;
				for (int i = 1; i < views.length; i++) {
					if (positions[i] >= sizes[i]) {
						done = true;
						return false;
					}
					long current = heads[i];
					if (current != maximum) {
						equal = false;
						if (Long.compareUnsigned(current, maximum) > 0)
							maximum = current;
					}
				}
				if (equal) {
					value = maximum;
					for (int i = 0; i < views.length; i++)
						consumeGroup(i, maximum);
					group = true;
					return true;
				}
				for (int i = 0; i < views.length; i++) {
					if (Long.compareUnsigned(heads[i], maximum) < 0)
						advance(i, maximum, false);
				}
			}
		}

		private void consumeGroup(int index, long matched) {
			long start = positions[index];
			advance(index, matched, true);
			duplicates[index] = positions[index] - start;
			digits[index] = 0;
		}

		public long groupMultiplicity() {
			if (!group || done)
				throw new IllegalStateException("no positioned group");
			if (duplicates.length == 2)
				return Math.multiplyExact(duplicates[0], duplicates[1]);
			long count = 1;
			for (long duplicatesInRun : duplicates)
				count = Math.multiplyExact(count, duplicatesInRun);
			return count;
		}

		/**
		 * Exact sum of subsequent groups. Immediately after bind this is the complete intersection cardinality. Like
		 * nextGroup(), it skips an already-positioned group (including any unreplayed duplicates in it). Suitable only
		 * when no per-value continuation, grouping key or expression needs the omitted members.
		 */
		public long countRemainingGroups() {
			if (done)
				return 0L;
			KernelRuntime.checkCancelled(cancellation);
			if (views.length == 1) {
				long remaining = sizes[0] - positions[0];
				done = true;
				group = false;
				return remaining;
			}
			if (canStreamPair() && bindBorrowedPair()) {
				done = true;
				group = false;
				return borrowed.count();
			}
			long count = 0L;
			while (advanceGroup())
				count = Math.addExact(count, groupMultiplicity());
			return count;
		}

		/** Sequential decode is local to balanced, sufficiently long count-only pairs; skew retains indexed seeks. */
		private boolean canStreamPair() {
			if (views.length != 2 || initialized || borrowedUnavailable)
				return false;
			long shorter = Math.min(sizes[0], sizes[1]);
			return shorter >= 256L && 1L + (Math.max(sizes[0], sizes[1]) - 1L) / shorter <= 8L;
		}

		private boolean bindBorrowedPair() {
			if (borrowed == null) {
				BorrowedFactorBatch.Source left = views[0].openFactorSource();
				if (left == null) {
					borrowedUnavailable = true;
					return false;
				}
				BorrowedFactorBatch.Source right;
				try {
					right = views[1].openFactorSource();
				} catch (RuntimeException | Error failure) {
					KernelRuntime.closeCursor(left, failure);
					throw failure;
				}
				if (right == null) {
					borrowedUnavailable = true;
					left.close();
					return false;
				}
				try {
					borrowed = new BorrowedPair(left, right, cancellation);
				} catch (RuntimeException | Error failure) {
					KernelRuntime.closeCursor(left, failure);
					KernelRuntime.closeCursor(right, failure);
					throw failure;
				}
			}
			return borrowed.bind(views, handles, sizes);
		}

		/** Exact count over already stored weighted fibers; addresses stay behind their owning source readers. */
		private static final class BorrowedPair implements AutoCloseable {
			private final BorrowedFactorBatch.Source leftSource, rightSource;
			private final BorrowedFactorBatch leftBatch, rightBatch;
			private final KernelCancellation cancellation;
			private FiberStream left, right;

			BorrowedPair(BorrowedFactorBatch.Source left, BorrowedFactorBatch.Source right,
					KernelCancellation cancellation) {
				this.leftSource = left;
				this.rightSource = right;
				this.cancellation = cancellation;
				leftBatch = new BorrowedFactorBatch(left, 1);
				rightBatch = new BorrowedFactorBatch(right, 1);
			}

			boolean bind(NativeAdjacency[] views, long[] handles, long[] sizes) {
				leftBatch.reset(1);
				rightBatch.reset(1);
				if (!views[0].borrowRun(handles[0], leftBatch, 0)
						|| !views[1].borrowRun(handles[1], rightBatch, 0))
					return false;
				if (leftBatch.count(0) != sizes[0] || rightBatch.count(0) != sizes[1])
					throw new IllegalStateException("borrowed intersection changed logical cardinality");
				// Raw/heap long duplicate runs are cheaper to skip by indexed galloping than to scan into windows.
				// Restrict this path to encoded fiber readers; do not impose a full scan on an existing raw view.
				if (!encoded(leftBatch.kind(0)) || !encoded(rightBatch.kind(0)))
					return false;
				if (left == null)
					left = new FiberStream(leftBatch, cancellation);
				if (right == null)
					right = new FiberStream(rightBatch, cancellation);
				left.bind();
				right.bind();
				return true;
			}

			private static boolean encoded(byte kind) {
				return kind == BorrowedFactorBatch.CSF_PAGE || kind == BorrowedFactorBatch.ENCODED_RUN;
			}

			long count() {
				boolean hasLeft = left.nextGroup(), hasRight = right.nextGroup();
				long count = 0L;
				while (hasLeft && hasRight) {
					int comparison = Long.compareUnsigned(left.value, right.value);
					if (comparison == 0) {
						count = Math.addExact(count, Math.multiplyExact(left.weight, right.weight));
						hasLeft = left.nextGroup();
						if (hasLeft)
							hasRight = right.nextGroup();
					} else if (comparison < 0)
						hasLeft = left.nextGroup();
					else
						hasRight = right.nextGroup();
				}
				return count;
			}

			@Override
			public void close() {
				Throwable failure = KernelRuntime.closeResource(left, null);
				failure = KernelRuntime.closeResource(right, failure);
				failure = KernelRuntime.closeResource(leftSource, failure);
				failure = KernelRuntime.closeResource(rightSource, failure);
				KernelRuntime.rethrowCloseFailure(failure);
			}
		}

		/** Coalesces fragments split by decode windows or page seams without flattening their weights. */
		private static final class FiberStream implements AutoCloseable {
			private final BorrowedFactorBatch.Cursor cursor;
			private final KernelCancellation cancellation;
			private final long[] values, weights;
			private int index, end;
			private long value, weight;

			FiberStream(BorrowedFactorBatch batch, KernelCancellation cancellation) {
				cursor = batch.cursor(256);
				this.cancellation = cancellation;
				values = cursor.windowValues();
				weights = cursor.windowWeights();
			}

			void bind() {
				cursor.bind(0);
				index = end = 0;
			}

			private boolean available() {
				if (index < end)
					return true;
				KernelRuntime.checkCancelled(cancellation);
				int size = cursor.nextWindow();
				KernelRuntime.checkCancelled(cancellation);
				index = cursor.windowStart();
				end = index + size;
				return size != 0;
			}

			boolean nextGroup() {
				if (!available())
					return false;
				value = values[index];
				weight = weights[index++];
				while (available() && values[index] == value)
					weight = Math.addExact(weight, weights[index++]);
				return true;
			}

			@Override
			public void close() {
				cursor.close();
			}
		}

		/**
		 * Current head is already known to precede the requested bound. Read the following entry once, retaining the
		 * bounding value even when galloping is needed. Unique/aligned and adjacent merge steps take this first branch;
		 * long duplicate runs and skewed inputs use the bounded exponential/binary-search continuation.
		 */
		private void advance(int index, long target, boolean upper) {
			long end = sizes[index];
			long next = positions[index] + 1L; // positioned index is strictly below size, including Long.MAX_VALUE
			if (next == end) {
				positions[index] = end;
				return;
			}
			long current = views[index].neighborAt(handles[index], next);
			if (!before(current, target, upper)) {
				positions[index] = next;
				heads[index] = current;
				return;
			}
			seekBeyond(index, next, target, upper);
		}

		// The entry at from was already read and precedes the bound. high is either the run end or a retained
		// not-before candidate. Arithmetic is capped by the remaining run length; neither doubling nor addition wraps.
		private void seekBeyond(int index, long from, long target, boolean upper) {
			NativeAdjacency view = views[index];
			long handle = handles[index], end = sizes[index];
			long low = from + 1L, high = low, step = 1L, highValue = 0L;
			while (high < end) {
				poll();
				long candidate = view.neighborAt(handle, high);
				if (!before(candidate, target, upper)) {
					highValue = candidate;
					break;
				}
				low = high + 1L;
				long remaining = end - high;
				step = step >= remaining - step ? remaining : step + step;
				high += step;
			}
			while (low < high) {
				poll();
				long mid = low + ((high - low) >>> 1);
				long candidate = view.neighborAt(handle, mid);
				if (before(candidate, target, upper))
					low = mid + 1L;
				else {
					high = mid;
					highValue = candidate;
				}
			}
			positions[index] = low;
			if (low < end)
				heads[index] = highValue;
		}

		private static boolean before(long value, long target, boolean upper) {
			int comparison = Long.compareUnsigned(value, target);
			return upper ? comparison <= 0 : comparison < 0;
		}

		@Override
		public void close() {
			super.close();
			group = initialized = false;
			views = null;
			handles = positions = sizes = heads = duplicates = digits = keys = null;
			BorrowedPair owned = borrowed;
			borrowed = null;
			if (owned != null)
				owned.close();
		}
	}
}
