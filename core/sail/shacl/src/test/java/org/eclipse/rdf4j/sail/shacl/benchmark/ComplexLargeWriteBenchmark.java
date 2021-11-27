/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
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
@Warmup(iterations = 0)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ComplexLargeWriteBenchmark {
	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	private static final String transaction1;
	private static final String transaction2;
	private static final String transaction3;
	private static final String transaction4;
	private static final Model data;

	static {
		try {
			transaction1 = IOUtils.toString(
					ComplexLargeWriteBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction1.qr"),
					StandardCharsets.UTF_8);
			transaction2 = IOUtils.toString(
					ComplexLargeWriteBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction2.qr"),
					StandardCharsets.UTF_8);
			transaction3 = IOUtils.toString(
					ComplexLargeWriteBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction3.qr"),
					StandardCharsets.UTF_8);
			transaction4 = IOUtils.toString(
					ComplexLargeWriteBenchmark.class.getClassLoader()
							.getResourceAsStream("complexBenchmark/transaction4.qr"),
					StandardCharsets.UTF_8);

			data = getData();

		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	private SailRepository repository;

	@Setup(Level.Invocation)
	public void setUp() throws InterruptedException {

		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.WARN);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName())).setLevel(ch.qos.logback.classic.Level.ERROR);
		System.setProperty("org.eclipse.rdf4j.sail.shacl.experimentalSparqlValidation", "true");

		try {
			repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.ttl"));

//			((ShaclSail) repository.getSail()).disableValidation();
//
//			try (SailRepositoryConnection connection = repository.getConnection()) {
//				connection.begin(IsolationLevels.NONE);
//				connection.add(data);
//				connection.commit();
//			}
//
//			((ShaclSail) repository.getSail()).enableValidation();
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
	public void shaclParallelCacheDeletionPreloaded() {

		((ShaclSail) repository.getSail()).setParallelValidation(true);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(true);
		((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

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
		((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

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
		((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

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
		((ShaclSail) repository.getSail()).setTransactionalValidationLimit(1000000);

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
		ClassLoader classLoader = ComplexLargeWriteBenchmark.class.getClassLoader();
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(
				classLoader.getResourceAsStream("complexBenchmark/datagovbe-valid.ttl"))) {
			return Rio.parse(bufferedInputStream, RDFFormat.TURTLE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
