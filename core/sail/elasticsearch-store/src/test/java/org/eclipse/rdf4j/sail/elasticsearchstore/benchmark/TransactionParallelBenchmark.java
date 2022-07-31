/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.elasticsearchstore.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.util.Files;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.eclipse.rdf4j.sail.elasticsearchstore.TestHelpers;
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
public class TransactionParallelBenchmark {

	private static final File installLocation = Files.newTemporaryFolder();
	private static ElasticsearchClusterRunner runner;

	private SailRepository repository;

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {
		// JMH does not correctly set JAVA_HOME. Change the JAVA_HOME below if you the following error:
		// [EmbeddedElsHandler] INFO p.a.t.e.ElasticServer - could not find java; set JAVA_HOME or ensure java is in
		// PATH
		runner = TestHelpers.startElasticsearch(installLocation);

		repository = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex"));
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"), "", RDFFormat.TURTLE);
			connection.commit();
		}

		System.gc();

	}

	private static InputStream getResourceAsStream(String name) {
		return TransactionParallelBenchmark.class.getClassLoader().getResourceAsStream(name);
	}

	@TearDown(Level.Trial)
	public void afterClass() {
		repository.shutDown();
		TestHelpers.stopElasticsearch(runner);
	}

	@Benchmark
	public boolean transaction100SerialTransactions() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		IntStream
				.range(0, 100)
				.mapToObj(k -> IntStream
						.range(0, 10)
						.mapToObj(i -> vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)))
						.collect(Collectors.toList()))
				.collect(Collectors.toList())
				.forEach(list -> {

					try (SailRepositoryConnection connection = repository.getConnection()) {
						connection.begin(IsolationLevels.READ_COMMITTED);
						connection.add(list);
						connection.commit();
					}
				});

		return hasStatement();

	}

	@Benchmark
	public boolean transaction100ParallelTransactions() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		IntStream
				.range(0, 100)
				.mapToObj(k -> IntStream
						.range(0, 10)
						.mapToObj(i -> vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)))
						.collect(Collectors.toList()))
				.collect(Collectors.toList())
				.parallelStream()
				.forEach(list -> {

					try (SailRepositoryConnection connection = repository.getConnection()) {
						connection.begin(IsolationLevels.READ_COMMITTED);
						connection.add(list);
						connection.commit();
					}
				});

		return hasStatement();

	}

	@Benchmark
	public boolean transaction100ParallelTransactionsIsolationNone() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		IntStream
				.range(0, 100)
				.mapToObj(k -> IntStream
						.range(0, 10)
						.mapToObj(i -> vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)))
						.collect(Collectors.toList()))
				.collect(Collectors.toList())
				.parallelStream()
				.forEach(list -> {

					try (SailRepositoryConnection connection = repository.getConnection()) {
						connection.begin(IsolationLevels.NONE);
						connection.add(list);
						connection.commit();
					}
				});

		return hasStatement();

	}

	private boolean hasStatement() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection.hasStatement(RDF.TYPE, RDF.TYPE, RDF.TYPE, true);
		}
	}

}
