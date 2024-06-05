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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
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
 * Tests on SPRQL UNION clauses.
 *
 * @author Jeen Broekstra
 *
 */
public class UnionTest extends AbstractComplianceTest {

	public UnionTest(Supplier<Repository> repo) {
		super(repo);
	}

	private void testEmptyUnion(RepositoryConnection conn) {
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

	private void testSameTermRepeatInUnion(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-query.trig", conn);
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

	private void testSameTermRepeatInUnionAndOptional(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-query.trig", conn);

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

	public Stream<DynamicTest> tests() {
		return Stream.of(makeTest("EmptyUnion", this::testEmptyUnion),
				makeTest("SameTermRepeatInUnion", this::testSameTermRepeatInUnion),
				makeTest("SameTermRepeatInUnionAndOptional", this::testSameTermRepeatInUnionAndOptional));
	}

}
