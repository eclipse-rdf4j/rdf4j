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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.AbstractParserTest;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Custom (non-manifest) tests for TriG parser.
 *
 * @author Peter Ansell
 */
@Timeout(value = 10, unit = TimeUnit.MINUTES)
public class TriGParserCustomTest extends AbstractParserTest {

	private Model model;

	@BeforeEach
	public void setUp() {
		model = new LinkedHashModel();
		statementCollector = new StatementCollector(model);
		super.setUp();
	}

	@Test
	public void testSPARQLGraphKeyword() throws Exception {
		String data = "GRAPH <urn:a> { [] <http://www.example.net/test> \"Foo\" }";
		parser.parse(new StringReader(data));

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:a", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testGraph() throws Exception {
		String data = "<urn:a> { [] <http://www.example.net/test> \"Foo\" }";
		parser.parse(new StringReader(data));

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:a", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testGraphLocalNameGraph() throws Exception {
		String data = "@prefix graph: <urn:> .\n graph:a { [] <http://www.example.net/test> \"Foo\" }";
		parser.parse(new StringReader(data));

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:a", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testGraphLocalNameIntegerGraph() throws Exception {
		String data = "@prefix graph: <urn:> .\n graph:1 { [] <http://www.example.net/test> \"Foo\" }";
		parser.parse(new StringReader(data));

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:1", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testGraphLocalNameNotGraph() throws Exception {
		String data = "@prefix ex: <urn:> .\n ex:a { [] <http://www.example.net/test> \"Foo\" }";
		parser.parse(new StringReader(data));

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:a", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testGraphLocalNameIntegerNotGraph() throws Exception {
		String data = "@prefix ex: <urn:> .\n ex:1 { [] <http://www.example.net/test> \"Foo\" }";
		parser.parse(new StringReader(data));

		assertEquals(1, model.size());
		assertNotNull(model.contexts().iterator().next());
		assertEquals("urn:1", model.contexts().iterator().next().stringValue());
		assertTrue(model.subjects().iterator().next() instanceof BNode);
		assertEquals("http://www.example.net/test", model.predicates().iterator().next().stringValue());
		assertEquals("Foo", model.objects().iterator().next().stringValue());
	}

	@Test
	public void testTrailingSemicolon() throws Exception {
		parser.parse(new StringReader("{<http://example/s> <http://example/p> <http://example/o> ;}"), "");
	}

	@Test
	public void testAnonymousGraph1() throws Exception {
		parser.parse(new StringReader("PREFIX : <http://example/>\n GRAPH [] { :s :p :o }"), "");
	}

	@Test
	public void testAnonymousGraph2() throws Exception {
		parser.parse(new StringReader("PREFIX : <http://example/>\n [] { :s :p :o }"), "");
	}

	@Test
	public void testTurtle() throws Exception {
		parser.parse(new StringReader("<urn:a> <urn:b> <urn:c>"), "");
	}

	@Test
	public void testMinimalWhitespace() throws Exception {
		parser.parse(this.getClass().getResourceAsStream("/testcases/trig/trig-syntax-minimal-whitespace-01.trig"), "");
	}

	@Test
	public void testMinimalWhitespaceLine12() throws Exception {
		parser.parse(new StringReader("@prefix : <http://example/c/> . {_:s:p :o ._:s:p\"Alice\". _:s:p _:o .}"), "");
	}

	@Test
	public void testBadPname02() throws Exception {
		try {
			parser.parse(new StringReader("@prefix : <http://example/> . {:a%2 :p :o .}"), "");
			fail("Did not receive expected exception");
		} catch (RDFParseException e) {

		}
	}

	@Test
	public void testSupportedSettings() {
		assertThat(Rio.createParser(RDFFormat.TRIG).getSupportedSettings()).hasSize(15);
	}

	@Test
	public void testParseTruePrefix() throws Exception {
		parser.parse(new StringReader("@prefix true: <http://example/c/> . {true:s true:p true:o .}"), "");
	}

	@Test
	public void testParseTrig_booleanLiteral() throws Exception {
		String trig = "{\n" + "  <http://www.ex.com/s> <http://www.ex.com/b> true.\n" + "}";
		parser.parse(new StringReader(trig), "http://ex/");
		assertEquals(1, model.size());
	}

	@Test
	public void testParseTrig_booleanLiteral_space() throws Exception {
		String trig = "{\n" + "  <http://www.ex.com/s> <http://www.ex.com/b> true .\n" + "}";
		parser.parse(new StringReader(trig), "http://ex/");
		assertEquals(1, model.size());
	}

	@Test
	public void testParseTrig_intLiteral() throws Exception {
		String trig = "{\n" + "  <http://www.ex.com/s> <http://www.ex.com/b> 1.\n" + "}";
		parser.parse(new StringReader(trig), "http://ex/");
		assertEquals(1, Models.objectLiteral(model).get().intValue());
	}

	@Test
	public void testParseTrig_doubleLiteral() throws Exception {
		String trig = "{\n" + "  <http://www.ex.com/s> <http://www.ex.com/b> 1.2.\n" + "}";
		parser.parse(new StringReader(trig), "http://ex/");
		assertEquals(1.2d, Models.objectLiteral(model).get().doubleValue(), 0.01);
	}

	@Test
	public void testDirLangStringRTLNoContext() {
		String data = "<http://example/a> <http://example/b> \"שלום\"@he--rtl";
		dirLangStringTest(data, false, "he", Literal.RTL_SUFFIX, false, false);
	}

	@Test
	public void testDirLangStringRTLWithContext() {
		String data = "<http://example/a> <http://example/b> \"שלום\"@he--rtl";
		dirLangStringTest(data, true, "he", Literal.RTL_SUFFIX, false, false);
	}

	@Test
	public void testDirLangStringLTRWithNormalizationNoContext() {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--ltr";
		dirLangStringTest(data, false, "en", Literal.LTR_SUFFIX, true, false);
	}

	@Test
	public void testDirLangStringLTRWithNormalizationWithContext() {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--ltr";
		dirLangStringTest(data, true, "en", Literal.LTR_SUFFIX, true, false);
	}

	@Test
	public void testBadDirLangStringNoContext() {
		String data = "<http://example/a> <http://example/b> \"hello\"@en--unk";
		dirLangStringTest(data, false, "", "", true, true);
	}

	@Test
	public void testBadDirLangStringWithContext() {
		String data = "<http://example/a> <http://example/b> \"hello\"@en--unk";
		dirLangStringTest(data, true, "", "", true, true);
	}

	@Test
	public void testBadCapitalizationDirLangStringNoContext() {
		String data = "<http://example/a> <http://example/b> \"Hello\"@en--LTR";
		dirLangStringTest(data, false, "", "", true, true);
	}

	@Test
	public void testBadCapitalizationDirLangStringWithContext() {
		final String data = "<http://example/a> <http://example/b> \"Hello\"@en--LTR";
		dirLangStringTest(data, true, "", "", true, true);
	}

	@Test
	public void testDirLangStringNoLanguage() throws IOException {
		final String data = "<http://example/a> <http://example/b> \"Hello\"^^<http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString> .";
		dirLangStringNoLanguageTestHelper(data);
	}

	private void dirLangStringTest(
			final String triple, final boolean withContext, final String expectedLang, final String expectedBaseDir,
			final boolean normalize,
			final boolean shouldCauseException) {
		final String data = (withContext ? "<http://www.example.org/> { " : "") + triple + " ."
				+ (withContext ? " }" : "");

		dirLangStringTestHelper(data, expectedLang, expectedBaseDir, normalize, shouldCauseException);
	}

	@Override
	public RDFParser createRDFParser() {
		return new TriGParser();
	}
}
