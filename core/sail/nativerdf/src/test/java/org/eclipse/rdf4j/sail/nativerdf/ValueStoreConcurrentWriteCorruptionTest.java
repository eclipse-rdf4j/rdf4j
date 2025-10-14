/**
 *******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************
 */
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

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Induces concurrent writes directly against {@link ValueStore} to demonstrate a race condition that corrupts the
 * underlying values data files. This does not go through {@link NativeStore}'s internal locking and therefore
 * deliberately stresses {@link org.eclipse.rdf4j.sail.nativerdf.datastore.DataFile} concurrent write behavior.
 */
@Isolated
public class ValueStoreConcurrentWriteCorruptionTest {

	@TempDir
	File dataDir;

	private ValueStore valueStore;

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@BeforeEach
	public void setup() throws IOException {
		valueStore = new ValueStore(dataDir);
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
	}

	@AfterEach
	public void tearDown() throws IOException {
		try {
			if (valueStore != null) {
				valueStore.close();
			}
		} finally {
			NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
		}
	}

	@Test
	public void concurrentStoreValueShouldCorrupt() throws Exception {
		int writers = 16;
		int valuesPerWriter = 8000;

		ExecutorService pool = Executors.newFixedThreadPool(writers);
		CountDownLatch start = new CountDownLatch(1);

		try {
			List<Future<?>> futures = new ArrayList<>();

			for (int i = 0; i < writers; i++) {
				final int seed = 42 + i;
				futures.add(pool.submit(() -> writeLoad(start, seed, valuesPerWriter)));
			}

			// start all writers concurrently
			start.countDown();

			// wait for completion (propagate any exception from workers)
			for (Future<?> f : futures) {
				f.get();
			}
		} finally {
			pool.shutdownNow();
		}

		// Close and reopen the ValueStore to force data reload from disk
		valueStore.close();
		valueStore = new ValueStore(dataDir);

		valueStore.checkConsistency();

	}

	private void writeLoad(CountDownLatch start, int seed, int count) {
		try {
			start.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		Random rnd = new Random(seed);
		for (int i = 0; i < count; i++) {
			try {
				// small values to exercise buffered write path
				String ns = "http://ex/" + rnd.nextInt(100) + "/";
				String local = "l" + rnd.nextInt(50_000);
				valueStore.storeValue(vf.createIRI(ns + local));

				switch (rnd.nextInt(3)) {
				case 0:
					valueStore.storeValue(vf.createLiteral("v" + rnd.nextInt(10_000)));
					break;
				case 1:
					valueStore.storeValue(vf.createLiteral("v" + rnd.nextInt(10_000), "en"));
					break;
				default:
					valueStore.storeValue(
							vf.createLiteral("v" + rnd.nextInt(10_000), vf.createIRI("http://dt/" + rnd.nextInt(100))));
					break;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
