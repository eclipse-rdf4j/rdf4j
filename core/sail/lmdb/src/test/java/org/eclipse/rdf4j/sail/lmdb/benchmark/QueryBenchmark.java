/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
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
 * Benchmarks query performance with real data.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 2)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G", "-Xmn1G", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class QueryBenchmark {

	private SailRepository repository;

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private static final String query1;
	private static final String query2;
	private static final String query3;
	private static final String query4;
	private static final String query5;
	private static final String query7_pathexpression1;

	static {
		try {
			query1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query1.qr"), StandardCharsets.UTF_8);
			query2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query2.qr"), StandardCharsets.UTF_8);
			query3 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query3.qr"), StandardCharsets.UTF_8);
			query4 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query4.qr"), StandardCharsets.UTF_8);
			query5 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query5.qr"), StandardCharsets.UTF_8);
			query7_pathexpression1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query7-pathexpression1.qr"),
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	List<Statement> statementList;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("QueryBenchmark") // adapt to control which benchmark tests to run
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException {
		tempDir.create();
		File file = tempDir.newFolder();

		repository = new SailRepository(new LmdbStore(file, ConfigUtil.createConfig()));

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

	@TearDown(Level.Trial)
	public void afterClass() throws IOException {
		tempDir.delete();
		repository.shutDown();
	}

	@Benchmark
	public long groupByQuery() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query1)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long complexQuery() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query4)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long distinctPredicatesQuery() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query5)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long pathExpressionQuery1() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query7_pathexpression1)
					.evaluate()
					.stream()
					.count();

		}
	}

	@Benchmark
	public boolean removeByQuery() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.remove((Resource) null, RDF.TYPE, null);
			connection.commit();
			connection.begin(IsolationLevels.NONE);
			connection.add(statementList);
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
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.add(statementList);
			connection.commit();
		}
		return hasStatement();

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

	private boolean hasStatement() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection.hasStatement(RDF.TYPE, RDF.TYPE, RDF.TYPE, true);
		}
	}

}
