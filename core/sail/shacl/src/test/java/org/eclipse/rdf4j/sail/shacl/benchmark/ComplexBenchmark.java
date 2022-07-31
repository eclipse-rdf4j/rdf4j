/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.RepositoryConnectionShapeSource;
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
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ComplexBenchmark {

	private static final String transaction1;
	private static final String transaction2;

	static {
		try {
			transaction1 = IOUtils.toString(
					ComplexBenchmark.class.getClassLoader().getResourceAsStream("complexBenchmark/transaction1.qr"),
					StandardCharsets.UTF_8);
			transaction2 = IOUtils.toString(
					ComplexBenchmark.class.getClassLoader().getResourceAsStream("complexBenchmark/transaction2.qr"),
					StandardCharsets.UTF_8);

		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	@Setup(Level.Trial)
	public void setUp() throws InterruptedException {
		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.ERROR);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName())).setLevel(ch.qos.logback.classic.Level.ERROR);

	}

	@Benchmark
	public void shaclParallelCache() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));
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

		repository.shutDown();

	}

	@Benchmark
	public void shaclNoTransactions() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));

		repository.shutDown();

	}

	SailRepository memoryStore = new SailRepository(new MemoryStore());

	{
		memoryStore.init();

		try (SailRepositoryConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			try {
				connection.add(
						ComplexBenchmark.class.getClassLoader().getResourceAsStream("complexBenchmark/shacl.trig"), "",
						RDFFormat.TRIG);
			} catch (IOException e) {
				e.printStackTrace();
			}
			connection.commit();
		}
	}

	@Benchmark
	public void shaclPropertiesSwitch(Blackhole blackhole) {

		Resource[] context = { RDF4J.SHACL_SHAPE_GRAPH };

		try (SailRepositoryConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);

			try (Stream<Statement> stream = connection.getStatements(null, SHACL.PROPERTY, null).stream()) {
				stream.map(Statement::getObject).forEach(o -> {
					ShaclProperties shaclProperties = new ShaclProperties((Resource) o,
							new RepositoryConnectionShapeSource(connection).withContext(context));
					blackhole.consume(shaclProperties);
				});
			}
			connection.commit();

		}

	}

	@Benchmark
	public void shaclEmptyTransactions() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));
		((ShaclSail) repository.getSail()).setParallelValidation(false);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.commit();

		}

		repository.shutDown();

	}

	@Benchmark
	public void shaclNothingToValidateTransactions() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));
		((ShaclSail) repository.getSail()).setParallelValidation(false);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.add(connection.getValueFactory().createBNode(), RDFS.LABEL,
					connection.getValueFactory().createLiteral(""));
			connection.commit();

		}

		repository.shutDown();

	}

	@Benchmark
	public void shaclParallelCacheSingleTransactionNoIsolation() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));
		((ShaclSail) repository.getSail()).setParallelValidation(true);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(true);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			connection.prepareUpdate(transaction1).execute();

			connection.prepareUpdate(transaction2).execute();
			connection.commit();

		}

		repository.shutDown();

	}

	@Benchmark
	public void shaclParallel() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));

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

		repository.shutDown();

	}

	@Benchmark
	public void shaclCache() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));

		((ShaclSail) repository.getSail()).setParallelValidation(false);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction1).execute();
			connection.commit();

			connection.begin(IsolationLevels.SNAPSHOT);
			connection.prepareUpdate(transaction2).execute();
			connection.commit();

		}

		repository.shutDown();

	}

	@Benchmark
	public void shacl() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("complexBenchmark/shacl.trig"));

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

		repository.shutDown();

	}

}
