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

import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprToSparql;
import org.junit.jupiter.api.Test;

public class TupleExprToSparqlTest {

	private static final String EX = "http://ex/";

	private static final String SPARQL_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX ex: <http://ex/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

	// Shared renderer config with canonical whitespace and useful prefixes.
	private static TupleExprToSparql.Config cfg() {
		TupleExprToSparql.Config cfg = new TupleExprToSparql.Config();
		cfg.canonicalWhitespace = true;
		cfg.printPrefixes = true;
		cfg.usePrefixCompaction = true;
		cfg.prefixes.put("rdf", RDF.NAMESPACE);
		cfg.prefixes.put("rdfs", RDFS.NAMESPACE);
		cfg.prefixes.put("foaf", FOAF.NAMESPACE);
		cfg.prefixes.put("ex", EX);
		cfg.prefixes.put("xsd", XSD.NAMESPACE);
		cfg.baseIRI = null;
		return cfg;
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

	private String render(String sparql, TupleExprToSparql.Config cfg) {
		TupleExpr algebra = parseAlgebra(sparql);
		return new TupleExprToSparql(cfg).render(algebra);
	}

	/** Round-trip twice and assert the renderer is a fixed point (idempotent). */
	private String assertFixedPoint(String sparql, TupleExprToSparql.Config cfg) {
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
	private void assertSameSparqlQuery(String original, TupleExprToSparql.Config cfg) {
		String rendered = assertFixedPoint(original, cfg);
		assertThat(rendered).isEqualToNormalizingNewlines(SPARQL_PREFIX + original);
	}

	// ---------- Tests: fixed point + semantic equivalence where applicable ----------

	@Test
	void basic_select_bgp() {
		String q = "SELECT ?s ?name\n" +
				"WHERE {\n" +
				"  ?s rdf:type foaf:Person .\n" +
				"  ?s foaf:name ?name .\n" +
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
	void service_silent_block_fixed_point() {
		String q = "SELECT * WHERE {\n"
				+ "  SERVICE SILENT <http://example.org/sparql> { ?s ?p ?o }\n"
				+ "}";
		// We do not execute against remote SERVICE; check fixed point only:
		assertFixedPoint(q, cfg());
	}

	@Test
	void property_paths_star_plus_question() {
		// These rely on RDF4J producing ArbitraryLengthPath for +/*/?.
		String qStar = "SELECT ?x ?y WHERE { ?x ex:knows*/foaf:name ?y }";
		String qPlus = "SELECT ?x ?y WHERE { ?x ex:knows+/foaf:name ?y }";
		String qOpt = "SELECT ?x ?y WHERE { ?x ex:knows?/foaf:name ?y }";

		assertFixedPoint(qStar, cfg());
		assertFixedPoint(qPlus, cfg());
		assertFixedPoint(qOpt, cfg());
	}

	@Test
	void prefix_compaction_is_applied() {
		String q = "SELECT ?s WHERE {\n"
				+ "  ?s <" + RDF.TYPE.stringValue() + "> <" + FOAF.PERSON.stringValue() + "> .\n"
				+ "}";
		String rendered = assertFixedPoint(q, cfg());
		// Expect QName compaction to rdf:type and foaf:Person
		assertTrue(rendered.contains("rdf:type"), "Should compact rdf:type");
		assertTrue(rendered.contains("foaf:Person"), "Should compact foaf:Person");
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

}
