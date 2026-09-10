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
 * Borrowed native unsigned-64-bit ID spans. The supplied lease must keep the allocation immutable and address-stable
 * until this source closes; a raw address alone is not a lease. Readers validate a span against the original
 * allocation, never ofAddress/reinterpret. Closing a caller-owned FFM arena early therefore fails safely rather than
 * reading freed memory.
 */
public final class NativeLongFactorSource extends BorrowedFactorBatch.Source {
	private static final ValueLayout.OfLong ID = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
	private final MemorySegment allocation;
	private final AutoCloseable lease;

	public NativeLongFactorSource(MemorySegment allocation, AutoCloseable lease) {
		super(Objects.requireNonNull(allocation, "allocation"));
		if (!allocation.isNative())
			throw new IllegalArgumentException("native allocation required");
		this.allocation = allocation.asReadOnly();
		this.lease = Objects.requireNonNull(lease, "lease");
	}

	/** Exports a byte-bounded raw span; no payload read, copy, or unsafe reinterpretation. */
	public void bind(BorrowedFactorBatch target, int lane, long byteOffset, long count) {
		checkOpen();
		if (target.source() != this)
			throw new IllegalArgumentException("different source");
		if (count <= 0L)
			throw new IllegalArgumentException("non-positive count");
		long bytes = Math.multiplyExact(count, Long.BYTES);
		Objects.checkFromIndexSize(byteOffset, bytes, allocation.byteSize());
		target.bindNative(lane, BorrowedFactorBatch.RAW_U64, allocation.address() + byteOffset, byteOffset, 0, count);
	}

	@Override
	public BorrowedFactorBatch.Reader openReader() {
		checkOpen();
		return new BorrowedFactorBatch.Reader() {
			private long byteBase;
			private boolean bound;
			private long count;

			@Override
			public void bind(BorrowedFactorBatch batch, int lane) {
				checkOpen();
				if (batch.source() != NativeLongFactorSource.this || batch.kind(lane) != BorrowedFactorBatch.RAW_U64)
					throw new IllegalArgumentException("incompatible raw factor");
				bound = false;
				long length = batch.count(lane);
				long base = batch.reference(lane);
				Objects.checkFromIndexSize(base, Math.multiplyExact(length, Long.BYTES), allocation.byteSize());
				if (allocation.address() + base != batch.address(lane))
					throw new IllegalArgumentException("inconsistent raw address");
				byteBase = base;
				count = length;
				bound = true;
			}

			@Override
			public int copyFibers(long offset, int maximum, long[] values, long[] weights) {
				checkOpen();
				if (!bound || offset < 0L || offset > count || maximum < 0
						|| maximum > values.length || maximum > weights.length)
					throw new IllegalArgumentException("invalid raw read");
				// Validate the retained allocation once per bulk window, not once per decoded ID.
				// This copies only the requested transient read window; the factor remains borrowed.
				// Adjacent duplicate fragments across windows retain exact weights by Reader contract.
				int end = (int) Math.min(count - offset, maximum);
				MemorySegment.copy(allocation, ID, byteBase + offset * Long.BYTES, values, 0, end);
				int at = 0, copied = 0;
				while (at < end) {
					long value = values[at];
					int start = at++;
					while (at < end && values[at] == value)
						at++;
					values[copied] = value;
					weights[copied++] = at - start;
				}
				return copied;
			}
		};
	}

	@Override
	protected void release() {
		try {
			lease.close();
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("closing native factor lease", e);
		}
	}
}
