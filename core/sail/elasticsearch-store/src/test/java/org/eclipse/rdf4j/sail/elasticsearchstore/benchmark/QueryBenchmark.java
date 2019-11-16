/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.elasticsearchstore.benchmark;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.Iterations;
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
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.JavaHomeOption;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

	private static EmbeddedElastic embeddedElastic;

	private static File installLocation = Files.newTemporaryFolder();

	private SailRepository elasticsearchStore;

	private static final String query1;
	private static final String query2;
	private static final String query3;

	static {
		try {
			query1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query1.qr"), StandardCharsets.UTF_8);
			query2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query2.qr"), StandardCharsets.UTF_8);
			query3 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query3.qr"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {

		embeddedElastic = TestHelpers.startElasticsearch(installLocation,
				"/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home");

		elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"), "", RDFFormat.TURTLE);
			connection.commit();
		}

		System.gc();

	}

	private static InputStream getResourceAsStream(String name) {
		return QueryBenchmark.class.getClassLoader().getResourceAsStream(name);
	}

	@TearDown(Level.Trial)
	public void afterClass() {

		elasticsearchStore.shutDown();
		TestHelpers.stopElasticsearch(embeddedElastic, installLocation);

	}

	@Benchmark
	public List<BindingSet> groupByQuery() {

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			List<BindingSet> bindingSets = Iterations.asList(connection
					.prepareTupleQuery(query1)
					.evaluate());

			return bindingSets;
		}
	}

	@Benchmark
	public void simpleUpdateQuery() {

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.prepareUpdate(query2).execute();
		}

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.prepareUpdate(query3).execute();
		}
	}

	@Benchmark
	public void simpleUpdateQueryIsolationNone() {

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.prepareUpdate(query2).execute();
			connection.commit();
		}

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.prepareUpdate(query3).execute();
			connection.commit();
		}
	}

}
