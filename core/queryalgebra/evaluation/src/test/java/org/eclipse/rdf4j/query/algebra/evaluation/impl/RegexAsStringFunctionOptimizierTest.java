/** *****************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.RegexAsStringFunctionOptimizer;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.Test;

/**
 * Tests to make sure the RegexAsStringFunctionOptomizer behaves.
 *
 * @author Jerven Bolleman
 */
public class RegexAsStringFunctionOptimizierTest {

	@Test
	public void testEqualsTerm() throws MalformedQueryException {
		String unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX(?o, '^a$'))}";
		String optimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(?o =  'a')}";
		testOptimizer(optimizedQuery, unoptimizedQuery);
	}

	@Test
	public void testStrStartsTerm() throws MalformedQueryException {
		String unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX(?o, '^a'))}";
		String optimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(strStarts(?o, 'a'))}";

		testOptimizer(optimizedQuery, unoptimizedQuery);
	}

	@Test
	public void testStrEndsTerm() throws MalformedQueryException {
		String unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX(?o, 'a$'))}";
		String optimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(strEnds(?o, 'a'))}";

		testOptimizer(optimizedQuery, unoptimizedQuery);
	}

	@Test
	public void testContains() throws MalformedQueryException {
		String unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX(?o, 'a'))}";
		String optimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(contains(?o, 'a'))}";
		testOptimizer(optimizedQuery, unoptimizedQuery);
	}

	@Test
	public void testContainsWithDollar() throws MalformedQueryException {
		String unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX($o, 'a'))}";
		String optimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(contains(?o, 'a'))}";
		testOptimizer(optimizedQuery, unoptimizedQuery);
	}

	@Test
	public void testContainsWithStrangeSpacingAndCaptials() throws MalformedQueryException {
		String unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REgeX(  $o , \"a\" ))}";
		String optimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(contains(?o,  'a'))}";
		testOptimizer(optimizedQuery, unoptimizedQuery);
	}

	@Test
	public void testNotContains() throws MalformedQueryException {
		String unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX(?o, 'a', 'i'))}";

		testOptimizer(unoptimizedQuery, unoptimizedQuery);
	}

	@Test
	public void testContainsFunction() throws MalformedQueryException {
		String optimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(CONTAINS(STR(?o), 'a'))}";
		String unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX(STR(?o), 'a'))}";

		testOptimizer(optimizedQuery, unoptimizedQuery);
	}

	@Test
	public void testRealRegexDoesNotRedirect() {

		String unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX(?o, 'a*'))}";

		testOptimizer(unoptimizedQuery, unoptimizedQuery);
		unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX(?o, 'a+'))}";

		testOptimizer(unoptimizedQuery, unoptimizedQuery);
		unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX(?o, 'a[abc]'))}";

		testOptimizer(unoptimizedQuery, unoptimizedQuery);
		unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(REGEX(?o, 'a&&b'))}";

		testOptimizer(unoptimizedQuery, unoptimizedQuery);
	}

	@Test
	public void testContainsAsIs() throws MalformedQueryException {
		String unoptimizedQuery = "SELECT ?o WHERE {?s ?p ?o . FILTER(contains(?o, 'a*'))}";

		testOptimizer(unoptimizedQuery, unoptimizedQuery);
	}

	private void testOptimizer(String expectedQuery, String actualQuery)
			throws MalformedQueryException, UnsupportedQueryLanguageException {
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, actualQuery, null);
		QueryOptimizer opt = new RegexAsStringFunctionOptimizer(SimpleValueFactory.getInstance());
		QueryRoot optRoot = new QueryRoot(pq.getTupleExpr());
		opt.optimize(optRoot, null, null);

		ParsedQuery expectedParsedQuery = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, expectedQuery, null);
		QueryRoot root = new QueryRoot(expectedParsedQuery.getTupleExpr());
		assertQueryModelTrees(root, optRoot);
	}

	private void assertQueryModelTrees(QueryModelNode expected, QueryModelNode actual) {
		assertEquals(expected, actual);
	}
}
