/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.MaxCountConstraintComponent;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xmx2G", "-Xms2G" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MaxCountSparqlBenchmarkEmpty {

	@Param({ "1", "2", "3", "4" })
	public int MAX_COUNT = 1;

	@Param({ "manyInvalidStatements", "mostlyValidStatements" })
	public String statementList;

	private static List<List<Statement>> manyInvalidStatements;
	private static List<List<Statement>> mostlyValidStatements1;
	private static List<List<Statement>> mostlyValidStatements2;
	private static List<List<Statement>> mostlyValidStatements3;
	private static List<List<Statement>> mostlyValidStatements4;

	static {
		fillData();
	}

	@Setup(Level.Trial)
	public void setUp() throws InterruptedException {

		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.INFO);

		System.gc();
		Thread.sleep(100);
	}

	private static void fillData() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		manyInvalidStatements = BenchmarkConfigs.generateStatements(1, 10, 0, ((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/invalid_" + i + "_" + j);

			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label1" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label2" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label3" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label4" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label5" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label6" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label7" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label8" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label9" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label10" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label11" + "_" + i + "_" + j)));

			for (int i2 = 0; i2 < 1000; i2++) {
				IRI validIri = vf.createIRI("http://example.com/valid" + i2 + "_" + i + "_" + j);
				statements.add(vf.createStatement(validIri, RDF.TYPE, RDFS.RESOURCE));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label1" + i2 + "_" + i + "_" + j)));
			}

		}));

		mostlyValidStatements1 = BenchmarkConfigs.generateStatements(1, 1, 0, ((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/invalid_" + i + "_" + j);

			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label1" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label2" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label3" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label4" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label5" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label6" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label7" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label8" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label9" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label10" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label11" + "_" + i + "_" + j)));

			for (int i2 = 0; i2 < 10000; i2++) {
				IRI validIri = vf.createIRI("http://example.com/valid" + i2 + "_" + i + "_" + j);
				statements.add(vf.createStatement(validIri, RDF.TYPE, RDFS.RESOURCE));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label1" + i2 + "_" + i + "_" + j)));
			}

		}));

		mostlyValidStatements2 = BenchmarkConfigs.generateStatements(1, 1, 0, ((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/invalid_" + i + "_" + j);

			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label1" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label2" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label3" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label4" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label5" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label6" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label7" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label8" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label9" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label10" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label11" + "_" + i + "_" + j)));

			for (int i2 = 0; i2 < 10000; i2++) {
				IRI validIri = vf.createIRI("http://example.com/valid" + i2 + "_" + i + "_" + j);
				statements.add(vf.createStatement(validIri, RDF.TYPE, RDFS.RESOURCE));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label1" + i2 + "_" + i + "_" + j)));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label2" + i2 + "_" + i + "_" + j)));
			}

		}));

		mostlyValidStatements3 = BenchmarkConfigs.generateStatements(1, 1, 0, ((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/invalid_" + i + "_" + j);

			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label1" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label2" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label3" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label4" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label5" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label6" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label7" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label8" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label9" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label10" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label11" + "_" + i + "_" + j)));

			for (int i2 = 0; i2 < 10000; i2++) {
				IRI validIri = vf.createIRI("http://example.com/valid" + i2 + "_" + i + "_" + j);
				statements.add(vf.createStatement(validIri, RDF.TYPE, RDFS.RESOURCE));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label1" + i2 + "_" + i + "_" + j)));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label2" + i2 + "_" + i + "_" + j)));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label3" + i2 + "_" + i + "_" + j)));
			}

		}));

		mostlyValidStatements4 = BenchmarkConfigs.generateStatements(1, 1, 0, ((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/invalid_" + i + "_" + j);

			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label1" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label2" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label3" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label4" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label5" + i)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label6" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label7" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label8" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label9" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label10" + "_" + i + "_" + j)));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label11" + "_" + i + "_" + j)));

			for (int i2 = 0; i2 < 10000; i2++) {
				IRI validIri = vf.createIRI("http://example.com/valid" + i2 + "_" + i + "_" + j);
				statements.add(vf.createStatement(validIri, RDF.TYPE, RDFS.RESOURCE));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label1" + i2 + "_" + i + "_" + j)));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label2" + i2 + "_" + i + "_" + j)));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label3" + i2 + "_" + i + "_" + j)));
				statements.add(
						vf.createStatement(validIri, RDFS.LABEL, vf.createLiteral("label4" + i2 + "_" + i + "_" + j)));
			}

		}));
	}

	@TearDown(Level.Trial)
	public void tearDown() {
		MaxCountConstraintComponent.SPARQL_VALIDATION_APPROACH_LIMIT = 5;
	}

	@Benchmark
	public void shaclBulkSparql() throws Exception {

		MaxCountConstraintComponent.SPARQL_VALIDATION_APPROACH_LIMIT = 10;

		SailRepository repository = new SailRepository(
				Utils.getInitializedShaclSail("shaclMaxCountBenchmark" + MAX_COUNT + ".trig"));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk);
			for (List<Statement> statements : getStatements()) {
				connection.add(statements);
			}
			try {
				connection.commit();
			} catch (RepositoryException e) {
				if (!(e.getCause() instanceof ShaclSailValidationException)) {
					throw e;
				}
			}
		}
		repository.shutDown();

	}

	private List<List<Statement>> getStatements() {
		if (statementList.equals("mostlyValidStatements")) {
			statementList = "mostlyValidStatements" + MAX_COUNT;
		}

		switch (statementList) {
		case "manyInvalidStatements":
			return manyInvalidStatements;
		case "mostlyValidStatements1":
			return mostlyValidStatements1;
		case "mostlyValidStatements2":
			return mostlyValidStatements2;
		case "mostlyValidStatements3":
			return mostlyValidStatements3;
		case "mostlyValidStatements4":
			return mostlyValidStatements4;
		}
		throw new IllegalStateException();
	}

	@Benchmark
	public void shaclBulkNonSparql() throws Exception {

		MaxCountConstraintComponent.SPARQL_VALIDATION_APPROACH_LIMIT = 0;

		SailRepository repository = new SailRepository(
				Utils.getInitializedShaclSail("shaclMaxCountBenchmark" + MAX_COUNT + ".trig"));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk);
			for (List<Statement> statements : getStatements()) {
				connection.add(statements);
			}
			try {
				connection.commit();
			} catch (RepositoryException e) {
				if (!(e.getCause() instanceof ShaclSailValidationException)) {
					throw e;
				}
			}
		}
		repository.shutDown();

	}

}
