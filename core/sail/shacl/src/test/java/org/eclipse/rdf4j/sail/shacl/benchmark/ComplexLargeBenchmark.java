/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.Utils;
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
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ComplexLargeBenchmark {

	private static final String transaction1;
	private static final String transaction2;
	private static final String transaction3;
	private static final String transaction4;
	private static final Model data;

	static {
		try {
			transaction1 = IOUtils.toString(
					ComplexLargeBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction1.qr"),
					StandardCharsets.UTF_8);
			transaction2 = IOUtils.toString(
					ComplexLargeBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction2.qr"),
					StandardCharsets.UTF_8);
			transaction3 = IOUtils.toString(
					ComplexLargeBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction3.qr"),
					StandardCharsets.UTF_8);
			transaction4 = IOUtils.toString(
					ComplexLargeBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction4.qr"),
					StandardCharsets.UTF_8);

			data = getData();

		} catch (IOException e) {
			throw new RuntimeException();
		}
	}


	@Setup(Level.Trial)
	public void setUp() throws InterruptedException {

		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.WARN);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName())).setLevel(ch.qos.logback.classic.Level.ERROR);


		System.gc();
		Thread.sleep(100);
	}

	@TearDown(Level.Trial)
	public void teardown() {
	}

	@Benchmark
	public void noPreloading() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setParallelValidation(false);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(true);
//			((ShaclSail) repository.getSail()).setPerformanceLogging(true);
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(data);
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noPreloadingParallel() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setParallelValidation(true);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(true);
			((ShaclSail) repository.getSail()).setPerformanceLogging(true);
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(data);
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noPreloadingParallelNoCache() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setParallelValidation(true);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(false);
//			((ShaclSail) repository.getSail()).setPerformanceLogging(true);
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(data);
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noPreloadingNonEmpty() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).disableValidation();
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				SimpleValueFactory vf = SimpleValueFactory.getInstance();
				connection.add(vf.createBNode(), vf.createIRI("http://fjljfiwoejfoiwefiew/a"), vf.createBNode());
				connection.commit();
			}
			((ShaclSail) repository.getSail()).enableValidation();

			((ShaclSail) repository.getSail()).setParallelValidation(false);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(true);
//			((ShaclSail) repository.getSail()).setPerformanceLogging(true);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(data);
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noPreloadingNonEmptyParallel() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);
			((ShaclSail) repository.getSail()).setParallelValidation(true);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(true);
			((ShaclSail) repository.getSail()).setPerformanceLogging(false);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
				SimpleValueFactory vf = SimpleValueFactory.getInstance();
				connection.add(vf.createBNode(), vf.createIRI("http://fjljfiwoejfoiwefiew/a"), vf.createBNode());
				connection.commit();
			}

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(data);
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noPreloadingRevalidate() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setParallelValidation(true);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(true);
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			((ShaclSail) repository.getSail()).disableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(data);
				connection.commit();
			}

			((ShaclSail) repository.getSail()).enableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				((ShaclSailConnection) connection.getSailConnection()).revalidate();
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noPreloadingRevalidateLowMem() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setParallelValidation(false);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(false);
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			((ShaclSail) repository.getSail()).disableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(data);
				connection.commit();
			}

			((ShaclSail) repository.getSail()).enableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				((ShaclSailConnection) connection.getSailConnection()).revalidate();
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noPreloadingBulk() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setParallelValidation(true);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(true);
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Bulk);
				connection.add(data);
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noPreloadingBulkParallelCached() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setParallelValidation(false);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(false);
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE,
						ShaclSail.TransactionSettings.ValidationApproach.Bulk,
						ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation,
						ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled);
				connection.add(data);
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noPreloadingTransactionalValidationLimit() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(600000);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
				connection.commit();
			}

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation,
						ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled);
				connection.add(data);
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noPreloadingRevalidateNativeStore() throws IOException {
		File file = Files.newTemporaryFolder();

		try {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSail(new NativeStore(file, "spoc,ospc,psoc"),
							"complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setParallelValidation(true);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(true);
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			((ShaclSail) repository.getSail()).disableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(data);
				connection.commit();
			}

			((ShaclSail) repository.getSail()).enableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				((ShaclSailConnection) connection.getSailConnection()).revalidate();
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			FileUtils.deleteDirectory(file);

		}

	}

	@Benchmark
	public void disabledValidationSail() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));
			((ShaclSail) repository.getSail()).disableValidation();
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(data);
				connection.commit();
			}

			((ShaclSail) repository.getSail()).enableValidation();

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void disabledValidationTransaction() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));
			((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
				connection.add(data);
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noShacl() {
		SailRepository repository = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(data);
			connection.commit();
		}

		repository.shutDown();
	}

	private static Model getData() {
		ClassLoader classLoader = ComplexLargeBenchmark.class.getClassLoader();
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(
				classLoader.getResourceAsStream("complexBenchmark/datagovbe-valid.ttl"))) {
			return Rio.parse(bufferedInputStream, RDFFormat.TURTLE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

//	public static void main(String[] args) throws InterruptedException {
//		ComplexLargeBenchmark complexLargeBenchmark = new ComplexLargeBenchmark();
//		complexLargeBenchmark.setUp();
//		while(true){
//			complexLargeBenchmark.noPreloadingNonEmpty();
//			System.out.println(".");
//		}
//	}
}
