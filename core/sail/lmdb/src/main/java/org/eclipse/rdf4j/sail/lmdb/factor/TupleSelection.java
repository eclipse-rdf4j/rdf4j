/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Query-local selection of complete correlated tuples. A stable-row source retains only row offsets and prefix weights
 * (12 bytes/member); other sources retain logical position ranges. No tuple values are copied. The builder is bound to
 * one source descriptor and may be reused after {@link SelectedTupleSource#select(TupleSelection)} copies its metadata.
 *
 * <p>
 * Accepted tuple fragments must arrive in source order. This preserves duplicates, correlation and encounter order
 * within a group; it is not a set or a global distinct operation.
 */
public final class TupleSelection {
	private final int initialCapacity;
	private BorrowedTupleBatch input;
	private int lane;
	private long generation, reference, inputCount, lastEnd, count;
	private long[] backing;
	private int[] offsets;
	private long[] prefixEnds;
	private FactorSelection ranges;
	private int size;
	private boolean direct;

	public TupleSelection(int initialCapacity) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException("negative selection capacity");
		this.initialCapacity = initialCapacity;
	}

	public void begin(BorrowedTupleBatch.Cursor cursor) {
		Objects.requireNonNull(cursor, "cursor");
		input = cursor.boundBatch();
		lane = cursor.boundLane();
		generation = cursor.boundGeneration();
		reference = input.reference(lane);
		inputCount = input.count(lane);
		direct = cursor.hasDirectRows();
		backing = direct && inputCount != 0L ? cursor.directRowArray() : null;
		lastEnd = count = 0L;
		size = 0;
		if (!direct && ranges == null)
			ranges = new FactorSelection(initialCapacity);
		if (ranges != null)
			ranges.clear();
	}

	/** Retains the reader's current coordinates, not its cursor or decoded values. */
	public void add(BorrowedTupleBatch.Cursor cursor) {
		Objects.requireNonNull(cursor, "cursor").capture(this);
	}

	/** Only the checked cursor capture entrypoint supplies physical coordinates. */
	void addCurrent(BorrowedTupleBatch source, int sourceLane, long sourceGeneration,
			long at, long weight, long[] rows, int rowOffset) {
		if (input == null || input != source || lane != sourceLane || generation != sourceGeneration)
			throw new IllegalArgumentException("tuple selection belongs to a different binding");
		if (at < lastEnd || at > inputCount || weight <= 0L || weight > inputCount - at)
			throw new IllegalArgumentException("unordered or invalid tuple fragment");
		long total = Math.addExact(count, weight);
		if (direct) {
			if (backing != rows)
				throw new IllegalStateException("direct backing changed");
			ensureCapacity(Math.addExact(size, 1));
			offsets[size] = rowOffset;
			prefixEnds[size] = total;
		} else
			ranges.add(at, weight);
		size++;
		count = total;
		lastEnd = at + weight;
	}

	public boolean hasDirectRows() {
		return direct;
	}

	public int members() {
		return size;
	}

	public long count() {
		return count;
	}

	public long retainedBytes() {
		return (offsets == null ? 0L : (long) offsets.length * (Integer.BYTES + Long.BYTES))
				+ (ranges == null ? 0L : ranges.retainedBytes());
	}

	void checkValid() {
		if (input == null)
			throw new IllegalStateException("unbound tuple selection");
		input.checkSnapshot(lane, generation, reference, inputCount);
	}

	BorrowedTupleBatch input() {
		checkValid();
		return input;
	}

	int lane() {
		return lane;
	}

	long[] backing() {
		return backing;
	}

	int[] offsets() {
		return offsets;
	}

	long[] prefixEnds() {
		return prefixEnds;
	}

	FactorSelection ranges() {
		return ranges;
	}

	/** Drop borrowed references without discarding reusable primitive selection buffers. */
	public void clear() {
		input = null;
		backing = null;
		count = 0L;
		size = 0;
		if (ranges != null)
			ranges.clear();
	}

	private void ensureCapacity(int required) {
		if (offsets != null && required <= offsets.length)
			return;
		int capacity = (int) Math.min(Integer.MAX_VALUE, Math.max(required,
				Math.max(initialCapacity, offsets == null ? 8L : 2L * offsets.length)));
		offsets = offsets == null ? new int[capacity] : Arrays.copyOf(offsets, capacity);
		prefixEnds = prefixEnds == null ? new long[capacity] : Arrays.copyOf(prefixEnds, capacity);
	}
}
