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

import java.io.File;
import java.io.IOException;

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

	@BeforeEach
	public void before(@TempDir File dataDir) throws Exception {
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"));
	}

	int count(RecordIterator it) throws IOException {
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

	@AfterEach
	public void after() throws Exception {
		tripleStore.close();
	}
}
