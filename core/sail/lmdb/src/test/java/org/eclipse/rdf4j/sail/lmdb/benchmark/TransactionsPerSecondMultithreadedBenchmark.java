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
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmarks insertion performance with synthetic data using multiple threads.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.Throughput })
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G", "-XX:+UseG1GC" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(4)
public class TransactionsPerSecondMultithreadedBenchmark {

	RandomLiteralGenerator literalGenerator;
	Random random;
	int i;
	List<IRI> resources;
	List<IRI> predicates;
	protected SailRepository repository;
	protected File file;
	protected boolean forceSync = false;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("TransactionsPerSecondBenchmark\\.") // adapt to control which benchmarks to run
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Iteration)
	public void beforeClass() {
		i = 0;
		file = Files.newTemporaryFolder();

		LmdbStore sail = new LmdbStore(file, ConfigUtil.createConfig().setForceSync(forceSync));
		repository = new SailRepository(sail);
		random = new Random(1337);
		try (SailRepositoryConnection connection = repository.getConnection()) {

			literalGenerator = new RandomLiteralGenerator(connection.getValueFactory(), random);
			resources = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				resources.add(connection.getValueFactory().createIRI("some:resource-" + i));
			}
			predicates = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				predicates.add(connection.getValueFactory().createIRI("some:predicate-" + i));
			}
		}

		System.gc();
	}

	IRI randomResource() {
		return resources.get(random.nextInt(resources.size()));
	}

	IRI randomPredicate() {
		return predicates.get(random.nextInt(predicates.size()));
	}

	@TearDown(Level.Iteration)
	public void afterClass() throws IOException {
		repository.shutDown();
		FileUtils.deleteDirectory(file);

	}

	@Benchmark
	public void transactions() {
		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin();
			connection.add(randomResource(), randomPredicate(), literalGenerator.createRandomLiteral());
			connection.commit();
		}
	}

	@Benchmark
	public void transactionsLevelNone() {
		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			connection.add(randomResource(), randomPredicate(), literalGenerator.createRandomLiteral());
			connection.commit();
		}
	}

	@Benchmark
	public void mediumTransactionsLevelNone() {
		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			for (int k = 0; k < 10; k++) {
				connection.add(randomResource(), randomPredicate(), literalGenerator.createRandomLiteral());
			}
			connection.commit();
		}
	}

	@Benchmark
	public void mediumTransactionsLevelSnapshot() {
		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			for (int k = 0; k < 10; k++) {
				connection.add(randomResource(), randomPredicate(), literalGenerator.createRandomLiteral());
			}
			connection.commit();
		}
	}

	@Benchmark
	public void mediumTransactionsLevelSerializable() {
		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SERIALIZABLE);
			for (int k = 0; k < 10; k++) {
				connection.add(randomResource(), randomPredicate(), literalGenerator.createRandomLiteral());
			}
			connection.commit();
		}
	}

	@Benchmark
	public void largerTransaction() {
		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin();
			for (int k = 0; k < 10000; k++) {
				connection.add(randomResource(), randomPredicate(), literalGenerator.createRandomLiteral());
			}
			connection.commit();
		}
	}

	@Benchmark
	public void largerTransactionLevelNone() {
		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			for (int k = 0; k < 10000; k++) {
				connection.add(randomResource(), randomPredicate(), literalGenerator.createRandomLiteral());
			}
			connection.commit();
		}
	}

	@Benchmark
	public void veryLargerTransactionLevelNone() {
		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			for (int k = 0; k < 1000000; k++) {
				connection.add(randomResource(), randomPredicate(), literalGenerator.createRandomLiteral());
			}
			connection.commit();
		}
	}
}
