/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Exact query-local selection over a zipped tuple relation. Direct views retain immutable row
 * offsets/weights, not copied payloads or hash links; generic views retain logical bag intervals.
 * Selecting a direct selection captures the ultimate payload offsets again. Generic interval
 * selections compose into ultimate-source coordinates rather than stacking decoding readers.
 *
 * <p>One producer owns the reusable descriptor. Each publication is a complete, exact relation,
 * possibly one bounded part of a larger group. Repeated parts need not have distinct parent keys.
 * Readers must finish before select/clear, upstream advancement, or close. Closing this view does
 * not close its upstream owner. All operations are thread-confined, not concurrent reclamation.
 */
public final class SelectedTupleSource extends BorrowedTupleBatch.Source {
	private final BorrowedTupleBatch batch;
	private final boolean direct;
	private final FactorSelection ranges;
	private int[] rowOffsets;
	private long[] prefixEnds;
	private long[] backing;
	private int members;
	private long count;
	private BorrowedTupleBatch original, base;
	private int originalLane, baseLane;
	private long originalGeneration, originalReference, originalCount, baseGeneration;

	/** Generic, range-based view; use the explicit direct overload for stable row captures. */
	public SelectedTupleSource(int columns, int initialCapacity) { this(columns, initialCapacity, false); }

	public SelectedTupleSource(int columns, int initialCapacity, boolean direct) {
		super(columns, true);
		if (initialCapacity < 0) throw new IllegalArgumentException("negative selection capacity");
		this.direct = direct;
		ranges = direct ? null : new FactorSelection(initialCapacity);
		if (direct) { rowOffsets = new int[initialCapacity]; prefixEnds = new long[initialCapacity]; }
		batch = new BorrowedTupleBatch(this, 1);
		batch.reset(1);
	}

	public boolean hasDirectRows() { return direct; }
	public BorrowedTupleBatch batch() { checkOpen(); return batch; }
	public long retainedSelectionBytes() {
		return direct ? (long) rowOffsets.length * (Integer.BYTES + Long.BYTES) : ranges.retainedBytes();
	}

	/** Invalidates descriptors, including copies; safe to call after upstream advancement. */
	public void clear() {
		checkNotClosed();
		original = base = null; backing = null; members = 0; count = 0L;
		if (ranges != null) ranges.clear();
		batch.reset(1);
	}

	/** Capture already-located rows; the caller may immediately reuse its primitive builder. */
	public BorrowedTupleBatch select(TupleSelection selection) {
		Objects.requireNonNull(selection, "selection");
		BorrowedTupleBatch input = selection.input();
		int lane = selection.lane();
		if (columns() != input.columns() || selection.hasDirectRows() != direct)
			throw new IllegalArgumentException("incompatible tuple selection representation");
		checkInput(input, lane);
		if (!direct) return select(input, lane, selection.ranges());
		clear();
		ensureCapacity(selection.members());
		if (selection.members() != 0) {
			System.arraycopy(selection.offsets(), 0, rowOffsets, 0, selection.members());
			System.arraycopy(selection.prefixEnds(), 0, prefixEnds, 0, selection.members());
		}
		backing = selection.backing(); members = selection.members(); count = selection.count();
		retainOriginal(input, lane);
		batch.bind(0, batch.generation(), count);
		return batch;
	}

	/** Logical ranges may cut through tuple weights; all columns remain correlated. */
	public BorrowedTupleBatch select(BorrowedTupleBatch input, int lane, FactorSelection selected) {
		if (direct) throw new IllegalStateException("direct tuple selections require captured row positions");
		Objects.requireNonNull(selected, "selection");
		checkInput(input, lane);
		selected.validateBounds(input.count(lane));
		clear();
		if (input.source() instanceof SelectedTupleSource view && !view.direct) {
			ranges.compose(view.ranges, selected);
			base = view.base; baseLane = view.baseLane; baseGeneration = view.baseGeneration;
		} else {
			ranges.copyFrom(selected);
			base = input; baseLane = lane; baseGeneration = input.generation();
		}
		count = ranges.count();
		retainOriginal(input, lane);
		batch.bind(0, batch.generation(), count);
		return batch;
	}

	private void checkInput(BorrowedTupleBatch input, int lane) {
		checkNotClosed();
		Objects.requireNonNull(input, "input").count(lane);
		if (input.columns() != columns()) throw new IllegalArgumentException("tuple arity changed");
		for (BorrowedTupleBatch.Source source = input.source(); source instanceof SelectedTupleSource view;
				source = view.original == null ? null : view.original.source())
			if (view == this) throw new IllegalArgumentException("cyclic tuple selection dependency");
	}

	private void retainOriginal(BorrowedTupleBatch input, int lane) {
		original = input; originalLane = lane; originalGeneration = input.generation();
		originalReference = input.reference(lane); originalCount = input.count(lane);
	}

	@Override protected void validateState() {
		if (original != null)
			original.checkSnapshot(originalLane, originalGeneration, originalReference, originalCount);
		if (base != null && base.generation() != baseGeneration)
			throw new IllegalStateException("selected tuple base was advanced");
	}

	@Override protected void validate(long reference, long exactCount) {
		// Callers have already checked dependency liveness. Recursing again here would multiply
		// validation work at every level of a chain of selected views.
		checkNotClosed();
		if (reference != batch.generation() || exactCount != count)
			throw new IllegalStateException("selected tuple descriptor was invalidated");
	}

	@Override public BorrowedTupleBatch.Reader openReader() {
		checkOpen();
		return direct ? new RowReader() : new RangeReader();
	}

	@Override protected void release() {
		original = base = null; backing = null; members = 0; count = 0L;
	}

	private void ensureCapacity(int required) {
		if (rowOffsets.length >= required) return;
		int capacity = (int) Math.min(Integer.MAX_VALUE, Math.max(required, Math.max(8L, 2L * rowOffsets.length)));
		rowOffsets = Arrays.copyOf(rowOffsets, capacity); prefixEnds = Arrays.copyOf(prefixEnds, capacity);
	}

	private abstract class ViewReader implements BorrowedTupleBatch.Reader {
		long generation;
		boolean bound, closed;
		final void start(long reference, long expected) {
			if (closed) throw new IllegalStateException("selected tuple reader closed");
			SelectedTupleSource.this.checkOpen();
			validate(reference, expected); generation = batch.generation(); bound = true;
		}
		final void check() {
			if (closed || !bound) throw new IllegalStateException("selected tuple reader closed or unbound");
			SelectedTupleSource.this.checkOpen();
			validate(generation, count);
		}
		final int maximum(long offset, int maximum, long[] rows, long[] weights) {
			check();
			if (offset < 0L || offset > count || maximum < 0) throw new IllegalArgumentException("invalid selected read");
			int limit = (int) Math.min(maximum, count - offset);
			Objects.checkFromIndexSize(0, Math.multiplyExact(limit, columns()), rows.length);
			Objects.checkFromIndexSize(0, limit, weights.length);
			return limit;
		}
		@Override public void close() { closed = true; bound = false; }
	}

	/** Direct navigation reads selection metadata only; it never revisits the hash duplicate chain. */
	private final class RowReader extends ViewReader implements BorrowedTupleBatch.DirectReader {
		int next, current = -1;
		long position, weight;
		@Override public void bind(long reference, long expected) {
			start(reference, expected); next = 0; current = -1; position = weight = 0L;
		}
		@Override public boolean nextTuple() {
			check();
			if (position == count) { current = -1; weight = 0L; return false; }
			current = next;
			weight = prefixEnds[next] - position;
			position = prefixEnds[next++];
			return true;
		}
		@Override public long[] rowArray() { check(); return backing; }
		@Override public int rowOffset() {
			if (current < 0) throw new IllegalStateException("no selected tuple");
			return rowOffsets[current];
		}
		@Override public long tupleWeight() { return weight; }

		@Override public int copyRows(long offset, int maximum, long[] rows, long[] weights) {
			int limit = maximum(offset, maximum, rows, weights);
			current = -1; weight = 0L;
			if (limit == 0) return 0;
			if (offset != position) next = memberAt(offset);
			long end = offset + limit;
			int written = 0, width = columns();
			while (offset < end) {
				long take = Math.min(end - offset, prefixEnds[next] - offset);
				System.arraycopy(backing, rowOffsets[next], rows, written * width, width);
				weights[written++] = take; offset += take;
				if (offset == prefixEnds[next]) next++;
			}
			position = offset;
			return written;
		}

		private int memberAt(long position) {
			int lo = 0, hi = members;
			while (lo < hi) {
				int mid = (lo + hi) >>> 1;
				if (prefixEnds[mid] <= position) lo = mid + 1; else hi = mid;
			}
			return lo;
		}
	}

	private final class RangeReader extends ViewReader {
		BorrowedTupleBatch.Source source;
		BorrowedTupleBatch.Reader reader;
		long[] decodedRows = new long[0], decodedWeights = new long[0];
		int range;
		@Override public void bind(long reference, long expected) {
			start(reference, expected);
			if (count == 0L) return;
			if (base == null) throw new IllegalStateException("selected tuple has no base");
			if (source != base.source()) {
				if (reader != null) reader.close();
				reader = null; source = null;
				reader = base.source().openReader(); source = base.source();
			}
			reader.bind(base.reference(baseLane), base.count(baseLane)); range = 0;
		}
		@Override public int copyRows(long offset, int maximum, long[] rows, long[] weights) {
			int limit = maximum(offset, maximum, rows, weights);
			if (limit == 0) return 0;
			int width = columns();
			if (decodedWeights.length < limit) {
				decodedWeights = new long[limit]; decodedRows = new long[Math.multiplyExact(limit, width)];
			}
			if (range == ranges.ranges() || offset < ranges.prefixStart(range) || offset >= ranges.prefixEnd(range))
				range = ranges.rangeAt(offset);
			long end = offset + limit;
			int written = 0;
			while (offset < end) {
				long physical = ranges.start(range) + offset - ranges.prefixStart(range);
				int request = (int) Math.min(end - offset, ranges.prefixEnd(range) - offset);
				int n = reader.copyRows(physical, request, decodedRows, decodedWeights);
				if (n <= 0 || n > request) throw new IllegalStateException("invalid selected tuple source progress");
				long consumed = 0;
				for (int i = 0; i < n; i++) {
					long w = decodedWeights[i];
					if (w <= 0L || w > request - consumed) throw new IllegalStateException("invalid selected tuple weight");
					consumed += w;
				}
				System.arraycopy(decodedRows, 0, rows, written * width, n * width);
				System.arraycopy(decodedWeights, 0, weights, written, n);
				written += n; offset += consumed;
				if (offset == ranges.prefixEnd(range)) range++;
			}
			return written;
		}
		@Override public void close() {
			if (closed) return;
			super.close();
			try { if (reader != null) reader.close(); }
			finally { reader = null; source = null; }
		}
	}
}
