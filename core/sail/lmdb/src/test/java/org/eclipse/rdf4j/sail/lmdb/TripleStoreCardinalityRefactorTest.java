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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TripleStoreCardinalityRefactorTest {

	private static final long PRED_A = 100;
	private static final long PRED_B = 200;
	private static final long CONTEXT_ONE = 10;
	private static final long CONTEXT_TWO = 20;

	@TempDir
	File tempFolder;

	private TripleStore tripleStore;
	private int explicitStatements;

	@BeforeEach
	void setUp() throws Exception {
		File dataDir = new File(tempFolder, "triplestore");
		dataDir.mkdir();
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"), null);
		populateData();
	}

	@AfterEach
	void tearDown() throws Exception {
		tripleStore.close();
	}

	@Test
	void predicateAndContextMatchIteratorCounts() throws Exception {
		assertEstimatorBounds(LmdbValue.UNKNOWN_ID, PRED_A, LmdbValue.UNKNOWN_ID, CONTEXT_ONE);
	}

	@Test
	void subjectAndPredicateMatchIteratorCounts() throws Exception {
		assertEstimatorBounds(1, PRED_B, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID);
	}

	@Test
	void fullScanMatchesTotalTripleCount() throws Exception {
		double estimate = tripleStore.cardinality(LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID,
				LmdbValue.UNKNOWN_ID);
		assertEquals(explicitStatements, estimate, 0.0);
	}

	@Test
	void moreSpecificPatternDoesNotExceedBroaderEstimate() throws Exception {
		double general = tripleStore.cardinality(LmdbValue.UNKNOWN_ID, PRED_A, LmdbValue.UNKNOWN_ID, CONTEXT_ONE);
		double specific = tripleStore.cardinality(1, PRED_A, LmdbValue.UNKNOWN_ID, CONTEXT_ONE);
		assertTrue(specific <= general);
	}

	private void populateData() throws IOException {
		tripleStore.startTransaction();
		explicitStatements = 0;
		for (long subj = 1; subj <= 3; subj++) {
			for (int i = 0; i < 120; i++) {
				long pred = (i % 2 == 0) ? PRED_A : PRED_B;
				long obj = subj * 1000 + i;
				long context = (i % 3 == 0) ? CONTEXT_ONE : CONTEXT_TWO;
				storeTriple(subj, pred, obj, context);
			}
		}
		tripleStore.commit();
	}

	private void storeTriple(long subj, long pred, long obj, long context) throws IOException {
		tripleStore.storeTriple(subj, pred, obj, context, true);
		explicitStatements++;
	}

	private void assertEstimatorBounds(long subj, long pred, long obj, long context) throws Exception {
		double estimate = tripleStore.cardinality(subj, pred, obj, context);
		int expected = count(subj, pred, obj, context, true) + count(subj, pred, obj, context, false);
		assertTrue(estimate >= expected,
				() -> "Estimator should not undercount actual matches for pattern: " + patternLabel(subj, pred, obj,
						context) + " expected=" + expected + " actual=" + estimate);
		assertTrue(estimate <= explicitStatements,
				() -> "Estimator should not exceed total explicit statements in dataset for pattern: "
						+ patternLabel(subj, pred, obj, context));
	}

	private int count(long subj, long pred, long obj, long context, boolean explicit) throws Exception {
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iterator = tripleStore.getTriples(txn, subj, pred, obj, context, explicit)) {
			return count(iterator);
		}
	}

	private int count(RecordIterator iterator) throws IOException {
		int count = 0;
		while (iterator.next() != null) {
			count++;
		}
		return count;
	}

	private String patternLabel(long subj, long pred, long obj, long context) {
		return "subj=" + subj + ", pred=" + pred + ", obj=" + obj + ", context=" + context;
	}
}
