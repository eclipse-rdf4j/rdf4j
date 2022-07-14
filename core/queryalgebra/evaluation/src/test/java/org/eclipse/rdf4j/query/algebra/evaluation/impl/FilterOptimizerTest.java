/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.FilterOptimizer;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.Test;

public class FilterOptimizerTest extends QueryOptimizerTest {

	@Override
	public FilterOptimizer getOptimizer() {
		return new FilterOptimizer();
	}

	@Test
	public void merge() {
		String expectedQuery = "SELECT * WHERE {?s ?p ?o . FILTER(?o > 2 && ?o <4) }";
		String query = "SELECT * WHERE {?s ?p ?o . FILTER(?o > 2) . FILTER(?o <4) }";

		testOptimizer(expectedQuery, query);
	}

	@Test
	public void dontMerge() {
		Var s = new Var("s");
		Var p = new Var("p");
		Var o = new Var("o");
		Var o2 = new Var("o2");
		ValueConstant two = new ValueConstant(SimpleValueFactory.getInstance().createLiteral(2));
		ValueConstant four = new ValueConstant(SimpleValueFactory.getInstance().createLiteral(4));
		Compare oSmallerThanTwo = new Compare(o, two, CompareOp.GT);
		Filter spo = new Filter(new StatementPattern(s, p, o), oSmallerThanTwo);
		Compare o2SmallerThanFour = new Compare(o2, four, CompareOp.LT);
		Filter spo2 = new Filter(new StatementPattern(s, p, o2), o2SmallerThanFour);
		TupleExpr expected = new QueryRoot(
				new Projection(new Join(spo, spo2), new ProjectionElemList(new ProjectionElem("s"),
						new ProjectionElem("p"), new ProjectionElem("o"), new ProjectionElem("o2"))));
		String query = "SELECT * WHERE {?s ?p ?o . ?s ?p ?o2  . FILTER(?o > '2'^^xsd:int)  . FILTER(?o2 < '4'^^xsd:int) }";

		testOptimizer(expected, query);
	}

	@Test
	public void deMerge() {
		Var s = new Var("s");
		Var p = new Var("p");
		Var o = new Var("o");
		Var o2 = new Var("o2");
		ValueConstant two = new ValueConstant(SimpleValueFactory.getInstance().createLiteral(2));
		ValueConstant four = new ValueConstant(SimpleValueFactory.getInstance().createLiteral(4));
		Compare oSmallerThanTwo = new Compare(o, two, CompareOp.GT);
		Filter spo = new Filter(new StatementPattern(s, p, o), oSmallerThanTwo);
		Compare o2SmallerThanFour = new Compare(o2, four, CompareOp.LT);
		Filter spo2 = new Filter(new StatementPattern(s, p, o2), o2SmallerThanFour);
		TupleExpr expected = new QueryRoot(
				new Projection(new Join(spo, spo2), new ProjectionElemList(new ProjectionElem("s"),
						new ProjectionElem("p"), new ProjectionElem("o"), new ProjectionElem("o2"))));

		String query = "SELECT * WHERE {?s ?p ?o . ?s ?p ?o2  . FILTER(?o > '2'^^xsd:int && ?o2 < '4'^^xsd:int) }";

		testOptimizer(expected, query);
	}

	void testOptimizer(String expectedQuery, String actualQuery)
			throws MalformedQueryException, UnsupportedQueryLanguageException {
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, actualQuery, null);
		FilterOptimizer opt = getOptimizer();
		opt.optimize(pq.getTupleExpr(), null, null);

		ParsedQuery expectedParsedQuery = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, expectedQuery, null);
		assertEquals(pq.getTupleExpr(), expectedParsedQuery.getTupleExpr());
	}

	void testOptimizer(TupleExpr expectedQuery, String actualQuery)
			throws MalformedQueryException, UnsupportedQueryLanguageException {
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, actualQuery, null);
		FilterOptimizer opt = getOptimizer();
		opt.optimize(pq.getTupleExpr(), null, null);

		assertEquals(pq.getTupleExpr(), expectedQuery);
	}
}
