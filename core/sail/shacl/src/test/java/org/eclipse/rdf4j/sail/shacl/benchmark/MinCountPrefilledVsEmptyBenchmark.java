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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MinCountPrefilledVsEmptyBenchmark {

	private List<List<Statement>> allStatements;
	private SailRepository shaclRepo;

	@Setup(Level.Invocation)
	public void setUp() throws Exception {
		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.INFO);

		if (shaclRepo != null) {
			shaclRepo.shutDown();
		}

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		allStatements = BenchmarkConfigs.generateStatements(((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/" + i + "_" + j);
			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			statements.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label" + i)));
		}));

		List<Statement> allStatements2 = new ArrayList<>();

		for (int i = 0; i < 1; i++) {
			IRI iri = vf.createIRI("http://example.com/preinserted/" + i);
			allStatements2.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			allStatements2.add(vf.createStatement(iri, RDFS.LABEL, vf.createLiteral("label" + i)));
		}

		ShaclSail shaclRepo = Utils.getInitializedShaclSail("shacl.trig");
		this.shaclRepo = new SailRepository(shaclRepo);

		try (SailRepositoryConnection connection = this.shaclRepo.getConnection()) {
			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(allStatements2);
			connection.commit();
		}

		System.gc();
		Thread.sleep(100);
	}

	@TearDown(Level.Invocation)
	public void tearDown() {
		if (shaclRepo != null) {
			shaclRepo.shutDown();
		}
	}

	@Benchmark
	public void shaclPrefilled() {

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
	public void shaclEmpty() throws Exception {

		ShaclSail shaclRepo = Utils.getInitializedShaclSail("shacl.trig");
		SailRepository repository = new SailRepository(shaclRepo);

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
	public void shaclEmptyJustinit() throws Exception {

		ShaclSail shaclRepo = Utils.getInitializedShaclSail("shacl.trig");
		SailRepository repository = new SailRepository(shaclRepo);
		repository.shutDown();

	}

	@Benchmark
	public void shaclEmptyJustInitializeAndEmptyTransaction() throws Exception {

		ShaclSail shaclRepo = Utils.getInitializedShaclSail("shacl.trig");
		SailRepository repository = new SailRepository(shaclRepo);

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.commit();
		}

		repository.shutDown();

	}

}
