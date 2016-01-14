/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.BindingAssigner;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ConstantOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SimpleEvaluationStrategy;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.Test;


/**
 */
public class ConstantOptimizerTest {
	@Test
	public void testAndOptimization() throws OpenRDFException {
		String query = "prefix ex: <ex:>"
				+ "select ?a ?b ?c\n"
				+ "where {\n"
				+ " bind((?a && ?b) as ?c) \n"
				+ "}";

		QueryBindingSet bs = new QueryBindingSet();
		bs.addBinding("a", ValueFactoryImpl.getInstance().createLiteral(true));
		bs.addBinding("b", ValueFactoryImpl.getInstance().createLiteral(true));

		testOptimizer(query, bs);
	}

	@Test
	public void testBoundOptimization() throws OpenRDFException {
		String query = "prefix ex: <ex:>"
				+ "select ?a ?c\n"
				+ "where {\n"
				+ " bind(bound(?a) as ?c) \n"
				+ "}";

		QueryBindingSet bs = new QueryBindingSet();
		bs.addBinding("a", ValueFactoryImpl.getInstance().createLiteral("foo"));

		testOptimizer(query, bs);
	}

	@Test
	public void testFunctionOptimization() throws OpenRDFException {
		String query = "prefix ex: <ex:>"
				+ "construct {\n"
				+ "ex:a rdfs:label ?a .\n"
				+ "ex:b rdfs:label ?b .\n"
				+ "ex:c rdfs:label ?c .\n"
				+ "} where {\n"
				+ " bind(concat(?a, ?b) as ?c) \n"
				+ "}";

		QueryBindingSet bs = new QueryBindingSet();
		bs.addBinding("a", ValueFactoryImpl.getInstance().createLiteral("foo"));
		bs.addBinding("b", ValueFactoryImpl.getInstance().createLiteral("bah"));

		testOptimizer(query, bs);
	}

	private void testOptimizer(String query, BindingSet bs)
		throws OpenRDFException
	{
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
		EvaluationStrategy strategy = new SimpleEvaluationStrategy(new EmptyTripleSource(), null);
		TupleExpr opt = optimize(pq.getTupleExpr().clone(), bs, strategy);
		Set<BindingSet> expected = Iterations.asSet(strategy.evaluate(pq.getTupleExpr(), bs));
		Set<BindingSet> actual = Iterations.asSet(strategy.evaluate(opt, EmptyBindingSet.getInstance()));
		assertEquals(expected, actual);
	}

	private TupleExpr optimize(TupleExpr expr, BindingSet bs, EvaluationStrategy strategy)
	{
		QueryRoot optRoot = new QueryRoot(expr);
		new BindingAssigner().optimize(optRoot, null, bs);
		new ConstantOptimizer(strategy).optimize(optRoot, null, bs);
		return optRoot;
	}
}
