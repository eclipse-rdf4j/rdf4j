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
import static org.eclipse.rdf4j.model.vocabulary.RDF.REIFIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.TripleTerm;
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
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for triple term support in the Repository.
 *
 * @author Jeen Broekstra
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 10, unit = TimeUnit.MINUTES)
public abstract class TripleTermSupportTest {

	private Repository testRepository;

	protected RepositoryConnection testCon;

	private ValueFactory vf;

	private BNode bob;
	private Literal nameBob;

	@BeforeEach
	public void setUp() {
		testRepository = createRepository();

		testCon = testRepository.getConnection();
		testCon.clear();
		testCon.clearNamespaces();

		vf = testRepository.getValueFactory();

		// Initialize values
		bob = vf.createBNode();
		nameBob = vf.createLiteral("Bob");
	}

	@AfterEach
	public void tearDown() {
		try {
			testCon.close();
		} finally {
			testRepository.shutDown();
		}
	}

	@Test
	public void testAddRDFStarObject() {
		TripleTerm tripleTerm = vf.createTripleTerm(bob, FOAF.NAME, nameBob);

		testCon.add(RDF.ALT, RDF.TYPE, tripleTerm);

		assertThat(testCon.hasStatement(RDF.ALT, RDF.TYPE, tripleTerm, false)).isTrue();
	}

	@Test
	public void testSparqlTripleTermInObjectPosition() {
		TripleTerm tripleTerm = vf.createTripleTerm(bob, FOAF.NAME, nameBob);

		testCon.add(RDF.ALT, RDF.TYPE, tripleTerm);

		String query = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
				"SELECT DISTINCT * WHERE { ?s ?p <<( ?s1 foaf:name ?o )>> }";

		List<BindingSet> result = QueryResults.asList(testCon.prepareTupleQuery(query).evaluate());
		assertThat(result).hasSize(1);

		BindingSet bs = result.getFirst();
		assertThat(bs.getValue("s1")).isEqualTo(bob);
		assertThat(bs.getValue("o")).isEqualTo(nameBob);
		assertThat(bs.getValue("p")).isEqualTo(RDF.TYPE);
		assertThat(bs.getValue("s")).isEqualTo(RDF.ALT);

	}

	@Test
	public void testSparqlReifiedTripleOnSubjectPosition() {
		var reifier = vf.createBNode();
		TripleTerm tripleTerm = vf.createTripleTerm(bob, FOAF.NAME, nameBob);

		testCon.add(reifier, REIFIES, tripleTerm);
		testCon.add(reifier, RDF.TYPE, RDF.ALT);

		String query = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
				"SELECT DISTINCT * WHERE { <<?s foaf:name ?o>> ?b ?c }";

		List<BindingSet> result = QueryResults.asList(testCon.prepareTupleQuery(query).evaluate());
		assertThat(result).hasSize(2);

		BindingSet bs = result.getFirst();
		assertThat(bs.getValue("s")).isEqualTo(bob);
		assertThat(bs.getValue("o")).isEqualTo(nameBob);
		assertThat(bs.getValue("b")).isEqualTo(REIFIES);
		assertThat(bs.getValue("c")).isEqualTo(tripleTerm);

		bs = result.get(1);
		assertThat(bs.getValue("s")).isEqualTo(bob);
		assertThat(bs.getValue("o")).isEqualTo(nameBob);
		assertThat(bs.getValue("b")).isEqualTo(RDF.TYPE);
		assertThat(bs.getValue("c")).isEqualTo(RDF.ALT);

	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("tripleTermSetups")
	public void testSparqlUpdateWithTripleTermVariants(String testName, Runnable setup) {
		IRI bobClass = vf.createIRI("urn:Bob");

		setup.run();

		String update = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
				"INSERT { ?s foaf:age 23 } WHERE { <<?s foaf:name ?o>> ?b ?c . }";
		testCon.prepareUpdate(update).execute();

		assertTrue(testCon.hasStatement(bobClass, FOAF.AGE, vf.createLiteral(BigInteger.valueOf(23)), false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("tripleTermInsertionMethods")
	public void testTripleTermAddAndRetrieveSparql(String testName,
			Runnable setup,
			String queryString) {
		testCon.begin();

		setup.run();

		TupleQuery query = testCon.prepareTupleQuery(queryString);

		assertTrue(testCon.prepareBooleanQuery("ASK { ?t a 'I am a triple ;-D'}").evaluate());
		assertEquals(2, getCount(query));

		testCon.commit();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("tripleTermInsertionMethods")
	public void testTripleTermAddAndRetrieveSparqlSeparateTransaction(String testName,
			Runnable setup,
			String queryString) {
		testCon.begin();
		setup.run();
		testCon.commit();

		testCon.begin();
		Assertions.assertTrue(testCon.prepareBooleanQuery("ASK { ?t a 'I am a triple ;-D'}").evaluate());
		TupleQuery tupleQuery = testCon.prepareTupleQuery(queryString);
		Assertions.assertEquals(2, getCount(tupleQuery));
		testCon.commit();
	}

	private static long getCount(TupleQuery tupleQuery) {
		try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
			return evaluate.stream().count();
		}
	}

	@Test
	public void testRdf12AddAndRetrieve() {
		TripleTerm insertedTripleTerm = vf.createTripleTerm(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		TripleTerm copyOfInsertedTripleTerm = vf.createTripleTerm(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		Literal literal = vf.createLiteral("I am a triple ;-D");
		BNode reifier = vf.createBNode();
		testCon.begin();

		testCon.add(reifier, REIFIES, insertedTripleTerm);
		testCon.add(reifier, RDF.TYPE, literal);

		assertEquals(1, testCon.getStatements(null, RDF.TYPE, literal, false).stream().count());
		assertEquals(1,
				testCon.getStatements(null, REIFIES, copyOfInsertedTripleTerm, false).stream().count());
		testCon.commit();
	}

	@Test
	public void testMemoryStore_ReifiedTripleTerm() {
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
				assertEquals(100, statements.size());
			}
		}
	}

	@Test
	public void testReifiedTripleInObjectPosition() {
		var reifier = Values.bnode();
		testCon.add(reifier, REIFIES, Values.tripleTerm(Values.bnode(), FOAF.NAME, Values.literal("John Doe")));
		testCon.add(Values.bnode(), FOAF.KNOWS, reifier);
		TupleQuery tupleQuery = testCon.prepareTupleQuery(
				"PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
						"SELECT * WHERE { ?a ?b <<?s foaf:name ?o>>. }");
		try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
			List<BindingSet> collect = evaluate.stream().toList();
			assertEquals(1, collect.size());
		}
	}

	@Test
	public void testTripleTermInObjectPosition() {
		testCon.add(Values.bnode(), FOAF.KNOWS,
				Values.tripleTerm(Values.bnode(), FOAF.NAME, Values.literal("John Doe")));
		TupleQuery tupleQuery = testCon.prepareTupleQuery(
				"PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
						"SELECT * WHERE { ?a ?b <<( ?s foaf:name ?o )>>. }");
		try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
			List<BindingSet> collect = evaluate.stream().toList();
			assertEquals(1, collect.size());
		}
	}

	protected abstract Repository createRepository();

	private Stream<Arguments> tripleTermSetups() {
		return Stream.of(
				Arguments.of("API with TripleTerm", (Runnable) this::setupViaApi),
				Arguments.of("SPARQL INSERT DATA", (Runnable) this::setupViaSparqlInsertData),
				Arguments.of("SPARQL INSERT WHERE", (Runnable) this::setupViaSparqlInsertWhere)
		);
	}

	private void setupViaApi() {
		BNode reifier = vf.createBNode();
		IRI bobClass = vf.createIRI("urn:Bob");
		TripleTerm tripleTerm = vf.createTripleTerm(bobClass, FOAF.NAME, nameBob);
		testCon.add(reifier, REIFIES, tripleTerm);
		testCon.add(reifier, RDF.TYPE, RDF.ALT);
	}

	private void setupViaSparqlInsertData() {
		String insertQuery = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
				"INSERT DATA { << <urn:Bob> foaf:name \"Bob\" >> a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt> }";
		testCon.prepareUpdate(insertQuery).execute();
	}

	private void setupViaSparqlInsertWhere() {
		String insertQuery = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
				"INSERT { << <urn:Bob> foaf:name \"Bob\" >> a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt> } WHERE {}";
		testCon.prepareUpdate(insertQuery).execute();
	}

	private Stream<Arguments> tripleTermInsertionMethods() {
		return Stream.of(
				Arguments.of(
						"API with TripleTerm and reifier",
						(Runnable) this::insertViaApi,
						"""
								SELECT * WHERE {
								    _:reifier <http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies> <<( <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> )>> ;
								              ?a ?b .
								}
								"""
				),
				Arguments.of(
						"SPARQL INSERT DATA",
						(Runnable) this::insertViaSparqlInsertData,
						"SELECT * WHERE { << <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> " +
								"<http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> " +
								"<http://www.w3.org/1999/02/22-rdf-syntax-ns#object> >> ?a ?b}"
				),
				Arguments.of(
						"SPARQL INSERT WHERE",
						(Runnable) this::insertViaSparqlInsertWhere,
						"SELECT * WHERE { << <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> " +
								"<http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> " +
								"<http://www.w3.org/1999/02/22-rdf-syntax-ns#object> >> ?a ?b}"
				)
		);
	}

	private void insertViaApi() {
		var reifier = vf.createBNode();
		TripleTerm insertedTriple = vf.createTripleTerm(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		Literal literal = vf.createLiteral("I am a triple ;-D");
		testCon.add(reifier, REIFIES, insertedTriple);
		testCon.add(reifier, RDF.TYPE, literal);
	}

	private void insertViaSparqlInsertData() {
		String insertQuery = "INSERT DATA { " +
				"<< <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> " +
				"<http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> " +
				"<http://www.w3.org/1999/02/22-rdf-syntax-ns#object> >> " +
				"a \"I am a triple ;-D\" }";
		testCon.prepareUpdate(insertQuery).execute();
	}

	private void insertViaSparqlInsertWhere() {
		testCon.clear();
		String insertQuery = "INSERT { " +
				"<< <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> " +
				"<http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> " +
				"<http://www.w3.org/1999/02/22-rdf-syntax-ns#object> >> " +
				"a \"I am a triple ;-D\" } WHERE {}";
		testCon.prepareUpdate(insertQuery).execute();
	}
}
