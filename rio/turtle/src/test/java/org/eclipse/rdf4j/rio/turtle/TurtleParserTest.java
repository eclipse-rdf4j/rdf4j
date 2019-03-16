/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import org.assertj.core.api.AbstractBooleanAssert;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.SimpleParseLocationListener;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author jeen
 */
public class TurtleParserTest {

	private TurtleParser parser;

	private ValueFactory vf = SimpleValueFactory.getInstance();

	private final ParseErrorCollector errorCollector = new ParseErrorCollector();

	private final StatementCollector statementCollector = new StatementCollector();

	private final String prefixes = "@prefix ex: <http://example.org/ex/> . \n@prefix : <http://example.org/> . \n";

	private final String baseURI = "http://example.org/";

	private SimpleParseLocationListener locationListener = new SimpleParseLocationListener();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() {
		parser = new TurtleParser();
		parser.setParseErrorListener(errorCollector);
		parser.setRDFHandler(statementCollector);
		parser.setParseLocationListener(locationListener);
	}

	@Test
	public void testParseDots() throws IOException {
		String data = prefixes + " ex:foo.bar ex:\\~foo.bar ex:foobar. ";

		parser.parse(new StringReader(data), baseURI);

		assertTrue(errorCollector.getWarnings().isEmpty());
		assertTrue(errorCollector.getErrors().isEmpty());
		assertTrue(errorCollector.getFatalErrors().isEmpty());

		assertFalse(statementCollector.getStatements().isEmpty());
		assertEquals(1, statementCollector.getStatements().size());

		for (Statement st : statementCollector.getStatements()) {
			System.out.println(st);
		}
	}

	@Test
	public void testParseIllegalURIFatal() throws IOException {
		String data = " <urn:foo_bar\\r> <urn:foo> <urn:bar> ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		try {
			parser.parse(new StringReader(data), baseURI);
			fail("default config should result in fatal error / parse exception");
		} catch (RDFParseException e) {
			// expected
		}
	}

	@Test
	public void testParseIllegalURINonFatal() throws IOException {
		String data = " <urn:foo_bar\\r> <urn:foo> <urn:bar> ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_URI_SYNTAX);
		parser.parse(new StringReader(data), baseURI);
		assertThat(errorCollector.getErrors()).hasSize(1);
		assertThat(errorCollector.getFatalErrors()).isEmpty();
		assertThat(statementCollector.getStatements()).isNotEmpty();
		assertThat(statementCollector.getStatements()).hasSize(1)
				.overridingErrorMessage("only syntactically legal triples should have been reported");
	}

	@Test
	public void testParseIllegalURINoVerify() throws IOException {
		String data = " <urn:foo_bar\\r> <urn:foo> <urn:bar> ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		parser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false);

		parser.parse(new StringReader(data), baseURI);
		assertThat(errorCollector.getErrors()).isEmpty();
		assertThat(errorCollector.getFatalErrors()).isEmpty();
		assertThat(statementCollector.getStatements()).isNotEmpty();
		assertThat(statementCollector.getStatements()).hasSize(3)
				.overridingErrorMessage("all triples should have been reported");
	}

	@Test
	public void testParseIllegalDatatypeURIFatal() throws IOException {
		String data = " <urn:foo_bar> <urn:foo> \"a\"^^<urn:foo bar> ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		try {
			parser.parse(new StringReader(data), baseURI);
			fail("default config should result in fatal error / parse exception");
		} catch (RDFParseException e) {
			// expected
		}
	}

	@Test
	public void testParseIllegalDatatypeValueFatalIRI() throws IOException {
		String data = " <urn:foo_bar> <urn:foo> \"a\"^^\"b\" ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		try {
			parser.parse(new StringReader(data), baseURI);
			fail("default config should result in fatal error / parse exception");
		} catch (RDFParseException e) {
			// expected
		}
	}

	@Test
	public void testParseIllegalDatatypeURINonFatal() throws IOException {
		String data = " <urn:foo_bar> <urn:foo> \"a\"^^<urn:foo bar> ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_URI_SYNTAX);
		parser.parse(new StringReader(data), baseURI);
		assertThat(errorCollector.getErrors()).hasSize(2);
		assertThat(errorCollector.getFatalErrors()).isEmpty();
		assertThat(statementCollector.getStatements()).isNotEmpty();
		assertThat(statementCollector.getStatements()).hasSize(2)
				.overridingErrorMessage("only syntactically legal triples should have been reported");
	}

	@Test
	public void testParseIllegalDatatypValueINonFatalIRI() throws IOException {
		String data = " <urn:foo_bar> <urn:foo> \"a\"^^\"b\" ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		try {
			parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_URI_SYNTAX);
			parser.parse(new StringReader(data), baseURI);
			fail("literal as datatype should result in fatal error / parse exception");
		} catch (RDFParseException e) {
			// expected
		}
	}

	@Test
	public void testParseIllegalDatatypeURINoVerify() throws IOException {
		String data = " <urn:foo_bar> <urn:foo> \"a\"^^<urn:foo bar> ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		parser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false);

		parser.parse(new StringReader(data), baseURI);
		assertThat(errorCollector.getErrors()).isEmpty();
		assertThat(errorCollector.getFatalErrors()).isEmpty();
		assertThat(statementCollector.getStatements()).isNotEmpty();
		assertThat(statementCollector.getStatements()).hasSize(3)
				.overridingErrorMessage("all triples should have been reported");
	}

	@Test
	public void testParseIllegalDatatypValueINoVerify() throws IOException {
		String data = " <urn:foo_bar> <urn:foo> \"a\"^^\"b\" ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		try {
			parser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false);
			parser.parse(new StringReader(data), baseURI);
			fail("literal as datatype should result in fatal error / parse exception");
		} catch (RDFParseException e) {
			// expected
		}
	}

	@Test
	public void testUnparsableIRIFatal() throws IOException {
		// subject IRI is not processable by ParsedIRI
		String data = " <http://www:example.org/> <urn:foo> <urn:bar> . ";

		try {
			parser.parse(new StringReader(data), baseURI);
			fail("default config should result in fatal error / parse exception");
		} catch (RDFParseException e) {
			// expected
		}

	}

	@Test
	public void testUnparsableIRINonFatal() throws IOException {
		// subject IRI is not processable by ParsedIRI
		String data = " <http://www:example.org/> <urn:foo> <urn:bar> . <urn:foo2> <urn:foo> <urn:bar> .";
		parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_URI_SYNTAX);
		parser.parse(new StringReader(data), baseURI);
		assertThat(errorCollector.getErrors()).hasSize(1);
		assertThat(errorCollector.getFatalErrors()).isEmpty();
		assertThat(statementCollector.getStatements()).isNotEmpty();
		assertThat(statementCollector.getStatements()).hasSize(1)
				.overridingErrorMessage("only syntactically legal triples should have been reported");

	}

	@Test
	public void testUnparsableIRINoVerify() throws IOException {
		// subject IRI is not processable by ParsedIRI
		String data = " <http://www:example.org/> <urn:foo> <urn:bar> . <urn:foo2> <urn:foo> <urn:bar> .";
		parser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false);

		parser.parse(new StringReader(data), baseURI);
		assertThat(errorCollector.getErrors()).isEmpty();
		assertThat(errorCollector.getFatalErrors()).isEmpty();
		assertThat(statementCollector.getStatements()).isNotEmpty();
		assertThat(statementCollector.getStatements()).hasSize(2)
				.overridingErrorMessage("all triples should have been reported");

	}

	@Test
	public void testParseBNodes() throws IOException {
		String data = prefixes + " [ :p  :o1,:2 ] . ";

		parser.parse(new StringReader(data), baseURI);

		assertTrue(errorCollector.getWarnings().isEmpty());
		assertTrue(errorCollector.getErrors().isEmpty());
		assertTrue(errorCollector.getFatalErrors().isEmpty());

		assertFalse(statementCollector.getStatements().isEmpty());
		assertEquals(2, statementCollector.getStatements().size());

		for (Statement st : statementCollector.getStatements()) {
			System.out.println(st);
		}
	}

	@Test
	public void testLineNumberReporting() throws IOException {
		InputStream in = this.getClass().getResourceAsStream("/test-newlines.ttl");
		try {
			parser.parse(in, baseURI);
			fail("expected to fail parsing input file");
		} catch (RDFParseException e) {
			// expected
			assertFalse(errorCollector.getFatalErrors().isEmpty());
			final String error = errorCollector.getFatalErrors().get(0);
			// expected to fail at line 9.
			assertTrue(error.contains("(9,"));
			assertEquals(9, locationListener.getLineNo());
			assertEquals(-1, locationListener.getColumnNo());
		}
	}

	@Test
	public void testLineNumberReportingNoErrorsSingleLine() throws IOException {
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.");
		parser.parse(in, baseURI);
		assertEquals(1, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingNoErrorsSingleLineEndNewline() throws IOException {
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.\n");
		parser.parse(in, baseURI);
		assertEquals(2, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingNoErrorsMultipleLinesNoEndNewline() throws IOException {
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.\n<urn:a> <urn:b> <urn:d>.");
		parser.parse(in, baseURI);
		assertEquals(2, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingNoErrorsMultipleLinesEndNewline() throws IOException {
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.\n<urn:a> <urn:b> <urn:d>.\n");
		parser.parse(in, baseURI);
		assertEquals(3, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentNoEndline() throws IOException {
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("# This is just a comment");
		parser.parse(in, baseURI);
		assertEquals(1, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentEndline() throws IOException {
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("# This is just a comment\n");
		parser.parse(in, baseURI);
		assertEquals(2, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentCarriageReturn() throws IOException {
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("# This is just a comment\r");
		parser.parse(in, baseURI);
		assertEquals(2, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentCarriageReturnNewline() throws IOException {
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("# This is just a comment\r\n");
		parser.parse(in, baseURI);
		assertEquals(2, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testParseBooleanLiteralComma() throws IOException {
		String data = "<urn:a> <urn:b> true, false .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 2);
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testParseBooleanLiteralWhitespaceComma() throws IOException {
		String data = "<urn:a> <urn:b> true , false .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 2);
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testParseBooleanLiteralSemicolumn() throws IOException {
		String data = "<urn:a> <urn:b> true; <urn:c> false .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 2);
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testParseBooleanLiteralWhitespaceSemicolumn() throws IOException {
		String data = "<urn:a> <urn:b> true ; <urn:c> false .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 2);
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void rdfXmlLoadedFromInsideAJarResolvesRelativeUris() throws IOException {
		URL zipfileUrl = TurtleParserTest.class.getResource("sample-with-turtle-data.zip");

		assertNotNull("The sample-with-turtle-data.zip file must be present for this test", zipfileUrl);

		String url = "jar:" + zipfileUrl + "!/index.ttl";

		RDFParser parser = new TurtleParser();

		StatementCollector sc = new StatementCollector();
		parser.setRDFHandler(sc);

		try (InputStream in = new URL(url).openStream()) {
			parser.parse(in, url);
		}

		Collection<Statement> stmts = sc.getStatements();

		assertThat(stmts).hasSize(2);

		Iterator<Statement> iter = stmts.iterator();

		Statement stmt1 = iter.next(), stmt2 = iter.next();

		assertEquals(vf.createIRI("http://www.example.com/#"), stmt1.getSubject());
		assertEquals(vf.createIRI("http://www.example.com/ns/#document-about"), stmt1.getPredicate());

		Resource res = (Resource) stmt1.getObject();

		String resourceUrl = res.stringValue();

		assertThat(resourceUrl).startsWith("jar:" + zipfileUrl + "!");

		URL javaUrl = new URL(resourceUrl);
		assertEquals("jar", javaUrl.getProtocol());

		try (InputStream uc = javaUrl.openStream()) {
			assertEquals("The resource stream should be empty", -1, uc.read());
		}

		assertEquals(res, stmt2.getSubject());
		assertEquals(DC.TITLE, stmt2.getPredicate());
		assertEquals(vf.createLiteral("Empty File"), stmt2.getObject());
	}

	@Test
	public void testIllegalNewlineInQuotedObjectLiteral() throws IOException {
		String data = "<urn:a> <urn:b> \"not\nallowed\" .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			fail("Did not catch illegal new line");
		} catch (RDFParseException e) {
			assertThat(e.getMessage().startsWith("Illegal carriage return or new line in literal"));
		}
	}

	@Test
	public void testLegalNewlineInTripleQuotedObjectLiteral() throws IOException {
		String data = "<urn:a> <urn:b> \"\"\"is\nallowed\"\"\" .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 1);
		} catch (RDFParseException e) {
			fail("New line is legal inside triple quoted literal");
		}
	}

}
