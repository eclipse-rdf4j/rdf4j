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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.Before;
import org.junit.Test;

public class SimpleEvaluationStrategyTest {

	private EvaluationStrategy strategy;

	@Before
	public void setUp()
		throws Exception
	{
		strategy = new SimpleEvaluationStrategy(new EmptyTripleSource(), null);
	}

	/**
	 * Verifies if only those input bindings that actually occur in the query are returned in the result. See
	 * SES-2373.
	 */
	@Test
	public void testBindings()
		throws Exception
	{
		String query = "SELECT ?a ?b WHERE {}";
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		QueryBindingSet constants = new QueryBindingSet();
		constants.addBinding("a", ValueFactoryImpl.getInstance().createLiteral("foo"));
		constants.addBinding("b", ValueFactoryImpl.getInstance().createLiteral("bar"));
		constants.addBinding("x", ValueFactoryImpl.getInstance().createLiteral("X"));
		constants.addBinding("y", ValueFactoryImpl.getInstance().createLiteral("Y"));

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

}
