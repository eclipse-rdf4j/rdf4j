/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.testsuite.repository.RDFSchemaRepositoryConnectionTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SchemaCachingRDFSInferencerRDFSchemaMemoryRepositoryConnectionTest
		extends RDFSchemaRepositoryConnectionTest {

	@Override
	protected Repository createRepository(File dataDir) {
		SchemaCachingRDFSInferencer sail = new SchemaCachingRDFSInferencer(new MemoryStore(), true);
		sail.setAddInferredStatementsToDefaultContext(false);

		return new SailRepository(sail);
	}

	@Override
	@ParameterizedTest
	@MethodSource("parameters")
	@Disabled
	public void testQueryDefaultGraph(IsolationLevel level) {
		// ignore
	}

	@Override
	@ParameterizedTest
	@MethodSource("parameters")
	@Disabled
	public void testDeleteDefaultGraph(IsolationLevel level) {
		// ignore
	}

	@Override
	@ParameterizedTest
	@MethodSource("parameters")
	@Disabled
	public void testContextStatementsNotDuplicated(IsolationLevel level) {
		// ignore
	}

	@Override
	@ParameterizedTest
	@MethodSource("parameters")
	@Disabled
	public void testContextStatementsNotDuplicated2(IsolationLevel level) {
		// ignore
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testContextTbox(IsolationLevel level) {
		setupTest(level);

//		Man subClassOf Human g1
//		Human subClassOf Animal g2
//	-> Man subClassOf Animal ??

		IRI man = vf.createIRI("http://example.org/Man");
		IRI human = vf.createIRI("http://example.org/Human");
		IRI animal = vf.createIRI("http://example.org/Animal");
		IRI bob = vf.createIRI("http://example.org/bob");

		IRI graph1 = vf.createIRI("http://example.org/graph1");
		IRI graph2 = vf.createIRI("http://example.org/graph2");
		IRI graph3 = vf.createIRI("http://example.org/graph3");

		testCon.add(man, RDFS.SUBCLASSOF, human, graph1);
		testCon.add(human, RDFS.SUBCLASSOF, animal, graph2);
		testCon.add(bob, RDF.TYPE, man, graph3);

		/*
		 * The SchemaCachingRDFSInferencer correctly adds inferred A-box statements to the correct graph, but does not
		 * add inferred T-box statements to the correct graph.
		 */

		System.out.println("-----------");
		try (Stream<Statement> stream = testCon.getStatements(man, RDFS.SUBCLASSOF, null, true).stream()) {
			stream.forEach(System.out::println);
		}
		System.out.println("-----------");
		try (Stream<Statement> stream = testCon.getStatements(bob, RDF.TYPE, null, true).stream()) {
			stream.peek(statement -> assertEquals(graph3, statement.getContext())).forEach(System.out::println);
		}

		System.out.println("-----------");

	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testUpdateInsertData(IsolationLevel level) {
		setupTest(level);

		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore()));
		try (SailRepositoryConnection connection = sail.getConnection()) {

			IRI foo_s1 = connection.getValueFactory().createIRI("foo:s1");
			IRI foo_C2 = connection.getValueFactory().createIRI("foo:C2");

			connection.begin();
			connection.prepareUpdate("insert data { <foo:s1> a <foo:C1> . <foo:C1> rdfs:subClassOf <foo:C2> } ")
					.execute();
			connection.commit();

			assertTrue(connection.hasStatement(foo_s1, RDF.TYPE, foo_C2, true));

		}

	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testUpdateInsert(IsolationLevel level) {
		setupTest(level);

		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore()));
		try (SailRepositoryConnection connection = sail.getConnection()) {

			IRI foo_s1 = connection.getValueFactory().createIRI("foo:s1");
			IRI foo_C2 = connection.getValueFactory().createIRI("foo:C2");

			connection.begin();
			connection
					.prepareUpdate("insert {<foo:s1> a <foo:C1> . <foo:C1> rdfs:subClassOf <foo:C2>} where {?a ?b ?c}")
					.execute();
			connection.commit();

			assertTrue(connection.hasStatement(foo_s1, RDF.TYPE, foo_C2, true));

		}

	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testInsert(IsolationLevel level) {
		setupTest(level);

		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore()));
		try (SailRepositoryConnection connection = sail.getConnection()) {

			IRI foo_s1 = connection.getValueFactory().createIRI("foo:s1");
			IRI foo_C2 = connection.getValueFactory().createIRI("foo:C2");
			IRI foo_C1 = connection.getValueFactory().createIRI("foo:C1");

			connection.begin();
			connection.add(foo_s1, RDF.TYPE, foo_C1);
			connection.add(foo_C1, RDFS.SUBCLASSOF, foo_C2);
			connection.commit();

			assertTrue(connection.hasStatement(foo_s1, RDF.TYPE, foo_C2, true));

		}

	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testUpdateRemove(IsolationLevel level) {
		setupTest(level);

		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore()));
		try (SailRepositoryConnection connection = sail.getConnection()) {

			IRI foo_s1 = connection.getValueFactory().createIRI("foo:s1");
			IRI foo_C2 = connection.getValueFactory().createIRI("foo:C2");

			connection.begin();
			connection.prepareUpdate("insert data { <foo:s1> a <foo:C1> . <foo:C1> rdfs:subClassOf <foo:C2> } ")
					.execute();
			connection.commit();

			assertTrue(connection.hasStatement(foo_s1, RDF.TYPE, foo_C2, true));

			connection.begin();
			connection.prepareUpdate("delete data { <foo:s1> a <foo:C1> . <foo:C1> rdfs:subClassOf <foo:C2> } ")
					.execute();
			connection.commit();

			assertFalse(connection.hasStatement(foo_s1, RDF.TYPE, foo_C2, true));

		}

	}

}
