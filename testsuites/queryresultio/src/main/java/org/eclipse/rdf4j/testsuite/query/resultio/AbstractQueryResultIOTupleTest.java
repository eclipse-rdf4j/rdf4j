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

package org.eclipse.rdf4j.testsuite.query.resultio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.junit.jupiter.api.Test;

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
	 * @return The {@link TupleQueryResultFormat} that this test is running against.
	 */
	protected abstract TupleQueryResultFormat getTupleFormat();

	/**
	 * @return The {@link BooleanQueryResultFormat} that may be parsed by the same parser as the one for
	 *         {@link #getTupleFormat()}, or null if this functionality is not supported.
	 */
	protected abstract BooleanQueryResultFormat getMatchingBooleanFormatOrNull();

	@Test
	public final void testSPARQLResultFormatSingleVarMultipleBindingSets() throws Exception {
		doTupleNoLinks(getTupleFormat(), createTupleSingleVarMultipleBindingSets(),
				createTupleSingleVarMultipleBindingSets());
	}

	@Test
	public final void testSPARQLResultFormatMultipleBindingsMultipleBindingSets() throws Exception {
		doTupleNoLinks(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets());
	}

	@Test
	public final void testSPARQLResultFormatNoResults() throws Exception {
		doTupleNoLinks(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets());
	}

	@Test
	public final void testNoHandlerNoResults() throws Exception {
		doTupleNoHandler(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets());
	}

	@Test
	public final void testNoHandlerWithResults() throws Exception {
		doTupleNoHandler(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets());
	}

	@Test
	public final void testTupleParseNoHandlerOnBooleanResults() throws Exception {
		doTupleParseNoHandlerOnBooleanResults(getTupleFormat(), true, getMatchingBooleanFormatOrNull());
		doTupleParseNoHandlerOnBooleanResults(getTupleFormat(), false, getMatchingBooleanFormatOrNull());
	}

	@Test
	public final void testNoLinksNoResults() throws Exception {
		doTupleLinks(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets(), List.<String>of());
	}

	@Test
	public final void testNoLinksWithResults() throws Exception {
		doTupleLinks(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets(),
				List.<String>of());
	}

	@Test
	public final void testOneLinkNoResults() throws Exception {
		doTupleLinks(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets(), List.of("info"));
	}

	@Test
	public final void testOneLinkWithResults() throws Exception {
		doTupleLinks(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets(),
				List.of("info"));
	}

	@Test
	public final void testMultipleLinksNoResults() throws Exception {
		doTupleLinks(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets(),
				Arrays.asList("info", "alternate", "other", "another"));
	}

	@Test
	public final void testMultipleLinksWithResults() throws Exception {
		doTupleLinks(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets(),
				Arrays.asList("info", "alternate", "other", "another"));
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheet() throws Exception {
		doTupleLinksAndStylesheet(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets(),
				Arrays.asList("info", "alternate", "other", "another"), "test.xsl");
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetAndNamespaces() throws Exception {
		doTupleLinksAndStylesheetAndNamespaces(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"), "test.xsl",
				getNamespaces());
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetAndNamespacesQName() throws Exception {
		doTupleLinksAndStylesheetAndNamespacesQName(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"), "test.xsl",
				getNamespaces());
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetAndNamespacesWithEmpty() throws Exception {
		doTupleLinksAndStylesheetAndNamespaces(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"), "test.xsl",
				getNamespacesWithEmpty());
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetAndNamespacesQNameWithEmpty() throws Exception {
		doTupleLinksAndStylesheetAndNamespacesQName(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"), "test.xsl",
				getNamespacesWithEmpty());
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetNoStarts() throws Exception {
		doTupleLinksAndStylesheetNoStarts(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"), "test.xsl");
	}

	@Test
	public final void testMultipleLinksWithResultsAndStylesheetMultipleEndHeaders() throws Exception {
		doTupleLinksAndStylesheetMultipleEndHeaders(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"), "test.xsl");
	}

	@Test
	public final void testNoResultsAndStylesheet() throws Exception {
		doTupleStylesheet(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets(), "test.xsl");
	}

	@Test
	public final void testMultipleResultsAndStylesheet() throws Exception {
		doTupleStylesheet(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets(),
				"test.xsl");
	}

	@Test
	public final void testMultipleResultsJSONPCallback() throws Exception {
		doTupleJSONPCallback(getTupleFormat(), createTupleMultipleBindingSets(), createTupleMultipleBindingSets());
	}

	@Test
	public final void testNoResultsJSONPCallback() throws Exception {
		doTupleJSONPCallback(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets());
	}

	@Test
	public final void testNoResultsExceptionHandleSolutionBeforeStartQueryResult() throws Exception {
		doTupleMissingStartQueryResult(getTupleFormat(), createTupleNoBindingSets(), createTupleNoBindingSets(),
				Arrays.asList("info", "alternate", "other", "another"), "test.xsl");
	}

	@Test
	public final void testMultipleExceptionHandleSolutionBeforeStartQueryResult() throws Exception {
		doTupleMissingStartQueryResult(getTupleFormat(), createTupleMultipleBindingSets(),
				createTupleMultipleBindingSets(), Arrays.asList("info", "alternate", "other", "another"), "test.xsl");
	}

	@Test
	public final void testRDFStarCompatibility() throws IOException {
		ValueFactory vf = SimpleValueFactory.getInstance();

		List<String> bindingNames = Arrays.asList("a", "b", "c");
		List<BindingSet> bindings = new ArrayList<>();
		MapBindingSet bs1 = new MapBindingSet();
		// Note that the CSV format seems to ignore the datatype and assume it's xsd:integer
		// so no other datatype works with it properly.
		bs1.addBinding("a", vf.createLiteral("1984", XSD.INTEGER));
		bs1.addBinding("b", vf.createIRI("urn:test"));
		bs1.addBinding("c", vf.createBNode("bnode1"));
		bindings.add(bs1);
		MapBindingSet bs2 = new MapBindingSet();
		bs2.addBinding("a", vf.createLiteral("foo"));
		bs2.addBinding("b", vf.createTriple(vf.createBNode("bnode2"), RDFS.LABEL,
				vf.createLiteral("\"literal with\tfunny\nchars")));
		bs2.addBinding("c", vf.createTriple(vf.createTriple(vf.createTriple(vf.createIRI("urn:a"), RDF.TYPE,
				vf.createIRI("urn:b")), vf.createIRI("urn:c"), vf.createIRI("urn:d")), vf.createIRI("urn:e"),
				vf.createIRI("urn:f")));
		bindings.add(bs2);

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			QueryResultIO.writeTuple(new IteratingTupleQueryResult(bindingNames, bindings), getTupleFormat(), bos);
			try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray())) {
				TupleQueryResult parsedBindings = QueryResultIO.parseTuple(bis, getTupleFormat(),
						null);
				assertEquals(bindingNames, parsedBindings.getBindingNames());
				List<BindingSet> actualBindings = new ArrayList<>();
				parsedBindings.forEach(actualBindings::add);
				assertEquals(bindings, actualBindings);
			}
		}
	}
}
