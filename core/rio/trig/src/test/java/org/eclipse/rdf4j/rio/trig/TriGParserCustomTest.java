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
package org.eclipse.rdf4j.rio.trig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
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
	public Timeout timeout = new Timeout(10, TimeUnit.MINUTES);

	private ValueFactory vf;

	private ParserConfig settingsNoVerifyLangTag;

	private ParseErrorCollector errors;

	private RDFParser parser;

	private StatementCollector statementCollector;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		vf = SimpleValueFactory.getInstance();
		settingsNoVerifyLangTag = new ParserConfig();
		settingsNoVerifyLangTag.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
		errors = new ParseErrorCollector();
		parser = Rio.createParser(RDFFormat.TRIG);
		statementCollector = new StatementCollector(new LinkedHashModel());
		parser.setRDFHandler(statementCollector);
	}

	@Test
	public void testSPARQLGraphKeyword() throws Exception {
		Model model = Rio.parse(new StringReader("GRAPH <urn:a> { [] <http://www.example.net/test> \"Foo\" }"), "",
				RDFFormat.TRIG);

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:a", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testGraph() throws Exception {
		Model model = Rio.parse(new StringReader("<urn:a> { [] <http://www.example.net/test> \"Foo\" }"), "",
				RDFFormat.TRIG);

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:a", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testGraphLocalNameGraph() throws Exception {
		Model model = Rio.parse(
				new StringReader("@prefix graph: <urn:> .\n graph:a { [] <http://www.example.net/test> \"Foo\" }"), "",
				RDFFormat.TRIG);

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:a", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testGraphLocalNameIntegerGraph() throws Exception {
		Model model = Rio.parse(
				new StringReader("@prefix graph: <urn:> .\n graph:1 { [] <http://www.example.net/test> \"Foo\" }"), "",
				RDFFormat.TRIG);

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:1", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testGraphLocalNameNotGraph() throws Exception {
		Model model = Rio.parse(
				new StringReader("@prefix ex: <urn:> .\n ex:a { [] <http://www.example.net/test> \"Foo\" }"), "",
				RDFFormat.TRIG);

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:a", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testGraphLocalNameIntegerNotGraph() throws Exception {
		Model model = Rio.parse(
				new StringReader("@prefix ex: <urn:> .\n ex:1 { [] <http://www.example.net/test> \"Foo\" }"), "",
				RDFFormat.TRIG);

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:1", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testTrailingSemicolon() throws Exception {
		Rio.parse(new StringReader("{<http://example/s> <http://example/p> <http://example/o> ;}"), "", RDFFormat.TRIG);
	}

	@Test
	public void testAnonymousGraph1() throws Exception {
		Rio.parse(new StringReader("PREFIX : <http://example/>\n GRAPH [] { :s :p :o }"), "", RDFFormat.TRIG);
	}

	@Test
	public void testAnonymousGraph2() throws Exception {
		Rio.parse(new StringReader("PREFIX : <http://example/>\n [] { :s :p :o }"), "", RDFFormat.TRIG);
	}

	@Test
	public void testTurtle() throws Exception {
		Rio.parse(new StringReader("<urn:a> <urn:b> <urn:c>"), "", RDFFormat.TRIG);
	}

	@Test
	public void testMinimalWhitespace() throws Exception {
		Rio.parse(this.getClass().getResourceAsStream("/testcases/trig/trig-syntax-minimal-whitespace-01.trig"), "",
				RDFFormat.TRIG);
	}

	@Test
	public void testMinimalWhitespaceLine12() throws Exception {
		Rio.parse(new StringReader("@prefix : <http://example/c/> . {_:s:p :o ._:s:p\"Alice\". _:s:p _:o .}"), "",
				RDFFormat.TRIG);
	}

	@Test
	public void testBadPname02() throws Exception {
		try {
			Rio.parse(new StringReader("@prefix : <http://example/> . {:a%2 :p :o .}"), "", RDFFormat.TRIG);
			fail("Did not receive expected exception");
		} catch (RDFParseException e) {

		}
	}

	@Test
	public void testSupportedSettings() throws Exception {
		assertThat(Rio.createParser(RDFFormat.TRIG).getSupportedSettings()).hasSize(15);
	}

	@Test
	public void testParseTruePrefix() throws Exception {
		Rio.parse(new StringReader("@prefix true: <http://example/c/> . {true:s true:p true:o .}"), "", RDFFormat.TRIG);
	}

	@Test
	public void testParseTrig_booleanLiteral() throws Exception {
		String trig = "{\n" + "  <http://www.ex.com/s> <http://www.ex.com/b> true.\n" + "}";
		Model m = Rio.parse(new StringReader(trig), "http://ex/", RDFFormat.TRIG);
		assertEquals(1, m.size());
	}

	@Test
	public void testParseTrig_booleanLiteral_space() throws Exception {
		String trig = "{\n" + "  <http://www.ex.com/s> <http://www.ex.com/b> true .\n" + "}";
		Model m = Rio.parse(new StringReader(trig), "http://ex/", RDFFormat.TRIG);
		assertEquals(1, m.size());
	}

	@Test
	public void testParseTrig_intLiteral() throws Exception {
		String trig = "{\n" + "  <http://www.ex.com/s> <http://www.ex.com/b> 1.\n" + "}";
		Model m = Rio.parse(new StringReader(trig), "http://ex/", RDFFormat.TRIG);
		assertEquals(1, Models.objectLiteral(m).get().intValue());
	}

	@Test
	public void testParseTrig_doubleLiteral() throws Exception {
		String trig = "{\n" + "  <http://www.ex.com/s> <http://www.ex.com/b> 1.2.\n" + "}";
		Model m = Rio.parse(new StringReader(trig), "http://ex/", RDFFormat.TRIG);
		assertEquals(1.2d, Models.objectLiteral(m).get().doubleValue(), 0.01);
	}

}
