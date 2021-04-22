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
        String query = loadQuery("queryWithValuesClause");
        testSingleQuery("queryWithValuesClause", query);
    }

    @Test
    public void testQueryWithValuesExternal() throws Exception {
        String query = loadQuery("queryWithValuesClauseExternal");
        testSingleQuery("queryWithValuesClauseExternal", query);
    }

    @Test
    public void testAskQueryWithValues() throws Exception {
        String query = loadQuery("askQueryWithValuesClause");
        testSingleQuery("askQueryWithValuesClause", query);
    }

    @Test
    public void testConstructQueryWithValues() throws Exception {
        String query = loadQuery("constructQueryWithValuesClause");
        testSingleQuery("constructQueryWithValuesClause", query);
    }

    @Test
    public void testSimpleRegex() throws Exception {
        String query = loadQuery("simpleRegexQuery");
        testSingleQuery("simpleRegexQuery", query);
    }

    @Test
    public void testQueryWithLiterals() throws Exception {
        String query = loadQuery("queryWithLiterals");
        testSingleQuery("queryWithLiterals", query);
    }

    @Test
    public void testWikiNextprotExample() throws Exception {
        String query = loadQuery("wikiNextprotExample");
        testSingleQuery("wikiNextprotExample", query);
    }

    @Test
    public void testQueryWithGroupByOrderLimitAndAggFilter() throws Exception {
        String query = loadQuery("queryWithGroupByOrderLimitAndAggFilter");
        testSingleQuery("queryWithGroupByOrderLimitAndAggFilter", query);
    }

    @Test
    public void testQueryWithASubqueryAndGroupBy() throws Exception {
        String query = loadQuery("queryWithASubqueryAndGroupBy");
        testSingleQuery("queryWithASubqueryAndGroupBy", query);
    }

    @Test
    public void testQueryWithGroupByAndBind() throws Exception {
        String query = loadQuery("queryWithGroupByAndBind");
        testSingleQuery("queryWithGroupByAndBind", query);
    }

    @Test
    public void testAskQueryWithASubqueryAndGroupBy() throws Exception {
        String query = loadQuery("askQueryWithASubqueryAndGroupBy");
        testSingleQuery("askQueryWithASubqueryAndGroupBy", query);
    }

    @Test
    public void testConstructQueryWithASubqueryAndGroupBy() throws Exception {
        String query = loadQuery("constructQueryWithASubqueryAndGroupBy");
        testSingleQuery("constructQueryWithASubqueryAndGroupBy", query);
    }

    @Test
    public void testQueryWithPropertyPathStar() throws Exception {
        String query = loadQuery("queryWithPropertyPathStar");
        testSingleQuery("queryWithPropertyPathStar", query);
    }

    @Test
    public void testQueryWithNonIriFunctions() throws Exception {
        String query = loadQuery("queryWithNonIriFunctions");
        testSingleQuery("queryWithNonIriFunctions", query);
    }

    @Test
    public void testQueryWithSameTerm() throws Exception {
        String query = loadQuery("queryWithSameTerm");
        testSingleQuery("queryWithSameTerm", query);
    }

    @Test
    public void testQueryWithLangMatches() throws Exception {
        String query = loadQuery("queryWithLangMatches");
        testSingleQuery("queryWithLangMatches", query);
    }

    @Test
    public void testQueryWithProjectionBind() throws Exception {
        String query = loadQuery("queryWithProjectionBind");
        testSingleQuery("queryWithProjectionBind", query);
    }

    @Test
    public void testQueryEmptySetBind() throws Exception {
        String query = loadQuery("queryEmptySetBind");
        testSingleQuery("queryEmptySetBind", query);
    }

    @Test
    public void testDeleteQueryWithASubqueryAndGroupBy() throws Exception {
        String query = loadQuery("deleteQueryWithASubqueryAndGroupBy");
        testSingleQuery("deleteQueryWithASubqueryAndGroupBy", query);
    }

    private void testSingleQuery(String id, String strQuery) {
        SPARQLQueryRenderer mpQueryRenderer = new SPARQLQueryRenderer();
        ParsedQuery query = (ParsedQuery) QueryParserUtil.parseQuery(QueryLanguage.SPARQL, strQuery, null);
        try {
            String renderedQuery = mpQueryRenderer.render(query);

            assertThat(strQuery.toLowerCase()).isEqualToIgnoringNewLines(renderedQuery.toLowerCase());
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
