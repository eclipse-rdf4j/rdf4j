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
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
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
 * Concurrent benchmark for LmdbStore to measure performance under multi-threaded workloads.
 * <p>
 * This benchmark tests:
 * <ul>
 * <li>Concurrent read performance (multiple reader threads)</li>
 * <li>Concurrent write performance (multiple writer threads)</li>
 * <li>Mixed read/write concurrency (reader-writer contention)</li>
 * <li>Transaction isolation level impact on concurrency</li>
 * </ul>
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@BenchmarkMode({ Mode.Throughput })
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G", "-XX:+UseG1GC" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
public class LmdbStoreConcurrentBenchmark {

	private static final String NAMESPACE = "http://example.org/";
	private static final int PRELOAD_STATEMENTS = 100_000;

	private SailRepository repository;
	private File dataDir;
	private ValueFactory vf;

	// Pre-generated test data
	private List<IRI> subjects;
	private List<IRI> predicates;
	private List<IRI> types;
	private List<Resource> contexts;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("LmdbStoreConcurrentBenchmark\\.")
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void setupTrial() throws IOException {
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

		LmdbStoreConfig config = ConfigUtil.createConfig();
		repository = new SailRepository(new LmdbStore(dataDir, config));

		// Preload data
		try (SailRepositoryConnection conn = repository.getConnection()) {
			conn.begin(IsolationLevels.NONE);
			Random preloadRandom = new Random(42);
			RandomLiteralGenerator gen = new RandomLiteralGenerator(vf, preloadRandom);
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
	}

	@TearDown(Level.Trial)
	public void teardownTrial() throws IOException {
		if (repository != null) {
			repository.shutDown();
		}
		FileUtils.deleteDirectory(dataDir);
	}

	/**
	 * Thread-local state for each benchmark thread.
	 */
	@State(Scope.Thread)
	public static class ThreadState {
		SailRepositoryConnection connection;
		Random random;
		RandomLiteralGenerator literalGenerator;
		int threadId;
		private static int threadCounter = 0;

		@Setup(Level.Trial)
		public void setup(LmdbStoreConcurrentBenchmark benchmark) {
			connection = benchmark.repository.getConnection();
			threadId = threadCounter++;
			random = new Random(42 + threadId);
			literalGenerator = new RandomLiteralGenerator(benchmark.vf, random);
		}

		@TearDown(Level.Trial)
		public void teardown() {
			if (connection != null) {
				connection.close();
			}
		}

		IRI randomSubject(LmdbStoreConcurrentBenchmark benchmark) {
			return benchmark.subjects.get(random.nextInt(benchmark.subjects.size()));
		}

		IRI randomPredicate(LmdbStoreConcurrentBenchmark benchmark) {
			return benchmark.predicates.get(random.nextInt(benchmark.predicates.size()));
		}

		Resource randomContext(LmdbStoreConcurrentBenchmark benchmark) {
			return benchmark.contexts.get(random.nextInt(benchmark.contexts.size()));
		}
	}

	// ==================== CONCURRENT READ BENCHMARKS ====================

	/**
	 * Concurrent reads with 4 threads.
	 */
	@Benchmark
	@Group("concurrentReads4")
	@GroupThreads(4)
	public long concurrentReads4(ThreadState state, Blackhole bh) {
		long count = 0;
		IRI subject = state.randomSubject(this);
		try (CloseableIteration<Statement> iter = state.connection.getStatements(
				subject, null, null, false)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
			}
		}
		return count;
	}

	/**
	 * Concurrent reads with 8 threads.
	 */
	@Benchmark
	@Group("concurrentReads8")
	@GroupThreads(8)
	public long concurrentReads8(ThreadState state, Blackhole bh) {
		long count = 0;
		IRI subject = state.randomSubject(this);
		try (CloseableIteration<Statement> iter = state.connection.getStatements(
				subject, null, null, false)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
			}
		}
		return count;
	}

	// ==================== CONCURRENT WRITE BENCHMARKS ====================

	/**
	 * Concurrent writes with 2 threads (LMDB allows single writer).
	 */
	@Benchmark
	@Group("concurrentWrites2")
	@GroupThreads(2)
	public void concurrentWrites2(ThreadState state) {
		state.connection.begin(IsolationLevels.NONE);
		state.connection.add(
				state.randomSubject(this),
				state.randomPredicate(this),
				state.literalGenerator.createRandomLiteral(),
				state.randomContext(this));
		state.connection.commit();
	}

	/**
	 * Concurrent writes with 4 threads.
	 */
	@Benchmark
	@Group("concurrentWrites4")
	@GroupThreads(4)
	public void concurrentWrites4(ThreadState state) {
		state.connection.begin(IsolationLevels.NONE);
		state.connection.add(
				state.randomSubject(this),
				state.randomPredicate(this),
				state.literalGenerator.createRandomLiteral(),
				state.randomContext(this));
		state.connection.commit();
	}

	// ==================== MIXED READ/WRITE BENCHMARKS ====================

	/**
	 * Mixed workload: 4 readers, 1 writer.
	 */
	@Benchmark
	@Group("mixed4r1w")
	@GroupThreads(4)
	public long mixedReader(ThreadState state, Blackhole bh) {
		long count = 0;
		IRI subject = state.randomSubject(this);
		try (CloseableIteration<Statement> iter = state.connection.getStatements(
				subject, null, null, false)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
			}
		}
		return count;
	}

	@Benchmark
	@Group("mixed4r1w")
	@GroupThreads(1)
	public void mixedWriter(ThreadState state) {
		state.connection.begin(IsolationLevels.NONE);
		for (int i = 0; i < 10; i++) {
			state.connection.add(
					state.randomSubject(this),
					state.randomPredicate(this),
					state.literalGenerator.createRandomLiteral(),
					state.randomContext(this));
		}
		state.connection.commit();
	}

	/**
	 * Mixed workload: 8 readers, 2 writers.
	 */
	@Benchmark
	@Group("mixed8r2w")
	@GroupThreads(8)
	public long mixedReader8(ThreadState state, Blackhole bh) {
		long count = 0;
		IRI subject = state.randomSubject(this);
		try (CloseableIteration<Statement> iter = state.connection.getStatements(
				subject, null, null, false)) {
			while (iter.hasNext()) {
				bh.consume(iter.next());
				count++;
			}
		}
		return count;
	}

	@Benchmark
	@Group("mixed8r2w")
	@GroupThreads(2)
	public void mixedWriter8(ThreadState state) {
		state.connection.begin(IsolationLevels.NONE);
		for (int i = 0; i < 10; i++) {
			state.connection.add(
					state.randomSubject(this),
					state.randomPredicate(this),
					state.literalGenerator.createRandomLiteral(),
					state.randomContext(this));
		}
		state.connection.commit();
	}

	// ==================== ISOLATION LEVEL COMPARISON ====================

	/**
	 * Write with READ_COMMITTED isolation (default).
	 */
	@Benchmark
	@Group("isolationReadCommitted")
	@GroupThreads(2)
	public void writeReadCommitted(ThreadState state) {
		state.connection.begin(IsolationLevels.READ_COMMITTED);
		for (int i = 0; i < 10; i++) {
			state.connection.add(
					state.randomSubject(this),
					state.randomPredicate(this),
					state.literalGenerator.createRandomLiteral(),
					state.randomContext(this));
		}
		state.connection.commit();
	}

	/**
	 * Write with SNAPSHOT isolation.
	 */
	@Benchmark
	@Group("isolationSnapshot")
	@GroupThreads(2)
	public void writeSnapshot(ThreadState state) {
		state.connection.begin(IsolationLevels.SNAPSHOT);
		for (int i = 0; i < 10; i++) {
			state.connection.add(
					state.randomSubject(this),
					state.randomPredicate(this),
					state.literalGenerator.createRandomLiteral(),
					state.randomContext(this));
		}
		state.connection.commit();
	}

	/**
	 * Write with SERIALIZABLE isolation.
	 */
	@Benchmark
	@Group("isolationSerializable")
	@GroupThreads(2)
	public void writeSerializable(ThreadState state) {
		state.connection.begin(IsolationLevels.SERIALIZABLE);
		for (int i = 0; i < 10; i++) {
			state.connection.add(
					state.randomSubject(this),
					state.randomPredicate(this),
					state.literalGenerator.createRandomLiteral(),
					state.randomContext(this));
		}
		state.connection.commit();
	}
}
