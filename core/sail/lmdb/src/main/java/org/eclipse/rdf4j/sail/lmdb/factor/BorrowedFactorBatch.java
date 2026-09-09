/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials are made
 * available under the Eclipse Distribution License v1.0 (BSD-3-Clause).
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Predicate-major descriptors for exact, immutable, context-unrestricted neighbor relations.
 * One lane belongs to one parent binding; counts are logical quad multiplicities, NOT bytes or
 * distinct-neighbor counts. Values remain in the source. No descriptor owns a mutable cursor.
 *
 * <p>The source owns one lifetime lease for all its batches. Close readers before closing the
 * source. A batch is producer-confined and must not be reset while a reader is consuming it.
 * Separate readers have independent decode state. Scalar IDs never contain descriptor handles.
 */
public final class BorrowedFactorBatch {
	public static final byte EMPTY = 0;
	public static final byte HEAP = 1;
	public static final byte RAW_U64 = 2;
	public static final byte CSF_PAGE = 3;
	public static final byte ENCODED_RUN = 4;
	/** Query-local position selection; its address is intentionally zero, not a raw value span. */
	public static final byte SELECTED = 5;

	private final Source source;
	private byte[] kinds;
	private long[] addresses;
	private long[] references;
	private long[] counts;
	private int[] coordinates;
	private long[][] arrays;
	private int size;
	private long generation;

	public BorrowedFactorBatch(Source source, int capacity) {
		this.source = Objects.requireNonNull(source, "source");
		if (capacity < 0) throw new IllegalArgumentException("negative capacity");
		kinds = new byte[capacity];
		addresses = new long[capacity];
		references = new long[capacity];
		counts = new long[capacity];
		coordinates = new int[capacity];
	}

	public Source source() { return source; }
	public int size() { return size; }
	public int capacity() { return counts.length; }
	public long generation() { return generation; }
	public byte kind(int lane) { checkLane(lane); return kinds[lane]; }
	/** Actual page/run/payload address. Only RAW_U64 permits address + 8 * position loads. */
	public long address(int lane) { checkLane(lane); return addresses[lane]; }
	/** Cold relocation/codec reference; never a scalar value binding. */
	public long reference(int lane) { checkLane(lane); return references[lane]; }
	public int coordinate(int lane) { checkLane(lane); return coordinates[lane]; }
	public long[] array(int lane) { checkLane(lane); return arrays == null ? null : arrays[lane]; }
	public long count(int lane) { checkLane(lane); return counts[lane]; }
	/** Read-only by contract; shared with topology-specialized cardinality kernels. */
	public long[] exactCounts() { source.checkOpen(); return counts; }

	public void reset(int lanes) {
		source.checkOpen();
		if (lanes < 0) throw new IllegalArgumentException("negative lane count");
		ensureCapacity(lanes);
		Arrays.fill(kinds, 0, Math.max(size, lanes), EMPTY);
		Arrays.fill(counts, 0, Math.max(size, lanes), 0L);
		if (source instanceof SelectedFactorSource) Arrays.fill(references, 0, Math.max(size, lanes), 0L);
		if (arrays != null) Arrays.fill(arrays, 0, size, null);
		size = lanes;
		generation++;
	}

	private void ensureCapacity(int required) {
		if (required <= counts.length) return;
		int capacity = (int) Math.min(Integer.MAX_VALUE,
				Math.max(required, Math.max(16L, (long) counts.length * 2L)));
		kinds = Arrays.copyOf(kinds, capacity);
		addresses = Arrays.copyOf(addresses, capacity);
		references = Arrays.copyOf(references, capacity);
		counts = Arrays.copyOf(counts, capacity);
		coordinates = Arrays.copyOf(coordinates, capacity);
		if (arrays != null) arrays = Arrays.copyOf(arrays, capacity);
	}

	public void bindNative(int lane, byte kind, long address, long reference, int coordinate, long count) {
		checkTargetLane(lane);
		if (kind != RAW_U64 && kind != CSF_PAGE && kind != ENCODED_RUN)
			throw new IllegalArgumentException("not a native relation kind");
		if (count <= 0L || address == 0L || coordinate < 0)
			throw new IllegalArgumentException("invalid borrowed native relation");
		if (kind == RAW_U64) {
			long bytes = Math.multiplyExact(count, Long.BYTES);
			if (Long.compareUnsigned(address + bytes, address) < 0)
				throw new IllegalArgumentException("native span wraps the address space");
		}
		kinds[lane] = kind;
		addresses[lane] = address;
		references[lane] = reference;
		coordinates[lane] = coordinate;
		counts[lane] = count;
		if (arrays != null) arrays[lane] = null;
	}

	public void bindHeap(int lane, long[] values, int offset, int count) {
		checkTargetLane(lane);
		Objects.checkFromIndexSize(offset, count, Objects.requireNonNull(values).length);
		if (arrays == null) arrays = new long[counts.length][];
		arrays[lane] = values;
		coordinates[lane] = offset;
		counts[lane] = count;
		addresses[lane] = 0L;
		references[lane] = 0L;
		kinds[lane] = count == 0 ? EMPTY : HEAP;
	}

	/** Package-private: only the selected source can publish this descriptor kind. */
	void bindSelected(int lane, long count) {
		source.checkOpen();
		Objects.checkIndex(lane, size);
		if (!(source instanceof SelectedFactorSource) || count < 0L)
			throw new IllegalArgumentException("invalid selected factor source or count");
		kinds[lane] = count == 0L ? EMPTY : SELECTED;
		counts[lane] = count;
		addresses[lane] = 0L;
		references[lane] = generation; // selected-source version survives descriptor copies
		coordinates[lane] = 0;
		if (arrays != null) arrays[lane] = null;
	}

	/** Copies positioning information only, never the relation's values. */
	public void copyLaneFrom(int lane, BorrowedFactorBatch from, int fromLane) {
		checkTargetLane(lane);
		from.checkLane(fromLane);
		if (!source.sameSnapshot(from.source)) throw new IllegalArgumentException("mixed factor snapshots");
		kinds[lane] = from.kinds[fromLane];
		addresses[lane] = from.addresses[fromLane];
		references[lane] = from.references[fromLane];
		coordinates[lane] = from.coordinates[fromLane];
		counts[lane] = from.counts[fromLane];
		if (from.arrays != null && from.arrays[fromLane] != null) {
			if (arrays == null) arrays = new long[counts.length][];
			arrays[lane] = from.arrays[fromLane];
		} else if (arrays != null) arrays[lane] = null;
	}

	public Cursor cursor(int windowSize) { return new Cursor(this, windowSize); }

	private void checkTargetLane(int lane) {
		source.checkOpen();
		Objects.checkIndex(lane, size);
	}

	private void checkLane(int lane) {
		checkTargetLane(lane);
		if (source.extraValidation) source.validateDescriptor(kinds[lane], references[lane], coordinates[lane]);
	}

	/** An evaluation/snapshot lease, not one reference count per descriptor. */
	public abstract static class Source implements AutoCloseable {
		private final Object snapshot;
		private final boolean extraValidation;
		private boolean closed;
		protected Source(Object snapshot) { this(snapshot, false); }
		/** Dependent query-local sources opt into generation/descriptor checks at consumption boundaries. */
		protected Source(Object snapshot, boolean extraValidation) {
			this.snapshot = Objects.requireNonNull(snapshot);
			this.extraValidation = extraValidation;
		}
		public final boolean sameSnapshot(Source other) {
			return other != null && getClass() == other.getClass() && snapshot == other.snapshot;
		}
		public final void checkOpen() {
			checkNotClosed();
			if (extraValidation) validate();
		}
		/** Producer reset may discard an old view after its upstream producer has advanced. */
		protected final void checkNotClosed() {
			if (closed) throw new IllegalStateException("borrowed factor source is closed");
		}
		/** Derived sources validate dependency generations; ordinary physical sources have no extra work. */
		protected void validate() { }
		/** Versioned query-local descriptors remain valid even when copied to another batch. */
		protected void validateDescriptor(byte kind, long reference, int coordinate) { }
		public abstract Reader openReader();
		protected void release() { }
		@Override public final void close() {
			if (!closed) { closed = true; release(); }
		}
	}

	/** One mutable decoder per consumer. The offset and returned weights are in logical quad rows. */
	public interface Reader extends AutoCloseable {
		void bind(BorrowedFactorBatch batch, int lane);
		int copyFibers(long quadOffset, int maximum, long[] values, long[] weights);
		@Override default void close() { }
	}

	/**
	 * Bounded, replayable enumeration. No allocation depends on degree and positions remain long.
	 * A physical extent may split an equal-neighbor group; adjacent equal weighted outputs are legal
	 * (and preserve the exact multiset). Consumers requiring uniqueness must coalesce them.
	 */
	public static final class Cursor implements AutoCloseable {
		private BorrowedFactorBatch batch;
		private final Source source;
		private final boolean extraValidation;
		private final Reader reader;
		private final long[] values;
		private final long[] weights;
		private long generation;
		private int lane;
		private long end;
		private long offset;
		private int index;
		private int size;
		private int windowStart;
		private boolean closed;
		private boolean bound;
		private long value;
		private long weight;

		private Cursor(BorrowedFactorBatch batch, int windowSize) {
			if (windowSize <= 0) throw new IllegalArgumentException("non-positive window");
			this.batch = batch;
			this.source = batch.source;
			this.extraValidation = source.extraValidation;
			this.reader = source.openReader();
			values = new long[windowSize];
			weights = new long[windowSize];
		}

		public void bind(int lane) {
			checkOpen();
			bound = false;
			end = batch.count(lane);
			this.lane = lane;
			generation = batch.generation;
			offset = 0L;
			index = size = 0;
			value = weight = 0L;
			if (end != 0L) reader.bind(batch, lane);
			bound = true;
		}

		/**
		 * Reuses this decoder and its bounded window for another descriptor batch owned by the
		 * identical source. Snapshot equality alone is insufficient: the source owns the reader.
		 * Rebinding ends the previous traversal, just like {@link #bind(int)}.
		 */
		public void bind(BorrowedFactorBatch nextBatch, int lane) {
			checkOpen();
			if (Objects.requireNonNull(nextBatch, "batch").source != source)
				throw new IllegalArgumentException("cannot transfer a reader to another owner");
			batch = nextBatch;
			bind(lane);
		}

		public boolean next() {
			checkPosition();
			if (index == size && !refill()) return false;
			value = values[index];
			weight = weights[index++];
			return true;
		}

		/**
		 * Consumes the remainder of one bounded decode window without a per-member cursor dispatch.
		 * Read the returned range [windowStart(), windowStart()+result) immediately, before any next
		 * cursor operation. The arrays are read-only to consumers and are reused on refill. Scalar
		 * next() and nextWindow() may be interleaved without losing or repeating a member.
		 */
		public int nextWindow() {
			checkPosition();
			if (index == size && !refill()) return 0;
			windowStart = index;
			int count = size - index;
			index = size;
			return count;
		}

		public long[] windowValues() { return values; }
		public long[] windowWeights() { return weights; }
		public int windowStart() { return windowStart; }

		private void checkPosition() {
			checkOpen();
			if (!bound) throw new IllegalStateException("factor cursor has not been bound");
			if (generation != batch.generation) throw new IllegalStateException("factor batch was reset during consumption");
			if (extraValidation) source.validateDescriptor(batch.kinds[lane], batch.references[lane], batch.coordinates[lane]);
		}

		private boolean refill() {
			if (offset == end) return false;
			int maximum = (int) Math.min(values.length, end - offset);
			size = reader.copyFibers(offset, maximum, values, weights);
			index = 0;
			if (size <= 0 || size > maximum) throw new IllegalStateException("factor decoder made no progress");
			long nextOffset = offset;
			for (int i = 0; i < size; i++) {
				long w = weights[i];
				if (w <= 0 || w > end - nextOffset) throw new IllegalStateException("invalid factor multiplicity");
				nextOffset += w;
			}
			offset = nextOffset;
			return true;
		}

		public long value() { return value; }
		public long weight() { return weight; }
		private void checkOpen() {
			if (closed) throw new IllegalStateException("factor cursor is closed");
			if (extraValidation) source.checkOpen();
			else source.checkNotClosed();
		}
		@Override public void close() {
			if (!closed) { closed = true; reader.close(); }
		}
	}
}
