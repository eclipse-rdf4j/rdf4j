/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/** Independently addressable unsigned FOR or affine vector, with an explicit eight-byte safe tail. */
final class PackedVector {
    private static final int HEADER = 24;
    private PackedVector() { }

    static byte[] encode(long[] values, int count, boolean affine) {
        Objects.checkFromIndexSize(0, count, values.length);
        if (count == 0) throw new IllegalArgumentException("empty vector");
        long base = values[0];
        long max = base;
        for (int i = 1; i < count; i++) {
            if (Long.compareUnsigned(values[i], base) < 0) base = values[i];
            if (Long.compareUnsigned(values[i], max) > 0) max = values[i];
        }
        long step = count < 2 ? 0 : values[1] - values[0];
        boolean sequence = affine && base == values[0];
        for (int i = 1; sequence && i < count; i++) {
            sequence = Long.compareUnsigned(values[i], values[i - 1]) > 0
                    && values[i] == values[0] + step * i;
        }
        int bits = sequence ? -1 : 64 - Long.numberOfLeadingZeros(max - base);
        int bytes = HEADER + (bits <= 0 ? 0 : Math.toIntExact(((long) bits * count + 7) >>> 3)) + 8;
        byte[] encoded = new byte[bytes];
        ByteBuffer b = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(count).putInt(bits).putLong(base).putLong(step);
        if (bits > 0) for (int i = 0; i < count; i++) put(encoded, HEADER, (long) i * bits, bits, values[i] - base);
        return encoded;
    }

    static long get(NativeSlabAllocator allocator, long handle, int index) {
        MemorySegment s = allocator.segment(handle);
        long p = NativeSlabAllocator.offset(handle);
        int n = s.get(FfmAccess.INT_LE, p);
        Objects.checkIndex(index, n);
        int bits = s.get(FfmAccess.INT_LE, p + 4);
        long base = s.get(FfmAccess.LONG_LE, p + 8);
        if (bits == -1) return base + index * s.get(FfmAccess.LONG_LE, p + 16);
        if (bits < 0 || bits > 64) throw new IllegalStateException("corrupt vector width");
        return base + getBits(s, p + HEADER, (long) index * bits, bits);
    }

    static int lowerBound(NativeSlabAllocator allocator, long handle, long value) {
        MemorySegment s = allocator.segment(handle);
        long p = NativeSlabAllocator.offset(handle);
        int n = s.get(FfmAccess.INT_LE, p);
        int bits = s.get(FfmAccess.INT_LE, p + 4);
        long base = s.get(FfmAccess.LONG_LE, p + 8);
        if (Long.compareUnsigned(value, base) <= 0) return 0;
        if (bits == -1) {
            long step = s.get(FfmAccess.LONG_LE, p + 16);
            if (step == 0) return n;
            long delta = value - base;
            long q = Long.divideUnsigned(delta, step);
            if (Long.compareUnsigned(q, n) >= 0) return n;
            return (int) q + (Long.remainderUnsigned(delta, step) == 0 ? 0 : 1);
        }
        if (bits < 0 || bits > 64) throw new IllegalStateException("corrupt vector width");
        int lo = 0, hi = n;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            long candidate = base + getBits(s, p + HEADER, (long) mid * bits, bits);
            if (Long.compareUnsigned(candidate, value) < 0) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    static long getBits(MemorySegment s, long base, long bit, int width) {
        if (width == 0) return 0;
        int shift = (int) bit & 7;
        long address = base + (bit >>> 3);
        long low = s.get(FfmAccess.LONG_LE, address) >>> shift;
        // Only 64-bit widths need the next byte; the producer owns a bounded safe tail.
        if (shift != 0 && width > 64 - shift) {
            low |= (long) Byte.toUnsignedInt(s.get(java.lang.foreign.ValueLayout.JAVA_BYTE, address + 8)) << (64 - shift);
        }
        return width == 64 ? low : low & ((1L << width) - 1);
    }

    static void put(byte[] out, int base, long bit, int width, long value) {
        int pos = Math.addExact(base, Math.toIntExact(bit >>> 3));
        int shift = (int) bit & 7;
        // Explicit ninth byte avoids Java's masked shift-by-64 semantics.
        long low = value << shift;
        int bytes = Math.min(8, (shift + width + 7) >>> 3);
        for (int b = 0; b < bytes; b++) out[pos + b] |= (byte) (low >>> (b * 8));
        if (shift != 0 && width > 64 - shift) out[pos + 8] |= (byte) (value >>> (64 - shift));
    }
}
