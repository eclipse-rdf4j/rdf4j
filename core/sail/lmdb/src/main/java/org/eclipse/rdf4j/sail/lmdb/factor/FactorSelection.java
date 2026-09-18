/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Ordered, disjoint intervals in a factor's logical (multiplicity-expanded) position space. Only positions are
 * retained, never term values. Adjacent intervals are coalesced. Both positions and cardinalities are long: a single
 * compressed fiber may represent more than Integer.MAX_VALUE logical rows. A builder belongs to its producer;
 * SelectedFactorSource copies its metadata before publishing it, so the builder can immediately be reused.
 */
public final class FactorSelection {
	private long[] starts;
	private long[] ends;
	private long[] prefixEnds;
	private int size;
	private long count;

	public FactorSelection(int initialCapacity) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException("negative selection capacity");
		starts = new long[initialCapacity];
		ends = new long[initialCapacity];
		prefixEnds = new long[initialCapacity];
	}

	public void clear() {
		size = 0;
		count = 0L;
	}

	public int ranges() {
		return size;
	}

	public long count() {
		return count;
	}

	public long start(int range) {
		return starts[Objects.checkIndex(range, size)];
	}

	public long end(int range) {
		return ends[Objects.checkIndex(range, size)];
	}

	public long prefixEnd(int range) {
		return prefixEnds[Objects.checkIndex(range, size)];
	}

	public long prefixStart(int range) {
		Objects.checkIndex(range, size);
		return range == 0 ? 0L : prefixEnds[range - 1];
	}

	public long retainedBytes() {
		return (long) starts.length * 3L * Long.BYTES;
	}

	/** Add a qualifying interval; overlapping or out-of-order intervals are programming errors. */
	public void add(long start, long length) {
		if (start < 0L || length < 0L)
			throw new IllegalArgumentException("negative selected interval");
		long end = Math.addExact(start, length);
		if (size != 0 && start < ends[size - 1])
			throw new IllegalArgumentException("selection intervals must be ordered and disjoint");
		if (length == 0L)
			return;
		long total = Math.addExact(count, length);
		if (size != 0 && start == ends[size - 1]) {
			ends[size - 1] = end;
			prefixEnds[size - 1] = total;
		} else {
			ensureCapacity(Math.addExact(size, 1));
			starts[size] = start;
			ends[size] = end;
			prefixEnds[size++] = total;
		}
		count = total;
	}

	public void validateBounds(long sourceCount) {
		if (sourceCount < 0L || (size != 0 && ends[size - 1] > sourceCount))
			throw new IllegalArgumentException("selection extends beyond its source relation");
	}

	/** Find a physical interval by a logical position in the selected relation. */
	public int rangeAt(long logicalPosition) {
		if (logicalPosition < 0L || logicalPosition >= count)
			throw new IndexOutOfBoundsException("selected position " + logicalPosition);
		int lo = 0, hi = size;
		while (lo < hi) {
			int middle = (lo + hi) >>> 1;
			if (prefixEnds[middle] <= logicalPosition)
				lo = middle + 1;
			else
				hi = middle;
		}
		return lo;
	}

	public void copyFrom(FactorSelection from) {
		if (from == this)
			return;
		ensureCapacity(from.size);
		System.arraycopy(from.starts, 0, starts, 0, from.size);
		System.arraycopy(from.ends, 0, ends, 0, from.size);
		System.arraycopy(from.prefixEnds, 0, prefixEnds, 0, from.size);
		size = from.size;
		count = from.count;
	}

	/**
	 * Compose positions selected from another selection, directly into the ultimate source's position space. This
	 * flattens reader indirections; it does not copy or decode source values.
	 */
	public void compose(FactorSelection source, FactorSelection selected) {
		if (source == this || selected == this)
			throw new IllegalArgumentException("aliased selection composition");
		selected.validateBounds(source.count);
		clear();
		int sourceRange = 0;
		for (int i = 0; i < selected.size; i++) {
			long at = selected.starts[i], limit = selected.ends[i];
			if (sourceRange == source.size || at >= source.prefixEnds[sourceRange])
				sourceRange = source.rangeAt(at);
			while (at < limit) {
				long before = sourceRange == 0 ? 0L : source.prefixEnds[sourceRange - 1];
				long take = Math.min(limit - at, source.prefixEnds[sourceRange] - at);
				add(source.starts[sourceRange] + (at - before), take);
				at += take;
				if (at == source.prefixEnds[sourceRange])
					sourceRange++;
			}
		}
	}

	private void ensureCapacity(int required) {
		if (required <= starts.length)
			return;
		int capacity = (int) Math.min(Integer.MAX_VALUE,
				Math.max(required, Math.max(8L, (long) starts.length * 2L)));
		starts = Arrays.copyOf(starts, capacity);
		ends = Arrays.copyOf(ends, capacity);
		prefixEnds = Arrays.copyOf(prefixEnds, capacity);
	}
}
