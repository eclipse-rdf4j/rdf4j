/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedTupleBatch;

/**
 * Borrows grouped matches from the existing primitive hash table, without building another index, copying whole payload
 * chains or confusing correlated payload columns with independent factors. The table is sealed at export. Close this
 * source before releasing its builder's memory ledger.
 */
final class PrimitiveHashJoinFactorSource extends BorrowedTupleBatch.Source {
	private PrimitiveHashJoinTable table;

	PrimitiveHashJoinFactorSource(PrimitiveHashJoinTable table) {
		super(Objects.requireNonNull(table, "table").payloadWidth);
		table.seal();
		this.table = table;
	}

	void export(BorrowedTupleBatch batch, int lane, int bucket) {
		checkOpen();
		if (batch.source() != this)
			throw new IllegalArgumentException("foreign descriptor source");
		Objects.checkIndex(bucket, table.occupied.length);
		if (table.occupied[bucket] == 0)
			throw new IllegalArgumentException("unoccupied bucket");
		batch.bind(lane, bucket, table.chainCounts[bucket]);
	}

	@Override
	protected void validate(long reference, long count) {
		checkOpen();
		if (reference < 0 || reference >= table.occupied.length || table.occupied[(int) reference] == 0
				|| count != table.chainCounts[(int) reference])
			throw new IllegalArgumentException("not an exact hash group descriptor");
	}

	@Override
	public BorrowedTupleBatch.Reader openReader() {
		checkOpen();
		return new ChainReader();
	}

	@Override
	protected void release() {
		table = null;
	}

	private final class ChainReader implements BorrowedTupleBatch.DirectReader {
		private int head = -1, next = -1, current = -1;
		private long position, count;
		private boolean closed, bound;

		@Override
		public void bind(long reference, long count) {
			checkReader();
			validate(reference, count);
			head = table.heads[(int) reference];
			next = head;
			current = -1;
			position = 0L;
			this.count = count;
			bound = true;
		}

		@Override
		public boolean nextTuple() {
			checkReader();
			if (!bound)
				throw new IllegalStateException("unbound hash group reader");
			if (position == count) {
				current = -1;
				return false;
			}
			if (next < 0)
				throw new IllegalStateException("truncated hash group");
			current = next;
			next = table.next[current];
			position++;
			return true;
		}

		@Override
		public long[] rowArray() {
			return table.payloads;
		}

		@Override
		public int rowOffset() {
			return current * columns();
		}

		@Override
		public long tupleWeight() {
			return 1L;
		}

		@Override
		public int copyRows(long offset, int maximum, long[] rows, long[] weights) {
			checkReader();
			if (!bound)
				throw new IllegalStateException("unbound hash group reader");
			if (offset < 0 || offset > count || maximum < 0)
				throw new IllegalArgumentException("invalid group range");
			int n = (int) Math.min(maximum, count - offset);
			int width = columns();
			Objects.checkFromIndexSize(0, Math.multiplyExact(n, width), rows.length);
			Objects.checkFromIndexSize(0, n, weights.length);
			current = -1;
			if (n == 0)
				return 0;
			int[] links = table.next;
			if (offset < position) {
				next = head;
				position = 0L;
			}
			while (position < offset) {
				if (next < 0)
					throw new IllegalStateException("truncated hash group");
				next = links[next];
				position++;
			}
			long[] payload = table.payloads;
			int at = next;
			for (int i = 0; i < n; i++) {
				if (at < 0)
					throw new IllegalStateException("truncated hash group");
				System.arraycopy(payload, at * width, rows, i * width, width);
				at = links[at];
			}
			Arrays.fill(weights, 0, n, 1L);
			next = at;
			position = offset + n;
			return n;
		}

		private void checkReader() {
			checkOpen();
			if (closed)
				throw new IllegalStateException("hash group reader closed");
		}

		@Override
		public void close() {
			closed = true;
			head = next = current = -1;
		}
	}
}
