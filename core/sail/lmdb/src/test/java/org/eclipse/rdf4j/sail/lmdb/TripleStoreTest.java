/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Low-level tests for {@link TripleStore}.
 */
public class TripleStoreTest {
	protected TripleStore tripleStore;
	private File dataDir;

	@BeforeEach
	public void before(@TempDir File dataDir) throws Exception {
		this.dataDir = dataDir;
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"), null);
	}

	int count(RecordIterator it) {
		int count = 0;
		while (it.next() != null) {
			count++;
		}
		return count;
	}

	@Test
	public void testInferredStmts() throws Exception {
		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 3, 1, false);
		tripleStore.commit();

		try (Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			assertEquals("Store should have 1 inferred statement", 1,
					count(tripleStore.getTriples(txn, 1, 2, 3, 1, false)));

			assertEquals("Store should have 0 explicit statements", 0,
					count(tripleStore.getTriples(txn, 1, 2, 3, 1, true)));

			tripleStore.startTransaction();
			tripleStore.storeTriple(1, 2, 3, 1, true);
			tripleStore.commit();

			assertEquals("Store should have 0 inferred statements", 0,
					count(tripleStore.getTriples(txn, 1, 2, 3, 1, false)));

			assertEquals("Store should have 1 explicit statements", 1,
					count(tripleStore.getTriples(txn, 1, 2, 3, 1, true)));
		}
	}

	@Test
	public void testExplicitPromotionRemovesSecondaryInferredRows() throws Exception {
		File secondaryIndexDir = new File(dataDir, "secondary-index-store");
		secondaryIndexDir.mkdirs();
		try (TripleStore secondaryIndexStore = new TripleStore(secondaryIndexDir, new LmdbStoreConfig("spoc,ospc,psoc"),
				null)) {
			secondaryIndexStore.startTransaction();
			secondaryIndexStore.storeTriple(11, 22, 33, 44, false);
			secondaryIndexStore.commit();

			secondaryIndexStore.startTransaction();
			secondaryIndexStore.storeTriple(11, 22, 33, 44, true);
			secondaryIndexStore.commit();

			try (Txn txn = secondaryIndexStore.getTxnManager().createReadTxn()) {
				assertEquals("PSOC inferred row should be removed", 0,
						count(secondaryIndexStore.getTriples(txn, -1, 22, -1, -1, false)));
				assertEquals("OSPC inferred row should be removed", 0,
						count(secondaryIndexStore.getTriples(txn, -1, -1, 33, -1, false)));
				assertEquals("PSOC explicit row should exist", 1,
						count(secondaryIndexStore.getTriples(txn, -1, 22, -1, -1, true)));
				assertEquals("OSPC explicit row should exist", 1,
						count(secondaryIndexStore.getTriples(txn, -1, -1, 33, -1, true)));
			}
		}
	}

	@Test
	public void testLeadingFieldSortPreservesPriorOrderWithinGroups() throws Exception {
		Method method = TripleStore.class.getDeclaredMethod("sortStatementIndicesByLeadingField", int[].class,
				int.class, char.class, long[].class, long[].class, long[].class, long[].class);
		method.setAccessible(true);

		int[] statementIndices = { 0, 1, 2, 3 };
		long[] subj = { 1, 2, 3, 4 };
		long[] pred = { 5, 5, 5, 5 };
		long[] obj = { 10, 20, 30, 40 };
		long[] context = { 100, 200, 300, 400 };

		method.invoke(tripleStore, statementIndices, statementIndices.length, 'p', subj, pred, obj, context);

		assertEquals("Stable leading-field sort should preserve prior order for equal leading values",
				Arrays.toString(new int[] { 0, 1, 2, 3 }), Arrays.toString(statementIndices));
	}

	@Test
	public void testShouldResetToMainIndexOrderWhenTrailingFieldsDiffer() throws Exception {
		Method method = TripleStore.class.getDeclaredMethod("shouldResetToMainIndexOrder", char[].class, char[].class,
				char[].class);
		method.setAccessible(true);

		assertFalse("SPOC -> PSOC should reuse the current order",
				(boolean) method.invoke(tripleStore, "spoc".toCharArray(), "spoc".toCharArray(), "psoc".toCharArray()));
		assertFalse("SPOC -> OSPC should reuse the current order",
				(boolean) method.invoke(tripleStore, "spoc".toCharArray(), "spoc".toCharArray(), "ospc".toCharArray()));
		assertFalse("PSOC -> OPSC should reuse the current order",
				(boolean) method.invoke(tripleStore, "spoc".toCharArray(), "psoc".toCharArray(), "opsc".toCharArray()));
		assertTrue("PSOC -> OSPC should reset to SPOC first",
				(boolean) method.invoke(tripleStore, "spoc".toCharArray(), "psoc".toCharArray(), "ospc".toCharArray()));
	}

	@Test
	public void testIndexesAreReorderedForAlignedBatchTraversal() throws Exception {
		File orderedIndexDir = new File(dataDir, "ordered-index-store");
		orderedIndexDir.mkdirs();

		try (TripleStore orderedIndexStore = new TripleStore(orderedIndexDir,
				new LmdbStoreConfig("spoc,psoc,sopc,opsc,posc,ospc"), null)) {
			Field indexesField = TripleStore.class.getDeclaredField("indexes");
			indexesField.setAccessible(true);

			@SuppressWarnings("unchecked")
			List<TripleStore.TripleIndex> configuredIndexes = (List<TripleStore.TripleIndex>) indexesField
					.get(orderedIndexStore);

			assertEquals(Arrays.asList("spoc", "ospc", "posc", "opsc", "sopc", "psoc"),
					configuredIndexes.stream()
							.map(index -> new String(index.getFieldSeq()))
							.collect(Collectors.toList()));
		}
	}

	@Test
	public void testGc() throws Exception {
		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 3, 1, true);
		tripleStore.storeTriple(1, 2, 4, 1, true);
		tripleStore.storeTriple(1, 2, 5, 1, true);
		tripleStore.storeTriple(1, 6, 7, 1, true);
		tripleStore.storeTriple(1, 6, 7, 8, true);
		Set<Long> removed = new HashSet<>();
		tripleStore.removeTriplesByContext(1, 6, -1, -1, true, quad -> {
			for (Long c : quad) {
				removed.add(c);
			}
		});
		tripleStore.commit();
		tripleStore.filterUsedIds(removed);
		assertEquals(Arrays.asList(6L, 7L, 8L), removed.stream().sorted().collect(Collectors.toList()));
	}

	@AfterEach
	public void after() throws Exception {
		try {
			tripleStore.close();
		} finally {
			LmdbTestUtil.deleteDir(dataDir);
		}
	}
}
