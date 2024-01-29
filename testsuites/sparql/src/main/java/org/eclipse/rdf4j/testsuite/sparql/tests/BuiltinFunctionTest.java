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

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.jupiter.api.DynamicTest;

/**
 * Tests on various SPARQL built-in functions.
 *
 * @author Jeen Broekstra
 *
 */
public class BuiltinFunctionTest extends AbstractComplianceTest {

	public BuiltinFunctionTest(Supplier<Repository> repo) {
		super(repo);
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1267
	 */

	private void testSeconds(RepositoryConnection conn) {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (SECONDS(\"2011-01-10T14:45:13\"^^xsd:dateTime) AS ?sec) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("13", result.next().getValue("sec").stringValue());
			assertFalse(result.hasNext());
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1267
	 */

	private void testSecondsMilliseconds(RepositoryConnection conn) {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (SECONDS(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) AS ?sec) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("13.815", result.next().getValue("sec").stringValue());
			assertFalse(result.hasNext());
		}
	}

	private void testSES1991NOWEvaluation(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl", conn);
		String query = "SELECT ?d WHERE {?s ?p ?o . BIND(NOW() as ?d) } LIMIT 2";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			Literal d1 = (Literal) result.next().getValue("d");
			assertTrue(result.hasNext());
			Literal d2 = (Literal) result.next().getValue("d");
			assertFalse(result.hasNext());
			assertNotNull(d1);
			assertEquals(d1, d2);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void testSES869ValueOfNow(RepositoryConnection conn) {
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				"SELECT ?p ( NOW() as ?n ) { BIND (NOW() as ?p ) }");

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			BindingSet bs = result.next();
			Value p = bs.getValue("p");
			Value n = bs.getValue("n");

			assertNotNull(p);
			assertNotNull(n);
			assertEquals(p, n);
			assertTrue(p == n);
		}
	}

	private void testSES1991UUIDEvaluation(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl", conn);
		String query = "SELECT ?uid WHERE {?s ?p ?o . BIND(UUID() as ?uid) } LIMIT 2";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			IRI uuid1 = (IRI) result.next().getValue("uid");
			IRI uuid2 = (IRI) result.next().getValue("uid");

			assertNotNull(uuid1);
			assertNotNull(uuid2);
			assertNotEquals(uuid1, uuid2);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void testSES1991STRUUIDEvaluation(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl", conn);
		String query = "SELECT ?uid WHERE {?s ?p ?o . BIND(STRUUID() as ?uid) } LIMIT 2";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			Literal uid1 = (Literal) result.next().getValue("uid");
			Literal uid2 = (Literal) result.next().getValue("uid");

			assertNotNull(uid1);
			assertNotEquals(uid1, uid2);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void testSES1991RANDEvaluation(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl", conn);
		String query = "SELECT ?r WHERE {?s ?p ?o . BIND(RAND() as ?r) } LIMIT 3";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			Literal r1 = (Literal) result.next().getValue("r");
			Literal r2 = (Literal) result.next().getValue("r");
			Literal r3 = (Literal) result.next().getValue("r");

			assertNotNull(r1);

			// there is a small chance that two successive calls to the random
			// number generator will generate the exact same value, so we check
			// for
			// three successive calls (still theoretically possible to be
			// identical, but phenomenally unlikely).
			assertFalse(r1.equals(r2) && r1.equals(r3));
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void testSES2121URIFunction(RepositoryConnection conn) {
		String query = "SELECT (URI(\"foo bar\") as ?uri) WHERE {}";
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI uri = (IRI) bs.getValue("uri");
			assertNull(uri, "uri result for invalid URI should be unbound");
		}

		query = "BASE <http://example.org/> SELECT (URI(\"foo bar\") as ?uri) WHERE {}";
		tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI uri = (IRI) bs.getValue("uri");
			assertNotNull(uri, "uri result for valid URI reference should be bound");
		}
	}

	private void test27NormalizeIRIFunction(RepositoryConnection conn) {

		String query = "SELECT (IRI(\"../bar\") as ?Iri) WHERE {}";
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query, "http://example.com/foo/");
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI actual = (IRI) bs.getValue("Iri");
			IRI expected = iri("http://example.com/bar");
			assertEquals(expected, actual, "IRI result for relative IRI should be normalized");
		}
	}

	private void testSES2052If1(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-query.trig", conn);
		String query = "SELECT ?p \n" +
				"WHERE { \n" +
				"         ?s ?p ?o . \n" +
				"        FILTER(IF(BOUND(?p), ?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>, false)) \n" +
				"}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			while (result.hasNext()) {
				BindingSet bs = result.next();

				IRI p = (IRI) bs.getValue("p");
				assertNotNull(p);
				assertEquals(RDF.TYPE, p);
			}
		}
	}

	private void testSES2052If2(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-query.trig", conn);
		String query = "SELECT ?p \n" +
				"WHERE { \n" +
				"         ?s ?p ?o . \n" +
				"        FILTER(IF(!BOUND(?p), false , ?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)) \n" +
				"}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			while (result.hasNext()) {
				BindingSet bs = result.next();

				IRI p = (IRI) bs.getValue("p");
				assertNotNull(p);
				assertEquals(RDF.TYPE, p);
			}
		}
	}

	private void testRegexCaseNonAscii(RepositoryConnection conn) {

		String query = "ask {filter (regex(\"Валовой\", \"валовой\", \"i\")) }";

		assertTrue(conn.prepareBooleanQuery(query).evaluate(), "case-insensitive match on Cyrillic should succeed");

		query = "ask {filter (regex(\"Валовой\", \"валовой\")) }";

		assertFalse(conn.prepareBooleanQuery(query).evaluate(), "case-sensitive match on Cyrillic should fail");
	}

	private void testFilterRegexBoolean(RepositoryConnection conn) throws Exception {

		loadTestData("/testdata-query/dataset-query.trig", conn);

		// test case for issue SES-1050
		String query = getNamespaceDeclarations() +
				" SELECT *" +
				" WHERE { " +
				"       ?x foaf:name ?name ; " +
				"          foaf:mbox ?mbox . " +
				"       FILTER(EXISTS { " +
				"            FILTER(REGEX(?name, \"Bo\") && REGEX(?mbox, \"bob\")) " +
				// query.append(" FILTER(REGEX(?mbox, \"bob\")) ");
				"            } )" +
				" } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (Stream<BindingSet> result = tq.evaluate().stream()) {
			long count = result.count();
			assertEquals(1, count);
		}
	}

	private void testDateCastFunction_date(RepositoryConnection conn) {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (xsd:date(\"2022-09-09\") AS ?date) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("2022-09-09", result.next().getValue("date").stringValue());
			assertFalse(result.hasNext());
		}
	}

	private void testDateCastFunction_date_withTimeZone_utc(RepositoryConnection conn) {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (xsd:date(\"2022-09-09Z\") AS ?date) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("2022-09-09Z", result.next().getValue("date").stringValue());
			assertFalse(result.hasNext());
		}
	}

	private void testDateCastFunction_dateTime_withTimeZone_offset(RepositoryConnection conn) {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (xsd:date(\"2022-09-09T14:45:13+03:00\") AS ?date) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("2022-09-09+03:00", result.next().getValue("date").stringValue());
			assertFalse(result.hasNext());
		}
	}

	private void testDateCastFunction_invalidInput(RepositoryConnection conn) {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (xsd:date(\"2022-09-xx\") AS ?date) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertFalse(result.next().hasBinding("date"),
					"There should be no binding because the cast should have failed.");
			assertFalse(result.hasNext());
		}
	}

	public Stream<DynamicTest> tests() {
		return Stream.of(makeTest("Seconds", this::testSeconds),
				makeTest("SecondsMilliseconds", this::testSecondsMilliseconds),
				makeTest("DateCastFunction_invalidInput", this::testDateCastFunction_invalidInput),
				makeTest("DateCastFunction_dateTime_withTimeZone_offset",
						this::testDateCastFunction_dateTime_withTimeZone_offset),
				makeTest("DateCastFunction_date_withTimeZone_utc", this::testDateCastFunction_date_withTimeZone_utc),
				makeTest("DateCastFunction_date", this::testDateCastFunction_date),
				makeTest("FilterRegexBoolean", this::testFilterRegexBoolean),
				makeTest("RegexCaseNonAscii", this::testRegexCaseNonAscii),
				makeTest("SES2052If2", this::testSES2052If2), makeTest("SES2052If1", this::testSES2052If1),
				makeTest("27NormalizeIRIFunction", this::test27NormalizeIRIFunction),
				makeTest("SES2121URIFunction", this::testSES2121URIFunction),
				makeTest("SES1991RANDEvaluation", this::testSES1991RANDEvaluation),
				makeTest("SES1991STRUUIDEvaluation", this::testSES1991STRUUIDEvaluation),
				makeTest("SES1991UUIDEvaluation", this::testSES1991UUIDEvaluation),
				makeTest("SES869ValueOfNow", this::testSES869ValueOfNow),
				makeTest("SES1991NOWEvaluation", this::testSES1991NOWEvaluation));
	}
}
