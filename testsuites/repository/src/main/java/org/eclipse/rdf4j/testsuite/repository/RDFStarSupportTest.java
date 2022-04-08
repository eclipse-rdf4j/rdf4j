/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Test cases for RDF-star support in the Repository.
 *
 * @author Jeen Broekstra
 *
 */
@Timeout(value = 10, unit = TimeUnit.MINUTES)
public abstract class RDFStarSupportTest {

	private Repository testRepository;

	private RepositoryConnection testCon;

	private ValueFactory vf;

	private BNode bob;

	private BNode alice;

	private BNode alexander;

	private Literal nameAlice;

	private Literal nameBob;

	private Literal mboxAlice;

	private Literal mboxBob;

	private IRI context1;

	private IRI context2;

	@BeforeEach
	public void setUp() throws Exception {
		testRepository = createRepository();

		testCon = testRepository.getConnection();
		testCon.clear();
		testCon.clearNamespaces();

		vf = testRepository.getValueFactory();

		// Initialize values
		bob = vf.createBNode();
		alice = vf.createBNode();
		alexander = vf.createBNode();

		nameAlice = vf.createLiteral("Alice");
		nameBob = vf.createLiteral("Bob");

		mboxAlice = vf.createLiteral("alice@example.org");
		mboxBob = vf.createLiteral("bob@example.org");

		context1 = vf.createIRI("urn:x-local:graph1");
		context2 = vf.createIRI("urn:x-local:graph2");
	}

	@AfterEach
	public void tearDown() throws Exception {
		try {
			testCon.close();
		} finally {
			testRepository.shutDown();
		}
	}

	@Test
	public void testAddRDFStarSubject() throws Exception {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		testCon.add(rdfStarTriple, RDF.TYPE, RDF.ALT);

		assertThat(testCon.hasStatement(rdfStarTriple, RDF.TYPE, RDF.ALT, false)).isTrue();
	}

	@Test
	public void testAddRDFStarObject() throws Exception {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		testCon.add(RDF.ALT, RDF.TYPE, rdfStarTriple);

		assertThat(testCon.hasStatement(RDF.ALT, RDF.TYPE, rdfStarTriple, false)).isTrue();
	}

	@Test
	public void testAddRDFStarContext() throws Exception {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		try {
			testCon.add(RDF.ALT, RDF.TYPE, RDF.ALT, rdfStarTriple);
			fail("RDF-star triple value should not be allowed by store as context identifier");
		} catch (UnsupportedOperationException e) {
			// fall through, expected behavior
			testCon.rollback();
		}

	}

	@Test
	public void testSparqlStar() {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		testCon.add(rdfStarTriple, RDF.TYPE, RDF.ALT);

		String query = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\nSELECT DISTINCT * WHERE { <<?s foaf:name ?o>> ?b ?c. }";

		List<BindingSet> result = QueryResults.asList(testCon.prepareTupleQuery(query).evaluate());
		assertThat(result).hasSize(1);

		BindingSet bs = result.get(0);
		assertThat(bs.getValue("s")).isEqualTo(bob);
		assertThat(bs.getValue("o")).isEqualTo(nameBob);
		assertThat(bs.getValue("b")).isEqualTo(RDF.TYPE);
		assertThat(bs.getValue("c")).isEqualTo(RDF.ALT);

	}

	@Test
	public void testSparqlStarUpdate() {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);
		testCon.add(rdfStarTriple, RDF.TYPE, RDF.ALT);

		String update = "PREFIX foaf: <" + FOAF.NAMESPACE
				+ ">\n INSERT { ?s foaf:age 23 } WHERE { <<?s foaf:name ?o>> ?b ?c .}";

		testCon.prepareUpdate(update).execute();

		assertThat(testCon.hasStatement(bob, FOAF.AGE, vf.createLiteral(23), false));
	}

	@Test
	public void testRdfStarAddAndRetrieveSparql() throws InterruptedException {

		Triple insertedTriple = vf.createTriple(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);

		Literal literal = vf.createLiteral("I am a triple ;-D");

		testCon.begin();
		testCon.add(insertedTriple, RDF.TYPE, literal);

		TupleQuery query = testCon.prepareTupleQuery(
				"SELECT * WHERE { << <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> >> ?a ?b}");

		assertTrue(testCon.prepareBooleanQuery("ASK { ?t a 'I am a triple ;-D'}").evaluate());
		assertEquals(1, query.evaluate().stream().count());
		testCon.commit();
	}

	@Test
	public void testRdfStarAddAndRetrieveSparqlSeparateTransaction() throws InterruptedException {

		Triple insertedTriple = vf.createTriple(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		Literal literal = vf.createLiteral("I am a triple ;-D");
		testCon.begin();

		testCon.add(insertedTriple, RDF.TYPE, literal);
		testCon.commit();
		testCon.begin();
		assertTrue(testCon.prepareBooleanQuery("ASK { ?t a 'I am a triple ;-D'}").evaluate());
		assertEquals(1, testCon.prepareTupleQuery(
				"SELECT * WHERE { << <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> >> ?a ?b}")
				.evaluate()
				.stream()
				.count());
		testCon.commit();

	}

	@Test
	public void testRdfStarAddAndRetrieve() throws InterruptedException {

		Triple insertedTriple = vf.createTriple(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		Triple copyOfInsertedTriple = vf.createTriple(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		Literal literal = vf.createLiteral("I am a triple ;-D");
		testCon.begin();

		testCon.add(insertedTriple, RDF.TYPE, literal);

		assertEquals(1, testCon.getStatements(null, RDF.TYPE, literal, false).stream().count());
		assertEquals(1, testCon.getStatements(copyOfInsertedTriple, null, null, false).stream().count());
		testCon.commit();

	}

	protected abstract Repository createRepository();
}
