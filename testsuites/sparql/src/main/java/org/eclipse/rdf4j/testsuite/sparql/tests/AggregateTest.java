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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.Test;

/**
 * Tests on SPARQL aggregate function compliance.
 *
 * @author Jeen Broekstra
 *
 */
public class AggregateTest extends AbstractComplianceTest {

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testMaxAggregateWithGroupEmptyResult() {
		String query = "select ?s (max(?o) as ?omax) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n" +
				" group by ?s\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.hasNext()).isFalse();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testMaxAggregateWithoutGroupEmptySolution() {
		String query = "select (max(?o) as ?omax) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.next()).isEmpty();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testMinAggregateWithGroupEmptyResult() {
		String query = "select ?s (min(?o) as ?omin) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n" +
				" group by ?s\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.hasNext()).isFalse();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testMinAggregateWithoutGroupEmptySolution() {
		String query = "select (min(?o) as ?omin) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.next()).isEmpty();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testSampleAggregateWithGroupEmptyResult() {
		String query = "select ?s (sample(?o) as ?osample) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n" +
				" group by ?s\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.hasNext()).isFalse();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testSampleAggregateWithoutGroupEmptySolution() {
		String query = "select (sample(?o) as ?osample) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.next()).isEmpty();
		}
	}

	@Test
	public void testSES2361UndefMin() {
		String query = "SELECT (MIN(?v) as ?min) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("1", result.next().getValue("min").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefMax() {
		String query = "SELECT (MAX(?v) as ?max) WHERE { VALUES ?v { 1 2 7 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("7", result.next().getValue("max").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefCount() {
		String query = "SELECT (COUNT(?v) as ?c) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("4", result.next().getValue("c").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefCountWildcard() {
		String query = "SELECT (COUNT(*) as ?c) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("4", result.next().getValue("c").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefSum() {
		String query = "SELECT (SUM(?v) as ?s) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("10", result.next().getValue("s").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES1979MinMaxInf() throws Exception {
		loadTestData("/testdata-query/dataset-ses1979.trig");
		String query = "prefix : <http://example.org/> select (min(?o) as ?min) (max(?o) as ?max) where { ?s :float ?o }";

		ValueFactory vf = conn.getValueFactory();
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult evaluate = tq.evaluate()) {
			List<BindingSet> result = QueryResults.asList(evaluate);
			assertNotNull(result);
			assertEquals(1, result.size());

			assertEquals(vf.createLiteral(Float.NEGATIVE_INFINITY), result.get(0).getValue("min"));
			assertEquals(vf.createLiteral(Float.POSITIVE_INFINITY), result.get(0).getValue("max"));
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testGroupConcatDistinct() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");

		String query = getNamespaceDeclarations() +
				"SELECT (GROUP_CONCAT(DISTINCT ?l) AS ?concat)" +
				"WHERE { ex:groupconcat-test ?p ?l . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Value concat = bs.getValue("concat");

				assertTrue(concat instanceof Literal);

				String lexValue = ((Literal) concat).getLabel();

				int occ = countCharOccurrences(lexValue, 'a');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'b');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'c');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'd');
				assertEquals(1, occ);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testGroupConcatNonDistinct() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = getNamespaceDeclarations() +
				"SELECT (GROUP_CONCAT(?l) AS ?concat)" +
				"WHERE { ex:groupconcat-test ?p ?l . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Value concat = bs.getValue("concat");

				assertTrue(concat instanceof Literal);

				String lexValue = ((Literal) concat).getLabel();

				int occ = countCharOccurrences(lexValue, 'a');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'b');
				assertEquals(2, occ);
				occ = countCharOccurrences(lexValue, 'c');
				assertEquals(2, occ);
				occ = countCharOccurrences(lexValue, 'd');
				assertEquals(1, occ);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES1970CountDistinctWildcard() throws Exception {
		loadTestData("/testdata-query/dataset-ses1970.trig");

		String query = "SELECT (COUNT(DISTINCT *) AS ?c) {?s ?p ?o }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			assertTrue(result.hasNext());
			BindingSet s = result.next();
			Literal count = (Literal) s.getValue("c");
			assertNotNull(count);

			assertEquals(3, count.intValue());
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private int countCharOccurrences(String string, char ch) {
		int count = 0;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == ch) {
				count++;
			}
		}
		return count;
	}
}
