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
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.jupiter.api.DynamicTest;

/**
 * Tests on SPARQL VALUES clauses.
 *
 * @author Jeen Broekstra
 */
public class ValuesTest extends AbstractComplianceTest {

	public ValuesTest(Supplier<Repository> repo) {
		super(repo);
	}

	private void testValuesInOptional(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-ses1692.trig", conn);
		String query = " PREFIX : <http://example.org/>\n" +
				" SELECT DISTINCT ?a ?name ?isX WHERE { ?b :p1 ?a . ?a :name ?name. OPTIONAL { ?a a :X . VALUES(?isX) { (:X) } } } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				// System.out.println(bs);
				IRI a = (IRI) bs.getValue("a");
				assertNotNull(a);
				Value isX = bs.getValue("isX");
				Literal name = (Literal) bs.getValue("name");
				assertNotNull(name);
				if (a.stringValue().endsWith("a1")) {
					assertNotNull(isX);
				} else if (a.stringValue().endsWith(("a2"))) {
					assertNull(isX);
				}
			}
			assertEquals(2, count);
		}

	}

	private void testValuesClauseNamedGraph(RepositoryConnection conn) throws Exception {

		String ex = "http://example.org/";
		String data = "@prefix foaf: <" + FOAF.NAMESPACE + "> .\n"
				+ "@prefix ex: <" + ex + "> .\n"
				+ "ex:graph1 {\n" +
				"	ex:Person1 rdf:type foaf:Person ;\n" +
				"		foaf:name \"Person 1\" .	ex:Person2 rdf:type foaf:Person ;\n" +
				"		foaf:name \"Person 2\" .	ex:Person3 rdf:type foaf:Person ;\n" +
				"		foaf:name \"Person 3\" .\n" +
				"}";

		conn.add(new StringReader(data), "", RDFFormat.TRIG);

		String query = "SELECT  ?person ?name ?__index \n"
				+ "WHERE { "
				+ "        VALUES (?person ?name  ?__index) { \n"
				+ "                  (<http://example.org/Person1> UNDEF \"0\") \n"
				+ "                  (<http://example.org/Person3> UNDEF \"2\")  } \n"
				+ "        GRAPH <http://example.org/graph1> { ?person <http://xmlns.com/foaf/0.1/name> ?name .   } }";

		TupleQuery q = conn.prepareTupleQuery(query);

		List<BindingSet> result = QueryResults.asList(q.evaluate());
		assertThat(result).hasSize(2);
	}

	private void testValuesCartesianProduct(RepositoryConnection conn) {
		final String queryString = ""
				+ "select ?x ?y where { "
				+ "  values ?x { undef 67 } "
				+ "  values ?y { undef 42 } "
				+ "}";
		final TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);

		List<BindingSet> bindingSets = QueryResults.asList(tupleQuery.evaluate());
		assertThat(bindingSets).hasSize(4);
	}

	private void testSES1081SameTermWithValues(RepositoryConnection conn) throws Exception {

		loadTestData("/testdata-query/dataset-ses1081.trig", conn);
		String query = "PREFIX ex: <http://example.org/>\n" +
				" SELECT * \n" +
				" WHERE { \n " +
				"          ?s ex:p ?a . \n" +
				"          FILTER sameTerm(?a, ?e) \n " +
				"          VALUES ?e { ex:b } \n " +
				" } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				Value s = bs.getValue("s");
				Value a = bs.getValue("a");

				assertNotNull(s);
				assertNotNull(a);
				assertEquals(iri("http://example.org/a"), s);
				assertEquals(iri("http://example.org/b"), a);
			}
			assertEquals(1, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	private void testSES2136(RepositoryConnection conn) throws Exception {

		loadTestData("/testcases-sparql-1.1-w3c/bindings/data02.ttl", conn);
		String query = "PREFIX : <http://example.org/>\n" +
				"SELECT ?s ?o { \n" +
				" { SELECT * WHERE { ?s ?p ?o . } }\n" +
				"	VALUES (?o) { (:b) }\n" +
				"}\n";

		ValueFactory vf = conn.getValueFactory();
		final IRI a = vf.createIRI("http://example.org/a");
		final IRI b = vf.createIRI("http://example.org/b");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			assertFalse(result.hasNext(), "only one result expected");
			assertEquals(a, bs.getValue("s"));
			assertEquals(b, bs.getValue("o"));
		}

	}

	/**
	 * https://github.com/eclipse/rdf4j/issues/1026
	 */

	private void testFilterExistsExternalValuesClause(RepositoryConnection conn) {
		String ub = "insert data {\n" +
				"  <http://subj1> a <http://type> .\n" +
				"  <http://subj2> a <http://type> .\n" +
				"  <http://subj1> <http://predicate> <http://obj1> .\n" +
				"  <http://subj2> <http://predicate> <http://obj2> .\n" +
				"}";
		conn.prepareUpdate(QueryLanguage.SPARQL, ub).execute();

		String query = "select ?s  {\n"
				+ "    ?s a* <http://type> .\n"
				+ "    FILTER EXISTS {?s <http://predicate> ?o}\n"
				+ "} limit 100 values ?o {<http://obj1>}";

		TupleQuery tq = conn.prepareTupleQuery(query);

		List<BindingSet> result = QueryResults.asList(tq.evaluate());
		assertEquals(1, result.size(), "single result expected");
		assertEquals("http://subj1", result.get(0).getValue("s").stringValue());
	}

	public void testMultipleValuesClauses(RepositoryConnection conn) {
		Update update = conn.prepareUpdate("PREFIX ex: <http://example.org/>\n" +
				"\n" +
				"INSERT DATA { ex:sub ex:somePred \"value\" . };\n" +
				"\n" +
				"INSERT { ?s ?newPred ?newObj }\n" +
				"WHERE {\n" +
				"  # If one combines these into a single VALUES clause then it also works\n" +
				"  VALUES ?newPred { ex:somePred2 }\n" +
				"  VALUES ?newObj { \"value2\" }\n" +
				"  ?s ex:somePred [] .\n" +
				"}");
		update.execute();
	}

	public Stream<DynamicTest> tests() {
		return Stream.of(makeTest("ValuesInOptional", this::testValuesInOptional),
				makeTest("ValuesClauseNamedGraph", this::testValuesClauseNamedGraph),
				makeTest("ValuesCartesianProduct", this::testValuesCartesianProduct),
				makeTest("SES1081SameTermWithValues", this::testSES1081SameTermWithValues),
				makeTest("SES2136", this::testSES2136),
				makeTest("FilterExistsExternalValuesClause", this::testFilterExistsExternalValuesClause),
				makeTest("MultipleValuesClauses", this::testMultipleValuesClauses));
	}
}
