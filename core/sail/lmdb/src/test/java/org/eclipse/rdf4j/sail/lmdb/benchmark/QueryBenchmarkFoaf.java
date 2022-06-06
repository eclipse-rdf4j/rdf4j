/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmarks query performance with extended FOAF data.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 2)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G", "-Xmn1G", "-XX:+UseSerialGC" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class QueryBenchmarkFoaf extends BenchmarkBaseFoaf {
	private static final String query1, query2, query3;

	static {
		try {
			query1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query-persons-friends.qr"),
					StandardCharsets.UTF_8);
			query2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query-persons-count-friends-sorted.qr"),
					StandardCharsets.UTF_8);
			query3 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query-persons-count-friends.qr"),
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("QueryBenchmarkFoaf") // adapt to control which benchmark tests to run
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void setup() throws IOException {
		super.setup();

		// add 100,000 persons => 1,000,000 triples
		for (int i = 0; i < 10; i++) {
			connection.begin(IsolationLevels.NONE);
			for (int j = 0; j < 10000; j++) {
				addPerson();
			}
			connection.commit();
		}
		connection.close();

		connection = repository.getConnection();

		System.gc();
	}

	private static InputStream getResourceAsStream(String name) {
		return QueryBenchmarkFoaf.class.getClassLoader().getResourceAsStream(name);
	}

	@TearDown(Level.Trial)
	public void tearDown() throws IOException {
		super.tearDown();
		repository.shutDown();
	}

	@Benchmark
	public long personsAndFriends() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query1)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long groupByCount() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query2)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long groupByCountSorted() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query3)
					.evaluate()
					.stream()
					.count();
		}
	}
}
