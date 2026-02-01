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
 * Simple standalone benchmark for LMDB performance measurement. Does not require JMH annotation processing.
 */
public class SimpleLmdbBenchmark {

	private static final String NAMESPACE = "http://example.org/";
	private static final int WARMUP_ITERATIONS = 3;
	private static final int MEASURE_ITERATIONS = 5;
	private static final int PRELOAD_STATEMENTS = 100_000;

	private SailRepository repository;
	private SailRepositoryConnection connection;
	private File dataDir;
	private ValueFactory vf;
	private Random random;
	private RandomLiteralGenerator literalGenerator;

	private List<IRI> subjects;
	private List<IRI> predicates;
	private List<IRI> types;
	private List<Resource> contexts;

	private IRI knownSubject;
	private IRI knownPredicate;

	public static void main(String[] args) throws Exception {
		SimpleLmdbBenchmark benchmark = new SimpleLmdbBenchmark();
		benchmark.run();
	}

	public void run() throws Exception {
		System.out.println("=== LMDB Store Performance Benchmark ===\n");

		setup();

		try {
			// Write benchmarks
			System.out.println("--- WRITE BENCHMARKS ---");
			runBenchmark("Single Statement Write", this::writeSingleStatement);
			runBenchmark("Small Batch Write (10)", this::writeSmallBatch);
			runBenchmark("Medium Batch Write (100)", this::writeMediumBatch);
			runBenchmark("Large Batch Write (1000)", this::writeLargeBatch);

			// Read benchmarks
			System.out.println("\n--- READ BENCHMARKS ---");
			runBenchmark("Read by Subject", this::readBySubject);
			runBenchmark("Read by Predicate", this::readByPredicate);
			runBenchmark("Read by Subject+Predicate", this::readBySubjectPredicate);
			runBenchmark("Read Random Subjects (100)", this::readRandomSubjects);

			// Mixed workload
			System.out.println("\n--- MIXED WORKLOAD ---");
			runBenchmark("Mixed 80% Read / 20% Write", this::mixedWorkload);

		} finally {
			teardown();
		}
	}

	private void setup() throws Exception {
		System.out.println("Setting up benchmark environment...");
		dataDir = Files.newTemporaryFolder();
		vf = SimpleValueFactory.getInstance();
		random = new Random(42);
		literalGenerator = new RandomLiteralGenerator(vf, random);

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

		knownSubject = subjects.get(0);
		knownPredicate = predicates.get(0);

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc,ospc");
		config.setForceSync(false);
		config.setValueDBSize(1_073_741_824L);
		config.setTripleDBSize(1_073_741_824L);
		config.setValueCacheSize(2048);

		repository = new SailRepository(new LmdbStore(dataDir, config));
		connection = repository.getConnection();

		// Preload data
		System.out.println("Preloading " + PRELOAD_STATEMENTS + " statements...");
		long start = System.currentTimeMillis();
		connection.begin(IsolationLevels.NONE);
		for (int i = 0; i < PRELOAD_STATEMENTS; i++) {
			IRI subject = subjects.get(i % subjects.size());
			IRI predicate = predicates.get(i % predicates.size());
			Resource context = contexts.get(i % contexts.size());

			if (i % 3 == 0) {
				connection.add(subject, RDF.TYPE, types.get(i % types.size()), context);
			} else if (i % 3 == 1) {
				connection.add(subject, RDFS.LABEL, literalGenerator.createRandomLiteral(), context);
			} else {
				IRI object = subjects.get((i + 1) % subjects.size());
				connection.add(subject, predicate, object, context);
			}
		}
		connection.commit();
		long elapsed = System.currentTimeMillis() - start;
		System.out.println("Preload completed in " + elapsed + " ms (" +
				String.format("%.0f", PRELOAD_STATEMENTS * 1000.0 / elapsed) + " stmts/sec)\n");
	}

	private void teardown() throws Exception {
		if (connection != null) {
			connection.close();
		}
		if (repository != null) {
			repository.shutDown();
		}
		FileUtils.deleteDirectory(dataDir);
	}

	private void runBenchmark(String name, BenchmarkOperation op) {
		// Warmup
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			try {
				op.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Measure
		long totalTime = 0;
		long totalOps = 0;
		for (int i = 0; i < MEASURE_ITERATIONS; i++) {
			try {
				long start = System.nanoTime();
				long ops = op.run();
				long elapsed = System.nanoTime() - start;
				totalTime += elapsed;
				totalOps += ops;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		double avgTimeMs = (totalTime / MEASURE_ITERATIONS) / 1_000_000.0;
		double opsPerSec = (totalOps / MEASURE_ITERATIONS) * 1_000_000_000.0 / (totalTime / MEASURE_ITERATIONS);

		System.out.printf("%-35s: %8.3f ms  (%,.0f ops/sec)%n", name, avgTimeMs, opsPerSec);
	}

	// ==================== BENCHMARK OPERATIONS ====================

	private long writeSingleStatement() {
		connection.begin(IsolationLevels.NONE);
		connection.add(randomSubject(), randomPredicate(), literalGenerator.createRandomLiteral(), randomContext());
		connection.commit();
		return 1;
	}

	private long writeSmallBatch() {
		connection.begin(IsolationLevels.NONE);
		for (int i = 0; i < 10; i++) {
			connection.add(randomSubject(), randomPredicate(), literalGenerator.createRandomLiteral(), randomContext());
		}
		connection.commit();
		return 10;
	}

	private long writeMediumBatch() {
		connection.begin(IsolationLevels.NONE);
		for (int i = 0; i < 100; i++) {
			connection.add(randomSubject(), randomPredicate(), literalGenerator.createRandomLiteral(), randomContext());
		}
		connection.commit();
		return 100;
	}

	private long writeLargeBatch() {
		connection.begin(IsolationLevels.NONE);
		for (int i = 0; i < 1000; i++) {
			connection.add(randomSubject(), randomPredicate(), literalGenerator.createRandomLiteral(), randomContext());
		}
		connection.commit();
		return 1000;
	}

	private long readBySubject() {
		long count = 0;
		try (CloseableIteration<Statement> iter = connection.getStatements(knownSubject, null, null, false)) {
			while (iter.hasNext()) {
				iter.next();
				count++;
			}
		}
		return Math.max(count, 1);
	}

	private long readByPredicate() {
		long count = 0;
		try (CloseableIteration<Statement> iter = connection.getStatements(null, knownPredicate, null, false)) {
			while (iter.hasNext()) {
				iter.next();
				count++;
			}
		}
		return Math.max(count, 1);
	}

	private long readBySubjectPredicate() {
		long count = 0;
		try (CloseableIteration<Statement> iter = connection.getStatements(knownSubject, knownPredicate, null, false)) {
			while (iter.hasNext()) {
				iter.next();
				count++;
			}
		}
		return Math.max(count, 1);
	}

	private long readRandomSubjects() {
		long count = 0;
		for (int i = 0; i < 100; i++) {
			IRI subject = subjects.get(random.nextInt(subjects.size()));
			try (CloseableIteration<Statement> iter = connection.getStatements(subject, null, null, false)) {
				while (iter.hasNext()) {
					iter.next();
					count++;
				}
			}
		}
		return Math.max(count, 1);
	}

	private long mixedWorkload() {
		long count = 0;
		for (int i = 0; i < 100; i++) {
			if (random.nextInt(100) < 80) {
				// Read
				IRI subject = subjects.get(random.nextInt(subjects.size()));
				try (CloseableIteration<Statement> iter = connection.getStatements(subject, null, null, false)) {
					while (iter.hasNext()) {
						iter.next();
						count++;
					}
				}
			} else {
				// Write
				connection.begin(IsolationLevels.NONE);
				connection.add(randomSubject(), randomPredicate(), literalGenerator.createRandomLiteral(),
						randomContext());
				connection.commit();
				count++;
			}
		}
		return count;
	}

	private IRI randomSubject() {
		return subjects.get(random.nextInt(subjects.size()));
	}

	private IRI randomPredicate() {
		return predicates.get(random.nextInt(predicates.size()));
	}

	private Resource randomContext() {
		return contexts.get(random.nextInt(contexts.size()));
	}

	@FunctionalInterface
	interface BenchmarkOperation {
		long run() throws Exception;
	}
}
