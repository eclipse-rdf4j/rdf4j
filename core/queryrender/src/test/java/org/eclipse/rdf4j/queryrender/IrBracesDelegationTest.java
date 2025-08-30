/**
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

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.junit.jupiter.api.Test;

/**
 * Focused tests to lock-in brace delegation rules: IrBGP owns curly braces and container nodes delegate to it.
 */
public class IrBracesDelegationTest {

	private static final String SPARQL_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX ex: <http://ex/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

	private TupleExpr parse(String sparql) {
		try {
			ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, sparql, null);
			return pq.getTupleExpr();
		} catch (MalformedQueryException e) {
			throw new MalformedQueryException("Failed to parse SPARQL:\n" + sparql, e);
		}
	}

	private TupleExprIRRenderer.Config cfg() {
		TupleExprIRRenderer.Config c = new TupleExprIRRenderer.Config();
		c.prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		c.prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		c.prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
		c.prefixes.put("ex", "http://ex/");
		c.prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		c.valuesPreserveOrder = true;
		return c;
	}

	@Test
	void exists_mixed_body_preserves_inner_group() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  FILTER EXISTS {\n" +
				"    ?s ex:p ?o .\n" +
				"    FILTER EXISTS { ?s ex:q ?o . }\n" +
				"  }\n" +
				"}";

		String expected = SPARQL_PREFIX +
				"SELECT ?s ?o WHERE {\n" +
				"  FILTER EXISTS {\n" +
				"    {\n" +
				"      ?s ex:p ?o .\n" +
				"      FILTER EXISTS {\n" +
				"        ?s ex:q ?o .\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		TupleExprIRRenderer r = new TupleExprIRRenderer(cfg());
		String rendered = r.render(parse(SPARQL_PREFIX + q), null).trim();
		assertThat(rendered).isEqualToNormalizingNewlines(expected);
	}

	@Test
	void union_branches_have_single_brace_each() {
		String q = "SELECT ?x WHERE {\n" +
				"  { ?x a ex:Thing . }\n" +
				"    UNION\n" +
				"  { ?x foaf:name ?n . }\n" +
				"}";

		String expected = SPARQL_PREFIX +
				"SELECT ?x WHERE {\n" +
				"  {\n" +
				"    ?x a ex:Thing .\n" +
				"  }\n" +
				"  UNION\n" +
				"  {\n" +
				"    ?x foaf:name ?n .\n" +
				"  }\n" +
				"}";

		TupleExprIRRenderer r = new TupleExprIRRenderer(cfg());
		String rendered = r.render(parse(SPARQL_PREFIX + q), null).trim();
		assertThat(rendered).isEqualToNormalizingNewlines(expected);
	}
}
