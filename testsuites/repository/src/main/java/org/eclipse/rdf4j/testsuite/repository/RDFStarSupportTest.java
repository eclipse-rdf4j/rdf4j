/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Test cases for RDF-star support in the Repository.
 *
 * @author Jeen Broekstra
 */
@Timeout(value = 10, unit = TimeUnit.MINUTES)
public abstract class RDFStarSupportTest {

	private Repository testRepository;

	protected RepositoryConnection testCon;

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
	public void testAddRDFStarSubject() {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		testCon.add(rdfStarTriple, RDF.TYPE, RDF.ALT);

		assertThat(testCon.hasStatement(rdfStarTriple, RDF.TYPE, RDF.ALT, false)).isTrue();
	}

	@Test
	public void testAddRDFStarObject() {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		testCon.add(RDF.ALT, RDF.TYPE, rdfStarTriple);

		assertThat(testCon.hasStatement(RDF.ALT, RDF.TYPE, rdfStarTriple, false)).isTrue();
	}

	@Test
	public void testAddRDFStarContext() {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		try {
			testCon.add(RDF.ALT, RDF.TYPE, RDF.ALT, rdfStarTriple);
			Assertions.fail("RDF-star triple value should not be allowed by store as context identifier");
		} catch (UnsupportedOperationException e) {
			// fall through, expected behavior
			testCon.rollback();
		}

	}

	@Test
	public void testSparqlStar() {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		testCon.add(rdfStarTriple, RDF.TYPE, RDF.ALT);

		String query = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
				"SELECT DISTINCT * WHERE { <<?s foaf:name ?o>> ?b ?c. }";

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

		String update = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
				"INSERT { ?s foaf:age 23 } WHERE { <<?s foaf:name ?o>> ?b ?c .}";

		testCon.prepareUpdate(update).execute();

		Assertions.assertTrue(testCon.hasStatement(bob, FOAF.AGE, vf.createLiteral(BigInteger.valueOf(23)), false));
	}

	@Test
	public void testRdfStarAddAndRetrieveSparql() {

		Triple insertedTriple = vf.createTriple(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);

		Literal literal = vf.createLiteral("I am a triple ;-D");

		testCon.begin();
		testCon.add(insertedTriple, RDF.TYPE, literal);

		TupleQuery query = testCon.prepareTupleQuery(
				"SELECT * WHERE { << <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> >> ?a ?b}");

		Assertions.assertTrue(testCon.prepareBooleanQuery("ASK { ?t a 'I am a triple ;-D'}").evaluate());
		Assertions.assertEquals(1, getCount(query));
		testCon.commit();
	}

	@Test
	public void testRdfStarAddAndRetrieveSparqlSeparateTransaction() {

		Triple insertedTriple = vf.createTriple(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		Literal literal = vf.createLiteral("I am a triple ;-D");
		testCon.begin();

		testCon.add(insertedTriple, RDF.TYPE, literal);
		testCon.commit();
		testCon.begin();
		Assertions.assertTrue(testCon.prepareBooleanQuery("ASK { ?t a 'I am a triple ;-D'}").evaluate());
		TupleQuery tupleQuery = testCon.prepareTupleQuery(
				"SELECT * WHERE { << <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> >> ?a ?b}");
		Assertions.assertEquals(1, getCount(tupleQuery));
		testCon.commit();

	}

	private static long getCount(TupleQuery tupleQuery) {
		try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
			return evaluate.stream().count();
		}
	}

	@Test
	public void testRdfStarAddAndRetrieve() {

		Triple insertedTriple = vf.createTriple(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		Triple copyOfInsertedTriple = vf.createTriple(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		Literal literal = vf.createLiteral("I am a triple ;-D");
		testCon.begin();

		testCon.add(insertedTriple, RDF.TYPE, literal);

		Assertions.assertEquals(1, testCon.getStatements(null, RDF.TYPE, literal, false).stream().count());
		Assertions.assertEquals(1, testCon.getStatements(copyOfInsertedTriple, null, null, false).stream().count());
		testCon.commit();

	}

	@Test
	public void testMemoryStore_RDFstar() {
		String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "PREFIX person: <http://example.com/person/>\n"
				+ "PREFIX org: <http://example.com/org/>\n"
				+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
				+ "CONSTRUCT {\n"
				+ "  ?s ?p ?o.\n"
				+ "  << ?s ?p ?o >> <http://example.com/certainty> ?cert.\n"
				+ "  << ?s ?p ?o >> <http://example.com/certaintyDeviation> ?certDiv.\n"
				+ "  << person:alice foaf:knows person:bob >> <http://example.com/observedBy> person:mike.\n"
				+ "}\n"
				+ "WHERE {\n"
				+ "  {\n"
				+ "    SELECT ?s ?p ?o ?cert ?certDiv WHERE {\n"
				+ "      VALUES (?s ?p ?o ?cert ?certDiv) {\n"
				+ "        (person:alice foaf:knows person:bob \"1.0\"^^xsd:decimal 0 )\n"
				+ "        (person:alice foaf:knows person:carol \"0.3\"^^xsd:decimal \"0.1\"^^xsd:decimal)\n"
				+ "        (person:carol foaf:knows person:mike \"0.7\"^^xsd:decimal \"0.2\"^^xsd:decimal)\n"
				+ "        (person:mike foaf:knows person:carol \"0.1\"^^xsd:decimal UNDEF)\n"
				+ "        (person:bob foaf:knows person:carol \"0.8\"^^xsd:decimal UNDEF)\n"
				+ "        (person:alice foaf:knows person:mike \"0.6\"^^xsd:decimal \"0.1\"^^xsd:decimal)\n"
				+ "        (person:alice foaf:member org:W3C UNDEF UNDEF)\n"
				+ "        (person:mike foaf:member org:W3C UNDEF UNDEF)\n"
				+ "        (person:alice rdf:type foaf:Person UNDEF UNDEF)\n"
				+ "        (person:carol rdf:type foaf:Person UNDEF UNDEF)\n"
				+ "        (person:mike rdf:type foaf:Person UNDEF UNDEF)\n"
				+ "        (person:bob rdf:type foaf:Person UNDEF UNDEF)\n"
				+ "        (org:W3C rdf:type foaf:Organization UNDEF UNDEF)\n"
				+ "        (person:alice rdfs:label \"Alice\" UNDEF UNDEF)\n"
				+ "        (person:alice foaf:birthday \"1990-01-01\" UNDEF UNDEF)\n"
				+ "        (person:bob rdfs:label \"Bob\" UNDEF UNDEF)\n"
				+ "        (person:carol rdfs:label \"Carol\" UNDEF UNDEF)\n"
				+ "        (person:mike rdfs:label \"Mike\" UNDEF UNDEF)\n"
				+ "      }\n"
				+ "    }\n"
				+ "  }\n"
				+ "}";

		try (RepositoryConnection con = testRepository.getConnection()) {
			GraphQuery graphQuery = con.prepareGraphQuery(queryString);
			try (GraphQueryResult result = graphQuery.evaluate()) {
				List<Statement> statements = QueryResults.asList(result);
				Assertions.assertEquals(29, statements.size());
			}
		}
	}

	@Test
	public void testSparqlStarInObjectPosition() {
		testCon.add(Values.bnode(), FOAF.KNOWS, Values.triple(Values.bnode(), FOAF.NAME, Values.literal("John Doe")));
		TupleQuery tupleQuery = testCon.prepareTupleQuery(
				"PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
						"SELECT * WHERE { ?a ?b <<?s foaf:name ?o>>. }");
		try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
			List<BindingSet> collect = evaluate.stream().collect(Collectors.toList());
			Assertions.assertEquals(1, collect.size());
		}
	}

	protected abstract Repository createRepository();
}
