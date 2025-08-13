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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.AbstractParserTest;
import org.eclipse.rdf4j.rio.LanguageHandler;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.languages.RFC3066LanguageHandler;
import org.junit.jupiter.api.Test;

/**
 * @author jeen
 */
public class TurtleParserTest extends AbstractParserTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private final String prefixes = "@prefix ex: <http://example.org/ex/> . \n@prefix : <http://example.org/> . \n";

	private final String baseURI = "http://example.org/";

	private final Statement simpleSPOStatement = vf.createStatement(vf.createIRI("http://example/s"),
			vf.createIRI("http://example/p"), vf.createIRI("http://example/o"));
	private final Triple simpleSPOTriple = vf.createTriple(vf.createIRI("http://example/s"),
			vf.createIRI("http://example/p"), vf.createIRI("http://example/o"));

	@Override
	protected RDFParser createRDFParser() {
		return new TurtleParser();
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
			locationListener.assertListener(9, -1);
		}
	}

	@Test
	public void testLineNumberReportingNoErrorsSingleLine() throws IOException {
		locationListener.assertListener(0, 0);
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.");
		parser.parse(in, baseURI);
		locationListener.assertListener(1, -1);
	}

	@Test
	public void testLineNumberReportingNoErrorsSingleLineEndNewline() throws IOException {
		locationListener.assertListener(0, 0);
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.\n");
		parser.parse(in, baseURI);
		locationListener.assertListener(2, -1);
	}

	@Test
	public void testLineNumberReportingNoErrorsMultipleLinesNoEndNewline() throws IOException {
		locationListener.assertListener(0, 0);
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.\n<urn:a> <urn:b> <urn:d>.");
		parser.parse(in, baseURI);
		locationListener.assertListener(2, -1);
	}

	@Test
	public void testLineNumberReportingNoErrorsMultipleLinesEndNewline() throws IOException {
		locationListener.assertListener(0, 0);
		Reader in = new StringReader("<urn:a> <urn:b> <urn:c>.\n<urn:a> <urn:b> <urn:d>.\n");
		parser.parse(in, baseURI);
		locationListener.assertListener(3, -1);
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentNoEndline() throws IOException {
		locationListener.assertListener(0, 0);
		Reader in = new StringReader("# This is just a comment");
		parser.parse(in, baseURI);
		locationListener.assertListener(1, -1);
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentEndline() throws IOException {
		locationListener.assertListener(0, 0);
		Reader in = new StringReader("# This is just a comment\n");
		parser.parse(in, baseURI);
		locationListener.assertListener(2, -1);
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentCarriageReturn() throws IOException {
		locationListener.assertListener(0, 0);
		Reader in = new StringReader("# This is just a comment\r");
		parser.parse(in, baseURI);
		locationListener.assertListener(2, -1);
	}

	@Test
	public void testLineNumberReportingOnlySingleCommentCarriageReturnNewline() throws IOException {
		locationListener.assertListener(0, 0);
		Reader in = new StringReader("# This is just a comment\r\n");
		parser.parse(in, baseURI);
		locationListener.assertListener(2, -1);
	}

	@Test
	public void testLineNumberReportingInLongStringLiterals() throws IOException {
		locationListener.assertListener(0, 0);
		Reader in = new StringReader("<urn:a> <urn:b> \"\"\"is\nallowed\nin\na very long string\"\"\" .");
		parser.parse(in, baseURI);
		locationListener.assertListener(4, -1);
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

		assertNotNull(zipfileUrl, "The sample-with-turtle-data.zip file must be present for this test");

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
			assertEquals(-1, uc.read(), "The resource stream should be empty");
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

	@Test
	public void testParseAdditionalDatatypes() throws IOException {
		String data = prefixes + ":s :p \"o\"^^rdf:JSON . \n"
				+ ":s :p \"o\"^^rdf:HTML . \n"
				+ ":s :p \"o\"^^rdf:XMLLiteral . ";
		Reader r = new StringReader(data);

		try {
			parser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);

			parser.parse(r, baseURI);

			assertThat(errorCollector.getErrors()).isEmpty();

			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).hasSize(3);

			Iterator<Statement> iter = stmts.iterator();

			Statement stmt1 = iter.next(), stmt2 = iter.next(), stmt3 = iter.next();

			assertEquals(CoreDatatype.RDF.JSON.getIri(), ((Literal) stmt1.getObject()).getDatatype());
			assertEquals(CoreDatatype.RDF.HTML.getIri(), ((Literal) stmt2.getObject()).getDatatype());
			assertEquals(CoreDatatype.RDF.XMLLITERAL.getIri(), ((Literal) stmt3.getObject()).getDatatype());
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	/**
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/nt-ttl12-langdir-1.ttl
	 */
	@Test
	public void testLanguageDirectionLTR() throws IOException {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--ltr .";
		dirLangStringTestHelper(data, "en", Literal.LTR_SUFFIX, false, false);
	}

	/**
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/nt-ttl12-langdir-1.ttl
	 */
	@Test
	public void testLanguageDirectionLTRWithNormalization() throws IOException {
		String data = "<http://example/a> <http://example/b> \"Hello\"@EN--ltr .";
		dirLangStringTestHelper(data, "en", Literal.LTR_SUFFIX, true, false);
	}

	/**
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/nt-ttl12-langdir-2.ttl
	 */
	@Test
	public void testLanguageDirectionRTL() throws IOException {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--rtl .";
		dirLangStringTestHelper(data, "en", Literal.RTL_SUFFIX, false, false);
	}

	/**
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/nt-ttl12-langdir-2.ttl
	 */
	@Test
	public void testLanguageDirectionRTLWithNormalization() throws IOException {
		String data = "<http://example/a> <http://example/b> \"Hello\"@EN--rtl .";
		dirLangStringTestHelper(data, "en", Literal.RTL_SUFFIX, true, false);
	}

	/**
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/nt-ttl12-langdir-bad-1.ttl
	 */
	@Test
	public void testBadLanguageDirection() throws IOException {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--unk .";
		dirLangStringTestHelper(data, "", "", false, true);
	}

	/**
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/nt-ttl12-langdir-bad-2.ttl
	 */
	@Test
	public void testBadCapitalizationLanguageDirection() throws IOException {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--LTR .";
		dirLangStringTestHelper(data, "", "", false, true);
	}

	@Test
	public void testDirLangStringNoLanguage() throws IOException {
		String data = "<http://example/a> <http://example/b> \"Hello\"^^rdf:dirLangString .";
		dirLangStringNoLanguageTestHelper(data);
	}

	@Test
	public void testRFC3066LanguageHandler() throws IOException {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--ltr .";

		try {
			List<LanguageHandler> customHandlers = List.of(new RFC3066LanguageHandler());
			parser.getParserConfig().set(BasicParserSettings.LANGUAGE_HANDLERS, customHandlers);
			parser.parse(new StringReader(data), baseURI);

			assertThat(errorCollector.getErrors()).isEmpty();

			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).hasSize(1);

			Iterator<Statement> iter = stmts.iterator();
			Statement stmt1 = iter.next();

			assertEquals(CoreDatatype.RDF.DIRLANGSTRING.getIri(), ((Literal) stmt1.getObject()).getDatatype());
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	private Statement simpleExampleStatement(String s, String p, String o) {
		return vf.createStatement(vf.createIRI("http://example/" + s), vf.createIRI("http://example/" + p),
				vf.createIRI("http://example/" + o));
	}

	private Triple simpleExampleTriple(String s, String p, String o) {
		return vf.createTriple(vf.createIRI("http://example/" + s), vf.createIRI("http://example/" + p),
				vf.createIRI("http://example/" + o));
	}

	/*
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/turtle12-syntax-basic-03.ttl
	 */
	@Test
	public void testTripleTermInObjectPosition() throws IOException {
		String data = "PREFIX : <http://example/>\n" + ":s :p <<(:s :p :o )>> .";

		try {
			parser.parse(new StringReader(data));
			assertEquals(1, statementCollector.getStatements().size());

			assertThat(errorCollector.getErrors()).isEmpty();

			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).hasSize(1);

			Iterator<Statement> iter = stmts.iterator();
			Statement stmt1 = iter.next();

			assertEquals(stmt1.getSubject(), vf.createIRI("http://example/s"));
			assertEquals(stmt1.getPredicate(), vf.createIRI("http://example/p"));
			assertTrue(stmt1.getObject() instanceof Triple);
			assertEquals(stmt1.getObject(), simpleSPOTriple);
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testBadTripleTermInSubjectPosition() throws IOException {
		String data = "PREFIX : <http://example/>\n" + "<<(:s :p :o )>> :p :o .";
		assertThrows(RDFParseException.class, () -> parser.parse(new StringReader(data)));
	}

	@Test
	public void testNestedTripleTerm() throws IOException {
		String data = "PREFIX : <http://example/>\n" + ":s :p <<(:s2 :p2 <<( :s3 :p3 :o3 )>> )>> .";

		try {
			parser.parse(new StringReader(data));
			assertEquals(1, statementCollector.getStatements().size());

			assertThat(errorCollector.getErrors()).isEmpty();

			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).hasSize(1);

			Iterator<Statement> iter = stmts.iterator();
			Statement stmt1 = iter.next();

			assertEquals(stmt1.getSubject(), vf.createIRI("http://example/s"));
			assertEquals(stmt1.getPredicate(), vf.createIRI("http://example/p"));

			assertInstanceOf(Triple.class, stmt1.getObject());
			Triple obj = (Triple) stmt1.getObject();
			assertEquals(obj.getSubject(), vf.createIRI("http://example/s2"));
			assertEquals(obj.getPredicate(), vf.createIRI("http://example/p2"));

			assertInstanceOf(Triple.class, obj.getObject());
			Triple objObj = (Triple) obj.getObject();
			assertEquals(objObj, simpleExampleTriple("s3", "p3", "o3"));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testNestedTripleTerm2() throws IOException {
		String data = "PREFIX : <http://example/>\n" + "_:b :p <<(:s2 :p2 <<( _:b2 :p3 \"9\"^^xsd:int )>> )>> .";

		try {
			parser.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			parser.parse(new StringReader(data));
			assertEquals(1, statementCollector.getStatements().size());

			assertThat(errorCollector.getErrors()).isEmpty();

			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).hasSize(1);

			Iterator<Statement> iter = stmts.iterator();
			Statement stmt1 = iter.next();

			assertEquals(stmt1.getSubject(), vf.createBNode("b"));
			assertEquals(stmt1.getPredicate(), vf.createIRI("http://example/p"));

			assertTrue(stmt1.getObject().isTriple());
			Triple obj = (Triple) stmt1.getObject();
			assertEquals(obj.getSubject(), vf.createIRI("http://example/s2"));
			assertEquals(obj.getPredicate(), vf.createIRI("http://example/p2"));

			assertTrue(obj.getObject().isTriple());
			obj = (Triple) obj.getObject();
			assertEquals(obj.getSubject(), vf.createBNode("b2"));
			assertEquals(obj.getPredicate(), vf.createIRI("http://example/p3"));
			assertEquals(obj.getObject(), vf.createLiteral(9));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	/*
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/nt-ttl12-syntax-1.ttl
	 */
	@Test
	public void testTripleTermNTriples() throws IOException {
		String data = "<http://example/a> <http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies> <<( <http://example/s> <http://example/p> <http://example/o> )>> .";
		try {
			parser.parse(new StringReader(data));
			assertEquals(1, statementCollector.getStatements().size());

			assertThat(errorCollector.getErrors()).isEmpty();

			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).hasSize(1);

			Iterator<Statement> iter = stmts.iterator();
			Statement stmt1 = iter.next();

			assertEquals(vf.createIRI("http://example/a"), stmt1.getSubject());
			assertEquals(RDF.REIFIES, stmt1.getPredicate());
			assertInstanceOf(Triple.class, stmt1.getObject());
			assertEquals(stmt1.getObject(), simpleSPOTriple);
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	/*
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/nt-ttl12-syntax-2.ttl
	 */
	@Test
	public void testTripleTermNTriplesNoWhitespace() throws IOException {
		String data = "<http://example/s><http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies><<(<http://example/s2><http://example/p2><http://example/o2>)>>.";

		try {
			parser.parse(new StringReader(data));
			assertEquals(1, statementCollector.getStatements().size());

			assertThat(errorCollector.getErrors()).isEmpty();

			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).hasSize(1);

			Iterator<Statement> iter = stmts.iterator();
			Statement stmt1 = iter.next();

			assertEquals(vf.createIRI("http://example/s"), stmt1.getSubject());
			assertEquals(RDF.REIFIES, stmt1.getPredicate());
			assertInstanceOf(Triple.class, stmt1.getObject());
			assertEquals(stmt1.getObject(), vf.createTriple(vf.createIRI("http://example/s2"),
					vf.createIRI("http://example/p2"), vf.createIRI("http://example/o2")));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	/*
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/nt-ttl12-nested-1.ttl
	 */
	@Test
	public void testNestedTripleTerms() throws IOException {
		String data = "<http://example/s> <http://example/p> <http://example/o> .\n" +
				"<http://example/a> <http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies> <<( <http://example/s1> <http://example/p1> <http://example/o1> )>> .\n"
				+
				"<http://example/r> <http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies> <<( <http://example/23> <http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies> <<( <http://example/s3> <http://example/p3> <http://example/o3> )>> )>> .";

		try {
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).hasSize(3);

			Iterator<Statement> iter = stmts.iterator();
			Statement stmt1 = iter.next(), stmt2 = iter.next(), stmt3 = iter.next();

			assertEquals(stmt1, simpleSPOStatement);
			assertEquals(stmt2, vf.createStatement(vf.createIRI("http://example/a"), RDF.REIFIES,
					simpleExampleTriple("s1", "p1", "o1")));
			assertEquals(stmt3, vf.createStatement(vf.createIRI("http://example/r"), RDF.REIFIES,
					vf.createTriple(vf.createIRI("http://example/23"), RDF.REIFIES,
							simpleExampleTriple("s3", "p3", "o3"))));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	/*
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/turtle12-syntax-basic-01.ttl
	 */
	@Test
	public void testSubjectReifiedTriple() throws IOException {
		String data = "PREFIX : <http://example/>\n" +
				"\n" +
				":s :p :o .\n" +
				"<<:s :p :o>> :q 123 .";
		try {
			Model model = new LinkedHashModel();
			statementCollector = new StatementCollector(model);
			parser.setRDFHandler(statementCollector);
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			assertThat(model).hasSize(3);

			assertTrue(model.contains(simpleSPOStatement));
			Model reifyingTriples = model.filter(null, RDF.REIFIES, simpleSPOTriple);
			assertThat(reifyingTriples).hasSize(1);
			Resource reifier = Models.subject(reifyingTriples).get();
			assertInstanceOf(BNode.class, reifier);
			assertTrue(model.contains(reifier, vf.createIRI("http://example/q"),
					vf.createLiteral(BigInteger.valueOf(123))));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	/*
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/turtle12-syntax-basic-02.ttl
	 */
	@Test
	public void testObjectReifiedTriple() throws IOException {
		String data = "PREFIX : <http://example/>\n" +
				"\n" +
				":s :p :o .\n" +
				":x :p <<:s :p :o>> .";
		try {
			Model model = new LinkedHashModel();
			statementCollector = new StatementCollector(model);
			parser.setRDFHandler(statementCollector);
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			assertThat(model).hasSize(3);

			assertTrue(model.contains(simpleSPOStatement));
			Model reifyingTriples = model.filter(null, RDF.REIFIES, simpleSPOTriple);
			assertThat(reifyingTriples).hasSize(1);
			Resource reifier = Models.subject(reifyingTriples).get();
			assertInstanceOf(BNode.class, reifier);
			assertTrue(model.contains(vf.createIRI("http://example/x"), vf.createIRI("http://example/p"), reifier));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	/*
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/turtle12-syntax-basic-04.ttl
	 */
	@Test
	public void testReifiedTripleNoPredObjList() throws IOException {
		String data = "PREFIX : <http://example/>\n" +
				"\n" +
				":s :p :o .\n" +
				"<<:s :p :o>> .";
		try {
			Model model = new LinkedHashModel();
			statementCollector = new StatementCollector(model);
			parser.setRDFHandler(statementCollector);
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			assertThat(model).hasSize(2);

			assertTrue(model.contains(simpleSPOStatement));
			Model reifyingTriples = model.filter(null, RDF.REIFIES, simpleSPOTriple);
			assertThat(reifyingTriples).hasSize(1);
			Resource reifier = Models.subject(reifyingTriples).get();
			assertInstanceOf(BNode.class, reifier);
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	/*
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/turtle12-syntax-inside-01.ttl
	 */
	@Test
	public void testReifiedTripleInBlankNodePropertyList() throws IOException {
		String data = "PREFIX : <http://example/>\n" +
				"\n" +
				":s :p :o .\n" +
				"[ :q <<:s :p :o>> ] :b :c .";
		try {
			Model model = new LinkedHashModel();
			statementCollector = new StatementCollector(model);
			parser.setRDFHandler(statementCollector);
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			assertThat(model).hasSize(4);

			assertTrue(model.contains(simpleSPOStatement));
			Model reifyingTriples = model.filter(null, RDF.REIFIES, simpleSPOTriple);
			assertThat(reifyingTriples).hasSize(1);
			Resource reifier = Models.subject(reifyingTriples).get();
			assertInstanceOf(BNode.class, reifier);
			assertTrue(model.contains(null, vf.createIRI("http://example/q"), reifier));
			assertTrue(model.contains(null, vf.createIRI("http://example/b"), vf.createIRI("http://example/c")));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	/*
	 * https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/syntax/turtle12-syntax-inside-02.ttl
	 */
	@Test
	public void testReifiedTripleInCollection() throws IOException {
		String data = "PREFIX : <http://example/>\n" +
				"\n" +
				":s :p :o1 .\n" +
				":s :p :o2 .\n" +
				"( <<:s :p :o1>> <<:s :p :o2>> )  :q 123 .";
		try {
			Model model = new LinkedHashModel();
			statementCollector = new StatementCollector(model);
			parser.setRDFHandler(statementCollector);
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			assertThat(model).hasSize(9);
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testReifierInReifiedTriple() throws IOException {
		String data = "PREFIX : <http://example/>\n" +
				":x :p <<:s :p :o ~ _:b1 >> .";
		try {
			Model model = new LinkedHashModel();
			statementCollector = new StatementCollector(model);
			parser.setRDFHandler(statementCollector);
			parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			assertThat(model).hasSize(2);

			assertTrue(model.contains(vf.createBNode("b1"), RDF.REIFIES, simpleSPOTriple));
			assertTrue(model.contains(vf.createIRI("http://example/x"), vf.createIRI("http://example/p"),
					vf.createBNode("b1")));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testReifierWithoutNode() throws IOException {
		String data = "PREFIX : <http://example/>\n" +
				":s :p :o ~ .";
		try {
			Model model = new LinkedHashModel();
			statementCollector = new StatementCollector(model);
			parser.setRDFHandler(statementCollector);
			parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			assertThat(model).hasSize(2);

			assertTrue(model.contains(null, RDF.REIFIES, simpleSPOTriple));
			assertTrue(model.contains(simpleSPOStatement));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testReifierAnnotation() throws IOException {
		String data = "PREFIX : <http://example/>\n" +
				":s :p :o ~ _:b1 .";
		try {
			Model model = new LinkedHashModel();
			statementCollector = new StatementCollector(model);
			parser.setRDFHandler(statementCollector);
			parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			assertThat(model).hasSize(2);

			assertTrue(model.contains(vf.createBNode("b1"), RDF.REIFIES, simpleSPOTriple));
			assertTrue(model.contains(simpleSPOStatement));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testReifierAnnotationBlock() throws IOException {
		String data = "PREFIX : <http://example/>\n" +
				":s :p :o ~ _:b1 {| :p2 :o2 |} .";
		try {
			Model model = new LinkedHashModel();
			statementCollector = new StatementCollector(model);
			parser.setRDFHandler(statementCollector);
			parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			assertThat(model).hasSize(3);

			assertTrue(model.contains(vf.createBNode("b1"), RDF.REIFIES, simpleSPOTriple));
			assertTrue(model.contains(simpleSPOStatement));
			assertTrue(model.contains(vf.createBNode("b1"), vf.createIRI("http://example/p2"),
					vf.createIRI("http://example/o2")));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testAnnotationBlock() throws IOException {
		String data = "PREFIX : <http://example/>\n" +
				":s :p :o {| :p2 :o2 ; :p3 :o3 |} .";
		try {
			Model model = new LinkedHashModel();
			statementCollector = new StatementCollector(model);
			parser.setRDFHandler(statementCollector);
			parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			assertThat(model).hasSize(4);

			assertTrue(model.contains(simpleSPOStatement));

			Model reifyingTriples = model.filter(null, RDF.REIFIES, simpleSPOTriple);
			assertThat(reifyingTriples).hasSize(1);
			Resource reifier = Models.subject(reifyingTriples).get();
			assertInstanceOf(BNode.class, reifier);

			assertTrue(model.contains(reifier, vf.createIRI("http://example/p2"), vf.createIRI("http://example/o2")));
			assertTrue(model.contains(reifier, vf.createIRI("http://example/p3"), vf.createIRI("http://example/o3")));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testVersionDirectiveDoubleQuotes() throws IOException {
		String data = "@version \"1.2\" .";
		try {
			parser.parse(new StringReader(data));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testVersionDirectiveSingleQuotes() throws IOException {
		String data = "@version '1.2' .";
		try {
			parser.parse(new StringReader(data));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testVersionDirectiveNoPeriod() throws IOException {
		String data = "@version '1.2'";
		assertThrows(RDFParseException.class, () -> parser.parse(new StringReader(data)));
	}

	@Test
	public void testVersionDirectiveMismatchedQuotes() throws IOException {
		String data = "@version '1.2\" .";
		assertThrows(RDFParseException.class, () -> parser.parse(new StringReader(data)));
	}

	@Test
	public void testSparqlVersionDirectiveDoubleQuotes() throws IOException {
		String data = "VERSION \"1.2--basic\"";
		try {
			parser.parse(new StringReader(data));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testSparqlVersionDirectiveSingleQuotes() throws IOException {
		String data = "VERSION '1.2--basic'";
		try {
			parser.parse(new StringReader(data));
		} catch (RDFParseException e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	@Test
	public void testSparqlVersionDirectiveMismatchedQuotes() throws IOException {
		String data = "VERSION '1.2--basic\"";
		assertThrows(RDFParseException.class, () -> parser.parse(new StringReader(data)));
	}
}
