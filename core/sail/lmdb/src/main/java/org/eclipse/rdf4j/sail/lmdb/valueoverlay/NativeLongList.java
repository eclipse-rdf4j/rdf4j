/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Objects;

/** Segmented native directory; only one Java long per 8,192 directory entries. Builder-confined until publication. */
final class NativeLongList {
	private final int shift, mask;
	private final NativeSlabAllocator allocator;
	private long[] segments = new long[4];
	private int size;

	NativeLongList(NativeSlabAllocator allocator) {
		this(allocator, 13);
	}

	NativeLongList(NativeSlabAllocator allocator, int shift) {
		if (shift < 4 || shift > 13)
			throw new IllegalArgumentException("directory shift");
		this.allocator = allocator;
		this.shift = shift;
		this.mask = (1 << shift) - 1;
		allocator.retainHeap(OverlayMemoryBudget.arrayBytes(segments.length, 8) + 64);
	}

	int size() {
		return size;
	}

	void add(long value) {
		if (size == Integer.MAX_VALUE)
			throw new OverlayCapacityException("directory limit");
		int seg = size >>> shift;
		if (seg == segments.length) {
			int capacity = Math.multiplyExact(seg, 2);
			allocator.retainHeap(OverlayMemoryBudget.arrayBytes(capacity, 8));
			segments = Arrays.copyOf(segments, capacity);
		}
		if (segments[seg] == 0)
			segments[seg] = allocator.allocate((1L << shift) * 8, 8);
		long h = segments[seg];
		allocator.segment(h).set(FfmAccess.LONG_LE, NativeSlabAllocator.offset(h) + (size & mask) * 8L, value);
		size++;
	}

	void set(int index, long value) {
		Objects.checkIndex(index, size);
		long h = segments[index >>> shift];
		allocator.segment(h).set(FfmAccess.LONG_LE, NativeSlabAllocator.offset(h) + (index & mask) * 8L, value);
	}

	long get(int index) {
		Objects.checkIndex(index, size);
		long h = segments[index >>> shift];
		MemorySegment s = allocator.segment(h);
		return s.get(FfmAccess.LONG_LE, NativeSlabAllocator.offset(h) + (index & mask) * 8L);
	}
}
