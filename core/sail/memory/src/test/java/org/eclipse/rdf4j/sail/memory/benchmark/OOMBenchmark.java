/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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
 * This is a special benchmark that counts the number of benchmark iterations that can be run before running out of
 * memory. The benchmarks will all fail because they will all eventually run out of memory because the Epsilon garbage
 * collector is a no-op collector. Each benchmark prints the number of iterations they manage before running out of
 * memory, this is the measurement that we care about. A higher number of iterations means that we produce less garbage.
 * It doesn't necessarily mean that we use less memory.
 *
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseEpsilonGC",
		"-XX:+AlwaysPreTouch" })
@Measurement(iterations = 1, time = 99999999)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class OOMBenchmark {

	private SailRepository repository;

	private static final String query9;
	int count = 0;
	List<Value> valuesList;

	static {
		try {
			query9 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query9.qr"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("OOMBenchmark.*") // adapt to run other benchmark tests
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void setup() throws IOException, InterruptedException {

		repository = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"), "", RDFFormat.TURTLE);
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			try (Stream<Statement> stream = connection.getStatements(null, null, null, false).stream()) {
				valuesList = new ArrayList<>((int) stream.count());
			}

			try (Stream<Statement> stream = connection.getStatements(null, null, null, false).stream()) {
				stream
						.map(Statement::getObject)
						.sorted(new ValueComparator())
						.forEach(valuesList::add);
			}

		}

	}

	@Setup(Level.Iteration)
	public void setupIteration() {
		count = 0;
	}

	@TearDown(Level.Trial)
	public void afterClass() {

		repository.shutDown();

	}

	@Benchmark
	public List<BindingSet> sortAllObjectsWithSparql() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			try (Stream<BindingSet> stream = connection
					.prepareTupleQuery(query9)
					.evaluate()
					.stream()) {
				List<BindingSet> collect = stream.limit(1).collect(Collectors.toList());
				incrementAndPrintCount();
				return collect;
			}
		}
	}

	@Benchmark
	public Value sortDirectly() {

		Collections.shuffle(valuesList, new Random(47583672));

		valuesList.sort(new ValueComparator());
		incrementAndPrintCount();

		return valuesList.get(0);

	}

	private void incrementAndPrintCount() {
		System.out.println("\nCount: " + (++count));
	}

	private static InputStream getResourceAsStream(String name) {
		return OOMBenchmark.class.getClassLoader().getResourceAsStream(name);
	}

}
