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

package org.eclipse.rdf4j.sail.lmdb.benchmark;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;

/**
 * Simple concurrent benchmark for LMDB multi-threaded performance.
 */
public class SimpleConcurrentBenchmark {

	private static final String NAMESPACE = "http://example.org/";
	private static final int PRELOAD_STATEMENTS = 100_000;
	private static final int BENCHMARK_DURATION_SECONDS = 5;

	private SailRepository repository;
	private File dataDir;
	private ValueFactory vf;

	private List<IRI> subjects;
	private List<IRI> predicates;
	private List<IRI> types;
	private List<Resource> contexts;

	public static void main(String[] args) throws Exception {
		SimpleConcurrentBenchmark benchmark = new SimpleConcurrentBenchmark();
		benchmark.run();
	}

	public void run() throws Exception {
		System.out.println("=== LMDB Store Concurrent Benchmark ===\n");

		setup();

		try {
			// Concurrent read benchmarks
			System.out.println("--- CONCURRENT READ BENCHMARKS ---");
			runConcurrentReadBenchmark(1);
			runConcurrentReadBenchmark(2);
			runConcurrentReadBenchmark(4);
			runConcurrentReadBenchmark(8);

			// Concurrent write benchmarks
			System.out.println("\n--- CONCURRENT WRITE BENCHMARKS ---");
			runConcurrentWriteBenchmark(1);
			runConcurrentWriteBenchmark(2);
			runConcurrentWriteBenchmark(4);

			// Mixed read/write benchmarks
			System.out.println("\n--- MIXED READ/WRITE BENCHMARKS ---");
			runMixedBenchmark(4, 1); // 4 readers, 1 writer
			runMixedBenchmark(8, 2); // 8 readers, 2 writers

		} finally {
			teardown();
		}
	}

	private void setup() throws Exception {
		System.out.println("Setting up benchmark environment...");
		dataDir = Files.newTemporaryFolder();
		vf = SimpleValueFactory.getInstance();

		subjects = new ArrayList<>(1000);
		predicates = new ArrayList<>(50);
		types = new ArrayList<>(20);
		contexts = new ArrayList<>(10);

		for (int i = 0; i < 1000; i++) {
			subjects.add(vf.createIRI(NAMESPACE, "subject-" + i));
		}
		for (int i = 0; i < 50; i++) {
			predicates.add(vf.createIRI(NAMESPACE, "predicate-" + i));
		}
		for (int i = 0; i < 20; i++) {
			types.add(vf.createIRI(NAMESPACE, "Type-" + i));
		}
		for (int i = 0; i < 10; i++) {
			contexts.add(vf.createIRI(NAMESPACE, "graph-" + i));
		}

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc,ospc");
		config.setForceSync(false);
		config.setValueDBSize(1_073_741_824L);
		config.setTripleDBSize(1_073_741_824L);
		config.setValueCacheSize(2048);

		repository = new SailRepository(new LmdbStore(dataDir, config));

		// Preload data
		System.out.println("Preloading " + PRELOAD_STATEMENTS + " statements...");
		try (SailRepositoryConnection conn = repository.getConnection()) {
			Random preloadRandom = new Random(42);
			RandomLiteralGenerator gen = new RandomLiteralGenerator(vf, preloadRandom);

			conn.begin(IsolationLevels.NONE);
			for (int i = 0; i < PRELOAD_STATEMENTS; i++) {
				IRI subject = subjects.get(i % subjects.size());
				IRI predicate = predicates.get(i % predicates.size());
				Resource context = contexts.get(i % contexts.size());

				if (i % 3 == 0) {
					conn.add(subject, RDF.TYPE, types.get(i % types.size()), context);
				} else if (i % 3 == 1) {
					conn.add(subject, RDFS.LABEL, gen.createRandomLiteral(), context);
				} else {
					IRI object = subjects.get((i + 1) % subjects.size());
					conn.add(subject, predicate, object, context);
				}
			}
			conn.commit();
		}
		System.out.println("Preload completed.\n");
	}

	private void teardown() throws Exception {
		if (repository != null) {
			repository.shutDown();
		}
		FileUtils.deleteDirectory(dataDir);
	}

	private void runConcurrentReadBenchmark(int numThreads) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		AtomicLong totalOps = new AtomicLong(0);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(numThreads);

		for (int t = 0; t < numThreads; t++) {
			final int threadId = t;
			executor.submit(() -> {
				try (SailRepositoryConnection conn = repository.getConnection()) {
					Random random = new Random(42 + threadId);
					startLatch.await();

					long endTime = System.currentTimeMillis() + BENCHMARK_DURATION_SECONDS * 1000;
					long ops = 0;

					while (System.currentTimeMillis() < endTime) {
						IRI subject = subjects.get(random.nextInt(subjects.size()));
						try (CloseableIteration<Statement> iter = conn.getStatements(subject, null, null, false)) {
							while (iter.hasNext()) {
								iter.next();
								ops++;
							}
						}
					}

					totalOps.addAndGet(ops);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		long startTime = System.currentTimeMillis();
		startLatch.countDown();
		doneLatch.await();
		long elapsed = System.currentTimeMillis() - startTime;

		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.SECONDS);

		double opsPerSec = totalOps.get() * 1000.0 / elapsed;
		System.out.printf("Concurrent Read (%d threads)       : %,12.0f ops/sec%n", numThreads, opsPerSec);
	}

	private void runConcurrentWriteBenchmark(int numThreads) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		AtomicLong totalOps = new AtomicLong(0);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(numThreads);

		for (int t = 0; t < numThreads; t++) {
			final int threadId = t;
			executor.submit(() -> {
				try (SailRepositoryConnection conn = repository.getConnection()) {
					Random random = new Random(42 + threadId);
					RandomLiteralGenerator gen = new RandomLiteralGenerator(vf, random);
					startLatch.await();

					long endTime = System.currentTimeMillis() + BENCHMARK_DURATION_SECONDS * 1000;
					long ops = 0;

					while (System.currentTimeMillis() < endTime) {
						conn.begin(IsolationLevels.NONE);
						for (int i = 0; i < 10; i++) {
							conn.add(
									subjects.get(random.nextInt(subjects.size())),
									predicates.get(random.nextInt(predicates.size())),
									gen.createRandomLiteral(),
									contexts.get(random.nextInt(contexts.size())));
							ops++;
						}
						conn.commit();
					}

					totalOps.addAndGet(ops);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		long startTime = System.currentTimeMillis();
		startLatch.countDown();
		doneLatch.await();
		long elapsed = System.currentTimeMillis() - startTime;

		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.SECONDS);

		double opsPerSec = totalOps.get() * 1000.0 / elapsed;
		System.out.printf("Concurrent Write (%d threads)      : %,12.0f ops/sec%n", numThreads, opsPerSec);
	}

	private void runMixedBenchmark(int numReaders, int numWriters) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(numReaders + numWriters);
		AtomicLong totalReadOps = new AtomicLong(0);
		AtomicLong totalWriteOps = new AtomicLong(0);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(numReaders + numWriters);

		// Start readers
		for (int t = 0; t < numReaders; t++) {
			final int threadId = t;
			executor.submit(() -> {
				try (SailRepositoryConnection conn = repository.getConnection()) {
					Random random = new Random(42 + threadId);
					startLatch.await();

					long endTime = System.currentTimeMillis() + BENCHMARK_DURATION_SECONDS * 1000;
					long ops = 0;

					while (System.currentTimeMillis() < endTime) {
						IRI subject = subjects.get(random.nextInt(subjects.size()));
						try (CloseableIteration<Statement> iter = conn.getStatements(subject, null, null, false)) {
							while (iter.hasNext()) {
								iter.next();
								ops++;
							}
						}
					}

					totalReadOps.addAndGet(ops);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		// Start writers
		for (int t = 0; t < numWriters; t++) {
			final int threadId = t + numReaders;
			executor.submit(() -> {
				try (SailRepositoryConnection conn = repository.getConnection()) {
					Random random = new Random(42 + threadId);
					RandomLiteralGenerator gen = new RandomLiteralGenerator(vf, random);
					startLatch.await();

					long endTime = System.currentTimeMillis() + BENCHMARK_DURATION_SECONDS * 1000;
					long ops = 0;

					while (System.currentTimeMillis() < endTime) {
						conn.begin(IsolationLevels.NONE);
						for (int i = 0; i < 10; i++) {
							conn.add(
									subjects.get(random.nextInt(subjects.size())),
									predicates.get(random.nextInt(predicates.size())),
									gen.createRandomLiteral(),
									contexts.get(random.nextInt(contexts.size())));
							ops++;
						}
						conn.commit();
					}

					totalWriteOps.addAndGet(ops);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		long startTime = System.currentTimeMillis();
		startLatch.countDown();
		doneLatch.await();
		long elapsed = System.currentTimeMillis() - startTime;

		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.SECONDS);

		double readOpsPerSec = totalReadOps.get() * 1000.0 / elapsed;
		double writeOpsPerSec = totalWriteOps.get() * 1000.0 / elapsed;
		System.out.printf("Mixed (%dR/%dW) - Reads             : %,12.0f ops/sec%n", numReaders, numWriters,
				readOpsPerSec);
		System.out.printf("Mixed (%dR/%dW) - Writes            : %,12.0f ops/sec%n", numReaders, numWriters,
				writeOpsPerSec);
	}
}
