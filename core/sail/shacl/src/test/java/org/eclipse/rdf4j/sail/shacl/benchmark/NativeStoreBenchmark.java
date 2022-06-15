/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint.CacheDisabled;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint.SerialValidation;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.ValidationApproach.Bulk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
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
@Warmup(iterations = 0)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms35M", "-Xmx35M" })
//@Fork(value = 1, jvmArgs = { "-Xms35M", "-Xmx35M", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class NativeStoreBenchmark {

	@Setup(Level.Trial)
	public void setUp() throws InterruptedException {
		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.ERROR);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName())).setLevel(ch.qos.logback.classic.Level.ERROR);
		System.setProperty("org.eclipse.rdf4j.sail.shacl.sparqlValidation", "false");
	}

	@Benchmark
	public void shaclNativeStore() throws IOException {

		File file = Files.newTemporaryFolder();

		ShaclSail shaclSail = new ShaclSail(new NativeStore(file, "spoc,ospc,psoc"));

		runBenchmark(file, shaclSail, true, IsolationLevels.NONE, Bulk);

	}

	@Benchmark
	public void shaclNativeStoreLoadShapesFirst() throws IOException {

		File file = Files.newTemporaryFolder();

		ShaclSail shaclSail = new ShaclSail(new NativeStore(file, "spoc,ospc,psoc"));

		runBenchmark(file, shaclSail, false, IsolationLevels.NONE, Bulk);

	}

	@Benchmark
	public void shaclParallelNativeStore() throws IOException {

		File file = Files.newTemporaryFolder();

		ShaclSail shaclSail = new ShaclSail(new NativeStore(file, "spoc,ospc,psoc"));

		runBenchmark(file, shaclSail, true, IsolationLevels.NONE, Bulk, ParallelValidation, CacheDisabled);

	}

	@Benchmark
	public void shaclCacheNativeStore() throws IOException {

		File file = Files.newTemporaryFolder();

		ShaclSail shaclSail = new ShaclSail(new NativeStore(file, "spoc,ospc,psoc"));

		runBenchmark(file, shaclSail, true, IsolationLevels.NONE, Bulk, SerialValidation, CacheEnabled);
	}

	@Benchmark
	public void shaclCacheParallelNativeStore() throws IOException {

		File file = Files.newTemporaryFolder();

		ShaclSail shaclSail = new ShaclSail(new NativeStore(file, "spoc,ospc,psoc"));

		runBenchmark(file, shaclSail, true, IsolationLevels.NONE, Bulk, ParallelValidation, CacheEnabled);

	}

	private void runBenchmark(File file, ShaclSail shaclSail, boolean singleTransaction,
			TransactionSetting... transactionSettings) throws IOException {
		SailRepository sailRepository = new SailRepository(shaclSail);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			if (singleTransaction) {
				connection.begin(transactionSettings);
				try (InputStream inputStream = getFile("complexBenchmark/shacl.trig")) {
					connection.add(inputStream, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
				}

				try (InputStream inputStream = getFile("complexBenchmark/generated.ttl")) {
					connection.add(inputStream, "", RDFFormat.TRIG);
				}
				connection.commit();

			} else {
				connection.begin(transactionSettings);
				try (InputStream inputStream = getFile("complexBenchmark/shacl.trig")) {
					connection.add(inputStream, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
				}

				connection.commit();
				connection.begin(transactionSettings);

				try (InputStream inputStream = getFile("complexBenchmark/generated.ttl")) {
					connection.add(inputStream, "", RDFFormat.TRIG);
				}
				connection.commit();
			}

		}

		sailRepository.shutDown();

		FileUtils.deleteDirectory(file);
	}

	@Benchmark
	public void nativeStore() throws IOException {

		File file = Files.newTemporaryFolder();

		NotifyingSail nativeStore = new NativeStore(file, "spoc,ospc,psoc");

		SailRepository sailRepository = new SailRepository(nativeStore);
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(IsolationLevels.NONE);

			try (InputStream inputStream = getFile("complexBenchmark/shacl.trig")) {
				connection.add(inputStream, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			try (InputStream inputStream = getFile("complexBenchmark/generated.ttl")) {
				connection.add(inputStream, "", RDFFormat.TURTLE);
			}
			connection.commit();

		}

		sailRepository.shutDown();

		FileUtils.deleteDirectory(file);

	}

	@Benchmark // this should always run out of memory, as proof that we need the native store
	public void memoryStore() throws IOException {

		SailRepository sailRepository = new SailRepository(new MemoryStore());
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(IsolationLevels.NONE);

			try (InputStream inputStream = getFile("complexBenchmark/shacl.trig")) {
				connection.add(inputStream, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			try (InputStream inputStream = getFile("complexBenchmark/generated.ttl")) {
				connection.add(inputStream, "", RDFFormat.TRIG);
			}
			connection.commit();

		}

		sailRepository.shutDown();

	}

	private InputStream getFile(String s) {
		return NativeStoreBenchmark.class.getClassLoader().getResourceAsStream(s);
	}

}
