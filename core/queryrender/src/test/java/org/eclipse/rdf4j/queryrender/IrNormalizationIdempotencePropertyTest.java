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
package org.eclipse.rdf4j.queryrender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.junit.jupiter.api.Test;

/**
 * Metamorphic tests: IR normalization + rendering is idempotent across representative families of queries.
 *
 * Property: render(parse(render(x))) == render(x)
 */
public class IrNormalizationIdempotencePropertyTest {

	private static final String SPARQL_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "PREFIX ex: <http://ex/>\n"
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

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

	private static Stream<String> queries() {
		return Stream.of(
				// BGP
				"SELECT ?s ?n WHERE { ?s foaf:name ?n . }",
				// OPTIONAL with filter on left var
				"SELECT ?s ?age WHERE { ?s foaf:name ?n . OPTIONAL { ?s ex:age ?age . FILTER (?age >= 18) } }",
				// UNION of simple branches
				"SELECT ?who WHERE { { ?who foaf:name \"Alice\" . } UNION { ?who foaf:name \"Bob\" . } }",
				// VALUES single var
				"SELECT ?x WHERE { VALUES (?x) { (ex:a) (UNDEF) (ex:b) } ?x foaf:name ?n . }",
				// ORDER + LIMIT/OFFSET
				"SELECT ?n WHERE { ?s foaf:name ?n . } ORDER BY DESC(?n) LIMIT 2 OFFSET 0",
				// GRAPH + OPTIONAL in body
				"SELECT ?g ?s WHERE { GRAPH ?g { ?s a foaf:Person . } OPTIONAL { ?s rdfs:label ?l . } }"
		);
	}

	@Test
	void render_is_idempotent_across_families() {
		TupleExprIRRenderer r = new TupleExprIRRenderer(cfg());
		queries().forEach(q -> {
			String r1 = r.render(parse(SPARQL_PREFIX + q));
			String r2 = r.render(parse(r1));
			assertEquals(r1, r2, "Renderer must be idempotent for query: " + q);
		});
	}

	private static TupleExpr parse(String sparql) {
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, sparql, null);
		return pq.getTupleExpr();
	}
}
