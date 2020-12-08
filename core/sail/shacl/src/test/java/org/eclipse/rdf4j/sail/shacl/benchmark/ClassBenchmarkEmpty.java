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
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.eclipse.rdf4j.sail.shacl.testimp.TestNotifyingSail;
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
public class ClassBenchmarkEmpty {
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
			IRI friend = vf.createIRI("http://example.com/friend" + i + "_" + j);

			statements.add(vf.createStatement(iri, RDF.TYPE, FOAF.PERSON));
			statements.add(vf.createStatement(iri, FOAF.KNOWS, friend));
			statements.add(vf.createStatement(friend, RDF.TYPE, FOAF.PERSON));
		}));

		System.gc();
		Thread.sleep(100);
	}

	@Benchmark
	public void shacl() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclClassBenchmark.ttl"));

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

	@Benchmark
	public void noShacl() {

		SailRepository repository = new SailRepository(new TestNotifyingSail(new MemoryStore()));

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

	@Benchmark
	public void sparqlInsteadOfShacl() {

		SailRepository repository = new SailRepository(new MemoryStore());

		repository.init();

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.commit();
		}
		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin();
				connection.add(statements);
				try (Stream<BindingSet> stream = connection
						.prepareTupleQuery("select * where {?a a <" + FOAF.PERSON + ">. ?a <"
								+ FOAF.KNOWS + "> ?c. FILTER(NOT EXISTS{?c a <" + FOAF.PERSON + ">})}")
						.evaluate()
						.stream()) {
					stream.forEach(System.out::println);
				}
				connection.commit();
			}
		}
		repository.shutDown();

	}

}
