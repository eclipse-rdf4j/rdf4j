/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
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
import org.openjdk.jmh.annotations.Param;
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
@Warmup(iterations = 3)
@BenchmarkMode({ Mode.AverageTime })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G",  "-XX:StartFlightRecording=delay=5s,duration=60s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G" })
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ComplexTargetBenchmark {

	@Param({ "1", "1000", "100000" })
	public int existingTargets = 10;

	public String shape = "shaclDatatypeTargetFilterWithUnion.trig";

	public int NUMBER_OF_TRANSACTIONS = 10;

	List<Statement> initialStatements = new ArrayList<>();

	private List<List<Statement>> transactions;

	SailRepository repository;

	@Setup(Level.Trial)
	public void trialSetup() throws InterruptedException {
		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.INFO);

		transactions = new ArrayList<>(NUMBER_OF_TRANSACTIONS);

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		for (int j = 0; j < NUMBER_OF_TRANSACTIONS; j++) {
			List<Statement> statements = new ArrayList<>(BenchmarkConfigs.STATEMENTS_PER_TRANSACTION);
			transactions.add(statements);
			for (int i = 0; i < BenchmarkConfigs.STATEMENTS_PER_TRANSACTION; i++) {
				IRI iri = vf.createIRI("http://example.com/transaction_" + i + "_" + j);
				statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
				statements.add(vf.createStatement(iri, FOAF.AGE, vf.createLiteral(i)));

				IRI iri2 = vf.createIRI("http://example.com/transaction_2" + i + "_" + j);
				statements.add(vf.createStatement(iri2, RDF.TYPE, RDFS.CLASS));
				statements.add(vf.createStatement(iri2, FOAF.AGE, vf.createLiteral(i)));

			}
		}

		for (int j = 0; j < existingTargets; j++) {
			IRI iri = vf.createIRI("http://example.com/base_" + j);
			initialStatements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			initialStatements.add(vf.createStatement(iri, FOAF.AGE, vf.createLiteral(j)));

			IRI iri2 = vf.createIRI("http://example.com/base_2" + j);
			initialStatements.add(vf.createStatement(iri2, RDF.TYPE, RDFS.CLASS));
			initialStatements.add(vf.createStatement(iri2, FOAF.AGE, vf.createLiteral(j)));
		}

		System.gc();
		Thread.sleep(100);
	}

	@Setup(Level.Invocation)
	public void invocationSetup() throws IOException, InterruptedException {

		repository = new SailRepository(Utils.getInitializedShaclSail(shape));

		((ShaclSail) repository.getSail()).setDashDataShapes(true);
		((ShaclSail) repository.getSail()).setEclipseRdf4jShaclExtensions(true);

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(initialStatements);
			connection.commit();
		}

		System.gc();
		Thread.sleep(100);
	}

	@Benchmark
	public void benchmark() throws Exception {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : transactions) {
				connection.begin(IsolationLevels.SNAPSHOT);
				connection.add(statements);
				connection.commit();
			}
		}

		repository.shutDown();
	}

}
