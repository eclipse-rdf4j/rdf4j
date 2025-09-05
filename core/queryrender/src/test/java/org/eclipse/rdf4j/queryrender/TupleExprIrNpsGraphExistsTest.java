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

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.junit.jupiter.api.Test;

/**
 * Focused regression harness around GRAPH + EXISTS + negated property set fusion to capture the exact algebra delta
 * without System.exit side effects.
 */
public class TupleExprIrNpsGraphExistsTest {

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
			String msg = "Failed to parse SPARQL query.\n"
					+ "###### QUERY ######\n"
					+ sparql
					+ "\n\n######################";
			throw new MalformedQueryException(msg, e);
		}
	}

	@Test
	void values_plus_group_with_filter_exists_inverse_roundtrip() {
		String q = SPARQL_PREFIX +
				"SELECT ?s ?o WHERE {\n" +
				"{ VALUES ?s { ex:s1 ex:s2 } { ?s ex:pC ?u0 . FILTER EXISTS { ?s ^<http://example.org/p/I1> ?o . } } }\n"
				+
				"}";

		TupleExpr expected = parseAlgebra(q);

		TupleExprIRRenderer.Config c = cfg();
		c.debugIR = true; // ensure IR dump if mismatch
		String rendered = new TupleExprIRRenderer(c).render(parseAlgebra(q), null).trim();

		TupleExpr actual = parseAlgebra(rendered);

		String normExpected = VarNameNormalizer.normalizeVars(expected.toString());
		String normActual = VarNameNormalizer.normalizeVars(actual.toString());

		if (!normActual.equals(normExpected)) {
			System.out.println("\n# Original SPARQL\n" + q);
			System.out.println("\n# Rendered SPARQL\n" + rendered);
			System.out.println("\n# Expected Algebra (normalized)\n" + normExpected);
			System.out.println("\n# Actual Algebra (normalized)\n" + normActual);
		}

		assertThat(normActual)
				.as("Rendered algebra should match original algebra (normalized)")
				.isEqualTo(normExpected);
	}

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

	@Test
	void values_plus_graph_roundtrip() {
		String q = SPARQL_PREFIX +
				"SELECT ?g WHERE {\n" +
				"  VALUES ?g { <http://graphs.example/g0> <http://graphs.example/g1> }\n" +
				"  GRAPH ?g { ?s ?p ?o }\n" +
				"}";

		TupleExpr expected = parseAlgebra(q);
		String rendered = new TupleExprIRRenderer(cfg()).render(parseAlgebra(q), null).trim();
		TupleExpr actual = parseAlgebra(rendered);
		String normExpected = VarNameNormalizer.normalizeVars(expected.toString());
		String normActual = VarNameNormalizer.normalizeVars(actual.toString());
		if (!normActual.equals(normExpected)) {
			System.out.println("\n# Original SPARQL\n" + q);
			System.out.println("\n# Rendered SPARQL\n" + rendered);
			System.out.println("\n# Expected Algebra (normalized)\n" + normExpected);
			System.out.println("\n# Actual Algebra (normalized)\n" + normActual);
		}
		assertThat(normActual)
				.as("Rendered algebra should match original algebra (normalized)")
				.isEqualTo(normExpected);
	}

	@Test
	void graph_exists_nps_roundtrip() {
		String q = SPARQL_PREFIX +
				"SELECT ?s ?o WHERE {\n" +
				"{ ?s ex:pC ?u1 . FILTER EXISTS { { GRAPH <http://graphs.example/g1> { ?s !(ex:pA|^ex:pD) ?o . } } } }\n"
				+
				"}";

		TupleExpr expected = parseAlgebra(q);

		String rendered = new TupleExprIRRenderer(cfg()).render(parseAlgebra(q), null).trim();

		TupleExpr actual = parseAlgebra(rendered);

		String normExpected = VarNameNormalizer.normalizeVars(expected.toString());
		String normActual = VarNameNormalizer.normalizeVars(actual.toString());

		// Help debugging locally if this diverges
		if (!normActual.equals(normExpected)) {
			System.out.println("\n# Original SPARQL\n" + q);
			System.out.println("\n# Rendered SPARQL\n" + rendered);
			System.out.println("\n# Expected Algebra (normalized)\n" + normExpected);
			System.out.println("\n# Actual Algebra (normalized)\n" + normActual);
		}

		assertThat(normActual)
				.as("Rendered algebra should match original algebra (normalized)")
				.isEqualTo(normExpected);
	}

	@Test
	void graph_optional_inverse_tail_roundtrip() {
		String q = SPARQL_PREFIX +
				"SELECT ?s ?o WHERE {\n" +
				"{ GRAPH ?g1 { { ?s ex:pA ?o . OPTIONAL { ?s ^ex:pA ?o . } } } }\n" +
				"}";

		TupleExpr expected = parseAlgebra(q);

		String rendered = new TupleExprIRRenderer(cfg()).render(parseAlgebra(q), null).trim();

		TupleExpr actual = parseAlgebra(rendered);

		String normExpected = VarNameNormalizer.normalizeVars(expected.toString());
		String normActual = VarNameNormalizer.normalizeVars(actual.toString());

		if (!normActual.equals(normExpected)) {
			System.out.println("\n# Original SPARQL\n" + q);
			System.out.println("\n# Rendered SPARQL\n" + rendered);
			System.out.println("\n# Expected Algebra (normalized)\n" + normExpected);
			System.out.println("\n# Actual Algebra (normalized)\n" + normActual);
		}

		assertThat(normActual)
				.as("Rendered algebra should match original algebra (normalized)")
				.isEqualTo(normExpected);
	}
}
