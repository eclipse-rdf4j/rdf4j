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

import java.io.File;
import java.util.Random;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Low-level tests for {@link TripleStore}.
 */
public class CardinalityTest {
	final static Logger logger = LoggerFactory.getLogger(CardinalityTest.class);

	@TempDir
	File tempFolder;

	protected TripleStore tripleStore;

	@BeforeEach
	public void before() throws Exception {
		File dataDir = new File(tempFolder, "triplestore");
		dataDir.mkdir();
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"));
	}

	int count(RecordIterator it) {
		int count = 0;
		while (it.next() != null) {
			count++;
		}
		return count;
	}

	@Test
	public void testCardinalities() throws Exception {
		Random random = new Random();

		int nrOfResources = 1000;

		tripleStore.startTransaction();
		for (int resource = 1; resource <= nrOfResources; resource++) {
			int nrOfTriples = 20 + random.nextInt(100);
			for (int i = 0; i < nrOfTriples; i++) {
				tripleStore.storeTriple(resource, 2, random.nextInt(100000) * (long) Math.pow(10, random.nextInt(4)), 1,
						true);
				tripleStore.storeTriple(resource, 2 + random.nextInt(1000) + 1,
						random.nextInt(100000) * (long) Math.pow(10, random.nextInt(4)), 1, true);
			}
		}
		tripleStore.commit();

		try (Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			logger.info(tripleStore.cardinality(LmdbValue.UNKNOWN_ID, 2, LmdbValue.UNKNOWN_ID, 1) + " --- " +
					count(tripleStore.getTriples(txn, LmdbValue.UNKNOWN_ID, 2, LmdbValue.UNKNOWN_ID, 1, true)));

			for (int resource = 1; resource <= nrOfResources; resource++) {
				logger.info(tripleStore.cardinality(resource, 2, LmdbValue.UNKNOWN_ID, 1) + " --- " +
						count(tripleStore.getTriples(txn, resource, 2, LmdbValue.UNKNOWN_ID, 1, true)));
			}

			logger.info(tripleStore.cardinality(LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, 3, 1) + " --- " +
					count(tripleStore.getTriples(txn, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, 3, 1, true)));
		}
	}

	@AfterEach
	public void after() throws Exception {
		tripleStore.close();
	}
}
