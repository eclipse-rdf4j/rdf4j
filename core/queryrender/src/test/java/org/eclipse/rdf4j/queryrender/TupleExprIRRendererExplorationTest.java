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

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
 * Exploration tests: parse selected SPARQL queries, dump their TupleExpr, convert to IR and dump the IR, render back to
 * SPARQL, and dump the rendered TupleExpr. Artifacts are written to surefire-reports for inspection.
 *
 * These tests are intentionally permissive (no strict textual assertions) and are meant to aid root-cause analysis and
 * to stabilize future transforms.
 */
public class TupleExprIRRendererExplorationTest {

	private static final String SPARQL_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX ex: <http://ex/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

	private static TupleExprIRRenderer.Config cfg() {
		TupleExprIRRenderer.Config style = new TupleExprIRRenderer.Config();
		style.prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		style.prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		style.prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
		style.prefixes.put("ex", "http://ex/");
		style.prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		style.valuesPreserveOrder = true;
		// Enable IR debug prints to stdout for additional context during runs
		style.debugIR = true;
		return style;
	}

	private static TupleExpr parseAlgebra(String sparql) {
		try {
			ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, sparql, null);
			return pq.getTupleExpr();
		} catch (MalformedQueryException e) {
			throw new MalformedQueryException(
					"Failed to parse SPARQL query.\n###### QUERY ######\n" + sparql + "\n\n######################",
					e);
		}
	}

	private static void writeReportFile(String base, String label, String content) {
		Path dir = Paths.get("target", "surefire-reports");
		try {
			Files.createDirectories(dir);
			Path file = dir.resolve(base + "_" + label + ".txt");
			Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
			System.out.println("[explore] wrote " + file.toAbsolutePath());
		} catch (IOException ioe) {
			System.err.println("[explore] Failed to write " + label + ": " + ioe);
		}
	}

	private static void dump(String baseName, String body, TupleExprIRRenderer.Config style) {
		// 1) Original SPARQL + TupleExpr
		String input = SPARQL_PREFIX + body;
		TupleExpr te = parseAlgebra(input);
		assertNotNull(te);

		// 2) IR (transformed) via converter
		TupleExprIRRenderer renderer = new TupleExprIRRenderer(style);
		TupleExprToIrConverter conv = new TupleExprToIrConverter(renderer);
		IrSelect ir = conv.toIRSelect(te);

		// 3) Render back to SPARQL
		String rendered = renderer.render(te, null).trim();

		// 4) Parse rendered TupleExpr for comparison reference
		TupleExpr teRendered;
		try {
			teRendered = parseAlgebra(rendered);
		} catch (Throwable t) {
			teRendered = null;
		}

		// 5) Write artifacts
		writeReportFile(baseName, "SPARQL_input", input);
		writeReportFile(baseName, "TupleExpr_input", VarNameNormalizer.normalizeVars(te.toString()));
		writeReportFile(baseName, "IR_transformed", IrDebug.dump(ir));
		writeReportFile(baseName, "SPARQL_rendered", rendered);
		writeReportFile(baseName, "TupleExpr_rendered",
				teRendered != null ? VarNameNormalizer.normalizeVars(teRendered.toString())
						: "<rendered parse failed>\n" + rendered);
	}

	private static String render(String body, TupleExprIRRenderer.Config style) {
		TupleExpr te = parseAlgebra(SPARQL_PREFIX + body);
		return new TupleExprIRRenderer(style).render(te, null).trim();
	}

	private static String algebra(String sparql) {
		TupleExpr te = parseAlgebra(sparql);
		return VarNameNormalizer.normalizeVars(te.toString());
	}

	// Optional helper left in place for local checks; not used in exploratory tests
	private static void assertSemanticRoundTrip(String body) {
	}

	@Test
	@DisplayName("Explore: SERVICE body with UNION of bare NPS")
	void explore_serviceUnionBareNps() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      { ?s !ex:pA ?o . } UNION { ?o !<http://example.org/p/I1> ?s . }\n" +
				"    }\n" +
				"  }\n" +
				"}";
		dump("Exploration_serviceUnionBareNps", q, cfg());
		// Exploratory: artifacts only; no strict assertions
	}

	@Test
	@DisplayName("Explore: SERVICE + GRAPH branches with NPS UNION")
	void explore_serviceGraphUnionBareNps() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      { GRAPH <http://graphs.example/g0> { ?s !ex:pA ?o . } } UNION { GRAPH <http://graphs.example/g0> { ?o !<http://example.org/p/I1> ?s . } }\n"
				+
				"    }\n" +
				"  }\n" +
				"}";
		dump("Exploration_serviceGraphUnionBareNps", q, cfg());
		// Exploratory: artifacts only; no strict assertions
	}

	@Test
	@DisplayName("Explore: SERVICE + VALUES/MINUS with NPS UNION")
	void explore_serviceValuesMinusUnionBareNps() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      { VALUES ?s { ex:s1 ex:s2 } { ?s ex:pB ?v0 . MINUS { { ?s !ex:pA ?o . } UNION { ?o !foaf:knows ?s . } } } }\n"
				+
				"    }\n" +
				"  }\n" +
				"}";
		dump("Exploration_serviceValuesMinusUnionBareNps", q, cfg());
		// Exploratory: artifacts only; no strict assertions
	}

	@Test
	@DisplayName("Explore: nested SELECT with SERVICE + single path")
	void explore_nestedSelectServiceSinglePath() {
		String q = "SELECT ?s WHERE {\n" +
				"  { SELECT ?s WHERE {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      { ?s ex:pZ ?o . }\n" +
				"    }\n" +
				"  } }\n" +
				"}";
		dump("Exploration_nestedSelectServiceSinglePath", q, cfg());
	}

	@Test
	@DisplayName("Explore: FILTER EXISTS with GRAPH/OPTIONAL and NPS")
	void explore_filterExistsGraphOptionalNps() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  GRAPH <http://graphs.example/g1> { ?s ex:pC ?u1 . }\n" +
				"  FILTER EXISTS { { GRAPH <http://graphs.example/g1> { ?s ex:pA ?o . } OPTIONAL { GRAPH <http://graphs.example/g1> { ?s !(<http://example.org/p/I0>) ?o . } } } }\n"
				+
				"}";
		dump("Exploration_filterExistsGraphOptionalNps", q, cfg());
	}
}
