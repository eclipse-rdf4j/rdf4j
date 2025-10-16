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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubjectPredicateIndexTest {

	@TempDir
	File dataDir;

	private TripleStore tripleStore;

	@BeforeEach
	void setUp() throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("posc");
		config.setDupsortIndices(true);
		config.setDupsortRead(true);
		tripleStore = new TripleStore(dataDir, config, null);

		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 3, 0, true);
		tripleStore.storeTriple(1, 2, 4, 0, true);
		tripleStore.commit();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (tripleStore != null) {
			tripleStore.close();
		}
	}

	@Test
	void subjectPredicatePatternUsesDupIterator() throws Exception {
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iter = tripleStore.getTriples(txn, 1, 2, -1, -1, true)) {
			assertThat(iter).isInstanceOf(LmdbDupRecordIterator.class);

			int count = 0;
			while (iter.next() != null) {
				count++;
			}
			assertEquals(2, count);
		}
	}

	@Test
	void predicateObjectPatternFallsBackToStandardIterator() throws Exception {
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iter = tripleStore.getTriples(txn, -1, 2, 3, -1, true)) {
			assertThat(iter).isNotInstanceOf(LmdbDupRecordIterator.class);

			int count = 0;
			while (iter.next() != null) {
				count++;
			}
			assertEquals(1, count);
		}
	}
}
