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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TupleExprIRRendererTest {

	private static final String EX = "http://ex/";

	private static final String SPARQL_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX ex: <http://ex/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

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

	// ---------- Helpers ----------

	private TupleExpr parseAlgebra(String sparql) {
		try {
			ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, sparql, null);
			return pq.getTupleExpr();
		} catch (MalformedQueryException e) {
			throw new MalformedQueryException("Failed to parse SPARQL query.\n### Original query ###\n" + sparql + "\n",
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
		System.out.println("# Original SPARQL query\n" + sparql + "\n");
		TupleExpr tupleExpr = parseAlgebra(SPARQL_PREFIX + sparql);
		System.out.println("# Original TupleExpr\n" + tupleExpr + "\n");
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

	/** Assert semantic equivalence by comparing result rows (order-insensitive). */
	private void assertSameSparqlQuery(String sparql, TupleExprIRRenderer.Config cfg) {
//		String rendered = assertFixedPoint(original, cfg);
		sparql = sparql.trim();

		TupleExpr tupleExpr = parseAlgebra(SPARQL_PREFIX + sparql);
		String rendered = render(SPARQL_PREFIX + sparql, cfg);

		try {
			assertThat(rendered).isEqualToNormalizingNewlines(SPARQL_PREFIX + sparql);

		} catch (Throwable t) {
			System.out.println("\n\n\n");
			System.out.println("# Original SPARQL query\n" + sparql + "\n");
			System.out.println("# Original TupleExpr\n" + tupleExpr + "\n");

			assertThat(rendered).isEqualToNormalizingNewlines(SPARQL_PREFIX + sparql);

		}
	}

	// ---------- Tests: fixed point + semantic equivalence where applicable ----------

	@Test
	void basic_select_bgp() {
		String q = "SELECT ?s ?name\n" +
				"WHERE {\n" +
				"  ?s a foaf:Person ; foaf:name ?name .\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void filter_compare_and_regex() {
		String q = "SELECT ?s ?name\n" +
				"WHERE {\n" +
				"  ?s foaf:name ?name .\n" +
				"  FILTER ((?name != \"Zed\") && REGEX(?name, \"a\", \"i\"))\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void optional_with_condition() {
		String q = "SELECT ?s ?age\n" +
				"WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"  OPTIONAL {\n" +
				"    ?s ex:age ?age .\n" +
				"    FILTER (?age >= 18)\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void union_of_groups() {
		String q = "SELECT ?who\n" +
				"WHERE {\n" +
				"  {\n" +
				"    ?who foaf:name \"Alice\" .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?who foaf:name \"Bob\" .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void order_by_limit_offset() {
		String q = "SELECT ?name\n" +
				"WHERE {\n" +
				"  ?s foaf:name ?name .\n" +
				"}\n" +
				"ORDER BY DESC(?name)\n" +
				"LIMIT 2\n" +
				"OFFSET 0";
		// Semantic equivalence depends on ordering; still fine since we run the same query
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void values_single_var_and_undef() {
		String q = "SELECT ?x\n" +
				"WHERE {\n" +
				"  VALUES (?x) {\n" +
				"    (ex:alice)\n" +
				"    (UNDEF)\n" +
				"    (ex:bob)\n" +
				"  }\n" +
				"  ?x foaf:name ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void values_multi_column() {
		String q = "SELECT ?s ?n\n" +
				"WHERE {\n" +
				"  VALUES (?n ?s) {\n" +
				"    (\"Alice\" ex:alice)\n" +
				"    (\"Bob\" ex:bob)\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void bind_inside_where() {
		String q = "SELECT ?s ?sn\n" +
				"WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"  BIND(STR(?n) AS ?sn)\n" +
				"  FILTER (STRSTARTS(?sn, \"A\"))\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void aggregates_count_star_and_group_by() {
		String q = "SELECT (COUNT(*) AS ?c)\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}";
		// No dataset dependency issues; simple count
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void aggregates_count_distinct_group_by() {
		String q = "SELECT ?s (COUNT(DISTINCT ?o) AS ?c)\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}\n" +
				"GROUP BY ?s";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void group_concat_with_separator_literal() {
		String q = "SELECT (GROUP_CONCAT(?name; SEPARATOR=\", \") AS ?names)\n" +
				"WHERE {\n" +
				"  ?s foaf:name ?name .\n" +
				"}";
		// Semantic equivalence: both queries run in the same engine; comparing string results
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void service_silent_block() {
		String q = "SELECT ?s ?p ?o\n" +
				"WHERE {\n" +
				"  SERVICE SILENT <http://example.org/sparql> {\n" +
				"    ?s ?p ?o .\n" +
				"  }\n" +
				"}";
		// We do not execute against remote SERVICE; check fixed point only:
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void property_paths_star_plus_question() {
		// These rely on RDF4J producing ArbitraryLengthPath for +/*/?.
		String qStar = "SELECT ?x ?y\n" +
				"WHERE {\n" +
				"  ?x ex:knows*/foaf:name ?y .\n" +
				"}";
		String qPlus = "SELECT ?x ?y\n" +
				"WHERE {\n" +
				"  ?x ex:knows+/foaf:name ?y .\n" +
				"}";
		String qOpt = "SELECT ?x ?y\n" +
				"WHERE {\n" +
				"  ?x ex:knows?/foaf:name ?y .\n" +
				"}";

		assertSameSparqlQuery(qStar, cfg());
		assertSameSparqlQuery(qPlus, cfg());
		assertSameSparqlQuery(qOpt, cfg());
	}

	@Test
	void regex_flags_and_lang_filters() {
		String q = "SELECT ?s ?n\n" +
				"WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"  FILTER (REGEX(?n, \"^a\", \"i\") || LANGMATCHES(LANG(?n), \"en\"))\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void datatype_filter_and_is_tests() {
		String q = "SELECT ?s ?age\n" +
				"WHERE {\n" +
				"  ?s ex:age ?age .\n" +
				"  FILTER ((DATATYPE(?age) = xsd:integer) && isLiteral(?age))\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void distinct_projection_and_reduced_shell() {
		String q = "SELECT DISTINCT ?s\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}\n" +
				"LIMIT 10\n" +
				"OFFSET 1";
		assertSameSparqlQuery(q, cfg());
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
		String q = "SELECT ?a ?b\n" +
				"WHERE {\n" +
				"  VALUES (?a ?b) {\n" +
				"    (\"x\" UNDEF)\n" +
				"    (UNDEF \"y\")\n" +
				"    (\"x\" \"y\")\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void count_and_sum_in_select_with_group_by() {
		String q = "SELECT ?s (COUNT(?o) AS ?c) (SUM(?age) AS ?sumAge)\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void order_by_multiple_keys() {
		String q = "SELECT ?s ?n\n" +
				"WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"}\n" +
				"ORDER BY ?n DESC(?s)";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void list_member_in_and_not_in() {
		String q = "SELECT ?s\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void exists_in_filter_and_bind() {
		String q = "SELECT ?hasX\n" +
				"WHERE {\n" +
				"  OPTIONAL {\n" +
				"    BIND(EXISTS { ?s ?p ?o . } AS ?hasX)\n" +
				"  }\n" +
				"  FILTER (EXISTS { ?s ?p ?o . })\n" +
				"}";
		String r = assertFixedPoint(q, cfg());
		assertTrue(r.contains("EXISTS {"), "should render EXISTS");
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void strlen_alias_for_fn_string_length() {
		String q = "SELECT ?s ?p ?o\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  FILTER (STRLEN(STR(?o)) > 1)\n" +
				"}";
		String r = assertFixedPoint(q, cfg());
		assertTrue(r.contains("STRLEN("), "fn:string-length should render as STRLEN");
		assertSameSparqlQuery(q, cfg());
	}

	// =========================
	// ===== New test cases ====
	// =========================

	// --- Negation: NOT EXISTS & MINUS ---

	@Test
	void filter_not_exists() {
		String q = "SELECT ?s\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  FILTER (NOT EXISTS { ?s foaf:name ?n . })\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void minus_set_difference() {
		String q = "SELECT ?s\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  MINUS {\n" +
				"    ?s foaf:name ?n .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
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
		String q = "SELECT ?x ?y WHERE { ?x !(rdf:type|^rdf:type) ?y }";
		assertFixedPoint(q, cfg());
	}

	@Test
	void property_paths_grouping_precedence() {
		String q = "SELECT ?x ?y WHERE { ?x (ex:knows/ (foaf:knows|^foaf:knows) ) ?y }";
		assertFixedPoint(q, cfg());
	}

	// --- Assignment forms: SELECT (expr AS ?v), GROUP BY (expr AS ?v) ---

	@Test
	void select_projection_expression_alias() {
		String q = "SELECT ((?age + 1) AS ?age1)\n" +
				"WHERE {\n" +
				"  ?s ex:age ?age .\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void group_by_with_alias_and_having() {
		String q = "SELECT ?name (COUNT(?s) AS ?c)\n" +
				"WHERE {\n" +
				"  ?s foaf:name ?n .\n" +
				"  BIND(STR(?n) AS ?name)\n" +
				"}\n" +
				"GROUP BY (?n AS ?name)\n" +
				"HAVING (COUNT(?s) > 1)\n" +
				"ORDER BY DESC(?c)";
		assertFixedPoint(q, cfg());
	}

	// --- Aggregates: MIN/MAX/AVG/SAMPLE + HAVING ---

	@Test
	void aggregates_min_max_avg_sample_having() {
		String q = "SELECT ?s (MIN(?o) AS ?minO) (MAX(?o) AS ?maxO) (AVG(?o) AS ?avgO) (SAMPLE(?o) AS ?anyO)\n" +
				"WHERE { ?s ?p ?o . }\n" +
				"GROUP BY ?s\n" +
				"HAVING (COUNT(?o) >= 1)";
		assertFixedPoint(q, cfg());
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
		assertFixedPoint(q, cfg());
	}

	// --- GRAPH with IRI and variable ---

	@Test
	void graph_iri_and_variable() {
		String q = "SELECT ?g ?s WHERE {\n" +
				"  GRAPH ex:g1 { ?s ?p ?o }\n" +
				"  GRAPH ?g   { ?s ?p ?o }\n" +
				"}";
		assertFixedPoint(q, cfg());
	}

	// --- Federation: SERVICE (no SILENT) and variable endpoint ---

	@Test
	void service_without_silent() {
		String q = "SELECT * WHERE { SERVICE <http://example.org/sparql> { ?s ?p ?o } }";
		assertFixedPoint(q, cfg());
	}

	@Test
	void service_variable_endpoint() {
		String q = "SELECT * WHERE { SERVICE ?svc { ?s ?p ?o } }";
		assertFixedPoint(q, cfg());
	}

	// --- Solution modifiers: REDUCED; ORDER BY expression; OFFSET-only; LIMIT-only ---

	@Test
	void select_reduced_modifier() {
		String q = "SELECT REDUCED ?s\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void order_by_expression_and_by_aggregate_alias() {
		String q = "SELECT ?n (COUNT(?s) AS ?c)\n" +
				"WHERE { ?s foaf:name ?n }\n" +
				"GROUP BY ?n\n" +
				"ORDER BY LCASE(?n) DESC(?c)";
		assertFixedPoint(q, cfg());
	}

	@Test
	void offset_only() {
		String q = "SELECT ?s ?p ?o\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}\n" +
				"OFFSET 5";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void limit_only_zero_and_positive() {
		String q1 = "SELECT ?s ?p ?o\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}\n" +
				"LIMIT 0";
		String q2 = "SELECT ?s ?p ?o\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"}\n" +
				"LIMIT 3";
		assertSameSparqlQuery(q1, cfg());
		assertSameSparqlQuery(q2, cfg());
	}

	@Test
	void construct_query() {
		String q = "CONSTRUCT { ?s ?p ?o }\n" +
				"WHERE     { ?s ?p ?o }";
		assertFixedPoint(q, cfg());
	}

	// --- Expressions & built-ins ---

	@Test
	void functional_forms_and_rdf_term_tests() {
		String q = "SELECT ?ok1 ?ok2 ?ok3 ?ok4\n" +
				"WHERE {\n" +
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
		assertFixedPoint(q, cfg());
	}

	@Test
	void string_functions_concat_substr_replace_encode() {
		String q = "SELECT ?a ?b ?c ?d\n" +
				"WHERE {\n" +
				"  VALUES (?n) { (\"Alice\") }\n" +
				"  BIND(CONCAT(?n, \" \", \"Doe\") AS ?a)\n" +
				"  BIND(SUBSTR(?n, 2) AS ?b)\n" +
				"  BIND(REPLACE(?n, \"A\", \"a\") AS ?c)\n" +
				"  BIND(ENCODE_FOR_URI(?n) AS ?d)\n" +
				"}";
		assertFixedPoint(q, cfg());
	}

	@Test
	void numeric_datetime_hash_and_random() {
		String q = "SELECT ?r ?now ?y ?tz ?abs ?ceil ?floor ?round ?md5\n" +
				"WHERE {\n" +
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
		assertFixedPoint(q, cfg());
	}

	@Test
	void uuid_and_struuid() {
		String q = "SELECT (UUID() AS ?u) (STRUUID() AS ?su)\n" +
				"WHERE {\n" +
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
		String q = "SELECT ?s\n" +
				"WHERE {\n" +
				"  VALUES (?s) {\n" +
				"    (ex:alice)\n" +
				"    (ex:bob)\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void values_empty_block() {
		String q = "SELECT ?s\n" +
				"WHERE {\n" +
				"  VALUES (?s) {\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	// --- Syntactic sugar: blank node property list and collections ---

	@Test
	void blank_node_property_list() {
		String q = "SELECT ?n\n" +
				"WHERE {\n" +
				"  [] foaf:name ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void collections() {
		String q = "SELECT ?el\n" +
				"WHERE {\n" +
				"  (1 2 3) rdf:rest*/rdf:first ?el .\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	// ==========================================
	// ===== Complex integration-style tests ====
	// ==========================================

	@Test
	@Disabled
	void complex_kitchen_sink_paths_graphs_subqueries() {
		String q = "SELECT REDUCED ?g ?y (?cnt AS ?count) (COALESCE(?avgAge, -1) AS ?ageOrMinus1)\n" +
				"WHERE {\n" +
				"  VALUES (?g) {\n" +
				"  (ex:g1)\n" +
				"  (ex:g2)\n" +
				"  }\n" +
				"  GRAPH ?g {\n" +
				"    ?x (foaf:knows|ex:knows)/^foaf:knows ?y .\n" +
				"    ?y foaf:name ?name .\n" +
				"  }\n" +
				"  OPTIONAL {\n" +
				"  GRAPH ?g {\n" +
				"    ?y ex:age ?age .\n" +
				"  }\n" +
				"  FILTER (?age >= 21)\n" +
				"  }\n" +
				"  MINUS {\n" +
				"   ?y a ex:Robot }\n" +
				"  FILTER (NOT EXISTS { ?y foaf:nick ?nick FILTER(STRLEN(?nick) > 0) })\n" +
				"  {\n" +
				"    SELECT ?y (COUNT(DISTINCT ?name) AS ?cnt) (AVG(?age) AS ?avgAge)\n" +
				"    WHERE {\n" +
				"      ?y foaf:name ?name .\n" +
				"      OPTIONAL { ?y ex:age ?age }\n" +
				"    }\n" +
				"    GROUP BY ?y\n" +
				"  }\n" +
				"}\n" +
				"ORDER BY DESC(?cnt) LCASE(?name)\n" +
				"LIMIT 10\n" +
				"OFFSET 5";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void testMoreGraph1() {
		String q = "SELECT REDUCED ?g ?y (?cnt AS ?count) (COALESCE(?avgAge, -1) AS ?ageOrMinus1)\n" +
				"WHERE {\n" +
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
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void testMoreGraph2() {
		String q = "SELECT REDUCED ?g ?y (?cnt AS ?count) (COALESCE(?avgAge, -1) AS ?ageOrMinus1)\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void morePathInGraph() {
		String q = "SELECT REDUCED ?g ?y (?cnt AS ?count) (COALESCE(?avgAge, -1) AS ?ageOrMinus1)\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void complex_deep_union_optional_with_grouping() {
		String q = "SELECT ?s ?label ?src (SUM(?innerC) AS ?c)\n" +
				"WHERE {\n" +
				"  VALUES (?src) {\n" +
				"    (\"A\")\n" +
				"    (\"B\")\n" +
				"  }\n" +
				"  {\n" +
				"    ?s a foaf:Person .\n" +
				"    OPTIONAL {\n" +
				"      ?s rdfs:label ?label .\n" +
				"      FILTER (LANGMATCHES(LANG(?label), \"en\"))\n" +
				"    }\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    ?_anon_1 foaf:name ?label .\n" +
				"    BIND(\"B\" AS ?src)\n" +
				"    BIND(BNODE() AS ?s)\n" +
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
				"ORDER BY DESC(?c) STRLEN(COALESCE(?label, \"\"))\n" +
				"LIMIT 20";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void complex_federated_service_subselect_and_graph() {
		String q = "SELECT ?u ?g (COUNT(DISTINCT ?p) AS ?pc)\n" +
				"WHERE {\n" +
				"  SERVICE <http://example.org/sparql> {\n" +
				"    {\n" +
				"      SELECT ?u ?p\n" +
				"      WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void complex_ask_with_subselect_exists_and_not_exists() {
		String q = "SELECT ?g ?s ?n\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void complex_expressions_aggregation_and_ordering() {
		String q = "SELECT ?s (CONCAT(LCASE(STR(?n)), \"-\", STRUUID()) AS ?tag) (MAX(?age) AS ?maxAge)\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void complex_mutual_knows_with_degree_subqueries() {
		String q = "SELECT ?a ?b ?aC ?bC\n" +
				"WHERE {\n" +
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
				"ORDER BY DESC((?aC + ?bC))\n" +
				"LIMIT 10";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void complex_path_inverse_and_negated_set_mix() {
		String q = "SELECT ?a ?n\n" +
				"WHERE {\n" +
				"  ?a (^foaf:knows/!(ex:knows|rdf:type|ex:helps|rdf:subject)/foaf:name) ?n .\n" +
				"  FILTER ((LANG(?n) = \"\") || LANGMATCHES(LANG(?n), \"en\"))\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void complex_service_variable_and_nested_subqueries() {
		String q = "SELECT ?svc ?s (SUM(?c) AS ?total)\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void complex_values_matrix_paths_and_groupby_alias() {
		String q = "SELECT ?key ?person (COUNT(?o) AS ?c)\n" +
				"WHERE {\n" +
				"  {\n" +
				"    VALUES (?k) {\n" +
				"      (\"foaf\")\n" +
				"    }\n" +
				"    ?person foaf:knows/foaf:knows* ?other .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    VALUES (?k) {\n" +
				"      (\"ex\")\n" +
				"    }\n" +
				"    ?person ex:knows/foaf:knows* ?other .\n" +
				"  }\n" +
				"  ?person ?p ?o .\n" +
				"  FILTER (?p != rdf:type)\n" +
				"}\n" +
				"GROUP BY (?k AS ?key) ?person\n" +
				"ORDER BY ?key DESC(?c)\n" +
				"LIMIT 100";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void groupByAlias() {
		String q = "SELECT ?predicate\n" +
				"WHERE {\n" +
				"  ?a ?b ?c .\n" +
				"}\n" +
				"GROUP BY (?b AS ?predicate)\n" +
				"ORDER BY ?predicate\n" +
				"LIMIT 100";
		assertSameSparqlQuery(q, cfg());
	}

	// ================================================
	// ===== Ultra-heavy, limit-stretching tests ======
	// ================================================

	@Test
	@Disabled
	void mega_monster_deep_nesting_everything() {
		String q = "SELECT REDUCED ?g ?x ?y (?cnt AS ?count) (IF(BOUND(?avgAge), (xsd:decimal(?cnt) + xsd:decimal(?avgAge)), xsd:decimal(?cnt)) AS ?score)\n"
				+
				"WHERE {\n" +
				"  VALUES ?g { ex:g1 ex:g2 ex:g3 }\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void mega_massive_union_chain_with_mixed_paths() {
		String q = "SELECT ?s ?kind\n" +
				"WHERE {\n" +
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
				"    ?s !(rdf:type|ex:age) ?o .\n" +
				"  }\n" +
				"    UNION\n" +
				"  {\n" +
				"    BIND(\"zeroOrOne\" AS ?kind)\n" +
				"    ?s foaf:knows? ?o .\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void mega_wide_values_matrix_typed_and_undef() {
		String q = "SELECT ?s ?p ?o ?tag ?n (IF(BOUND(?o), STRLEN(STR(?o)), -1) AS ?len)\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	@Disabled
	void mega_parentheses_precedence_and_whitespace_stress() {
		String q = "SELECT ?s ?o (?score AS ?score2)\n" +
				"WHERE {\n" +
				"  ?s ( (foaf:knows) / ( ( ^foaf:knows ) | ( ex:knows ) ) ) ?o .\n" +
				"  BIND( ( ( ( IF(BOUND(?o), 1, 0) + 0 ) * 1 ) ) AS ?score )\n" +
				"  FILTER(     ( ( ( BOUND(?s) && BOUND(?o) ) ) ) && ( ( REGEX( STR(?o), \"^.+$\", \"i\" ) ) )   )\n" +
				"}\n" +
				"ORDER BY ?score\n" +
				"LIMIT 100";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	@Disabled
	void mega_construct_with_blank_nodes_graphs_and_paths() {
		String q = "CONSTRUCT {\n" +
				"  ?s ex:edge [ a ex:Edge ; ex:to ?t ; ex:score ?score ] .\n" +
				"  ?s ex:seenIn ?g .\n" +
				"}\n" +
				"WHERE {\n" +
				"  VALUES ?g { ex:g1 ex:g2 } \n" +
				"  GRAPH ?g { ?s (foaf:knows/foaf:knows?) ?t }\n" +
				"  OPTIONAL { ?s ex:age ?age }\n" +
				"  BIND(IF(BOUND(?age), xsd:decimal(?age) / 100, 0.0) AS ?score)\n" +
				"  FILTER(NOT EXISTS { ?t rdf:type ex:Robot })\n" +
				"}\n" +
				"ORDER BY DESC(?score)\n" +
				"LIMIT 500";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	@Disabled
	void mega_ask_deep_exists_notexists_filters() {
		String q = "ASK WHERE {\n" +
				"  { ?a foaf:knows ?b } UNION { ?b foaf:knows ?a }\n" +
				"  FILTER EXISTS { ?a foaf:name ?n FILTER(REGEX(?n, \"^A\", \"i\")) }\n" +
				"  FILTER NOT EXISTS { ?a ex:blockedBy ?b }\n" +
				"  GRAPH ?g { ?a !(rdf:type|ex:age)/foaf:name ?x }\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void path_in_graph() {
		String q = "SELECT ?g ?a ?x\n" +
				"WHERE {\n" +
				"  GRAPH ?g {\n" +
				"    ?a !(rdf:type|ex:age)/foaf:name ?x .\n" +
				"  }\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void mega_service_graph_interleaved_with_subselects() {
		String q = "SELECT ?s ?g (SUM(?c) AS ?total)\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
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
		String q = "SELECT ?s ?bestName ?avgAge\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void mega_optional_minus_nested() {
		String q = "SELECT ?s ?o\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void mega_scoped_variables_and_aliasing_across_subqueries() {
		String q = "SELECT ?s ?bestName ?deg\n" +
				"WHERE {\n" +
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
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void mega_type_shorthand_and_mixed_sugar() {
		String q = "SELECT ?s ?n\n" +
				"WHERE {\n" +
				"  ?s a foaf:Person ; foaf:name ?n .\n" +
				"  [] foaf:knows ?s .\n" +
				"  (ex:alice ex:bob ex:carol) rdf:rest*/rdf:first ?x .\n" +
				"  FILTER (STRLEN(?n) > 0)\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}

	@Test
	void mega_exists_union_inside_exists_and_notexists() {
		String q = "SELECT ?s\n" +
				"WHERE {\n" +
				"  ?s ?p ?o .\n" +
				"  FILTER (EXISTS { { ?s foaf:knows ?t . } UNION { ?t foaf:knows ?s . } FILTER (NOT EXISTS { ?t ex:blockedBy ?s . }) })\n"
				+
				"}";
		assertSameSparqlQuery(q, cfg());
	}

}
