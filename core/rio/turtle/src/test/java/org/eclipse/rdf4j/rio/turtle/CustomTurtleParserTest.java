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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.ParseErrorLogger;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom tests for Turtle Parser
 *
 * @author Peter Ansell
 */
public class CustomTurtleParserTest {

	@Rule
	public Timeout timeout = Timeout.millis(1000000);

	private ValueFactory vf;

	private ParserConfig settingsNoVerifyLangTag;

	private ParseErrorCollector errors;

	private RDFParser parser;

	private StatementCollector statementCollector;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		vf = SimpleValueFactory.getInstance();
		settingsNoVerifyLangTag = new ParserConfig();
		settingsNoVerifyLangTag.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
		errors = new ParseErrorCollector();
		parser = Rio.createParser(RDFFormat.TURTLE);
		statementCollector = new StatementCollector(new LinkedHashModel());
		parser.setRDFHandler(statementCollector);
	}

	@Test
	public void testSES1887NoLangTagFailure() throws Exception {
		try {
			Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> \"Foo\"@."), "", RDFFormat.TURTLE);
			fail("Did not receive an exception");
		} catch (RDFParseException e) {
			assertTrue(e.getMessage().contains("Expected a letter, found '.'"));
		}
	}

	@Test
	public void testSES1887NoLangTagFailure2() throws Exception {
		try {
			// NOTE: Bad things may happen when VERIFY_LANGUAGE_TAGS is turned off
			// on a file of this structure
			Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> \"Foo\"@."), "", RDFFormat.TURTLE,
					settingsNoVerifyLangTag, vf, errors);
			fail("Did not receive an exception");
		} catch (RDFParseException e) {
			assertTrue(e.getMessage().contains("Unexpected end of file"));
		}
	}

	@Test
	public void testSES1887Whitespace() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> \"Foo\"@fr-FR ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
	}

	@Test
	public void testSES1887Period() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> \"Foo\"@fr-FR."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
	}

	@Test
	public void testSES1887Semicolon() throws Exception {
		Model model = Rio.parse(new StringReader(
				"<urn:a> <http://www.example.net/test> \"Foo\"@fr-FR;<http://other.example.org>\"Blah\"@en-AU."), "",
				RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
		assertTrue(model.contains(null, null, vf.createLiteral("Blah", "en-AU")));
	}

	@Test
	public void testSES1887Comma() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> \"Foo\"@fr-FR,\"Blah\"@en-AU."),
				"", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
		assertTrue(model.contains(null, null, vf.createLiteral("Blah", "en-AU")));
	}

	@Test
	public void testSES1887CloseParentheses() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> (\"Foo\"@fr-FR)."), "",
				RDFFormat.TURTLE);

		assertEquals(3, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
	}

	@Test
	public void testSES1887CloseSquareBracket() throws Exception {
		Model model = Rio.parse(new StringReader("[<http://www.example.net/test> \"Foo\"@fr-FR]."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
	}

	@Test
	public void testLiteralWithNewlines() throws Exception {
		String namespace = "http://www.foo.com/bar#";
		String okLiteralString = "Literal \n without \n new line at the beginning. \n ";
		String errLiteralString = "\n Literal \n with \n new line at the beginning. \n ";

		IRI mySubject = vf.createIRI(namespace, "Subject");
		IRI myPredicate = vf.createIRI(namespace, "Predicate");
		Literal myOkObject = vf.createLiteral(okLiteralString);
		Literal myErrObject = vf.createLiteral(errLiteralString);

		StringWriter out = new StringWriter();
		Model model = new LinkedHashModel();
		model.add(mySubject, myPredicate, myOkObject);
		model.add(mySubject, myPredicate, myErrObject);
		Rio.write(model, out, RDFFormat.TURTLE);

		String str = out.toString();

		assertTrue("okLiteralString not found", str.contains(okLiteralString));
		assertTrue("errLiteralString not found", str.contains(errLiteralString));
	}

	@Test
	public void testSupportedSettings() throws Exception {
		assertThat(parser.getSupportedSettings()).hasSize(15);
	}

	@Test
	public void testSES1988BlankNodePeriodEOF() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank."), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodSpace() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank. "), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodTab() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank.\t"), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodNewLine() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank.\n"), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodCarriageReturn() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank.\r"), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodURI() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank.<urn:c> <urn:d> <urn:e>."), "",
				RDFFormat.TURTLE);

		assertEquals(2, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodBNode() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank._:blank <urn:d> <urn:e>."), "",
				RDFFormat.TURTLE);

		assertEquals(2, model.size());
	}

	@Test
	public void testSES2013BlankNodeSemiColonBNodeSpaceA() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> a _:c2; a <urn:b> ."), "", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), RDF.TYPE, vf.createIRI("urn:b")));
	}

	@Test
	public void testSES2013BlankNodeSemiColonBNodeA() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> a _:c2;a <urn:b> ."), "", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), RDF.TYPE, vf.createIRI("urn:b")));
	}

	@Test
	public void testSES2013BlankNodeSemiColonBNodeSpaceURI() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> a _:c2; <urn:b> <urn:c> ."), "", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), vf.createIRI("urn:b"), vf.createIRI("urn:c")));
	}

	@Test
	public void testSES2013BlankNodeSemiColonBNodeURI() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> a _:c2;<urn:b> <urn:c> ."), "", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), vf.createIRI("urn:b"), vf.createIRI("urn:c")));
	}

	@Test
	public void testSES2019ParseLongLiterals() throws Exception {
		parser.parse(this.getClass().getResourceAsStream("/testcases/turtle/turtle-long-literals-test.ttl"), "");

		assertTrue(errors.getWarnings().isEmpty());
		assertTrue(errors.getErrors().isEmpty());
		assertTrue(errors.getFatalErrors().isEmpty());

		assertFalse(statementCollector.getStatements().isEmpty());
		assertEquals(5, statementCollector.getStatements().size());

		Models.isomorphic(statementCollector.getStatements(),
				Rio.parse(this.getClass().getResourceAsStream("/testcases/turtle/turtle-long-literals-test.nt"), "",
						RDFFormat.NTRIPLES));
	}

	@Test
	public void testSES2086PeriodEndingLocalNamesFailure1() throws Exception {
		try {
			Rio.parse(
					new StringReader(
							"@prefix : <http://example.org> .\n <urn:a> <http://www.example.net/test> :test. ."),
					"", RDFFormat.TURTLE);
			fail("Did not receive an exception");
		} catch (RDFParseException e) {
			logger.debug(e.getMessage(), e);
			assertTrue(e.getMessage().contains("Object for statement missing"));
		}
	}

	@Test
	public void testSES2086PeriodEndingLocalNamesFailure2() throws Exception {
		try {
			Rio.parse(new StringReader(
					"@prefix ns: <http://example.org/data/> . ns:uriWithDot. a ns:Product ; ns:title \"An example subject ending with a dot.\" . "),
					"", RDFFormat.TURTLE);
			fail("Did not receive an exception");
		} catch (RDFParseException e) {
			logger.debug(e.getMessage(), e);
			assertTrue(e.getMessage()
					.contains("Illegal predicate value: \"\"^^<http://www.w3.org/2001/XMLSchema#integer>"));
		}
	}

	@Test
	public void testSES2086PeriodEndingLocalNamesFailure3() throws Exception {
		try {
			Rio.parse(new StringReader(
					"@prefix ns: <http://example.org/data/> . ns:1 a ns:Product ; ns:affects ns:4 , ns:16 , ns:uriWithDot. ; ns:title \"An example entity with uriWithDot as an object\" . "),
					"", RDFFormat.TURTLE);
			fail("Did not receive an exception");
		} catch (RDFParseException e) {
			logger.debug(e.getMessage(), e);
			assertTrue(e.getMessage().contains("Expected an RDF value here, found ';'"));
		}
	}

	@Test
	public void testSES2086PeriodEndingLocalNamesFailure4() throws Exception {
		try {
			Rio.parse(new StringReader(
					"@prefix ns: <http://example.org/data/> . ns:1 a ns:uriWithDot. ; ns:title \"An example entity with uriWithDot as an object\" . "),
					"", RDFFormat.TURTLE);
			fail("Did not receive an exception");
		} catch (RDFParseException e) {
			logger.debug(e.getMessage(), e);
			assertTrue(e.getMessage().contains("Expected an RDF value here, found ';'"));
		}
	}

	@Test
	public void testSES2165LiteralSpaceDatatypeNewline() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> \"testliteral\"^^\n<urn:datatype> ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), vf.createIRI("urn:b"),
				vf.createLiteral("testliteral", vf.createIRI("urn:datatype"))));
	}

	@Test
	public void testSES2165LiteralSpaceDatatypeTab() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> \"testliteral\"^^\t<urn:datatype> ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), vf.createIRI("urn:b"),
				vf.createLiteral("testliteral", vf.createIRI("urn:datatype"))));
	}

	@Test
	public void testSES2165LiteralSpaceDatatypeCarriageReturn() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> \"testliteral\"^^\r<urn:datatype> ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), vf.createIRI("urn:b"),
				vf.createLiteral("testliteral", vf.createIRI("urn:datatype"))));
	}

	@Test
	public void testSES2165LiteralSpaceDatatypeSpace() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> \"testliteral\"^^ <urn:datatype> ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), vf.createIRI("urn:b"),
				vf.createLiteral("testliteral", vf.createIRI("urn:datatype"))));
	}

	@Test
	public void testSES2165LiteralSpaceDatatypeComment() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> \"testliteral\"^^#comment\n<urn:datatype> ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), vf.createIRI("urn:b"),
				vf.createLiteral("testliteral", vf.createIRI("urn:datatype"))));
	}

	@Test
	public void testParsingDefaultNamespaces() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> skos:broader <urn:b>."), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), SKOS.BROADER, vf.createIRI("urn:b")));
	}

	@Test
	public void testParsingNamespacesWithOption() throws Exception {
		ParserConfig aConfig = new ParserConfig();

		aConfig.set(BasicParserSettings.NAMESPACES,
				Collections.<Namespace>singleton(new SimpleNamespace("foo", SKOS.NAMESPACE)));

		Model model = Rio.parse(new StringReader("<urn:a> foo:broader <urn:b>."), "", RDFFormat.TURTLE, aConfig, vf,
				new ParseErrorLogger());

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), SKOS.BROADER, vf.createIRI("urn:b")));
	}

	@Test
	public void testParsingNamespacesWithOverride() throws Exception {
		ParserConfig aConfig = new ParserConfig();

		aConfig.set(BasicParserSettings.NAMESPACES,
				Collections.<Namespace>singleton(new SimpleNamespace("foo", SKOS.NAMESPACE)));

		Model model = Rio.parse(new StringReader("@prefix skos : <urn:not_skos:> ." + "<urn:a> skos:broader <urn:b>."),
				"", RDFFormat.TURTLE, aConfig, vf, new ParseErrorLogger());

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createIRI("urn:a"), vf.createIRI("urn:not_skos:broader"), vf.createIRI("urn:b")));
	}

	@Test
	public void test780IRISpace() throws Exception {
		String ttl = "_:b25978837	a <http://purl.bioontology.org/ontology/UATC/\\u0020SERINE\\u0020\\u0020> .";
		try {
			Rio.parse(new StringReader(ttl), "", RDFFormat.TURTLE);
			fail();
		} catch (RDFParseException e) {
			// Invalid IRI
		}
		Model model = Rio.parse(new StringReader(ttl), "", RDFFormat.TURTLE,
				new ParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false), SimpleValueFactory.getInstance(),
				new ParseErrorLogger());
		assertEquals(1, model.size());
		model.filter(null, RDF.TYPE, null)
				.objects()
				.forEach(obj -> assertEquals("http://purl.bioontology.org/ontology/UATC/ SERINE  ", obj.stringValue()));
	}

	@Test
	public void testParseTruePrefix() throws Exception {
		Rio.parse(new StringReader("@prefix true: <http://example/c/> . true:s true:p true:o ."), "", RDFFormat.TURTLE);
	}

	@Test
	public void testParseBooleanLiteral() throws Exception {
		String ttl = "<http://www.ex.com/s> <http://www.ex.com/b> true.\n";
		Model m = Rio.parse(new StringReader(ttl), "http://ex/", RDFFormat.TURTLE);
		assertEquals(1, m.size());
	}

	@Test
	public void testParseBooleanLiteral_space() throws Exception {
		String ttl = "<http://www.ex.com/s> <http://www.ex.com/b> true .\n";
		Model m = Rio.parse(new StringReader(ttl), "http://ex/", RDFFormat.TURTLE);
		assertEquals(1, m.size());
	}

	@Test
	public void testParseIntLiteral() throws Exception {
		String ttl = "<http://www.ex.com/s> <http://www.ex.com/b> 1.\n";
		Model m = Rio.parse(new StringReader(ttl), "http://ex/", RDFFormat.TURTLE);
		assertEquals(1, Models.objectLiteral(m).get().intValue());
	}

	@Test
	public void testParseDoubleLiteral() throws Exception {
		String ttl = "<http://www.ex.com/s> <http://www.ex.com/b> 1.2.\n";
		Model m = Rio.parse(new StringReader(ttl), "http://ex/", RDFFormat.TURTLE);
		assertEquals(1.2d, Models.objectLiteral(m).get().doubleValue(), 0.01);
	}
}
