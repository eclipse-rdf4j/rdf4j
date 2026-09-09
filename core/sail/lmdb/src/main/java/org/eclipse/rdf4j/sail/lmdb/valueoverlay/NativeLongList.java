/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Objects;

/** Segmented native directory; only one Java long per 8,192 directory entries. Builder-confined until publication. */
final class NativeLongList {
    private static final int SHIFT = 13, MASK = (1 << SHIFT) - 1;
    private final NativeSlabAllocator allocator;
    private long[] segments = new long[4];
    private int size;
    NativeLongList(NativeSlabAllocator allocator) { this.allocator = allocator; }
    int size() { return size; }
    void add(long value) {
        if (size == Integer.MAX_VALUE) throw new OverlayCapacityException("directory limit");
        int seg = size >>> SHIFT;
        if (seg == segments.length) segments = Arrays.copyOf(segments, Math.multiplyExact(seg, 2));
        if (segments[seg] == 0) segments[seg] = allocator.allocate((1L << SHIFT) * 8, 8);
        long h = segments[seg];
        allocator.segment(h).set(FfmAccess.LONG_LE, NativeSlabAllocator.offset(h) + (size & MASK) * 8L, value);
        size++;
    }
    void set(int index, long value) {
        Objects.checkIndex(index, size);
        long h = segments[index >>> SHIFT];
        allocator.segment(h).set(FfmAccess.LONG_LE, NativeSlabAllocator.offset(h) + (index & MASK) * 8L, value);
    }
    long get(int index) {
        Objects.checkIndex(index, size);
        long h = segments[index >>> SHIFT];
        MemorySegment s = allocator.segment(h);
        return s.get(FfmAccess.LONG_LE, NativeSlabAllocator.offset(h) + (index & MASK) * 8L);
    }
}
