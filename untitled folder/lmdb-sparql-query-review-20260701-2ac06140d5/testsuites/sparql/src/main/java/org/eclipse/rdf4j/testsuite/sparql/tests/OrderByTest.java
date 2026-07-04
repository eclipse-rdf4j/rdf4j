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

import java.io.StringReader;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.jupiter.api.DynamicTest;

public class OrderByTest extends AbstractComplianceTest {

	public OrderByTest(Supplier<Repository> repo) {
		super(repo);
	}

	private void testDistinctOptionalOrderBy(RepositoryConnection conn) throws Exception {

		conn.add(new StringReader("[] a <test:Class>.\n" +
				"[] a <test:Class>; <test:nr> 123 ."), "", RDFFormat.TURTLE);

		String query = "select distinct ?o ?nr { ?o a <test:Class> optional { ?o <test:nr> ?nr } } order by ?nr";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (Stream<BindingSet> result = tq.evaluate().stream()) {
			long count = result.count();
			assertEquals(2, count);
		}
	}

	private void testOrderByVariableNotInUse(RepositoryConnection conn) throws Exception {

		conn.add(new StringReader("_:bob1 a <foaf:Person> ; rdfs:label \"Bob1\" .\n" +
				"_:bob2 a <foaf:Person> ; rdfs:label \"Bob2\" ."), "", RDFFormat.TURTLE);

		String query = "SELECT * WHERE { ?person a <foaf:Person> } ORDER BY ?score\n";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (Stream<BindingSet> result = tq.evaluate().stream()) {
			// use distinct because the issue is that the query produces duplicates
			long count = result.distinct().count();
			assertEquals(2, count);
		}
	}

	private void testDistinctOptionalOrderByMath(RepositoryConnection conn) throws Exception {

		conn.add(new StringReader("[] a <test:Class>.\n" +
				"[] a <test:Class>; <test:nr> 123 ."), "", RDFFormat.TURTLE);

		String query = "select distinct ?o ?nr { ?o a <test:Class> optional { ?o <test:nr> ?nr } } order by (?nr + STRLEN(?o))";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (Stream<BindingSet> result = tq.evaluate().stream()) {
			long count = result.count();
			assertEquals(2, count);
		}
	}

	public Stream<DynamicTest> tests() {
		return Stream.of(makeTest("DistinctOptionalOrderBy", this::testDistinctOptionalOrderBy),
				makeTest("OrderByVariableNotInUse", this::testOrderByVariableNotInUse),
				makeTest("DistinctOptionalOrderByMath", this::testDistinctOptionalOrderByMath));
	}

}
