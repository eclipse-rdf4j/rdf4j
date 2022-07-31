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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.Test;

/**
 * Tests on SPRQL UNION clauses.
 *
 * @author Jeen Broekstra
 *
 */
public class UnionTest extends AbstractComplianceTest {

	@Test
	public void testEmptyUnion() {
		String query = "PREFIX : <http://example.org/> "
				+ "SELECT ?visibility WHERE {"
				+ "OPTIONAL { SELECT ?var WHERE { :s a :MyType . BIND (:s as ?var ) .} } ."
				+ "BIND (IF(BOUND(?var), 'VISIBLE', 'HIDDEN') as ?visibility)"
				+ "}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSameTermRepeatInUnion() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" +
				"SELECT * {\n" +
				"    {\n" +
				"        ?sameTerm foaf:mbox ?mbox\n" +
				"        FILTER sameTerm(?sameTerm,$william)\n" +
				"    } UNION {\n" +
				"        ?x foaf:knows ?sameTerm\n" +
				"        FILTER sameTerm(?sameTerm,$william)\n" +
				"    }\n" +
				"}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		tq.setBinding("william", conn.getValueFactory().createIRI("http://example.org/william"));

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				// System.out.println(bs);

				Value mbox = bs.getValue("mbox");
				Value x = bs.getValue("x");

				assertTrue(mbox instanceof Literal || x instanceof IRI);
			}
			assertEquals(3, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSameTermRepeatInUnionAndOptional() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");

		String query = getNamespaceDeclarations() +
				"SELECT * {\n" +
				"    {\n" +
				"        ex:a ?p ?prop1\n" +
				"        FILTER (?p = ex:prop1)\n" +
				"    } UNION {\n" +
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
				"    }\n" +
				"}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				// System.out.println(bs);

				Value prop1 = bs.getValue("prop1");
				Value l = bs.getValue("l");

				assertTrue(prop1 instanceof Literal || l instanceof Literal);
				if (l instanceof Literal) {
					Value opt1 = bs.getValue("opt1");
					assertNull(opt1);

					Value opt2 = bs.getValue("opt2");
					assertNull(opt2);
				}
			}
			assertEquals(2, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

}
