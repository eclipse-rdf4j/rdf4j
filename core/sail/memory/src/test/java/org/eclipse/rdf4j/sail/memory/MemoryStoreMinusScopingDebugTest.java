/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Local duplicates of selected {@link org.eclipse.rdf4j.testsuite.sparql.tests.SparqlMinusScopingTests} cases to ease
 * debugging against a concrete {@link MemoryStore} repository.
 */
public class MemoryStoreMinusScopingDebugTest {

	private static final String PREFIX = "PREFIX : <http://ex/>\n";
	private static final String DATA_BASE_IRI = "http://ex/";

	private SailRepository repository;

	@BeforeEach
	public void setUp() {
		repository = new SailRepository(new MemoryStore());
		repository.init();
	}

	@AfterEach
	public void tearDown() {
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Test
	public void T16_rhs_bind_of_outer_var_produces_unbound_then_overremoves_on_shared_subset() throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 1, 2 .\n" +
				":b :p 3 ; :q 4 .\n" +
				":c :p 7 .";

		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
					"SELECT ?x WHERE {\n" +
							"  ?x :p ?n .\n" +
							"  MINUS { BIND(?n AS ?k) ?x :q ?k }\n" +
							"} ORDER BY ?x");

			assertEquals(setOf("c"), names(rows, "x"),
					"RHS BIND on unbound outer var must not correlate; shared-vars logic should remove :a,:b only.");
		}
	}

	@Test
	public void T21_not_exists_over_optional_is_always_false_here() throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :name \"Alice\" ; :formerName \"Alice\" .\n" +
				":f :name \"Carol\" .";

		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
					"SELECT ?x WHERE {\n" +
							"  ?x :name ?n .\n" +
							"  FILTER NOT EXISTS { OPTIONAL { ?x :formerName ?n } }\n" +
							"}");

			assertEquals(List.of(), rows);
		}
	}

	private List<BindingSet> selectWithData(RepositoryConnection conn, String data, RDFFormat format, String body)
			throws IOException {
		String sparql = PREFIX + body;

		conn.clear();
		conn.add(new StringReader(data), DATA_BASE_IRI, format);

		TupleQuery query = conn.prepareTupleQuery(sparql);
		try (TupleQueryResult result = query.evaluate()) {
			return QueryResults.asList(result);
		}
	}

	private static Set<String> names(List<BindingSet> rows, String var) {
		return rows.stream()
				.map(bs -> bs.getValue(var))
				.filter(Objects::nonNull)
				.map(MemoryStoreMinusScopingDebugTest::name)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static String name(Value value) {
		if (value instanceof IRI) {
			return ((IRI) value).getLocalName();
		}
		return value.stringValue();
	}

	private static Set<String> setOf(String... values) {
		return new LinkedHashSet<>(Arrays.asList(values));
	}
}
