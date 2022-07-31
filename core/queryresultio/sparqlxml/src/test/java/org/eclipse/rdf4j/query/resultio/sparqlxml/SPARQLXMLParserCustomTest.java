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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.QueryResultParser;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Custom tests for SPARQL/XML Parser.
 *
 * @author Michael Grove
 * @author Peter Ansell
 */
public class SPARQLXMLParserCustomTest {

	/**
	 * Test with the default ParserConfig settings. Ie, setParserConfig is not called.
	 *
	 * @throws Exception
	 */
	@Test
	public void testEntityExpansionDefaultSettings() throws Exception {
		QueryResultCollector handler = new QueryResultCollector();
		ParseErrorCollector errorCollector = new ParseErrorCollector();
		QueryResultParser aParser = QueryResultIO.createTupleParser(TupleQueryResultFormat.SPARQL)
				.setQueryResultHandler(handler)
				.setParseErrorListener(errorCollector);

		try {
			// this should trigger a SAX parse exception that will blow up at
			// the 64k entity limit rather than OOMing
			aParser.parseQueryResult(this.getClass().getResourceAsStream("/sparqlxml/bad-entity-expansion-limit.srx"));
			fail("Parser did not throw an exception");
		} catch (QueryResultParseException e) {
			// assertTrue(e.getMessage().contains(
			// "The parser has encountered more than \"64,000\" entity
			// expansions in this document; this is the limit imposed by the
			// "));
		}
		assertEquals(0, errorCollector.getWarnings().size());
		assertEquals(0, errorCollector.getErrors().size());
		assertEquals(1, errorCollector.getFatalErrors().size());
	}

	/**
	 * Test with unrelated ParserConfig settings
	 *
	 * @throws Exception
	 */
	@Test
	public void testEntityExpansionUnrelatedSettings() throws Exception {
		ParserConfig config = new ParserConfig();
		QueryResultCollector handler = new QueryResultCollector();
		ParseErrorCollector errorCollector = new ParseErrorCollector();
		QueryResultParser aParser = QueryResultIO.createTupleParser(TupleQueryResultFormat.SPARQL)
				.setQueryResultHandler(handler)
				.setParserConfig(config)
				.setParseErrorListener(errorCollector);

		try {
			// this should trigger a SAX parse exception that will blow up at
			// the 64k entity limit rather than OOMing
			aParser.parseQueryResult(this.getClass().getResourceAsStream("/sparqlxml/bad-entity-expansion-limit.srx"));
			fail("Parser did not throw an exception");
		} catch (QueryResultParseException e) {
			// assertTrue(e.getMessage().contains(
			// "The parser has encountered more than \"64,000\" entity
			// expansions in this document; this is the limit imposed by the
			// "));
		}
		assertEquals(0, errorCollector.getWarnings().size());
		assertEquals(0, errorCollector.getErrors().size());
		assertEquals(1, errorCollector.getFatalErrors().size());
	}

	@Test
	public void testLangMissingOnStringLang() throws Exception {
		ParserConfig config = new ParserConfig();
		QueryResultCollector handler = new QueryResultCollector();
		ParseErrorCollector errorCollector = new ParseErrorCollector();
		QueryResultParser aParser = QueryResultIO.createTupleParser(TupleQueryResultFormat.SPARQL)
				.setQueryResultHandler(handler)
				.setParserConfig(config)
				.setParseErrorListener(errorCollector);

		aParser.parseQueryResult(this.getClass()
				.getResourceAsStream("/sparqlxml/dbpedia-stringlang-bug.srx"));

		assertEquals(2, handler.getBindingSets().size());
		assertEquals("Altin Lala", handler.getBindingSets().get(0).getBinding("lc").getValue().stringValue());
		assertEquals("http://de.dbpedia.org/resource/Altin_Lala",
				handler.getBindingSets().get(0).getBinding("subj").getValue().stringValue());
		assertEquals("Hans Lala", handler.getBindingSets().get(1).getBinding("lc").getValue().stringValue());
		assertEquals("http://de.dbpedia.org/resource/Hans_Lala",
				handler.getBindingSets().get(1).getBinding("subj").getValue().stringValue());
	}

	/**
	 * Test with Secure processing setting on.
	 *
	 * @throws Exception
	 */
	@Test
	public void testEntityExpansionSecureProcessing() throws Exception {
		QueryResultCollector handler = new QueryResultCollector();
		ParseErrorCollector errorCollector = new ParseErrorCollector();
		QueryResultParser aParser = QueryResultIO.createTupleParser(TupleQueryResultFormat.SPARQL)
				.setQueryResultHandler(handler)
				.set(XMLParserSettings.SECURE_PROCESSING, true)
				.setParseErrorListener(errorCollector);

		try {
			// this should trigger a SAX parse exception that will blow up at
			// the 64k entity limit rather than OOMing
			aParser.parseQueryResult(this.getClass().getResourceAsStream("/sparqlxml/bad-entity-expansion-limit.srx"));
			fail("Parser did not throw an exception");
		} catch (QueryResultParseException e) {
			// assertTrue(e.getMessage().contains(
			// "The parser has encountered more than \"64,000\" entity
			// expansions in this document; this is the limit imposed by the
			// "));
		}
		assertEquals(0, errorCollector.getWarnings().size());
		assertEquals(0, errorCollector.getErrors().size());
		assertEquals(1, errorCollector.getFatalErrors().size());
	}

	/**
	 * Test with Secure processing setting off.
	 * <p>
	 * IMPORTANT: Only turn this on to verify it is still working, as there is no way to safely perform this test.
	 * <p>
	 * WARNING: This test will cause an OutOfMemoryException when it eventually fails, as it will eventually fail.
	 *
	 * @throws Exception
	 */
	@Ignore
	@Test(timeout = 10000)
	public void testEntityExpansionNoSecureProcessing() throws Exception {
		QueryResultCollector handler = new QueryResultCollector();
		ParseErrorCollector errorCollector = new ParseErrorCollector();
		QueryResultParser aParser = QueryResultIO.createTupleParser(TupleQueryResultFormat.SPARQL)
				.setQueryResultHandler(handler)
				.set(XMLParserSettings.SECURE_PROCESSING, false)
				.setParseErrorListener(errorCollector);

		try {
			// IMPORTANT: This will not use the entity limit
			aParser.parseQueryResult(this.getClass().getResourceAsStream("/sparqlxml/bad-entity-expansion-limit.srx"));
			fail("Parser did not throw an exception");
		} catch (QueryResultParseException e) {
			// assertTrue(e.getMessage().contains(
			// "The parser has encountered more than \"64,000\" entity
			// expansions in this document; this is the limit imposed by the"));
		}
		assertEquals(0, errorCollector.getWarnings().size());
		assertEquals(0, errorCollector.getErrors().size());
		assertEquals(1, errorCollector.getFatalErrors().size());
	}

	@Test
	public void testSupportedSettings() throws Exception {
		assertTrue(QueryResultIO.createTupleParser(TupleQueryResultFormat.SPARQL).getSupportedSettings().size() > 0);
	}
}
