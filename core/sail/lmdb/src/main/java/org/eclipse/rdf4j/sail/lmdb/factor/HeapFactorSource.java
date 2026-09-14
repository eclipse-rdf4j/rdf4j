/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials are made
 * available under the Eclipse Distribution License v1.0 (BSD-3-Clause).
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

/** Immutable heap-array borrowing; the source keeps the snapshot owner strongly reachable. */
public final class HeapFactorSource extends BorrowedFactorBatch.Source {
	public HeapFactorSource(Object owner) {
		super(owner);
	}

	@Override
	public BorrowedFactorBatch.Reader openReader() {
		checkOpen();
		return new BorrowedFactorBatch.Reader() {
			private long[] values;
			private int start;
			private int count;

			@Override
			public void bind(BorrowedFactorBatch batch, int lane) {
				checkOpen();
				if (batch.source() != HeapFactorSource.this || batch.kind(lane) != BorrowedFactorBatch.HEAP)
					throw new IllegalArgumentException("incompatible heap factor");
				values = batch.array(lane);
				start = batch.coordinate(lane);
				count = Math.toIntExact(batch.count(lane));
			}

			@Override
			public int copyFibers(long offset, int maximum, long[] out, long[] weights) {
				checkOpen();
				int at = Math.toIntExact(offset);
				if (at < 0 || at > count || maximum < 0 || maximum > out.length || maximum > weights.length)
					throw new IllegalArgumentException("invalid heap factor copy");
				int copied = 0;
				int end = at + Math.min(count - at, Math.max(maximum, 4096));
				while (at < end && copied < maximum) {
					long value = values[start + at];
					int from = at++;
					while (at < end && values[start + at] == value)
						at++;
					out[copied] = value;
					weights[copied++] = at - from;
				}
				return copied;
			}
		};
	}
}
