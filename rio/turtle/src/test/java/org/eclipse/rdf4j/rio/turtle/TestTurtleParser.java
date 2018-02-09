/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

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
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class TestTurtleParser {

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
	public void setUp()
		throws Exception
	{
		parser = new TurtleParser();
		parser.setParseErrorListener(errorCollector);
		parser.setRDFHandler(statementCollector);
		parser.setParseLocationListener(locationListener);
	}

	@Test
	public void testParseDots()
		throws Exception
	{

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
	public void testParseIllegalURIFatal()
		throws Exception
	{
		String data = " <urn:foo_bar\\r> <urn:foo> <urn:bar> ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		try {
			parser.parse(new StringReader(data), baseURI);
			fail("default config should result in fatal error / parse exception");
		}
		catch (RDFParseException e) {
			// expected
		}
	}

	@Test
	public void testParseIllegalURINonFatal()
		throws Exception
	{
		String data = " <urn:foo_bar\\r> <urn:foo> <urn:bar> ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_URI_SYNTAX);
		parser.parse(new StringReader(data), baseURI);
		assertThat(errorCollector.getErrors(), hasSize(1));
		assertThat(errorCollector.getFatalErrors(), empty());
		assertThat(statementCollector.getStatements(), not(empty()));
		assertThat("only syntactically legal triples should have been reported",
				statementCollector.getStatements(), hasSize(1));
	}

	@Test
	public void testParseIllegalURINoVerify()
		throws Exception
	{
		String data = " <urn:foo_bar\\r> <urn:foo> <urn:bar> ; <urn:foo2> <urn:bar2> . <urn:foobar> <urn:food> <urn:barf> . ";

		parser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false);

		parser.parse(new StringReader(data), baseURI);
		assertThat(errorCollector.getErrors(), empty());
		assertThat(errorCollector.getFatalErrors(), empty());
		assertThat(statementCollector.getStatements(), not(empty()));
		assertThat("all triples should have been reported", statementCollector.getStatements(), hasSize(3));
	}

	@Test
	public void testParseBNodes()
		throws Exception
	{
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
	public void testLineNumberReporting()
		throws Exception
	{

		InputStream in = this.getClass().getResourceAsStream("/test-newlines.ttl");
		try {
			parser.parse(in, baseURI);
			fail("expected to fail parsing input file");
		}
		catch (RDFParseException e) {
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
	public void testLineNumberReportingNoErrorsSingleLine()
		throws Exception
	{
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.");
		parser.parse(in, baseURI);
		assertEquals(1, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingNoErrorsSingleLineEndNewline()
		throws Exception
	{
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.\n");
		parser.parse(in, baseURI);
		assertEquals(2, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingNoErrorsMultipleLinesNoEndNewline()
		throws Exception
	{
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.\n<urn:a> <urn:b> <urn:d>.");
		parser.parse(in, baseURI);
		assertEquals(2, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingNoErrorsMultipleLinesEndNewline()
		throws Exception
	{
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.\n<urn:a> <urn:b> <urn:d>.\n");
		parser.parse(in, baseURI);
		assertEquals(3, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentNoEndline()
		throws Exception
	{
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("# This is just a comment");
		parser.parse(in, baseURI);
		assertEquals(1, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentEndline()
		throws Exception
	{
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("# This is just a comment\n");
		parser.parse(in, baseURI);
		assertEquals(2, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentCarriageReturn()
		throws Exception
	{
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("# This is just a comment\r");
		parser.parse(in, baseURI);
		assertEquals(2, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentCarriageReturnNewline()
		throws Exception
	{
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("# This is just a comment\r\n");
		parser.parse(in, baseURI);
		assertEquals(2, locationListener.getLineNo());
		assertEquals(-1, locationListener.getColumnNo());
	}

	@Test
	public void testParseBooleanLiteralComma()
		throws Exception
	{
		String data = "<urn:a> <urn:b> true, false .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 2);
		}
		catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testParseBooleanLiteralWhitespaceComma()
		throws Exception
	{
		String data = "<urn:a> <urn:b> true , false .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 2);
		}
		catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testParseBooleanLiteralSemicolumn()
		throws Exception
	{
		String data = "<urn:a> <urn:b> true; <urn:c> false .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 2);
		}
		catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testParseBooleanLiteralWhitespaceSemicolumn()
		throws Exception
	{
		String data = "<urn:a> <urn:b> true ; <urn:c> false .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 2);
		}
		catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void rdfXmlLoadedFromInsideAJarResolvesRelativeUris()
		throws Exception
	{
		URL zipfileUrl = TestTurtleParser.class.getResource("sample-with-turtle-data.zip");

		assertNotNull("The sample-with-turtle-data.zip file must be present for this test", zipfileUrl);

		String url = "jar:" + zipfileUrl + "!/index.ttl";

		RDFParser parser = new TurtleParser();

		StatementCollector sc = new StatementCollector();
		parser.setRDFHandler(sc);

		InputStream in = new URL(url).openStream();
		parser.parse(in, url);
		in.close();

		Collection<Statement> stmts = sc.getStatements();

		assertThat(stmts, Matchers.<Statement> iterableWithSize(2));

		Iterator<Statement> iter = stmts.iterator();

		Statement stmt1 = iter.next(), stmt2 = iter.next();

		assertEquals(vf.createIRI("http://www.example.com/#"), stmt1.getSubject());
		assertEquals(vf.createIRI("http://www.example.com/ns/#document-about"), stmt1.getPredicate());

		Resource res = (Resource)stmt1.getObject();

		String resourceUrl = res.stringValue();

		assertThat(resourceUrl, CoreMatchers.startsWith("jar:" + zipfileUrl + "!"));

		URL javaUrl = new URL(resourceUrl);
		assertEquals("jar", javaUrl.getProtocol());

		InputStream uc = javaUrl.openStream();
		assertEquals("The resource stream should be empty", -1, uc.read());
		uc.close();

		assertEquals(res, stmt2.getSubject());
		assertEquals(DC.TITLE, stmt2.getPredicate());
		assertEquals(vf.createLiteral("Empty File"), stmt2.getObject());
	}
}
