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
package org.eclipse.rdf4j.rio.rdfxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Custom tests for RDFXML Parser.
 *
 * @author Michael Grove
 */
public class RDFXMLParserCustomTest {

	/**
	 * Test with the default ParserConfig settings. Ie, setParserConfig is not called.
	 *
	 * @throws Exception
	 */
	@Test
	public void testEntityExpansionDefaultSettings() throws Exception {
		final Model aGraph = new LinkedHashModel();
		ParseErrorCollector errorCollector = new ParseErrorCollector();
		RDFParser aParser = Rio.createParser(RDFFormat.RDFXML)
				.setRDFHandler(new StatementCollector(aGraph))
				.setParseErrorListener(errorCollector);

		try {
			// this should trigger a SAX parse exception that will blow up at the
			// 64k entity limit rather than OOMing
			aParser.parse(
					this.getClass().getResourceAsStream("/testcases/rdfxml/openrdf/bad-entity-expansion-limit.rdf"),
					"http://example.org");
			fail("Parser did not throw an exception");
		} catch (RDFParseException e) {
			// assertTrue(e.getMessage().contains(
			// "The parser has encountered more than \"64,000\" entity expansions in this document; this is the limit
			// imposed by the "));
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
		final Model aGraph = new LinkedHashModel();
		ParseErrorCollector errorCollector = new ParseErrorCollector();
		ParserConfig config = new ParserConfig();
		RDFParser aParser = Rio.createParser(RDFFormat.RDFXML)
				.setRDFHandler(new StatementCollector(aGraph))
				.setParserConfig(config)
				.setParseErrorListener(errorCollector);

		try {
			// this should trigger a SAX parse exception that will blow up at the
			// 64k entity limit rather than OOMing
			aParser.parse(
					this.getClass().getResourceAsStream("/testcases/rdfxml/openrdf/bad-entity-expansion-limit.rdf"),
					"http://example.org");
			fail("Parser did not throw an exception");
		} catch (RDFParseException e) {
			// assertTrue(e.getMessage().contains(
			// "The parser has encountered more than \"64,000\" entity expansions in this document; this is the limit
			// imposed by the "));
		}
		assertEquals(0, errorCollector.getWarnings().size());
		assertEquals(0, errorCollector.getErrors().size());
		assertEquals(1, errorCollector.getFatalErrors().size());
	}

	/**
	 * Test with Secure processing setting on.
	 *
	 * @throws Exception
	 */
	@Test
	public void testEntityExpansionSecureProcessing() throws Exception {
		final Model aGraph = new LinkedHashModel();
		ParseErrorCollector errorCollector = new ParseErrorCollector();
		RDFParser aParser = Rio.createParser(RDFFormat.RDFXML)
				.setRDFHandler(new StatementCollector(aGraph))
				.set(XMLParserSettings.SECURE_PROCESSING, true)
				.setParseErrorListener(errorCollector);

		try {
			// this should trigger a SAX parse exception that will blow up at the
			// 64k entity limit rather than OOMing
			aParser.parse(
					this.getClass().getResourceAsStream("/testcases/rdfxml/openrdf/bad-entity-expansion-limit.rdf"),
					"http://example.org");
			fail("Parser did not throw an exception");
		} catch (RDFParseException e) {
			// assertTrue(e.getMessage().contains(
			// "The parser has encountered more than \"64,000\" entity expansions in this document; this is the limit
			// imposed by the "));
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
		final Model aGraph = new LinkedHashModel();
		ParseErrorCollector errorCollector = new ParseErrorCollector();
		RDFParser aParser = Rio.createParser(RDFFormat.RDFXML)
				.setRDFHandler(new StatementCollector(aGraph))
				.set(XMLParserSettings.SECURE_PROCESSING, false)
				.setParseErrorListener(errorCollector);

		try {
			// IMPORTANT: This will not use the entity limit
			aParser.parse(
					this.getClass().getResourceAsStream("/testcases/rdfxml/openrdf/bad-entity-expansion-limit.rdf"),
					"http://example.org");
			fail("Parser did not throw an exception");
		} catch (RDFParseException e) {
			// assertTrue(e.getMessage().contains(
			// "The parser has encountered more than \"64,000\" entity expansions in this document; this is the limit
			// imposed by the"));
		}
		assertEquals(0, errorCollector.getWarnings().size());
		assertEquals(0, errorCollector.getErrors().size());
		assertEquals(1, errorCollector.getFatalErrors().size());
	}

	@Test
	public void testParseCollection() throws Exception {
		// Example from:
		// http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-parsetype-Collection
		StringBuilder string = new StringBuilder();
		string.append("<?xml version=\"1.0\"?>\n");
		string.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" ");
		string.append(" xmlns:ex=\"http://example.org/stuff/1.0/\"> \n");
		string.append("  <rdf:Description rdf:about=\"http://example.org/basket\"> \n");
		string.append("    <ex:hasFruit rdf:parseType=\"Collection\">\n");
		string.append("      <rdf:Description rdf:about=\"http://example.org/banana\"/>\n");
		string.append("      <rdf:Description rdf:about=\"http://example.org/apple\"/>\n");
		string.append("      <rdf:Description rdf:about=\"http://example.org/pear\"/>\n");
		string.append("    </ex:hasFruit>\n");
		string.append("  </rdf:Description>\n");
		string.append("</rdf:RDF>");

		Model parse = Rio.parse(new StringReader(string.toString()), "", RDFFormat.RDFXML);
		Rio.write(parse, System.out, RDFFormat.NTRIPLES);
		assertEquals(7, parse.size());
		assertEquals(3, parse.filter(null, RDF.FIRST, null).size());
		assertEquals(3, parse.filter(null, RDF.REST, null).size());
		assertEquals(1, parse.filter(null, null, RDF.NIL).size());
	}

	@Test
	public void testParseCommentAtStart() throws Exception {
		// Example from:
		// http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-parsetype-Collection
		StringBuilder string = new StringBuilder();
		string.append("<!-- Test comment for parser to ignore -->\n");
		string.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" ");
		string.append(" xmlns:ex=\"http://example.org/stuff/1.0/\"> \n");
		string.append("  <rdf:Description rdf:about=\"http://example.org/basket\"> \n");
		string.append("    <ex:hasFruit>\n");
		string.append("    	Mango\n");
		string.append("    </ex:hasFruit>\n");
		string.append("  </rdf:Description>\n");
		string.append("</rdf:RDF>");

		Model parse = Rio.parse(new StringReader(string.toString()), "", RDFFormat.RDFXML);
		Rio.write(parse, System.out, RDFFormat.NTRIPLES);
		assertEquals(1, parse.size());
	}

	@Test
	public void testSupportedSettings() throws Exception {
		assertEquals(26, Rio.createParser(RDFFormat.RDFXML).getSupportedSettings().size());
	}

	@Test
	public void tset() throws IOException {
		ValueFactory vf = SimpleValueFactory.getInstance();
		Model mm = new LinkedHashModel();
		mm.add(vf.createStatement(vf.createIRI("urn:foo:момтен.т=2-"), RDF.TYPE, vf.createIRI("urn:foo:момтен")));
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			RDFXMLPrettyWriter rdfWriter = new RDFXMLPrettyWriter(System.out);
			Rio.write(mm, rdfWriter);
			RDFXMLPrettyWriter rdfWriter2 = new RDFXMLPrettyWriter(bos);
			Rio.write(mm, rdfWriter2);
			Model mmm = Rio.parse(new ByteArrayInputStream(bos.toByteArray()), "urn:base:", RDFFormat.RDFXML);
			mmm.forEach(System.out::println);
		}
	}
}
