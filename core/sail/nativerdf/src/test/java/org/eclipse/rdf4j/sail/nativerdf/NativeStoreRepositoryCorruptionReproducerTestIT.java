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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Reproduces a corruption scenario using only public repository APIs. Multiple writer threads perform rapid
 * begin/add/commit cycles with {@link IsolationLevels#NONE} while a concurrent reader thread continuously iterates over
 * all statements. This stresses NativeStore's value writes (buffered and direct-write) and flush-on-read behavior
 * without manipulating files directly.
 *
 * The test expects that, with soft-fail disabled, iterating statements after the workload can throw a
 * {@link RepositoryException} if values have become corrupted on disk. This is a reproducer; it is expected to fail on
 * affected implementations.
 */
@Tag("slow")
@Isolated
public class NativeStoreRepositoryCorruptionReproducerTestIT {

	@TempDir
	File dataDir;

	@AfterEach
	public void resetSoftFail() {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
	}

	@Test
	public void concurrentAddAndReadMayCorrupt() throws Exception {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false; // surface corruption

		NativeStore store = new NativeStore(dataDir, "spoc,posc");
		store.init();
		SailRepository repo = new SailRepository(store);
		repo.init();
		final SailRepository repoRef = repo; // effectively final for lambdas

		int writers = 8;
		int perWriterOps = 15_000;

		ExecutorService pool = Executors.newFixedThreadPool(writers + 2);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();

		// Writer threads: add small and occasional large values to exercise both buffered and direct-write paths
		for (int i = 0; i < writers; i++) {
			final int seed = 1234 + i;
			futures.add(pool.submit(() -> {
				Random rnd = new Random(seed);
				try (RepositoryConnection conn = repoRef.getConnection()) {
					try {
						start.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException(e);
					}

					ValueFactory vf = conn.getValueFactory();
					for (int j = 0; j < perWriterOps; j++) {
						conn.begin(IsolationLevels.NONE);
						try {
							// small IRI + literal
							String ns = "http://ex/" + rnd.nextInt(128) + "/";
							String local = "l" + rnd.nextInt(50_000);
							conn.add(vf.createIRI(ns, local), vf.createIRI("urn:p"), vf.createLiteral("v" + j));

							// occasionally write larger literal to hit direct write path
							if ((j % 200) == 0) {
								String big = buildString(12_000 + rnd.nextInt(4000));
								conn.add(vf.createIRI("urn:s" + j), vf.createIRI("urn:p"), vf.createLiteral(big));
							}
							conn.commit();
						} catch (Throwable t) {
							try {
								conn.rollback();
							} catch (Throwable ignore) {
							}
							// surface failures in worker
							throw new RuntimeException(t);
						}
					}
				}
				return null;
			}));
		}

		// Reader thread: continuously iterate, forcing flush-on-read in DataFile through ValueStore
		futures.add(pool.submit(() -> {
			try (RepositoryConnection conn = repoRef.getConnection()) {
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				conn.begin(IsolationLevels.NONE);
				try {
					long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
					while (System.nanoTime() < until) {
						try (RepositoryResult<Statement> statements = conn.getStatements(null, null, null, false)) {
							// no-op; force materialization
							// noinspection ResultOfMethodCallIgnored
							statements.forEachRemaining(Object::toString);
						}
						Thread.onSpinWait();
					}
					conn.commit();
				} catch (Throwable t) {
					try {
						conn.rollback();
					} catch (Throwable ignore) {
					}
					throw new RuntimeException(t);
				}
			}
			return null;
		}));

		// kick off
		start.countDown();
		try {
			for (Future<?> f : futures) {
				f.get();
			}
		} finally {
			pool.shutdownNow();
			pool.awaitTermination(10, TimeUnit.SECONDS);
		}

		// Close and reopen to ensure disk state is reloaded
		repo.shutDown();
		store.shutDown();

		store = new NativeStore(dataDir, "spoc,posc");
		store.init();
		repo = new SailRepository(store);
		repo.init();

		try (RepositoryConnection conn = repo.getConnection()) {
			// If corruption occurred, iterating statements should throw RepositoryException

			try (RepositoryResult<Statement> statements = conn.getStatements(null, null, null, false)) {
				// noinspection ResultOfMethodCallIgnored
				statements.forEachRemaining(Object::toString);
			}
		}

		repo.shutDown();
		store.shutDown();
	}

	private static String buildString(int len) {
		StringBuilder sb = new StringBuilder(len);
		while (sb.length() < len) {
			sb.append('x');
		}
		return sb.toString();
	}
}
