/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-XX:+UseSerialGC" })
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DatatypeBenchmarkPrefilled {
	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	private List<List<Statement>> allStatements;

	private SailRepository shaclRepo;
	private SailRepository memoryStoreRepo;
	private SailRepository sparqlQueryMemoryStoreRepo;

	@Setup(Level.Invocation)
	public void setUp() throws Exception {
		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.INFO);

		if (shaclRepo != null) {
			shaclRepo.shutDown();
		}
		if (memoryStoreRepo != null) {
			memoryStoreRepo.shutDown();
		}
		if (sparqlQueryMemoryStoreRepo != null) {
			sparqlQueryMemoryStoreRepo.shutDown();
		}

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		allStatements = BenchmarkConfigs.generateStatements(((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/" + i + "_" + j);
			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			statements.add(vf.createStatement(iri, FOAF.AGE, vf.createLiteral(i)));
		}));

		List<Statement> allStatements2 = new ArrayList<>();

		for (int i = 0; i < 100000; i++) {
			allStatements2.add(
					vf.createStatement(vf.createIRI("http://example.com/preinserted/" + i), RDF.TYPE, RDFS.RESOURCE));
			allStatements2.add(vf.createStatement(vf.createIRI("http://example.com/preinserted/" + i), FOAF.AGE,
					vf.createLiteral(i)));
		}

		ShaclSail shaclRepo = Utils.getInitializedShaclSail("shaclDatatype.ttl");
		this.shaclRepo = new SailRepository(shaclRepo);

		memoryStoreRepo = new SailRepository(new MemoryStore());
		memoryStoreRepo.init();

		sparqlQueryMemoryStoreRepo = new SailRepository(new MemoryStore());
		sparqlQueryMemoryStoreRepo.init();

		shaclRepo.disableValidation();
		try (SailRepositoryConnection connection = this.shaclRepo.getConnection()) {
			connection.add(allStatements2);
		}
		shaclRepo.enableValidation();

		try (SailRepositoryConnection connection = memoryStoreRepo.getConnection()) {
			connection.add(allStatements2);
		}

		try (SailRepositoryConnection connection = sparqlQueryMemoryStoreRepo.getConnection()) {
			connection.add(allStatements2);
		}
		System.gc();
		Thread.sleep(100);
	}

	@TearDown(Level.Invocation)
	public void tearDown() throws Exception {
		if (shaclRepo != null) {
			shaclRepo.shutDown();
		}
		if (memoryStoreRepo != null) {
			memoryStoreRepo.shutDown();
		}
		if (sparqlQueryMemoryStoreRepo != null) {
			sparqlQueryMemoryStoreRepo.shutDown();
		}

	}

	@Benchmark
	public void shacl() {

		try (SailRepositoryConnection connection = shaclRepo.getConnection()) {
			connection.begin();
			connection.commit();
		}

		try (SailRepositoryConnection connection = shaclRepo.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin();
				connection.add(statements);
				connection.commit();
			}
		}

	}

	@Benchmark
	public void noShacl() {

		try (SailRepositoryConnection connection = memoryStoreRepo.getConnection()) {
			connection.begin();
			connection.commit();
		}
		try (SailRepositoryConnection connection = memoryStoreRepo.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin();
				connection.add(statements);
				connection.commit();
			}
		}

	}

	@Benchmark
	public void sparqlInsteadOfShacl() {

		try (SailRepositoryConnection connection = sparqlQueryMemoryStoreRepo.getConnection()) {
			connection.begin();
			connection.commit();
		}
		try (SailRepositoryConnection connection = sparqlQueryMemoryStoreRepo.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin();
				connection.add(statements);
				try (Stream<BindingSet> stream = connection
						.prepareTupleQuery("select * where {?a a <" + RDFS.RESOURCE + ">; <" + FOAF.AGE
								+ "> ?age. FILTER(datatype(?age) != <http://www.w3.org/2001/XMLSchema#int>)}")
						.evaluate()
						.stream()) {
					stream.forEach(System.out::println);
				}
				connection.commit();
			}
		}

	}

}
