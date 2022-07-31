/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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
import java.util.Arrays;
import java.util.List;
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
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
// use UseSerialGC to make GC more evident
@Fork(value = 1, jvmArgs = { "-Xms400M", "-Xmx400M", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms400M", "-Xmx400M", "-XX:+UseSerialGC",  "-XX:StartFlightRecording=delay=60s,duration=240s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SortBenchmark {

	private SailRepository repository;

	private static final String query9;

	static {
		try {

			query9 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query9.qr"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	List<Value> valuesList;

	public static void main(String[] args) throws RunnerException, IOException, InterruptedException {
		Options opt = new OptionsBuilder()
				.include("SortBenchmark.*") // adapt to run other benchmark tests
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
				valuesList = stream
						.map(Statement::getObject)
						.collect(Collectors.toList());
			}
		}

	}

	private static InputStream getResourceAsStream(String name) {
		return SortBenchmark.class.getClassLoader().getResourceAsStream(name);
	}

	@TearDown(Level.Trial)
	public void tearDown() {

		repository.shutDown();

	}

	@Benchmark
	public List<BindingSet> sortByQuery() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			try (Stream<BindingSet> stream = connection
					.prepareTupleQuery(query9)
					.evaluate()
					.stream()) {
				return stream.limit(1).collect(Collectors.toList());
			}
		}
	}

	@Benchmark
	public Value sortGetStatements() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			try (Stream<Statement> stream = connection.getStatements(null, null, null, false).stream()) {
				Value[] values = stream
						.map(Statement::getObject)
						.toArray(Value[]::new);

				Arrays.parallelSort(values, new ValueComparator());

				return values[0];
			}
		}
	}

	@Benchmark
	public Value sortDirectly() {

		Value[] values = new ArrayList<>(valuesList).toArray(new Value[0]);

		Arrays.parallelSort(values, new ValueComparator());

		return values[0];

	}

}
