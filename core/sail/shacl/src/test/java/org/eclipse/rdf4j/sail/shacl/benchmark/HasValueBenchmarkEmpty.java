/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:+UseG1GC", "-XX:StartFlightRecording=delay=5s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class HasValueBenchmarkEmpty {

	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	ValueFactory vf = SimpleValueFactory.getInstance();

	static String ex = "http://example.com/ns#";

	IRI Person = vf.createIRI(ex, "Person");
	IRI knows = vf.createIRI(ex, "knows");
	IRI steve = vf.createIRI(ex, "steve");
	IRI peter = vf.createIRI(ex, "peter");

	private List<List<Statement>> allStatements;

	@Setup(Level.Iteration)
	public void setUp() throws InterruptedException {
		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.INFO);

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		allStatements = BenchmarkConfigs.generateStatements(((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/" + i + "_" + j);
			statements.add(vf.createStatement(iri, RDF.TYPE, Person));
			statements.add(vf.createStatement(iri, knows, vf.createLiteral(i)));
			statements.add(vf.createStatement(iri, knows, vf.createBNode()));
			statements.add(vf.createStatement(iri, knows, steve));
			statements.add(vf.createStatement(iri, knows, peter));
		}));

		System.gc();
		Thread.sleep(100);
	}

	@Benchmark
	public void shacl() throws Exception {

		SailRepository repository = new SailRepository(
				Utils.getInitializedShaclSail("test-cases/hasValue/simple/shacl.ttl"));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.SNAPSHOT);
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin(IsolationLevels.SNAPSHOT);
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
			connection.begin(IsolationLevels.SNAPSHOT);
			connection.commit();
		}
		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin(IsolationLevels.SNAPSHOT);
				connection.add(statements);
				connection.commit();
			}
		}

//		repository.shutDown();

	}

//	@Benchmark
//	public void sparqlInsteadOfShacl() {
//
//		SailRepository repository = new SailRepository(new MemoryStore());
//
//		repository.init();
//
//		try (SailRepositoryConnection connection = repository.getConnection()) {
//			connection.begin(IsolationLevels.SNAPSHOT);
//			connection.commit();
//		}
//		try (SailRepositoryConnection connection = repository.getConnection()) {
//			for (List<Statement> statements : allStatements) {
//				connection.begin(IsolationLevels.SNAPSHOT);
//				connection.add(statements);
//				try (Stream<BindingSet> stream = connection
//						.prepareTupleQuery(
//							"select * where {?a a <" +ex+"Person" + ">; " +
//								"<" +ex+"knows"+"> ?knows. " +
//								"FILTER(?knows != <http://www.w3.org/2001/XMLSchema#int>)}")
//						.evaluate()
//						.stream()) {
//					stream.forEach(System.out::println);
//				}
//				connection.commit();
//			}
//		}
//
////		repository.shutDown();
//
//	}

}
