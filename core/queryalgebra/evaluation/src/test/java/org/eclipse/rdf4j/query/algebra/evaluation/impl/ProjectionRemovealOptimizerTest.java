/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Test;

/**
 */
public class ProjectionRemovealOptimizerTest {

	@Test
	public void testRemovingOptimization() throws RDF4JException {
		String query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o }";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		TupleExpr original = pq.getTupleExpr();

		TupleExpr optimized = original.clone();
		new ProjectionRemovealOptimizer().optimize(optimized, null, null);

		assertNotEquals(original, optimized);
	}

	@Test
	public void testNotRemovingOptimization() throws RDF4JException {
		String query = "SELECT ?s ?p WHERE { ?s ?p ?o }";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		TupleExpr original = pq.getTupleExpr();

		TupleExpr optimized = original.clone();
		new ProjectionRemovealOptimizer().optimize(optimized, null, null);

		assertEquals(original, optimized);
	}

	@Test
	public void testNotRemovingOptimization2() throws RDF4JException {
		String query = "SELECT (?s as ?o2) (?p as ?s1) ?o WHERE { ?s ?p ?o }";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		TupleExpr original = pq.getTupleExpr();

		TupleExpr optimized = original.clone();
		new ProjectionRemovealOptimizer().optimize(optimized, null, null);

		assertEquals(original, optimized);
	}
}
