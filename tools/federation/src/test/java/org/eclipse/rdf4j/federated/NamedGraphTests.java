/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NamedGraphTests extends SPARQLBaseTest {

	private static class TestVocabulary {
		public final String NAMESPACE;

		public final IRI GRAPH_1;
		public final IRI GRAPH_2;
		public final IRI SHARED_GRAPH;

		public TestVocabulary(String namespace) {
			this.NAMESPACE = namespace;
			this.GRAPH_1 = iri("graph1");
			this.GRAPH_2 = iri("graph2");
			this.SHARED_GRAPH = iri("sharedGraph");
		}

		public IRI iri(String localName) {
			return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
		}
	}

	private static final TestVocabulary NS_1 = new TestVocabulary("http://namespace1.org/");
	private static final TestVocabulary NS_2 = new TestVocabulary("http://namespace2.org/");
	private static final TestVocabulary NS_3 = new TestVocabulary("http://namespace3.org/");
	private static final TestVocabulary NS_4 = new TestVocabulary("http://namespace4.org/");
	private static final TestVocabulary EX = new TestVocabulary("http://example.org/");

	@BeforeEach
	public void registerPrefixes() {
		QueryManager qm = federationContext().getQueryManager();
		qm.addPrefixDeclaration("foaf", FOAF.NAMESPACE);
		qm.addPrefixDeclaration("owl", OWL.NAMESPACE);
		qm.addPrefixDeclaration("ns1", NS_1.NAMESPACE);
		qm.addPrefixDeclaration("ns4", NS_4.NAMESPACE);
	}

	@Test
	public void testGetContextIDs() throws Exception {
		prepareTest(Arrays.asList("/tests/named-graphs/data1.trig", "/tests/named-graphs/data2.trig",
				"/tests/named-graphs/data3.trig", "/tests/named-graphs/data4.trig"));

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {
			List<Resource> graphs = Iterations.asList(conn.getContextIDs());

			assertThat(graphs).containsExactlyInAnyOrder(NS_1.GRAPH_1, NS_1.GRAPH_2, NS_3.SHARED_GRAPH, NS_2.GRAPH_1,
					NS_2.GRAPH_2);
		}
	}

	@Test
	public void testGetStatements() throws Exception {

		prepareTest(Arrays.asList("/tests/named-graphs/data1.trig", "/tests/named-graphs/data2.trig",
				"/tests/named-graphs/data3.trig", "/tests/named-graphs/data4.trig"));

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {

			// 1. named graph only present in single endpoint
			assertThat(Models.subjectIRIs(conn.getStatements(null, RDF.TYPE, FOAF.PERSON, NS_1.GRAPH_1)))
					.containsExactlyInAnyOrder(NS_1.iri("Person_1"), NS_1.iri("Person_2"));

			// 2. multiple graphs
			assertThat(Models.subjectIRIs(conn.getStatements(null, RDF.TYPE, FOAF.PERSON, NS_1.GRAPH_1, NS_2.GRAPH_1)))
					.containsExactlyInAnyOrder(
							NS_1.iri("Person_1"), NS_1.iri("Person_2"), NS_2.iri("Person_6"), NS_2.iri("Person_7"));

			// 3. graph is available in multiple endpoints
			assertThat(Models.subjectIRIs(conn.getStatements(null, RDF.TYPE, FOAF.PERSON, NS_3.SHARED_GRAPH)))
					.containsExactlyInAnyOrder(NS_1.iri("Person_5"), NS_2.iri("Person_10"));
		}
	}

	@Test
	public void testSimpleSelect_FromClause() throws Exception {

		prepareTest(Arrays.asList("/tests/named-graphs/data1.trig", "/tests/named-graphs/data2.trig",
				"/tests/named-graphs/data3.trig", "/tests/named-graphs/data4.trig"));

		List<BindingSet> res;

		// 1. named graph only present in single endpoint
		res = runQuery("SELECT ?person FROM <http://namespace1.org/graph1> WHERE { ?person a foaf:Person }");
		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_1"), NS_1.iri("Person_2"));

		// 2. multiple graphs
		res = runQuery(
				"SELECT ?person FROM <http://namespace1.org/graph1> "
						+ "FROM <http://namespace2.org/graph1> "
						+ "WHERE { ?person a foaf:Person }");
		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_1"), NS_1.iri("Person_2"), NS_2.iri("Person_6"),
						NS_2.iri("Person_7"));

		// 3. graph is available in multiple endpoints
		res = runQuery(
				"SELECT ?person FROM <http://namespace3.org/sharedGraph> WHERE { ?person a foaf:Person }");
		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_5"), NS_2.iri("Person_10"));
	}

	@Test
	public void testSimpleSelect_GraphClause() throws Exception {

		prepareTest(Arrays.asList("/tests/named-graphs/data1.trig", "/tests/named-graphs/data2.trig",
				"/tests/named-graphs/data3.trig", "/tests/named-graphs/data4.trig"));

		List<BindingSet> res;

		// 1. named graph only present in single endpoint
		res = runQuery(
				"SELECT ?person WHERE { GRAPH <http://namespace1.org/graph1> { ?person a foaf:Person } }");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_1"), NS_1.iri("Person_2"));

		// 3. graph is available in multiple endpoints
		res = runQuery(
				"SELECT ?person WHERE { GRAPH <http://namespace3.org/sharedGraph> { ?person a foaf:Person } }");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_5"), NS_2.iri("Person_10"));

	}

	@Test
	public void testSimpleSelect_FromNamedClause() throws Exception {

		prepareTest(Arrays.asList("/tests/named-graphs/data1.trig", "/tests/named-graphs/data2.trig",
				"/tests/named-graphs/data3.trig", "/tests/named-graphs/data4.trig"));

		List<BindingSet> res;

		res = runQuery("SELECT ?person FROM NAMED <http://namespace1.org/graph1> "
				+ "WHERE { GRAPH <http://namespace1.org/graph1> { ?person a foaf:Person } }");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_1"), NS_1.iri("Person_2"));

		res = runQuery("SELECT ?person FROM NAMED <http://namespace1.org/graph1> "
				+ "WHERE { ?person a foaf:Person }");
		assertThat(values(res, "person")).isEmpty();

		// 3. graph is available in multiple endpoints
		res = runQuery("SELECT ?person FROM NAMED <http://namespace3.org/sharedGraph> "
				+ " WHERE { GRAPH <http://namespace3.org/sharedGraph> { ?person a foaf:Person } }");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_5"), NS_2.iri("Person_10"));

	}

	@Test
	public void testSimpleSelect_ExclusiveGroup_GraphClause() throws Exception {

		prepareTest(Arrays.asList("/tests/named-graphs/data1.trig", "/tests/named-graphs/data2.trig",
				"/tests/named-graphs/data3.trig", "/tests/named-graphs/data4.trig"));

		List<BindingSet> res;

		// 1. named graph only present in single endpoint
		// => single source query
		res = runQuery(
				"SELECT ?person ?name WHERE { GRAPH <http://namespace1.org/graph1> { ?person a foaf:Person . ?person foaf:name ?name } }");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_1"), NS_1.iri("Person_2"));
		assertThat(values(res, "name"))
				.containsExactlyInAnyOrder(l("Person1"), l("Person2"));

		// 2. graph is available in multiple endpoints
		res = runQuery(
				"SELECT ?person ?name WHERE { GRAPH <http://namespace3.org/sharedGraph> { ?person a foaf:Person . ?person foaf:name ?name } }");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_5"), NS_2.iri("Person_10"));
		assertThat(values(res, "name"))
				.containsExactlyInAnyOrder(l("Person5"), l("Person10"));

		// 3. graph is available in multiple endpoints, data is exclusive to ep1
		// join argument is present in other endpoint
		res = runQuery(
				"SELECT ?person ?name WHERE { "
						+ "GRAPH <http://namespace3.org/sharedGraph> { ns1:Person_5 a foaf:Person . ns1:Person_5 foaf:name ?name }  "
						+ "?author owl:sameAs ns1:Person_5 "
						+ "}");

		assertThat(values(res, "name"))
				.containsExactlyInAnyOrder(l("Person5"));
	}

	@Test
	public void testSelect_JoinOfGraphs() throws Exception {

		prepareTest(Arrays.asList("/tests/named-graphs/data1.trig", "/tests/named-graphs/data2.trig",
				"/tests/named-graphs/data3.trig", "/tests/named-graphs/data4.trig"));

		List<BindingSet> res;

		res = runQuery(
				"SELECT ?person ?author WHERE { "
						+ "GRAPH <http://namespace1.org/graph1> { ?person a foaf:Person  } "
						+ "GRAPH <http://namespace3.org/sharedGraph> { ?author owl:sameAs ?person } "
						+ "}");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_2"));
		assertThat(values(res, "author"))
				.containsExactlyInAnyOrder(NS_4.iri("Author_2"));

		// 2. more complex join groups
		res = runQuery(
				"SELECT ?person ?author ?name ?authorId WHERE { "
						+ "GRAPH <http://namespace1.org/graph1> { ?person a foaf:Person . ?person foaf:name ?name } "
						+ "GRAPH <http://namespace3.org/sharedGraph> { ?author owl:sameAs ?person . ?author ns4:authorId ?authorId } "
						+ "}");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_2"));
		assertThat(values(res, "author"))
				.containsExactlyInAnyOrder(NS_4.iri("Author_2"));
		assertThat(values(res, "name"))
				.containsExactlyInAnyOrder(l("Person2"));
		assertThat(values(res, "authorId"))
				.containsExactlyInAnyOrder(l("Author2"));
	}

	@Test
	public void testBoundJoin() throws Exception {

		prepareTest(
				Arrays.asList("/tests/named-graphs/data-boundjoin1.trig", "/tests/named-graphs/data-boundjoin2.trig"));

		List<BindingSet> res;
		res = runQuery(
				"SELECT ?person ?name WHERE { GRAPH <http://example.org/graph1> { ?person a foaf:Person . ?person foaf:name ?name } }");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(EX.iri("Person1"), EX.iri("Person2"), EX.iri("Person3"),
						EX.iri("Person4"), EX.iri("Person11"), EX.iri("Person12"),
						EX.iri("Person13"), EX.iri("Person14"));
		assertThat(values(res, "name"))
				.containsExactlyInAnyOrder(l("Person 1"), l("Person 2"), l("Person 3"), l("Person 4"),
						l("Person 11"), l("Person 12"), l("Person 13"), l("Person 14"));
	}

	@Test
	public void testBoundJoin_FROM_CLAUSE() throws Exception {

		prepareTest(
				Arrays.asList("/tests/named-graphs/data-boundjoin1.trig", "/tests/named-graphs/data-boundjoin2.trig"));

		List<BindingSet> res;
		res = runQuery(
				"SELECT ?person ?name FROM <http://example.org/graph1> WHERE {  ?person a foaf:Person . ?person foaf:name ?name }");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(EX.iri("Person1"), EX.iri("Person2"), EX.iri("Person3"),
						EX.iri("Person4"), EX.iri("Person11"), EX.iri("Person12"),
						EX.iri("Person13"), EX.iri("Person14"));
		assertThat(values(res, "name"))
				.containsExactlyInAnyOrder(l("Person 1"), l("Person 2"), l("Person 3"), l("Person 4"),
						l("Person 11"), l("Person 12"), l("Person 13"), l("Person 14"));
	}

	@Test
	public void testVariableGraph() throws Exception {

		prepareTest(Arrays.asList("/tests/named-graphs/data1.trig", "/tests/named-graphs/data2.trig",
				"/tests/named-graphs/data3.trig", "/tests/named-graphs/data4.trig"));

		List<BindingSet> res;

		res = runQuery(
				"SELECT DISTINCT ?g WHERE { GRAPH ?g { ?person a foaf:Person } }");

		assertThat(values(res, "g"))
				.containsExactlyInAnyOrder(NS_1.GRAPH_1, NS_1.GRAPH_2, NS_2.GRAPH_1, NS_2.GRAPH_2, NS_3.SHARED_GRAPH);

		res = runQuery(
				"SELECT DISTINCT ?g WHERE { GRAPH ?g { ?person a foaf:Person . ?person foaf:age ?age } }");

		System.out.println(res);

		assertThat(values(res, "g"))
				.containsExactlyInAnyOrder(NS_1.GRAPH_1, NS_1.GRAPH_2, NS_2.GRAPH_1);

		res = runQuery(
				"SELECT ?person WHERE { BIND (<http://namespace1.org/graph1> AS ?g) . GRAPH ?g { ?person a foaf:Person } }");

		assertThat(values(res, "person"))
				.containsExactlyInAnyOrder(NS_1.iri("Person_1"), NS_1.iri("Person_2"));
	}

	private List<Value> values(List<BindingSet> result, String bindingName) {
		return result.stream().map(bs -> bs.getValue(bindingName)).collect(Collectors.toList());
	}

	protected List<BindingSet> runQuery(String query) {
		TupleQuery tq = federationContext().getQueryManager().prepareTupleQuery(query);
		try (TupleQueryResult tqr = tq.evaluate()) {
			return Iterations.asList(tqr);
		}

	}
}
