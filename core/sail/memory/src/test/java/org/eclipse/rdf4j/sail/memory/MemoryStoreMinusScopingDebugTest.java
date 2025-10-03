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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.eclipse.rdf4j.query.QueryLanguage;
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

	@Test
	public void testSES2250BindErrors() {

		try (SailRepositoryConnection conn = repository.getConnection()) {

			conn.prepareUpdate(QueryLanguage.SPARQL, "insert data { <urn:test:subj> <urn:test:pred> _:blank }")
					.execute();

			String qb = "SELECT * {\n" +
					"    ?s1 ?p1 ?blank . " +
					"    FILTER(isBlank(?blank))" +
					"    BIND (iri(?blank) as ?biri)" +
					"    ?biri ?p2 ?o2 ." +
					"}";

			TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb);
			try (TupleQueryResult evaluate = tq.evaluate()) {
				assertFalse(evaluate.hasNext(),
						"The query should not return a result: " + Arrays.toString(evaluate.stream().toArray()));
			}
		}
	}

	@Test
	public void testEmptyUnion() {
		try (SailRepositoryConnection conn = repository.getConnection()) {

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
	}

	@Test
	public void complexMinus_rhsOptionalBindOfOuterVar_unsharedBindIgnored() throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 1, 2 .\n" +
				":b :p 3 ; :q 4 .\n" +
				":c :p 7 .";

		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
					"SELECT ?x WHERE {\n" +
							"  ?x :p ?n .\n" +
							"  MINUS { OPTIONAL { BIND(?n AS ?k) } ?x :q ?k }\n" +
							"} ORDER BY ?x");

			// Only :c lacks :q; binding of out-of-scope ?n in the RHS is ignored for scoping,
			// and ?k is bound by ?x :q ?k when available. Thus :a and :b are removed.
			assertEquals(setOf("c"), names(rows, "x"));
		}
	}

	@Test
	public void complexMinus_rhsUnionSharedAndUnsharedBranches_onlySharedAffects() throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 10 ; :q 20 .\n" +
				":b :p 20 ; :q 30 .\n" +
				":c :p 30 .\n" +
				":z :q 999 .";

		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
					"SELECT ?x WHERE {\n" +
							"  ?x :p ?v .\n" +
							"  MINUS { { ?x :q ?v } UNION { ?y :q ?w } }\n" +
							"} ORDER BY ?x");

			// Unshared UNION branch must not affect MINUS; only { ?x :q ?v } would remove rows
			// where q==p, which does not occur in the dataset. All subjects with :p remain.
			assertEquals(setOf("a", "b", "c"), names(rows, "x"));
		}
	}

	@Test
	public void complexNotExists_overBareOptional_alwaysFalse() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			String query = "SELECT * WHERE { BIND(1 AS ?d) FILTER NOT EXISTS { OPTIONAL { BIND(1 AS ?z) } } }";
			TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
			try (TupleQueryResult r = tq.evaluate()) {
				assertNotNull(r);
				assertFalse(r.hasNext());
			}
		}
	}

	@Test
	public void complexOptionalSubselect_noLeftBindings_emptyOptionalYieldsNoRow() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			String query = "SELECT ?flag WHERE { " +
					"OPTIONAL { SELECT ?v WHERE { VALUES ?u { } } } " +
					"BIND(IF(BOUND(?v), 'Y','N') AS ?flag) }";
			TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
			try (TupleQueryResult r = tq.evaluate()) {
				assertNotNull(r);
				assertFalse(r.hasNext());
			}
		}
	}

	@Test
	public void complexMinus_rhsSubselectOrderLimitBindEquality_removesLimitedMatches() throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :p 10 ; :q 10 .\n" +
				":f :p 20 ; :q 20 .\n" +
				":g :p 30 ; :q 99 .\n" +
				":h :p 40 ; :q 40 .";

		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
					"SELECT ?x WHERE {\n" +
							"  ?x :p ?v .\n" +
							"  MINUS { { SELECT ?x ?rv WHERE { ?x :q ?rv } ORDER BY ?rv LIMIT 2 } BIND(?rv AS ?v) }\n" +
							"} ORDER BY ?x");

			// The two smallest q-values are 10 and 20; only e and f match q==p among those,
			// so they are removed. g and h remain.
			assertEquals(setOf("g", "h"), names(rows, "x"));
		}
	}

	@Test
	public void graphIsolation_sameGraphNoRemovalWhenValuesDiffer() throws IOException {
		String trig = "@prefix : <http://ex/> .\n" +
				"GRAPH :g1 { :a :p 1 . }\n" +
				"GRAPH :g2 { :a :q 1 . :a :p 2 . }";

		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, trig, RDFFormat.TRIG,
					"SELECT ?g ?x ?n WHERE {\n" +
							"  GRAPH ?g { ?x :p ?n }\n" +
							"  MINUS { GRAPH ?g { ?x :q ?n } }\n" +
							"} ORDER BY ?g ?x ?n");
			assertEquals(setOf("g1|a|1", "g2|a|2"),
					rows.stream()
							.map(bs -> name(bs.getValue("g")) + "|" + name(bs.getValue("x")) + "|"
									+ name(bs.getValue("n")))
							.collect(Collectors.toCollection(LinkedHashSet::new)));
		}
	}

	@Test
	public void graphIsolation_removesOnlyInGraphWithMatch() throws IOException {
		String trig = "@prefix : <http://ex/> .\n" +
				"GRAPH :g1 { :a :p 1 . }\n" +
				"GRAPH :g2 { :a :q 1 . :a :p 2 . :a :q 2 . }";

		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, trig, RDFFormat.TRIG,
					"SELECT ?g ?x ?n WHERE {\n" +
							"  GRAPH ?g { ?x :p ?n }\n" +
							"  MINUS { GRAPH ?g { ?x :q ?n } }\n" +
							"} ORDER BY ?g");
			assertEquals(setOf("g1|a|1"),
					rows.stream()
							.map(bs -> name(bs.getValue("g")) + "|" + name(bs.getValue("x")) + "|"
									+ name(bs.getValue("n")))
							.collect(Collectors.toCollection(LinkedHashSet::new)));
		}
	}

	@Test
	public void valuesOnRight_sharedVarRemovesOnlyListedSubjects() throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 . :b :p 2 . :c :p 3 .";
		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
					"SELECT ?x WHERE { ?x :p ?v MINUS { VALUES ?x { :a :c } } } ORDER BY ?x");
			assertEquals(setOf("b"), names(rows, "x"));
		}
	}

	@Test
	public void nestedNotExistsOverOptionalWithUnion_isAlwaysFalse() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			String q = "SELECT * WHERE { BIND(1 AS ?d) FILTER NOT EXISTS { OPTIONAL { { BIND(1 AS ?z) } UNION { BIND(2 AS ?z) FILTER(?z>1) } } } }";
			TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
			try (TupleQueryResult r = tq.evaluate()) {
				assertNotNull(r);
				assertFalse(r.hasNext());
			}
		}
	}

	@Test
	public void optionalSubselectWithLeftBindings_keepsLeftRows() throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 .";
		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
					"SELECT ?x WHERE { ?x :p ?v OPTIONAL { SELECT ?z WHERE { FILTER(false) } } } ORDER BY ?x");
			assertEquals(setOf("a"), names(rows, "x"));
		}
	}

	@Test
	public void minusRhsUnionSubselects_withGraphs_onlySameGraphBranchRemoves() throws IOException {
		String trig = "@prefix : <http://ex/> .\n" +
				"GRAPH :g1 { :a :p 1 . :a :q 1 . }\n" +
				"GRAPH :g2 { :b :p 2 . :c :q 2 . }";
		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, trig, RDFFormat.TRIG,
					"SELECT ?g ?x ?n WHERE {\n" +
							"  GRAPH ?g { ?x :p ?n }\n" +
							"  MINUS { { GRAPH ?g { ?x :q ?n } } UNION { GRAPH :g2 { ?x :q ?n } } }\n" +
							"} ORDER BY ?g ?x ?n");
			assertEquals(setOf("g2|b|2"),
					rows.stream()
							.map(bs -> name(bs.getValue("g")) + "|" + name(bs.getValue("x")) + "|"
									+ name(bs.getValue("n")))
							.collect(Collectors.toCollection(LinkedHashSet::new)));
		}
	}

	@Test
	public void bnodeOnRhsViaUnionAndSubselect_cannotMatchDataIds() throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				"_:b1 a [] . _:b2 a [] .\n" +
				":k :id _:b1 . :l :id _:b2 .";
		try (SailRepositoryConnection conn = repository.getConnection()) {
			List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
					"SELECT ?s WHERE { ?s :id ?id MINUS { { BIND(BNODE() AS ?id) } UNION { SELECT ?id WHERE { BIND(BNODE() AS ?id) } } } } ORDER BY ?s");
			assertEquals(setOf("k", "l"), names(rows, "s"));
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
