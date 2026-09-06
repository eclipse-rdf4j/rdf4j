/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Borrowed native unsigned-64-bit ID spans. The supplied lease must keep the allocation
 * immutable and address-stable until this source closes; a raw address alone is not a lease.
 * Readers use a bounded slice of the original segment, never ofAddress/reinterpret.
 * Closing a caller-owned FFM arena early therefore fails safely rather than reading freed memory.
 */
public final class NativeLongFactorSource extends BorrowedFactorBatch.Source {
	private static final ValueLayout.OfLong ID = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
	private final MemorySegment allocation;
	private final AutoCloseable lease;

	public NativeLongFactorSource(MemorySegment allocation, AutoCloseable lease) {
		super(Objects.requireNonNull(allocation, "allocation"));
		if (!allocation.isNative()) throw new IllegalArgumentException("native allocation required");
		this.allocation = allocation.asReadOnly();
		this.lease = Objects.requireNonNull(lease, "lease");
	}

	/** Exports a byte-bounded raw span; no payload read, copy, or unsafe reinterpretation. */
	public void bind(BorrowedFactorBatch target, int lane, long byteOffset, long count) {
		checkOpen();
		if (target.source() != this) throw new IllegalArgumentException("different source");
		if (count <= 0L) throw new IllegalArgumentException("non-positive count");
		long bytes = Math.multiplyExact(count, Long.BYTES);
		MemorySegment slice = allocation.asSlice(byteOffset, bytes);
		target.bindNative(lane, BorrowedFactorBatch.RAW_U64, slice.address(), byteOffset, 0, count);
	}

	@Override public BorrowedFactorBatch.Reader openReader() {
		checkOpen();
		return new BorrowedFactorBatch.Reader() {
			private MemorySegment span;
			private long count;
			@Override public void bind(BorrowedFactorBatch batch, int lane) {
				checkOpen();
				if (batch.source() != NativeLongFactorSource.this || batch.kind(lane) != BorrowedFactorBatch.RAW_U64)
					throw new IllegalArgumentException("incompatible raw factor");
				count = batch.count(lane);
				span = allocation.asSlice(batch.reference(lane), Math.multiplyExact(count, Long.BYTES));
				if (span.address() != batch.address(lane)) throw new IllegalArgumentException("inconsistent raw address");
			}
			@Override public int copyFibers(long offset, int maximum, long[] values, long[] weights) {
				checkOpen();
				if (span == null || offset < 0L || offset > count || maximum < 0
						|| maximum > values.length || maximum > weights.length)
					throw new IllegalArgumentException("invalid raw read");
				MemorySegment memory = span;
				int copied = 0;
				long end = offset + Math.min(count - offset, Math.max(maximum, 4096));
				while (offset < end && copied < maximum) {
					long value = memory.get(ID, offset * Long.BYTES);
					long start = offset++;
					while (offset < end && memory.get(ID, offset * Long.BYTES) == value) offset++;
					values[copied] = value;
					weights[copied++] = offset - start;
				}
				return copied;
			}
		};
	}

	@Override protected void release() {
		try { lease.close(); }
		catch (RuntimeException | Error e) { throw e; }
		catch (Exception e) { throw new IllegalStateException("closing native factor lease", e); }
	}
}
