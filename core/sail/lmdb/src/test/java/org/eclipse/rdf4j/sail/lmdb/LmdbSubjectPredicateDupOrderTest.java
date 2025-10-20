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
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that the subject–predicate dup index returns duplicate values in ascending numeric order for object/context
 * ids. This guards against little-endian encoding of dup values, which breaks LMDB's lexicographic ordering and thus
 * ORDER BY semantics.
 */
public class LmdbSubjectPredicateDupOrderTest {

	private TripleStore tripleStore;

	@BeforeEach
	public void setup(@TempDir File dataDir) throws Exception {
		// Ensure dupsort indices are enabled (default true) and pick a simple index set
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"), null);
	}

	@AfterEach
	public void teardown() throws Exception {
		if (tripleStore != null) {
			tripleStore.close();
		}
	}

	@Test
	public void subjectPredicateDuplicateValuesOrderedByObjectAscending() throws Exception {
		long s = 100L;
		long p = 200L;
		long c = 1L;

		// Object ids specifically chosen to expose little-endian vs big-endian lexicographic ordering
		long[] objects = new long[] { 1L, 256L, 3L, 255L, 2L };

		tripleStore.startTransaction();
		for (long o : objects) {
			tripleStore.storeTriple(s, p, o, c, true);
		}
		tripleStore.commit();

		List<Long> observed = new ArrayList<>();

		try (TxnManager.Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator it = tripleStore.getTriples(txn, s, p, -1, -1, true)) {
			long[] quad;
			while ((quad = it.next()) != null) {
				// Ensure we only consider the (s,p,?,?) pattern we inserted
				if (quad[TripleStore.SUBJ_IDX] == s && quad[TripleStore.PRED_IDX] == p) {
					observed.add(quad[TripleStore.OBJ_IDX]);
				}
			}
		}

		long[] expectedOrder = new long[] { 1L, 2L, 3L, 255L, 256L };
		long[] actualOrder = observed.stream().mapToLong(Long::longValue).toArray();

		// If dup values are little-endian, LMDB will sort lexicographically and return 256 before 1.
		// We assert strictly ascending numeric order by object id.
		assertArrayEquals(expectedOrder, actualOrder, "Objects must be returned in ascending order by id");
	}
}
