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

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.helpers.QueryModelVisitorBase;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.Test;

/**
 */
public class ConstantOptimizerTest {

	@Test
	public void testAndOptimization() throws RDF4JException {
		String query = "prefix ex: <ex:>" + "select ?a ?b ?c\n" + "where {\n" + " bind((?a && ?b) as ?c) \n" + "}";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		TupleExpr original = pq.getTupleExpr();

		final AlgebraFinder finder = new AlgebraFinder();
		original.visit(finder);
		assertTrue(finder.logicalAndfound);

		// reset for re-use on optimized query
		finder.reset();

		QueryBindingSet constants = new QueryBindingSet();
		constants.addBinding("a", BooleanLiteral.TRUE);
		constants.addBinding("b", BooleanLiteral.FALSE);

		EvaluationStrategy strategy = new StrictEvaluationStrategy(new EmptyTripleSource(), null);
		TupleExpr optimized = optimize(pq.getTupleExpr().clone(), constants, strategy);

		optimized.visit(finder);
		assertFalse("optimized query should no longer contain && operator", finder.logicalAndfound);

		CloseableIteration<BindingSet, QueryEvaluationException> result = strategy.evaluate(optimized,
				new EmptyBindingSet());
		assertNotNull(result);
		assertTrue(result.hasNext());
		BindingSet bindings = result.next();
		assertTrue(bindings.hasBinding("a"));
		assertTrue(bindings.hasBinding("b"));
		assertTrue(bindings.hasBinding("c"));
	}

	@Test
	public void testFunctionOptimization() throws RDF4JException {
		String query = "prefix ex: <ex:>" + "select ?a ?b ?c \n " + "where {\n" + " bind(concat(?a, ?b) as ?c) \n"
				+ "}";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
		EvaluationStrategy strategy = new StrictEvaluationStrategy(new EmptyTripleSource(), null);
		TupleExpr original = pq.getTupleExpr();

		final AlgebraFinder finder = new AlgebraFinder();
		original.visit(finder);
		assertTrue(finder.functionCallFound);

		// reset for re-use on optimized query
		finder.reset();

		QueryBindingSet constants = new QueryBindingSet();
		constants.addBinding("a", ValueFactoryImpl.getInstance().createLiteral("foo"));
		constants.addBinding("b", ValueFactoryImpl.getInstance().createLiteral("bar"));

		TupleExpr optimized = optimize(pq.getTupleExpr().clone(), constants, strategy);

		optimized.visit(finder);
		assertFalse("optimized query should no longer contain function call", finder.functionCallFound);

		CloseableIteration<BindingSet, QueryEvaluationException> result = strategy.evaluate(optimized,
				new EmptyBindingSet());
		assertNotNull(result);
		assertTrue(result.hasNext());
		BindingSet bindings = result.next();
		assertTrue(bindings.hasBinding("a"));
		assertTrue(bindings.hasBinding("b"));
		assertTrue(bindings.hasBinding("c"));

	}

	private class AlgebraFinder extends QueryModelVisitorBase<RuntimeException> {

		public boolean logicalAndfound = false;

		public boolean functionCallFound = false;

		@Override
		public void meet(And and) {
			logicalAndfound = true;
			super.meet(and);

		}

		@Override
		public void meet(FunctionCall arg) {
			functionCallFound = true;
			super.meet(arg);
		}

		public void reset() {
			logicalAndfound = false;
			functionCallFound = false;
		}
	}

	private TupleExpr optimize(TupleExpr expr, BindingSet bs, EvaluationStrategy strategy) {
		QueryRoot optRoot = new QueryRoot(expr);
		new BindingAssigner().optimize(optRoot, null, bs);
		new ConstantOptimizer(strategy).optimize(optRoot, null, bs);
		return optRoot;
	}
}
