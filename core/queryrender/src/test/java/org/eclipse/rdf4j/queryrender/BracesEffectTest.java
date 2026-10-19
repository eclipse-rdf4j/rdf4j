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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.eclipse.rdf4j.queryrender.sparql.TupleExprToIrConverter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.IrDebug;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests to explore how adding extra curly braces around various parts of a query affects the RDF4J TupleExpr and our
 * IR, and which brace placements are semantically neutral (produce identical TupleExpr structures).
 */
public class BracesEffectTest {

	private static final String SPARQL_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX ex: <http://ex/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

	private static TupleExpr parse(String sparql) {
		try {
			ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, sparql, null);
			return pq.getTupleExpr();
		} catch (MalformedQueryException e) {
			throw new MalformedQueryException("Failed to parse SPARQL query\n" + sparql, e);
		}
	}

	private static String algebra(String sparql) {
		return VarNameNormalizer.normalizeVars(parse(sparql).toString());
	}

	private static TupleExprIRRenderer.Config cfg() {
		TupleExprIRRenderer.Config c = new TupleExprIRRenderer.Config();
		c.prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		c.prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		c.prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
		c.prefixes.put("ex", "http://ex/");
		c.prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		return c;
	}

	private static void write(String base, String label, String text) {
		Path dir = Paths.get("target", "surefire-reports");
		try {
			Files.createDirectories(dir);
			Files.writeString(dir.resolve(base + "_" + label + ".txt"), text, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// ignore in tests
		}
	}

	private static void dumpIr(String base, String body) {
		TupleExprIRRenderer r = new TupleExprIRRenderer(cfg());
		TupleExpr te = parse(SPARQL_PREFIX + body);
		IrSelect ir = new TupleExprToIrConverter(r).toIRSelect(te);
		write(base, "IR", IrDebug.dump(ir));
	}

	private static String render(String body) {
		TupleExprIRRenderer r = new TupleExprIRRenderer(cfg());
		TupleExpr te = parse(SPARQL_PREFIX + body);
		return r.render(te, null).trim();
	}

	private static String stripScopeMarkers(String algebraDump) {
		if (algebraDump == null) {
			return null;
		}
		// Remove RDF4J pretty-printer markers indicating explicit variable-scope changes
		return algebraDump.replace(" (new scope)", "");
	}

	private static void assertSemanticRoundTrip(String base, String body) {
		String input = SPARQL_PREFIX + body;
		String aIn = stripScopeMarkers(algebra(input));
		String rendered = render(body);
		String aOut = stripScopeMarkers(algebra(rendered));
		write(base, "Rendered", rendered);
		write(base, "TupleExpr_input", aIn);
		write(base, "TupleExpr_rendered", aOut);
		assertEquals(aIn, aOut, "Renderer must preserve semantics (algebra equal)");
	}

	private static void compareAndDump(String baseName, String q1, String q2) {
		String a1 = algebra(SPARQL_PREFIX + q1);
		String a2 = algebra(SPARQL_PREFIX + q2);
		write(baseName, "TupleExpr_1", a1);
		write(baseName, "TupleExpr_2", a2);
		String verdict = a1.equals(a2) ? "EQUAL" : "DIFFERENT";
		write(baseName, "TupleExpr_verdict", verdict);
		// Also dump IR for both variants to inspect newScope/grouping differences if any
		dumpIr(baseName + "_1", q1);
		dumpIr(baseName + "_2", q2);
		// Additionally, assert renderer round-trip preserves semantics for both variants
		assertSemanticRoundTrip(baseName + "_rt1", q1);
		assertSemanticRoundTrip(baseName + "_rt2", q2);
	}

	@Test
	@DisplayName("Braces around single triple in WHERE")
	void bracesAroundBGP_noEffect() {
		String q1 = "SELECT ?s ?o WHERE { ?s ex:pA ?o . }";
		String q2 = "SELECT ?s ?o WHERE { { ?s ex:pA ?o . } }";
		compareAndDump("Braces_BGP", q1, q2);
	}

	@Test
	@DisplayName("Double braces around single triple")
	void doubleBracesAroundBGP_noEffect() {
		String q1 = "SELECT ?s ?o WHERE { ?s ex:pA ?o . }";
		String q2 = "SELECT ?s ?o WHERE { { { ?s ex:pA ?o . } } }";
		compareAndDump("Braces_BGP_Double", q1, q2);
	}

	@Test
	@DisplayName("Braces inside GRAPH body")
	void bracesInsideGraph_noEffect() {
		String q1 = "SELECT ?s ?o WHERE { GRAPH <http://graphs.example/g0> { ?s ex:pA ?o . } }";
		String q2 = "SELECT ?s ?o WHERE { GRAPH <http://graphs.example/g0> { { ?s ex:pA ?o . } } }";
		compareAndDump("Braces_GRAPH", q1, q2);
	}

	@Test
	@DisplayName("Braces inside SERVICE body")
	void bracesInsideService_noEffect() {
		String q1 = "SELECT ?s ?o WHERE { SERVICE SILENT <http://federation.example/ep> { ?s ex:pA ?o . } }";
		String q2 = "SELECT ?s ?o WHERE { SERVICE SILENT <http://federation.example/ep> { { ?s ex:pA ?o . } } }";
		compareAndDump("Braces_SERVICE", q1, q2);
	}

	@Test
	@DisplayName("Braces inside MINUS body")
	void bracesInsideMinus_noEffect() {
		String q1 = "SELECT ?s ?o WHERE { ?s ex:pA ?o . MINUS { ?o ex:pB ?x . } }";
		String q2 = "SELECT ?s ?o WHERE { ?s ex:pA ?o . MINUS { { ?o ex:pB ?x . } } }";
		compareAndDump("Braces_MINUS", q1, q2);
	}

	@Test
	@DisplayName("Braces around UNION branches")
	void bracesAroundUnionBranches_noEffect() {
		String q1 = "SELECT ?s ?o WHERE { { ?s ex:pA ?o . } UNION { ?o ex:pB ?s . } }";
		String q2 = "SELECT ?s ?o WHERE { { { ?s ex:pA ?o . } } UNION { { ?o ex:pB ?s . } } }";
		compareAndDump("Braces_UNION_Branches", q1, q2);
	}

	@Test
	@DisplayName("Braces inside FILTER EXISTS body")
	void bracesInsideExists_noEffect() {
		String q1 = "SELECT ?s ?o WHERE { ?s ex:pA ?o . FILTER EXISTS { ?o ex:pB ?x . } }";
		String q2 = "SELECT ?s ?o WHERE { ?s ex:pA ?o . FILTER EXISTS { { ?o ex:pB ?x . } } }";
		compareAndDump("Braces_EXISTS", q1, q2);
	}

	@Test
	@DisplayName("FILTER EXISTS with GRAPH + OPTIONAL NPS: brace vs no-brace body")
	void bracesInsideExists_graphOptionalNps_compare() {
		// With extra curly brackets inside FILTER EXISTS
		String q1 = "SELECT ?s ?o WHERE {\n" +
				"  GRAPH <http://graphs.example/g1> {\n" +
				"    ?s ex:pC ?u1 . \n" +
				"    FILTER EXISTS {\n" +
				"      {\n" +
				"        ?s ex:pA ?o . OPTIONAL {\n" +
				"          ?s !<http://example.org/p/I0> ?o .\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		// Without those extra curly brackets (same content, no inner grouping)
		String q2 = "SELECT ?s ?o WHERE {\n" +
				"  GRAPH <http://graphs.example/g1> {\n" +
				"    ?s ex:pC ?u1 . \n" +
				"    FILTER EXISTS {\n" +
				"        ?s ex:pA ?o . OPTIONAL {\n" +
				"          ?s !<http://example.org/p/I0> ?o .\n" +
				"        }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		compareAndDump("Braces_EXISTS_GraphOptionalNPS", q1, q2);
	}

	@Test
	@DisplayName("Braces around VALUES group")
	void bracesAroundValues_noEffect() {
		String q1 = "SELECT ?s WHERE { VALUES ?s { ex:s1 ex:s2 } ?s ex:pA ex:o . }";
		String q2 = "SELECT ?s WHERE { { VALUES ?s { ex:s1 ex:s2 } } ?s ex:pA ex:o . }";
		compareAndDump("Braces_VALUES", q1, q2);
	}
}
