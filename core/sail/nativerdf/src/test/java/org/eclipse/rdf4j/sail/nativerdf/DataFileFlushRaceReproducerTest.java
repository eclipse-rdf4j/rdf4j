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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Attempts to deterministically reproduce corruption caused by a race between {@code DataFile.flush()} and concurrent
 * buffered writes. The test hammers the {@link ValueStore} with small writes while a separate thread forces frequent
 * {@code DataFile.getData(...)} calls (which invoke {@code flush()}). Finally, after closing and reopening the store, a
 * consistency check is expected to fail if corruption has occurred.
 *
 * The workload and explicit flusher increase the likelihood of the following race: - Writer increases the in-memory
 * write buffer and {@code nioFileSize} without flushing - Concurrent read forces {@code flush()}, which computes the
 * flush offset using the current {@code nioFileSize} while copying a smaller snapshot of the buffer content, producing
 * misaligned writes.
 */
@Isolated
public class DataFileFlushRaceReproducerTest {

	@TempDir
	File dataDir;

	private ValueStore valueStore;

	@BeforeEach
	public void setup() throws IOException {
		valueStore = new ValueStore(dataDir);
		// surface corruption as exceptions (not soft-fail)
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
	public void flushDuringConcurrentWritesTriggersCorruption() throws Exception {
		int writerThreads = 8; // DataStore serializes storeData, but this keeps the buffer hot
		int valuesPerWriter = 50_000; // small values -> buffered path, many flush opportunities

		ExecutorService pool = Executors.newFixedThreadPool(writerThreads + 1);
		CountDownLatch start = new CountDownLatch(1);

		// reflectively access the underlying DataStore so we can force getData(id) to
		// call DataFile.flush() on every iteration (ValueStore caches may avoid reads otherwise)
		DataStore ds = (DataStore) reflect(valueStore, "dataStore");

		List<Future<?>> futures = new ArrayList<>();

		// Writers
		for (int i = 0; i < writerThreads; i++) {
			final int seed = 4242 + i;
			futures.add(pool.submit(() -> {
				Random rnd = new Random(seed);
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}

				for (int j = 0; j < valuesPerWriter; j++) {
					try {
						// keep values small to stay on buffered write path
						String ns = "http://ex/" + rnd.nextInt(64) + "/";
						String local = "l" + rnd.nextInt(20_000);
						valueStore.storeValue(valueStore.createIRI(ns + local));

						switch (rnd.nextInt(3)) {
						case 0:
							valueStore.storeValue(valueStore.createLiteral("v" + rnd.nextInt(10_000)));
							break;
						case 1:
							valueStore.storeValue(valueStore.createLiteral("v" + rnd.nextInt(10_000), "en"));
							break;
						default:
							valueStore.storeValue(valueStore
									.createLiteral("v" + rnd.nextInt(10_000),
											valueStore.createIRI("http://dt/" + rnd.nextInt(128))));
							break;
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				return null;
			}));
		}

		// Flusher: repeatedly read an early id to force DataFile.getData(...)->flush()
		futures.add(pool.submit(() -> {
			try {
				start.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			// spin for roughly the duration of writers
			long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
			while (System.nanoTime() < until) {
				try {
					int max = ds.getMaxID();
					if (max >= 1) {
						// focus on a small id to avoid cache churn and ensure frequent flushes
						ds.getData(1);
					}
				} catch (IOException e) {
					// propagate to fail the test
					throw new RuntimeException(e);
				}
				// minimal pause to yield
				Thread.onSpinWait();
			}
			return null;
		}));

		// go!
		start.countDown();

		try {
			for (Future<?> f : futures) {
				f.get();
			}
		} finally {
			pool.shutdownNow();
		}

		// Close and reopen to ensure we load from disk only
		valueStore.close();
		valueStore = new ValueStore(dataDir);

		valueStore.checkConsistency();
	}

	private static Object reflect(Object target, String field) {
		try {
			Field f = target.getClass().getDeclaredField(field);
			f.setAccessible(true);
			return f.get(target);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
