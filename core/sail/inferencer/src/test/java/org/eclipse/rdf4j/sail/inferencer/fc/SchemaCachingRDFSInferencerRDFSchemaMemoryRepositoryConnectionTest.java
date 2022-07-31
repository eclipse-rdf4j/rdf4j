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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
import org.junit.Ignore;
import org.junit.Test;

public class SchemaCachingRDFSInferencerRDFSchemaMemoryRepositoryConnectionTest
		extends RDFSchemaRepositoryConnectionTest {

	public SchemaCachingRDFSInferencerRDFSchemaMemoryRepositoryConnectionTest(IsolationLevel level) {
		super(level);
	}

	@Override
	protected Repository createRepository() {
		SchemaCachingRDFSInferencer sail = new SchemaCachingRDFSInferencer(new MemoryStore(), true);
		sail.setAddInferredStatementsToDefaultContext(false);

		return new SailRepository(sail);
	}

	@Override
	@Test
	@Ignore
	public void testQueryDefaultGraph() throws Exception {
		// ignore
	}

	@Override
	@Test
	@Ignore
	public void testDeleteDefaultGraph() throws Exception {
		// ignore
	}

	@Override
	@Test
	@Ignore
	public void testContextStatementsNotDuplicated() throws Exception {
		// ignore
	}

	@Override
	@Test
	@Ignore
	public void testContextStatementsNotDuplicated2() throws Exception {
		// ignore
	}

	@Test
	public void testContextTbox() {

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

	@Test
	public void testUpdateInsertData() {

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

	@Test
	public void testUpdateInsert() {

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

	@Test
	public void testInsert() {

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

	@Test
	public void testUpdateRemove() {

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
