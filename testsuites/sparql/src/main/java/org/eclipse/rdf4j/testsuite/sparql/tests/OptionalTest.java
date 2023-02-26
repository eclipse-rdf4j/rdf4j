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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.Test;

/**
 * Tests on OPTIONAL clause behavior.
 *
 * @author Jeen Broekstra
 *
 */
public class OptionalTest extends AbstractComplianceTest {

	@Test
	public void testSES1898LeftJoinSemantics1() throws Exception {
		loadTestData("/testdata-query/dataset-ses1898.trig");
		String query = "  PREFIX : <http://example.org/> " +
				"  SELECT * WHERE { " +
				"    ?s :p1 ?v1 . " +
				"    OPTIONAL {?s :p2 ?v2 } ." +
				"     ?s :p3 ?v2 . " +
				"  } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (Stream<BindingSet> result = tq.evaluate().stream()) {
			long count = result.count();
			assertEquals(0, count);
		}
	}

	@Test
	public void testSES1121VarNamesInOptionals() throws Exception {
		// Verifying that variable names have no influence on order of optionals
		// in query. See SES-1121.

		loadTestData("/testdata-query/dataset-ses1121.trig");

		String query1 = getNamespaceDeclarations() +
				" SELECT DISTINCT *\n" +
				" WHERE { GRAPH ?g { \n" +
				"          OPTIONAL { ?var35 ex:p ?b . } \n " +
				"          OPTIONAL { ?b ex:q ?c . } \n " +
				"       } \n" +
				" } \n";

		String query2 = getNamespaceDeclarations() +
				" SELECT DISTINCT *\n" +
				" WHERE { GRAPH ?g { \n" +
				"          OPTIONAL { ?var35 ex:p ?b . } \n " +
				"          OPTIONAL { ?b ex:q ?var2 . } \n " +
				"       } \n" +
				" } \n";

		TupleQuery tq1 = conn.prepareTupleQuery(QueryLanguage.SPARQL, query1);
		TupleQuery tq2 = conn.prepareTupleQuery(QueryLanguage.SPARQL, query2);

		try (TupleQueryResult result1 = tq1.evaluate(); TupleQueryResult result2 = tq2.evaluate()) {
			assertNotNull(result1);
			assertNotNull(result2);

			List<BindingSet> qr1 = QueryResults.asList(result1);
			List<BindingSet> qr2 = QueryResults.asList(result2);

			// System.out.println(qr1);
			// System.out.println(qr2);

			// if optionals are not kept in same order, query results will be
			// different size.
			assertEquals(qr1.size(), qr2.size());

		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSameTermRepeatInOptional() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = getNamespaceDeclarations() +
				" SELECT ?l ?opt1 ?opt2 " +
				" FROM ex:optional-sameterm-graph " +
				" WHERE { " +
				"          ?s ex:p ex:A ; " +
				"          { " +
				"              { " +
				"                 ?s ?p ?l ." +
				"                 FILTER(?p = rdfs:label) " +
				"              } " +
				"              OPTIONAL { " +
				"                 ?s ?p ?opt1 . " +
				"                 FILTER (?p = ex:prop1) " +
				"              } " +
				"              OPTIONAL { " +
				"                 ?s ?p ?opt2 . " +
				"                 FILTER (?p = ex:prop2) " +
				"              } " +
				"          }" +
				" } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				// System.out.println(bs);

				Value l = bs.getValue("l");
				assertTrue(l instanceof Literal);
				assertEquals("label", ((Literal) l).getLabel());

				Value opt1 = bs.getValue("opt1");
				assertNull(opt1);

				Value opt2 = bs.getValue("opt2");
				assertNull(opt2);
			}
			assertEquals(1, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/3072
	 *
	 */
	@Test
	public void testValuesAfterOptional() throws Exception {
		String data = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . \n"
				+ "@prefix :     <urn:ex:> . \n"
				+ ":r1 a rdfs:Resource . \n"
				+ ":r2 a rdfs:Resource ; rdfs:label \"a label\" . \n";

		String query = ""
				+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
				+ "prefix :     <urn:ex:> \n"
				+ "\n"
				+ "select ?resource ?label where { \n"
				+ "  ?resource a rdfs:Resource . \n"
				+ "  optional { ?resource rdfs:label ?label } \n"
				+ "  values ?label { undef } \n"
				+ "}";

		conn.add(new StringReader(data), RDFFormat.TURTLE);

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());
		assertThat(result).hasSize(2);
	}
}
