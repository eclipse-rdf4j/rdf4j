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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

class MergeIteratorTest {

	private final QuadIndex spoc = new QuadIndex("spoc");

	@Test
	void newerSourceWins() {
		// Newer MemTable overrides older SSTable
		MemTable newer = new MemTable(spoc);
		newer.put(1, 2, 3, 0, true); // explicit in newer

		MemTable older = new MemTable(spoc);
		older.put(1, 2, 3, 0, false); // inferred in older

		List<RawEntrySource> sources = Arrays.asList(
				newer.asRawSource(-1, -1, -1, -1),
				older.asRawSource(-1, -1, -1, -1));

		MergeIterator iter = new MergeIterator(sources, spoc, MemTable.FLAG_EXPLICIT, -1, -1, -1, -1);
		List<long[]> results = toList(iter);
		assertEquals(1, results.size());
		assertArrayEquals(new long[] { 1, 2, 3, 0 }, results.get(0));
	}

	@Test
	void tombstoneSuppression() {
		MemTable newer = new MemTable(spoc);
		newer.remove(1, 2, 3, 0, true); // tombstone

		MemTable older = new MemTable(spoc);
		older.put(1, 2, 3, 0, true); // explicit

		List<RawEntrySource> sources = Arrays.asList(
				newer.asRawSource(-1, -1, -1, -1),
				older.asRawSource(-1, -1, -1, -1));

		MergeIterator iter = new MergeIterator(sources, spoc, MemTable.FLAG_EXPLICIT, -1, -1, -1, -1);
		List<long[]> results = toList(iter);
		assertEquals(0, results.size());
	}

	@Test
	void multiSourceMerge() {
		MemTable m1 = new MemTable(spoc);
		m1.put(1, 2, 3, 0, true);
		m1.put(3, 4, 5, 0, true);

		MemTable m2 = new MemTable(spoc);
		m2.put(2, 3, 4, 0, true);
		m2.put(4, 5, 6, 0, true);

		List<RawEntrySource> sources = Arrays.asList(
				m1.asRawSource(-1, -1, -1, -1),
				m2.asRawSource(-1, -1, -1, -1));

		MergeIterator iter = new MergeIterator(sources, spoc, MemTable.FLAG_EXPLICIT, -1, -1, -1, -1);
		List<long[]> results = toList(iter);
		assertEquals(4, results.size());
		// Should be sorted by key (SPOC order)
		assertEquals(1, results.get(0)[0]);
		assertEquals(2, results.get(1)[0]);
		assertEquals(3, results.get(2)[0]);
		assertEquals(4, results.get(3)[0]);
	}

	@Test
	void emptySource() {
		MemTable empty = new MemTable(spoc);
		MemTable withData = new MemTable(spoc);
		withData.put(1, 2, 3, 0, true);

		List<RawEntrySource> sources = Arrays.asList(
				empty.asRawSource(-1, -1, -1, -1),
				withData.asRawSource(-1, -1, -1, -1));

		MergeIterator iter = new MergeIterator(sources, spoc, MemTable.FLAG_EXPLICIT, -1, -1, -1, -1);
		List<long[]> results = toList(iter);
		assertEquals(1, results.size());
	}

	@Test
	void allEmptySources() {
		MemTable empty1 = new MemTable(spoc);
		MemTable empty2 = new MemTable(spoc);

		List<RawEntrySource> sources = Arrays.asList(
				empty1.asRawSource(-1, -1, -1, -1),
				empty2.asRawSource(-1, -1, -1, -1));

		MergeIterator iter = new MergeIterator(sources, spoc, MemTable.FLAG_EXPLICIT, -1, -1, -1, -1);
		assertFalse(iter.hasNext());
	}

	@Test
	void patternFilter() {
		MemTable m1 = new MemTable(spoc);
		m1.put(1, 2, 3, 0, true);
		m1.put(1, 2, 4, 0, true);
		m1.put(2, 3, 4, 0, true);

		List<RawEntrySource> sources = List.of(m1.asRawSource(1, -1, -1, -1));

		MergeIterator iter = new MergeIterator(sources, spoc, MemTable.FLAG_EXPLICIT, 1, -1, -1, -1);
		List<long[]> results = toList(iter);
		assertEquals(2, results.size());
	}

	@Test
	void mergeMemTableWithOlderSource() {
		// MemTable (newer) + older MemTable source
		MemTable memTable = new MemTable(spoc);
		memTable.put(1, 2, 3, 0, true);

		MemTable olderData = new MemTable(spoc);
		olderData.put(2, 3, 4, 0, true);
		olderData.put(4, 5, 6, 0, true);

		List<RawEntrySource> sources = Arrays.asList(
				memTable.asRawSource(-1, -1, -1, -1),
				olderData.asRawSource(-1, -1, -1, -1));

		MergeIterator iter = new MergeIterator(sources, spoc, MemTable.FLAG_EXPLICIT, -1, -1, -1, -1);
		List<long[]> results = toList(iter);
		assertEquals(3, results.size());
	}

	@Test
	void tombstoneInNewerShadowsOlder() {
		// Older source has a value, newer MemTable deletes it
		MemTable olderData = new MemTable(spoc);
		olderData.put(1, 2, 3, 0, true);
		olderData.put(4, 5, 6, 0, true);

		MemTable memTable = new MemTable(spoc);
		memTable.remove(1, 2, 3, 0, true); // tombstone shadows older entry

		List<RawEntrySource> sources = Arrays.asList(
				memTable.asRawSource(-1, -1, -1, -1),
				olderData.asRawSource(-1, -1, -1, -1));

		MergeIterator iter = new MergeIterator(sources, spoc, MemTable.FLAG_EXPLICIT, -1, -1, -1, -1);
		List<long[]> results = toList(iter);
		assertEquals(1, results.size());
		assertArrayEquals(new long[] { 4, 5, 6, 0 }, results.get(0));
	}

	private List<long[]> toList(Iterator<long[]> iter) {
		List<long[]> list = new ArrayList<>();
		while (iter.hasNext()) {
			list.add(iter.next());
		}
		return list;
	}
}
