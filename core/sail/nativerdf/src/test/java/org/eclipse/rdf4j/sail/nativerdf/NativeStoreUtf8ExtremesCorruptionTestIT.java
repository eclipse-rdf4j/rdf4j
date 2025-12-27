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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
 * Attempts to reproduce a corrupt NativeStore by iteratively storing IRIs and BNodes whose UTF-8 encodings exercise
 * extremes and boundary lengths (1/2/3/4-byte code points; sizes near small and page-sized thresholds). A concurrent
 * reader thread repeatedly calls into the underlying DataStore to force frequent DataFile.flush() operations while
 * writes happen, increasing the chance of a flush/write race corrupting values.
 *
 * If corruption occurs, the subsequent checkConsistency() call will throw, causing this test to fail. This test is
 * designed as a reproducer (expected to fail when corruption is possible) rather than an assertion of correctness.
 */
@Tag("slow")
@Isolated
public class NativeStoreUtf8ExtremesCorruptionTestIT {

	@TempDir
	File dataDir;

	private NativeStore store;

	private ValueStore valueStore;

	@BeforeEach
	public void setup() throws Exception {
		// surface corruption as exceptions
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
		store = new NativeStore(dataDir, "spoc,posc");
		store.init();
		valueStore = (ValueStore) store.getValueFactory();
	}

	@AfterEach
	public void tearDown() throws Exception {
		try {
			if (store != null) {
				store.shutDown();
			}
		} finally {
			NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
		}
	}

	@Test
	public void utf8ExtremesForIrisAndBNodesMayCorrupt() throws Exception {
		// Reflect underlying DataStore to drive flushes via getData(id)
		DataStore ds = (DataStore) reflect(valueStore, "dataStore");

		ExecutorService pool = Executors.newFixedThreadPool(3);
		CountDownLatch start = new CountDownLatch(1);

		List<Future<?>> futures = new ArrayList<>();

		// Writer: iterate across code point classes and length boundaries
		futures.add(pool.submit(() -> {
			try {
				start.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}

			String ns = "http://example.org/ns/";

			// code point classes: ASCII(1B), U+00A2(2B), U+20AC(3B), U+1F600(4B)
			String[] cps = new String[] { "a", "\u00A2", "\u20AC", "\uD83D\uDE00" };

			// length targets (bytes) to exercise small/medium/page-ish boundaries
			int[] byteTargets = new int[] {
					1, 2, 3, 4, 5,
					15, 31, 63, 127, 255, 256, 257,
					512 - 8, 512 - 1, 512, 512 + 1,
					1024 - 8, 1024 - 1, 1024, 1024 + 1,
					2048 - 8, 2048 - 1, 2048, 2048 + 1,
					4096 - 16, 4096 - 8, 4096 - 5, 4096 - 4, 4096 - 1, 4096, 4096 + 1, (64 * 1024) - 1, (64 * 1024),
					(64 * 1024) + 1,
					(1024 * 1024) - 1, (1024 * 1024), (1024 * 1024) + 1
			};

			try {
				// prime namespace so ns id is stable
				valueStore.storeValue(valueStore.createIRI(ns, "seed"));

				for (String cp : cps) {
					for (int targetBytes : byteTargets) {
						// Build local name approximately targetBytes long in UTF-8
						String local = buildUtf8Length(cp, targetBytes);
						valueStore.storeValue(valueStore.createIRI(ns, local));

						// Also add a BNode id with similar UTF-8 size
						String bnodeId = buildUtf8Length(cp, targetBytes);
						valueStore.storeValue(valueStore.createBNode(bnodeId));

						// Interleave small literal writes to keep buffered path hot
						valueStore.storeValue(valueStore.createLiteral("v" + targetBytes));
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return null;
		}));

		// Flusher: aggressively force reads to trigger DataFile.flush()
		futures.add(pool.submit(() -> {
			try {
				start.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			long until = System.nanoTime() + Duration.ofSeconds(30).toNanos();
			while (System.nanoTime() < until) {
				try {
					int max = ds.getMaxID();
					if (max >= 1) {
						// alternate between a stable low id and a random-ish recent id
						ds.getData(1);
						ds.getData(Math.max(1, max));
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				Thread.onSpinWait();
			}
			return null;
		}));

		// Big IRI writer: periodically write very large IRI/BNode values to trigger direct-write path
		futures.add(pool.submit(() -> {
			try {
				start.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			String ns = "http://example.org/big/";
			try {
				// prime
				valueStore.storeValue(valueStore.createIRI(ns, "start"));
				for (int i = 0; i < 2000; i++) {
					String bigLocal = buildUtf8Length("a", 12_000 + (i % 200));
					valueStore.storeValue(valueStore.createIRI(ns, bigLocal));
					// large bnode ids too
					String bigBNode = buildUtf8Length("\uD83D\uDE00", 10_000 + (i % 300));
					valueStore.storeValue(valueStore.createBNode(bigBNode));
					// keep buffer hot in between
					valueStore.storeValue(valueStore.createLiteral("keep" + i));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
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
			pool.awaitTermination(5, TimeUnit.SECONDS);
		}

		// Close and reopen to force load-from-disk
		store.shutDown();
		store = new NativeStore(dataDir, "spoc,posc");
		store.init();
		valueStore = (ValueStore) store.getValueFactory();

		// If corruption occurred this should throw, causing the test to fail (reproducer)
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

	private static String buildUtf8Length(String unit, int targetBytes) {
		if (targetBytes <= 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		int unitBytes = unit.getBytes(StandardCharsets.UTF_8).length;
		if (unitBytes <= 0) {
			unitBytes = 1;
		}
		int reps = Math.max(1, targetBytes / unitBytes);
		for (int i = 0; i < reps; i++) {
			sb.append(unit);
		}
		// if we overshot, trim by bytes (safe: only append ASCII 'x' when under)
		while (sb.toString().getBytes(StandardCharsets.UTF_8).length > targetBytes && sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		while (sb.toString().getBytes(StandardCharsets.UTF_8).length < targetBytes) {
			sb.append('x');
		}
		return sb.toString();
	}
}
