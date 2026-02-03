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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test to demonstrate and verify the lock striping optimization for concurrent writes. This test shows that multiple
 * threads can add statements concurrently when they target different subjects, thanks to the lock striping mechanism.
 */
public class LockStripingConcurrencyTest {

	@TempDir
	File tempDir;

	private Repository repository;
	private ValueFactory vf;

	@BeforeEach
	public void setUp() {
		vf = SimpleValueFactory.getInstance();
		LmdbStore store = new LmdbStore(tempDir);
		repository = new SailRepository(store);
		repository.init();
	}

	@AfterEach
	public void tearDown() {
		if (repository != null) {
			repository.shutDown();
		}
	}

	/**
	 * Test concurrent writes to different subjects. Lock striping should allow these to proceed in parallel.
	 */
	@Test
	public void testConcurrentWritesToDifferentSubjects() throws Exception {
		int numThreads = 16;
		int statementsPerThread = 1000;

		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch finishLatch = new CountDownLatch(numThreads);
		AtomicLong totalTime = new AtomicLong(0);

		// Each thread writes statements with different subjects
		for (int threadId = 0; threadId < numThreads; threadId++) {
			final int tid = threadId;
			executor.submit(() -> {
				try {
					// Wait for all threads to be ready
					startLatch.await();

					long start = System.nanoTime();
					try (RepositoryConnection conn = repository.getConnection()) {
						conn.begin();
						for (int i = 0; i < statementsPerThread; i++) {
							// Different subject per thread (should use different lock stripes)
							IRI subject = vf.createIRI("http://example.org/subject" + tid + "_" + i);
							IRI predicate = vf.createIRI("http://example.org/predicate");
							IRI object = vf.createIRI("http://example.org/object" + i);
							conn.add(subject, predicate, object);
						}
						conn.commit();
					}
					long duration = System.nanoTime() - start;
					totalTime.addAndGet(duration);
					finishLatch.countDown();
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		}

		// Start all threads simultaneously
		long overallStart = System.nanoTime();
		startLatch.countDown();

		// Wait for all threads to complete
		boolean completed = finishLatch.await(60, TimeUnit.SECONDS);
		assertTrue(completed, "All threads should complete within timeout");

		long overallDuration = System.nanoTime() - overallStart;
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		// Verify all statements were added
		try (RepositoryConnection conn = repository.getConnection()) {
			long count = conn.size();
			assertEquals(numThreads * statementsPerThread, count,
					"All statements should be persisted");
		}

		// Report timing
		long avgThreadTime = totalTime.get() / numThreads;
		double parallelismFactor = (double) totalTime.get() / overallDuration;

		System.out.println("\n=== Lock Striping Concurrency Test Results ===");
		System.out.println("Threads: " + numThreads);
		System.out.println("Statements per thread: " + statementsPerThread);
		System.out.println("Total statements: " + (numThreads * statementsPerThread));
		System.out.println("Overall duration: " + (overallDuration / 1_000_000) + " ms");
		System.out.println("Average thread time: " + (avgThreadTime / 1_000_000) + " ms");
		System.out.println("Parallelism factor: " + String.format("%.2f", parallelismFactor) + "x");
		System.out.println("(A factor > 1.0 indicates effective parallelism)");
		System.out.println("===============================================\n");

		// With lock striping, we expect some degree of parallelism
		// The exact factor depends on hardware, but should be > 1.0
		assertTrue(parallelismFactor > 1.0,
				"Lock striping should enable some parallelism (factor > 1.0)");
	}

	/**
	 * Test concurrent writes to the SAME subject. These will contend on the same lock stripe, limiting parallelism.
	 */
	@Test
	public void testConcurrentWritesToSameSubject() throws Exception {
		int numThreads = 16;
		int statementsPerThread = 100;
		IRI sharedSubject = vf.createIRI("http://example.org/sharedSubject");

		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch finishLatch = new CountDownLatch(numThreads);

		// Each thread writes statements with the SAME subject
		for (int threadId = 0; threadId < numThreads; threadId++) {
			final int tid = threadId;
			executor.submit(() -> {
				try {
					startLatch.await();
					try (RepositoryConnection conn = repository.getConnection()) {
						conn.begin();
						for (int i = 0; i < statementsPerThread; i++) {
							// Same subject (will contend on same lock stripe)
							IRI predicate = vf.createIRI("http://example.org/pred" + tid + "_" + i);
							IRI object = vf.createIRI("http://example.org/obj" + i);
							conn.add(sharedSubject, predicate, object);
						}
						conn.commit();
					}
					finishLatch.countDown();
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		}

		startLatch.countDown();
		boolean completed = finishLatch.await(60, TimeUnit.SECONDS);
		assertTrue(completed, "All threads should complete within timeout");

		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		// Verify all statements were added
		try (RepositoryConnection conn = repository.getConnection()) {
			long count = conn.size();
			assertEquals(numThreads * statementsPerThread, count,
					"All statements should be persisted despite contention");
		}
	}

	/**
	 * Stress test with mixed subjects to verify correctness under load.
	 */
	@Test
	public void testMixedConcurrentWrites() throws Exception {
		int numThreads = 8;
		int statementsPerThread = 500;
		int subjectVariety = 100; // Each thread will cycle through 100 different subjects

		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch finishLatch = new CountDownLatch(numThreads);
		List<Exception> exceptions = new ArrayList<>();

		for (int threadId = 0; threadId < numThreads; threadId++) {
			final int tid = threadId;
			executor.submit(() -> {
				try {
					startLatch.await();
					try (RepositoryConnection conn = repository.getConnection()) {
						conn.begin();
						for (int i = 0; i < statementsPerThread; i++) {
							// Cycle through different subjects (some collision expected)
							int subjId = (tid * statementsPerThread + i) % subjectVariety;
							IRI subject = vf.createIRI("http://example.org/mixed_subject" + subjId);
							IRI predicate = vf.createIRI("http://example.org/pred" + tid + "_" + i);
							IRI object = vf.createIRI("http://example.org/obj" + i);
							conn.add(subject, predicate, object);
						}
						conn.commit();
					}
					finishLatch.countDown();
				} catch (Exception e) {
					synchronized (exceptions) {
						exceptions.add(e);
					}
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		}

		startLatch.countDown();
		boolean completed = finishLatch.await(60, TimeUnit.SECONDS);
		assertTrue(completed, "All threads should complete within timeout");

		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		// Check for exceptions
		assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent writes");

		// Verify statements
		try (RepositoryConnection conn = repository.getConnection()) {
			long count = conn.size();
			assertEquals(numThreads * statementsPerThread, count,
					"All statements should be persisted correctly");
		}
	}
}
