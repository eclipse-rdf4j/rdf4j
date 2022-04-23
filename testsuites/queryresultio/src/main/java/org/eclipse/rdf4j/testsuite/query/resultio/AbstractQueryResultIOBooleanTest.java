/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.testsuite.query.resultio;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.junit.jupiter.api.Test;

/**
 * Abstract test for QueryResultIO.
 *
 * @author jeen
 * @author Peter Ansell
 */
public abstract class AbstractQueryResultIOBooleanTest extends AbstractQueryResultIOTest {

	@Override
	protected final QueryResultFormat getFormat() {
		return getBooleanFormat();
	}

	/**
	 * @return The {@link BooleanQueryResultFormat} that this test is running against.
	 */
	protected abstract BooleanQueryResultFormat getBooleanFormat();

	/**
	 * @return The {@link TupleQueryResultFormat} that may be parsed by the same parser as the one for
	 *         {@link #getBooleanFormat()}, or null if this functionality is not supported.
	 */
	protected abstract TupleQueryResultFormat getMatchingTupleFormatOrNull();

	@Test
	public final void testBooleanNoLinks() throws Exception {
		doBooleanNoLinks(getBooleanFormat(), true);
		doBooleanNoLinks(getBooleanFormat(), false);
	}

	@Test
	public final void testBooleanEmptyLinks() throws Exception {
		doBooleanLinks(getBooleanFormat(), true, List.<String>of());
		doBooleanLinks(getBooleanFormat(), false, List.<String>of());
	}

	@Test
	public final void testBooleanOneLink() throws Exception {
		doBooleanLinks(getBooleanFormat(), true, List.of("info"));
		doBooleanLinks(getBooleanFormat(), false, List.of("info"));
	}

	@Test
	public final void testBooleanMultipleLinks() throws Exception {
		doBooleanLinks(getBooleanFormat(), true, Arrays.asList("info", "alternate", "other", "another"));
		doBooleanLinks(getBooleanFormat(), false, Arrays.asList("info", "alternate", "other", "another"));
	}

	@Test
	public final void testBooleanEmptyLinksOnly() throws Exception {
		doBooleanLinksOnly(getBooleanFormat(), true, List.<String>of());
		doBooleanLinksOnly(getBooleanFormat(), false, List.<String>of());
	}

	@Test
	public final void testBooleanOneLinkOnly() throws Exception {
		doBooleanLinksOnly(getBooleanFormat(), true, List.of("info"));
		doBooleanLinksOnly(getBooleanFormat(), false, List.of("info"));
	}

	@Test
	public final void testBooleanMultipleLinksOnly() throws Exception {
		doBooleanLinksOnly(getBooleanFormat(), true, Arrays.asList("info", "alternate", "other", "another"));
		doBooleanLinksOnly(getBooleanFormat(), false, Arrays.asList("info", "alternate", "other", "another"));
	}

	@Test
	public final void testBooleanMultipleLinksWithStylesheet() throws Exception {
		doBooleanLinksAndStylesheet(getBooleanFormat(), true, Arrays.asList("info", "alternate", "other", "another"),
				"test.xsl");
		doBooleanLinksAndStylesheet(getBooleanFormat(), false, Arrays.asList("info", "alternate", "other", "another"),
				"test.xsl");
	}

	@Test
	public final void testMultipleResultsAndStylesheet() throws Exception {
		doBooleanStylesheet(getBooleanFormat(), true, "test.xsl");
		doBooleanStylesheet(getBooleanFormat(), false, "test.xsl");
	}

	@Test
	public final void testInvalidBooleanAfterStartQueryResult() throws Exception {
		doInvalidBooleanAfterStartQueryResult(getBooleanFormat(), true,
				Arrays.asList("info", "alternate", "other", "another"));
		doInvalidBooleanAfterStartQueryResult(getBooleanFormat(), false,
				Arrays.asList("info", "alternate", "other", "another"));
	}

	@Test
	public final void testBooleanNoHandler() throws Exception {
		doBooleanNoHandler(getBooleanFormat(), true);
		doBooleanNoHandler(getBooleanFormat(), false);
	}

	@Test
	public final void testBooleanParseNoHandlerOnTupleResultsNoResults() throws Exception {
		doBooleanParseNoHandlerOnTupleResults(getBooleanFormat(), createTupleNoBindingSets(),
				getMatchingTupleFormatOrNull());
	}

	@Test
	public final void testBooleanParseNoHandlerOnTupleResultsSingleVarMultipleBindingSets() throws Exception {
		doBooleanParseNoHandlerOnTupleResults(getBooleanFormat(), createTupleSingleVarMultipleBindingSets(),
				getMatchingTupleFormatOrNull());
	}

	@Test
	public final void testBooleanParseNoHandlerOnTupleResultsMultipleBindingsMultipleBindingSets() throws Exception {
		doBooleanParseNoHandlerOnTupleResults(getBooleanFormat(), createTupleMultipleBindingSets(),
				getMatchingTupleFormatOrNull());
	}

}
