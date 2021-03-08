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
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
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
@Warmup(iterations = 10)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ComplexLargeBenchmark {
	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	private static String transaction1;
	private static String transaction2;
	private static String transaction3;
	private static String transaction4;

	static {
		try {
			transaction1 = IOUtils.toString(
					ComplexLargeBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction1.qr"),
					"utf-8");
			transaction2 = IOUtils.toString(
					ComplexLargeBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction2.qr"),
					"utf-8");
			transaction3 = IOUtils.toString(
					ComplexLargeBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction3.qr"),
					"utf-8");
			transaction4 = IOUtils.toString(
					ComplexLargeBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction4.qr"),
					"utf-8");

		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	private SailRepository repository;

	@Setup(Level.Invocation)
	public void setUp() throws InterruptedException {

		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.ERROR);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName())).setLevel(ch.qos.logback.classic.Level.ERROR);

		try {
			repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).disableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
				connection.commit();
			}

			((ShaclSail) repository.getSail()).enableValidation();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		System.gc();
		Thread.sleep(100);
	}

	@TearDown(Level.Invocation)
	public void teardown() {
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Benchmark
	public void shaclParallelCacheTwoTransactionPreloaded() {

		((ShaclSail) repository.getSail()).setParallelValidation(true);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(true);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction1).execute();
			connection.commit();

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction2).execute();
			connection.commit();

		}

	}

	@Benchmark
	public void shaclNothingToValidateTransactionsPreloaded() {

		((ShaclSail) repository.getSail()).setParallelValidation(false);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.add(connection.getValueFactory().createBNode(), RDFS.LABEL,
					connection.getValueFactory().createLiteral(""));
			connection.commit();

		}

	}

	@Benchmark
	public void shaclParallelTwoTransactionPreloaded() {

		((ShaclSail) repository.getSail()).setParallelValidation(true);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(false);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction1).execute();
			connection.commit();

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction2).execute();
			connection.commit();

		}

	}

	@Benchmark
	public void shaclCacheTwoTransactionPreloaded() {

		((ShaclSail) repository.getSail()).setParallelValidation(false);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction1).execute();
			connection.commit();

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction2).execute();
			connection.commit();

		}

	}

	@Benchmark
	public void shaclTwoTransactionPreloaded() {

		((ShaclSail) repository.getSail()).setParallelValidation(false);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(false);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction1).execute();
			connection.commit();

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction2).execute();
			connection.commit();

		}

	}

	@Benchmark
	public void noPreloading() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).setParallelValidation(false);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(true);
//			((ShaclSail) repository.getSail()).setPerformanceLogging(true);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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
//			((ShaclSail) repository.getSail()).setPerformanceLogging(true);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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
			((ShaclSail) repository.getSail()).disableValidation();
			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				SimpleValueFactory vf = SimpleValueFactory.getInstance();
				connection.add(vf.createBNode(), vf.createIRI("http://fjljfiwoejfoiwefiew/a"), vf.createBNode());
				connection.commit();
			}
			((ShaclSail) repository.getSail()).enableValidation();

			((ShaclSail) repository.getSail()).setParallelValidation(true);
			((ShaclSail) repository.getSail()).setCacheSelectNodes(true);
//			((ShaclSail) repository.getSail()).setPerformanceLogging(true);

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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

			((ShaclSail) repository.getSail()).disableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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

			((ShaclSail) repository.getSail()).disableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Bulk);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE,
						ShaclSail.TransactionSettings.ValidationApproach.Bulk,
						ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation,
						ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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

			((ShaclSail) repository.getSail()).disableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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
	public void shaclParallelCacheDeletionPreloaded() {

		((ShaclSail) repository.getSail()).setParallelValidation(true);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(true);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction3).execute();
			connection.commit();

		}

	}

	@Benchmark
	public void shaclParallelCacheUpdatePreloaded() {

		((ShaclSail) repository.getSail()).setParallelValidation(true);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(true);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction4).execute();
			connection.commit();

		}

	}

	@Benchmark
	public void shaclCacheDeletionPreloaded() {

		((ShaclSail) repository.getSail()).setParallelValidation(false);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(true);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction3).execute();
			connection.commit();

		}

	}

	@Benchmark
	public void shaclCacheUpdatePreloaded() {

		((ShaclSail) repository.getSail()).setParallelValidation(false);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(true);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction4).execute();
			connection.commit();

		}

	}

	@Benchmark
	public void disabledValidationSail() {

		try {
			SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));
			((ShaclSail) repository.getSail()).disableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
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

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	public void noShacl() {

		try {
			SailRepository repository = new SailRepository(new MemoryStore());

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
				connection.commit();
			}

			repository.shutDown();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private static BufferedInputStream getData() {
		ClassLoader classLoader = ComplexLargeBenchmark.class.getClassLoader();
		return new BufferedInputStream(classLoader.getResourceAsStream("complexBenchmark/datagovbe-valid.ttl"));
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
