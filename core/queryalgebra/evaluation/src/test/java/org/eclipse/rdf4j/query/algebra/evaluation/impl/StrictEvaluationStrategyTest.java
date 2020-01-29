/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.Before;
import org.junit.Test;

public class StrictEvaluationStrategyTest {

	private EvaluationStrategy strategy;

	@Before
	public void setUp() throws Exception {
		strategy = new StrictEvaluationStrategy(new EmptyTripleSource(), null);
	}

	/**
	 * Verifies if only those input bindings that actually occur in the query are returned in the result. See SES-2373.
	 */
	@Test
	public void testBindings() throws Exception {
		String query = "SELECT ?a ?b WHERE {}";
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		final ValueFactory vf = SimpleValueFactory.getInstance();
		QueryBindingSet constants = new QueryBindingSet();
		constants.addBinding("a", vf.createLiteral("foo"));
		constants.addBinding("b", vf.createLiteral("bar"));
		constants.addBinding("x", vf.createLiteral("X"));
		constants.addBinding("y", vf.createLiteral("Y"));

		CloseableIteration<BindingSet, QueryEvaluationException> result = strategy.evaluate(pq.getTupleExpr(),
				constants);
		assertNotNull(result);
		assertTrue(result.hasNext());
		BindingSet bs = result.next();
		assertTrue(bs.hasBinding("a"));
		assertTrue(bs.hasBinding("b"));
		assertFalse(bs.hasBinding("x"));
		assertFalse(bs.hasBinding("y"));
	}

	@Test
	public void testOptimize() throws Exception {

		QueryOptimizer optimizer1 = mock(QueryOptimizer.class);
		QueryOptimizer optimizer2 = mock(QueryOptimizer.class);

		strategy.setOptimizerPipeline(() -> Arrays.asList(optimizer1, optimizer2));

		TupleExpr expr = mock(TupleExpr.class);
		EvaluationStatistics stats = new EvaluationStatistics();
		BindingSet bindings = new QueryBindingSet();

		strategy.optimize(expr, stats, bindings);
		verify(optimizer1, times(1)).optimize(expr, null, bindings);
		verify(optimizer2, times(1)).optimize(expr, null, bindings);
	}
}
