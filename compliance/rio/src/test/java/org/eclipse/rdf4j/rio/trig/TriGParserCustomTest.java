/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.trig;

import static org.junit.Assert.*;

import java.io.StringReader;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

/**
 * Custom (non-manifest) tests for TriG parser.
 * 
 * @author Peter Ansell
 */
public class TriGParserCustomTest {

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
		vf = SimpleValueFactory.getInstance();
		settingsNoVerifyLangTag = new ParserConfig();
		settingsNoVerifyLangTag.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
		errors = new ParseErrorCollector();
		parser = Rio.createParser(RDFFormat.TRIG);
		statementCollector = new StatementCollector(new LinkedHashModel());
		parser.setRDFHandler(statementCollector);
	}

	@Test
	public void testSPARQLGraphKeyword()
		throws Exception
	{
		Rio.parse(new StringReader("GRAPH <urn:a> { [] <http://www.example.net/test> \"Foo\" }"), "",
				RDFFormat.TRIG);
	}

	@Test
	public void testTrailingSemicolon()
		throws Exception
	{
		Rio.parse(new StringReader("{<http://example/s> <http://example/p> <http://example/o> ;}"), "",
				RDFFormat.TRIG);
	}

	@Test
	public void testAnonymousGraph1()
		throws Exception
	{
		Rio.parse(new StringReader("PREFIX : <http://example/>\n GRAPH [] { :s :p :o }"), "", RDFFormat.TRIG);
	}

	@Test
	public void testAnonymousGraph2()
		throws Exception
	{
		Rio.parse(new StringReader("PREFIX : <http://example/>\n [] { :s :p :o }"), "", RDFFormat.TRIG);
	}

	@Test
	public void testTurtle()
		throws Exception
	{
		Rio.parse(new StringReader("<urn:a> <urn:b> <urn:c>"), "", RDFFormat.TRIG);
	}

	@Test
	public void testMinimalWhitespace()
		throws Exception
	{
		Rio.parse(
				this.getClass().getResourceAsStream("/testcases/trig/trig-syntax-minimal-whitespace-01.trig"),
				"", RDFFormat.TRIG);
	}

	@Test
	public void testMinimalWhitespaceLine12()
		throws Exception
	{
		Rio.parse(new StringReader("@prefix : <http://example/c/> . {_:s:p :o ._:s:p\"Alice\". _:s:p _:o .}"),
				"", RDFFormat.TRIG);
	}

	@Test
	public void testBadPname02()
		throws Exception
	{
		try {
			Rio.parse(new StringReader("@prefix : <http://example/> . {:a%2 :p :o .}"), "", RDFFormat.TRIG);
			fail("Did not receive expected exception");
		}
		catch (RDFParseException e) {

		}
	}

	@Test
	public void testSupportedSettings()
		throws Exception
	{
		assertEquals(12, Rio.createParser(RDFFormat.TRIG).getSupportedSettings().size());
	}

}
