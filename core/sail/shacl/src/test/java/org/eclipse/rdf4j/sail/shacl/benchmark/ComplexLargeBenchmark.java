/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
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

	private static final String transaction1;
	private static final String transaction2;
	private static final String transaction3;
	private static final String transaction4;

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

		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	private SailRepository repository;

	@Setup(Level.Invocation)
	public void setUp() throws InterruptedException, IOException {

		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.ERROR);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName())).setLevel(ch.qos.logback.classic.Level.ERROR);

		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));

			((ShaclSail) repository.getSail()).disableValidation();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
				connection.commit();
			}

			((ShaclSail) repository.getSail()).enableValidation();
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
	public void noPreloading() throws IOException {
		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));

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

		}

	}

	@Benchmark
	public void noPreloadingParallel() throws IOException {
		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));

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

		}

	}

	@Benchmark
	public void noPreloadingParallelNoCache() throws IOException {
		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));

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

		}
	}

	@Benchmark
	public void noPreloadingNonEmpty() throws IOException {

		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));
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
		}

	}

	@Benchmark
	public void noPreloadingNonEmptyParallel() throws IOException {

		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));
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

		}
	}

	@Benchmark
	public void noPreloadingRevalidate() throws IOException {

		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));

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
		}

	}

	@Benchmark
	public void noPreloadingRevalidateLowMem() throws IOException {

		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));

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
		}

	}

	@Benchmark
	public void noPreloadingBulk() throws IOException {
		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));

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

		}
	}

	@Benchmark
	public void noPreloadingBulkParallelCached() throws IOException {

		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));

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
	public void disabledValidationSail() throws IOException {
		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));
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
		}

	}

	@Benchmark
	public void disabledValidationTransaction() throws IOException {

		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "complexBenchmark/shacl.ttl"));

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
				connection.commit();
			}

			repository.shutDown();
		}

	}

	@Benchmark
	public void noShacl() throws IOException {

		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(Utils.getTestNotifyingSailNativeStore(temporaryFolder));

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				try (InputStream resourceAsStream = getData()) {
					connection.add(resourceAsStream, "", RDFFormat.TURTLE);
				}
				connection.commit();
			}

			repository.shutDown();
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
