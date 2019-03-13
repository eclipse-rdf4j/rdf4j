/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import ch.qos.logback.classic.Logger;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({Mode.AverageTime})
@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MinCountBenchmarkEmpty {


	private List<List<Statement>> allStatements;

	@Setup(Level.Invocation)
	public void setUp() {
		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.INFO);

		allStatements = new ArrayList<>(10);


		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		for (int j = 0; j < 10; j++) {
			List<Statement> statements = new ArrayList<>(101);
			allStatements.add(statements);
			for (int i = 0; i < 1000; i++) {
				statements.add(
					vf.createStatement(vf.createIRI("http://example.com/" + i + "_" + j), RDF.TYPE, RDFS.RESOURCE)
				);
				statements.add(
					vf.createStatement(vf.createIRI("http://example.com/" + i + "_" + j), RDFS.LABEL, vf.createLiteral("label" + i))
				);
			}
		}
		System.gc();

	}

	@TearDown(Level.Iteration)
	public void tearDown() {
		allStatements.clear();
	}


	@Benchmark
	public void shacl() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shacl.ttl"));


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

	}


	@Benchmark
	public void noShacl() {

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
				connection.commit();
			}
		}

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
				try (Stream<BindingSet> stream = Iterations.stream(connection.prepareTupleQuery("select * where {?a a <" + RDFS.RESOURCE + ">. FILTER(! EXISTS {?a <" + RDFS.LABEL + "> ?c})}").evaluate())) {
					stream.forEach(System.out::println);
				}
				connection.commit();
			}
		}

	}


}
