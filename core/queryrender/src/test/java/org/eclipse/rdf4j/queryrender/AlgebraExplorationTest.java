/*
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.rdf4j.queryrender;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.junit.jupiter.api.Test;

/**
 * Ad-hoc exploration tests to inspect the TupleExpr (algebra) RDF4J produces for various SPARQL constructs. These tests
 * intentionally do not assert, they print the algebra and the re-rendered query (with IR debug enabled on failure in
 * other tests).
 */
public class AlgebraExplorationTest {

	private static final String SPARQL_PREFIX = "BASE <http://ex/>\n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX ex: <http://ex/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

	private static TupleExpr parseAlgebra(String sparql) {
		try {
			ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, sparql, null);
			return pq.getTupleExpr();
		} catch (MalformedQueryException e) {
			String msg = "Failed to parse SPARQL query.\n" +
					"###### QUERY ######\n" + sparql + "\n\n######################";
			throw new MalformedQueryException(msg, e);
		}
	}

	private static TupleExprIRRenderer.Config cfg() {
		TupleExprIRRenderer.Config style = new TupleExprIRRenderer.Config();
		style.prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		style.prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		style.prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
		style.prefixes.put("ex", "http://ex/");
		style.prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		style.valuesPreserveOrder = true;
		style.debugIR = true;
		return style;
	}

	@Test
	void explore_service_graph_nested_1() {
		String q = SPARQL_PREFIX +
				"SELECT ?s ?o WHERE {\n" +
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

		TupleExpr te = parseAlgebra(q);
		System.out.println("\n# EXPLORE: SERVICE + nested GRAPH (1)\n\n# SPARQL\n" + q);
		System.out.println("\n# Algebra\n" + te + "\n");
		String rendered = new TupleExprIRRenderer(cfg()).render(te, null).trim();
		System.out.println("# Rendered\n" + rendered + "\n");
	}

	@Test
	void explore_service_graph_nested_2() {
		String q = SPARQL_PREFIX +
				"SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      {\n" +
				"        GRAPH ?g1 {\n" +
				"          {\n" +
				"            GRAPH <http://graphs.example/g1> {\n" +
				"              ?s !(ex:pA|^<http://example.org/p/I1>) ?o . \n" +
				"            }\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		TupleExpr te = parseAlgebra(q);
		System.out.println("\n# EXPLORE: SERVICE + nested GRAPH (2)\n\n# SPARQL\n" + q);
		System.out.println("\n# Algebra\n" + te + "\n");
		String rendered = new TupleExprIRRenderer(cfg()).render(te, null).trim();
		System.out.println("# Rendered\n" + rendered + "\n");
	}

	@Test
	void explore_service_values_minus_fuse_nps_union() {
		String q = SPARQL_PREFIX +
				"SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SERVICE SILENT <http://federation.example/ep> {\n" +
				"      {\n" +
				"        VALUES ?s { ex:s1 ex:s2 }\n" +
				"        { ?s ex:pB ?v0 . MINUS { ?s !(ex:pA|^foaf:knows) ?o . } }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}\n";

		TupleExpr te = parseAlgebra(q);
		System.out.println("\n# EXPLORE: SERVICE + VALUES + MINUS (NPS union)\n\n# SPARQL\n" + q);
		System.out.println("\n# Algebra\n" + te + "\n");
		String rendered = new TupleExprIRRenderer(cfg()).render(te, null).trim();
		System.out.println("# Rendered\n" + rendered + "\n");
	}
}
