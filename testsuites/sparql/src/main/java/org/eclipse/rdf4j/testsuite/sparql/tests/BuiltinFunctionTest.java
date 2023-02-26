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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.Test;

/**
 * Tests on various SPARQL built-in functions.
 *
 * @author Jeen Broekstra
 *
 */
public class BuiltinFunctionTest extends AbstractComplianceTest {

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1267
	 */
	@Test
	public void testSeconds() {
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
	@Test
	public void testSecondsMilliseconds() {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (SECONDS(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) AS ?sec) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("13.815", result.next().getValue("sec").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES1991NOWEvaluation() throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl");
		String query = "SELECT ?d WHERE {?s ?p ?o . BIND(NOW() as ?d) } LIMIT 2";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			Literal d1 = (Literal) result.next().getValue("d");
			Literal d2 = (Literal) result.next().getValue("d");

			assertNotNull(d1);
			assertEquals(d1, d2);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES869ValueOfNow() {
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
		}
	}

	@Test
	public void testSES1991UUIDEvaluation() throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl");
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

	@Test
	public void testSES1991STRUUIDEvaluation() throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl");
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

	@Test
	public void testSES1991RANDEvaluation() throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl");
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

	@Test
	public void testSES2121URIFunction() {
		String query = "SELECT (URI(\"foo bar\") as ?uri) WHERE {}";
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI uri = (IRI) bs.getValue("uri");
			assertNull("uri result for invalid URI should be unbound", uri);
		}

		query = "BASE <http://example.org/> SELECT (URI(\"foo bar\") as ?uri) WHERE {}";
		tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI uri = (IRI) bs.getValue("uri");
			assertNotNull("uri result for valid URI reference should be bound", uri);
		}
	}

	@Test
	public void test27NormalizeIRIFunction() {
		String query = "SELECT (IRI(\"../bar\") as ?Iri) WHERE {}";
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query, "http://example.com/foo/");
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI actual = (IRI) bs.getValue("Iri");
			IRI expected = iri("http://example.com/bar");
			assertEquals("IRI result for relative IRI should be normalized", expected, actual);
		}
	}

	@Test
	public void testSES2052If1() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
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
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES2052If2() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
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
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testRegexCaseNonAscii() {
		String query = "ask {filter (regex(\"Валовой\", \"валовой\", \"i\")) }";

		assertTrue("case-insensitive match on Cyrillic should succeed", conn.prepareBooleanQuery(query).evaluate());

		query = "ask {filter (regex(\"Валовой\", \"валовой\")) }";

		assertFalse("case-sensitive match on Cyrillic should fail", conn.prepareBooleanQuery(query).evaluate());

	}

	@Test
	public void testFilterRegexBoolean() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");

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
}
