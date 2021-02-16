/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
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
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms45M", "-Xmx45M", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = { "-Xms45M", "-Xmx45M", "-XX:+UseSerialGC", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class NativeStoreBenchmark {
	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	@Setup(Level.Iteration)
	public void setUp() throws InterruptedException {
		System.gc();
		Thread.sleep(100);
		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.ERROR);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName())).setLevel(ch.qos.logback.classic.Level.ERROR);
	}

	@Benchmark
	public void shaclNativeStore() throws IOException {

		File file = Files.newTemporaryFolder();

		ShaclSail shaclSail = new ShaclSail(new NativeStore(file, "spoc,ospc,psoc"));

		// significantly reduce required memory
		shaclSail.setCacheSelectNodes(false);

		// run validation in parallel as much as possible,
		// this can be disabled to reduce memory load further
		shaclSail.setParallelValidation(false);

		runBenchmark(file, shaclSail);

	}

	@Benchmark
	public void shaclParallelNativeStore() throws IOException {

		File file = Files.newTemporaryFolder();

		ShaclSail shaclSail = new ShaclSail(new NativeStore(file, "spoc,ospc,psoc"));

		// significantly reduce required memory
		shaclSail.setCacheSelectNodes(false);

		// run validation in parallel as much as possible,
		// this can be disabled to reduce memory load further
		shaclSail.setParallelValidation(true);

		runBenchmark(file, shaclSail);

	}

	@Benchmark
	public void shaclCacheNativeStore() throws IOException {

		File file = Files.newTemporaryFolder();

		ShaclSail shaclSail = new ShaclSail(new NativeStore(file, "spoc,ospc,psoc"));

		// significantly reduce required memory
		shaclSail.setCacheSelectNodes(true);

		// run validation in parallel as much as possible,
		// this can be disabled to reduce memory load further
		shaclSail.setParallelValidation(false);

		runBenchmark(file, shaclSail);

	}

	@Benchmark
	public void shaclCacheParallelNativeStore() throws IOException {

		File file = Files.newTemporaryFolder();

		ShaclSail shaclSail = new ShaclSail(new NativeStore(file, "spoc,ospc,psoc"));

		// significantly reduce required memory
		shaclSail.setCacheSelectNodes(true);

		// run validation in parallel as much as possible,
		// this can be disabled to reduce memory load further
		shaclSail.setParallelValidation(true);

		runBenchmark(file, shaclSail);

	}

	private void runBenchmark(File file, ShaclSail shaclSail) throws IOException {
		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();
		shaclSail.disableValidation();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			try (InputStream inputStream = getFile("complexBenchmark/shacl.ttl")) {
				connection.add(inputStream, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}
			connection.commit();

			connection.begin(IsolationLevels.NONE);

			try (InputStream inputStream = new BufferedInputStream(getFile("complexBenchmark/generated.ttl"))) {
				connection.add(inputStream, "", RDFFormat.TURTLE);
			}
			connection.commit();

		}
		shaclSail.enableValidation();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
			connection.commit();

			if (!revalidate.conforms()) {
				Rio.write(revalidate.asModel(), System.out, RDFFormat.TURTLE);
			}

		}

		sailRepository.shutDown();

		FileUtils.deleteDirectory(file);
	}

	@Benchmark
	public void nativeStore() throws IOException {

		File file = Files.newTemporaryFolder();

		NotifyingSail shaclSail = new NativeStore(file, "spoc,ospc,psoc");

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(IsolationLevels.NONE);

			try (InputStream inputStream = getFile("complexBenchmark/shacl.ttl")) {
				connection.add(inputStream, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}
			connection.commit();

			connection.begin(IsolationLevels.NONE);

			try (InputStream inputStream = new BufferedInputStream(getFile("complexBenchmark/generated.ttl"))) {
				connection.add(inputStream, "", RDFFormat.TURTLE);
			}
			connection.commit();

		}

		sailRepository.shutDown();

		FileUtils.deleteDirectory(file);

	}

	@Benchmark // this should always run out of memory, as proof that we need the native store
	public void memoryStore() throws IOException {

		NotifyingSail shaclSail = new MemoryStore();

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(IsolationLevels.NONE);

			try (InputStream inputStream = getFile("complexBenchmark/shacl.ttl")) {
				connection.add(inputStream, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}
			connection.commit();

			connection.begin(IsolationLevels.NONE);

			try (InputStream inputStream = new BufferedInputStream(getFile("complexBenchmark/generated.ttl"))) {
				connection.add(inputStream, "", RDFFormat.TURTLE);
			}
			connection.commit();

		}

		sailRepository.shutDown();

	}

	private InputStream getFile(String s) {
		return NativeStoreBenchmark.class.getClassLoader()
				.getResourceAsStream(s);
	}

}
