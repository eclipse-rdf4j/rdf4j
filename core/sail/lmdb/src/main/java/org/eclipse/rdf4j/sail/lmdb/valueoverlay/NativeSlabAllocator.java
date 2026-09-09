/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

/** Append-only shared-arena slab allocator using a compact slab+offset handle. */
public final class NativeSlabAllocator implements AutoCloseable {
    private static final long OFFSET_MASK = (1L << 48) - 1;

    private final Arena arena = Arena.ofShared();
    private final long standardSlabBytes;
    private final long maximumReservedBytes;
    private volatile MemorySegment[] slabs = new MemorySegment[0];
    private long nextSlabBytes;
    private long currentOffset;
    private long usedBytes;
    private long reservedBytes;
    private boolean closed;

    public NativeSlabAllocator(long standardSlabBytes) {
        this(standardSlabBytes, Long.MAX_VALUE);
    }

    public NativeSlabAllocator(long standardSlabBytes, long maximumReservedBytes) {
        if (standardSlabBytes < (1L << 16)) {
            throw new IllegalArgumentException("standard slab must be at least 64 KiB");
        }
        if (maximumReservedBytes < 0) throw new IllegalArgumentException("negative budget");
        this.maximumReservedBytes = maximumReservedBytes;
        this.standardSlabBytes = standardSlabBytes;
        this.nextSlabBytes = 1L << 16;
    }

    public synchronized long allocate(long bytes, long alignment) {
        ensureOpen();
        if (bytes < 0 || bytes > (1L << 47) || alignment <= 0 || (alignment & (alignment - 1)) != 0) {
            throw new IllegalArgumentException("invalid native allocation");
        }
        if (bytes == 0) {
            bytes = 1;
        }
        MemorySegment[] snapshot = slabs;
        long bias = snapshot.length == 0 ? 0 : snapshot[snapshot.length - 1].address() & (alignment - 1);
        long offset = FfmAccess.alignUp(Math.addExact(currentOffset, bias), alignment) - bias;
        if (snapshot.length == 0 || offset > snapshot[snapshot.length - 1].byteSize() - bytes) {
            long requested = FfmAccess.alignUp(bytes, 8);
            long capacity = Math.max(nextSlabBytes, requested);
            long available = maximumReservedBytes - reservedBytes;
            if (requested > available) throw new OverlayCapacityException("native overlay budget exceeded");
            capacity = Math.min(capacity, available);
            if (capacity >= (1L << 48)) {
                throw new IllegalArgumentException("one slab cannot reach 2^48 bytes");
            }
            if (snapshot.length >= 65_534) {
                throw new IllegalStateException("native slab handle space exhausted");
            }
            MemorySegment slab = arena.allocate(capacity, Math.max(8, alignment));
            snapshot = Arrays.copyOf(snapshot, snapshot.length + 1);
            snapshot[snapshot.length - 1] = slab;
            slabs = snapshot; // safely publish slab before any handle can be published
            reservedBytes = Math.addExact(reservedBytes, capacity);
            if (capacity <= standardSlabBytes) {
                long doubled = capacity > (Long.MAX_VALUE >>> 1) ? Long.MAX_VALUE : capacity << 1;
                nextSlabBytes = Math.min(standardSlabBytes, Math.max(nextSlabBytes, doubled));
            }
            currentOffset = 0;
            offset = 0;
        }
        int slabIndex = snapshot.length - 1;
        currentOffset = Math.addExact(offset, bytes);
        usedBytes = Math.addExact(usedBytes, bytes);
        return ((long) (slabIndex + 1) << 48) | offset;
    }

    public long copy(byte[] bytes) {
        long handle = allocate(bytes.length, 1);
        write(handle, bytes);
        return handle;
    }

    public void write(long handle, byte[] bytes) {
        MemorySegment segment = segment(handle);
        long offset = offset(handle);
        if (offset > segment.byteSize() - bytes.length) {
            throw new IndexOutOfBoundsException("write crosses native slab");
        }
        segment.asSlice(offset, bytes.length).copyFrom(MemorySegment.ofArray(bytes));
    }

    public byte[] read(long handle, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("negative length");
        }
        MemorySegment segment = segment(handle);
        long offset = offset(handle);
        if (offset > segment.byteSize() - length) {
            throw new IllegalStateException("corrupt native handle/length");
        }
        return segment.asSlice(offset, length).toArray(ValueLayout.JAVA_BYTE);
    }

    public MemorySegment segment(long handle) {
        int index = (int) (handle >>> 48) - 1;
        MemorySegment[] snapshot = slabs;
        if (index < 0 || index >= snapshot.length) {
            throw new IllegalStateException("invalid native handle");
        }
        return snapshot[index];
    }

    public static long offset(long handle) {
        return handle & OFFSET_MASK;
    }

    public synchronized long usedBytes() {
        return usedBytes;
    }

    public synchronized long reservedBytes() {
        return reservedBytes;
    }

    public int slabCount() {
        return slabs.length;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("native allocator is closed");
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            arena.close();
            slabs = new MemorySegment[0];
        }
    }
}
