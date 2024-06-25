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
package org.eclipse.rdf4j.federated;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class PropertyPathTests extends SPARQLBaseTest {

	@BeforeEach
	public void before() {
		QueryManager qm = federationContext().getQueryManager();
		qm.addPrefixDeclaration("owl", OWL.NAMESPACE);
		qm.addPrefixDeclaration("rdfs", RDFS.NAMESPACE);
		qm.addPrefixDeclaration("skos", SKOS.NAMESPACE);
		qm.addPrefixDeclaration("foaf", FOAF.NAMESPACE);
		qm.addPrefixDeclaration("", EXAMPLE_NAMESPACE);
	}

	@Test
	public void testArbitratyLengthPath() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { ?subClass rdfs:subClassOf+ :MyClass . ?subClass rdfs:label ?label }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);
			assertContainsAll(res, "subClass",
					Sets.newHashSet(iri("MySubClass1"), iri("MySubClass2"), iri("MySubSubClass1")));
		}
	}

	@Test
	public void testPropertyPath() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { ?subClass rdfs:subClassOf/rdfs:label ?label }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);
			assertContainsAll(res, "subClass",
					Sets.newHashSet(iri("MySubClass1"), iri("MySubClass2"), iri("MySubSubClass1"), FOAF.PERSON));
		}
	}

	@Test
	public void testPropertyPath_Alternatives() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { ?concept a :MyClass . ?concept rdfs:label|skos:altLabel ?label }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);

			assertContainsAll(res, "label",
					Sets.newHashSet(l("Concept1"), l("Concept1 AltLabel"), l("Concept2"), l("Concept2 AltLabel"),
							l("Concept3"), l("Concept3 AltLabel")));
		}
	}

	@Test
	public void testPropertyPath_ExclusiveGroup() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { ?concept a skos:Concept . ?concept skos:broader+ :mammals . ?concept rdfs:label ?label}";

		String actualQueryPlan = federationContext().getQueryManager().getQueryPlan(query);
		assertQueryPlanEquals(readResourceAsString("/tests/propertypath/query_path_exclusiveGroup.qp"),
				actualQueryPlan);

		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);

			assertContainsAll(res, "label",
					Sets.newHashSet(l("Bovinae"), l("Cows")));
			assertContainsAll(res, "concept",
					Sets.newHashSet(iri("bovinae"), iri("cows")));
		}
	}

	@Test
	public void testPropertyPath_Combination() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { ?x rdf:type/rdfs:subClassOf* foaf:Agent . ?x rdfs:label ?label}";

		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);

			assertContainsAll(res, "label",
					Sets.newHashSet(l("Person 1"), l("Person 2")));
		}
	}

	@Test
	public void testPropertyPath_BoundInJoin() throws Exception {

		prepareTest(Arrays.asList("/tests/propertypath/data1.ttl", "/tests/propertypath/data2.ttl"));

		String query = "SELECT * WHERE { BIND(foaf:Person AS ?subClass) . ?subClass rdfs:subClassOf+ foaf:Agent . ?subClass rdfs:label ?label }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);
			assertContainsAll(res, "subClass", Sets.newHashSet(FOAF.PERSON));
			assertContainsAll(res, "label", Sets.newHashSet(l("Person")));
		}

	}

	@Test
	public void testZeroLengthPath_length1() throws Exception {

		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);

		try (RepositoryConnection con = repo1.getConnection()) {
			con.add(Values.iri("http://example.org/A"), RDFS.SUBCLASSOF, Values.iri("http://example.org/B"));
		}

		try (RepositoryConnection con = repo2.getConnection()) {
			con.add(Values.iri("http://example.org/X"), RDFS.SUBCLASSOF, Values.iri("http://example.org/Y"));
		}

		Repository fedxRepo = fedxRule.getRepository();

		// 1: variable subject & object
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "SELECT * WHERE { "
							+ "   ?subClass (rdfs:subClassOf*) ?myClass. "
							+ " } "
			);

			Assertions.assertEquals(6, QueryResults.asSet(tupleQuery.evaluate()).size());
		}

		// 2: bound (matching) object
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "SELECT * WHERE { "
							+ "   ?subClass (rdfs:subClassOf*) ?myClass. "
							+ " } "
			);
			tupleQuery.setBinding("myClass", Values.iri("http://example.org/B"));

			Assertions.assertEquals(2, QueryResults.asSet(tupleQuery.evaluate()).size());
		}

		// 3: bound (non-matching) object
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "PREFIX ex: <http://example.com/> "
							+ "SELECT * WHERE { "
							+ "   ?subClass (ex:something*) ?myClass. "
							+ " } "
			);
			tupleQuery.setBinding("myClass", Values.iri("http://example.org/B"));

			Assertions.assertEquals(1, QueryResults.asSet(tupleQuery.evaluate()).size());
		}
	}

	@Test
	public void testZeroLengthPath_length2() throws Exception {

		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);

		try (RepositoryConnection con = repo1.getConnection()) {
			con.add(Values.iri("http://example.org/A"), RDFS.SUBCLASSOF, Values.iri("http://example.org/B"));
			con.add(Values.iri("http://example.org/B"), RDFS.SUBCLASSOF, Values.iri("http://example.org/C"));
		}

		try (RepositoryConnection con = repo2.getConnection()) {
			con.add(Values.iri("http://example.org/X"), RDFS.SUBCLASSOF, Values.iri("http://example.org/Y"));
		}

		Repository fedxRepo = fedxRule.getRepository();

		// 1: variable subject & object
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "SELECT * WHERE { "
							+ "   ?subClass (rdfs:subClassOf*) ?myClass. "
							+ " } "
			);

			Assertions.assertEquals(9, QueryResults.asSet(tupleQuery.evaluate()).size());
		}

		// 2: bound (matching) object
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "SELECT * WHERE { "
							+ "   ?subClass (rdfs:subClassOf*) ?myClass. "
							+ " } "
			);
			tupleQuery.setBinding("myClass", Values.iri("http://example.org/C"));

			Assertions.assertEquals(3, QueryResults.asSet(tupleQuery.evaluate()).size());
		}

		// 2a: bound (matching) object
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "SELECT * WHERE { "
							+ "   ?subClass (rdfs:subClassOf*) <http://example.org/C> . "
							+ " } "
			);

			Assertions.assertEquals(3, QueryResults.asSet(tupleQuery.evaluate()).size());
		}

		// 3: bound (non-matching) object
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "PREFIX ex: <http://example.com/> "
							+ "SELECT * WHERE { "
							+ "   ?subClass (ex:something*) ?myClass. "
							+ " } "
			);
			tupleQuery.setBinding("myClass", Values.iri("http://example.org/C"));

			Assertions.assertEquals(1, QueryResults.asSet(tupleQuery.evaluate()).size());
		}
	}

	@Test
	public void testZeroLengthPath_length2_crossRepository() throws Exception {

		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);

		try (RepositoryConnection con = repo1.getConnection()) {
			con.add(Values.iri("http://example.org/A"), RDFS.SUBCLASSOF, Values.iri("http://example.org/B"));
		}

		try (RepositoryConnection con = repo2.getConnection()) {
			con.add(Values.iri("http://example.org/B"), RDFS.SUBCLASSOF, Values.iri("http://example.org/C"));
		}

		Repository fedxRepo = fedxRule.getRepository();

		// 2a: bound (matching) object
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "SELECT * WHERE { "
							+ "   ?subClass (rdfs:subClassOf*) <http://example.org/C> . "
							+ " } "
			);

			Assertions.assertEquals(3, QueryResults.asSet(tupleQuery.evaluate()).size());
		}

	}

	@Test
	public void testPropertyPath_sourceSelection_crossRepository() throws Exception {

		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);

		try (RepositoryConnection con = repo1.getConnection()) {
			con.add(Values.iri("http://example.org/A"), RDFS.SUBCLASSOF, Values.iri("http://example.org/B"),
					Values.iri("http://example.org/graph1"));
		}

		try (RepositoryConnection con = repo2.getConnection()) {
			con.add(Values.iri("http://example.org/B"), RDFS.SUBCLASSOF, Values.iri("http://example.org/C"),
					Values.iri("http://example.org/graph2"));
		}

		Repository fedxRepo = fedxRule.getRepository();

		// 1a: bound (matching) object
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "SELECT * WHERE { "
							+ "   ?subClass (rdfs:subClassOf+) <http://example.org/C> . "
							+ " } "
			);

			Assertions.assertEquals(2, QueryResults.asSet(tupleQuery.evaluate()).size());
		}

		// 1b: with named graph
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "SELECT * WHERE { "
							+ " GRAPH <http://example.org/graph2> {"
							+ "   ?subClass (rdfs:subClassOf+) <http://example.org/C> . "
							+ " }"
							+ "} "
			);

			Assertions.assertEquals(1, QueryResults.asSet(tupleQuery.evaluate()).size());
		}

	}

}
