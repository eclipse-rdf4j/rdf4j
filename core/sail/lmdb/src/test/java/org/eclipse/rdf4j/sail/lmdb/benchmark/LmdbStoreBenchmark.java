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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Comprehensive benchmark for LmdbStore read/write performance.
 * <p>
 * This benchmark measures:
 * <ul>
 * <li>Write performance: bulk loading, single statements, various transaction sizes</li>
 * <li>Read performance: statement pattern lookups with different selectivity</li>
 * <li>Index utilization: queries using different index patterns (SPOC, POSC, OSPC)</li>
 * <li>Value store operations: value lookups, cache hit rates</li>
 * </ul>
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@BenchmarkMode({ Mode.Throughput, Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G", "-XX:+UseG1GC" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class LmdbStoreBenchmark {

	private static final String NAMESPACE = "http://example.org/";
	private static final int PRELOAD_STATEMENTS = 100_000;

	private SailRepository repository;
	private SailRepositoryConnection connection;
	private File dataDir;
	private ValueFactory vf;
	private Random random;
	private RandomLiteralGenerator literalGenerator;

	// Pre-generated test data
	private List<IRI> subjects;
	private List<IRI> predicates;
	private List<IRI> types;
	private List<Resource> contexts;

	// Known values for targeted lookups
	private IRI knownSubject;
	private IRI knownPredicate;
	private IRI knownObject;
	private Resource knownContext;

	@Param({ "spoc,posc", "spoc,posc,ospc", "spoc,posc,ospc,cspo" })
	private String tripleIndexes;

	@Param({ "512", "2048", "8192" })
	private int valueCacheSize;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("LmdbStoreBenchmark\\.")
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void setupTrial() throws IOException {
		dataDir = Files.newTemporaryFolder();
		vf = SimpleValueFactory.getInstance();
		random = new Random(42); // Fixed seed for reproducibility
		literalGenerator = new RandomLiteralGenerator(vf, random);

		// Initialize test data collections
		subjects = new ArrayList<>(1000);
		predicates = new ArrayList<>(50);
		types = new ArrayList<>(20);
		contexts = new ArrayList<>(10);

		// Generate test IRIs
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

		// Store known values for targeted lookups
		knownSubject = subjects.get(0);
		knownPredicate = predicates.get(0);
		knownObject = types.get(0);
		knownContext = contexts.get(0);

		// Create and initialize repository
		LmdbStoreConfig config = new LmdbStoreConfig(tripleIndexes);
		config.setForceSync(false);
		config.setValueDBSize(1_073_741_824L); // 1 GiB
		config.setTripleDBSize(1_073_741_824L);
		config.setValueCacheSize(valueCacheSize);

		repository = new SailRepository(new LmdbStore(dataDir, config));
		connection = repository.getConnection();

		// Preload data for read benchmarks
		preloadData();
	}

	private void preloadData() {
		connection.begin(IsolationLevels.NONE);
		for (int i = 0; i < PRELOAD_STATEMENTS; i++) {
			IRI subject = subjects.get(i % subjects.size());
			IRI predicate = predicates.get(i % predicates.size());
			Resource context = contexts.get(i % contexts.size());

			if (i % 3 == 0) {
				// Add type statements
				connection.add(subject, RDF.TYPE, types.get(i % types.size()), context);
			} else if (i % 3 == 1) {
				// Add label statements
				connection.add(subject, RDFS.LABEL, literalGenerator.createRandomLiteral(), context);
			} else {
				// Add relationship statements
				IRI object = subjects.get((i + 1) % subjects.size());
				connection.add(subject, predicate, object, context);
			}
		}
		connection.commit();
	}

	@TearDown(Level.Trial)
	public void teardownTrial() throws IOException {
		if (connection != null) {
			connection.close();
		}
		if (repository != null) {
			repository.shutDown();
		}
		FileUtils.deleteDirectory(dataDir);
	}

	// ==================== WRITE BENCHMARKS ====================

	/**
	 * Benchmark single statement add with individual transactions.
	 */
	@Benchmark
	public void writeSingleStatement() {
		connection.begin(IsolationLevels.NONE);
		connection.add(
				randomSubject(),
				randomPredicate(),
				literalGenerator.createRandomLiteral(),
				randomContext());
		connection.commit();
	}

	/**
	 * Benchmark small batch writes (10 statements per transaction).
	 */
	@Benchmark
	public void writeSmallBatch() {
		connection.begin(IsolationLevels.NONE);
		for (int i = 0; i < 10; i++) {
			connection.add(
					randomSubject(),
					randomPredicate(),
					literalGenerator.createRandomLiteral(),
					randomContext());
		}
		connection.commit();
	}

	/**
	 * Benchmark medium batch writes (100 statements per transaction).
	 */
	@Benchmark
	public void writeMediumBatch() {
		connection.begin(IsolationLevels.NONE);
		for (int i = 0; i < 100; i++) {
			connection.add(
					randomSubject(),
					randomPredicate(),
					literalGenerator.createRandomLiteral(),
					randomContext());
		}
		connection.commit();
	}

	/**
	 * Benchmark large batch writes (1000 statements per transaction).
	 */
	@Benchmark
	public void writeLargeBatch() {
		connection.begin(IsolationLevels.NONE);
		for (int i = 0; i < 1000; i++) {
			connection.add(
					randomSubject(),
					randomPredicate(),
					literalGenerator.createRandomLiteral(),
					randomContext());
		}
		connection.commit();
	}

	/**
	 * Benchmark write with default isolation level (READ_COMMITTED).
	 */
	@Benchmark
	public void writeWithIsolation() {
		connection.begin(IsolationLevels.READ_COMMITTED);
		for (int i = 0; i < 100; i++) {
			connection.add(
					randomSubject(),
					randomPredicate(),
					literalGenerator.createRandomLiteral(),
					randomContext());
		}
		connection.commit();
	}

	// ==================== READ BENCHMARKS ====================

	/**
	 * Benchmark lookup by subject (uses SPOC index).
	 */
	@Benchmark
	public long readBySubject(Blackhole bh) {
		long count = 0;
		try (CloseableIteration<Statement> iter = connection.getStatements(
				knownSubject, null, null, false)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
			}
		}
		return count;
	}

	/**
	 * Benchmark lookup by predicate (uses POSC index if available).
	 */
	@Benchmark
	public long readByPredicate(Blackhole bh) {
		long count = 0;
		try (CloseableIteration<Statement> iter = connection.getStatements(
				null, knownPredicate, null, false)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
			}
		}
		return count;
	}

	/**
	 * Benchmark lookup by object (uses OSPC index if available).
	 */
	@Benchmark
	public long readByObject(Blackhole bh) {
		long count = 0;
		try (CloseableIteration<Statement> iter = connection.getStatements(
				null, null, knownObject, false)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
			}
		}
		return count;
	}

	/**
	 * Benchmark lookup by context (uses CSPO index if available).
	 */
	@Benchmark
	public long readByContext(Blackhole bh) {
		long count = 0;
		try (CloseableIteration<Statement> iter = connection.getStatements(
				null, null, null, false, knownContext)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
			}
		}
		return count;
	}

	/**
	 * Benchmark lookup by subject and predicate (highly selective).
	 */
	@Benchmark
	public long readBySubjectPredicate(Blackhole bh) {
		long count = 0;
		try (CloseableIteration<Statement> iter = connection.getStatements(
				knownSubject, knownPredicate, null, false)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
			}
		}
		return count;
	}

	/**
	 * Benchmark lookup by predicate and object.
	 */
	@Benchmark
	public long readByPredicateObject(Blackhole bh) {
		long count = 0;
		try (CloseableIteration<Statement> iter = connection.getStatements(
				null, RDF.TYPE, knownObject, false)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
			}
		}
		return count;
	}

	/**
	 * Benchmark full scan (no constraints).
	 */
	@Benchmark
	public long readFullScan(Blackhole bh) {
		long count = 0;
		try (CloseableIteration<Statement> iter = connection.getStatements(
				null, null, null, false)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
				if (count >= 10000) {
					break; // Limit to avoid excessive runtime
				}
			}
		}
		return count;
	}

	/**
	 * Benchmark random subject lookups (simulates cache behavior).
	 */
	@Benchmark
	public long readRandomSubjects(Blackhole bh) {
		long count = 0;
		for (int i = 0; i < 100; i++) {
			IRI subject = subjects.get(random.nextInt(subjects.size()));
			try (CloseableIteration<Statement> iter = connection.getStatements(
					subject, null, null, false)) {
				while (iter.hasNext()) {
					bh.consume(iter.next());
					count++;
				}
			}
		}
		return count;
	}

	// ==================== MIXED WORKLOAD BENCHMARKS ====================

	/**
	 * Benchmark mixed read/write workload (80% read, 20% write).
	 */
	@Benchmark
	public long mixedWorkload(Blackhole bh) {
		long count = 0;
		for (int i = 0; i < 100; i++) {
			if (random.nextInt(100) < 80) {
				// Read operation
				IRI subject = subjects.get(random.nextInt(subjects.size()));
				try (CloseableIteration<Statement> iter = connection.getStatements(
						subject, null, null, false)) {
					while (iter.hasNext()) {
						bh.consume(iter.next());
						count++;
					}
				}
			} else {
				// Write operation
				connection.begin(IsolationLevels.NONE);
				connection.add(
						randomSubject(),
						randomPredicate(),
						literalGenerator.createRandomLiteral(),
						randomContext());
				connection.commit();
			}
		}
		return count;
	}

	// ==================== DELETE BENCHMARKS ====================

	/**
	 * Benchmark statement removal.
	 */
	@Benchmark
	public void deleteStatements() {
		// First add some statements
		connection.begin(IsolationLevels.NONE);
		IRI subject = vf.createIRI(NAMESPACE, "temp-subject-" + random.nextInt(10000));
		for (int i = 0; i < 10; i++) {
			connection.add(subject, randomPredicate(), literalGenerator.createRandomLiteral());
		}
		connection.commit();

		// Then remove them
		connection.begin(IsolationLevels.NONE);
		connection.remove(subject, null, null);
		connection.commit();
	}

	// ==================== HELPER METHODS ====================

	private IRI randomSubject() {
		return subjects.get(random.nextInt(subjects.size()));
	}

	private IRI randomPredicate() {
		return predicates.get(random.nextInt(predicates.size()));
	}

	private Resource randomContext() {
		return contexts.get(random.nextInt(contexts.size()));
	}
}
