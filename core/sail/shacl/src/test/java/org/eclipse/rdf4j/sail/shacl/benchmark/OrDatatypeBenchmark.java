/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
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
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xmx64M" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class OrDatatypeBenchmark {

	private List<List<Statement>> allStatements;

	@Setup(Level.Trial)
	public void setUp() throws InterruptedException {
		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.WARN);

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		Random random = new Random(490343534);

		allStatements = BenchmarkConfigs.generateStatements(((statements, i, j) -> {

			IRI iri = vf.createIRI("http://example.com/" + i + "_" + j);
			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));

			if (random.nextBoolean()) {
				statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral(i + "1")));
			}
			if (random.nextBoolean()) {
				statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral(i + "2")));
				statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral(i + "3")));
				statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral(i + "4")));
				statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral(i + "1", "en")));
				statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral(i + "2", "no")));
			}
			if (random.nextBoolean()) {
				statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral(i + "3", "se")));
				statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral(i + "4", "dk")));
			}

			if (random.nextBoolean()) {
				statements.add(vf.createStatement(iri, RDFS.COMMENT, vf.createLiteral(i + "8", "en")));
				statements.add(vf.createStatement(iri, RDFS.COMMENT, vf.createLiteral(i + "9", "no")));
			} else if (random.nextBoolean()) {
				statements.add(vf.createStatement(iri, RDFS.COMMENT, vf.createLiteral(i + "5")));
				statements.add(vf.createStatement(iri, RDFS.COMMENT, vf.createLiteral(i + "6")));
				statements.add(vf.createStatement(iri, RDFS.COMMENT, vf.createLiteral(i + "7")));
			}

		}));

	}

	@Benchmark
	public void shacl() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclOrDatatype.trig"));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin(IsolationLevels.SNAPSHOT);
				connection.add(statements);
				connection.commit();
			}
		}

		repository.shutDown();

	}

	public static void main(String[] args) throws Exception {
		OrDatatypeBenchmark orDatatypeBenchmark = new OrDatatypeBenchmark();
		orDatatypeBenchmark.setUp();
		orDatatypeBenchmark.shacl();
	}

	@Benchmark
	public void shaclBulk() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclOrDatatype.trig"));

		((ShaclSail) repository.getSail()).setCacheSelectNodes(true);

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk);
			for (List<Statement> statements : allStatements) {
				connection.add(statements);
			}
			connection.commit();

		}

		repository.shutDown();

	}

}
