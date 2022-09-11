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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.Test;

/**
 * Tests on SPARQL aggregate function compliance.
 *
 * @author Jeen Broekstra
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

	@Test
	public void testCountHaving() {
		BNode bnode1 = Values.bnode();
		BNode bnode2 = Values.bnode();
		BNode bnode3 = Values.bnode();

		conn.add(bnode3, FOAF.KNOWS, Values.bnode());
		conn.add(bnode1, FOAF.KNOWS, Values.bnode());
		conn.add(bnode1, FOAF.KNOWS, Values.bnode());
		conn.add(bnode2, FOAF.KNOWS, Values.bnode());
		conn.add(bnode3, FOAF.KNOWS, Values.bnode());
		conn.add(bnode3, FOAF.KNOWS, Values.bnode());
		conn.add(bnode1, FOAF.KNOWS, Values.bnode());

		String query = "SELECT ?a WHERE { ?a ?b ?c } GROUP BY ?a HAVING( (COUNT(?c) > 1 ) && ( COUNT(?c)  != 0 ) ) ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			assertEquals(2, collect.size());
		}
	}

	@Test
	public void testSum() {
		mixedDataForNumericAggregates();

		String query = "SELECT ?a (SUM(?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertNull(collect.get(i++).getValue("aggregate"));
			assertNull(collect.get(i++).getValue("aggregate"));
			assertNull(collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(30.11), collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(new BigDecimal("89.4786576482391284723864721567342354783275234")),
					collect.get(i++).getValue("aggregate"));

		}

	}

	@Test
	public void testDistinctSum() {
		mixedDataForNumericAggregates();

		String query = "SELECT ?a (SUM(DISTINCT ?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertNull(collect.get(i++).getValue("aggregate"));
			assertNull(collect.get(i++).getValue("aggregate"));
			assertNull(collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(30.11), collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(new BigDecimal("55.4786576482391284723864721567342354783275234")),
					collect.get(i++).getValue("aggregate"));
		}

	}

	@Test
	public void testAvg() {
		mixedDataForNumericAggregates();

		String query = "SELECT ?a (AVG(?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertNull(collect.get(i++).getValue("aggregate"));
			assertNull(collect.get(i++).getValue("aggregate"));
			assertNull(collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(15.055), collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(new BigDecimal("17.89573152964782569447729443134684709566550468")),
					collect.get(i++).getValue("aggregate"));
		}

	}

	@Test
	public void testDistinctAvg() {
		mixedDataForNumericAggregates();

		String query = "SELECT ?a (AVG(DISTINCT ?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertNull(collect.get(i++).getValue("aggregate"));
			assertNull(collect.get(i++).getValue("aggregate"));
			assertNull(collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(15.055), collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(new BigDecimal("18.492885882746376157462157")),
					collect.get(i++).getValue("aggregate"));
		}

	}

	@Test
	public void testMax() {
		mixedDataForNumericAggregates();

		String query = "SELECT ?a (MAX(?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertEquals(Values.literal(new BigDecimal("19.4786576482391284723864721567342354783275234")),
					collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(23), collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(23), collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal("2022-01-01T01:01:01.000000001Z", CoreDatatype.XSD.DATETIME),
					collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal("3"), collect.get(i++).getValue("aggregate"));
		}

	}

	@Test
	public void testDistinctMax() {
		mixedDataForNumericAggregates();

		String query = "SELECT ?a (MAX(DISTINCT ?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertEquals(Values.literal(new BigDecimal("19.4786576482391284723864721567342354783275234")),
					collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(23), collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal(23), collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal("2022-01-01T01:01:01.000000001Z", CoreDatatype.XSD.DATETIME),
					collect.get(i++).getValue("aggregate"));
			assertEquals(Values.literal("3"), collect.get(i++).getValue("aggregate"));
		}

	}

	private void mixedDataForNumericAggregates() {
		IRI node1 = Values.iri("http://example.com/1");
		IRI node2 = Values.iri("http://example.com/2");
		IRI node3 = Values.iri("http://example.com/3");
		IRI node4 = Values.iri("http://example.com/4");
		IRI node5 = Values.iri("http://example.com/5");

		conn.add(node3, FOAF.AGE, Values.bnode());
		conn.add(node1, FOAF.AGE, Values.literal("3"));
		conn.add(node1, FOAF.AGE, Values.literal(5));
		conn.add(node2, FOAF.AGE, Values.literal(7.11));
		conn.add(node3, FOAF.AGE, Values.literal(13));
		conn.add(node3, FOAF.AGE, Values.literal(17));
		conn.add(node1, FOAF.AGE, Values.literal(19));
		conn.add(node2, FOAF.AGE, Values.literal(23));

		conn.add(node4, FOAF.AGE, Values.literal(19));
		conn.add(node4, FOAF.AGE, Values.literal(ZonedDateTime.of(2022, 01, 01, 01, 01, 01, 01, ZoneId.of("UTC"))));

		conn.add(node1, FOAF.AGE, Values.literal(23));
		conn.add(node2, FOAF.AGE, Values.literal(23));
		conn.add(node3, FOAF.AGE, Values.literal(23));
		conn.add(node4, FOAF.AGE, Values.literal(23));

		conn.add(node3, FOAF.KNOWS, node1);

		conn.add(node5, FOAF.AGE, Values.literal(17));
		conn.add(node5, FOAF.PHONE, Values.literal(17));
		conn.add(node5, FOAF.DNA_CHECKSUM, Values.literal(17));
		conn.add(node5, FOAF.DNA_CHECKSUM, Values.literal(19));
		conn.add(node5, FOAF.PHONE, Values.literal(new BigDecimal("19.4786576482391284723864721567342354783275234")));
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
