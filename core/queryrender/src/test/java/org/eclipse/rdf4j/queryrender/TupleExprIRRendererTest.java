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

package org.eclipse.rdf4j.queryrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class TupleExprIRRendererTest {

	private static final String EX = "http://ex/";

	private static final String SPARQL_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX ex: <http://ex/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";
	private TestInfo testInfo;

	// Shared renderer config with canonical whitespace and useful prefixes.
	private static TupleExprIRRenderer.Config cfg() {
		TupleExprIRRenderer.Config style = new TupleExprIRRenderer.Config();
		style.prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		style.prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		style.prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
		style.prefixes.put("ex", "http://ex/");
		style.prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		style.valuesPreserveOrder = true;
		return style;
	}

//	@Test
//	void render_throws_when_round_trip_differs() {
//		String q = "SELECT * WHERE { ?s ?p ?o . }";
//		TupleExpr tupleExpr = parseAlgebra(SPARQL_PREFIX + q);
//
//		TupleExprIRRenderer tamperingRenderer = new TupleExprIRRenderer() {
//			@Override
//			public IrSelect toIRSelect(TupleExpr original) {
//				IrSelect ir = super.toIRSelect(original);
//				// Strip the WHERE body to force a semantic mismatch after rendering.
//				ir.setWhere(new IrBGP(false));
//				return ir;
//			}
//		};
//
//		assertThrows(IllegalStateException.class, () -> tamperingRenderer.render(tupleExpr));
//	}

	@BeforeEach
	void _captureTestInfo(TestInfo info) {
		this.testInfo = info;
		purgeReportFilesForCurrentTest();
	}

	private static void writeReportFile(String base, String label, String content) {
		Path dir = Paths.get("target", "surefire-reports");
		try {
			Files.createDirectories(dir);
			Path file = dir.resolve(base + "_" + label + ".txt");
			Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
			// Optional: surface where things went
			System.out.println("[debug] wrote " + file.toAbsolutePath());
		} catch (IOException ioe) {
			// Don't mask the real assertion failure if file I/O borks
			System.err.println("⚠️ Failed to write " + label + " to surefire-reports: " + ioe);
		}
	}

	// ---------- Helpers ----------

	// --- compute full-class-name#test-method-name (same as your writer uses) ---
	private String currentTestBaseName() {
		String cls = testInfo != null && testInfo.getTestClass().isPresent()
				? testInfo.getTestClass().get().getName()
				: "UnknownClass";
		String method = testInfo != null && testInfo.getTestMethod().isPresent()
				? testInfo.getTestMethod().get().getName()
				: "UnknownMethod";
		return cls + "#" + method;
	}

	// --- delete the four files if they exist ---
	private static final Path SUREFIRE_DIR = Paths.get("target", "surefire-reports");
	private static final String[] REPORT_LABELS = new String[] {
			"SPARQL_expected",
			"SPARQL_actual",
			"TupleExpr_expected",
			"TupleExpr_actual"
	};

	private void purgeReportFilesForCurrentTest() {
		String base = currentTestBaseName();
		for (String label : REPORT_LABELS) {
			Path file = SUREFIRE_DIR.resolve(base + "_" + label + ".txt");
			try {
				Files.deleteIfExists(file);
			} catch (IOException e) {
				// Don’t block the test on cleanup trouble; just log
				System.err.println("⚠️ Unable to delete old report file: " + file.toAbsolutePath() + " :: " + e);
			}
		}
	}

	private TupleExpr parseAlgebra(String sparql) {
		try {
			ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, sparql, null);
			return pq.getTupleExpr();
		} catch (MalformedQueryException e) {
			throw new MalformedQueryException(
					"Failed to parse SPARQL query.\n###### QUERY ######\n" + sparql + "\n\n######################",
					e);
		}

	}

	private String render(String sparql, TupleExprIRRenderer.Config cfg) {
		TupleExpr algebra = parseAlgebra(sparql);
		if (sparql.contains("ASK")) {
			return new TupleExprIRRenderer(cfg).renderAsk(algebra, null).trim();
		}

		if (sparql.contains("DESCRIBE")) {
			return new TupleExprIRRenderer(cfg).renderAsk(algebra, null).trim();
		}

		return new TupleExprIRRenderer(cfg).render(algebra, null).trim();
	}

	/** Round-trip twice and assert the renderer is a fixed point (idempotent). */
	private String assertFixedPoint(String sparql, TupleExprIRRenderer.Config cfg) {
//		System.out.println("# Original SPARQL query\n" + sparql + "\n");
		TupleExpr tupleExpr = parseAlgebra(SPARQL_PREFIX + sparql);
//		System.out.println("# Original TupleExpr\n" + tupleExpr + "\n");
		String r1 = render(SPARQL_PREFIX + sparql, cfg);
		String r2;
		try {
			r2 = render(r1, cfg);
		} catch (MalformedQueryException e) {
			throw new RuntimeException("Failed to parse SPARQL query after rendering.\n### Original query ###\n"
					+ sparql + "\n\n### Rendered query ###\n" + r1 + "\n", e);
		}
		assertEquals(r1, r2, "Renderer must be idempotent after one round-trip");
		String r3 = render(r2, cfg);
		assertEquals(r2, r3, "Renderer must be idempotent after two round-trips");
		return r2;
	}

//	private String currentTestBaseName() {
//		String cls = testInfo != null && testInfo.getTestClass().isPresent()
//				? testInfo.getTestClass().get().getName()
//				: "UnknownClass";
//		String method = testInfo != null && testInfo.getTestMethod().isPresent()
//				? testInfo.getTestMethod().get().getName()
//				: "UnknownMethod";
//		return cls + "#" + method;
//	}

	/** Assert semantic equivalence by comparing result rows (order-insensitive). */

	/** Assert semantic equivalence by comparing result rows (order-insensitive). */
	private void assertSameSparqlQuery(String sparql, TupleExprIRRenderer.Config cfg, boolean requireStringEquality) {
//		cfg.debugIR = true;

		sparql = sparql.trim();

		TupleExpr expected = parseAlgebra(SPARQL_PREFIX + sparql);
//		System.out.println("# Original SPARQL query\n" + SparqlFormatter.format(sparql) + "\n");
//		System.out.println("# Original TupleExpr\n" + expected + "\n");
		String rendered = render(SPARQL_PREFIX + sparql, cfg);
//		System.out.println("# Actual SPARQL query\n" + SparqlFormatter.format(rendered) + "\n");
		TupleExpr actual = parseAlgebra(rendered);

		try {
			assertThat(VarNameNormalizer.normalizeVars(actual.toString()))
					.as("Algebra after rendering must be identical to original")
					.isEqualTo(VarNameNormalizer.normalizeVars(expected.toString()));

			if (requireStringEquality) {
				assertThat(rendered).isEqualToNormalizingNewlines(SPARQL_PREFIX + sparql);
			}

		} catch (Throwable t) {

//			assertThat(VarNameNormalizer.normalizeVars(actual.toString()))
//					.as("Algebra after rendering must be identical to original")
//					.isEqualTo(VarNameNormalizer.normalizeVars(expected.toString()));

			// Gather as much as we can without throwing during diagnostics
			String base = currentTestBaseName();

			String expectedSparql = SPARQL_PREFIX + sparql;
			TupleExpr expectedTe = null;
			try {
				expectedTe = parseAlgebra(expectedSparql);
			} catch (Throwable parseExpectedFail) {
				// Extremely unlikely, but don't let this hide the original failure
			}

			TupleExpr actualTe = null;

			System.out.println("\n\n\n");
			System.out.println("# Original SPARQL query\n" + SparqlFormatter.format(sparql) + "\n");
			if (expectedTe != null) {
				System.out.println("# Original TupleExpr\n" + expectedTe + "\n");
			}

			try {
				cfg.debugIR = true;
				System.out.println("\n# Re-rendering with IR debug enabled for this failing test\n");
				rendered = render(expectedSparql, cfg);
				System.out.println("\n# Rendered SPARQL query\n" + rendered + "\n");
			} catch (Throwable renderFail) {
				rendered = "<render failed: " + renderFail + ">";
			} finally {
				cfg.debugIR = false;
			}

			try {
				if (!rendered.startsWith("<render failed")) {
					actualTe = parseAlgebra(rendered);
					System.out.println("# Actual TupleExpr\n" + actualTe + "\n");
				}
			} catch (Throwable parseActualFail) {
				System.out.println("# Actual TupleExpr\n<parse failed: " + parseActualFail + ">\n");
				// Keep actualTe as null; we'll record a placeholder
			}

			// --- Write the four artifacts ---
			writeReportFile(base, "SPARQL_expected", expectedSparql);
			writeReportFile(base, "SPARQL_actual", rendered);

			writeReportFile(base, "TupleExpr_expected",
					expectedTe != null ? VarNameNormalizer.normalizeVars(expectedTe.toString())
							: "<expected TupleExpr unavailable: parse failed>");

			writeReportFile(base, "TupleExpr_actual",
					actualTe != null ? VarNameNormalizer.normalizeVars(actualTe.toString())
							: "<actual TupleExpr unavailable: " +
									"parse failed" + ">");

			rendered = render(expectedSparql, cfg);

			// Fail (again) with the original comparison so the test result is correct
			assertThat(rendered).isEqualToNormalizingNewlines(SPARQL_PREFIX + sparql);
		}
	}
	// ---------- Tests: fixed point + semantic equivalence where applicable ----------

	@Test
	void basic_select_bgp() {
		String q = "SELECT ?s ?name WHERE {\n" +
				"  ?s a foaf:Person ; foaf:name ?name .\n" +
				"}";
		assertFixedPoint(q, cfg());
	}

	@Test
	void filter_compare_and_regex() {
		String q = "SELECT ?s ?name WHERE {\n" +
				"  ?s foaf:name ?name .\n" +
				"  FILTER ((?name != \"Zed\") && REGEX(?name, \"a\", \"i\"))\n" +
				"}";
		assertFixedPoint(q, cfg());
	}

	@Test
	void optional_with_condition() {
		String q = "SELECT ?s ?age WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"  OPTIONAL {\n" +
				"    ?s ex:age ?age .\n" +
				"    FILTER (?age >= 18)\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void union_of_groups() {
		String q = "SELECT ?who WHERE {\n" +
				"  {\n" +
				"    ?who foaf:name \"Alice\" .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?who foaf:name \"Bob\" .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void order_by_limit_offset() {
		String q = "SELECT ?name WHERE {\n" +
				"  ?s foaf:name ?name .\n" +
				"}\n" +
				"ORDER BY DESC(?name)\n" +
				"LIMIT 2\n" +
				"OFFSET 0";
		// Semantic equivalence depends on ordering; still fine since we run the same query
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void values_single_var_and_undef() {
		String q = "SELECT ?x WHERE {\n" +
				"  VALUES (?x) {\n" +
				"    (ex:alice)\n" +
				"    (UNDEF)\n" +
				"    (ex:bob)\n" +
				"  }\n" +
				"  ?x foaf:name ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void values_multi_column() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  VALUES (?n ?s) {\n" +
				"    (\"Alice\" ex:alice)\n" +
				"    (\"Bob\" ex:bob)\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void bind_inside_where() {
		String q = "SELECT ?s ?sn WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"  BIND(STR(?n) AS ?sn)\n" +
				"  FILTER (STRSTARTS(?sn, \"A\"))\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void aggregates_count_star_and_group_by() {
		String q = "SELECT (COUNT(*) AS ?c) WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}";
		// No dataset dependency issues; simple count
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void aggregates_count_distinct_group_by() {
		String q = "SELECT (COUNT(DISTINCT ?o) AS ?c) ?s  WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}\n" +
				"GROUP BY ?s";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void group_concat_with_separator_literal() {
		String q = "SELECT (GROUP_CONCAT(?name; SEPARATOR=\", \") AS ?names) WHERE {\n" +
				"  ?s foaf:name ?name .\n" +
				"}";
		// Semantic equivalence: both queries run in the same engine; comparing string results
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void service_silent_block() {
		String q = "SELECT ?s ?p ?o WHERE {\n" +
				"  SERVICE SILENT <http://example.org/sparql> {\n" +
				"    ?s ?p ?o .\n" +
				"  }\n" +
				"}";
		// We do not execute against remote SERVICE; check fixed point only:
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void property_paths_star_plus_question() {
		// These rely on RDF4J producing ArbitraryLengthPath for +/*/?.
		String qStar = "SELECT ?x ?y WHERE {\n" +
				"  ?x ex:knows*/foaf:name ?y .\n" +
				"}";
		String qPlus = "SELECT ?x ?y WHERE {\n" +
				"  ?x ex:knows+/foaf:name ?y .\n" +
				"}";
		String qOpt = "SELECT ?x ?y WHERE {\n" +
				"  ?x ex:knows?/foaf:name ?y .\n" +
				"}";

		assertSameSparqlQuery(qStar, cfg(), false);
		assertSameSparqlQuery(qPlus, cfg(), false);
		assertSameSparqlQuery(qOpt, cfg(), false);
	}

	@Test
	void rdf_star_triple_terms_render_verbatim() {
		String q = "SELECT * WHERE {\n" +
				"  <<ex:s ex:p ex:o>> ex:q ?x .\n" +
				"}";
		String rendered = render(SPARQL_PREFIX + q, cfg());
//		assertTrue(rendered.contains("<<ex:s ex:p ex:o>>"), "RDF-star triple term must render as <<...>>");
		// Round-trip to ensure algebra equivalence once triple text is correct.
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void blank_node_square_brackets_render_as_empty_bnode() {
		String q = "SELECT ?s1 ?s2 WHERE {\n" +
				"  ?s1 ex:p [] .\n" +
				"  _:bnode1 ex:p [] .\n" +
				"  ?s2 ex:p [] .\n" +
				"  [] ex:p _:bnode1 .\n" +
				"  [] ex:p _:bnode1 .\n" +
				"}";
		String rendered = render(SPARQL_PREFIX + q, cfg());
		assertSameSparqlQuery(q, cfg(), true);
	}

	@Test
	void rdf_type_renders_as_a_keyword() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s a ?o .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), true);

	}

	@Test
	void regex_flags_and_lang_filters() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"  FILTER (REGEX(?n, \"^a\", \"i\") || LANGMATCHES(LANG(?n), \"en\"))\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void datatype_filter_and_is_tests() {
		String q = "SELECT ?s ?age WHERE {\n" +
				"  ?s ex:age ?age .\n" +
				"  FILTER ((DATATYPE(?age) = xsd:integer) && isLiteral(?age))\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void distinct_projection_and_reduced_shell() {
		String q = "SELECT DISTINCT ?s WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}\n" +
				"LIMIT 10\n" +
				"OFFSET 1";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// ----------- Edge/robustness cases ------------

	@Test
	void empty_where_is_not_produced_and_triple_format_stable() {
		String q = "SELECT * WHERE { ?s ?p ?o . }";
		String rendered = assertFixedPoint(q, cfg());
		// Ensure one triple per line and trailing dot
		assertTrue(rendered.contains("?s ?p ?o ."), "Triple should be printed with trailing dot");
		assertTrue(rendered.contains("WHERE {\n"), "Block should open with newline");
	}

	@Test
	void values_undef_matrix() {
		String q = "SELECT ?a ?b WHERE {\n" +
				"  VALUES (?a ?b) {\n" +
				"    (\"x\" UNDEF)\n" +
				"    (UNDEF \"y\")\n" +
				"    (\"x\" \"y\")\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void count_and_sum_in_select_with_group_by() {
		String q = "SELECT ?s (COUNT(?o) AS ?c) (SUM(?age) AS ?sumAge) WHERE {\n" +
				"  {\n" +
				"    ?s ?p ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s ex:age ?age .\n" +
				"  }\n" +
				"}\n" +
				"GROUP BY ?s";
		// Semantic equivalence: engine evaluates both sides consistently
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void order_by_multiple_keys() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"}\n" +
				"ORDER BY ?n DESC(?s)";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void list_member_in_and_not_in() {
		String q = "SELECT ?s WHERE {\n" +
				"  VALUES (?s) {\n" +
				"    (ex:alice)\n" +
				"    (ex:bob)\n" +
				"    (ex:carol)\n" +
				"  }\n" +
				"  FILTER (?s IN (ex:alice, ex:bob))\n" +
				"  FILTER (?s != ex:bob)\n" +
				"  FILTER (!(?s = ex:bob))\n" +
				"}";
		String r = assertFixedPoint(q, cfg());
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void exists_in_filter_and_bind() {
		String q = "SELECT ?hasX WHERE {\n" +
				"  OPTIONAL {\n" +
				"    BIND(EXISTS { ?s ?p ?o . } AS ?hasX)\n" +
				"  }\n" +
				"  FILTER (EXISTS { ?s ?p ?o . })\n" +
				"}";
		String r = assertFixedPoint(q, cfg());
		assertTrue(r.contains("EXISTS {"), "should render EXISTS");
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void strlen_alias_for_fn_string_length() {
		String q = "SELECT ?s ?p ?o WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  FILTER (STRLEN(STR(?o)) > 1)\n" +
				"}";
		String r = assertFixedPoint(q, cfg());
		assertTrue(r.contains("STRLEN("), "fn:string-length should render as STRLEN");
		assertSameSparqlQuery(q, cfg(), false);
	}

	// =========================
	// ===== New test cases ====
	// =========================

	// --- Negation: NOT EXISTS & MINUS ---

	@Test
	void filter_not_exists() {
		String q = "SELECT ?s WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  FILTER (NOT EXISTS { ?s foaf:name ?n . })\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void minus_set_difference() {
		String q = "SELECT ?s WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  MINUS {\n" +
				"    ?s foaf:name ?n .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// --- Property paths (sequence, alternation, inverse, NPS, grouping) ---

	@Test
	void property_paths_sequence_and_alternation() {
		String q = "SELECT ?x ?name WHERE { ?x (ex:knows/foaf:knows)|(foaf:knows/ex:knows) ?y . ?y foaf:name ?name }";
		assertFixedPoint(q, cfg());
	}

	@Test
	void property_paths_inverse() {
		String q = "SELECT ?x ?y WHERE { ?x ^foaf:knows ?y }";
		assertFixedPoint(q, cfg());
	}

	@Test
	void property_paths_negated_property_set() {
		String q = "SELECT ?x ?y WHERE {\n" +
				"  ?x !(rdf:type|^rdf:type) ?y .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void property_paths_grouping_precedence() {
		String q = "SELECT ?x ?y WHERE { ?x (ex:knows/ (foaf:knows|^foaf:knows)) ?y }";
		assertFixedPoint(q, cfg());
	}

	// --- Assignment forms: SELECT (expr AS ?v), GROUP BY (expr AS ?v) ---

	@Test
	void select_projection_expression_alias() {
		String q = "SELECT ((?age + 1) AS ?age1) WHERE {\n" +
				"  ?s ex:age ?age .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void group_by_with_alias_and_having() {
		String q = "SELECT ?name (COUNT(?s) AS ?c) WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"  BIND(STR(?n) AS ?name)\n" +
				"}\n" +
				"GROUP BY (?n AS ?name)\n" +
				"HAVING (COUNT(?s) > 1)\n" +
				"ORDER BY DESC(?c)";
		assertSameSparqlQuery(q, cfg(), true);
	}

	// --- Aggregates: MIN/MAX/AVG/SAMPLE + HAVING ---

	@Test
	void aggregates_min_max_avg_sample_having() {
		String q = "SELECT ?s (MIN(?o) AS ?minO) (MAX(?o) AS ?maxO) (AVG(?o) AS ?avgO) (SAMPLE(?o) AS ?anyO)\n" +
				"WHERE { ?s ?p ?o . }\n" +
				"GROUP BY ?s\n" +
				"HAVING (COUNT(?o) >= 1)";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// --- Subquery with aggregate and scope ---

	@Test
	void subquery_with_aggregate_and_having() {
		String q = "SELECT ?y ?minName WHERE {\n" +
				"  ex:alice foaf:knows ?y .\n" +
				"  {\n" +
				"    SELECT ?y (MIN(?name) AS ?minName)\n" +
				"    WHERE { ?y foaf:name ?name . }\n" +
				"    GROUP BY ?y\n" +
				"    HAVING (MIN(?name) >= \"A\")\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// --- GRAPH with IRI and variable ---

	@Test
	void graph_iri_and_variable() {
		String q = "SELECT ?g ?s WHERE {\n" +
				"  GRAPH ex:g1 { ?s ?p ?o }\n" +
				"  GRAPH ?g   { ?s ?p ?o }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// --- Federation: SERVICE (no SILENT) and variable endpoint ---

	@Test
	void service_without_silent() {
		String q = "SELECT * WHERE { SERVICE <http://example.org/sparql> { ?s ?p ?o } }";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void service_variable_endpoint() {
		String q = "SELECT * WHERE { SERVICE ?svc { ?s ?p ?o } }";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// --- Solution modifiers: REDUCED; ORDER BY expression; OFFSET-only; LIMIT-only ---

	@Test
	void select_reduced_modifier() {
		String q = "SELECT REDUCED ?s WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void order_by_expression_and_by_aggregate_alias() {
		String q = "SELECT ?n (COUNT(?s) AS ?c)\n" +
				"WHERE { ?s foaf:name ?n }\n" +
				"GROUP BY ?n\n" +
				"ORDER BY LCASE(?n) DESC(?c)";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void offset_only() {
		String q = "SELECT ?s ?p ?o WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}\n" +
				"OFFSET 5";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void limit_only_zero_and_positive() {
		String q1 = "SELECT ?s ?p ?o WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}\n" +
				"LIMIT 0";
		String q2 = "SELECT ?s ?p ?o WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}\n" +
				"LIMIT 3";
		assertSameSparqlQuery(q1, cfg(), false);
		assertSameSparqlQuery(q2, cfg(), false);
	}

	// --- Expressions & built-ins ---

	@Test
	void functional_forms_and_rdf_term_tests() {
		String q = "SELECT ?ok1 ?ok2 ?ok3 ?ok4 WHERE {\n" +
				"  VALUES (?x) { (1) }\n" +
				"  BIND(IRI(CONCAT(\"http://ex/\", \"alice\")) AS ?iri)\n" +
				"  BIND(BNODE() AS ?b)\n" +
				"  BIND(STRDT(\"2020-01-01\", xsd:date) AS ?d)\n" +
				"  BIND(STRLANG(\"hi\", \"en\") AS ?l)\n" +
				"  BIND(IF(BOUND(?iri), true, false) AS ?ok1)\n" +
				"  BIND(COALESCE(?missing, ?x) AS ?ok2)\n" +
				"  BIND(sameTerm(?iri, IRI(\"http://ex/alice\")) AS ?ok3)\n" +
				"  BIND((isIRI(?iri) && isBlank(?b) && isLiteral(?l) && isNumeric(?x)) AS ?ok4)\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void string_functions_concat_substr_replace_encode() {
		String q = "SELECT ?a ?b ?c ?d WHERE {\n" +
				"  VALUES (?n) { (\"Alice\") }\n" +
				"  BIND(CONCAT(?n, \" \", \"Doe\") AS ?a)\n" +
				"  BIND(SUBSTR(?n, 2) AS ?b)\n" +
				"  BIND(REPLACE(?n, \"A\", \"a\") AS ?c)\n" +
				"  BIND(ENCODE_FOR_URI(?n) AS ?d)\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void numeric_datetime_hash_and_random() {
		String q = "SELECT ?r ?now ?y ?tz ?abs ?ceil ?floor ?round ?md5 WHERE {\n" +
				"  VALUES (?x) { (\"abc\") }\n" +
				"  BIND(RAND() AS ?r)\n" +
				"  BIND(NOW() AS ?now)\n" +
				"  BIND(YEAR(?now) AS ?y)\n" +
				"  BIND(TZ(?now) AS ?tz)\n" +
				"  BIND(ABS(-2.5) AS ?abs)\n" +
				"  BIND(CEIL(2.1) AS ?ceil)\n" +
				"  BIND(FLOOR(2.9) AS ?floor)\n" +
				"  BIND(ROUND(2.5) AS ?round)\n" +
				"  BIND(MD5(?x) AS ?md5)\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void uuid_and_struuid() {
		String q = "SELECT (UUID() AS ?u) (STRUUID() AS ?su) WHERE {\n" +
				"}";
		assertFixedPoint(q, cfg());
	}

	@Test
	void not_in_and_bound() {
		String q = "SELECT ?s WHERE {\n" +
				"  VALUES ?s { ex:alice ex:bob ex:carol }\n" +
				"  OPTIONAL { ?s foaf:nick ?nick }\n" +
				"  FILTER(BOUND(?nick) || (?s NOT IN (ex:bob)))\n" +
				"}";
		assertFixedPoint(q, cfg());
	}

	// --- VALUES short form and empty edge case ---

	@Test
	void values_single_var_short_form() {
		String q = "SELECT ?s WHERE {\n" +
				"  VALUES (?s) {\n" +
				"    (ex:alice)\n" +
				"    (ex:bob)\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void values_empty_block() {
		String q = "SELECT ?s WHERE {\n" +
				"  VALUES (?s) {\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// --- Syntactic sugar: blank node property list and collections ---

	@Test
	void blank_node_property_list() {
		String q = "SELECT ?n WHERE {\n" +
				"  [] foaf:name ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void collections() {
		String q = "SELECT ?el WHERE {\n" +
				"  (1 2 3) rdf:rest*/rdf:first ?el .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// ==========================================
	// ===== Complex integration-style tests ====
	// ==========================================

	@Test
	void complex_kitchen_sink_paths_graphs_subqueries() {
		String q = "SELECT REDUCED ?g ?y (?cnt AS ?count) (COALESCE(?avgAge, -1) AS ?ageOrMinus1) WHERE {\n" +
				"  VALUES (?g) {\n" +
				"    (ex:g1)\n" +
				"    (ex:g2)\n" +
				"  }\n" +
				"  GRAPH ?g {\n" +
				"    ?x (foaf:knows|ex:knows)/^foaf:knows ?y .\n" +
				"    ?y foaf:name ?name .\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    GRAPH ?g {\n" +
				"      ?y ex:age ?age .\n" +
				"    }\n" +
				"    FILTER (?age >= 21)\n" +
				"  }\n" +
				"  MINUS {\n" +
				"    ?y a ex:Robot .\n" +
				"  }\n" +
				"  FILTER (NOT EXISTS { ?y foaf:nick ?nick . FILTER (STRLEN(?nick) > 0) })\n" +
				"  {\n" +
				"    SELECT ?y (COUNT(DISTINCT ?name) AS ?cnt) (AVG(?age) AS ?avgAge)\n" +
				"    WHERE {\n" +
				"      ?y foaf:name ?name .\n" +
				"      OPTIONAL {\n" +
				"        ?y ex:age ?age .\n" +
				"      }\n" +
				"    }\n" +
				"    GROUP BY ?y\n" +
				"  }\n" +
				"}\n" +
				"ORDER BY DESC(?cnt) LCASE(?name)\n" +
				"LIMIT 10\n" +
				"OFFSET 5";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testMoreGraph1() {
		String q = "SELECT REDUCED ?g ?y (?cnt AS ?count) (COALESCE(?avgAge, -1) AS ?ageOrMinus1) WHERE {\n" +
				"  VALUES ?g { ex:g1 ex:g2 }\n" +
				"  GRAPH ?g {\n" +
				"    ?x (foaf:knows|ex:knows)/^foaf:knows ?y .\n" +
				"    ?y foaf:name ?name .\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    GRAPH ?g {\n" +
				"      ?y ex:age ?age .\n" +
				"    }\n" +
				"    FILTER (?age >= 21)\n" +
				"  }\n" +
				"  MINUS {\n" +
				"    ?y a ex:Robot .\n" +
				"  }\n" +
				"  FILTER NOT EXISTS {\n" +
				"    ?y foaf:nick ?nick .\n" +
				"    FILTER (STRLEN(?nick) > 0)\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testMoreGraph2() {
		String q = "SELECT REDUCED ?g ?y (?cnt AS ?count) (COALESCE(?avgAge, -1) AS ?ageOrMinus1) WHERE {\n" +
				"  VALUES (?g) {\n" +
				"    (ex:g1)\n" +
				"    (ex:g2)\n" +
				"  }\n" +
				"  GRAPH ?g {\n" +
				"    ?x (foaf:knows|ex:knows)/^foaf:knows ?y .\n" +
				"    ?y foaf:name ?name .\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    GRAPH ?g {\n" +
				"      ?y ex:age ?age .\n" +
				"    }\n" +
				"    FILTER (?age >= 21)\n" +
				"  }\n" +
				"  MINUS {\n" +
				"    ?y a ex:Robot .\n" +
				"  }\n" +
				"  FILTER (NOT EXISTS { ?y foaf:nick ?nick . FILTER (STRLEN(?nick) > 0) })\n" +
				"  {\n" +
				"    SELECT ?y ?name\n" +
				"    WHERE {\n" +
				"      ?y foaf:name ?name .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void morePathInGraph() {
		String q = "SELECT REDUCED ?g ?y (?cnt AS ?count) (COALESCE(?avgAge, -1) AS ?ageOrMinus1) WHERE {\n" +
				"  VALUES (?g) {\n" +
				"    (ex:g1)\n" +
				"    (ex:g2)\n" +
				"  }\n" +
				"  GRAPH ?g {\n" +
				"    ?x (foaf:knows|ex:knows)/^foaf:knows ?y .\n" +
				"    ?y foaf:name ?name .\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    ?y ex:age ?age .\n" +
				"    FILTER (?age >= 21)\n" +
				"  }\n" +
				"}\n" +
				"ORDER BY DESC(?cnt) LCASE(?name)\n" +
				"LIMIT 10\n" +
				"OFFSET 5";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void complex_deep_union_optional_with_grouping() {
		String q = "SELECT ?s ?label ?src (SUM(?innerC) AS ?c) WHERE {\n" +
				"  VALUES ?src { \"A\" \"B\" }\n" +
				"  {\n" +
				"    ?s a foaf:Person .\n" +
				"    OPTIONAL {\n" +
				"      ?s rdfs:label ?label .\n" +
				"      FILTER (LANGMATCHES(LANG(?label), \"en\"))\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?anon1 foaf:name ?label .\n" +
				"    BIND( \"B\" AS ?src)\n" +
				"    BIND( BNODE() AS ?s)\n" +
				"  }\n" +
				"  {\n" +
				"    SELECT ?s (COUNT(?o) AS ?innerC)\n" +
				"    WHERE {\n" +
				"      ?s ?p ?o .\n" +
				"      FILTER (?p != rdf:type)\n" +
				"    }\n" +
				"    GROUP BY ?s\n" +
				"    HAVING (COUNT(?o) >= 0)\n" +
				"  }\n" +
				"}\n" +
				"GROUP BY ?s ?label ?src\n" +
				"HAVING (SUM(?innerC) >= 1)\n" +
				"ORDER BY DESC( ?c) STRLEN( COALESCE(?label, \"\"))\n" +
				"LIMIT 20";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void complex_federated_service_subselect_and_graph() {
		String q = "SELECT ?u ?g (COUNT(DISTINCT ?p) AS ?pc) WHERE {\n" +
				"  SERVICE <http://example.org/sparql> {\n" +
				"    {\n" +
				"      SELECT ?u ?p WHERE {\n" +
				"        ?u ?p ?o .\n" +
				"        FILTER (?p != rdf:type)\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"  GRAPH ?g {\n" +
				"    ?u !(ex:age|foaf:knows) ?any .\n" +
				"  }\n" +
				"  FILTER (EXISTS { GRAPH ?g { ?u foaf:name ?n . } })\n" +
				"}\n" +
				"GROUP BY ?u ?g\n" +
				"ORDER BY DESC(?pc)\n" +
				"LIMIT 7\n" +
				"OFFSET 3";

		collections();

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void complex_ask_with_subselect_exists_and_not_exists() {
		String q = "SELECT ?g ?s ?n WHERE {\n" +
				"  VALUES (?g) {\n" +
				"    (ex:g1)\n" +
				"  }\n" +
				"  GRAPH ?g {\n" +
				"    ?s foaf:name ?n .\n" +
				"  }\n" +
				"  FILTER (EXISTS { { SELECT ?s WHERE { ?s foaf:knows ?t . } GROUP BY ?s HAVING (COUNT(?t) > 1) } })\n"
				+
				"  FILTER (NOT EXISTS { ?s ex:blockedBy ?b . })\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void complex_expressions_aggregation_and_ordering() {
		String q = "SELECT ?s (CONCAT(LCASE(STR(?n)), \"-\", STRUUID()) AS ?tag) (MAX(?age) AS ?maxAge) WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"  OPTIONAL {\n" +
				"    ?s ex:age ?age .\n" +
				"  }\n" +
				"  FILTER ((STRLEN(?n) > 1) && (isLiteral(?n) || BOUND(?n)))\n" +
				"  FILTER ((REPLACE(?n, \"A\", \"a\") != ?n) || (?s IN (ex:alice, ex:bob)))\n" +
				"  FILTER ((DATATYPE(?age) = xsd:integer) || !(BOUND(?age)))\n" +
				"}\n" +
				"GROUP BY ?s ?n\n" +
				"ORDER BY STRLEN(?n) DESC(?maxAge)\n" +
				"LIMIT 50";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void complex_mutual_knows_with_degree_subqueries() {
		String q = "SELECT ?a ?b ?aC ?bC WHERE {\n" +
				"  {\n" +
				"    SELECT ?a (COUNT(?ka) AS ?aC)\n" +
				"    WHERE {\n" +
				"      ?a foaf:knows ?ka .\n" +
				"    }\n" +
				"    GROUP BY ?a\n" +
				"  }\n" +
				"  {\n" +
				"    SELECT ?b (COUNT(?kb) AS ?bC)\n" +
				"    WHERE {\n" +
				"      ?b foaf:knows ?kb .\n" +
				"    }\n" +
				"    GROUP BY ?b\n" +
				"  }\n" +
				"  ?a foaf:knows ?b .\n" +
				"  FILTER (EXISTS { ?b foaf:knows ?a . })\n" +
				"}\n" +
				"ORDER BY DESC(?aC + ?bC)\n" +
				"LIMIT 10";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void complex_path_inverse_and_negated_set_mix() {
		String q = "SELECT ?a ?n WHERE {\n" +
				"  ?a (^foaf:knows/!(ex:helps|ex:knows|rdf:subject|rdf:type)/foaf:name) ?n .\n" +
				"  FILTER ((LANG(?n) = \"\") || LANGMATCHES(LANG(?n), \"en\"))\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void complex_service_variable_and_nested_subqueries() {
		String q = "SELECT ?svc ?s (SUM(?c) AS ?total) WHERE {\n" +
				"  BIND(<http://example.org/sparql> AS ?svc)\n" +
				"  SERVICE ?svc {\n" +
				"    {\n" +
				"      SELECT ?s (COUNT(?p) AS ?c)\n" +
				"      WHERE {\n" +
				"        ?s ?p ?o .\n" +
				"      }\n" +
				"      GROUP BY ?s\n" +
				"    }\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    GRAPH ?g {\n" +
				"      ?s foaf:name ?n .\n" +
				"    }\n" +
				"  }\n" +
				"  MINUS {\n" +
				"    ?s a ex:Robot .\n" +
				"  }\n" +
				"}\n" +
				"GROUP BY ?svc ?s\n" +
				"HAVING (SUM(?c) >= 0)\n" +
				"ORDER BY DESC(?total)";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void complex_values_matrix_paths_and_groupby_alias() {
		String q = "SELECT ?key ?person (COUNT(?o) AS ?c) WHERE {\n" +
				"  {\n" +
				"    VALUES ?k { \"foaf\" }\n" +
				"    ?person foaf:knows/foaf:knows* ?other .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    VALUES ?k { \"foaf\" }\n" +
				"    ?person ex:knows/foaf:knows* ?other .\n" +
				"  }\n" +
				"  ?person ?p ?o .\n" +
				"  FILTER (?p != rdf:type)\n" +
				"}\n" +
				"GROUP BY (?k AS ?key) ?person\n" +
				"ORDER BY ?key DESC(?c)\n" +
				"LIMIT 100";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void groupByAlias() {
		String q = "SELECT ?predicate WHERE {\n" +
				"  ?a ?b ?c .\n" +
				"}\n" +
				"GROUP BY (?b AS ?predicate)\n" +
				"ORDER BY ?predicate\n" +
				"LIMIT 100";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// ================================================
	// ===== Ultra-heavy, limit-stretching tests ======
	// ================================================

	@Test
	void mega_monster_deep_nesting_everything() {
		String q = "SELECT REDUCED ?g ?x ?y (?cnt AS ?count) (IF(BOUND(?avgAge), (xsd:decimal(?cnt) + xsd:decimal(?avgAge)), xsd:decimal(?cnt)) AS ?score)\n"
				+
				"WHERE {\n" +
				"  VALUES (?g) {\n" +
				"    (ex:g1)\n" +
				"    (ex:g2)\n" +
				"    (ex:g3)\n" +
				"  }\n" +
				"  GRAPH ?g {\n" +
				"    ?x (foaf:knows/(^foaf:knows|ex:knows)*) ?y .\n" +
				"    OPTIONAL { ?y rdfs:label ?label FILTER (LANGMATCHES(LANG(?label), \"en\")) }\n" +
				"  }\n" +
				"  FILTER (NOT EXISTS { ?y ex:blockedBy ?b } && !EXISTS { ?y ex:status \"blocked\"@en })\n" +
				"  MINUS { ?y rdf:type ex:Robot }\n" +
				"  {\n" +
				"    SELECT ?y (COUNT(DISTINCT ?name) AS ?cnt) (AVG(?age) AS ?avgAge)\n" +
				"    WHERE {\n" +
				"      ?y foaf:name ?name .\n" +
				"      OPTIONAL { ?y ex:age ?age FILTER (DATATYPE(?age) = xsd:integer) }\n" +
				"    }\n" +
				"    GROUP BY ?y\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    {\n" +
				"      SELECT ?x (COUNT(?k) AS ?deg)\n" +
				"      WHERE { ?x foaf:knows ?k }\n" +
				"      GROUP BY ?x\n" +
				"    }\n" +
				"    FILTER (?deg >= 0)\n" +
				"  }\n" +
				"}\n" +
				"ORDER BY DESC(?cnt) LCASE(COALESCE(?label, \"\"))\n" +
				"LIMIT 50\n" +
				"OFFSET 10";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void mega_monster_deep_nesting_everything_simple() {
		String q = "SELECT REDUCED ?g ?x ?y (?cnt AS ?count) (IF(BOUND(?avgAge), (xsd:decimal(?cnt) + xsd:decimal(?avgAge)), xsd:decimal(?cnt)) AS ?score)\n"
				+
				"WHERE {\n" +
				"  VALUES (?g) {\n" +
				"    (ex:g1)\n" +
				"    (ex:g2)\n" +
				"    (ex:g3)\n" +
				"  }\n" +
				"  GRAPH ?g {\n" +
				"    ?x foaf:knows/(^foaf:knows|ex:knows)* ?y .\n" +
				"    OPTIONAL {\n" +
				"      ?y rdfs:label ?label .\n" +
				"    }\n" +
				"  }\n" +
				"  FILTER (LANGMATCHES(LANG(?label), \"en\"))\n" +
				"  FILTER (NOT EXISTS { ?y ex:blockedBy ?b . } && NOT EXISTS { ?y ex:status \"blocked\"@en . })\n" +
				"}\n" +
				"ORDER BY DESC(?cnt) LCASE(COALESCE(?label, \"\"))\n" +
				"LIMIT 50\n" +
				"OFFSET 10";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void mega_massive_union_chain_with_mixed_paths() {
		String q = "SELECT ?s ?kind WHERE {\n" +
				"  {\n" +
				"    BIND(\"knows\" AS ?kind)\n" +
				"    ?s foaf:knows ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    BIND(\"knows2\" AS ?kind)\n" +
				"    ?s foaf:knows/foaf:knows ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    BIND(\"alt\" AS ?kind)\n" +
				"    ?s (foaf:knows|ex:knows) ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    BIND(\"inv\" AS ?kind)\n" +
				"    ?s ^foaf:knows ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    BIND(\"nps\" AS ?kind)\n" +
				"    ?o !(ex:age|rdf:type) ?s .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    BIND(\"zeroOrOne\" AS ?kind)\n" +
				"    ?s (foaf:knows)? ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    BIND(\"zeroOrMore\" AS ?kind)\n" +
				"    ?s foaf:knows* ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    BIND(\"oneOrMore\" AS ?kind)\n" +
				"    ?s foaf:knows+ ?o .\n" +
				"  }\n" +
				"}\n" +
				"ORDER BY ?kind\n" +
				"LIMIT 1000";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void mega_wide_values_matrix_typed_and_undef() {
		String q = "SELECT ?s ?p ?o ?tag ?n (IF(BOUND(?o), STRLEN(STR(?o)), -1) AS ?len) WHERE {\n" +
				"  VALUES (?s ?p ?o ?tag ?n) {\n" +
				"    (ex:a foaf:name \"Ann\"@en \"A\" 1)\n" +
				"    (ex:b foaf:name \"Böb\"@de \"B\" 2)\n" +
				"    (ex:c foaf:name \"Carol\"@en-US \"C\" 3)\n" +
				"    (ex:d ex:age 42 \"D\" 4)\n" +
				"    (ex:e ex:age 3.14 \"E\" 5)\n" +
				"    (ex:f foaf:name \"Δημήτρης\"@el \"F\" 6)\n" +
				"    (ex:g foaf:name \"Иван\"@ru \"G\" 7)\n" +
				"    (ex:h foaf:name \"李\"@zh \"H\" 8)\n" +
				"    (ex:i foaf:name \"علي\"@ar \"I\" 9)\n" +
				"    (ex:j foaf:name \"Renée\"@fr \"J\" 10)\n" +
				"    (UNDEF ex:age UNDEF \"U\" UNDEF)\n" +
				"    (ex:k foaf:name \"multi\\nline\" \"M\" 11)\n" +
				"    (ex:l foaf:name \"quote\\\"test\" \"Q\" 12)\n" +
				"    (ex:m foaf:name \"smile\uD83D\uDE42\" \"S\" 13)\n" +
				"    (ex:n foaf:name \"emoji\uD83D\uDE00\" \"E\" 14)\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    ?s ?p ?o .\n" +
				"  }\n" +
				"}\n" +
				"ORDER BY ?tag ?n\n" +
				"LIMIT 500";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void mega_parentheses_precedence() {
		String q = "SELECT ?s ?o (?score AS ?score2) WHERE {\n" +
				"  ?s foaf:knows/((^foaf:knows)|ex:knows) ?o .\n" +
				"  BIND(((IF(BOUND(?o), 1, 0) + 0) * 1) AS ?score)\n" +
				"  FILTER ((BOUND(?s) && BOUND(?o)) && REGEX(STR(?o), \"^.+$\", \"i\"))\n" +
				"}\n" +
				"ORDER BY ?score\n" +
				"LIMIT 100";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// ==========================
	// ===== New unit tests =====
	// ==========================

	@Test
	void filter_before_trailing_subselect_movable() {
		String q = "SELECT ?s WHERE {\n" +
				"  ?s a foaf:Person .\n" +
				"  FILTER (BOUND(?s))\n" +
				"  {\n" +
				"    SELECT ?x\n" +
				"    WHERE {\n" +
				"      ?x a ex:Thing .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void filter_after_trailing_subselect_depends_on_subselect() {
		String q = "SELECT ?x WHERE {\n" +
				"  ?s a foaf:Person .\n" +
				"  {\n" +
				"    SELECT ?x\n" +
				"    WHERE {\n" +
				"      ?x a ex:Thing .\n" +
				"    }\n" +
				"  }\n" +
				"  FILTER (?x = ?x)\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void graph_optional_merge_plain_body_expected_shape() {
		String q = "SELECT ?g ?s ?label WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?s a foaf:Person .\n" +
				"    OPTIONAL {\n" +
				"      ?s rdfs:label ?label .\n" +
				"    }\n" +
				"    FILTER (LANGMATCHES(LANG(?label), \"en\"))\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void graph_optional_inner_graph_same_expected_shape() {
		String q = "SELECT ?g ?s ?label WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?s a foaf:Person .\n" +
				"    OPTIONAL {\n" +
				"      ?s rdfs:label ?label .\n" +
				"    }\n" +
				"    FILTER (LANGMATCHES(LANG(?label), \"en\"))\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void graph_optional_inner_graph_mismatch_no_merge_expected_shape() {
		String q = "SELECT ?g ?h ?s ?label WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?s a foaf:Person .\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    GRAPH ?h {\n" +
				"      ?s rdfs:label ?label .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void values_empty_parentheses_rows() {
		String q = "SELECT ?s WHERE {\n" +
				"  VALUES () {\n" +
				"    ()\n" +
				"    ()\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void function_fallback_decimal_prefix_compaction() {
		String q = "SELECT (?cnt AS ?c) (xsd:decimal(?cnt) AS ?d) WHERE {\n" +
				"  VALUES (?cnt) {\n" +
				"    (1)\n" +
				"    (2)\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void function_fallback_unknown_prefixed_kept() {
		String q = "SELECT (ex:score(?x, ?y) AS ?s) WHERE {\n" +
				"  ?x ex:knows ?y .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void inverse_triple_heuristic_print_caret() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ^ex:knows ?o .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void property_list_with_a_and_multiple_preds() {
		String q = "SELECT ?s ?name ?age WHERE {\n" +
				"  ?s a ex:Person ; foaf:name ?name ; ex:age ?age .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void union_branches_to_path_alternation() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s foaf:knows|ex:knows ?o .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nps_via_not_in() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  FILTER (?p NOT IN (rdf:type, ex:age))\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nps_via_inequalities() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  FILTER (?p NOT IN (rdf:type, ex:age))\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void service_silent_block_layout() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  SERVICE SILENT ?svc {\n" +
				"    ?s ?p ?o .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void ask_basic_bgp() {
		String q = "ASK WHERE {\n" +
				"  ?s a foaf:Person .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void order_by_mixed_vars_and_exprs() {
		String q = "SELECT ?x ?name WHERE {\n" +
				"  ?x foaf:name ?name .\n" +
				"}\n" +
				"ORDER BY ?x DESC(?name)";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void graph_merge_with_following_filter_inside_group() {
		String q = "SELECT ?g ?s ?label WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?s a foaf:Person .\n" +
				"    OPTIONAL {\n" +
				"      ?s rdfs:label ?label .\n" +
				"    }\n" +
				"    FILTER (STRLEN(STR(?label)) >= 0)\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void values_with_undef_mixed() {
		String q = "SELECT ?s ?p ?o WHERE {\n" +
				"  VALUES (?s ?p ?o) {\n" +
				"    (ex:a ex:age 42)\n" +
				"    (UNDEF ex:age UNDEF)\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void optional_outside_graph_when_complex_body() {
		String q = "SELECT ?g ?s ?label ?nick WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?s a foaf:Person .\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    ?s rdfs:label ?label .\n" +
				"    FILTER (?label != \"\")\n" +
				"    OPTIONAL {\n" +
				"      ?s foaf:nick ?nick .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// -----------------------------
	// Deeply nested path scenarios
	// -----------------------------

	@Test
	void deep_path_in_optional_in_graph() {
		String q = "SELECT ?g ?s ?o WHERE {\n" +
				"  OPTIONAL {\n" +
				"    GRAPH ?g {\n" +
				"      ?s foaf:knows/(^foaf:knows|ex:knows)* ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_path_in_minus() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s a ex:Person .\n" +
				"  MINUS {\n" +
				"    ?s foaf:knows/foaf:knows? ?o .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void pathExample() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s a ex:Person .\n" +
				"  MINUS {\n" +
				"    ?s foaf:knows/foaf:knows? ?o .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_path_in_filter_not_exists() {
		String q = "SELECT ?s WHERE {\n" +
				"  FILTER (NOT EXISTS { ?s (foaf:knows|ex:knows)/^foaf:knows ?o . })\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_path_in_union_branch_with_graph() {
		String q = "SELECT ?g ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH ?g {\n" +
				"      ?s (foaf:knows|ex:knows)* ?o .\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s ^ex:knows ?o .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void zero_or_more_then_inverse_then_alt_in_graph() {
		String q = "SELECT ?g ?s ?o WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?s (foaf:knows*/^(foaf:knows|ex:knows)) ?o .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void optional_with_values_and_bind_inside_graph() {
		String q = "SELECT ?g ?s ?n ?name WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    OPTIONAL {\n" +
				"      VALUES (?s ?n) { (ex:a 1) (ex:b 2) }\n" +
				"      BIND(STR(?n) AS ?name)\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void exists_with_path_and_aggregate_in_subselect() {
		String q = "SELECT ?s WHERE {\n" +
				"  FILTER (EXISTS { { SELECT (COUNT(?x) AS ?c) WHERE { ?s foaf:knows+ ?x . } } FILTER (?c >= 0) })\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_union_optional_with_path_and_filter() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    OPTIONAL { ?s foaf:knows/foaf:knows ?o . FILTER (BOUND(?o)) }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s (ex:knows|foaf:knows)+ ?o .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void minus_with_graph_and_optional_path() {
		String q = "SELECT ?s WHERE {\n" +
				"  MINUS {\n" +
				"    OPTIONAL {\n" +
				"      ?s foaf:knows?/^ex:knows ?o . \n" +
				"    } \n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void service_with_graph_and_path() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  SERVICE ?svc { GRAPH ?g { ?s (foaf:knows|ex:knows) ?o . } }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void group_by_filter_with_path_in_where() {
		String q = "SELECT ?s (COUNT(?o) AS ?c) WHERE {\n" +
				"  ?s foaf:knows/foaf:knows? ?o .\n" +
				"  FILTER (?c >= 0)\n" +
				"}\n" +
				"GROUP BY ?s";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_subselect_with_path_and_order() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s foaf:knows+ ?o .\n" +
				"}\n" +
				"ORDER BY ?o";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void optional_chain_then_graph_path() {
		String q = "SELECT ?g ?s ?o WHERE {\n" +
				"  OPTIONAL {\n" +
				"    ?s foaf:knows ?mid .\n" +
				"    OPTIONAL {\n" +
				"      ?mid foaf:knows ?o .\n" +
				"    }\n" +
				"  }\n" +
				"  GRAPH ?g {\n" +
				"    ?s ex:knows/^foaf:knows ?o .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void values_then_graph_then_minus_with_path() {
		String q = "SELECT ?g ?s ?o WHERE {\n" +
				"  VALUES (?g) { (ex:g1) (ex:g2) }\n" +
				"  GRAPH ?g { ?s foaf:knows ?o . }\n" +
				"  MINUS { ?s (ex:knows|foaf:knows) ?o . }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nps_path_followed_by_constant_step_in_graph() {
		String q = "SELECT ?s ?x WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?s !(ex:age|rdf:type)/foaf:name ?x .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_nested_union_optional_minus_mix_with_paths() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    OPTIONAL {\n" +
				"      ?s foaf:knows/foaf:knows ?o .\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    MINUS {\n" +
				"      ?s (ex:knows/foaf:knows)? ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_exists_with_path_and_inner_filter() {
		String q = "SELECT ?s WHERE {\n" +
				"  FILTER (EXISTS { ?s foaf:knows+/^ex:knows ?o . FILTER (BOUND(?o)) })\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_zero_or_one_path_in_union() {
		String q = "SELECT ?o ?s WHERE {\n" +
				"  {\n" +
				"    ?s foaf:knows? ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s ex:knows? ?o .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_path_chain_with_graph_and_filter() {
		String q = "SELECT ?g ?s ?o WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?s (foaf:knows)/(((^ex:knows)|^foaf:knows)) ?o .\n" +
				"  }\n" +
				"  FILTER (BOUND(?s) && BOUND(?o))\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void mega_ask_deep_exists_notexists_filters() {
		String q = "ASK WHERE {\n" +
				"  { ?a foaf:knows ?b } UNION { ?b foaf:knows ?a }\n" +
				"  FILTER (EXISTS { ?a foaf:name ?n . FILTER (REGEX(?n, \"^A\", \"i\")) })\n" +
				"  FILTER (NOT EXISTS { ?a ex:blockedBy ?b . })" +
				"  GRAPH ?g { ?a !(rdf:type|ex:age)/foaf:name ?x }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void mega_ask_deep_exists_notexists_filters2() {
		String q = "ASK WHERE {\n" +
				"  {\n" +
				"    ?a foaf:knows ?b .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?b foaf:knows ?a .\n" +
				"  }\n" +
				"  FILTER (EXISTS {\n" +
				"    ?a foaf:name ?n .\n" +
				"    FILTER (REGEX(?n, \"^A\", \"i\"))\n" +
				"  })\n" +
				"  FILTER (NOT EXISTS {\n" +
				"    ?a ex:blockedBy ?b .\n" +
				"  })\n" +
				"  GRAPH ?g {\n" +
				"    ?a !(ex:age|rdf:type)/foaf:name ?x .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void path_in_graph() {
		String q = "SELECT ?g ?a ?x WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?a !(ex:age|rdf:type)/foaf:name ?x .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nps_fusion_graph_filter_graph_not_in_forward() {
		String expanded = "SELECT ?g ?a ?x WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?a ?p ?m .\n" +
				"  }\n" +
				"  FILTER (?p NOT IN (rdf:type, ex:age))\n" +
				"  GRAPH ?g {\n" +
				"    ?m foaf:name ?x .\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(expanded, cfg(), false);

	}

	@Test
	void nps_fusion_graph_filter_graph_ineq_chain_inverse() {
		String expanded = "SELECT ?g ?a ?x WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?a ?p ?m .\n" +
				"  }\n" +
				"  FILTER ((?p != rdf:type) && (?p != ex:age))\n" +
				"  GRAPH ?g {\n" +
				"    ?x foaf:name ?m .\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(expanded, cfg(), false);
	}

	@Test
	void nps_fusion_graph_filter_only() {
		String expanded = "SELECT ?g ?a ?m WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?a ?p ?m .\n" +
				"  }\n" +
				"  FILTER (?p NOT IN (rdf:type, ex:age))\n" +
				"}";

		assertSameSparqlQuery(expanded, cfg(), false);

	}

	@Test
	void nps_fusion_graph_filter_only2() {
		String expanded = "SELECT ?g ?a ?m ?n WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?a !(ex:age|^rdf:type) ?m .\n" +
				"    ?a !(^ex:age|rdf:type) ?n .\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(expanded, cfg(), false);

	}

	@Test
	void mega_service_graph_interleaved_with_subselects() {
		String q = "SELECT ?s ?g (SUM(?c) AS ?total) WHERE {\n" +
				"  VALUES (?svc) {\n" +
				"    (<http://example.org/sparql>)\n" +
				"  }\n" +
				"  SERVICE ?svc {\n" +
				"    {\n" +
				"      SELECT ?s (COUNT(?p) AS ?c)\n" +
				"      WHERE {\n" +
				"        GRAPH ?g {\n" +
				"          ?s ?p ?o .\n" +
				"        }\n" +
				"        FILTER (?p NOT IN (rdf:type, ex:type))\n" +
				"      }\n" +
				"      GROUP BY ?s\n" +
				"    }\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    ?s foaf:name ?n .\n" +
				"    FILTER (LANGMATCHES(LANG(?n), \"en\"))\n" +
				"  }\n" +
				"  MINUS {\n" +
				"    ?s a ex:Robot .\n" +
				"  }\n" +
				"}\n" +
				"GROUP BY ?s ?g\n" +
				"HAVING (SUM(?c) >= 0)\n" +
				"ORDER BY DESC(?total) LCASE(COALESCE(?n, \"\"))\n" +
				"LIMIT 25";
		assertSameSparqlQuery(q, cfg(), false);
	}

//	@Test
//	void mega_long_string_literals_and_escaping() {
//		String q = "SELECT ?txt ?repl WHERE {\n" +
//				"  BIND(\"\"\"Line1\\nLine2 \\\"quotes\\\" and backslash \\\\ and \\t tab and unicode \\u03B1 \\U0001F642\"\"\" AS ?txt)\n"
//				+
//				"  BIND(REPLACE(?txt, \"Line\", \"Ln\") AS ?repl)\n" +
//				"  FILTER(REGEX(?txt, \"Line\", \"im\"))\n" +
//				"}";
//		assertSameSparqlQuery(q, cfg());
//	}

	@Test
	void mega_order_by_on_expression_over_aliases() {
		String q = "SELECT ?s ?bestName ?avgAge WHERE {\n" +
				"  {\n" +
				"    SELECT ?s (MIN(?n) AS ?bestName) (AVG(?age) AS ?avgAge)\n" +
				"    WHERE {\n" +
				"      ?s foaf:name ?n .\n" +
				"      OPTIONAL {\n" +
				"        ?s ex:age ?age .\n" +
				"      }\n" +
				"    }\n" +
				"    GROUP BY ?s\n" +
				"  }\n" +
				"  FILTER (BOUND(?bestName))\n" +
				"}\n" +
				"ORDER BY DESC(COALESCE(?avgAge, -999)) LCASE(?bestName)\n" +
				"LIMIT 200";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void mega_optional_minus_nested() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  OPTIONAL {\n" +
				"    ?s foaf:knows ?k .\n" +
				"    OPTIONAL {\n" +
				"      ?k foaf:name ?kn .\n" +
				"      MINUS {\n" +
				"        ?k ex:blockedBy ?s .\n" +
				"      }\n" +
				"      FILTER (!(BOUND(?kn)) || (STRLEN(?kn) >= 0))\n" +
				"    }\n" +
				"  }\n" +
				"  FILTER ((?s IN (ex:a, ex:b, ex:c)) || EXISTS { ?s foaf:name ?nn . })\n" +
				"}\n" +
				"ORDER BY ?s ?o";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void mega_scoped_variables_and_aliasing_across_subqueries() {
		String q = "SELECT ?s ?bestName ?deg WHERE {\n" +
				"  {\n" +
				"    SELECT ?s (MIN(?n) AS ?bestName)\n" +
				"    WHERE {\n" +
				"      ?s foaf:name ?n .\n" +
				"    }\n" +
				"    GROUP BY ?s\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    {\n" +
				"      SELECT ?s (COUNT(?o) AS ?deg)\n" +
				"      WHERE {\n" +
				"        ?s foaf:knows ?o .\n" +
				"      }\n" +
				"      GROUP BY ?s\n" +
				"    }\n" +
				"  }\n" +
				"  FILTER (BOUND(?bestName))\n" +
				"}\n" +
				"ORDER BY ?bestName ?s";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void mega_type_shorthand_and_mixed_sugar() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  ?s a foaf:Person ; foaf:name ?n .\n" +
				"  [] foaf:knows ?s .\n" +
				"  (ex:alice ex:bob ex:carol) rdf:rest*/rdf:first ?x .\n" +
				"  FILTER (STRLEN(?n) > 0)\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void mega_exists_union_inside_exists_and_notexists() {
		String q = "SELECT ?s WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  FILTER EXISTS {\n" +
				"    {\n" +
				"      ?s foaf:knows ?t .\n" +
				"    } \n" +
				"      UNION\n" +
				"    {\n" +
				"      ?t foaf:knows ?s .\n" +
				"    } \n" +
				"\n" +
				"    FILTER NOT EXISTS {\n" +
				"      ?t ex:blockedBy ?s . \n" +
				"    } \n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// -------- New deep nested OPTIONAL path tests --------

	@Test
	void deep_optional_path_1() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  OPTIONAL {\n" +
				"    OPTIONAL {\n" +
				"      OPTIONAL {\n" +
				"        ?s (^foaf:knows)/(foaf:knows|ex:knows)/foaf:name ?n .\n" +
				"        FILTER (LANGMATCHES(LANG(?n), \"en\"))\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_optional_path_2() {
		String q = "SELECT ?x ?y WHERE {\n" +
				"  OPTIONAL {\n" +
				"    ?x ^foaf:knows|ex:knows/^foaf:knows ?y .\n" +
				"    FILTER (?x != ?y)\n" +
				"    OPTIONAL {\n" +
				"      ?y (foaf:knows|ex:knows)/foaf:knows ?x .\n" +
				"      FILTER (BOUND(?x))\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_optional_path_3() {
		String q = "SELECT ?a ?n WHERE {\n" +
				"  OPTIONAL {\n" +
				"    ?a (^foaf:knows/!(ex:helps|ex:knows|rdf:subject|rdf:type)/foaf:name) ?n .\n" +
				"    FILTER ((LANG(?n) = \"\") || LANGMATCHES(LANG(?n), \"en\"))\n" +
				"    OPTIONAL {\n" +
				"      ?a foaf:knows+ ?anon1 .\n" +
				"      FILTER (BOUND(?anon1))\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_optional_path_4() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  OPTIONAL {\n" +
				"    OPTIONAL {\n" +
				"      ?s (foaf:knows/foaf:knows|ex:knows/^ex:knows) ?o .\n" +
				"      FILTER (?s != ?o)\n" +
				"    }\n" +
				"    FILTER (BOUND(?s))\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_optional_path_5() {
		String q = "SELECT ?g ?s ?n WHERE {\n" +
				"  OPTIONAL {\n" +
				"    OPTIONAL {\n" +
				"      ?s (foaf:knows|ex:knows)/^foaf:knows/(foaf:name|^foaf:name) ?n .\n" +
				"      FILTER (STRLEN(STR(?n)) >= 0)\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void complexPath() {
		String q = "SELECT ?g ?s ?n WHERE {\n" +
				"      ?s ex:path1/ex:path2/(ex:alt1|ex:alt2) ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void complexPathUnionOptionalScope() {
		String q = "SELECT ?g ?s ?n WHERE {\n" +
				"  {\n" +
				"    ?s ex:path1/ex:path2 ?o .\n" +
				"    OPTIONAL {\n" +
				"      ?s (ex:alt1|ex:alt2) ?n .\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s ex:path1/ex:path2 ?o .\n" +
				"    OPTIONAL {\n" +
				"      ?s (ex:alt3|ex:alt4) ?n .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	// -------- New deep nested UNION path tests --------

	@Test
	void deep_union_path_1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s (foaf:knows|ex:knows)/^foaf:knows ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s ^foaf:knows/((foaf:knows|ex:knows)) ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    OPTIONAL {\n" +
				"      ?s foaf:knows ?x .\n" +
				"      ?x foaf:name ?_n .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_union_path_2() {
		String q = "SELECT ?a ?n WHERE {\n" +
				"  {\n" +
				"    ?a ^foaf:knows/foaf:knows/foaf:name ?n .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    {\n" +
				"      ?a foaf:knows|ex:knows ?_x .\n" +
				"    }\n" +
				"      UNION\n" +
				"    {\n" +
				"      ?a foaf:knows ?_x  .\n" +
				"    }\n" +
				"    OPTIONAL {\n" +
				"      ?_x foaf:name ?n .\n" +
				"    }\n" +
				"  }\n" +
				"}\n";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_union_path_3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    {\n" +
				"      ?s foaf:knows/foaf:knows ?o .\n" +
				"    }\n" +
				"      UNION\n" +
				"    {\n" +
				"      ?s (ex:knows1|^ex:knows2) ?o .\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    {\n" +
				"      ?s ^foaf:knows ?o .\n" +
				"    }\n" +
				"      UNION\n" +
				"    {\n" +
				"      ?o !(ex:age|rdf:type) ?s .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void simpleOrInversePath() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s (ex:knows1|^ex:knows2) ?o . " +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void simpleOrInversePathGraph() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  GRAPH ?g { ?s (ex:knows1|^ex:knows2) ?o . }" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void simpleOrNonInversePath() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s (ex:knows1|ex:knows2) ?o . " +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_union_path_4() {
		String q = "SELECT ?g ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s (foaf:knows|ex:knows)/^foaf:knows ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    OPTIONAL {\n" +
				"      ?s foaf:knows+ ?o .\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    OPTIONAL {\n" +
				"      ?s !(ex:age|rdf:type)/foaf:name ?_n .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_union_path_5() {
		String q = "SELECT ?o ?s WHERE {\n" +
				"  {\n" +
				"    {\n" +
				"      ?s foaf:knows/foaf:knows|ex:knows/^ex:knows ?o .\n" +
				"    }\n" +
				"      UNION\n" +
				"    {\n" +
				"      ?s ^foaf:knows/(foaf:knows|ex:knows) ?o .\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    {\n" +
				"      ?o !(ex:age|rdf:type) ?s .\n" +
				"    }\n" +
				"      UNION\n" +
				"    {\n" +
				"      ?s foaf:knows? ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void deep_union_path_5_curly_braces() {
		String q = "SELECT ?o ?s WHERE {\n" +
				"  {\n" +
				"    {\n" +
				"      ?s foaf:knows/foaf:knows|ex:knows/^ex:knows ?o .\n" +
				"    }\n" +
				"    UNION\n" +
				"    {\n" +
				"      ?s ^foaf:knows/(foaf:knows|ex:knows) ?o .\n" +
				"    }\n" +
				"  }\n" +
				"  UNION\n" +
				"  {\n" +
				"    {\n" +
				"      ?o !(ex:age|rdf:type) ?s .\n" +
				"    }\n" +
				"    UNION\n" +
				"    {\n" +
				"      ?s foaf:knows? ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), true);
	}

	// -------- Additional SELECT tests with deeper, more nested paths --------

	@Test
	void nested_paths_extreme_1() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  ?s ((foaf:knows/^foaf:knows | !(rdf:type|^rdf:type)/ex:knows?)\n" +
				"      /((ex:colleagueOf|^ex:colleagueOf)/(ex:knows/foaf:knows)?)*\n" +
				"      /(^ex:knows/(ex:knows|^ex:knows)+))/foaf:name ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_1_simple() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  ?s foaf:knows/^foaf:knows | !(rdf:type|^rdf:type)/ex:knows? ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_1_simple2() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  ?s (ex:knows1/ex:knows2)* ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_1_simple2_1() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  ?s (ex:knows1|ex:knows2)* ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_1_simple3() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  ?s (ex:knows1/ex:knows2)+ ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_1_simpleGraph() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?s foaf:knows/^foaf:knows | !(rdf:type|^rdf:type)/ex:knows? ?n .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_2_optional_and_graph() {
		String q = "SELECT ?g ?s ?n WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?s ((ex:p1|^ex:p2)+/(!(^ex:p4|ex:p3))? /((ex:p5|^ex:p6)/(foaf:knows|^foaf:knows))*) ?y .\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"    ?y (^foaf:knows/(ex:p7|^ex:p8)?/((ex:p9/foaf:knows)|(^ex:p10/ex:p11))) ?z .\n" +
				"  }\n" +
				"  ?z foaf:name ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_3_subquery_exists() {
		String q = "SELECT ?s WHERE {\n" +
				"  FILTER (EXISTS {\n" +
				"    {\n" +
				"      SELECT ?s\n" +
				"      WHERE {\n" +
				"        ?s (ex:p1|^ex:p2)/(!(rdf:type|^rdf:type))*/ex:p3? ?o .\n" +
				"      }\n" +
				"    GROUP BY ?s\n" +
				"    HAVING (COUNT(?o) >= 0)\n" +
				"  }\n" +
				"  })\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_4_union_mixed_mods() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  {\n" +
				"    ?s (((ex:a|^ex:b)/(ex:c/foaf:knows)?)*)/(^ex:d/(ex:e|^ex:f)+)/foaf:name ?n .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s (((!(ex:g|^ex:h))/(((ex:i|^ex:j))?))/((ex:k/foaf:knows)|(^ex:l/ex:m)))/foaf:name ?n .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_4_union_mixed_mods2() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  {\n" +
				"    ?s (((ex:a|^ex:b)/(ex:c/foaf:knows)?)*)/(^ex:d/(ex:e|^ex:f)+)/foaf:name ?n .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s (((!(^ex:h|ex:g))/(((ex:i|^ex:j))?))/((ex:k/foaf:knows)|(^ex:l/ex:m)))/foaf:name ?n .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_4_union_mixed_mods3() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  {\n" +
				"    ?s (((ex:a|^ex:b)/(ex:c/foaf:knows)?)*)/(^ex:d/(ex:e|^ex:f)+)/foaf:name ?n .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s (((!(ex:h|^ex:g))/(((ex:i|^ex:j))?))/((ex:k/foaf:knows)|(^ex:l/ex:m)))/foaf:name ?n .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_4_union_mixed_mods4() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  {\n" +
				"    ?s (((ex:a|^ex:b)/(ex:c/foaf:knows)?)*)/(^ex:d/(ex:e|^ex:f)+)/foaf:name ?n .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s (((!(^ex:g|ex:h))/(((ex:i|^ex:j))?))/((ex:k/foaf:knows)|(^ex:l/ex:m)))/foaf:name ?n .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_4_union_mixed_mods5() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  {\n" +
				"    ?s (^ex:g|ex:h)/foaf:name ?n .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s !(^ex:g|ex:h)/foaf:name ?n .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s (^ex:g|ex:h)*/foaf:name ?n .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s (^ex:g|ex:h)+/foaf:name ?n .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_4_union_mixed_mods6() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  ?s !(^ex:g|ex:h)/foaf:name ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nested_paths_extreme_5_grouped_repetition() {
		String q = "SELECT ?s ?n WHERE {\n" +
				"  ?s (((ex:pA|^ex:pB)/(ex:pC|^ex:pD))*/(^ex:pE/(ex:pF|^ex:pG)+)/(ex:pH/foaf:knows)?)/foaf:name ?n .\n"
				+
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void invertedPathInUnion() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s !^<http://example.org/p/I01> ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?o !^<http://example.org/p/I02> ?s .\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void invertedPathInUnion2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { ?s !^<http://example.org/p/I01> ?o . }\n" +
				"  UNION\n" +
				"  { ?s !<http://example.org/p/I02> ?o . }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testNegatedPathUnion() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { ?o !<http://example.org/p/I01> ?s . }\n" +
				"  UNION\n" +
				"  { ?s !<http://example.org/p/I02> ?o . }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void negatedPath() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s !ex:pA ?o .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void negatedInvertedPath() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s !^ex:pA ?o .\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testInvertedPathUnion() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { ?s ^<http://example.org/p/I0> ?o . }\n" +
				"  UNION\n" +
				"  { ?o ^<http://example.org/p/I0> ?s . }\n" +
				"}";
		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testUnionOrdering() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s !(ex:pA|^ex:pB) ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?o !(ex:pC|^ex:pD) ?s .\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testBnodes() {
		String q = "SELECT ?s ?x WHERE {\n" +
				"  [] ex:pA ?s ;\n" +
				"     ex:pB [ ex:pC ?x ] .\n" +
				"  ?s ex:pD (ex:Person ex:Thing) .\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testBnodes2() {
		String q = "SELECT ?s ?x WHERE {\n" +
				"  _:bnode1 ex:pA ?s ;\n" +
				"     ex:pB [ ex:pC ?x ] .\n" +
				"  ?s ex:pD (ex:Person ex:Thing) .\n" +
				" [] ex:pE _:bnode1 .\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testBnodes3() {
		String q = "SELECT ?s ?x WHERE {\n" +
				"  _:bnode1 ex:pA ?s ;\n" +
				"           ex:pB [\n" +
				"                   ex:pC ?x;\n" +
				"                   ex:pB [ ex:pF _:bnode1 ] \n" +
				"                 ] .\n" +
				"  ?s ex:pD (ex:Person ex:Thing) .\n" +
				"  [] !(ex:pE |^ex:pE) _:bnode1 .\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectDistinct() {
		String q = "SELECT ?s  WHERE {\n" +
				"  { SELECT DISTINCT ?s WHERE { ?s ex:pA ?o } ORDER BY ?s LIMIT 10 }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testPathGraphFilterExists() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  FILTER EXISTS {\n" +
				"    GRAPH <http://graphs.example/g1> {\n" +
				"      ?s !(ex:pA|^ex:pD) ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsForceNewScope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  { FILTER EXISTS {\n" +
				"    GRAPH <http://graphs.example/g1> {\n" +
				"      ?s ?b ?o .\n" +
				"    }\n" +
				"  } }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testPathFilterExistsForceNewScope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pC ?u1 .\n" +
				"    FILTER EXISTS {\n" +
				"      { \n" +
				"        GRAPH <http://graphs.example/g1> {\n" +
				"          ?s !(ex:pA|^ex:pD) ?o . \n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesPathUnionScope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { \n" +
				"    {\n" +
				"      VALUES (?s) {\n" +
				"        (ex:s1)\n" +
				"        (ex:s2)\n" +
				"      }\n" +
				"      ?s !^foaf:knows ?o .\n" +
				"    } \n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?u1 ex:pD ?v1 .\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesPathUnionScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"{\n" +
				"      VALUES (?s) {\n" +
				"        (ex:s1)\n" +
				"        (ex:s2)\n" +
				"      }\n" +
				"      ?o !(foaf:knows) ?s .\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?u1 ex:pD ?v1 .\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	// New tests to validate new-scope behavior and single-predicate inversion

	@Test
	void testValuesPrefersSubjectAndCaretForInverse() {
		// VALUES binds ?s; inverse single predicate should render with caret keeping ?s as subject
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { {\n" +
				"    VALUES (?s) { (ex:s1) }\n" +
				"    ?s !^foaf:knows ?o .\n" +
				"  } }\n" +
				"    UNION\n" +
				"  { ?u1 ex:pD ?v1 . }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesAllowsForwardSwappedVariant() {
		// VALUES binds ?s; swapped forward form should be preserved when written that way
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { {\n" +
				"    VALUES (?s) { (ex:s1) }\n" +
				"    ?o !(foaf:knows) ?s .\n" +
				"  } }\n" +
				"    UNION\n" +
				"  { ?u1 ex:pD ?v1 . }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsPrecedingTripleIsGrouped() {
		// Preceding triple + FILTER EXISTS with inner group must retain grouping braces
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  FILTER EXISTS { { \n" +
				"    ?s ex:pC ?u0 .\n" +
				"    FILTER EXISTS { ?s !(ex:pA|^<http://example.org/p/I0>) ?o . }\n" +
				"  } } \n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsNested() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  FILTER EXISTS {\n" +
				"    {\n" +
				"      ?s ex:pC ?u0 .\n" +
				"      FILTER EXISTS {\n" +
				"        ?s !( ex:pA|^<http://example.org/p/I0>) ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testComplexPath1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  ?s !( ex:pA|^<http://example.org/p/I0>) ?o .\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsNested2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pC ?u1 .\n" +
				"    FILTER EXISTS {\n" +
				"      {\n" +
				"        ?s ex:pC ?u0 .\n" +
				"        FILTER EXISTS {\n" +
				"          ?s !(ex:pA|^<http://example.org/p/I0>) ?o .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsNested2_1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  FILTER EXISTS {\n" +
				"{\n" +
				"      ?s ex:pC ?u0 .\n" +
				"      FILTER EXISTS {\n" +
				"        ?s !(ex:pA|^<http://example.org/p/I0>) ?o .\n" +
				"      }\n" +
				"    }\n" +
				"   }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsNested3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  FILTER EXISTS {\n" +
				"    { \n" +
				"      ?s ex:pC ?u0 .\n" +
				"      {\n" +
				"        FILTER EXISTS {\n" +
				"          ?s !(ex:pA|^<http://example.org/p/I0>) ?o .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  } \n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsNested4() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  FILTER EXISTS {\n" +
				"    ?s ex:pC ?u0 .\n" +
				"    {\n" +
				"      FILTER EXISTS {\n" +
				"        ?s !(ex:pA|^<http://example.org/p/I0>) ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsNested5() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"{\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  FILTER EXISTS {\n" +
				"    { \n" +
				"      ?s ex:pC ?u0 .\n" +
				"      {\n" +
				"        FILTER(?s != ?u1) " +
				"      }\n" +
				"    }\n" +
				"  } \n" +
				"}\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testNestedSelect() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      { \n" +
				"        SELECT ?s WHERE {\n" +
				"          ?s !^<http://example.org/p/I2> ?o . \n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testGraphOptionalPath() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH <http://graphs.example/g1> {\n" +
				"      { \n" +
				"        ?s ex:pA ?o . \n" +
				"        OPTIONAL {\n" +
				"          ?s !(ex:pA|foaf:knows) ?o .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void scopeMinusTest() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    {\n" +
				"      ?s ex:pB ?v0 .\n" +
				"      MINUS {\n" +
				"        ?s foaf:knows ?o . \n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testPathUnionAndServiceAndScope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      {\n" +
				"        ?s ^ex:pD ?o . \n" +
				"      }\n" +
				"        UNION\n" +
				"      {\n" +
				"        ?u0 ex:pD ?v0 .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testPathUnionAndServiceAndScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      {\n" +
				"        {\n" +
				"          ?s ^ex:pD ?o . \n" +
				"        }\n" +
				"          UNION\n" +
				"        {\n" +
				"          ?u0 ex:pD ?v0 .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testOptionalServicePathScope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pA ?o . \n" +
				"    OPTIONAL {\n" +
				"      SERVICE SILENT <http://services.example/sparql> {\n" +
				"        ?s !(ex:pA|^<http://example.org/p/I0>) ?o . \n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testOptionalServicePathScope3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pQ ?ok .\n" +
				"  {\n" +
				"    ?s ex:pA ?o .\n" +
				"    ?s ex:pA ?f .\n" +
				"    OPTIONAL {\n" +
				"      SERVICE SILENT <http://services.example/sparql> {\n" +
				"        ?s !(ex:pA|^<http://example.org/p/I0>) ?o . \n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testOptionalServicePathScope4() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pQ ?ok .\n" +
				"  ?s ex:pA ?o .\n" +
				"  ?s ex:pA ?f .\n" +
				"    OPTIONAL {\n" +
				"      SERVICE SILENT <http://services.example/sparql> {\n" +
				"        ?s !(ex:pA|^<http://example.org/p/I0>) ?o . \n" +
				"      }\n" +
				"    }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testOptionalServicePathScope5() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pQ ?ok .\n" +
				"  ?s ex:pA ?o .\n" +
				"  ?s ex:pA ?f .\n" +
				"  OPTIONAL { {\n" +
				"      ?o ex:pX ?vX . \n" +
				"      SERVICE SILENT <http://services.example/sparql> {\n" +
				"        ?s !(ex:pA|^<http://example.org/p/I0>) ?o . \n" +
				"      }\n" +
				"    } }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testOptionalServicePathScope6() {
		String q = "SELECT ?s ?o WHERE {\n" +
				" ?s ex:pQ ?ok . \n" +
				"  ?s ex:pA ?o . \n" +
				"  ?s ex:pA  ?f. \n" +
				"  OPTIONAL { {\n" +
				"      SERVICE SILENT <http://services.example/sparql> {\n" +
				"        ?s !(ex:pA|^<http://example.org/p/I0>) ?o . \n" +
				"      }\n" +
				"    } }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testOptionalServicePathScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pA ?o . \n" +
				"    OPTIONAL {\n" +
				"      {\n" +
				"        SERVICE SILENT <http://services.example/sparql> {\n" +
				"          ?s !(ex:pA|^<http://example.org/p/I0>) ?o . \n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testOptionalPathScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"{ ?s ex:pA ?o . OPTIONAL { { ?s ^<http://example.org/p/I1> ?o . } } }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesGraph1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  VALUES ?s { ex:s1 ex:s2 }\n" +
				"  {\n" +
				"    GRAPH ?g0 {\n" +
				"      ?s a ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesGraph2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    VALUES ?s { ex:s1 ex:s2 }\n" +
				"    {\n" +
				"      GRAPH ?g0 {\n" +
				"        ?s a ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsGraphScope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pC ?u1 .\n" +
				"    FILTER EXISTS {\n" +
				"      { \n" +
				"        GRAPH <http://graphs.example/g0> {\n" +
				"          ?s !foaf:knows ?o .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsGraphScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pC ?u1 .\n" +
				"    FILTER EXISTS {\n" +
				"      GRAPH <http://graphs.example/g0> {\n" +
				"        ?s !foaf:knows ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsGraphScope3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  FILTER EXISTS {\n" +
				"    { \n" +
				"      GRAPH <http://graphs.example/g0> {\n" +
				"        ?s !foaf:knows ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsGraphScope4() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  FILTER EXISTS {\n" +
				"    {\n" +
				"      GRAPH <http://graphs.example/g0> {\n" +
				"        ?s !foaf:knows ?o .\n" +
				"      }\n" +
				"    }\n" +
				"    GRAPH <http://graphs.example/g0> {\n" +
				"      ?s !foaf:knows2 ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterExistsGraphScope5() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u1 .\n" +
				"  FILTER EXISTS {\n" +
				"    GRAPH <http://graphs.example/g0> {\n" +
				"      {\n" +
				"        ?s !foaf:knows ?o .\n" +
				"      }\n" +
				"    }\n" +
				"    GRAPH <http://graphs.example/g0> {\n" +
				"      ?s !foaf:knows2 ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testNestedGraphScope1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH <http://graphs.example/g0> {\n" +
				"      {\n" +
				"        GRAPH ?g0 {\n" +
				"          ?s !(ex:pA|^<http://example.org/p/I1>) ?o .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testNestedGraphScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH <http://graphs.example/g0> {\n" +
				"      GRAPH ?g0 {\n" +
				"        ?s !(ex:pA|^<http://example.org/p/I1>) ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testNestedGraphScope3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  GRAPH <http://graphs.example/g0> {\n" +
				"    {\n" +
				"      GRAPH ?g0 {\n" +
				"        ?s !(ex:pA|^<http://example.org/p/I1>) ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testGraphValuesPathScope1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH ?g1 {\n" +
				"      {\n" +
				"        VALUES ?s {\n" +
				"          ex:s1 ex:s2 \n" +
				"        }\n" +
				"        ?s !^<http://example.org/p/I0> ?o . \n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testGraphValuesPathScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  GRAPH ?g1 {\n" +
				"    {\n" +
				"      VALUES ?s {\n" +
				"        ex:s1 ex:s2 \n" +
				"      }\n" +
				"      ?s !^<http://example.org/p/I0> ?o . \n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testGraphValuesPathScope3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH ?g1  {\n" +
				"      VALUES ?s {\n" +
				"        ex:s1 ex:s2 \n" +
				"      }\n" +
				"      ?s !^<http://example.org/p/I0> ?o . \n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void bgpScope1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s a ?o .  \n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void bgpScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s a ?o .  \n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectScope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      {\n" +
				"        ?s ^<http://example.org/p/I2> ?o . \n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectScope4() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      ?s ^<http://example.org/p/I2> ?o . \n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  SELECT ?s WHERE {\n" +
				"    {\n" +
				"      ?s ^<http://example.org/p/I2> ?o . \n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectScope3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  SELECT ?s WHERE {\n" +
				"    ?s ^<http://example.org/p/I2> ?o . \n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void filterExistsNestedScopeTest() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  FILTER EXISTS {\n" +
				"    {\n" +
				"      ?s ex:p ?o .\n" +
				"      FILTER EXISTS {\n" +
				"        ?s ex:q ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectGraph() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      {\n" +
				"        GRAPH <http://graphs.example/g1> {\n" +
				"          ?s ^ex:pB ?o . \n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectGraph2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH <http://graphs.example/g1> {\n" +
				"      {\n" +
				"        ?s ex:pC ?u0 . \nFILTER EXISTS {\n" +
				"          ?s !(ex:pB|^ex:pA) ?o . \n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectGraph3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      {\n" +
				"        GRAPH <http://graphs.example/g0> {\n" +
				"          ?s <http://example.org/p/I0> ?o . \n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void scopeGraphFilterExistsPathTest() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH <http://graphs.example/g0> {\n" +
				"      {\n" +
				"        ?s ex:pC ?u0 . \nFILTER EXISTS {\n" +
				"          ?s ^ex:pC ?o . \n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedServiceGraphPath() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      ?s !(ex:pA|^<http://example.org/p/I1>) ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedServiceGraphPath2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      ?s !(ex:pA|^<http://example.org/p/I1>) ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testServiceValuesPathMinus() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      {\n" +
				"        VALUES ?s {\n" +
				"          ex:s1 ex:s2 \n" +
				"        }\n" +
				"        {\n" +
				"          ?s ex:pB ?v0 . MINUS {\n" +
				"            ?s !(ex:pA|^foaf:knows) ?o . \n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testServiceGraphGraphPath() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      {\n" +
				"        GRAPH <http://graphs.example/g0> {\n" +
				"          {\n" +
				"            GRAPH ?g0 {\n" +
				"              ?s !(ex:pA|^<http://example.org/p/I1>) ?o . \n" +
				"            }\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testServiceGraphGraphPath2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      {\n" +
				"        GRAPH <http://graphs.example/g0> {\n" +
				"          {\n" +
				"            ?s !(ex:pA|^<http://example.org/p/I1>) ?o . \n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectServiceUnionPathTest() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      {\n" +
				"        SERVICE SILENT <http://federation.example/ep> {\n" +
				"          {\n" +
				"            {\n" +
				"              ?s ^ex:pD ?o . \n" +
				"            }\n" +
				"              UNION\n" +
				"            {\n" +
				"              ?u0 ex:pD ?v0 . \n" +
				"            }\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	// ---- Additional generalization tests to ensure robustness of SERVICE + UNION + SUBSELECT grouping ----

	@Test
	void nestedSelectServiceUnionSimpleTriples_bracedUnionInsideService() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      {\n" +
				"        SERVICE SILENT <http://federation.example/ep> {\n" +
				"          {\n" +
				"            { ?s ex:pA ?o . } UNION { ?u0 ex:pA ?v0 . }\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectServiceUnionWithGraphBranches_bracedUnionInsideService() {
		String q = "SELECT ?s WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      {\n" +
				"        SERVICE SILENT <http://federation.example/ep> {\n" +
				"          {\n" +
				"            GRAPH ?g {\n" +
				"              {\n" +
				"                ?s ex:pB ?t . \n" +
				"              }\n" +
				"                UNION\n" +
				"              {\n" +
				"                ?s ex:pC ?t . \n" +
				"              }\n" +
				"            }\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectServiceSinglePath_noExtraUnionGroup() {
		String q = "SELECT ?s WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      SERVICE SILENT <http://federation.example/ep> {\n" +
				"        {\n" +
				"          ?s ex:pZ ?o . \n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void nestedSelectServiceUnionInversePath_bracedUnionInsideService() {
		String q = "SELECT ?s WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      {\n" +
				"        SERVICE SILENT <http://federation.example/ep> {\n" +
				"          {\n" +
				"            {\n" +
				"              ?s ^ex:pD ?o . \n" +
				"            }\n" +
				"              UNION\n" +
				"            {\n" +
				"              ?u0 ex:pD ?v0 . \n" +
				"            }\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void yetAnotherTest() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH <http://graphs.example/g1> {\n" +
				"      {\n" +
				"        ?s ex:pC ?u1 . FILTER EXISTS {\n" +
				"          {\n" +
				"            ?s ex:pA ?o . OPTIONAL {\n" +
				"              ?s !<http://example.org/p/I0> ?o . \n" +
				"            }\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void yetAnotherTest2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  GRAPH <http://graphs.example/g1> {\n" +
				"    ?s ex:pC ?u1 .\n" +
				"    FILTER EXISTS {\n" +
				"      {\n" +
				"        ?s ex:pA ?o .\n" +
				"        OPTIONAL {\n" +
				"          ?s !<http://example.org/p/I0> ?o .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void pathUnionTest1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s !(ex:pA|ex:pB|^ex:pA) ?o . \n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?o !(ex:pA|ex:pB|^ex:pA) ?s . \n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void pathUnionTest2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s !(<http://example.org/p/I0>|ex:pA|^ex:pA) ?o . \n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?o !(<http://example.org/p/I0>|ex:pA|^ex:pA) ?s . \n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void pathUnionTest3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s !(<http://example.org/p/I0>|ex:pA|^ex:pA|ex:Pb|^ex:Pb|ex:Pc|^ex:Pc|ex:Pd|^ex:Pd|ex:Pe|^ex:Pe|ex:Pf|^ex:Pf) ?o . \n"
				+
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s !(<http://example.org/p/I0>|ex:pA|ex:Pb|ex:Pc|ex:Pd|ex:Pe|ex:Pf) ?o . \n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?s !(<http://example.org/p/I0>|ex:pA1|ex:Pb2|ex:Pc3|ex:Pd4|ex:Pe5|ex:Pf6) ?o . \n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void pathUnionTest4() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s !(ex:P1|ex:pA) ?o .\n" +
				"  }\n" +
				"  UNION\n" +
				"  {\n" +
				"    ?s !(ex:P1|ex:pA|ex:pA) ?o .\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testGraphFilterValuesPathAndScoping() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH ?g2 {\n" +
				"      {\n" +
				"        ?s ex:pC ?u1 . FILTER EXISTS {\n" +
				"          {\n" +
				"            VALUES ?s { ex:s1 ex:s2 }\n" +
				"            ?s !( ex:pA|^ex:pC) ?o .\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testScopeGraphUnionUnion() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH <http://graphs.example/g1> {\n" +
				"      {\n" +
				"        ?s !ex:pC ?o .\n" +
				"      }\n" +
				"        UNION\n" +
				"      {\n" +
				"        ?u0 ex:pD ?v0 .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?u2 ex:pD ?v2 .\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testMinusGraphUnion1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pB ?v2 .\n" +
				"    MINUS {\n" +
//				"      {\n" +
				"        {\n" +
//				"          {\n" +
				"            GRAPH <http://graphs.example/g1> {\n" +
				"              ?s !( ex:pA|foaf:name) ?o .\n" +
				"            }\n" +
//				"          }\n" +
				"        }\n" +
				"          UNION\n" +
				"        {\n" +
				"          ?u1 ex:pD ?v1 .\n" +
				"        }\n" +
				"      }\n" +
//				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testMinusGraphUnionScope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pB ?v2 .\n" +
				"    MINUS {\n" +
				"      {\n" +
				"        {\n" +
				"          GRAPH <http://graphs.example/g1> {\n" +
				"            ?s !( ex:pA|foaf:name) ?o .\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"        UNION\n" +
				"      {\n" +
				"        ?u1 ex:pD ?v1 .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterUnionUnionScope1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u2 .\n" +
				"  FILTER EXISTS {\n" +
				"    {\n" +
				"      {\n" +
				"        ?s ^ex:pC ?o .\n" +
				"      }\n" +
				"        UNION\n" +
				"      {\n" +
				"        ?u0 ex:pD ?v0 .\n" +
				"      }\n" +
				"    }\n" +
				"      UNION\n" +
				"    {\n" +
				"      ?u1 ex:pD ?v1 .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterUnionUnionScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pC ?u2 . FILTER EXISTS {\n" +
				"      {\n" +
				"        {\n" +
				"          {\n" +
				"            ?s ^ex:pC ?o .\n" +
				"          }\n" +
				"            UNION\n" +
				"          {\n" +
				"            ?u0 ex:pD ?v0 .\n" +
				"          }\n" +
				"        }\n" +
				"          UNION\n" +
				"        {\n" +
				"          ?u1 ex:pD ?v1 .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterUnionScope1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  ?s ex:pC ?u2 .\n" +
				"  FILTER EXISTS {\n" +
				"    {\n" +
				"      {\n" +
				"        ?s ex:pC ?u0 .\n" +
				"        FILTER EXISTS {\n" +
				"          ?s !(ex:pB|foaf:name) ?o .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"    UNION\n" +
				"    {\n" +
				"      ?u1 ex:pD ?v1 .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterUnionScope2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pC ?u2 .\n" +
				"    FILTER EXISTS {\n" +
				"      {\n" +
				"        {\n" +
				"          ?s ex:pC ?u0 .\n" +
				"          FILTER EXISTS {\n" +
				"            ?s !(ex:pB|foaf:name) ?o .\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"        UNION\n" +
				"      {\n" +
				"        ?u1 ex:pD ?v1 .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterUnionScope3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pC ?u2 .\n" +
				"    FILTER EXISTS {\n" +
//				"      {\n" +
				"        {\n" +
				"          ?s ex:pC ?u0 .\n" +
				"          FILTER EXISTS {\n" +
				"            ?s !(ex:pB|foaf:name) ?o .\n" +
				"          }\n" +
//				"        }\n" +
				"      }\n" +
				"        UNION\n" +
				"      {\n" +
				"        ?u1 ex:pD ?v1 .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterUnionScope4() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    ?s ex:pC ?u2 .\n" +
				"    FILTER EXISTS {\n" +
				"      {\n" +
				"        ?s ex:pC ?u0 .\n" +
				"        FILTER EXISTS {\n" +
				"          {\n" +
				"            ?s !( ex:pB|foaf:name) ?o .\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"        UNION\n" +
				"      {\n" +
				"        ?u1 ex:pD ?v1 .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testFilterUnionScope5() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    {\n" +
				"      ?s ex:pC ?u2 .\n" +
				"      FILTER EXISTS {\n" +
				"        {\n" +
				"          ?s ex:pC ?u0 .\n" +
				"          FILTER EXISTS {\n" +
				"            ?s !(ex:pB|foaf:name) ?o .\n" +
				"          }\n" +
				"        }\n" +
				"        UNION\n" +
				"        {\n" +
				"          ?u1 ex:pD ?v1 .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testNestedGraphScopeUnion() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  GRAPH <http://graphs.example/g1> {\n" +
				"    {\n" +
				"      {\n" +
				"        GRAPH ?g0 {\n" +
				"          ?s ^foaf:name ?o .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"      UNION\n" +
				"    {\n" +
				"      ?u1 ex:pD ?v1 .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testNestedGraphScopeUnion2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  GRAPH <http://graphs.example/g1> {\n" +
				"    {\n" +
//				"      {\n" +
				"        GRAPH ?g0 {\n" +
				"          ?s ^foaf:name ?o .\n" +
				"        }\n" +
//				"      }\n" +
				"    }\n" +
				"      UNION\n" +
				"    {\n" +
				"      ?u1 ex:pD ?v1 .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testNestedGraphScopeUnion3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    {\n" +
				"      GRAPH ?g0 {\n" +
				"        ?o foaf:name ?s .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    GRAPH <http://graphs.example/g1> {\n" +
				"      ?u1 ex:pD ?v1 .\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesGraphUnion() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    {\n" +
				"      GRAPH ?g0 {\n" +
				"        ?s !( ex:pA|^foaf:name) ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?u2 ex:pD ?v2 .\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesGraphUnion2() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    {\n" +
				"      GRAPH ?g0 {\n" +
				"        {\n" +
				"          ?s !ex:pA ?o .\n" +
				"        }\n" +
				"          UNION\n" +
				"        {\n" +
				"          ?o !foaf:name ?s .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?u2 ex:pD ?v2 .\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesGraphUnion3() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    VALUES ?s { ex:s1 ex:s2 }\n" +
				"    {\n" +
				"      GRAPH ?g0 {\n" +
				"        ?s ex:pA|^foaf:name ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?u2 ex:pD ?v2 .\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesGraphUnion4() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    VALUES ?s {\n" +
				"      ex:s1 ex:s2\n" +
				"    }\n" +
				"    {\n" +
				"      GRAPH ?g0 {\n" +
				"        ?s !( ex:pA|^foaf:name|ex:pB) ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?u2 ex:pD ?v2 .\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesGraphUnion5() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    VALUES ?s { ex:s1 ex:s2 }\n" +
				"    {\n" +
				"      GRAPH ?g0 {\n" +
				"        ?s ex:pA|!(foaf:knows|^foaf:name)|ex:pB ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?u2 ex:pD ?v2 .\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesGraphUnion6() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH ?g0 {\n" +
				"      ?s ex:pA|!(foaf:knows|^foaf:name)|ex:pB ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testValuesGraphUnion7() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    GRAPH ?g0 {\n" +
				"      ?s ex:pA|!foaf:knows ?o .\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testGraphUnionScope1() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"    GRAPH <http://graphs.example/g1> {\n" +
				"        {\n" +
				"          {\n" +
				"              ?s <http://example.org/p/I2> ?o .\n" +
				"          }\n" +
				"        }\n" +
				"          UNION\n" +
				"        {\n" +
				"          ?u1 ex:pD ?v1 .\n" +
				"        }\n" +
				"      }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

	@Test
	void testServiceFilterExistsAndScope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  SERVICE SILENT <http://services.example/sparql> {\n" +
				"    {\n" +
				"      ?s ex:pC ?u1 .\n" +
				"      FILTER EXISTS {\n" +
				"        {\n" +
				"          ?s ^ex:pB ?o .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		assertSameSparqlQuery(q, cfg(), false);
	}

}
