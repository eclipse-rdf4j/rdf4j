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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

class SSTableWriterReaderTest {

	private final QuadIndex spoc = new QuadIndex("spoc");

	@Test
	void roundTrip_singleEntry() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 2, 3, 4, true);

		byte[] sstData = SSTableWriter.write(mt);
		SSTable sst = new SSTable(sstData, spoc);

		assertEquals(1, sst.getEntryCount());

		Iterator<long[]> iter = sst.scan(1, 2, 3, 4, true);
		assertTrue(iter.hasNext());
		long[] quad = iter.next();
		assertArrayEquals(new long[] { 1, 2, 3, 4 }, quad);
		assertFalse(iter.hasNext());
	}

	@Test
	void roundTrip_multipleEntries() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 2, 3, 0, true);
		mt.put(1, 2, 4, 0, true);
		mt.put(2, 3, 4, 0, true);
		mt.put(10, 20, 30, 40, true);

		byte[] sstData = SSTableWriter.write(mt);
		SSTable sst = new SSTable(sstData, spoc);

		assertEquals(4, sst.getEntryCount());

		// Wildcard scan
		List<long[]> results = toList(sst.scan(-1, -1, -1, -1, true));
		assertEquals(4, results.size());
	}

	@Test
	void roundTrip_patternFilter() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 2, 3, 0, true);
		mt.put(1, 2, 4, 0, true);
		mt.put(2, 3, 4, 0, true);

		byte[] sstData = SSTableWriter.write(mt);
		SSTable sst = new SSTable(sstData, spoc);

		// Filter by subject=1
		List<long[]> results = toList(sst.scan(1, -1, -1, -1, true));
		assertEquals(2, results.size());
		assertEquals(1, results.get(0)[0]);
		assertEquals(1, results.get(1)[0]);

		// Filter by subject=2
		results = toList(sst.scan(2, -1, -1, -1, true));
		assertEquals(1, results.size());
		assertEquals(2, results.get(0)[0]);
	}

	@Test
	void roundTrip_tombstonesFilteredInScan() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 2, 3, 0, true);
		mt.put(1, 2, 4, 0, true);
		mt.remove(1, 2, 3, 0, true); // tombstone

		byte[] sstData = SSTableWriter.write(mt);
		SSTable sst = new SSTable(sstData, spoc);

		// Tombstone entry is still in the SSTable (entryCount includes it)
		assertEquals(2, sst.getEntryCount());

		// But scan filters it out
		List<long[]> results = toList(sst.scan(-1, -1, -1, -1, true));
		assertEquals(1, results.size());
		assertArrayEquals(new long[] { 1, 2, 4, 0 }, results.get(0));
	}

	@Test
	void roundTrip_tombstonesVisibleInRawSource() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 2, 3, 0, true);
		mt.remove(1, 2, 3, 0, true);

		byte[] sstData = SSTableWriter.write(mt);
		SSTable sst = new SSTable(sstData, spoc);

		RawEntrySource source = sst.asRawSource(-1, -1, -1, -1);
		assertTrue(source.hasNext());
		// The tombstone should be visible
		assertEquals(MemTable.FLAG_TOMBSTONE, source.peekFlag());
	}

	@Test
	void roundTrip_explicitVsInferred() {
		MemTable mt = new MemTable(spoc);
		mt.put(1, 2, 3, 0, true); // explicit
		mt.put(4, 5, 6, 0, false); // inferred

		byte[] sstData = SSTableWriter.write(mt);
		SSTable sst = new SSTable(sstData, spoc);

		List<long[]> explicitResults = toList(sst.scan(-1, -1, -1, -1, true));
		assertEquals(1, explicitResults.size());
		assertArrayEquals(new long[] { 1, 2, 3, 0 }, explicitResults.get(0));

		List<long[]> inferredResults = toList(sst.scan(-1, -1, -1, -1, false));
		assertEquals(1, inferredResults.size());
		assertArrayEquals(new long[] { 4, 5, 6, 0 }, inferredResults.get(0));
	}

	@Test
	void roundTrip_smallBlockSize() {
		// Use a very small block size to test multi-block SSTables
		MemTable mt = new MemTable(spoc);
		for (long i = 1; i <= 100; i++) {
			mt.put(i, i + 1, i + 2, 0, true);
		}

		byte[] sstData = SSTableWriter.write(mt, 64); // tiny blocks
		SSTable sst = new SSTable(sstData, spoc);

		assertEquals(100, sst.getEntryCount());

		// Verify all entries are retrievable
		List<long[]> results = toList(sst.scan(-1, -1, -1, -1, true));
		assertEquals(100, results.size());

		// Verify range scan with block index seeking
		results = toList(sst.scan(50, -1, -1, -1, true));
		assertEquals(1, results.size());
		assertEquals(50, results.get(0)[0]);
	}

	@Test
	void roundTrip_largeIds() {
		MemTable mt = new MemTable(spoc);
		mt.put(100000, 200000, 300000, 400000, true);

		byte[] sstData = SSTableWriter.write(mt);
		SSTable sst = new SSTable(sstData, spoc);

		List<long[]> results = toList(sst.scan(-1, -1, -1, -1, true));
		assertEquals(1, results.size());
		assertArrayEquals(new long[] { 100000, 200000, 300000, 400000 }, results.get(0));
	}

	@Test
	void emptyMemTable_throwsException() {
		MemTable mt = new MemTable(spoc);
		assertThrows(IllegalArgumentException.class, () -> SSTableWriter.write(mt));
	}

	private List<long[]> toList(Iterator<long[]> iter) {
		List<long[]> list = new ArrayList<>();
		while (iter.hasNext()) {
			list.add(iter.next());
		}
		return list;
	}
}
