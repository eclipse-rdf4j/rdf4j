package org.eclipse.rdf4j.sail.memory;

/**
 * **************************************************************************** Copyright (c) 2025 Eclipse RDF4J
 * contributors.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Distribution License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause ****************************************************************************
 */

public class SparqlOptimizerRewriteTest {
//
//	/* ---------- helpers ---------- */
//
//	private static Map<String, String> defaultPrefixes() {
//		Map<String, String> p = new LinkedHashMap<>();
//		p.put("ex", "http://ex/");
//		p.put(RDF.PREFIX, RDF.NAMESPACE);
//		p.put(RDFS.PREFIX, RDFS.NAMESPACE);
//		p.put(XSD.PREFIX, XSD.NAMESPACE);
//		p.put(DC.PREFIX, DC.NAMESPACE);
//		return p;
//	}
//
//	private static String renderOptimized(String sparql) {
//		SailRepository sailRepository = new SailRepository(new MemoryStore());
//		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
//			connection.add(new StringReader(""), "", RDFFormat.TURTLE);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//
//		String rendered;
//		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
//			TupleQuery query = connection.prepareTupleQuery(sparql);
//			TupleExpr tupleExpr = (TupleExpr) query.explain(Explanation.Level.Unoptimized).tupleExpr();
//
//			TupleExprIRRenderer.Config config = new TupleExprIRRenderer.Config();
//			defaultPrefixes().forEach((k, v) -> config.prefixes.put(k, v));
//
//			TupleExprIRRenderer tupleExprToSparql = new TupleExprIRRenderer(config);
//			rendered = tupleExprToSparql.render(tupleExpr);
//		}
//		sailRepository.shutDown();
//		return rendered;
//	}
//
//	/*
//	 * ============================================================== 1) Join reordering inside BGPs
//	 * ==============================================================
//	 */
//
//	@Test
//	@Disabled
//	public void testJoinReorder_Safe_withinBGP() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?o ?c\n"
//				+ "WHERE {\n"
//				+ "  ?o rdf:type ex:Order ; ex:customer ?c ; ex:total ?t .\n"
//				+ "  ?c ex:country \"NO\" .\n"
//				+ "  FILTER(?t > 1000)\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?o ?c\n"
//				+ "WHERE {\n"
//				+ "  ?c ex:country \"NO\" .\n"
//				+ "  ?o ex:total ?t .\n"
//				+ "  FILTER(?t > 1000)\n"
//				+ "  ?o rdf:type ex:Order ; ex:customer ?c .\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testJoinReorder_Unsafe_doNotCrossOptional() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?c\n"
//				+ "WHERE {\n"
//				+ "  OPTIONAL { ?c ex:email ?e . }\n"
//				+ "  ?c rdf:type ex:Customer .\n"
//				+ "}";
//		// Reordering the main BGP is fine, but the OPTIONAL block must remain intact and not be pulled out.
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?c\n"
//				+ "WHERE {\n"
//				+ "  ?c rdf:type ex:Customer .\n"
//				+ "  OPTIONAL { ?c ex:email ?e . }\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	/*
//	 * ============================================================== 2) FILTER pushdown & splitting
//	 * ==============================================================
//	 */
//
//	@Test
//	@Disabled
//	public void testFilterPushdown_Safe_intoBindingBGP() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?o\n"
//				+ "WHERE {\n"
//				+ "  ?o ex:total ?t ; ex:customer ?c .\n"
//				+ "  ?c ex:country ?cty .\n"
//				+ "  FILTER(?cty = \"NO\" && ?t > 100)\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?o\n"
//				+ "WHERE {\n"
//				+ "  ?c ex:country \"NO\" .\n"
//				+ "  ?o ex:total ?t ; ex:customer ?c .\n"
//				+ "  FILTER(?t > 100)\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testFilterPushdown_Unsafe_doNotPushIntoOptionalWithBOUND() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n" +
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
//				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
//				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
//				"PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
//				"SELECT ?c ?email\n" +
//				"WHERE {\n" +
//				"  ?c rdf:type ex:Customer .\n" +
//				"  OPTIONAL {\n" +
//				"    ?c ex:email ?email .\n" +
//				"  }\n" +
//				"  FILTER (!(BOUND(?email)) || (?email != \"spam@example.com\"))\n" +
//				"}";
//		// The filter must stay outside the OPTIONAL (null-tolerant/BOUND-sensitive).
//		String after = before;
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	/*
//	 * ============================================================== 3) Projection / variable pruning
//	 * ==============================================================
//	 */
//
//	@Test
//	@Disabled
//	public void testProjectionPruning_Safe_dropUnusedColumnInSubselect() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?name\n"
//				+ "WHERE {\n"
//				+ "  { SELECT ?name ?u WHERE { ?c ex:name ?name ; ex:unused ?u . } }\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?name\n"
//				+ "WHERE {\n"
//				+ "  { SELECT ?name WHERE { ?c ex:name ?name ; ex:unused ?u . } }\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testProjectionPruning_Unsafe_keepVarsUsedByOrderBy() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?name\n"
//				+ "WHERE {\n"
//				+ "  { SELECT ?name ?n WHERE { ?c ex:name ?n . BIND(UCASE(?n) AS ?name) } ORDER BY ?n }\n"
//				+ "}";
//		// ?n is required by ORDER BY inside the subselect; it must not be pruned.
//		String after = before;
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	/*
//	 * ============================================================== 4) OPTIONAL promotion (outer -> inner) & ordering
//	 * ==============================================================
//	 */
//
//	@Test
//	@Disabled
//	public void testOptionalPromotion_Safe_nullIntolerantFilter() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?o\n"
//				+ "WHERE {\n"
//				+ "  ?o rdf:type ex:Order .\n"
//				+ "  OPTIONAL { ?o ex:detail ?d . ?d ex:qty ?q . }\n"
//				+ "  FILTER(?q > 0)\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?o\n"
//				+ "WHERE {\n"
//				+ "  ?o rdf:type ex:Order ; ex:detail ?d .\n"
//				+ "  ?d ex:qty ?q .\n"
//				+ "  FILTER(?q > 0)\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testOptionalPromotion_Unsafe_withCOALESCE() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?o\n"
//				+ "WHERE {\n"
//				+ "  ?o rdf:type ex:Order .\n"
//				+ "  OPTIONAL { ?o ex:detail ?d . ?d ex:qty ?q . }\n"
//				+ "  FILTER(COALESCE(?q, 1) > 0)\n"
//				+ "}";
//		// COALESCE makes the filter null-tolerant; promotion must not occur.
//		String after = before;
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	/*
//	 * ============================================================== 5) Subquery unnesting / decorrelation
//	 * ==============================================================
//	 */
//
//	@Test
//	@Disabled
//	public void testExistsUnnesting_Safe_toJoinWithDistinct() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?o WHERE {\n"
//				+ "  ?o rdf:type ex:Order .\n"
//				+ "  FILTER EXISTS { ?o ex:detail ?d . ?d ex:qty ?q . FILTER(?q > 0) }\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT DISTINCT ?o WHERE {\n"
//				+ "  ?o rdf:type ex:Order ; ex:detail ?d .\n"
//				+ "  ?d ex:qty ?q .\n"
//				+ "  FILTER(?q > 0)\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testDecorrelation_Unsafe_doNotCrossLimit() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n" +
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
//				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
//				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
//				"PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
//				"SELECT ?c ?cnt\n" +
//				"WHERE {\n" +
//				"  ?c rdf:type ex:Customer .\n" +
//				"  {\n" +
//				"    SELECT (COUNT(?o) AS ?cnt)\n" +
//				"    WHERE {\n" +
//				"      ?o ex:customer ?c .\n" +
//				"    } LIMIT 1\n" +
//				"  }\n" +
//				"}";
//		// LIMIT inside subselect makes decorrelation unsafe; keep as-is.
//		String after = before;
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	/*
//	 * ============================================================== 6) UNION normalization & filter distribution
//	 * ==============================================================
//	 */
//
//	@Test
//	@Disabled
//	public void testUnionNormalization_Safe_flattenNested() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?o WHERE {\n"
//				+ "  { { ?o ex:country \"US\" } UNION { ?o ex:country \"CA\" } }\n"
//				+ "  UNION { ?o ex:country \"MX\" }\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?o WHERE {\n"
//				+ "  { ?o ex:country \"US\" } UNION { ?o ex:country \"CA\" } UNION { ?o ex:country \"MX\" }\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testUnionFilterDistribution_Safe_refsBranchVars() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?o WHERE {\n"
//				+ "  { ?o ex:country \"US\" . ?o ex:total ?t }\n"
//				+ "  UNION\n"
//				+ "  { ?o ex:country \"CA\" . ?o ex:total ?t }\n"
//				+ "  FILTER(?t > 100)\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?o WHERE {\n"
//				+ "  { ?o ex:country \"US\" . ?o ex:total ?t . FILTER(?t > 100) }\n"
//				+ "  UNION\n"
//				+ "  { ?o ex:country \"CA\" . ?o ex:total ?t . FILTER(?t > 100) }\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testUnionFilterDistribution_Unsafe_varNotInAllBranches() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?o WHERE {\n"
//				+ "  { ?o ex:country \"US\" . ?o ex:total ?t }\n"
//				+ "  UNION\n"
//				+ "  { ?o ex:country \"CA\" }\n"
//				+ "  FILTER(?t > 100)\n"
//				+ "}";
//		// ?t not bound in CA branch; filter must not be distributed.
//		String after = before;
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	/*
//	 * ============================================================== 7) LIMIT / TOP-K pushdown (with ORDER BY)
//	 * ==============================================================
//	 */
//
//	@Test
//	@Disabled
//	public void testLimitPushdown_Safe_oneToOneDecorate() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?o ?status\n"
//				+ "WHERE {\n"
//				+ "  ?o rdf:type ex:Order ; ex:total ?t ; ex:status ?status .\n"
//				+ "}\n"
//				+ "ORDER BY DESC(?t) LIMIT 100";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?o ?status\n"
//				+ "WHERE {\n"
//				+ "  { SELECT ?o\n"
//				+ "    WHERE { ?o rdf:type ex:Order ; ex:total ?t . }\n"
//				+ "    ORDER BY DESC(?t) LIMIT 100 }\n"
//				+ "  ?o ex:status ?status .\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testLimitPushdown_Unsafe_fanOutJoin() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?o ?i ?t\n"
//				+ "WHERE {\n"
//				+ "  ?o rdf:type ex:Order ; ex:total ?t ; ex:item ?i .\n"
//				+ "}\n"
//				+ "ORDER BY DESC(?t) LIMIT 1";
//		// Pushing LIMIT before fan-out would change row-count; must remain as-is.
//		String after = before;
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	/*
//	 * ============================================================== 8) GRAPH / SERVICE pruning & pushdown
//	 * ==============================================================
//	 */
//
//	@Test
//	@Disabled
//	public void testGraphPruning_Safe_fixedGraphByEquality() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?s ?p ?o WHERE {\n"
//				+ "  GRAPH ?g { ?s ?p ?o . }\n"
//				+ "  FILTER(?g = ex:g1)\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?s ?p ?o WHERE {\n"
//				+ "  GRAPH ex:g1 { ?s ?p ?o . }\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testGraphPruning_Unsafe_ambiguousInference() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?s ?p ?o WHERE {\n"
//				+ "  GRAPH ?g { ?s ?p ?o . }\n"
//				+ "  FILTER(STRSTARTS(STR(?g), STR(ex:g)))\n"
//				+ "}";
//		// Heuristic (prefix match) must not force a concrete GRAPH IRI.
//		String after = before;
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testServicePushdown_Safe_moveFilterInsideService() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?p ?name WHERE {\n"
//				+ "  ?p rdf:type ex:Person .\n"
//				+ "  SERVICE <http://hr/> { ?p ex:name ?name . }\n"
//				+ "  FILTER(STRSTARTS(?name, \"A\"))\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?p ?name WHERE {\n"
//				+ "  ?p rdf:type ex:Person .\n"
//				+ "  SERVICE <http://hr/> { ?p ex:name ?name . FILTER(STRSTARTS(?name, \"A\")) }\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testServicePushdown_Unsafe_optionalAndBOUND() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?p WHERE {\n"
//				+ "  OPTIONAL { SERVICE <http://hr/> { ?p ex:name ?name . } }\n"
//				+ "  FILTER(!BOUND(?name))\n"
//				+ "}";
//		// Moving the filter into the OPTIONAL/SERVICE would change its meaning; keep as-is.
//		String after = before;
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	/*
//	 * ============================================================== 9) Property-path rewriting
//	 * ==============================================================
//	 */
//
//	@Test
//	@Disabled
//	public void testPropertyPathRewrite_Safe_unrollFixedLength() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?y WHERE { ?x ex:knows{2} ?y . }";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?y WHERE { ?x ex:knows ?m . ?m ex:knows ?y . }";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testPropertyPathRewrite_Unsafe_doNotBoundPlus() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?y WHERE { ex:A ex:linkedTo+ ?y . }";
//		// Do not cap + into {1,k} automatically; leave as-is.
//		String after = before;
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	/*
//	 * ============================================================== 10) Semi-/anti-join rewrites
//	 * ==============================================================
//	 */
//
//	@Test
//	@Disabled
//	public void testAntiJoinRewrite_Safe_notExistsToMinus_sameSharedVars() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?p WHERE {\n"
//				+ "  ?p rdf:type ex:Person .\n"
//				+ "  FILTER NOT EXISTS { ?p ex:phone ?ph . }\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "PREFIX rdf: <" + RDF.NAMESPACE + ">\n"
//				+ "SELECT ?p WHERE {\n"
//				+ "  { ?p rdf:type ex:Person . }\n"
//				+ "  MINUS { ?p ex:phone ?ph . }\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testAntiJoinRewrite_Unsafe_notExistsWithNoSharedVars() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n" +
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
//				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
//				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
//				"PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
//				"SELECT ?p\n" +
//				"WHERE {\n" +
//				"  ?p rdf:type ex:Person .\n" +
//				"  FILTER (NOT EXISTS { ?x rdf:type ex:Dragon . })\n" +
//				"}";
//		// No shared vars; must not rewrite to MINUS.
//		String after = before;
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
//
//	@Test
//	@Disabled
//	public void testExistsRewrite_Safe_existsToJoinWithDistinct() {
//		String before = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT ?o WHERE {\n"
//				+ "  ?o ex:customer ?c .\n"
//				+ "  FILTER EXISTS { ?o ex:item ?i }\n"
//				+ "}";
//		String after = ""
//				+ "PREFIX ex: <http://ex/>\n"
//				+ "SELECT DISTINCT ?o WHERE {\n"
//				+ "  ?o ex:customer ?c ; ex:item ?i .\n"
//				+ "}";
//		assertThat(renderOptimized(before)).isEqualToNormalizingNewlines(after);
//	}
}
