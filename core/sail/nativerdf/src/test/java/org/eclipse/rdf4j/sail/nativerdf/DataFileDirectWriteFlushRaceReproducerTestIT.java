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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Reproduces a corruption scenario where a mix of small (buffered) and large (direct-write) values are written while a
 * concurrent reader forces frequent DataFile.flush() via reads. The race between flush() offset calculation and
 * direct-write size updates can misplace buffered bytes, corrupting records.
 */
@Tag("slow")
@Isolated
public class DataFileDirectWriteFlushRaceReproducerTestIT {

	@TempDir
	File dataDir;

	private ValueStore valueStore;

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
	public void mixedDirectAndBufferedWritesWithConcurrentFlushCauseCorruption() throws Exception {
		// Multiple writers with occasional large literals to trigger direct-write path
		int writerThreads = 4; // small-value writers (buffered path)
		int valuesPerWriter = 40_000;

		ExecutorService pool = Executors.newFixedThreadPool(writerThreads + 1);
		CountDownLatch start = new CountDownLatch(1);

		DataStore ds = (DataStore) reflect(valueStore, "dataStore");

		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < writerThreads; i++) {
			final int seed = 7777 + i;
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
						// Always write a small IRI first to exercise buffered writes
						String ns = "http://ns/" + rnd.nextInt(64) + "/";
						String local = "s" + rnd.nextInt(50_000);
						valueStore.storeValue(valueStore.createIRI(ns, local));

						// Small literal keeps buffer hot (buffered path)
						valueStore.storeValue(valueStore.createLiteral("v" + rnd.nextInt(10000)));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				return null;
			}));
		}

		// Big writer: continuously write large literals to trigger direct-write path
		futures.add(pool.submit(() -> {
			Random rnd = new Random(9999);
			try {
				start.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			for (int i = 0; i < valuesPerWriter; i++) {
				try {
					String big = buildString(12_000 + rnd.nextInt(6000));
					valueStore.storeValue(valueStore.createLiteral(big));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			return null;
		}));

		// Flusher thread: keep forcing getData(1) and random ids to trigger flush() frequently
		futures.add(pool.submit(() -> {
			try {
				start.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			Random r = new Random(1337);
			long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
			while (System.nanoTime() < until) {
				try {
					int max = ds.getMaxID();
					if (max >= 1) {
						// alternate between a stable low id and a random high id
						ds.getData(1);
						ds.getData(1 + r.nextInt(Math.max(1, max)));
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				Thread.onSpinWait();
			}
			return null;
		}));

		start.countDown();

		try {
			for (Future<?> f : futures) {
				f.get();
			}
		} finally {
			pool.shutdownNow();
		}

		valueStore.close();
		valueStore = new ValueStore(dataDir);

		valueStore.checkConsistency();
	}

	private static String buildString(int targetLength) {
		StringBuilder b = new StringBuilder(targetLength);
		while (b.length() < targetLength) {
			b.append('x');
		}
		return b.toString();
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
