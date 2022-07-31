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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Files;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
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
public class ReadCacheBenchmark {

	private static final File installLocation = Files.newTemporaryFolder();
	private static ElasticsearchClusterRunner runner;

	private SailRepository repoWithoutCache;
	private SailRepository repoWithCache;

	private static final String query1;
	private static final String query5;
	private static final String query6;

	static {
		try {
			query1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query1.qr"), StandardCharsets.UTF_8);
			query5 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query5.qr"), StandardCharsets.UTF_8);
			query6 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query6.qr"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {
		// JMH does not correctly set JAVA_HOME. Change the JAVA_HOME below if you the following error:
		// [EmbeddedElsHandler] INFO p.a.t.e.ElasticServer - could not find java; set JAVA_HOME or ensure java is in
		// PATH
		runner = TestHelpers.startElasticsearch(installLocation);

		repoWithoutCache = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex1",
						false));

		repoWithCache = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex2",
						true));

		try (SailRepositoryConnection connection = repoWithCache.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.clear();
			connection.add(getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"), "", RDFFormat.TURTLE);
			connection.commit();
		}

		try (SailRepositoryConnection connection = repoWithoutCache.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.clear();
			connection.add(getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"), "", RDFFormat.TURTLE);
			connection.commit();

		}

		System.gc();

	}

	private static InputStream getResourceAsStream(String name) {
		return ReadCacheBenchmark.class.getClassLoader().getResourceAsStream(name);
	}

	@TearDown(Level.Trial)
	public void afterClass() {
		repoWithoutCache.shutDown();
		TestHelpers.stopElasticsearch(runner);
	}

	@Benchmark
	public List<BindingSet> groupByQueryWithoutCache() {

		try (SailRepositoryConnection connection = repoWithoutCache.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query1)
					.evaluate());
		}
	}

	@Benchmark
	public List<BindingSet> groupByQueryWithCache() {

		try (SailRepositoryConnection connection = repoWithCache.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query1)
					.evaluate());
		}
	}

	@Benchmark
	public List<BindingSet> groupByQueryWithCacheCleared() {
		clearCache();

		try (SailRepositoryConnection connection = repoWithCache.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query1)
					.evaluate());
		}
	}

	@Benchmark
	public List<BindingSet> complexQueryWithoutCache() {

		try (SailRepositoryConnection connection = repoWithoutCache.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query5)
					.evaluate());
		}
	}

	@Benchmark
	public List<BindingSet> complexQueryWithCache() {

		try (SailRepositoryConnection connection = repoWithCache.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query5)
					.evaluate());
		}
	}

	@Benchmark
	public List<BindingSet> complexQueryWithCacheCleared() {
		clearCache();

		try (SailRepositoryConnection connection = repoWithCache.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query5)
					.evaluate());
		}
	}

	@Benchmark
	public List<BindingSet> pathQueryWithoutCache() {

		try (SailRepositoryConnection connection = repoWithoutCache.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query6)
					.evaluate());
		}
	}

	@Benchmark
	public List<BindingSet> pathQueryWithCache() {

		try (SailRepositoryConnection connection = repoWithCache.getConnection()) {
			return Iterations.asList(connection
					.prepareTupleQuery(query6)
					.evaluate());
		}
	}

	@Benchmark
	public List<BindingSet> pathQueryWithCacheCleared() {

		clearCache();

		try (SailRepositoryConnection connection = repoWithCache.getConnection()) {

			return Iterations.asList(connection
					.prepareTupleQuery(query6)
					.evaluate());
		}
	}

	private void clearCache() {
		try (SailRepositoryConnection connection = repoWithCache.getConnection()) {
			connection.begin();
			Literal literal = SimpleValueFactory.getInstance().createLiteral("jfiewojifjewo");
			connection.add(RDFS.RESOURCE, RDFS.LABEL, literal);
			connection.commit();
			connection.begin();
			connection.remove(RDFS.RESOURCE, RDFS.LABEL, literal);
			connection.commit();
		}
	}

}
