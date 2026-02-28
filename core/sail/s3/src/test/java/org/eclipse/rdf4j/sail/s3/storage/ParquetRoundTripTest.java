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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for Parquet write/read round-trips using {@link ParquetFileBuilder} and {@link ParquetQuadSource} with the
 * {@link ParquetSchemas#QUAD_SCHEMA} (5 columns, 4-varint keys).
 */
class ParquetRoundTripTest {

	@Test
	void roundTrip_spocOrder_allFieldsPreserved() {
		QuadIndex spoc = new QuadIndex("spoc");
		List<QuadEntry> entries = List.of(
				new QuadEntry(1, 2, 3, 4, MemTable.FLAG_EXPLICIT),
				new QuadEntry(5, 6, 7, 8, MemTable.FLAG_INFERRED),
				new QuadEntry(9, 10, 11, 0, MemTable.FLAG_TOMBSTONE));

		byte[] parquetData = ParquetFileBuilder.build(entries, ParquetSchemas.SortOrder.SPOC);
		ParquetQuadSource source = new ParquetQuadSource(parquetData, spoc);

		List<long[]> results = drainWithFlags(source, spoc);
		assertEquals(3, results.size());

		// First entry: (1,2,3,4) FLAG_EXPLICIT
		assertArrayEquals(new long[] { 1, 2, 3, 4, MemTable.FLAG_EXPLICIT }, results.get(0));
		// Second entry: (5,6,7,8) FLAG_INFERRED
		assertArrayEquals(new long[] { 5, 6, 7, 8, MemTable.FLAG_INFERRED }, results.get(1));
		// Third entry: (9,10,11,0) FLAG_TOMBSTONE
		assertArrayEquals(new long[] { 9, 10, 11, 0, MemTable.FLAG_TOMBSTONE }, results.get(2));
	}

	@Test
	void roundTrip_opscOrder_keysSortedByObject() {
		QuadIndex opsc = new QuadIndex("opsc");
		// Written sorted in OPSC order (by object: 10, 20, 30)
		List<QuadEntry> entries = List.of(
				new QuadEntry(100, 200, 10, 0, MemTable.FLAG_EXPLICIT),
				new QuadEntry(300, 400, 20, 0, MemTable.FLAG_EXPLICIT),
				new QuadEntry(500, 600, 30, 0, MemTable.FLAG_EXPLICIT));

		byte[] parquetData = ParquetFileBuilder.build(entries, ParquetSchemas.SortOrder.OPSC);
		ParquetQuadSource source = new ParquetQuadSource(parquetData, opsc);

		List<long[]> results = drain(source, opsc);
		assertEquals(3, results.size());
		// Keys should be in OPSC order (object first)
		assertEquals(10, results.get(0)[QuadIndex.OBJ_IDX]);
		assertEquals(20, results.get(1)[QuadIndex.OBJ_IDX]);
		assertEquals(30, results.get(2)[QuadIndex.OBJ_IDX]);
	}

	@Test
	void roundTrip_cspoOrder_keysSortedByContext() {
		QuadIndex cspo = new QuadIndex("cspo");
		// Written sorted in CSPO order (by context: 5, 10, 15)
		List<QuadEntry> entries = List.of(
				new QuadEntry(1, 2, 3, 5, MemTable.FLAG_EXPLICIT),
				new QuadEntry(4, 5, 6, 10, MemTable.FLAG_EXPLICIT),
				new QuadEntry(7, 8, 9, 15, MemTable.FLAG_EXPLICIT));

		byte[] parquetData = ParquetFileBuilder.build(entries, ParquetSchemas.SortOrder.CSPO);
		ParquetQuadSource source = new ParquetQuadSource(parquetData, cspo);

		List<long[]> results = drain(source, cspo);
		assertEquals(3, results.size());
		assertEquals(5, results.get(0)[QuadIndex.CONTEXT_IDX]);
		assertEquals(10, results.get(1)[QuadIndex.CONTEXT_IDX]);
		assertEquals(15, results.get(2)[QuadIndex.CONTEXT_IDX]);
	}

	@Test
	void roundTrip_filterBySubject() {
		QuadIndex spoc = new QuadIndex("spoc");
		List<QuadEntry> entries = List.of(
				new QuadEntry(1, 2, 3, 0, MemTable.FLAG_EXPLICIT),
				new QuadEntry(5, 6, 7, 0, MemTable.FLAG_EXPLICIT),
				new QuadEntry(10, 11, 12, 0, MemTable.FLAG_EXPLICIT));

		byte[] parquetData = ParquetFileBuilder.build(entries, ParquetSchemas.SortOrder.SPOC);
		ParquetQuadSource source = new ParquetQuadSource(parquetData, spoc, 5, -1, -1, -1);

		List<long[]> results = drain(source, spoc);
		assertEquals(1, results.size());
		assertEquals(5, results.get(0)[QuadIndex.SUBJ_IDX]);
	}

	@Test
	void roundTrip_filterByPredicate() {
		QuadIndex spoc = new QuadIndex("spoc");
		List<QuadEntry> entries = List.of(
				new QuadEntry(1, 10, 3, 0, MemTable.FLAG_EXPLICIT),
				new QuadEntry(2, 20, 4, 0, MemTable.FLAG_EXPLICIT),
				new QuadEntry(3, 10, 5, 0, MemTable.FLAG_EXPLICIT));

		byte[] parquetData = ParquetFileBuilder.build(entries, ParquetSchemas.SortOrder.SPOC);
		ParquetQuadSource source = new ParquetQuadSource(parquetData, spoc, -1, 10, -1, -1);

		List<long[]> results = drain(source, spoc);
		assertEquals(2, results.size());
		for (long[] q : results) {
			assertEquals(10, q[QuadIndex.PRED_IDX]);
		}
	}

	@Test
	void roundTrip_filterByMultipleComponents() {
		QuadIndex spoc = new QuadIndex("spoc");
		List<QuadEntry> entries = List.of(
				new QuadEntry(1, 2, 3, 4, MemTable.FLAG_EXPLICIT),
				new QuadEntry(1, 2, 99, 4, MemTable.FLAG_EXPLICIT),
				new QuadEntry(1, 99, 3, 4, MemTable.FLAG_EXPLICIT));

		byte[] parquetData = ParquetFileBuilder.build(entries, ParquetSchemas.SortOrder.SPOC);
		ParquetQuadSource source = new ParquetQuadSource(parquetData, spoc, 1, 2, 3, 4);

		List<long[]> results = drain(source, spoc);
		assertEquals(1, results.size());
		assertArrayEquals(new long[] { 1, 2, 3, 4 }, results.get(0));
	}

	@Test
	void roundTrip_emptyFile() {
		QuadIndex spoc = new QuadIndex("spoc");
		byte[] parquetData = ParquetFileBuilder.build(List.of(), ParquetSchemas.SortOrder.SPOC);
		ParquetQuadSource source = new ParquetQuadSource(parquetData, spoc);
		assertFalse(source.hasNext());
	}

	@Test
	void mergeIterator_acrossParquetSources() {
		QuadIndex spoc = new QuadIndex("spoc");

		// File 1: newer epoch
		List<QuadEntry> file1 = List.of(
				new QuadEntry(1, 2, 3, 0, MemTable.FLAG_EXPLICIT),
				new QuadEntry(5, 6, 7, 0, MemTable.FLAG_EXPLICIT));
		byte[] data1 = ParquetFileBuilder.build(file1, ParquetSchemas.SortOrder.SPOC);

		// File 2: older epoch, overlaps on (1,2,3,0)
		List<QuadEntry> file2 = List.of(
				new QuadEntry(1, 2, 3, 0, MemTable.FLAG_INFERRED),
				new QuadEntry(10, 11, 12, 0, MemTable.FLAG_EXPLICIT));
		byte[] data2 = ParquetFileBuilder.build(file2, ParquetSchemas.SortOrder.SPOC);

		List<RawEntrySource> sources = List.of(
				new ParquetQuadSource(data1, spoc),
				new ParquetQuadSource(data2, spoc));

		MergeIterator iter = new MergeIterator(sources, spoc, MemTable.FLAG_EXPLICIT, -1, -1, -1, -1);
		List<long[]> results = new ArrayList<>();
		while (iter.hasNext()) {
			results.add(iter.next());
		}

		// (1,2,3,0) from newer file is explicit → included
		// (5,6,7,0) explicit → included
		// (10,11,12,0) explicit → included
		assertEquals(3, results.size());
	}

	private List<long[]> drain(ParquetQuadSource source, QuadIndex decodeIndex) {
		List<long[]> result = new ArrayList<>();
		while (source.hasNext()) {
			long[] quad = new long[4];
			decodeIndex.keyToQuad(source.peekKey(), quad);
			result.add(quad);
			source.advance();
		}
		return result;
	}

	private List<long[]> drainWithFlags(ParquetQuadSource source, QuadIndex decodeIndex) {
		List<long[]> result = new ArrayList<>();
		while (source.hasNext()) {
			long[] quad = new long[4];
			decodeIndex.keyToQuad(source.peekKey(), quad);
			long[] withFlag = new long[] { quad[0], quad[1], quad[2], quad[3], source.peekFlag() };
			result.add(withFlag);
			source.advance();
		}
		return result;
	}
}
