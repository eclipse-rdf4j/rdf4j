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

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.jupiter.api.DynamicTest;

/**
 * Tests on the IN operator.
 *
 * @author Jeen Broekstra
 *
 */
public class InTest extends AbstractComplianceTest {

	public InTest(Supplier<Repository> repo) {
		super(repo);
	}

	private void testInComparison1(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-ses1913.trig", conn);
		String query = " PREFIX : <http://example.org/>\n" +
				" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1/0 , 1)) } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			BindingSet bs = result.next();
			Value y = bs.getValue("y");
			assertNotNull(y);
			assertTrue(y instanceof Literal);
			assertEquals(literal("1", CoreDatatype.XSD.INTEGER), y);
		}
	}

	private void testInComparison2(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-ses1913.trig", conn);
		String query = " PREFIX : <http://example.org/>\n" +
				" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1/0)) } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}

	private void testInComparison3(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-ses1913.trig", conn);
		String query = " PREFIX : <http://example.org/>\n" +
				" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1, 1/0)) } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			BindingSet bs = result.next();
			Value y = bs.getValue("y");
			assertNotNull(y);
			assertTrue(y instanceof Literal);
			assertEquals(literal("1", XSD.INTEGER), y);
		}
	}

	public Stream<DynamicTest> tests() {
		return Stream.of(makeTest("InComparison1", this::testInComparison1),
				makeTest("InComparison2", this::testInComparison2), makeTest("InComparison3", this::testInComparison3));
	}

}
