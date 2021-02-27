/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
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
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xmx64M", "-XX:+UseSerialGC" })
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class NotMaxCountBenchmarkEmpty {
	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	private List<List<Statement>> allStatements;

	@Setup(Level.Trial)
	public void setUp() throws InterruptedException {
		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.INFO);

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		allStatements = BenchmarkConfigs.generateStatements(((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/" + i + "_" + j);
			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label" + i)));
		}));

		System.gc();
		Thread.sleep(100);
	}

	@Benchmark
	public void shacl() throws Exception {
		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSailNativeStore(temporaryFolder, "shaclNotMaxCountBenchmark.ttl"));

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin();
				connection.commit();
			}

			try (SailRepositoryConnection connection = repository.getConnection()) {
				for (List<Statement> statements : allStatements) {
					connection.begin();
					connection.add(statements);
					connection.commit();
				}
			}
			repository.shutDown();
		}
	}

	@Benchmark
	public void noShacl() {
		try (Utils.TemporaryFolder temporaryFolder = Utils.newTemporaryFolder()) {

			SailRepository repository = new SailRepository(
					Utils.getTestNotifyingSailNativeStore(temporaryFolder));

			repository.init();

			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin();
				connection.commit();
			}
			try (SailRepositoryConnection connection = repository.getConnection()) {
				for (List<Statement> statements : allStatements) {
					connection.begin();
					connection.add(statements);
					connection.commit();
				}
			}
			repository.shutDown();
		}
	}

}
