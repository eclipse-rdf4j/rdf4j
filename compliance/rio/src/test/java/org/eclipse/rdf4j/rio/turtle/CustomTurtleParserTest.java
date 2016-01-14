/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.NamespaceImpl;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
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

/**
 * Custom tests for Turtle Parser
 * 
 * @author Peter Ansell
 */
public class CustomTurtleParserTest {

	@Rule
	public Timeout timeout = new Timeout(1000000);

	private ValueFactory vf;

	private ParserConfig settingsNoVerifyLangTag;

	private ParseErrorCollector errors;

	private RDFParser parser;

	private StatementCollector statementCollector;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		vf = ValueFactoryImpl.getInstance();
		settingsNoVerifyLangTag = new ParserConfig();
		settingsNoVerifyLangTag.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
		errors = new ParseErrorCollector();
		parser = Rio.createParser(RDFFormat.TURTLE);
		statementCollector = new StatementCollector(new LinkedHashModel());
		parser.setRDFHandler(statementCollector);
	}

	@Test
	public void testSES1887NoLangTagFailure()
		throws Exception
	{
		try {
			Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> \"Foo\"@."), "", RDFFormat.TURTLE);
			fail("Did not receive an exception");
		}
		catch (RDFParseException e) {
			assertTrue(e.getMessage().contains("Expected a letter, found '.'"));
		}
	}

	@Test
	public void testSES1887NoLangTagFailure2()
		throws Exception
	{
		try {
			// NOTE: Bad things may happen when VERIFY_LANGUAGE_TAGS is turned off
			// on a file of this structure
			Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> \"Foo\"@."), "", RDFFormat.TURTLE,
					settingsNoVerifyLangTag, vf, errors);
			fail("Did not receive an exception");
		}
		catch (RDFParseException e) {
			assertTrue(e.getMessage().contains("Unexpected end of file"));
		}
	}

	@Test
	public void testSES1887Whitespace()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> \"Foo\"@fr-FR ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
	}

	@Test
	public void testSES1887Period()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> \"Foo\"@fr-FR."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
	}

	@Test
	public void testSES1887Semicolon()
		throws Exception
	{
		Model model = Rio.parse(new StringReader(
				"<urn:a> <http://www.example.net/test> \"Foo\"@fr-FR;<http://other.example.org>\"Blah\"@en-AU."),
				"", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
		assertTrue(model.contains(null, null, vf.createLiteral("Blah", "en-AU")));
	}

	@Test
	public void testSES1887Comma()
		throws Exception
	{
		Model model = Rio.parse(new StringReader(
				"<urn:a> <http://www.example.net/test> \"Foo\"@fr-FR,\"Blah\"@en-AU."), "", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
		assertTrue(model.contains(null, null, vf.createLiteral("Blah", "en-AU")));
	}

	@Test
	public void testSES1887CloseParentheses()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <http://www.example.net/test> (\"Foo\"@fr-FR)."), "",
				RDFFormat.TURTLE);

		assertEquals(3, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
	}

	@Test
	public void testSES1887CloseSquareBracket()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("[<http://www.example.net/test> \"Foo\"@fr-FR]."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(null, null, vf.createLiteral("Foo", "fr-FR")));
	}

	@Test
	public void testLiteralWithNewlines()
		throws Exception
	{
		String namespace = "http://www.foo.com/bar#";
		String okLiteralString = "Literal \n without \n new line at the beginning. \n ";
		String errLiteralString = "\n Literal \n with \n new line at the beginning. \n ";

		URI mySubject = vf.createURI(namespace, "Subject");
		URI myPredicate = vf.createURI(namespace, "Predicate");
		Literal myOkObject = vf.createLiteral(okLiteralString);
		Literal myErrObject = vf.createLiteral(errLiteralString);

		StringWriter out = new StringWriter();
		Model model = new LinkedHashModel();
		model.add(mySubject, myPredicate, myOkObject);
		model.add(mySubject, myPredicate, myErrObject);
		Rio.write(model, out, RDFFormat.TURTLE);

		String str = out.toString();

		System.err.println(str);

		assertTrue("okLiteralString not found", str.contains(okLiteralString));
		assertTrue("errLiteralString not found", str.contains(errLiteralString));
	}

	@Test
	public void testSupportedSettings()
		throws Exception
	{
		assertEquals(12, parser.getSupportedSettings().size());
	}

	@Test
	public void testSES1988BlankNodePeriodEOF()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank."), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodSpace()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank. "), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodTab()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank.\t"), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodNewLine()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank.\n"), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodCarriageReturn()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank.\r"), "", RDFFormat.TURTLE);

		assertEquals(1, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodURI()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank.<urn:c> <urn:d> <urn:e>."), "",
				RDFFormat.TURTLE);

		assertEquals(2, model.size());
	}

	@Test
	public void testSES1988BlankNodePeriodBNode()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> _:blank._:blank <urn:d> <urn:e>."), "",
				RDFFormat.TURTLE);

		assertEquals(2, model.size());
	}

	@Test
	public void testSES2013BlankNodeSemiColonBNodeSpaceA()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> a _:c2; a <urn:b> ."), "", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), RDF.TYPE, vf.createURI("urn:b")));
	}

	@Test
	public void testSES2013BlankNodeSemiColonBNodeA()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> a _:c2;a <urn:b> ."), "", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), RDF.TYPE, vf.createURI("urn:b")));
	}

	@Test
	public void testSES2013BlankNodeSemiColonBNodeSpaceURI()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> a _:c2; <urn:b> <urn:c> ."), "", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), vf.createURI("urn:b"), vf.createURI("urn:c")));
	}

	@Test
	public void testSES2013BlankNodeSemiColonBNodeURI()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> a _:c2;<urn:b> <urn:c> ."), "", RDFFormat.TURTLE);

		assertEquals(2, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), vf.createURI("urn:b"), vf.createURI("urn:c")));
	}

	@Test
	public void testSES2019ParseLongLiterals()
		throws Exception
	{
		parser.parse(this.getClass().getResourceAsStream("/testcases/turtle/turtle-long-literals-test.ttl"), "");

		assertTrue(errors.getWarnings().isEmpty());
		assertTrue(errors.getErrors().isEmpty());
		assertTrue(errors.getFatalErrors().isEmpty());

		assertFalse(statementCollector.getStatements().isEmpty());
		assertEquals(5, statementCollector.getStatements().size());

		Models.isomorphic(statementCollector.getStatements(), Rio.parse(
				this.getClass().getResourceAsStream("/testcases/turtle/turtle-long-literals-test.nt"), "",
				RDFFormat.NTRIPLES));
	}

	@Test
	public void testSES2086PeriodEndingLocalNamesFailure1()
		throws Exception
	{
		try {
			Rio.parse(new StringReader(
					"@prefix : <http://example.org> .\n <urn:a> <http://www.example.net/test> :test. ."), "",
					RDFFormat.TURTLE);
			fail("Did not receive an exception");
		}
		catch (RDFParseException e) {
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("Object for statement missing"));
		}
	}

	@Test
	public void testSES2086PeriodEndingLocalNamesFailure2()
		throws Exception
	{
		try {
			Rio.parse(
					new StringReader(
							"@prefix ns: <http://example.org/data/> . ns:uriWithDot. a ns:Product ; ns:title \"An example subject ending with a dot.\" . "),
					"", RDFFormat.TURTLE);
			fail("Did not receive an exception");
		}
		catch (RDFParseException e) {
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains(
					"Illegal predicate value: \"\"^^<http://www.w3.org/2001/XMLSchema#integer>"));
		}
	}

	@Test
	public void testSES2086PeriodEndingLocalNamesFailure3()
		throws Exception
	{
		try {
			Rio.parse(
					new StringReader(
							"@prefix ns: <http://example.org/data/> . ns:1 a ns:Product ; ns:affects ns:4 , ns:16 , ns:uriWithDot. ; ns:title \"An example entity with uriWithDot as an object\" . "),
					"", RDFFormat.TURTLE);
			fail("Did not receive an exception");
		}
		catch (RDFParseException e) {
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("Expected an RDF value here, found ';'"));
		}
	}

	@Test
	public void testSES2086PeriodEndingLocalNamesFailure4()
		throws Exception
	{
		try {
			Rio.parse(
					new StringReader(
							"@prefix ns: <http://example.org/data/> . ns:1 a ns:uriWithDot. ; ns:title \"An example entity with uriWithDot as an object\" . "),
					"", RDFFormat.TURTLE);
			fail("Did not receive an exception");
		}
		catch (RDFParseException e) {
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("Expected an RDF value here, found ';'"));
		}
	}

	@Test
	public void testSES2165LiteralSpaceDatatypeNewline()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> \"testliteral\"^^\n<urn:datatype> ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), vf.createURI("urn:b"),
				vf.createLiteral("testliteral", vf.createURI("urn:datatype"))));
	}

	@Test
	public void testSES2165LiteralSpaceDatatypeTab()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> \"testliteral\"^^\t<urn:datatype> ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), vf.createURI("urn:b"),
				vf.createLiteral("testliteral", vf.createURI("urn:datatype"))));
	}

	@Test
	public void testSES2165LiteralSpaceDatatypeCarriageReturn()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> \"testliteral\"^^\r<urn:datatype> ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), vf.createURI("urn:b"),
				vf.createLiteral("testliteral", vf.createURI("urn:datatype"))));
	}

	@Test
	public void testSES2165LiteralSpaceDatatypeSpace()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> \"testliteral\"^^ <urn:datatype> ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), vf.createURI("urn:b"),
				vf.createLiteral("testliteral", vf.createURI("urn:datatype"))));
	}
	
	@Test
	public void testSES2165LiteralSpaceDatatypeComment()
		throws Exception
	{
		Model model = Rio.parse(new StringReader("<urn:a> <urn:b> \"testliteral\"^^#comment\n<urn:datatype> ."), "",
				RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), vf.createURI("urn:b"),
				vf.createLiteral("testliteral", vf.createURI("urn:datatype"))));
	}

	@Test
	public void testParsingDefaultNamespaces() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> skos:broader <urn:b>."), "",
		                        RDFFormat.TURTLE);

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), SKOS.BROADER, vf.createURI("urn:b")));
	}

	@Test
	public void testParsingNamespacesWithOption() throws Exception {
		ParserConfig aConfig = new ParserConfig();

		aConfig.set(BasicParserSettings.NAMESPACES, Collections.<Namespace>singleton(new NamespaceImpl("foo", SKOS.NAMESPACE)));

		Model model = Rio.parse(new StringReader("<urn:a> foo:broader <urn:b>."), "", RDFFormat.TURTLE, aConfig, vf, new ParseErrorLogger());

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), SKOS.BROADER, vf.createURI("urn:b")));
	}

	@Test
	public void testParsingNamespacesWithOverride() throws Exception {
		ParserConfig aConfig = new ParserConfig();

		aConfig.set(BasicParserSettings.NAMESPACES, Collections.<Namespace>singleton(new NamespaceImpl("foo", SKOS.NAMESPACE)));

		Model model = Rio.parse(new StringReader("@prefix skos : <urn:not_skos:> ." +
		                                         "<urn:a> skos:broader <urn:b>."), "",
		                        RDFFormat.TURTLE, aConfig, vf, new ParseErrorLogger());

		assertEquals(1, model.size());
		assertTrue(model.contains(vf.createURI("urn:a"), vf.createURI("urn:not_skos:broader"), vf.createURI("urn:b")));
	}
}
