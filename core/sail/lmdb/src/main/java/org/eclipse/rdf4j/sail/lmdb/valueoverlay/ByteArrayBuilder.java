/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.util.Arrays;

public final class ByteArrayBuilder {
    private byte[] bytes;
    private int size;

    public ByteArrayBuilder() {
        this(64);
    }

    public ByteArrayBuilder(int initialCapacity) {
        bytes = new byte[Math.max(8, initialCapacity)];
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
    }

    public void writeByte(int value) {
        ensure(1);
        bytes[size++] = (byte) value;
    }

    public void writeBytes(byte[] source) {
        writeBytes(source, 0, source.length);
    }

    public void writeBytes(byte[] source, int offset, int length) {
        if ((offset | length) < 0 || offset > source.length - length) {
            throw new IndexOutOfBoundsException();
        }
        ensure(length);
        System.arraycopy(source, offset, bytes, size, length);
        size += length;
    }

    public void writeVarUInt(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("negative unsigned int");
        }
        while ((value & ~0x7f) != 0) {
            writeByte((value & 0x7f) | 0x80);
            value >>>= 7;
        }
        writeByte(value);
    }

    public void writeVarULong(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("negative unsigned long");
        }
        while ((value & ~0x7fL) != 0) {
            writeByte((int) (value & 0x7fL) | 0x80);
            value >>>= 7;
        }
        writeByte((int) value);
    }

    public void writeIntLE(int value) {
        ensure(4);
        bytes[size++] = (byte) value;
        bytes[size++] = (byte) (value >>> 8);
        bytes[size++] = (byte) (value >>> 16);
        bytes[size++] = (byte) (value >>> 24);
    }

    public void writeLongLE(long value) {
        ensure(8);
        for (int shift = 0; shift < 64; shift += 8) {
            bytes[size++] = (byte) (value >>> shift);
        }
    }

    public void setByte(int index, int value) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        bytes[index] = (byte) value;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(bytes, size);
    }

    public byte[] unsafeArray() {
        return bytes;
    }

    private void ensure(int additional) {
        int required = Math.addExact(size, additional);
        if (required > bytes.length) {
            int capacity = bytes.length;
            while (capacity < required) {
                capacity = Math.max(required, capacity + (capacity >>> 1) + 16);
            }
            bytes = Arrays.copyOf(bytes, capacity);
        }
    }
}
