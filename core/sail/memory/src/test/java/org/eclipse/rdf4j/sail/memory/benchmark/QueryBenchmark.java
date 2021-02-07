/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class QueryBenchmark {

	private SailRepository repository;

	private static final String query1;
	private static final String query2;
	private static final String query3;
	private static final String query4;
	private static final String query7_pathexpression1;
	private static final String query8_pathexpression2;

	static {
		try {
			query1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query1.qr"), StandardCharsets.UTF_8);
			query2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query2.qr"), StandardCharsets.UTF_8);
			query3 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query3.qr"), StandardCharsets.UTF_8);
			query4 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query4.qr"), StandardCharsets.UTF_8);
			query7_pathexpression1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query7-pathexpression1.qr"),
					StandardCharsets.UTF_8);
			query8_pathexpression2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query8-pathexpression2.qr"),
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	List<Statement> statementList;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("QueryBenchmark") // adapt to run other benchmark tests
				// .addProfiler("stack", "lines=20;period=1;top=20")
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Invocation)
	public void beforeClass() throws IOException, InterruptedException {

		repository = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"), "", RDFFormat.TURTLE);
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {

			statementList = Iterations.asList(connection.getStatements(null, RDF.TYPE, null, false));
		}

		System.gc();

	}

	private static InputStream getResourceAsStream(String name) {
		return QueryBenchmark.class.getClassLoader().getResourceAsStream(name);
	}

	@TearDown(Level.Invocation)
	public void afterClass() {

		repository.shutDown();

	}

	@Benchmark
	public List<BindingSet> groupByQuery() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query1)
					.evaluate());
		}
	}

	@Benchmark
	public boolean simpleUpdateQueryIsolationReadCommitted() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.prepareUpdate(query2).execute();
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.prepareUpdate(query3).execute();
			connection.commit();
		}
		return hasStatement();

	}

	@Benchmark
	public boolean simpleUpdateQueryIsolationNone() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.prepareUpdate(query2).execute();
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.prepareUpdate(query3).execute();
			connection.commit();
		}
		return hasStatement();

	}

	@Benchmark
	public boolean removeByQuery() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.remove((Resource) null, RDF.TYPE, null);
			connection.commit();
		}
		return hasStatement();

	}

	@Benchmark
	public boolean removeByQueryReadCommitted() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.remove((Resource) null, RDF.TYPE, null);
			connection.commit();
		}
		return hasStatement();

	}

	@Benchmark
	public List<BindingSet> pathExpressionQuery1() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query7_pathexpression1)
					.evaluate());
		}
	}

	@Benchmark
	public List<BindingSet> pathExpressionQuery2() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query8_pathexpression2)
					.evaluate());
		}
	}

	private boolean hasStatement() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection.hasStatement(RDF.TYPE, RDF.TYPE, RDF.TYPE, true);
		}
	}

}
