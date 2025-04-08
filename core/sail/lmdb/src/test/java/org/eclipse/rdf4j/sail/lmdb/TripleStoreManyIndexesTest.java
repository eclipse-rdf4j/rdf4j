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

import static org.junit.Assert.*;

import java.io.File;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test ensuring {@link TripleStore} can handle up to 6 indexes.
 */
public class TripleStoreManyIndexesTest {
	private File dataDir;

	@BeforeEach
	public void before(@TempDir File dataDir) throws Exception {
		this.dataDir = dataDir;
	}

	@Test
	public void testSixIndexes() throws Exception {
		TripleStore tripleStore = new TripleStore(dataDir,
				new LmdbStoreConfig("spoc,posc,ospc,cspo,cpos,cosp"));
		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 3, 1, true);
		tripleStore.commit();

		try (TxnManager.Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			var it = tripleStore.getTriples(txn, 1, 2, 3, 1, true);
			assertNotNull("Store should have a stored statement", it.next());
		}
	}
}
