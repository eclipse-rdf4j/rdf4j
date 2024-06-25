/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.jupiter.api.DynamicTest;

/**
 * Test on SPARQL BIND function.
 *
 * @author Jeen Broekstra
 *
 */
public class BindTest extends AbstractComplianceTest {

	public BindTest(Supplier<Repository> repo) {
		super(repo);
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1018
	 */

	private void testBindError(RepositoryConnection conn) {

		conn.prepareUpdate(QueryLanguage.SPARQL, "insert data { <urn:test:subj> <urn:test:pred> _:blank }").execute();

		String qb = "SELECT * \n" +
				"WHERE { \n" +
				"  VALUES (?NAValue) { (<http://null>) } \n " +
				"  BIND(IF(?NAValue != <http://null>, ?NAValue, ?notBoundVar) as ?ValidNAValue) \n " +
				"  { ?disjClass (owl:disjointWith|^owl:disjointWith)? ?disjClass2 . }\n" +
				"}\n";

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(qb).evaluate());

		assertEquals(2, result.size(), "query should return 2 solutions");
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1405
	 */

	private void testBindScope(RepositoryConnection conn) {
		String query = "SELECT * {\n" +
				"  { BIND (\"a\" AS ?a) }\n" +
				"  { BIND (?a AS ?b) } \n" +
				"}";

		TupleQuery q = conn.prepareTupleQuery(query);
		List<BindingSet> result = QueryResults.asList(q.evaluate());

		assertEquals(1, result.size());

		assertEquals(conn.getValueFactory().createLiteral("a"), result.get(0).getValue("a"));
		assertNull(result.get(0).getValue("b"));
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1642
	 */

	private void testBindScopeUnion(RepositoryConnection conn) {

		ValueFactory f = conn.getValueFactory();
		String query = "prefix ex: <http://example.org/> \n" +
				"select * {\n" +
				"  bind(ex:v1 as ?v)\n" +
				"  bind(strafter(str(?v),str(ex:)) as ?b)\n" +
				"  {\n" +
				"    bind(?b as ?b1)\n" +
				"  } union {\n" +
				"    bind(?b as ?b2)\n" +
				"  }\n" +
				"}";

		TupleQuery q = conn.prepareTupleQuery(query);
		List<BindingSet> result = QueryResults.asList(q.evaluate());

		assertEquals(2, result.size());

		IRI v1 = f.createIRI("http://example.org/v1");
		Literal b = f.createLiteral("v1");
		for (BindingSet bs : result) {
			assertThat(bs.getValue("v")).isEqualTo(v1);
			assertThat(bs.getValue("b1")).isNull();
			assertThat(bs.getValue("b2")).isNull();
		}

	}

	private void testSES2250BindErrors(RepositoryConnection conn) {

		conn.prepareUpdate(QueryLanguage.SPARQL, "insert data { <urn:test:subj> <urn:test:pred> _:blank }").execute();

		String qb = "SELECT * {\n" +
				"    ?s1 ?p1 ?blank . " +
				"    FILTER(isBlank(?blank))" +
				"    BIND (iri(?blank) as ?biri)" +
				"    ?biri ?p2 ?o2 ." +
				"}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb);
		try (TupleQueryResult evaluate = tq.evaluate()) {
			assertFalse(evaluate.hasNext(), "The query should not return a result");
		}
	}

	private void testSES2250BindErrorsInPath(RepositoryConnection conn) {

		conn.prepareUpdate(QueryLanguage.SPARQL, "insert data { <urn:test:subj> <urn:test:pred> _:blank }").execute();

		String qb = "SELECT * {\n" +
				"    ?s1 ?p1 ?blank . " +
				"    FILTER(isBlank(?blank))" +
				"    BIND (iri(?blank) as ?biri)" +
				"    ?biri <urn:test:pred>* ?o2 ." +
				"}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb);
		try (TupleQueryResult evaluate = tq.evaluate()) {
			assertFalse(evaluate.hasNext(), "The query should not return a result");
		}

	}

	public void testSelectBindOnly(RepositoryConnection conn) {
		String query = "select ?b1 ?b2 ?b3\n"
				+ "where {\n"
				+ "  bind(1 as ?b1)\n"
				+ "}";

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());

		assertThat(result.size()).isEqualTo(1);
		BindingSet solution = result.get(0);

		assertThat(solution.getValue("b1")).isEqualTo(literal("1", CoreDatatype.XSD.INTEGER));
		assertThat(solution.getValue("b2")).isNull();
		assertThat(solution.getValue("b3")).isNull();
	}

	private void testGH3696Bind(RepositoryConnection conn) {

		Model testData = new ModelBuilder().setNamespace("ex", "http://example.org/")
				.subject("ex:unit1")
				.add(RDF.TYPE, "ex:Unit")
				.add(RDFS.LABEL, "Unit1")
				.add("ex:has", "Unit1")
				.subject("ex:unit2")
				.add(RDF.TYPE, "ex:Unit")
				.add(RDFS.LABEL, "Unit2")
				.build();
		conn.add(testData);

		String query = "PREFIX ex: <http://example.org/>\n" +
				"SELECT  * {\n" +
				"  ?bind rdfs:label ?b1 ;\n" +
				"        a ex:Unit .\n" +
				"  FILTER (?b1 = 'Unit2') .\n" +
				"  BIND(?bind AS ?n0)\n" +
				"  ?n0 ex:has ?n1 \n" +
				" }";

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());

		assertThat(result).isEmpty();
	}

	private void testGH4499BindFilterNotExist1(RepositoryConnection conn) {
		Model testData = new ModelBuilder().setNamespace("ex", "http://example.org/")
				.subject("ex:a")
				.add("ex:p", "ex:c1")
				.add("ex:p", "ex:c2")
				.add("ex:p", "ex:c3")
				.subject("ex:c1")
				.add(RDF.TYPE, "ex:T")
				.add("ex:q", "something")
				.subject("ex:c2")
				.add(RDF.TYPE, "ex:T")
				.build();
		conn.add(testData);

		String query = "PREFIX ex: <http://example.org/>\n"
				+ "SELECT *\n"
				+ "    WHERE {\n"
				+ "            BIND ( ex:a AS ?a )\n"
				+ "            BIND ( ex:b AS ?b )\n"
				+ "            ?a ex:p* ?c .\n"
				+ "            FILTER EXISTS { ?c rdf:type ex:T }\n"
				+ "            FILTER NOT EXISTS { ?c ex:q ?d}\n"
				+ "}";

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());

		assertThat(result).hasSize(1);

		var bs = result.get(0);

		assertThat(bs.getValue("a").stringValue()).isEqualTo("http://example.org/a");
		assertThat(bs.getValue("c").stringValue()).isEqualTo("http://example.org/c2");
		assertThat(bs.getValue("d")).isNull();
	}

	private void testGH4499BindFilterNotExist2(RepositoryConnection conn) {
		Model testData = new ModelBuilder().setNamespace("ex", "http://example.org/")
				.subject("ex:a")
				.add("ex:p", "ex:c1")
				.add("ex:p", "ex:c2")
				.add("ex:p", "ex:c3")
				.subject("ex:c1")
				.add(RDF.TYPE, "ex:T")
				.add("ex:q", "something")
				.subject("ex:c2")
				.add(RDF.TYPE, "ex:T")
				.build();
		conn.add(testData);

		String query = "PREFIX ex: <http://example.org/>\n"
				+ "SELECT *\n"
				+ "    WHERE {\n"
				+ "            FILTER EXISTS { ?c rdf:type ex:T }\n"
				+ "            FILTER NOT EXISTS { ?c ex:q ?d }\n"
				+ "            BIND ( ex:a AS ?a )\n"
				+ "            BIND ( ex:b AS ?b )\n"
				+ "            ?a ex:p* ?c .\n"
				+ "}";

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());

		assertThat(result).hasSize(1);

		var bs = result.get(0);

		assertThat(bs.getValue("a").stringValue()).isEqualTo("http://example.org/a");
		assertThat(bs.getValue("c").stringValue()).isEqualTo("http://example.org/c2");
		assertThat(bs.getValue("d")).isNull();

	}

	public Stream<DynamicTest> tests() {
		return Stream.of(makeTest("GH4499BindFilterNotExist2", this::testGH4499BindFilterNotExist2),
				makeTest("GH4499BindFilterNotExist1", this::testGH4499BindFilterNotExist1),
				makeTest("GH3696Bind", this::testGH3696Bind), makeTest("SelectBindOnly", this::testSelectBindOnly),
				makeTest("SES2250BindErrorsInPath", this::testSES2250BindErrorsInPath),
				makeTest("SES2250BindErrors", this::testSES2250BindErrors),
				makeTest("BindScopeUnion", this::testBindScopeUnion), makeTest("BindScope", this::testBindScope),
				makeTest("BindError", this::testBindError));
	}
}
