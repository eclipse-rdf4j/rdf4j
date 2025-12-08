/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * End-to-end optimizer tests: - For each optimization: a SAFE test (rewrite should happen) and an UNSAFE test (rewrite
 * must NOT happen). - Queries are rendered from the optimized TupleExpr using TupleExprIRRenderer (as in user example).
 *
 * Assumptions: - Your optimizer runs inside RDF4J's optimization pipeline so that Explanation.Level.Optimized reflects
 * the rewrite. - TupleExprIRRenderer exists on classpath (same utility you used in the sample).
 */

public class SparqlOptimizationTests {

	// Common prefix map (preserve insertion order for stable rendering)
	private static final Map<String, String> PREFIXES = new LinkedHashMap<>();
	static {
		PREFIXES.put("ex", "http://ex/");
		PREFIXES.put("rdf", RDF.NAMESPACE);
		PREFIXES.put("rdfs", RDFS.NAMESPACE);
		PREFIXES.put("xsd", XSD.NAMESPACE);
		PREFIXES.put("owl", "http://www.w3.org/2002/07/owl#");
		PREFIXES.put("geo", "http://www.opengis.net/ont/geosparql#");
		PREFIXES.put("geof", "http://www.opengis.net/def/function/geosparql/");
	}

	// Helpers
	private String renderOptimized(String sparql, String ttl) throws Exception {
		SailRepository repo = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection cx = repo.getConnection()) {
			cx.add(new StringReader(ttl == null ? "" : ttl), "", RDFFormat.TURTLE);
		}

		String rendered;
		try (SailRepositoryConnection cx = repo.getConnection()) {
			TupleQuery query = cx.prepareTupleQuery(sparql);
			TupleExpr tupleExpr = (TupleExpr) query.explain(Explanation.Level.Optimized).tupleExpr();

			TupleExprIRRenderer.Config cfg = new TupleExprIRRenderer.Config();
			PREFIXES.forEach((p, ns) -> cfg.prefixes.put(p, ns));
			TupleExprIRRenderer renderer = new TupleExprIRRenderer(cfg);
			rendered = renderer.render(tupleExpr);
		} catch (Exception e) {
			System.out.println("Failed to render query:\n" + sparql + "\n");
			throw e;
		}

		finally {
			repo.shutDown();
		}
		return rendered;
	}

	private String header() {
		return ""
				+ "PREFIX ex: <http://ex/>\n"
				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
				+ "PREFIX rdfs: <" + RDFS.NAMESPACE + ">\n"
				+ "PREFIX xsd: <" + XSD.NAMESPACE + ">\n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+ "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n"
				+ "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n";
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 1) Equality filter → SARGable triple
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void eqFilterToTriple_safe() throws Exception {
		String q = header() + ""
				+ "SELECT ?s WHERE {\n"
				+ "  ?s ex:status ?st .\n"
				+ "  FILTER(?st = \"PAID\")\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?s WHERE {\n"
				+ "  ?s ex:status \"PAID\" .\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void eqFilterToTriple_unsafe_typeMismatch_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?s WHERE {\n"
				+ "  ?s ex:price ?p .\n"
				+ "  FILTER(xsd:decimal(?p) = 10.0)\n"
				+ "}";
		// Cannot drop the cast or turn into term-equality without type guarantees
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 2) Range SARGing & move casts to constants
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void rangeSarg_moveCast_safe() throws Exception {
		String q = header() + ""
				+ "SELECT ?s WHERE {\n"
				+ "  ?s ex:ts ?t .\n"
				+ "  FILTER(xsd:dateTime(?t) >= \"2025-01-01T00:00:00Z\")\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?s WHERE {\n"
				+ "  ?s ex:ts ?t .\n"
				+ "  FILTER(?t >= \"2025-01-01T00:00:00Z\"^^xsd:dateTime)\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void rangeSarg_unsafe_untypedLiteral_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?s WHERE {\n"
				+ "  ?s ex:price ?p .\n"
				+ "  FILTER(xsd:decimal(?p) > \"10\")\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 3) Date-part → range
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void datepartToRange_safe_yearEquals() throws Exception {
		String q = header() + ""
				+ "SELECT ?s WHERE {\n"
				+ "  ?s ex:ts ?t .\n"
				+ "  FILTER(YEAR(?t) = 2024)\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?s WHERE {\n"
				+ "  ?s ex:ts ?t .\n"
				+ "  FILTER(?t >= \"2024-01-01T00:00:00Z\"^^xsd:dateTime && ?t < \"2025-01-01T00:00:00Z\"^^xsd:dateTime)\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 4) Filter pushdown (avoid OPTIONAL trap)
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void filterPushdown_safe_reorderWithinBGP() throws Exception {
		String q = header() + ""
				+ "SELECT ?a ?type1 ?b ?type2 WHERE {\n"
				+ "  ?a rdf:type ?type1 .\n"
				+ "  ?b rdf:type ?type2 .\n"
				+ "  FILTER (?type1 != ex:Agent)\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?a ?type1 ?b ?type2 WHERE {\n"
				+ "  ?a rdf:type ?type1 .\n"
				+ "  FILTER (?type1 != ex:Agent)\n"
				+ "  ?b rdf:type ?type2 .\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void filterPushdown_unsafe_crossOptional_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  ?c ex:id ?id .\n"
				+ "  OPTIONAL { ?c ex:email ?e }\n"
				+ "  FILTER(BOUND(?e) || ?flag)\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 5) EXISTS decorrelation → semi-join
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void existsToSemijoin_safe() throws Exception {
		String q = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  ?c ex:id ?id .\n"
				+ "  FILTER EXISTS { ?c ex:order ?o . ?o ex:status \"PAID\" }\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  { SELECT DISTINCT ?c WHERE { ?c ex:order ?o . ?o ex:status \"PAID\" } }\n"
				+ "  ?c ex:id ?id .\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void existsToSemijoin_unsafe_nondeterministic_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  ?c ex:id ?id .\n"
				+ "  FILTER EXISTS { BIND(RAND() AS ?r) FILTER(?r < 0.5) }\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 6) NOT EXISTS / MINUS → anti-join (reorder earlier)
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void antijoin_reorderEarly_safe() throws Exception {
		String q = header() + ""
				+ "SELECT ?o ?a WHERE {\n"
				+ "  ?o ex:customer ?c .\n"
				+ "  ?o ex:amount ?a .\n"
				+ "  FILTER NOT EXISTS { ?c ex:blocked true }\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?o ?a WHERE {\n"
				+ "  ?o ex:customer ?c .\n"
				+ "  FILTER NOT EXISTS { ?c ex:blocked true }\n"
				+ "  ?o ex:amount ?a .\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void antijoin_unsafe_crossOptional_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  ?c ex:id ?id .\n"
				+ "  OPTIONAL { ?c ex:vip true }\n"
				+ "  FILTER NOT EXISTS { ?c ex:email ?e }\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 7) OPTIONAL → inner join under null-rejecting filter
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void optionalToInnerJoin_safe_nullRejecting() throws Exception {
		String q = header() + ""
				+ "SELECT ?c ?e WHERE {\n"
				+ "  ?c ex:id ?id .\n"
				+ "  OPTIONAL { ?c ex:email ?e }\n"
				+ "  FILTER(?e != \"\")\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?c ?e WHERE {\n"
				+ "  ?c ex:id ?id .\n"
				+ "  ?c ex:email ?e .\n"
				+ "  FILTER(?e != \"\")\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void optionalToInnerJoin_unsafe_nonNullRejecting_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  ?c ex:id ?id .\n"
				+ "  OPTIONAL { ?c ex:email ?e }\n"
				+ "  FILTER(BOUND(?e) || ?flag)\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 8) Star-join fusion & selective anchor
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void starFusion_safe_anchorMostSelective() throws Exception {
		String q = header() + ""
				+ "SELECT ?p ?n ?c ?e WHERE {\n"
				+ "  ?p ex:name ?n .\n"
				+ "  ?p ex:country ?c .\n"
				+ "  ?p ex:email ?e .\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?p ?n ?c ?e WHERE {\n"
				+ "  ?p ex:email ?e .\n"
				+ "  ?p ex:country ?c .\n"
				+ "  ?p ex:name ?n .\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void starFusion_unsafe_crossOptional_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?p ?id ?img WHERE {\n"
				+ "  ?p ex:id ?id .\n"
				+ "  OPTIONAL { ?p ex:photo ?img }\n"
				+ "  ?p ex:country \"NO\" .\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 9) Early DISTINCT / drop redundant DISTINCT (via metadata)
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void distinctEarly_safe_dropViaFunctionalProperty() throws Exception {
		String ttl = ""
				+ "@prefix ex: <http://ex/> .\n"
				+ "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
				+ "ex:id a owl:FunctionalProperty .\n";
		String q = header() + ""
				+ "SELECT DISTINCT ?c WHERE { ?c ex:id ?id }";
		String expected = header() + ""
				+ "SELECT ?c WHERE { ?c ex:id ?id }";
		assertThat(renderOptimized(q, ttl)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void distinctEarly_unsafe_multiValued_kept() throws Exception {
		String q = header() + ""
				+ "SELECT DISTINCT ?c WHERE { ?c ex:name ?n }";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 10) Projection pushdown (into subselect)
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void projectionPushdown_safe_intoSubselect() throws Exception {
		String q = header() + ""
				+ "SELECT ?p ?name WHERE {\n"
				+ "  { SELECT ?p ?name ?bio WHERE { ?p ex:name ?name ; ex:bio ?bio } }\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?p ?name WHERE {\n"
				+ "  { SELECT ?p ?name WHERE { ?p ex:name ?name ; ex:bio ?bio } }\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void projectionPushdown_unsafe_neededOutside_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?p WHERE {\n"
				+ "  { SELECT ?p ?name WHERE { ?p ex:name ?name } }\n"
				+ "  FILTER(STRLEN(?name) > 3)\n"
				+ "}";
		// Cannot drop ?name from subselect since it's used by outer FILTER
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 11) IN/UNION/VALUES normalization
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void unionToValues_safe() throws Exception {
		String q = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  { ?c ex:status \"PAID\" }\n"
				+ "  UNION\n"
				+ "  { ?c ex:status \"PENDING\" }\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  VALUES ?st { \"PAID\" \"PENDING\" }\n"
				+ "  ?c ex:status ?st .\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void unionToValues_unsafe_branchSpecificFilter_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?o WHERE {\n"
				+ "  { ?o ex:status \"PAID\" ; ex:amount ?a . FILTER(?a > 100) }\n"
				+ "  UNION\n"
				+ "  { ?o ex:status \"PENDING\" }\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 12) OR → UNION (DNF sarging)
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void orToUnion_safe_disjoint() throws Exception {
		String q = header() + ""
				+ "SELECT ?o WHERE {\n"
				+ "  ?o ex:status ?st .\n"
				+ "  FILTER(?st = \"PAID\" || ?st = \"PENDING\")\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?o WHERE {\n"
				+ "  { ?o ex:status \"PAID\" }\n"
				+ "  UNION\n"
				+ "  { ?o ex:status \"PENDING\" }\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void orToUnion_unsafe_overlappingRanges_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?s WHERE {\n"
				+ "  ?s ex:age ?a .\n"
				+ "  FILTER(?a >= 10 || ?a <= 20)\n" // overlap [10,20]
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 13) ORDER BY LIMIT pushdown (+ tie-break)
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void topKPushdownThroughUnion_safe() throws Exception {
		String q = header() + ""
				+ "SELECT ?x ?s WHERE {\n"
				+ "  { ?x ex:score ?s }\n"
				+ "  UNION\n"
				+ "  { ?x ex:score2 ?s }\n"
				+ "}\nORDER BY DESC(?s) LIMIT 10";
		String expected = header() + ""
				+ "SELECT ?x ?s WHERE {\n"
				+ "  { SELECT ?x ?s WHERE { ?x ex:score ?s } ORDER BY DESC(?s) STR(?x) LIMIT 10 }\n"
				+ "  UNION\n"
				+ "  { SELECT ?x ?s WHERE { ?x ex:score2 ?s } ORDER BY DESC(?s) STR(?x) LIMIT 10 }\n"
				+ "}\nORDER BY DESC(?s) LIMIT 10";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void topKPushdown_unsafe_externalKey_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?x ?s WHERE {\n"
				+ "  { ?x ex:score ?s }\n"
				+ "  UNION\n"
				+ "  { ?x ex:score2 ?s }\n"
				+ "}\nORDER BY ?region DESC(?s) LIMIT 5";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 14) Seek pagination (OFFSET → keyset)
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void seekPagination_safe_replaceOffset() throws Exception {
		String q = header() + ""
				+ "SELECT ?id WHERE {\n"
				+ "  ?s ex:id ?id .\n"
				+ "}\nORDER BY ?id OFFSET 10000 LIMIT 50";
		String expected = header() + ""
				+ "SELECT ?id WHERE {\n"
				+ "  ?s ex:id ?id .\n"
				+ "  FILTER(?id > ?lastId)\n"
				+ "}\nORDER BY ?id LIMIT 50";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void seekPagination_unsafe_noStableOrder_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?id WHERE { ?s ex:id ?id } ORDER BY RAND() OFFSET 100 LIMIT 10";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 15) COUNT(DISTINCT) decomposition
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void countDistinct_decompose_safe() throws Exception {
		String q = header() + ""
				+ "SELECT ?c (COUNT(DISTINCT ?item) AS ?n) WHERE {\n"
				+ "  ?o ex:customer ?c ; ex:item ?item .\n"
				+ "} GROUP BY ?c";
		String expected = header() + ""
				+ "{ SELECT DISTINCT ?c ?item WHERE { ?o ex:customer ?c ; ex:item ?item } }\n"
				+ "SELECT ?c (COUNT(*) AS ?n) WHERE { } GROUP BY ?c";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void countDistinct_unsafe_unionNeedsPerBranchDedup_kept() throws Exception {
		String q = header() + ""
				+ "SELECT (COUNT(DISTINCT ?x) AS ?n) WHERE {\n"
				+ "  { ?x ex:p ?o } UNION { ?x ex:q ?o }\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 16) Join elimination via keys/functional (use domain for safe demo)
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void joinElimination_safe_domainImpliedType() throws Exception {
		String ttl = ""
				+ "@prefix ex: <http://ex/> .\n"
				+ "@prefix rdfs: <" + RDFS.NAMESPACE + "> .\n"
				+ "ex:customer rdfs:domain ex:Customer .\n";
		String q = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  ?o ex:customer ?c .\n"
				+ "  ?c a ex:Customer .\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  ?o ex:customer ?c .\n"
				+ "}";
		assertThat(renderOptimized(q, ttl)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void joinElimination_unsafe_typeUsedInFilter_kept() throws Exception {
		String ttl = "@prefix ex: <http://ex/> .";
		String q = header() + ""
				+ "SELECT ?c WHERE {\n"
				+ "  ?o ex:customer ?c .\n"
				+ "  ?c a ex:Customer .\n"
				+ "  FILTER(EXISTS { ?c a ex:Customer })\n"
				+ "}";
		assertThat(renderOptimized(q, ttl)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 17) Property-path planning: unroll short bounds
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void pathUnroll_safe_shortBound() throws Exception {
		String q = header() + ""
				+ "SELECT ?s ?t WHERE { ?s ex:next{1,3} ?t }";
		String expected = header() + ""
				+ "SELECT ?s ?t WHERE {\n"
				+ "  { ?s ex:next ?t }\n"
				+ "  UNION\n"
				+ "  { ?s ex:next/ex:next ?t }\n"
				+ "  UNION\n"
				+ "  { ?s ex:next/ex:next/ex:next ?t }\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void pathUnroll_unsafe_requiresAuthoritativeClosure_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?a ?b WHERE { ?a ex:dependsOn+ ?b }";
		// Without a guaranteed closure index, keep generic path (no textual change)
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 18) SERVICE bind-join & VALUES broadcast (push VALUES into SERVICE)
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void service_valuesBroadcast_safe_moveInsideService() throws Exception {
		String q = header() + ""
				+ "SELECT ?c ?city WHERE {\n"
				+ "  VALUES ?cty { \"NO\" \"SE\" }\n"
				+ "  SERVICE <http://geo> { ?c ex:country ?cty ; ex:city ?city }\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?c ?city WHERE {\n"
				+ "  SERVICE <http://geo> { VALUES ?cty { \"NO\" \"SE\" } ?c ex:country ?cty ; ex:city ?city }\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void service_valuesBroadcast_unsafe_unknownEndpointCapabilities_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?x WHERE { SERVICE <http://opaque> { ?x ex:p ?y } }";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 19) LANGMATCHES → equality/prefix
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void langmatchesToPrefix_safe_simpleTag() throws Exception {
		String q = header() + ""
				+ "SELECT ?p ?l WHERE {\n"
				+ "  ?p rdfs:label ?l .\n"
				+ "  FILTER(LANGMATCHES(LANG(?l), \"en\"))\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?p ?l WHERE {\n"
				+ "  ?p rdfs:label ?l .\n"
				+ "  FILTER(LANG(?l) = \"en\" || STRSTARTS(LANG(?l), \"en-\"))\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void langmatchesToPrefix_unsafe_complexRange_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?p ?l WHERE {\n"
				+ "  ?p rdfs:label ?l .\n"
				+ "  FILTER(LANGMATCHES(LANG(?l), \"*-Latn\"))\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// 20) Geo bounding-box prefilter (keep exact predicate)
	// ─────────────────────────────────────────────────────────────────────────────

	@Test
	@Disabled
	public void geo_bboxPrefilter_safe_addCoarseThenExact() throws Exception {
		String q = header() + ""
				+ "SELECT ?x WHERE {\n"
				+ "  ?x ex:lat ?lat ; ex:lon ?lon .\n"
				+ "  FILTER(geof:distance(geof:point(?lon,?lat), geof:point(10.75,59.91)) < 5000)\n"
				+ "}";
		String expected = header() + ""
				+ "SELECT ?x WHERE {\n"
				+ "  ?x ex:lat ?lat ; ex:lon ?lon .\n"
				+ "  FILTER(?lat > 59.865 && ?lat < 59.955 && ?lon > 10.675 && ?lon < 10.825)\n"
				+ "  FILTER(geof:distance(geof:point(?lon,?lat), geof:point(10.75,59.91)) < 5000)\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(expected);
	}

	@Test
	@Disabled
	public void geo_bboxPrefilter_unsafe_dateline_kept() throws Exception {
		String q = header() + ""
				+ "SELECT ?x WHERE {\n"
				+ "  ?x geo:asWKT ?w .\n"
				+ "  FILTER(geof:sfWithin(?w, <http://ex/PolygonCrossingDateline>))\n"
				+ "}";
		assertThat(renderOptimized(q, null)).isEqualToNormalizingNewlines(q);
	}
}
