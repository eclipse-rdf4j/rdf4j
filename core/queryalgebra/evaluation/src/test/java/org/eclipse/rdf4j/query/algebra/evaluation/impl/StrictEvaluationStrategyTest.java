/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
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

	@Test
	public void testEvaluateRegexFlags() throws Exception {

		String query = "SELECT ?a WHERE { "
				+ "VALUES ?a { \"foo.bar\" \"foo bar\" } \n"
				+ "FILTER REGEX(str(?a), \"foo.bar\")}";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		CloseableIteration<BindingSet, QueryEvaluationException> result = strategy.evaluate(pq.getTupleExpr(),
				new EmptyBindingSet());

		List<BindingSet> bindingSets = QueryResults.asList(result);
		assertThat(bindingSets).hasSize(2);

		// match with q flag
		query = "SELECT ?a WHERE { "
				+ "VALUES ?a { \"foo.bar\" \"foo bar\" } \n"
				+ "FILTER REGEX(str(?a), \"foo.bar\", \"q\")}";

		pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		result = strategy.evaluate(pq.getTupleExpr(),
				new EmptyBindingSet());

		bindingSets = QueryResults.asList(result);
		assertThat(bindingSets).hasSize(1);
		assertThat(bindingSets.get(0).getValue("a").stringValue()).isEqualTo("foo.bar");

		// match with i and q flag
		query = "SELECT ?a WHERE { "
				+ "VALUES ?a { \"foo.bar\" \"FOO.BAR\" \"foo bar\" } \n"
				+ "FILTER REGEX(str(?a), \"foo.bar\", \"iq\")}";

		pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		result = strategy.evaluate(pq.getTupleExpr(),
				new EmptyBindingSet());

		bindingSets = QueryResults.asList(result);
		assertThat(bindingSets).hasSize(2);

		List<String> values = bindingSets.stream().map(v -> v.getValue("a").stringValue()).collect(Collectors.toList());
		assertThat(values).containsExactlyInAnyOrder("foo.bar", "FOO.BAR");
	}

	@Test
	public void testComplex() {
		String query = "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
				"PREFIX foaf:  <http://xmlns.com/foaf/0.1/>\n" +
				"PREFIX dct: <http://purl.org/dc/terms/>\n" +

				"SELECT ?type1 ?type2 ?language ?mbox where {\n" +
				"        ?b dcat:dataset ?a.\n" +

				"        ?b a ?type1." +

				"        ?a a ?type2." +
				"        ?a dct:identifier ?identifier." +
				"        ?a dct:language ?language." +
				"        ?a dct:publisher [foaf:mbox ?mbox] .}";
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
		QueryEvaluationStep prepared = strategy.precompile(pq.getTupleExpr());
		assertNotNull(prepared);
	}

	@Test
	public void testNow() {
		String query = "SELECT ?now WHERE {BIND(NOW() AS ?now)}";
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
		QueryEvaluationStep prepared = strategy.precompile(pq.getTupleExpr());
		assertNotNull(prepared);
		try (CloseableIteration<BindingSet, QueryEvaluationException> evaluate = prepared
				.evaluate(EmptyBindingSet.getInstance())) {
			assertTrue(evaluate.hasNext());
			BindingSet next = evaluate.next();
			assertNotNull(next);
			Binding nowBound = next.getBinding("now");
			assertNotNull(nowBound);
			assertNotNull(nowBound.getValue());
			Value nowValue = nowBound.getValue();
			assertTrue(nowValue.isLiteral());
			Literal nowLiteral = (Literal) nowValue;
			assertEquals(CoreDatatype.XSD.DATETIME, nowLiteral.getCoreDatatype());
		}
	}
}
