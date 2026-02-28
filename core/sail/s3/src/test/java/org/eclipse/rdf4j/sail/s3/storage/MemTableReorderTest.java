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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MemTable#asRawSource(QuadIndex, long, long, long, long)} — re-encoding keys from native SPOC order
 * into a different target index order.
 */
class MemTableReorderTest {

	private final QuadIndex spoc = new QuadIndex("spoc");
	private final QuadIndex opsc = new QuadIndex("opsc");
	private final QuadIndex cspo = new QuadIndex("cspo");

	@Test
	void sameIndex_delegatesToNativeSource() {
		MemTable mt = new MemTable(spoc);
		mt.put(10, 20, 30, 40, true);
		mt.put(1, 2, 3, 4, true);

		RawEntrySource source = mt.asRawSource(spoc, -1, -1, -1, -1);
		List<long[]> results = drain(source, spoc);

		assertEquals(2, results.size());
		// SPOC order: (1,2,3,4) before (10,20,30,40)
		assertArrayEquals(new long[] { 1, 2, 3, 4 }, results.get(0));
		assertArrayEquals(new long[] { 10, 20, 30, 40 }, results.get(1));
	}

	@Test
	void reorderToOPSC_sortsByObjectFirst() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 2, 30, 4, true); // object=30
		mt.put(5, 6, 10, 8, true); // object=10
		mt.put(9, 10, 20, 12, true); // object=20

		RawEntrySource source = mt.asRawSource(opsc, -1, -1, -1, -1);
		List<long[]> results = drain(source, opsc);

		assertEquals(3, results.size());
		// OPSC order: sorted by object: 10, 20, 30
		assertEquals(10, results.get(0)[QuadIndex.OBJ_IDX]);
		assertEquals(20, results.get(1)[QuadIndex.OBJ_IDX]);
		assertEquals(30, results.get(2)[QuadIndex.OBJ_IDX]);
	}

	@Test
	void reorderToCSPO_sortsByContextFirst() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 2, 3, 30, true); // context=30
		mt.put(4, 5, 6, 10, true); // context=10
		mt.put(7, 8, 9, 20, true); // context=20

		RawEntrySource source = mt.asRawSource(cspo, -1, -1, -1, -1);
		List<long[]> results = drain(source, cspo);

		assertEquals(3, results.size());
		// CSPO order: sorted by context: 10, 20, 30
		assertEquals(10, results.get(0)[QuadIndex.CONTEXT_IDX]);
		assertEquals(20, results.get(1)[QuadIndex.CONTEXT_IDX]);
		assertEquals(30, results.get(2)[QuadIndex.CONTEXT_IDX]);
	}

	@Test
	void reorderedSource_preservesAllComponents() {
		MemTable mt = new MemTable(spoc);
		mt.put(11, 22, 33, 44, true);

		RawEntrySource source = mt.asRawSource(opsc, -1, -1, -1, -1);
		List<long[]> results = drain(source, opsc);

		assertEquals(1, results.size());
		assertEquals(11, results.get(0)[QuadIndex.SUBJ_IDX]);
		assertEquals(22, results.get(0)[QuadIndex.PRED_IDX]);
		assertEquals(33, results.get(0)[QuadIndex.OBJ_IDX]);
		assertEquals(44, results.get(0)[QuadIndex.CONTEXT_IDX]);
	}

	@Test
	void reorderedSource_appliesSubjectFilter() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 2, 3, 0, true);
		mt.put(5, 6, 7, 0, true);

		RawEntrySource source = mt.asRawSource(opsc, 1, -1, -1, -1);
		List<long[]> results = drain(source, opsc);

		assertEquals(1, results.size());
		assertEquals(1, results.get(0)[QuadIndex.SUBJ_IDX]);
	}

	@Test
	void reorderedSource_appliesPredicateFilter() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 10, 3, 0, true);
		mt.put(2, 20, 4, 0, true);
		mt.put(3, 10, 5, 0, true);

		RawEntrySource source = mt.asRawSource(cspo, -1, 10, -1, -1);
		List<long[]> results = drain(source, cspo);

		assertEquals(2, results.size());
		for (long[] q : results) {
			assertEquals(10, q[QuadIndex.PRED_IDX]);
		}
	}

	@Test
	void reorderedSource_includesAliveAndTombstones() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 2, 3, 0, true);
		mt.remove(5, 6, 7, 0);

		RawEntrySource source = mt.asRawSource(opsc, -1, -1, -1, -1);

		int count = 0;
		boolean foundTombstone = false;
		while (source.hasNext()) {
			if (source.peekFlag() == MemTable.FLAG_TOMBSTONE) {
				foundTombstone = true;
			}
			source.advance();
			count++;
		}

		assertEquals(2, count);
		assertTrue(foundTombstone, "RawEntrySource should include tombstones");
	}

	@Test
	void reorderedSource_emptyTable_returnsEmpty() {
		MemTable mt = new MemTable(spoc);

		RawEntrySource source = mt.asRawSource(opsc, -1, -1, -1, -1);
		assertFalse(source.hasNext());
	}

	private List<long[]> drain(RawEntrySource source, QuadIndex decodeIndex) {
		List<long[]> result = new ArrayList<>();
		while (source.hasNext()) {
			long[] quad = new long[4];
			decodeIndex.keyToQuad(source.peekKey(), quad);
			result.add(quad);
			source.advance();
		}
		return result;
	}
}
