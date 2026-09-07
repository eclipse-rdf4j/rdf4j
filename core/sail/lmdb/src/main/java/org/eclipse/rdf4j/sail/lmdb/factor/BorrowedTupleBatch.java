/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Exact borrowed tuple relations. Columns of one tuple are zipped, never independent factors.
 * Descriptors contain stable source references and logical bag counts, not scalar IDs. Physical
 * navigation belongs to the source; a reference does not imply contiguous or native storage.
 *
 * <p>The producer owns the source and descriptor batch. Readers are independent and bounded by
 * their window, and must finish before the producer advances or closes. A source keeps both its
 * storage and interpretation immutable until closed. This is the tuple counterpart of
 * {@link BorrowedFactorBatch}, not a replacement for its specialized unary loops.
 */
public final class BorrowedTupleBatch {
	private final Source source;
	private long[] references;
	private long[] counts;
	private int size;
	private long generation;

	public BorrowedTupleBatch(Source source, int capacity) {
		this.source = Objects.requireNonNull(source, "source");
		if (capacity < 0) throw new IllegalArgumentException("negative capacity");
		references = new long[capacity];
		counts = new long[capacity];
	}

	public Source source() { return source; }
	public int columns() { return source.columns(); }
	public int size() { return size; }
	public long generation() { return generation; }
	public long reference(int lane) { checkLane(lane); return references[lane]; }
	public long count(int lane) {
		checkLane(lane);
		if (source.dependentViews && (references[lane] != 0L || counts[lane] != 0L))
			source.validate(references[lane], counts[lane]);
		return counts[lane];
	}

	public void reset(int lanes) {
		source.checkOpen();
		if (lanes < 0) throw new IllegalArgumentException("negative lane count");
		if (lanes > counts.length) {
			int capacity = (int) Math.min(Integer.MAX_VALUE,
					Math.max((long) lanes, Math.max(16L, counts.length * 2L)));
			references = Arrays.copyOf(references, capacity);
			counts = Arrays.copyOf(counts, capacity);
		}
		Arrays.fill(references, 0, Math.max(size, lanes), 0L);
		Arrays.fill(counts, 0, Math.max(size, lanes), 0L);
		size = lanes;
		generation++;
	}

	/** Publishes one exact immutable group; reference interpretation is source-specific. */
	public void bind(int lane, long reference, long count) {
		checkLane(lane);
		if (count < 0L) throw new IllegalArgumentException("negative tuple count");
		if (count != 0L || source.dependentViews) source.validate(reference, count);
		references[lane] = reference;
		counts[lane] = count;
	}

	/** Copies a descriptor, not payload or reader state. Sources must be identical. */
	public void copyLaneFrom(int lane, BorrowedTupleBatch from, int fromLane) {
		checkLane(lane);
		from.count(fromLane); // includes provenance of copied selected descriptors
		if (source != from.source) throw new IllegalArgumentException("different tuple source");
		references[lane] = from.references[fromLane];
		counts[lane] = from.counts[fromLane];
	}

	public Cursor cursor(int window) { return new Cursor(this, window); }

	/** Validate a retained descriptor in one dependency traversal, not through several getters. */
	void checkSnapshot(int lane, long expectedGeneration, long reference, long count) {
		if (generation != expectedGeneration)
			throw new IllegalStateException("tuple descriptor generation changed");
		checkLane(lane);
		if (references[lane] != reference || counts[lane] != count)
			throw new IllegalStateException("tuple descriptor was rebound");
		if (source.dependentViews && (reference != 0L || count != 0L)) source.validate(reference, count);
	}

	private void checkLane(int lane) {
		source.checkOpen();
		Objects.checkIndex(lane, size);
	}

	/** One owner per immutable relation store, not an ownership operation per tuple or descriptor. */
	public abstract static class Source implements AutoCloseable {
		private final int columns;
		private final boolean dependentViews;
		private boolean closed;
		protected Source(int columns) { this(columns, false); }
		/** A selected source validates its borrowed dependencies outside payload access. */
		protected Source(int columns, boolean dependentViews) {
			this.dependentViews = dependentViews;
			if (columns < 1 || columns > Long.SIZE) throw new IllegalArgumentException("invalid tuple arity");
			this.columns = columns;
		}
		public final int columns() { return columns; }
		public final void checkOpen() {
			checkNotClosed();
			if (dependentViews) validateState();
		}
		protected final void checkNotClosed() {
			if (closed) throw new IllegalStateException("borrowed tuple source is closed");
		}
		/** Query-specific views override this; physical sources need no extra dependent check. */
		protected void validateState() { }
		/** Validates a descriptor at publication/bind, not once per value. */
		protected abstract void validate(long reference, long count);
		public abstract Reader openReader();
		protected void release() { }
		@Override public final void close() {
			if (!closed) { closed = true; release(); }
		}
	}

	/**
	 * Source-private mutable traversal. copyRows writes row-major correlated columns and positive
	 * weights. Their sum is at most maximum (logical rows); the returned physical row count must
	 * be positive before the exact end. Skips and backward seeks replay the same relation.
	 */
	public interface Reader extends AutoCloseable {
		void bind(long reference, long count);
		int copyRows(long logicalOffset, int maximum, long[] rows, long[] weights);
		@Override default void close() { }
	}

	/**
	 * Optional stable heap-row capability. Navigation is separate from value access: nextTuple()
	 * selects one physical tuple. rowArray() is valid immediately after bind and must return the
	 * same immutable backing array for that entire binding; rowOffset() selects its correlated
	 * columns until the next reader operation. Reusable decoding buffers cannot claim this capability. No decode window or payload
	 * copy is necessary for an already packed hash/merge group. Other sources retain copyRows().
	 */
	public interface DirectReader extends Reader {
		boolean nextTuple();
		long[] rowArray();
		int rowOffset();
		long tupleWeight();
	}

	public static final class Cursor implements AutoCloseable {
		private final BorrowedTupleBatch batch;
		private final Source source;
		private final Reader reader;
		private final DirectReader direct;
		private final int columns;
		private final long[] rows;
		private final long[] weights;
		private long[] currentRows;
		private int currentOffset;
		private long currentWeight;
		private long generation, count, offset, currentPosition;
		private int lane;
		private int index, size;
		private boolean bound, closed;

		private Cursor(BorrowedTupleBatch batch, int window) {
			if (window <= 0) throw new IllegalArgumentException("nonpositive window");
			this.batch = batch;
			source = batch.source;
			source.checkOpen();
			columns = source.columns();
			int cells = Math.multiplyExact(window, columns);
			reader = Objects.requireNonNull(source.openReader());
			direct = reader instanceof DirectReader available ? available : null;
			rows = direct == null ? new long[cells] : null;
			weights = direct == null ? new long[window] : null;
		}

		public void bind(int lane) {
			checkOpen();
			batch.checkLane(lane);
			count = batch.count(lane);
			this.lane = lane;
			currentPosition = 0L;
			generation = batch.generation;
			offset = 0L;
			index = -1;
			size = 0;
			currentRows = null;
			currentWeight = 0L;
			bound = false;
			if (count != 0L) {
				source.validate(batch.references[lane], count);
				reader.bind(batch.references[lane], count);
				currentRows = direct == null ? rows : Objects.requireNonNull(direct.rowArray(), "direct tuple array");
			}
			bound = true;
		}

		public boolean next() {
			checkBound();
			if (direct != null) return nextDirect();
			currentPosition += currentWeight;
			if (++index >= size && !refill()) {
				currentWeight = 0L; return false;
			}
			currentOffset = index * columns;
			currentWeight = weights[index];
			return true;
		}

		private boolean nextDirect() {
			if (offset == count) { currentWeight = 0L; return false; }
			if (!direct.nextTuple()) throw new IllegalStateException("truncated direct tuple group");
			long weight = direct.tupleWeight();
			if (weight <= 0L || weight > count - offset)
				throw new IllegalStateException("invalid direct tuple weight");
			int start = direct.rowOffset();
			Objects.checkFromIndexSize(start, columns, currentRows.length);
			currentOffset = start;
			currentWeight = weight;
			offset += weight;
			return true;
		}

		private boolean refill() {
			if (offset == count) { size = 0; return false; }
			int maximum = (int) Math.min(weights.length, count - offset);
			int n = reader.copyRows(offset, maximum, rows, weights);
			if (n <= 0 || n > maximum) throw new IllegalStateException("invalid tuple window size");
			long consumed = 0L;
			for (int i = 0; i < n; i++) {
				long weight = weights[i];
				if (weight <= 0 || weight > maximum - consumed)
					throw new IllegalStateException("invalid tuple window weight");
				consumed += weight;
			}
			offset += consumed;
			size = n;
			index = 0;
			return true;
		}

		/** True only for immutable source-owned rows, never a reusable decoding window. */
		public boolean hasDirectRows() { return direct != null; }

		/** Stable for this binding; callers must retain the source and descriptor provenance. */
		public long[] directRowArray() {
			checkBound();
			if (direct == null || count == 0L) throw new IllegalStateException("no direct tuple backing");
			return currentRows;
		}

		/** Offset already resolved by the source reader. No repeated chain walk or payload copy. */
		public int directRowOffset() {
			if (direct == null) throw new IllegalStateException("not a direct tuple reader");
			weight();
			return currentOffset;
		}

		/** Logical bag coordinate at the start of the current weighted tuple fragment. */
		public long position() { weight(); return direct == null ? currentPosition : offset - currentWeight; }

		/** One provenance check when capturing all current coordinates, not one check per accessor. */
		void capture(TupleSelection selection) {
			checkBound();
			if (currentWeight == 0L) throw new IllegalStateException("no current tuple");
			selection.addCurrent(batch, lane, generation, direct == null ? currentPosition : offset - currentWeight,
					currentWeight, currentRows, currentOffset);
		}

		BorrowedTupleBatch boundBatch() { checkBound(); return batch; }
		int boundLane() { checkBound(); return lane; }
		long boundGeneration() { checkBound(); return generation; }

		public long value(int column) {
			Objects.checkIndex(column, columns);
			if (currentWeight == 0L || closed) throw new IllegalStateException("no current tuple");
			return currentRows[currentOffset + column];
		}
		public long weight() {
			if (currentWeight == 0L || closed) throw new IllegalStateException("no current tuple");
			return currentWeight;
		}
		private void checkOpen() {
			if (closed) throw new IllegalStateException("tuple cursor is closed");
			source.checkOpen();
		}
		private void checkBound() {
			checkOpen();
			if (!bound) throw new IllegalStateException("tuple cursor is not bound");
			if (generation != batch.generation)
				throw new IllegalStateException("tuple producer advanced during consumption");
		}
		@Override public void close() {
			if (!closed) { closed = true; currentRows = null; reader.close(); }
		}
	}
}
