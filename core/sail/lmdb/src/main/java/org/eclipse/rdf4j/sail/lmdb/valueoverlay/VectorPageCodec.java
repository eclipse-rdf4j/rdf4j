/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lossless lexical templates: common bytes plus independent packed numeric or byte columns.
 * Decimal digit runs are integers ONLY as a reversible byte encoding, never as RDF numeric canonicalization.
 */
final class VectorPageCodec {
    static final int MAGIC = 0x32505643; // CVP2, process-local and not a persistent ValueStore format
    private static final int HEADER = 20, FIELD_BYTES = 20;
    record Field(int start, int length, int bits, long base, long[] values) { }
    private VectorPageCodec() { }

    static byte[] encode(PhysicalRecord[] records, int count, boolean digitVectors) {
        int length = records[0].bytes().length;
        if (count < 8 || length > 512) return null;
        for (int i = 0; i < count; i++) if (records[i].large() || records[i].bytes().length != length) return null;
        byte[] template = records[0].bytes().clone();
        List<Field> fields = new ArrayList<>();
        for (int pos = 0; pos < length;) {
            boolean allDigits = digitVectors;
            for (int r = 0; allDigits && r < count; r++) allDigits = digit(records[r].bytes()[pos]);
            if (allDigits) {
                int end = pos + 1;
                outer: while (end < length && end - pos < 18) {
                    for (int r = 0; r < count; r++) if (!digit(records[r].bytes()[end])) break outer;
                    end++;
                }
                long[] values = new long[count];
                long min = Long.MAX_VALUE, max = 0;
                for (int r = 0; r < count; r++) {
                    long v = 0;
                    for (int p = pos; p < end; p++) v = v * 10 + records[r].bytes()[p] - '0';
                    values[r] = v; min = Math.min(min, v); max = Math.max(max, v);
                }
                if (max != min) fields.add(new Field(pos, end - pos, 64 - Long.numberOfLeadingZeros(max - min), min, values));
                pos = end;
            } else {
                boolean constant = true;
                for (int r = 1; r < count; r++) constant &= records[r].bytes()[pos] == template[pos];
                if (constant) { pos++; continue; }
                // UUID/hex and base64-like literal or IRI components use compact lexical alphabets.
                // This is byte-preserving encoding, not parsing or normalization of an RDF value.
                int end = alphabetEnd(records, count, pos, 2);
                int alphabet = 2, bits = 5;
                if (end > pos) {
                    boolean lower = false, upper = false;
                    for (int r = 0; r < count; r++) for (int j = pos; j < end; j++) {
                        byte v = records[r].bytes()[j]; lower |= v >= 'a' && v <= 'f'; upper |= v >= 'A' && v <= 'F';
                    }
                    if (!upper) { alphabet = 0; bits = 4; }
                    else if (!lower) { alphabet = 1; bits = 4; }
                } else {
                    end = alphabetEnd(records, count, pos, 3);
                    int urlEnd = alphabetEnd(records, count, pos, 4);
                    alphabet = 3; bits = 6;
                    if (urlEnd > end) { end = urlEnd; alphabet = 4; }
                }
                if (end > pos) {
                    int run = end - pos;
                    long[] values = new long[Math.multiplyExact(count, run)];
                    for (int r = 0; r < count; r++) for (int j = 0; j < run; j++)
                        values[r * run + j] = alphabetValue(records[r].bytes()[pos + j], alphabet);
                    fields.add(new Field(pos, -run, bits, alphabet, values));
                    pos = end;
                } else {
                    long[] values = new long[count];
                    long min = 255, max = 0;
                    for (int r = 0; r < count; r++) {
                        long v = records[r].bytes()[pos] & 255L;
                        values[r] = v; min = Math.min(min, v); max = Math.max(max, v);
                    }
                    if (min != max) fields.add(new Field(pos, 0, 64 - Long.numberOfLeadingZeros(max - min), min, values));
                    pos++;
                }
            }
        }
        int bytes = Math.addExact(HEADER + length, Math.multiplyExact(fields.size(), FIELD_BYTES));
        for (Field f : fields) bytes = Math.addExact(bytes, (int) (((long) f.values.length * f.bits + 7) >>> 3) + 8);
        byte[] out = new byte[bytes];
        ByteBuffer b = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(MAGIC).putInt(count).putInt(length).putInt(fields.size()).putInt(bytes);
        b.put(template);
        int payload = HEADER + length + fields.size() * FIELD_BYTES;
        for (Field f : fields) {
            b.putInt(f.start).put((byte) (f.length < 0 ? 255 : f.length)).put((byte) f.bits)
                    .putShort((short) (f.length < 0 ? -f.length : 0)).putLong(f.base).putInt(payload);
            for (int r = 0; r < f.values.length; r++) PackedVector.put(out, payload, (long) r * f.bits, f.bits,
                    f.length < 0 ? f.values[r] : f.values[r] - f.base);
            payload += (int) (((long) f.values.length * f.bits + 7) >>> 3) + 8;
        }
        return out;
    }

    static PageCodec.DecodedRecord read(NativeSlabAllocator allocator, long handle, int slot) {
        MemorySegment raw = allocator.segment(handle);
        long p = NativeSlabAllocator.offset(handle);
        int total = raw.get(FfmAccess.INT_LE, p + 16);
        if (total < HEADER) throw new IllegalStateException("corrupt vector page size");
        MemorySegment s = raw.asSlice(p, total);
        int n = s.get(FfmAccess.INT_LE, 4), len = s.get(FfmAccess.INT_LE, 8), fields = s.get(FfmAccess.INT_LE, 12);
        if (n < 1 || slot < 0 || slot >= n || len < 0 || len > 512 || fields < 0 || fields > len
                || HEADER + (long) len + (long) fields * FIELD_BYTES > total) throw new IllegalStateException("corrupt vector page");
        byte[] out = s.asSlice(HEADER, len).toArray(ValueLayout.JAVA_BYTE);
        long at = HEADER + len;
        int previousEnd = 0;
        for (int f = 0; f < fields; f++, at += FIELD_BYTES) {
            int start = s.get(FfmAccess.INT_LE, at);
            int width = s.get(ValueLayout.JAVA_BYTE, at + 4) & 255;
            int bits = s.get(ValueLayout.JAVA_BYTE, at + 5) & 255;
            long base = s.get(FfmAccess.LONG_LE, at + 8);
            int payload = s.get(FfmAccess.INT_LE, at + 16);
            int run = width == 255 ? Short.toUnsignedInt(s.get(FfmAccess.SHORT_LE, at + 6)) : Math.max(1, width);
            int end = start + run;
            long payloadBytes = ((long) n * (width == 255 ? run : 1) * bits + 7) / 8 + 8;
            if (start < previousEnd || run < 1 || end > len || width > 18 && width != 255 || bits < 1 || bits > 60
                    || payload < HEADER + len + fields * FIELD_BYTES || payload + payloadBytes > total) {
                throw new IllegalStateException("corrupt vector field");
            }
            if (width == 255) {
                if (base < 0 || base > 4 || bits != (base <= 1 ? 4 : base == 2 ? 5 : 6))
                    throw new IllegalStateException("corrupt alphabet field");
                long bit = (long) slot * run * bits;
                for (int j = 0; j < run; j++, bit += bits) {
                    int code = (int) PackedVector.getBits(s, payload, bit, bits);
                    out[start + j] = alphabetByte(code, (int) base);
                }
            } else {
                long v = base + PackedVector.getBits(s, payload, (long) slot * bits, bits);
                if (width == 0) {
                    if ((v & ~255L) != 0) throw new IllegalStateException("corrupt byte field");
                    out[start] = (byte) v;
                } else {
                    if (v < 0) throw new IllegalStateException("corrupt digit field");
                    for (int i = end - 1; i >= start; i--) { out[i] = (byte) ('0' + v % 10); v /= 10; }
                    if (v != 0) throw new IllegalStateException("digit field overflows its lexical width");
                }
            }
            previousEnd = end;
        }
        return new PageCodec.DecodedRecord(out, false);
    }
    private static int alphabetEnd(PhysicalRecord[] records, int count, int start, int alphabet) {
        int end = start, length = records[0].bytes().length;
        outer: while (end < length) {
            for (int r = 0; r < count; r++) if (alphabetValue(records[r].bytes()[end], alphabet) < 0) break outer;
            end++;
        }
        return end;
    }
    private static int alphabetValue(byte v, int alphabet) {
        if (alphabet <= 2) {
            if (v >= '0' && v <= '9') return v - '0';
            if (v >= 'a' && v <= 'f' && alphabet != 1) return v - 'a' + 10;
            if (v >= 'A' && v <= 'F' && alphabet != 0) return v - 'A' + (alphabet == 2 ? 16 : 10);
        } else {
            if (v >= 'A' && v <= 'Z') return v - 'A';
            if (v >= 'a' && v <= 'z') return v - 'a' + 26;
            if (v >= '0' && v <= '9') return v - '0' + 52;
            if (v == (alphabet == 3 ? '+' : '-')) return 62;
            if (v == (alphabet == 3 ? '/' : '_')) return 63;
        }
        return -1;
    }
    private static byte alphabetByte(int value, int alphabet) {
        if (alphabet <= 2) {
            if (value < 10) return (byte) ('0' + value);
            if (value < 16) return (byte) ((alphabet == 1 ? 'A' : 'a') + value - 10);
            if (alphabet == 2 && value < 22) return (byte) ('A' + value - 16);
        } else {
            if (value < 26) return (byte) ('A' + value);
            if (value < 52) return (byte) ('a' + value - 26);
            if (value < 62) return (byte) ('0' + value - 52);
            if (value == 62) return (byte) (alphabet == 3 ? '+' : '-');
            if (value == 63) return (byte) (alphabet == 3 ? '/' : '_');
        }
        throw new IllegalStateException("corrupt alphabet code");
    }
    private static boolean digit(byte v) { return v >= '0' && v <= '9'; }
}
