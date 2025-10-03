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

package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.jupiter.api.DynamicTest;

public class SparqlMinusScopingTests extends AbstractComplianceTest {

	public SparqlMinusScopingTests(Supplier<Repository> repo) {
		super(repo);
	}

	private static final String NS = "http://ex/";
	private static final String PREFIX = "PREFIX : <http://ex/>\n";

	private static final String TTL = String.join("\n",
			"@prefix : <http://ex/> .",
			":a :p 1 .   :a :q 10 .  :a :r 100 .",
			":b :p 2 .   :b :q 20 .  :b :r 200 .",
			":c :p 3 .                 :c :r 300 .",
			":d :q 40 .  :d :r 400 .",
			":e :p 5 .   :e :q 50 ."
	);

	// ---------- Helpers

	private static List<BindingSet> select(RepositoryConnection conn, String body) throws IOException {
		String sparql = PREFIX + body;

		conn.add(new StringReader(TTL), "", RDFFormat.TURTLE);

		TupleQuery q = conn.prepareTupleQuery(sparql);
		try (TupleQueryResult r = q.evaluate()) {
			return QueryResults.asList(r);
		}

	}

	/**
	 * Run a SELECT with a custom dataset (clears the connection first).
	 */
	private static List<BindingSet> selectWithData(RepositoryConnection conn, String data, RDFFormat fmt, String body)
			throws IOException {
		String sparql = PREFIX + body;

		conn.clear();
		conn.add(new StringReader(data), NS, fmt);

		TupleQuery q = conn.prepareTupleQuery(sparql);
		try (TupleQueryResult r = q.evaluate()) {
			return QueryResults.asList(r);
		}
	}

	private static Set<String> names(List<BindingSet> rows, String var) {
		return rows.stream()
				.map(bs -> bs.getValue(var))
				.filter(Objects::nonNull)
				.map(SparqlMinusScopingTests::name)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<String> pairs(List<BindingSet> rows, String var1, String var2) {
		return rows.stream()
				.map(bs -> {
					Value v1 = bs.getValue(var1);
					Value v2 = bs.getValue(var2);
					return (v1 != null && v2 != null) ? name(v1) + "|" + name(v2) : null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<String> triples(List<BindingSet> rows, String v1, String v2, String v3) {
		return rows.stream()
				.map(bs -> {
					Value a = bs.getValue(v1), b = bs.getValue(v2), c = bs.getValue(v3);
					return (a != null && b != null && c != null) ? name(a) + "|" + name(b) + "|" + name(c) : null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static String name(Value v) {
		if (v instanceof IRI) {
			IRI iri = (IRI) v;
			return iri.getLocalName(); // ex:a -> "a"
		}
		return v.stringValue();
	}

	private static Set<String> setOf(String... items) {
		return new LinkedHashSet<>(Arrays.asList(items));
	}

	void T1_bindCreatesFreshVarInRight_NoOverlap_NoEffect(RepositoryConnection conn) throws IOException {
		List<BindingSet> rows = select(conn,
				"SELECT ?s ?pVal WHERE {\n" +
						"  ?s :p ?pVal .\n"
						+ "  MINUS { ?x :q ?qVal . BIND(?qVal*2 AS ?fresh) }\n" +
						"}"
		);
		assertEquals(4, rows.size());
		assertEquals(setOf("a", "b", "c", "e"), names(rows, "s"));
	}

	void T3_bindBeforeUseIntroducesOverlap_EverythingRemoved(RepositoryConnection conn) throws IOException {
		List<BindingSet> rows = select(conn,
				"SELECT ?s ?qVal WHERE {\n" +
						"  ?s :q ?qVal .\n" +
						"  MINUS { ?t :q ?x . BIND(?x AS ?qVal) }\n" +
						"}"
		);
		assertTrue(rows.isEmpty(), "All ?qVal values appear on the right after BIND, so MINUS removes all left rows");
	}

	void T4_renamedVarsInsideRight_NoTrueOverlap_NoEffect(RepositoryConnection conn) throws IOException {
		List<BindingSet> rows = select(conn,
				"SELECT ?s ?pVal WHERE {\n" +
						"  ?s :p ?pVal .\n"
						+ "  MINUS { ?s2 :p ?pVal2 . BIND(?pVal2 AS ?pVal_tmp) }\n" +
						"}"
		);
		assertEquals(4, rows.size());
		assertEquals(setOf("a", "b", "c", "e"), names(rows, "s"));
	}

	void T5_randInsideDisjointRight_MinusHasNoEffect(RepositoryConnection conn) throws IOException {
		List<BindingSet> rows = select(conn,
				"SELECT ?s WHERE {\n" +
						"  ?s :p ?v .\n" +
						"  MINUS { ?x :q ?w . FILTER(RAND() < 2) }\n" + // disjoint vars -> MINUS no effect by spec
						"}"
		);
		assertEquals(4, rows.size(), "Disjoint-variable MINUS must not remove any rows, regardless of RAND()");
		assertEquals(setOf("a", "b", "c", "e"), names(rows, "s"));
	}

	void T8_projectionExprOnLeftDoesNotAffectMinusOverlap_NoEffect(RepositoryConnection conn) throws IOException {
		List<BindingSet> rows = select(conn,
				"SELECT ?s ?pVal (STR(?s) AS ?z) WHERE {\n" +
						"  ?s :p ?pVal .\n"
						+ "  MINUS { ?x :q ?q . BIND(STR(?x) AS ?z) }\n" + // ?z only exists on the right (left ?z is
																			// projection-time)
						"}"
		);
		assertEquals(4, rows.size());
		assertEquals(setOf("a", "b", "c", "e"), names(rows, "s"));
	}

	void T9_projectionBeforeMinus_NoSharedVarsAfterSubselect_NoEffect(RepositoryConnection conn) throws IOException {
		List<BindingSet> rows = select(conn,
				"SELECT ?s WHERE {\n" +
						"  { SELECT ?s WHERE { ?s :p ?v } }\n" +
						"  MINUS { ?x :p ?v }\n" + // ?v not projected to the outer level; disjoint wrt left (?s)
						"}"
		);
		assertEquals(4, rows.size());
		assertEquals(setOf("a", "b", "c", "e"), names(rows, "s"));
	}

	void T10_minusVsNotExists_WithThisDataTheyCoincide(RepositoryConnection conn) throws IOException {
		List<BindingSet> minusRows = select(conn,
				"SELECT ?s WHERE {\n" +
						"  ?s :p ?v .\n" +
						"  MINUS { ?s :q ?w }\n" +
						"}"
		);
		assertEquals(setOf("c"), names(minusRows, "s"));

		List<BindingSet> notExistsRows = select(conn,
				"SELECT ?s WHERE {\n" +
						"  ?s :p ?v .\n" +
						"  FILTER NOT EXISTS { ?s :q ?w }\n" +
						"}"
		);
		assertEquals(setOf("c"), names(notExistsRows, "s"));
	}

	void T11_multipleMinus_sharedThenIndependent_onlyFirstMatters(RepositoryConnection conn) throws IOException {
		List<BindingSet> rows = select(conn,
				"SELECT ?s WHERE {\n" +
						"  ?s :p ?v .\n" +
						"  MINUS { ?s :q ?w }   # removes a, b, e\n"
						+ "  MINUS { ?x :r ?r }   # no shared vars -> no further effect\n" +
						"}"
		);
		assertEquals(setOf("c"), names(rows, "s"));
	}

	void T12_minusInsideOptional_affectsOnlyOptionalGroup(RepositoryConnection conn) throws IOException {
		List<BindingSet> rows = select(conn,
				"SELECT ?s ?maybe WHERE {\n" +
						"  ?s :p ?v .\n" +
						"  OPTIONAL {\n"
						+ "    BIND(1 AS ?maybe)\n" +
						"    MINUS { ?s :q ?w }\n" +
						"  }\n" +
						"}"
		);

		// Build subject -> hasMaybe mapping
		Map<String, Boolean> hasMaybe = new LinkedHashMap<>();
		for (BindingSet bs : rows) {
			String s = name(bs.getValue("s"));
			boolean bound = bs.hasBinding("maybe");
			hasMaybe.put(s, bound);
		}

		// With the dataset, only :c lacks :q, so OPTIONAL survives only for c.
		assertEquals(4, rows.size());
		assertEquals(Boolean.FALSE, hasMaybe.get("a"));
		assertEquals(Boolean.FALSE, hasMaybe.get("b"));
		assertEquals(Boolean.TRUE, hasMaybe.get("c"));
		assertEquals(Boolean.FALSE, hasMaybe.get("e"));
	}

	void T13_minus_no_shared_vars_is_noop_select(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 . :a :q 1 . :b :p 1 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?s WHERE { ?s :p 1 MINUS { ?x :q 1 } }");
		assertEquals(setOf("a", "b"), names(rows, "s"),
				"MINUS with disjoint var-sets must keep the LHS intact (§8.3).");
	}

	void T14_not_exists_contrast_to_minus_no_shared_vars(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 . :a :q 1 . :b :p 1 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?s WHERE { ?s :p 1 FILTER NOT EXISTS { ?x :q 1 } }");
		assertTrue(rows.isEmpty(), "NOT EXISTS is correlated and removes all rows when { ?x :q 1 } exists (§8.3).");
	}

	void T15_rhs_filter_referencing_outer_var_is_unbound_and_ignored(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 1, 2 .\n" +
				":b :p 3 ; :q 4 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?x ?n WHERE {\n" +
						"  ?x :p ?n .\n" +
						"  MINUS { ?x :q ?m . FILTER(?m = ?n) }  # ?n unbound on RHS → filter errors → RHS empty\n" +
						"} ORDER BY ?x");
		assertEquals(setOf("a|1", "b|3"), pairs(rows, "x", "n"),
				"RHS filter sees no outer vars under MINUS; subtract nothing (§8.3).");
	}

	void T16_rhs_bind_of_outer_var_produces_unbound_then_overremoves_on_shared_subset(RepositoryConnection conn)
			throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 1, 2 .\n" +
				":b :p 3 ; :q 4 .\n" +
				":c :p 7 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?x WHERE {\n" +
						"  ?x :p ?n .\n" +
						"  MINUS { BIND(?n AS ?k) ?x :q ?k }\n" +
						"} ORDER BY ?x");
		assertEquals(setOf("c"), names(rows, "x"),
				"RHS BIND on unbound outer var must not correlate; shared-vars logic should remove :a,:b only.");
	}

	void T17_rhs_bind_creates_intentional_shared_var_equalities(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :p 10 ; :q 42 .\n" +
				":f :p 20 ; :q 20 .\n" +
				":g :p 30 ; :q 99 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?x WHERE {\n" +
						"  ?x :p ?v .\n" +
						"  MINUS { ?x :q ?m . BIND(?m AS ?v) }  # removes only when q==p\n" +
						"} ORDER BY ?x");
		assertEquals(setOf("e", "g"), names(rows, "x"),
				"Only :f should be removed (q==p). Early projection must NOT change shared vars.");
	}

	void T18_early_projection_should_not_change_minus_semantics(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :p 10 ; :q 42 .\n" +
				":f :p 20 ; :q 20 .\n" +
				":g :p 30 ; :q 99 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?x WHERE { ?x :p ?v MINUS { ?x :q ?v } } ORDER BY ?x");
		assertEquals(setOf("e", "g"), names(rows, "x"),
				"Pushing projection before MINUS would wrongly remove :e and :g; don't do that.");
	}

	void T19_subquery_pins_shared_var_against_optimizer(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :p 10 ; :q 42 .\n" +
				":f :p 20 ; :q 20 .\n" +
				":g :p 30 ; :q 99 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?x WHERE {\n" +
						"  { SELECT ?x ?v WHERE { ?x :p ?v } }   # box the LHS\n" +
						"  MINUS { ?x :q ?v }\n" +
						"} ORDER BY ?x");
		assertEquals(setOf("e", "g"), names(rows, "x"), "Subquery must preserve shared vars until MINUS.");
	}

	void T20_optional_inside_minus_only_removes_when_optional_matches(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :name \"Alice\" ; :formerName \"Alice\" .\n" +
				":f :name \"Carol\" .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?x WHERE {\n" +
						"  ?x :name ?n .\n" +
						"  MINUS { OPTIONAL { ?x :formerName ?n } }\n" +
						"} ORDER BY ?x");
		assertEquals(setOf("f"), names(rows, "x"),
				"OPTIONAL inside MINUS: only rows for which the OPTIONAL binds compatibly are removed.");
	}

	void T21_not_exists_over_optional_is_always_false_here(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :name \"Alice\" ; :formerName \"Alice\" .\n" +
				":f :name \"Carol\" .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?x WHERE {\n" +
						"  ?x :name ?n .\n" +
						"  FILTER NOT EXISTS { OPTIONAL { ?x :formerName ?n } }\n" +
						"}");
		assertEquals(List.of(), rows,
				"Rewriting MINUS{OPTIONAL{…}} to NOT EXISTS { OPTIONAL{…} } is wrong: the inner group always yields at least the empty mapping.");
	}

	void T22_graph_isolation_same_g_on_both_sides_no_removal_when_values_differ(RepositoryConnection conn)
			throws IOException {
		String trig = "@prefix : <http://ex/> .\n" +
				"GRAPH :g1 { :a :p 1 . }\n" +
				"GRAPH :g2 { :a :q 1 . :a :p 2 . }";
		List<BindingSet> rows = selectWithData(conn, trig, RDFFormat.TRIG,
				"SELECT ?g ?x ?n WHERE {\n" +
						"  GRAPH ?g { ?x :p ?n }\n" +
						"  MINUS { GRAPH ?g { ?x :q ?n } }\n" +
						"} ORDER BY ?g ?x ?n");
		assertEquals(setOf("g1|a|1", "g2|a|2"), triples(rows, "g", "x", "n"),
				"Active graph must be respected on the RHS as well (§13.3).");
	}

	void T23_graph_isolation_removes_only_in_graph_where_match_exists(RepositoryConnection conn) throws IOException {
		String trig = "@prefix : <http://ex/> .\n" +
				"GRAPH :g1 { :a :p 1 . }\n" +
				"GRAPH :g2 { :a :q 1 . :a :p 2 . :a :q 2 . }";
		List<BindingSet> rows = selectWithData(conn, trig, RDFFormat.TRIG,
				"SELECT ?g ?x ?n WHERE {\n" +
						"  GRAPH ?g { ?x :p ?n }\n" +
						"  MINUS { GRAPH ?g { ?x :q ?n } }\n" +
						"} ORDER BY ?g");
		assertEquals(setOf("g1|a|1"), triples(rows, "g", "x", "n"),
				"Only the :g2 row should be removed because :q 2 exists in :g2.");
	}

	void T24_minus_disjoint_varsets_is_noop_even_with_union_on_lhs(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 1 .\n" +
				":b :p 1 .\n" +
				":c :p 2 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?x WHERE {\n" +
						"  { ?x :p 1 } UNION { ?x :p 2 }\n" +
						"  MINUS { ?y :q 1 }\n" +
						"} ORDER BY ?x");
		assertEquals(setOf("a", "b", "c"), names(rows, "x"), "No shared vars → MINUS must be a no-op (§8.3).");
	}

	void T25_values_left_only_no_shared_vars_is_noop(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> . :a :q 1 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?a WHERE { VALUES ?a { 1 2 } MINUS { ?x :q 1 } } ORDER BY ?a");
		assertEquals(setOf("1", "2"), names(rows, "a"),
				"VALUES introduces no shared vars with RHS, so MINUS removes nothing.");
	}

	void T26_minus_shared_subset_only_subject_shared_removes_all_rows_for_that_subject(RepositoryConnection conn)
			throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 99 .\n" +
				":b :p 2 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?x ?n WHERE { ?x :p ?n MINUS { ?x :q ?m } } ORDER BY ?x");
		assertEquals(setOf("b|2"), pairs(rows, "x", "n"),
				"Since only ?x is shared, any :q for :a kills *all* its :p rows.");
	}

	void T27_rhs_subselect_order_by_limit_one_global_elimination(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 1 .\n" +
				":b :p 2 ; :q 2 .\n" +
				":c :p 3 ; :q 3 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?x WHERE {\n" +
						"  ?x :p ?n .\n" +
						"  MINUS {\n" +
						"    { SELECT ?x WHERE { ?x :q ?m } ORDER BY ?m LIMIT 1 }\n" +
						"  }\n" +
						"} ORDER BY ?x");
		assertEquals(setOf("b", "c"), names(rows, "x"),
				"Flattening/pushing ORDER BY/LIMIT across MINUS would change which row is removed.");
	}

	void T28_bnode_function_on_rhs_cannot_match_data_terms(RepositoryConnection conn) throws IOException {
		String ttl = "@prefix : <http://ex/> .\n" +
				"_:b1 a [] . _:b2 a [] .\n" +
				":k :p _:b1 . :l :p _:b2 .";
		List<BindingSet> rows = selectWithData(conn, ttl, RDFFormat.TURTLE,
				"SELECT ?s WHERE { ?s :p ?id MINUS { BIND(BNODE() AS ?id) } } ORDER BY ?s");
		assertEquals(setOf("k", "l"), names(rows, "s"),
				"BNODE() creates fresh, distinct bnodes – cannot match dataset objects, so MINUS is a no-op here.");
	}

	void T29_syntax_error_rebinding_in_rhs_must_fail_to_parse(RepositoryConnection conn) {
		String body = "SELECT * WHERE {\n" +
				"  ?x :p ?v .\n" +
				"  MINUS { ?x :q ?v . BIND(1 AS ?v) }   # re-binding ?v inside same RHS group is illegal\n" +
				"}";
		assertThrows(MalformedQueryException.class, () -> conn.prepareTupleQuery(PREFIX + body),
				"BIND target must not have been used earlier in the same group; parser should reject.");
	}

	public Stream<DynamicTest> tests() {

		return Stream.of(
				makeTest("T1_bindCreatesFreshVarInRight_NoOverlap_NoEffect",
						this::T1_bindCreatesFreshVarInRight_NoOverlap_NoEffect),
				makeTest("T3_bindBeforeUseIntroducesOverlap_EverythingRemoved",
						this::T3_bindBeforeUseIntroducesOverlap_EverythingRemoved),
				makeTest("T4_renamedVarsInsideRight_NoTrueOverlap_NoEffect",
						this::T4_renamedVarsInsideRight_NoTrueOverlap_NoEffect),
				makeTest("T5_randInsideDisjointRight_MinusHasNoEffect",
						this::T5_randInsideDisjointRight_MinusHasNoEffect),
				makeTest("T8_projectionExprOnLeftDoesNotAffectMinusOverlap_NoEffect",
						this::T8_projectionExprOnLeftDoesNotAffectMinusOverlap_NoEffect),
				makeTest("T9_projectionBeforeMinus_NoSharedVarsAfterSubselect_NoEffect",
						this::T9_projectionBeforeMinus_NoSharedVarsAfterSubselect_NoEffect),
				makeTest("T10_minusVsNotExists_WithThisDataTheyCoincide",
						this::T10_minusVsNotExists_WithThisDataTheyCoincide),
				makeTest("T11_multipleMinus_sharedThenIndependent_onlyFirstMatters",
						this::T11_multipleMinus_sharedThenIndependent_onlyFirstMatters),
				makeTest("T12_minusInsideOptional_affectsOnlyOptionalGroup",
						this::T12_minusInsideOptional_affectsOnlyOptionalGroup),
				makeTest("T13_minus_no_shared_vars_is_noop_select", this::T13_minus_no_shared_vars_is_noop_select),
				makeTest("T14_not_exists_contrast_to_minus_no_shared_vars",
						this::T14_not_exists_contrast_to_minus_no_shared_vars),
				makeTest("T15_rhs_filter_referencing_outer_var_is_unbound_and_ignored",
						this::T15_rhs_filter_referencing_outer_var_is_unbound_and_ignored),
				makeTest("T16_rhs_bind_of_outer_var_produces_unbound_then_overremoves_on_shared_subset",
						this::T16_rhs_bind_of_outer_var_produces_unbound_then_overremoves_on_shared_subset),
				makeTest("T17_rhs_bind_creates_intentional_shared_var_equalities",
						this::T17_rhs_bind_creates_intentional_shared_var_equalities),
				makeTest("T18_early_projection_should_not_change_minus_semantics",
						this::T18_early_projection_should_not_change_minus_semantics),
				makeTest("T19_subquery_pins_shared_var_against_optimizer",
						this::T19_subquery_pins_shared_var_against_optimizer),
				makeTest("T20_optional_inside_minus_only_removes_when_optional_matches",
						this::T20_optional_inside_minus_only_removes_when_optional_matches),
				makeTest("T21_not_exists_over_optional_is_always_false_here",
						this::T21_not_exists_over_optional_is_always_false_here),
				makeTest("T22_graph_isolation_same_g_on_both_sides_no_removal_when_values_differ",
						this::T22_graph_isolation_same_g_on_both_sides_no_removal_when_values_differ),
				makeTest("T23_graph_isolation_removes_only_in_graph_where_match_exists",
						this::T23_graph_isolation_removes_only_in_graph_where_match_exists),
				makeTest("T24_minus_disjoint_varsets_is_noop_even_with_union_on_lhs",
						this::T24_minus_disjoint_varsets_is_noop_even_with_union_on_lhs),
				makeTest("T25_values_left_only_no_shared_vars_is_noop",
						this::T25_values_left_only_no_shared_vars_is_noop),
				makeTest("T26_minus_shared_subset_only_subject_shared_removes_all_rows_for_that_subject",
						this::T26_minus_shared_subset_only_subject_shared_removes_all_rows_for_that_subject),
				makeTest("T27_rhs_subselect_order_by_limit_one_global_elimination",
						this::T27_rhs_subselect_order_by_limit_one_global_elimination),
				makeTest("T28_bnode_function_on_rhs_cannot_match_data_terms",
						this::T28_bnode_function_on_rhs_cannot_match_data_terms),
				makeTest("T29_syntax_error_rebinding_in_rhs_must_fail_to_parse",
						this::T29_syntax_error_rebinding_in_rhs_must_fail_to_parse)
		);

	}

}
