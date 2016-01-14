/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio;

import java.util.Arrays;

import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.junit.Test;

/**
 * Abstract test for QueryResultIO.
 * 
 * @author jeen
 * @author Peter Ansell
 */
public abstract class AbstractQueryResultIOTupleTest extends AbstractQueryResultIOTest {

	@Override
	protected final QueryResultFormat getFormat() {
		return getTupleFormat();
	}

	/**
	 * @return The {@link TupleQueryResultFormat} that this test is running
	 *         against.
	 */
	protected abstract TupleQueryResultFormat getTupleFormat();

	/**
	 * @return The {@link BooleanQueryResultFormat} that may be parsed by the
	 *         same parser as the one for {@link #getTupleFormat()}, or null if
	 *         this functionality is not supported.
	 */
	protected abstract BooleanQueryResultFormat getMatchingBooleanFormatOrNull();

	@Test
	public final void testSPARQLResultFormatSingleVarMultipleBindingSets()
		throws Exception
	{
		doTupleNoLinks(getTupleFormat(), createTupleSingleVarMultipleBindingSets(),
				createTupleSingleVarMultipleBindingSets());
	}

	@Test
	public final void testSPARQLResultFormatMultipleBindingsMultipleBindingSets()
		throws Exception
	{
		doTupleNoLinks(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets());
	}

	@Test
	public final void testSPARQLResultFormatNoResults()
		throws Exception
	{
		doTupleNoLinks(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets());
	}

	@Test
	public final void testNoHandlerNoResults()
		throws Exception
	{
		doTupleNoHandler(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets());
	}

	@Test
	public final void testNoHandlerWithResults()
		throws Exception
	{
		doTupleNoHandler(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets());
	}

	@Test
	public final void testTupleParseNoHandlerOnBooleanResults()
		throws Exception
	{
		doTupleParseNoHandlerOnBooleanResults(getTupleFormat(), true, getMatchingBooleanFormatOrNull());
		doTupleParseNoHandlerOnBooleanResults(getTupleFormat(), false, getMatchingBooleanFormatOrNull());
	}

	@Test
	public final void testNoLinksNoResults()
		throws Exception
	{
		doTupleLinks(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets(),
				Arrays.<String> asList());
	}

	@Test
	public final void testNoLinksWithResults()
		throws Exception
	{
		doTupleLinks(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets(),
				Arrays.<String> asList());
	}

	@Test
	public final void testOneLinkNoResults()
		throws Exception
	{
		doTupleLinks(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets(),
				Arrays.asList("info"));
	}

	@Test
	public final void testOneLinkWithResults()
		throws Exception
	{
		doTupleLinks(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets(),
				Arrays.asList("info"));
	}

	@Test
	public final void testMultipleLinksNoResults()
		throws Exception
	{
		doTupleLinks(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets(),
				Arrays.asList("info", "alternate", "other", "another"));
	}

	@Test
	public final void testMultipleLinksWithResults()
		throws Exception
	{
		doTupleLinks(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets(),
				Arrays.asList("info", "alternate", "other", "another"));
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheet()
		throws Exception
	{
		doTupleLinksAndStylesheet(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"),
				"test.xsl");
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetAndNamespaces()
		throws Exception
	{
		doTupleLinksAndStylesheetAndNamespaces(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"),
				"test.xsl", getNamespaces());
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetAndNamespacesQName()
		throws Exception
	{
		doTupleLinksAndStylesheetAndNamespacesQName(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"),
				"test.xsl", getNamespaces());
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetAndNamespacesWithEmpty()
		throws Exception
	{
		doTupleLinksAndStylesheetAndNamespaces(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"),
				"test.xsl", getNamespacesWithEmpty());
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetAndNamespacesQNameWithEmpty()
		throws Exception
	{
		doTupleLinksAndStylesheetAndNamespacesQName(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"),
				"test.xsl", getNamespacesWithEmpty());
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetNoStarts()
		throws Exception
	{
		doTupleLinksAndStylesheetNoStarts(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"),
				"test.xsl");
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetMultipleEndHeaders()
		throws Exception
	{
		doTupleLinksAndStylesheetMultipleEndHeaders(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"),
				"test.xsl");
	}

	@Test
	public final void testNoResultsAndStylesheet()
		throws Exception
	{
		doTupleStylesheet(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets(), "test.xsl");
	}

	@Test
	public final void testMultipleResultsAndStylesheet()
		throws Exception
	{
		doTupleStylesheet(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets(),
				"test.xsl");
	}

	@Test
	public final void testMultipleResultsJSONPCallback()
		throws Exception
	{
		doTupleJSONPCallback(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets());
	}

	@Test
	public final void testNoResultsJSONPCallback()
		throws Exception
	{
		doTupleJSONPCallback(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets());
	}

	@Test
	public final void testNoResultsExceptionHandleSolutionBeforeStartQueryResult()
		throws Exception
	{
		doTupleMissingStartQueryResult(getTupleFormat(), createTupleNoBindingSets(),
				createTupleNoBindingSets(), Arrays.asList("info", "alternate", "other", "another"), "test.xsl");
	}

	@Test
	public final void testMultipleExceptionHandleSolutionBeforeStartQueryResult()
		throws Exception
	{
		doTupleMissingStartQueryResult(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"),
				"test.xsl");
	}
}
