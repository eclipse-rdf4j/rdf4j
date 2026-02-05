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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Quick performance test for LMDB store.
 */
public class QuickPerfTest {

	private static final String NS = "http://example.org/";
	private static final int PRELOAD = 100_000;
	private static final int OPS = 200_000;

	@TempDir
	File dataDir;

	@Test
	public void testConcurrentRead() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();
		List<IRI> subjects = new ArrayList<>(1000);
		for (int i = 0; i < 1000; i++) {
			subjects.add(vf.createIRI(NS, "s" + i));
		}

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		config.setForceSync(false);
		config.setValueDBSize(1_073_741_824L);
		config.setTripleDBSize(config.getValueDBSize());

		SailRepository repo = new SailRepository(new LmdbStore(dataDir, config));

		// Preload
		try (SailRepositoryConnection conn = repo.getConnection()) {
			conn.begin(IsolationLevels.NONE);
			for (int i = 0; i < PRELOAD; i++) {
				IRI s = subjects.get(i % subjects.size());
				IRI p = vf.createIRI(NS, "p" + (i % 50));
				IRI o = vf.createIRI(NS, "o" + (i % 200));
				IRI g = vf.createIRI(NS, "g" + (i % 10));
				conn.add(s, p, o, g);
			}
			conn.commit();
		}

		System.out.println("Preloaded " + PRELOAD + " statements");

		// Test concurrent reads
		for (int threads : new int[] { 1, 2, 4, 8 }) {
			runConcurrentRead(repo, subjects, threads, vf);
		}

		// Test concurrent writes
		for (int threads : new int[] { 1, 2, 4 }) {
			runConcurrentWrite(repo, subjects, threads, vf);
		}

		repo.shutDown();
	}

	private void runConcurrentRead(SailRepository repo, List<IRI> subjects, int threads, ValueFactory vf)
			throws Exception {
		ExecutorService exec = Executors.newFixedThreadPool(threads);
		AtomicLong totalOps = new AtomicLong(0);
		CountDownLatch latch = new CountDownLatch(threads);
		int opsPerThread = OPS / threads;

		long start = System.nanoTime();

		for (int t = 0; t < threads; t++) {
			final int threadId = t;
			exec.submit(() -> {
				try (SailRepositoryConnection conn = repo.getConnection()) {
					Random rnd = new Random(42 + threadId);
					for (int i = 0; i < opsPerThread; i++) {
						IRI subject = subjects.get(rnd.nextInt(subjects.size()));
						try (CloseableIteration<Statement> iter = conn.getStatements(subject, null, null, false)) {
							while (iter.hasNext()) {
								iter.next();
								totalOps.incrementAndGet();
							}
						}
					}
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		exec.shutdown();

		long elapsed = System.nanoTime() - start;
		double seconds = elapsed / 1_000_000_000.0;
		long ops = totalOps.get();
		System.out.printf("Read  %d threads: %,d ops in %.2fs = %,.0f ops/sec%n", threads, ops, seconds, ops / seconds);
	}

	private void runConcurrentWrite(SailRepository repo, List<IRI> subjects, int threads, ValueFactory vf)
			throws Exception {
		ExecutorService exec = Executors.newFixedThreadPool(threads);
		AtomicLong totalOps = new AtomicLong(0);
		CountDownLatch latch = new CountDownLatch(threads);
		int opsPerThread = 20_000 / threads;

		long start = System.nanoTime();

		for (int t = 0; t < threads; t++) {
			final int threadId = t;
			exec.submit(() -> {
				try (SailRepositoryConnection conn = repo.getConnection()) {
					Random rnd = new Random(42 + threadId);
					for (int i = 0; i < opsPerThread; i++) {
						conn.begin(IsolationLevels.NONE);
						IRI s = subjects.get(rnd.nextInt(subjects.size()));
						IRI p = vf.createIRI(NS, "wp" + rnd.nextInt(50));
						IRI o = vf.createIRI(NS, "wo" + rnd.nextInt(200));
						IRI g = vf.createIRI(NS, "wg" + rnd.nextInt(10));
						conn.add(s, p, o, g);
						conn.commit();
						totalOps.incrementAndGet();
					}
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		exec.shutdown();

		long elapsed = System.nanoTime() - start;
		double seconds = elapsed / 1_000_000_000.0;
		long ops = totalOps.get();
		System.out.printf("Write %d threads: %,d ops in %.2fs = %,.0f ops/sec%n", threads, ops, seconds, ops / seconds);
	}
}
