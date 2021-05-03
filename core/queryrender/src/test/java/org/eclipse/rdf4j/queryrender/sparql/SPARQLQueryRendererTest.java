/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Test;

public class SPARQLQueryRendererTest {

	@Test
	public void testQueryWithValues() throws Exception {
		testSingleQuery("queryWithValuesClause");
	}

	@Test
	public void testQueryWithValuesExternal() throws Exception {
		testSingleQuery("queryWithValuesClauseExternal");
	}

	@Test
	public void testAskQueryWithValues() throws Exception {
		testSingleQuery("askQueryWithValuesClause");
	}

	@Test
	public void testConstructQueryWithValues() throws Exception {
		testSingleQuery("constructQueryWithValuesClause");
	}

	@Test
	public void testSimpleRegex() throws Exception {
		testSingleQuery("simpleRegexQuery");
	}

	@Test
	public void testQueryWithLiterals() throws Exception {
		testSingleQuery("queryWithLiterals");
	}

	@Test
	public void testWikiNextprotExample() throws Exception {
		testSingleQuery("wikiNextprotExample");
	}

	@Test
	public void testQueryWithGroupByOrderLimitAndAggFilter() throws Exception {
		testSingleQuery("queryWithGroupByOrderLimitAndAggFilter");
	}

	@Test
	public void testQueryWithASubqueryAndGroupBy() throws Exception {
		testSingleQuery("queryWithASubqueryAndGroupBy");
	}

	@Test
	public void testQueryWithGroupByAndBind() throws Exception {
		testSingleQuery("queryWithGroupByAndBind");
	}

	@Test
	public void testAskQueryWithASubqueryAndGroupBy() throws Exception {
		testSingleQuery("askQueryWithASubqueryAndGroupBy");
	}

	@Test
	public void testConstructQueryWithASubqueryAndGroupBy() throws Exception {
		testSingleQuery("constructQueryWithASubqueryAndGroupBy");
	}

	@Test
	public void testQueryWithPropertyPathStar() throws Exception {
		testSingleQuery("queryWithPropertyPathStar");
	}

	@Test
	public void testQueryWithNonIriFunctions() throws Exception {
		testSingleQuery("queryWithNonIriFunctions");
	}

	@Test
	public void testQueryWithSameTerm() throws Exception {
		testSingleQuery("queryWithSameTerm");
	}

	@Test
	public void testQueryWithLangMatches() throws Exception {
		testSingleQuery("queryWithLangMatches");
	}

	@Test
	public void testQueryWithProjectionBind() throws Exception {
		testSingleQuery("queryWithProjectionBind");
	}

	@Test
	public void testQueryEmptySetBind() throws Exception {
		testSingleQuery("queryEmptySetBind");
	}

	@Test
	public void testDeleteQueryWithASubqueryAndGroupBy() throws Exception {
		testSingleQuery("deleteQueryWithASubqueryAndGroupBy");
	}

	private void testSingleQuery(String id) throws Exception {
		String query = loadQuery(id);
		String expected = loadQuery(id + "_rendered");
		SPARQLQueryRenderer mpQueryRenderer = new SPARQLQueryRenderer();
        ParsedQuery parsedOperation = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
		
        System.out.println(((ParsedQuery) parsedOperation).getTupleExpr());
		try {
			String renderedQuery = mpQueryRenderer.render(parsedOperation);
            assertThat(renderedQuery.toLowerCase()).isEqualToIgnoringWhitespace(expected.toLowerCase());
		} catch (Exception e) {
			fail("could not render query", e);
		}
	}

	private static String loadQuery(String queryId) throws Exception {
		return loadClasspathResourceAsUtf8String("/queryrender/" + queryId + ".sq");
	}

	private static String loadClasspathResourceAsUtf8String(String resourceFile) throws Exception {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(SPARQLQueryRendererTest.class.getResourceAsStream(resourceFile), "UTF-8"));
		StringBuilder textBuilder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			textBuilder.append(line + System.lineSeparator());
		}
		return textBuilder.toString().trim();
	}

}
