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
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises the {@link ValueStore} obtained from an initialized {@link NativeStore} with highly concurrent writes. The
 * workload mirrors {@link ValueStoreConcurrentWriteCorruptionTest} but goes through {@link NativeStore} to demonstrate
 * that the embedded {@link ValueStore} can still become corrupted.
 */
public class NativeStoreConcurrentValueStoreCorruptionTest {

	@TempDir
	File dataDir;

	@AfterEach
	public void resetSoftFailFlag() {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = true;
	}

	@Test
	public void concurrentNativeValueStoreWritesTriggerCorruption() throws Exception {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;

		NativeStore store = new NativeStore(dataDir, "spoc,posc");
		store.init();

		ValueStore valueStore = (ValueStore) store.getValueFactory();

		int writers = 16;
		int valuesPerWriter = 1000;

		ExecutorService pool = Executors.newFixedThreadPool(writers);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < writers; i++) {
			final int seed = 42 + i;
			futures.add(pool.submit(() -> {
				Random random = new Random(seed);
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}

				for (int j = 0; j < valuesPerWriter; j++) {
					try {
						storeRandomValue(random, valueStore);

					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				return null;
			}));
		}

		start.countDown();

		try {
			for (Future<?> future : futures) {
				future.get();
			}
		} finally {
			pool.shutdownNow();
		}

		store.shutDown();

		NativeStore reopened = new NativeStore(dataDir, "spoc,posc");
		reopened.init();

		ValueStore reopenedValueStore = (ValueStore) reopened.getValueFactory();

		reopenedValueStore.checkConsistency();

		reopened.shutDown();
	}

	private static void storeRandomValue(Random random, ValueStore valueStore) throws IOException {
		String namespace = "http://example.org/ns/" + random.nextInt(200) + "/";
		String localName = "s" + random.nextInt(50_000);
		valueStore.storeValue(valueStore.createIRI(namespace, localName));

		switch (random.nextInt(3)) {
		case 0:
			valueStore.storeValue(valueStore.createLiteral("value-" + random.nextInt(100_000)));
			break;
		case 1:
			valueStore.storeValue(valueStore.createLiteral("value-" + random.nextInt(100_000), "en"));
			break;
		default:
			valueStore.storeValue(valueStore.createLiteral(
					"value-" + random.nextInt(100_000),
					valueStore.createIRI("http://example.org/dt/" + random.nextInt(256))));
			break;
		}
	}
}
