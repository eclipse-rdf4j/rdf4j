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
package org.eclipse.rdf4j.rio.turtle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.SimpleParseLocationListener;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.helpers.TurtleParserSettings;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class TurtleParserTest {

	private TurtleParser parser;

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private final ParseErrorCollector errorCollector = new ParseErrorCollector();

	private final StatementCollector statementCollector = new StatementCollector();

	private final String prefixes = "@prefix ex: <http://example.org/ex/> . \n@prefix : <http://example.org/> . \n";

	private final String baseURI = "http://example.org/";

	private final SimpleParseLocationListener locationListener = new SimpleParseLocationListener();

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
	public void testLineNumberReportingInLongStringLiterals() throws IOException {
		assertEquals(0, locationListener.getLineNo());
		assertEquals(0, locationListener.getColumnNo());
		Reader in = new StringReader("<urn:a> <urn:b> \"\"\"is\nallowed\nin\na very long string\"\"\" .");
		parser.parse(in, baseURI);
		assertEquals(4, locationListener.getLineNo());
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

	@Test
	public void testLegalUnicodeInTripleSubject() throws IOException {
		String data = "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n<r:\uD804\uDC9D> a xsd:string .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 1);
		} catch (RDFParseException e) {
			fail("Complex unicode characters should be parsed correctly (" + e.getMessage() + ")");
		}
	}

	@Test
	public void testOverflowingUnicodeInTripleSubject() throws IOException {
		String data = "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n<r:\uD800\uDF32\uD800\uDF3F\uD800\uDF44\uD800\uDF39\uD800\uDF43\uD800\uDF3A> a xsd:string .";
		Reader r = new StringReader(data);

		try {
			parser.parse(r, baseURI);
			assertTrue(statementCollector.getStatements().size() == 1);
		} catch (RDFParseException e) {
			fail("Complex unicode characters should be parsed correctly (" + e.getMessage() + ")");
		}
	}

	/**
	 * Extend standard Turtle parser to also accept RDF-star data (see GH-2511)
	 *
	 */
	@Test
	public void testParseRDFStarData() throws IOException {
		IRI bob = vf.createIRI("http://example.com/bob");
		IRI alice = vf.createIRI("http://example.com/alice");
		IRI book = vf.createIRI("http://example.com/book");
		IRI otherbook = vf.createIRI("http://example.com/otherbook");
		IRI bobshomepage = vf.createIRI("http://example.com/bobshomepage");
		IRI a = vf.createIRI("http://example.org/a");
		IRI b = vf.createIRI("http://example.com/b");
		IRI c = vf.createIRI("http://example.com/c");
		IRI valid = vf.createIRI("http://example.com/valid");
		Literal abcDate = vf.createLiteral("1999-08-16", XSD.DATE);
		Literal birthDate = vf.createLiteral("1908-03-18", XSD.DATE);
		Literal titleEn = vf.createLiteral("Example book", "en");
		Literal titleDe = vf.createLiteral("Beispielbuch", "de");
		Literal titleEnUs = vf.createLiteral("Example Book", "en-US");

		Triple bobCreatedBook = vf.createTriple(bob, DCTERMS.CREATED, book);
		Triple aliceKnowsBobCreatedBook = vf.createTriple(alice, FOAF.KNOWS, bobCreatedBook);
		Triple bobCreatedBookKnowsAlice = vf.createTriple(bobCreatedBook, FOAF.KNOWS, alice);
		Triple bookCreatorAlice = vf.createTriple(book, DCTERMS.CREATOR, alice);
		Triple aliceCreatedBook = vf.createTriple(alice, DCTERMS.CREATED, book);
		Triple abc = vf.createTriple(a, b, c);
		Triple bobBirthdayDate = vf.createTriple(bob, FOAF.BIRTHDAY, birthDate);
		Triple bookTitleEn = vf.createTriple(book, DCTERMS.TITLE, titleEn);
		Triple bookTitleDe = vf.createTriple(book, DCTERMS.TITLE, titleDe);
		Triple bookTitleEnUs = vf.createTriple(book, DCTERMS.TITLE, titleEnUs);

		try (InputStream in = this.getClass().getResourceAsStream("/test-rdfstar.ttls")) {
			parser.parse(in, baseURI);

			Collection<Statement> stmts = statementCollector.getStatements();

			assertEquals(10, stmts.size());

			assertTrue(stmts.contains(vf.createStatement(bob, FOAF.KNOWS, aliceKnowsBobCreatedBook)));
			assertTrue(stmts.contains(vf.createStatement(bobCreatedBookKnowsAlice, DCTERMS.SOURCE, otherbook)));
			assertTrue(stmts.contains(vf.createStatement(bobshomepage, DCTERMS.SOURCE, bookCreatorAlice)));
			assertTrue(stmts.contains(vf.createStatement(bookCreatorAlice, DCTERMS.SOURCE, bobshomepage)));
			assertTrue(stmts.contains(vf.createStatement(bookCreatorAlice, DCTERMS.REQUIRES, aliceCreatedBook)));
			assertTrue(stmts.contains(vf.createStatement(abc, valid, abcDate)));
			assertTrue(stmts.contains(vf.createStatement(bobBirthdayDate, DCTERMS.SOURCE, bobshomepage)));
			assertTrue(stmts.contains(vf.createStatement(bookTitleEn, DCTERMS.SOURCE, bobshomepage)));
			assertTrue(stmts.contains(vf.createStatement(bookTitleDe, DCTERMS.SOURCE, bobshomepage)));
			assertTrue(stmts.contains(vf.createStatement(bookTitleEnUs, DCTERMS.SOURCE, bobshomepage)));
		}
	}

	@Test(expected = RDFParseException.class)
	public void testParseRDFStar_TurtleStarDisabled() throws IOException {
		parser.getParserConfig().set(TurtleParserSettings.ACCEPT_TURTLESTAR, false);

		try (InputStream in = this.getClass().getResourceAsStream("/test-rdfstar.ttls")) {
			parser.parse(in, baseURI);
		}
	}

}
