/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.jupiter.api.DynamicTest;

/**
 * Tests on LATERAL join clause behavior.
 *
 */
public class LateralTest extends AbstractComplianceTest {

	public LateralTest(Supplier<Repository> repo) {
		super(repo);
	}

	private void testLateralBasic(RepositoryConnection conn) throws Exception {
		String data = "@prefix ex: <http://example.org/> .\n"
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
				+ "\n"
				+ "ex:subject1 ex:predicate ex:object1 .\n"
				+ "ex:subject1 rdfs:label \"Label 1\" .\n"
				+ "ex:subject2 ex:predicate ex:object2 .\n"
				+ "ex:subject2 rdfs:label \"Label 2\" .\n"
				+ "ex:subject3 ex:predicate ex:object3 .\n"
				+ "ex:subject3 rdfs:label \"Label 3\" .\n";

		String query = "PREFIX ex: <http://example.org/>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "\n"
				+ "SELECT * {\n"
				+ "   ?s ex:predicate ?o\n"
				+ "   LATERAL {\n"
				+ "      SELECT * { ?s rdfs:label ?label } LIMIT 1\n"
				+ "   }\n"
				+ "}\n";

		conn.add(new StringReader(data), "", RDFFormat.TURTLE);

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			List<BindingSet> results = QueryResults.asList(result);
			assertEquals(3, results.size());

			// Verify first result
			BindingSet bs1 = results.get(0);
			assertThat(bs1.getValue("s").stringValue()).contains("subject1");
			assertThat(bs1.getValue("label").stringValue()).isEqualTo("Label 1");

		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void testLateralWithOptional(RepositoryConnection conn) throws Exception {
		String data = "@prefix ex: <http://example.org/> .\n"
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
				+ "\n"
				+ "ex:subject1 ex:predicate ex:object1 .\n"
				+ "ex:subject1 rdfs:label \"Label 1\" .\n"
				+ "ex:subject2 ex:predicate ex:object2 .\n"
				+ "ex:subject2 rdfs:label \"Label 2\" .\n"
				+ "ex:subject3 ex:predicate ex:object3 .\n";

		String query = "PREFIX ex: <http://example.org/>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "\n"
				+ "SELECT * {\n"
				+ "   ?s ex:predicate ?o\n"
				+ "   LATERAL { OPTIONAL { SELECT * { ?s rdfs:label ?label } LIMIT 1} }\n"
				+ "}\n";

		conn.add(new StringReader(data), "", RDFFormat.TURTLE);

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			List<BindingSet> results = QueryResults.asList(result);
			// Should have 3 results: 2 with labels, 1 without
			assertEquals(3, results.size());

		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void testLateralMultipleResults(RepositoryConnection conn) throws Exception {
		String data = "@prefix ex: <http://example.org/> .\n"
				+ "\n"
				+ "ex:subject1 ex:predicate ex:object1 .\n"
				+ "ex:subject1 ex:alternative \"Alt 1A\" .\n"
				+ "ex:subject1 ex:alternative \"Alt 1B\" .\n"
				+ "ex:subject2 ex:predicate ex:object2 .\n"
				+ "ex:subject2 ex:alternative \"Alt 2\" .\n"
				+ "ex:subject3 ex:predicate ex:object3 .\n";

		String query = "PREFIX ex: <http://example.org/>\n"
				+ "\n"
				+ "SELECT * {\n"
				+ "   ?s ex:predicate ?o\n"
				+ "   LATERAL {\n"
				+ "      SELECT * { ?s ex:alternative ?alt }\n"
				+ "   }\n"
				+ "}\n";

		conn.add(new StringReader(data), "", RDFFormat.TURTLE);

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			List<BindingSet> results = QueryResults.asList(result);
			// Should have 4 results: subject1 with 2 alternatives, subject2 with 1,
			// subject3 with none
			assertEquals(3, results.size());

		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void testLateralWithFilter(RepositoryConnection conn) throws Exception {
		String data = "@prefix ex: <http://example.org/> .\n"
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
				+ "\n"
				+ "ex:subject1 ex:predicate ex:object1 .\n"
				+ "ex:subject1 rdfs:label \"Label 1\" .\n"
				+ "ex:subject2 ex:predicate ex:object2 .\n"
				+ "ex:subject2 rdfs:label \"Label 2\" .\n"
				+ "ex:subject3 ex:predicate ex:object3 .\n"
				+ "ex:subject3 rdfs:label \"Label 3\" .\n";

		String query = "PREFIX ex: <http://example.org/>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "\n"
				+ "SELECT * {\n"
				+ "   ?s ex:predicate ?o\n"
				+ "   LATERAL {\n"
				+ "      SELECT * { ?s rdfs:label ?label FILTER(CONTAINS(STR(?label), \"1\")) } LIMIT 1\n"
				+ "   }\n"
				+ "}\n";

		conn.add(new StringReader(data), "", RDFFormat.TURTLE);

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			List<BindingSet> results = QueryResults.asList(result);
			// Should only return subject1 since filter matches "1"
			assertEquals(1, results.size());
			assertThat(results.get(0).getValue("label").stringValue()).isEqualTo("Label 1");

		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void testLateralVariableScoping(RepositoryConnection conn) throws Exception {
		String data = "@prefix ex: <http://example.org/> .\n"
				+ "\n"
				+ "ex:s1 ex:p ex:o1 .\n"
				+ "ex:s1 ex:related ex:r1 .\n"
				+ "ex:r1 ex:data \"data1\" .\n"
				+ "\n"
				+ "ex:s2 ex:p ex:o2 .\n"
				+ "ex:s2 ex:related ex:r2 .\n"
				+ "ex:r2 ex:data \"data2\" .\n";

		String query = "PREFIX ex: <http://example.org/>\n"
				+ "\n"
				+ "SELECT * {\n"
				+ "   ?s ex:p ?o\n"
				+ "   LATERAL {\n"
				+ "      SELECT ?related ?data { ?s ex:related ?related . ?related ex:data ?data }\n"
				+ "   }\n"
				+ "}\n";

		conn.add(new StringReader(data), "", RDFFormat.TURTLE);

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			List<BindingSet> results = QueryResults.asList(result);
			assertEquals(2, results.size());

			// Verify first result has correct values
			BindingSet bs1 = results.get(0);
			assertThat(bs1.getValue("s").stringValue()).contains("s1");
			assertThat(bs1.getValue("data").stringValue()).isEqualTo("data1");

		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public Stream<DynamicTest> tests() {
		return Stream.of(
				makeTest("LateralBasic", this::testLateralBasic),
				makeTest("LateralWithOptional", this::testLateralWithOptional),
				makeTest("LateralMultipleResults", this::testLateralMultipleResults),
				makeTest("LateralWithFilter", this::testLateralWithFilter),
				makeTest("LateralVariableScoping", this::testLateralVariableScoping));
	}
}
