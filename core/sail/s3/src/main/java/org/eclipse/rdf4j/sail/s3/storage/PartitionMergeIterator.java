/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.storage;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/**
 * K-way merge iterator for within-partition queries. Works with 3-varint keys (subject, object, context encoded in
 * partition sort order) where the predicate is implicit in the partition directory.
 *
 * <p>
 * Sources are ordered newest-to-oldest. Deduplicates entries with the same key (newest wins), suppresses tombstones,
 * and filters by expected flag.
 * </p>
 */
public class PartitionMergeIterator implements Iterator<long[]> {

	private final long predicateId;
	private final String sortOrder;
	private final byte expectedFlag;
	private final long patternS, patternO, patternC;
	private final PriorityQueue<SourceCursor> heap;
	private long[] next;

	/**
	 * @param sources      list of sources ordered newest-to-oldest (index 0 = newest)
	 * @param predicateId  the predicate ID for this partition (injected into results)
	 * @param sortOrder    the sort order of all sources ("soc", "osc", or "cso")
	 * @param expectedFlag the flag to match (FLAG_EXPLICIT or FLAG_INFERRED)
	 * @param s            subject pattern, or -1 for wildcard
	 * @param o            object pattern, or -1 for wildcard
	 * @param c            context pattern, or -1 for wildcard
	 */
	public PartitionMergeIterator(List<RawEntrySource> sources, long predicateId, String sortOrder,
			byte expectedFlag, long s, long o, long c) {
		this.predicateId = predicateId;
		this.sortOrder = sortOrder;
		this.expectedFlag = expectedFlag;
		this.patternS = s;
		this.patternO = o;
		this.patternC = c;
		this.heap = new PriorityQueue<>();

		for (int i = 0; i < sources.size(); i++) {
			RawEntrySource src = sources.get(i);
			if (src.hasNext()) {
				heap.add(new SourceCursor(src, i));
			}
		}

		advance();
	}

	private void advance() {
		next = null;
		while (!heap.isEmpty()) {
			// Pop minimum key
			SourceCursor min = heap.poll();
			byte[] winningKey = min.source.peekKey().clone();
			byte winningFlag = min.source.peekFlag();

			// Advance the winning source
			min.source.advance();
			if (min.source.hasNext()) {
				heap.add(min);
			}

			// Drain all sources with the same key (deduplication)
			while (!heap.isEmpty() && Arrays.compareUnsigned(heap.peek().source.peekKey(), winningKey) == 0) {
				SourceCursor dup = heap.poll();
				dup.source.advance();
				if (dup.source.hasNext()) {
					heap.add(dup);
				}
			}

			// Tombstone suppression
			if (winningFlag == MemTable.FLAG_TOMBSTONE) {
				continue;
			}

			// Flag filter
			if (winningFlag != expectedFlag) {
				continue;
			}

			// Decode 3-varint key to (subject, object, context) based on sort order
			long[] quad = decodePartitionKey(winningKey, sortOrder, predicateId);

			// Pattern filter
			if ((patternS >= 0 && quad[QuadIndex.SUBJ_IDX] != patternS)
					|| (patternO >= 0 && quad[QuadIndex.OBJ_IDX] != patternO)
					|| (patternC >= 0 && quad[QuadIndex.CONTEXT_IDX] != patternC)) {
				continue;
			}

			next = quad;
			return;
		}
	}

	/**
	 * Decodes a 3-varint partition key into a full SPOC quad array.
	 */
	static long[] decodePartitionKey(byte[] key, String sortOrder, long predicateId) {
		ByteBuffer bb = ByteBuffer.wrap(key);
		long v1 = Varint.readUnsigned(bb);
		long v2 = Varint.readUnsigned(bb);
		long v3 = Varint.readUnsigned(bb);

		long[] quad = new long[4];
		quad[QuadIndex.PRED_IDX] = predicateId;

		switch (sortOrder) {
		case "osc":
			quad[QuadIndex.OBJ_IDX] = v1;
			quad[QuadIndex.SUBJ_IDX] = v2;
			quad[QuadIndex.CONTEXT_IDX] = v3;
			break;
		case "cso":
			quad[QuadIndex.CONTEXT_IDX] = v1;
			quad[QuadIndex.SUBJ_IDX] = v2;
			quad[QuadIndex.OBJ_IDX] = v3;
			break;
		case "soc":
		default:
			quad[QuadIndex.SUBJ_IDX] = v1;
			quad[QuadIndex.OBJ_IDX] = v2;
			quad[QuadIndex.CONTEXT_IDX] = v3;
			break;
		}

		return quad;
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public long[] next() {
		if (next == null) {
			throw new NoSuchElementException();
		}
		long[] result = next;
		advance();
		return result;
	}

	private static class SourceCursor implements Comparable<SourceCursor> {
		final RawEntrySource source;
		final int sourceIndex; // lower = newer

		SourceCursor(RawEntrySource source, int sourceIndex) {
			this.source = source;
			this.sourceIndex = sourceIndex;
		}

		@Override
		public int compareTo(SourceCursor other) {
			int keyCmp = Arrays.compareUnsigned(this.source.peekKey(), other.source.peekKey());
			if (keyCmp != 0) {
				return keyCmp;
			}
			// Ties broken by source index: lower = newer = wins (poll first)
			return Integer.compare(this.sourceIndex, other.sourceIndex);
		}
	}
}
