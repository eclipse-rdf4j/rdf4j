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

import java.io.File;
import java.util.Random;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CardinalityExactTest {
	private static final int NUM_RESOURCES = 1000;
	private static final int MIN_TRIPLES_PER_RESOURCE = 20;
	private static final int MAX_TRIPLES_PER_RESOURCE = 100;
	private final int[] contextIds = new int[] { 1, 2, 3 };
	private final int[] objectIds = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
	@TempDir
	File tempFolder;

	protected TripleStore tripleStore;

	@BeforeEach
	public void before() throws Exception {
		File dataDir = new File(tempFolder, "triplestore");
		dataDir.mkdir();
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"));
	}

	private long countTriples(RecordIterator iterator) {
		long count = 0;
		while (iterator.next() != null) {
			count++;
		}
		return count;
	}

	private long randomObjectId(Random random) {
		return objectIds[random.nextInt(objectIds.length)];
	}

	private long randomContextId(Random random) {
		return contextIds[random.nextInt(contextIds.length)];
	}

	@Test
	public void testCardinalityExact() throws Exception {
		Random random = new Random();

		tripleStore.startTransaction();

		for (int resourceId = 1; resourceId <= NUM_RESOURCES; resourceId++) {
			int tripleCount = MIN_TRIPLES_PER_RESOURCE + random.nextInt(MAX_TRIPLES_PER_RESOURCE);
			for (int i = 0; i < tripleCount; i++) {
				long objectId = randomObjectId(random);
				long randomContextId = randomContextId(random);
				tripleStore.storeTriple(resourceId, 2, objectId, randomContextId, true);

				int predicateId = 2 + random.nextInt(1000) + 1;
				tripleStore.storeTriple(resourceId, predicateId, randomObjectId(random), randomContextId, true);
			}
		}

		tripleStore.commit();

		try (TxnManager.Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			for (final long contextId : contextIds) {
				long actual = tripleStore.cardinalityExact(txn, LmdbValue.UNKNOWN_ID, 2, LmdbValue.UNKNOWN_ID,
						contextId, true);
				long expected = countTriples(
						tripleStore.getTriples(txn, LmdbValue.UNKNOWN_ID, 2, LmdbValue.UNKNOWN_ID, contextId, false))
						+ countTriples(tripleStore.getTriples(txn, LmdbValue.UNKNOWN_ID, 2, LmdbValue.UNKNOWN_ID,
								contextId, true));
				assertEquals(expected, actual, "Exact size does not match counted triples.");

			}

			for (final long objectId : objectIds) {
				long explicitActual = tripleStore.cardinalityExact(txn, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID,
						objectId,
						LmdbValue.UNKNOWN_ID, false);
				long totalActual = tripleStore.cardinalityExact(txn, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID,
						objectId,
						LmdbValue.UNKNOWN_ID, true);
				long implicitExpected = countTriples(
						tripleStore.getTriples(txn, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, objectId,
								LmdbValue.UNKNOWN_ID, false));
				long explicitExpected = countTriples(
						tripleStore.getTriples(txn, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, objectId,
								LmdbValue.UNKNOWN_ID, true));

				assertEquals(explicitExpected, explicitActual);
				assertEquals(totalActual, implicitExpected + explicitExpected,
						"Exact size does not match counted triples.");
			}

			for (int resourceId = 1; resourceId <= NUM_RESOURCES; resourceId++) {
				long totalExactSize = tripleStore.cardinalityExact(txn, resourceId, 2, LmdbValue.UNKNOWN_ID, 1, true);
				long expectedCount = countTriples(
						tripleStore.getTriples(txn, resourceId, 2, LmdbValue.UNKNOWN_ID, 1, false))
						+ countTriples(tripleStore.getTriples(txn, resourceId, 2, LmdbValue.UNKNOWN_ID, 1, true));
				assertEquals(expectedCount, totalExactSize, "Exact size does not match counted triples.");
			}

			for (int resourceId = 1; resourceId <= 50; resourceId++) {
				long targetObjectId = randomObjectId(random);
				long targetContextId = randomContextId(random);
				long generalSize = tripleStore.cardinalityExact(txn, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID,
						targetObjectId,
						targetContextId, false);
				long generalExplicitCount = countTriples(
						tripleStore.getTriples(
								txn, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, targetObjectId, targetContextId,
								true));
				assertEquals(
						generalExplicitCount, generalSize,
						"Exact size does not match counted triples."
				);
			}

			long totalSize = tripleStore.cardinalityExact(txn, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID,
					LmdbValue.UNKNOWN_ID,
					LmdbValue.UNKNOWN_ID, true);
			long totalCount = countTriples(tripleStore.getTriples(txn, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID,
					LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, false))
					+ countTriples(tripleStore.getTriples(txn, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID,
							LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, true));
			assertEquals(totalCount, totalSize, "Total size does not match counted triples.");
		}
	}

	@AfterEach
	public void after() throws Exception {
		tripleStore.close();
	}
}
