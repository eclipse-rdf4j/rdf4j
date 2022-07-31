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
import java.util.concurrent.TimeUnit;

import org.assertj.core.util.Files;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.eclipse.rdf4j.sail.elasticsearchstore.SingletonClientProvider;
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
//@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints" })
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class InitBenchmark {

	private static final File installLocation = Files.newTemporaryFolder();
	private static ElasticsearchClusterRunner runner;
	SingletonClientProvider clientPool;

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {
		// JMH does not correctly set JAVA_HOME. Change the JAVA_HOME below if you the following error:
		// [EmbeddedElsHandler] INFO p.a.t.e.ElasticServer - could not find java; set JAVA_HOME or ensure java is in
		// PATH
		runner = TestHelpers.startElasticsearch(installLocation);

		clientPool = new SingletonClientProvider("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER);
		System.gc();
	}

	@TearDown(Level.Trial)
	public void afterClass() throws Exception {
		clientPool.close();
		TestHelpers.stopElasticsearch(runner);
	}

	@Benchmark
	public void initWithElasticsearchClientCreation() {

		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex",
						false));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.clear();
			connection.commit();
		}

		elasticsearchStore.shutDown();

	}

	@Benchmark
	public void initWithoutElasticsearchClientCreation() {

		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore(clientPool, "testindex", false));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.clear();
			connection.commit();
		}

		elasticsearchStore.shutDown();

	}

}
