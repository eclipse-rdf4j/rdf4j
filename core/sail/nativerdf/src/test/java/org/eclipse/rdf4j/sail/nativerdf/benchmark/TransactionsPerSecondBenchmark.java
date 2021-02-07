/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
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
@BenchmarkMode({ Mode.Throughput })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-XX:+UseG1GC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:+UseG1GC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TransactionsPerSecondBenchmark {

	private SailRepository repository;
	private File file;

	SailRepositoryConnection connection;
	int i;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("TransactionsPerSecondBenchmark") // adapt to control which benchmark tests to run
				// .addProfiler("stack", "lines=20;period=1;top=20")
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Iteration)
	public void beforeClass() {
		if (connection != null) {
			connection.close();
			connection = null;
		}
		i = 0;
		file = Files.newTemporaryFolder();

		NativeStore sail = new NativeStore(file, "spoc,ospc,psoc");
		sail.setForceSync(false);
		repository = new SailRepository(sail);
		connection = repository.getConnection();

		System.gc();

	}

	@TearDown(Level.Iteration)
	public void afterClass() throws IOException {
		if (connection != null) {
			connection.close();
			connection = null;
		}
		repository.shutDown();
		FileUtils.deleteDirectory(file);

	}

	@Benchmark
	public void transactions() {
		connection.begin();
		connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i++));
		connection.commit();
	}

	@Benchmark
	public void transactionsLevelNone() {
		connection.begin(IsolationLevels.NONE);
		connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i++));
		connection.commit();
	}

	@Benchmark
	public void mediumTransactionsLevelNone() {
		connection.begin(IsolationLevels.NONE);
		for (int k = 0; k < 10; k++) {
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i++ + "_" + k));
		}
		connection.commit();
	}

	@Benchmark
	public void largerTransaction() {
		connection.begin();
		for (int k = 0; k < 10000; k++) {
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i++ + "_" + k));
		}
		connection.commit();
	}

	@Benchmark
	public void largerTransactionLevelNone() {
		connection.begin(IsolationLevels.NONE);
		for (int k = 0; k < 10000; k++) {
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i++ + "_" + k));
		}
		connection.commit();
	}

	@Benchmark
	public void veryLargerTransactionLevelNone() {
		connection.begin(IsolationLevels.NONE);
		for (int k = 0; k < 1000000; k++) {
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i++ + "_" + k));
		}
		connection.commit();
	}
}
