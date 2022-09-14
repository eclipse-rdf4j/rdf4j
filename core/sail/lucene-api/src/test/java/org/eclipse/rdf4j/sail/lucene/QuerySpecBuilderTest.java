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
package org.eclipse.rdf4j.sail.lucene;

import static org.eclipse.rdf4j.model.vocabulary.RDF.TYPE;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.BOOST;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.LUCENE_QUERY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.QUERY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SNIPPET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.junit.Before;
import org.junit.Test;

public class QuerySpecBuilderTest {

	private QuerySpecBuilder interpreter;

	private SPARQLParser parser;

	@Before
	public void setUp() throws Exception {
		interpreter = new QuerySpecBuilder(true);
		parser = new SPARQLParser();
	}

	@Test
	public void testQueryInterpretation() {
		String buffer = "SELECT ?Subject ?Score ?Snippet " +
				"WHERE { ?Subject <" + MATCHES + "> [ " +
				"<" + TYPE + "> <" + LUCENE_QUERY + ">; " +
				"<" + QUERY + "> \"my Lucene query\"; " +
				"<" + SCORE + "> ?Score; " +
				"<" + SNIPPET + "> ?Snippet ]. } ";
		ParsedQuery query = parser.parseQuery(buffer, null);
		TupleExpr tupleExpr = query.getTupleExpr();
		Collection<SearchQueryEvaluator> queries = process(interpreter, tupleExpr);
		assertEquals(1, queries.size());

		QuerySpec querySpec = (QuerySpec) queries.iterator().next();
		assertEquals("Subject", querySpec.getMatchesPattern().getSubjectVar().getName());
		assertEquals(1, querySpec.getQueryPatterns().size());
		QuerySpec.QueryParam param = querySpec.getQueryPatterns().iterator().next();
		assertEquals("my Lucene query", ((Literal) param.getQueryPattern().getObjectVar().getValue()).getLabel());
		assertEquals("Score", querySpec.getScorePattern().getObjectVar().getName());
		assertEquals("Snippet", param.getSnippetPattern().getObjectVar().getName());
		assertEquals(LUCENE_QUERY, querySpec.getTypePattern().getObjectVar().getValue());
		assertEquals("my Lucene query", param.getQuery());
		assertNull(querySpec.getSubject());
	}

	@Test
	public void testMultipleQueriesInterpretation() {
		String buffer = "SELECT ?sub1 ?score1 ?snippet1 ?sub2 ?score2 ?snippet2 ?x ?p1 ?p2 " +
				"WHERE { ?sub1 <" + MATCHES + "> [ " +
				"<" + TYPE + "> <" + LUCENE_QUERY + ">; " +
				"<" + QUERY + "> \"my Lucene query\"; " +
				"<" + SCORE + "> ?score1; " +
				"<" + SNIPPET + "> ?snippet1 ]. " +
				" ?sub2 <" + MATCHES + "> [ " +
				"<" + TYPE + "> <" + LUCENE_QUERY + ">; " +
				"<" + QUERY + "> \"second lucene query\"; " +
				"<" + SCORE + "> ?score2; " +
				"<" + SNIPPET + "> ?snippet2 ]. " +
				// and connect them both via any X in between, just as salt to make the
				// parser do something
				" ?sub1 ?p1 ?x . ?x ?p2 ?sub2 .} ";
		ParsedQuery query = parser.parseQuery(buffer, null);
		TupleExpr tupleExpr = query.getTupleExpr();

		Collection<SearchQueryEvaluator> queries = process(interpreter, tupleExpr);
		assertEquals(2, queries.size());
		Iterator<SearchQueryEvaluator> i = queries.iterator();
		boolean matched1 = false;
		boolean matched2 = false;
		while (i.hasNext()) {
			QuerySpec querySpec = (QuerySpec) i.next();
			if ("sub1".equals(querySpec.getMatchesVariableName())) {
				// Matched the first
				assertEquals("sub1", querySpec.getMatchesPattern().getSubjectVar().getName());
				assertEquals(1, querySpec.getQueryPatterns().size());
				QuerySpec.QueryParam param = querySpec.getQueryPatterns().iterator().next();
				assertEquals("my Lucene query",
						((Literal) param.getQueryPattern().getObjectVar().getValue()).getLabel());
				assertEquals("score1", querySpec.getScorePattern().getObjectVar().getName());
				assertEquals("snippet1", param.getSnippetPattern().getObjectVar().getName());
				assertEquals(LUCENE_QUERY, querySpec.getTypePattern().getObjectVar().getValue());
				assertEquals("my Lucene query", param.getQuery());
				assertNull(querySpec.getSubject());
				matched1 = true;
			} else if ("sub2".equals(querySpec.getMatchesVariableName())) {
				// and the second
				assertEquals("sub2", querySpec.getMatchesPattern().getSubjectVar().getName());
				assertEquals(1, querySpec.getQueryPatterns().size());
				QuerySpec.QueryParam param = querySpec.getQueryPatterns().iterator().next();
				assertEquals("second lucene query",
						((Literal) param.getQueryPattern().getObjectVar().getValue()).getLabel());
				assertEquals("score2", querySpec.getScorePattern().getObjectVar().getName());
				assertEquals("snippet2", param.getSnippetPattern().getObjectVar().getName());
				assertEquals(LUCENE_QUERY, querySpec.getTypePattern().getObjectVar().getValue());
				assertEquals("second lucene query", param.getQuery());
				assertNull(querySpec.getSubject());
				matched2 = true;
			} else {
				fail("Found unexpected query spec: " + querySpec);
			}
		}
		if (!matched1) {
			fail("did not find query patter sub1");
		}
		if (!matched2) {
			fail("did not find query patter sub2");
		}
	}

	@Test
	public void testQueryInterpretation2() {
		String buffer = "SELECT ?Subject ?Score ?Snippet " +
				"WHERE { ?Subject <" + MATCHES + "> [ " +
				"<" + TYPE + "> <" + LUCENE_QUERY + ">; " +
				"<" + QUERY + ">" + "[ <" + QUERY + "> \"my Lucene query\"; " +
				"<" + SNIPPET + "> ?Snippet ] ;" +
				"<" + SCORE + "> ?Score ]. } ";
		ParsedQuery query = parser.parseQuery(buffer, null);
		TupleExpr tupleExpr = query.getTupleExpr();
		Collection<SearchQueryEvaluator> queries = process(interpreter, tupleExpr);
		assertEquals(1, queries.size());

		QuerySpec querySpec = (QuerySpec) queries.iterator().next();
		assertEquals("Subject", querySpec.getMatchesPattern().getSubjectVar().getName());
		assertEquals(1, querySpec.getQueryPatterns().size());
		QuerySpec.QueryParam param = querySpec.getQueryPatterns().iterator().next();
		assertEquals("my Lucene query", ((Literal) param.getQueryPattern().getObjectVar().getValue()).getLabel());
		assertEquals("Score", querySpec.getScorePattern().getObjectVar().getName());
		assertEquals("Snippet", param.getSnippetPattern().getObjectVar().getName());
		assertEquals(LUCENE_QUERY, querySpec.getTypePattern().getObjectVar().getValue());
		assertEquals("my Lucene query", param.getQuery());
		assertNull(querySpec.getSubject());
	}

	@Test
	public void testMultipleQueriesInterpretation2() {
		String buffer = "SELECT ?sub1 ?score1 ?snippet1 ?sub2 ?score2 ?snippet2 ?x ?p1 ?p2 " +
				"WHERE { ?sub1 <" + MATCHES + "> [ " +
				"<" + TYPE + "> <" + LUCENE_QUERY + ">; " +
				"<" + SCORE + "> ?score1; " +
				"<" + QUERY + "> [ <" + QUERY + "> \"my Lucene query\"; " +
				"<" + SNIPPET + "> ?snippet1 ]]. " +
				" ?sub2 <" + MATCHES + "> [ " +
				"<" + TYPE + "> <" + LUCENE_QUERY + ">; " +
				"<" + SCORE + "> ?score2; " +
				"<" + QUERY + "> [ <" + QUERY + "> \"second lucene query\"; " +
				"<" + SNIPPET + "> ?snippet2 ] ]. " +
				// and connect them both via any X in between, just as salt to make the
				// parser do something
				" ?sub1 ?p1 ?x . ?x ?p2 ?sub2 .} ";
		ParsedQuery query = parser.parseQuery(buffer, null);
		TupleExpr tupleExpr = query.getTupleExpr();

		Collection<SearchQueryEvaluator> queries = process(interpreter, tupleExpr);
		assertEquals(2, queries.size());
		Iterator<SearchQueryEvaluator> i = queries.iterator();
		boolean matched1 = false;
		boolean matched2 = false;
		while (i.hasNext()) {
			QuerySpec querySpec = (QuerySpec) i.next();
			if ("sub1".equals(querySpec.getMatchesVariableName())) {
				// Matched the first
				assertEquals("sub1", querySpec.getMatchesPattern().getSubjectVar().getName());
				assertEquals(1, querySpec.getQueryPatterns().size());
				QuerySpec.QueryParam param = querySpec.getQueryPatterns().iterator().next();
				assertEquals("my Lucene query",
						((Literal) param.getQueryPattern().getObjectVar().getValue()).getLabel());
				assertEquals("score1", querySpec.getScorePattern().getObjectVar().getName());
				assertEquals("snippet1", param.getSnippetPattern().getObjectVar().getName());
				assertEquals(LUCENE_QUERY, querySpec.getTypePattern().getObjectVar().getValue());
				assertEquals("my Lucene query", param.getQuery());
				assertNull(querySpec.getSubject());
				matched1 = true;
			} else if ("sub2".equals(querySpec.getMatchesVariableName())) {
				// and the second
				assertEquals("sub2", querySpec.getMatchesPattern().getSubjectVar().getName());
				assertEquals(1, querySpec.getQueryPatterns().size());
				QuerySpec.QueryParam param = querySpec.getQueryPatterns().iterator().next();
				assertEquals("second lucene query",
						((Literal) param.getQueryPattern().getObjectVar().getValue()).getLabel());
				assertEquals("score2", querySpec.getScorePattern().getObjectVar().getName());
				assertEquals("snippet2", param.getSnippetPattern().getObjectVar().getName());
				assertEquals(LUCENE_QUERY, querySpec.getTypePattern().getObjectVar().getValue());
				assertEquals("second lucene query", param.getQuery());
				assertNull(querySpec.getSubject());
				matched2 = true;
			} else {
				fail("Found unexpected query spec: " + querySpec);
			}
		}
		if (!matched1) {
			fail("did not find query patter sub1");
		}
		if (!matched2) {
			fail("did not find query patter sub2");
		}
	}

	@Test
	public void testQueryInterpretationMulti() {
		String buffer = "SELECT ?sub1 ?sub2 ?sub3 ?Subject ?Score ?Snippet ?Snippet2 " +
				"WHERE { ?Subject <" + MATCHES + "> [ " +
				"<" + TYPE + "> <" + LUCENE_QUERY + ">; " +
				"<" + QUERY + "> ?sub1, ?sub2, ?sub3;" +
				"<" + SCORE + "> ?Score ]. " +
				"?sub1 <" + QUERY + "> \"my Lucene query\"; <" + SNIPPET + "> ?Snippet ; <" + BOOST + "> 0.8 ." +
				"?sub2 <" + QUERY + "> \"my Lucene query2\"; <" + SNIPPET + "> ?Snippet2 ; <" + BOOST + "> 0.2 ." +
				"?sub3 <" + QUERY + "> \"my Lucene query3\"." +
				// and connect them both via any X in between, just as salt to make the
				// parser do something
				" ?sub1 ?p1 ?x . ?x ?p2 ?sub2 . ?x ?p3 ?sub3 } ";
		ParsedQuery query = parser.parseQuery(buffer, null);
		TupleExpr tupleExpr = query.getTupleExpr();
		Collection<SearchQueryEvaluator> queries = process(interpreter, tupleExpr);
		assertEquals(1, queries.size());

		QuerySpec querySpec = (QuerySpec) queries.iterator().next();
		assertEquals("Subject", querySpec.getMatchesPattern().getSubjectVar().getName());
		assertEquals(3, querySpec.getQueryPatterns().size());

		boolean read1 = false, read2 = false, read3 = false;
		for (QuerySpec.QueryParam param : querySpec.getQueryPatterns()) {
			switch (param.getFieldPattern().getObjectVar().getName()) {
			case "sub1": {
				assertEquals("my Lucene query", param.getQuery());
				assertEquals("my Lucene query",
						((Literal) param.getQueryPattern().getObjectVar().getValue()).getLabel());
				assertEquals("Snippet", param.getSnippetPattern().getObjectVar().getName());
				assertNotNull(param.getBoost());
				assertEquals(0.8f, param.getBoost(), 0.0001f);
				read1 = true;
			}
				break;
			case "sub2": {
				assertEquals("my Lucene query2", param.getQuery());
				assertEquals("my Lucene query2",
						((Literal) param.getQueryPattern().getObjectVar().getValue()).getLabel());
				assertEquals("Snippet2", param.getSnippetPattern().getObjectVar().getName());
				assertNotNull(param.getBoost());
				assertEquals(0.2f, param.getBoost(), 0.0001f);
				read2 = true;
			}
				break;
			case "sub3": {
				assertEquals("my Lucene query3", param.getQuery());
				assertEquals("my Lucene query3",
						((Literal) param.getQueryPattern().getObjectVar().getValue()).getLabel());
				assertNull(param.getBoost());
				assertNull(param.getSnippetPattern());
				read3 = true;
			}
				break;
			default:
				fail("unknown query var name: " + param.getFieldPattern().getObjectVar().getName());
			}
		}
		assertEquals(LUCENE_QUERY, querySpec.getTypePattern().getObjectVar().getValue());
		assertEquals("Score", querySpec.getScorePattern().getObjectVar().getName());
		assertTrue("lucene query 1 not read", read1);
		assertTrue("lucene query 2 not read", read2);
		assertTrue("lucene query 3 not read", read3);
		assertNull(querySpec.getSubject());
	}

	/**
	 * Incomplete queries should fail, if configured
	 *
	 * @throws Exception
	 */
	@Test
	public void testIncompleteFail() throws Exception {
		// default works
		String queryString = "SELECT ?sub1 ?score1 ?snippet1 WHERE " + "{ ?sub1 <" + MATCHES + "> [ " + "<" + TYPE
				+ "> <" + LUCENE_QUERY + ">; " + "<" + QUERY + "> \"my Lucene query\"; " + "<" + SCORE
				+ "> ?score1; " + "<" + SNIPPET + "> ?snippet1].}";
		checkQuery(queryString);

		// minimal works
		queryString = "SELECT ?sub1 WHERE " + "{ ?sub1 <" + MATCHES + "> [ " + "<" + TYPE + "> <" + LUCENE_QUERY + ">; "
				+ "<" + QUERY + "> \"my Lucene query\" ]. } ";
		checkQuery(queryString);

		// matches missing
		queryString = "SELECT ?sub1 ?score1 ?snippet1 WHERE "
				// + "{sub1} <" + MATCHES + "> {} "
				+ "<" + TYPE + "> <" + LUCENE_QUERY + ">; " + "<" + QUERY + "> \"my Lucene query\"; " + "<" + SCORE
				+ "> ?score1; " + "<" + SNIPPET + "> ?snippet1 .";
		try {
			checkQuery(queryString);
			fail("invalid query ignored");
		} catch (Exception x) {
			// excellent
		}

		// type missing
		queryString = "SELECT ?sub1 ?score1 ?snippet1 WHERE " + "{ ?sub1 <" + MATCHES + "> [" + "<" + QUERY
				+ "> \"my Lucene query\"; " + "<" + SCORE
				+ "> ?score1; " + "<" + SNIPPET + "> ?snippet1].}";
		try {
			checkQuery(queryString);
			// excellent
		} catch (Exception x) {
			fail("missing type is ok, should not throw an exception");
		}

		// query missing
		queryString = "SELECT ?sub1 ?score1 ?snippet1 WHERE " + "{ ?sub1 <" + MATCHES + "> [ " + "<" + TYPE
				+ "> <" + LUCENE_QUERY + ">; " + "<" + SCORE
				+ "> ?score1; " + "<" + SNIPPET + "> ?snippet1].}";
		try {
			checkQuery(queryString);
			fail("invalid missing query not detected");
		} catch (Exception x) {
			// excellent
		}
	}

	/**
	 * Checks if the querystring contains exactly one lucene query throws exceptions if not or if the query is
	 * incomplete
	 *
	 * @param queryString
	 */
	private void checkQuery(String queryString) throws Exception {
		ParsedQuery query = parser.parseQuery(queryString, null);
		TupleExpr tupleExpr = query.getTupleExpr();
		Collection<SearchQueryEvaluator> queries = process(interpreter, tupleExpr);
		assertEquals("expect one query", 1, queries.size());
	}

	private Collection<SearchQueryEvaluator> process(SearchQueryInterpreter interpreter, TupleExpr tupleExpr) {
		List<SearchQueryEvaluator> queries = new ArrayList<>();
		interpreter.process(tupleExpr, new QueryBindingSet(), queries);
		return queries;
	}
}
