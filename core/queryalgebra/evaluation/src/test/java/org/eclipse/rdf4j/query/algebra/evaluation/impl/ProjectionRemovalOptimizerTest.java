/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.ProjectionRemovalOptimizer;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Test;

public class ProjectionRemovalOptimizerTest {

	@Test
	public void testRemovingOptimization() throws RDF4JException {
		String query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o }";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		TupleExpr original = pq.getTupleExpr();

		TupleExpr optimized = original.clone();
		new ProjectionRemovalOptimizer().optimize(optimized, null, null);

		assertNotEquals(original, optimized);
	}

	@Test
	public void testRemovingOptimizationSubselect() throws RDF4JException {
		String query = "SELECT ?s ?p ?o WHERE { SELECT * WHERE { ?s ?p ?o }}";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		TupleExpr original = pq.getTupleExpr();

		TupleExpr optimized = original.clone();
		new ProjectionRemovalOptimizer().optimize(optimized, null, null);

		assertNotEquals(original, optimized);
		assertTrue(optimized instanceof QueryRoot);
		TupleExpr child = ((QueryRoot) optimized).getArg();
		assertTrue(child instanceof StatementPattern);
	}

	@Test
	public void testRemovingHalfOptimizationSubselect() throws RDF4JException {
		String query = "SELECT ?s ?p WHERE { SELECT * WHERE { ?s ?p ?o , ?o2 }}";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		TupleExpr original = pq.getTupleExpr();

		TupleExpr optimized = original.clone();
		new ProjectionRemovalOptimizer().optimize(optimized, null, null);

		assertNotEquals(original, optimized);
		assertTrue(optimized instanceof QueryRoot);
		TupleExpr child = ((QueryRoot) optimized).getArg();
		assertTrue(child instanceof Projection);
		TupleExpr grandChild = ((Projection) child).getArg();
		assertTrue(grandChild instanceof Join);
	}

	@Test
	public void testNotRemovingOptimizationTooFew() throws RDF4JException {
		String query = "SELECT ?s ?p WHERE { ?s ?p ?o }";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		TupleExpr original = pq.getTupleExpr();

		TupleExpr optimized = original.clone();
		new ProjectionRemovalOptimizer().optimize(optimized, null, null);

		assertEquals(original, optimized);
	}

	@Test
	public void testNotRemovingOptimizationTooMany() throws RDF4JException {
		String query = "SELECT ?s ?p ?o ?o2 WHERE { ?s ?p ?o }";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		TupleExpr original = pq.getTupleExpr();

		TupleExpr optimized = original.clone();
		new ProjectionRemovalOptimizer().optimize(optimized, null, null);

		assertEquals(original, optimized);
	}

	@Test
	public void testNotRemovingOptimization2() throws RDF4JException {
		String query = "SELECT (?s as ?o2) (?p as ?s1) ?o WHERE { ?s ?p ?o }";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		TupleExpr original = pq.getTupleExpr();

		TupleExpr optimized = original.clone();
		new ProjectionRemovalOptimizer().optimize(optimized, null, null);

		assertEquals(original, optimized);
	}
}
