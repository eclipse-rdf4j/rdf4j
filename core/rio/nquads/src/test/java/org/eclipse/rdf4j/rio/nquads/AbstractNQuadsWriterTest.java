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
package org.eclipse.rdf4j.rio.nquads;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractNQuadsWriterTest extends RDFWriterTest {

	private RDFParser parser;

	private RDFWriter writer;

	private ValueFactory vf;

	protected AbstractNQuadsWriterTest(RDFWriterFactory writerF, RDFParserFactory parserF) {
		super(writerF, parserF);
	}

	@Before
	public void setUp() throws Exception {
		parser = rdfParserFactory.getParser();
		vf = SimpleValueFactory.getInstance();
	}

	@After
	public void tearDown() {
		parser = null;
		writer = null;
	}

	@Test
	public void testReadWrite() throws RDFHandlerException, IOException, RDFParseException {
		StatementCollector statementCollector = new StatementCollector();
		parser.setRDFHandler(statementCollector);
		parser.parse(this.getClass().getResourceAsStream("/testcases/nquads/test2.nq"), "http://test.base.uri");

		Assert.assertEquals(400, statementCollector.getStatements().size());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = rdfWriterFactory.getWriter(baos);
		writer.startRDF();
		for (Statement nextStatement : statementCollector.getStatements()) {
			writer.handleStatement(nextStatement);
		}
		writer.endRDF();

		Assert.assertEquals("Unexpected number of lines.", 400, baos.toString().split("\n").length);
	}

	@Test
	public void testNoContext() throws RDFHandlerException {
		Statement s1 = vf.createStatement(vf.createIRI("http://test.example.org/test/subject/1"),
				vf.createIRI("http://other.example.com/test/predicate/1"), vf.createLiteral("test literal"));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = rdfWriterFactory.getWriter(baos);
		writer.startRDF();
		writer.handleStatement(s1);
		writer.endRDF();

		String content = baos.toString();
		String[] lines = content.split("\n");
		Assert.assertEquals("Unexpected number of lines.", 1, lines.length);
		Assert.assertEquals(
				"<http://test.example.org/test/subject/1> <http://other.example.com/test/predicate/1> \"test literal\" .",
				lines[0]);
	}

	@Test
	public void testNoContextAddXSDString() throws RDFHandlerException {
		Statement s1 = vf.createStatement(vf.createIRI("http://test.example.org/test/subject/1"),
				vf.createIRI("http://other.example.com/test/predicate/1"), vf.createLiteral("test literal"));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = rdfWriterFactory.getWriter(baos);
		writer.getWriterConfig().set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, false);
		writer.startRDF();
		writer.handleStatement(s1);
		writer.endRDF();

		String content = baos.toString();
		String[] lines = content.split("\n");
		Assert.assertEquals("Unexpected number of lines.", 1, lines.length);
		Assert.assertEquals(
				"<http://test.example.org/test/subject/1> <http://other.example.com/test/predicate/1> \"test literal\"^^<http://www.w3.org/2001/XMLSchema#string> .",
				lines[0]);
	}

	@Test
	public void testBlankNodeContext() throws RDFHandlerException {
		Statement s1 = vf.createStatement(vf.createIRI("http://test.example.org/test/subject/1"),
				vf.createIRI("http://other.example.com/test/predicate/1"), vf.createLiteral("test literal"),
				vf.createBNode());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = rdfWriterFactory.getWriter(baos);
		writer.startRDF();
		writer.handleStatement(s1);
		writer.endRDF();

		String content = baos.toString();
		String[] lines = content.split("\n");
		Assert.assertEquals("Unexpected number of lines.", 1, lines.length);
		Assert.assertTrue(lines[0].startsWith(
				"<http://test.example.org/test/subject/1> <http://other.example.com/test/predicate/1> \"test literal\" _:"));
	}

	@Test
	public void testBlankNodeContextAddXSDString() throws RDFHandlerException {
		Statement s1 = vf.createStatement(vf.createIRI("http://test.example.org/test/subject/1"),
				vf.createIRI("http://other.example.com/test/predicate/1"), vf.createLiteral("test literal"),
				vf.createBNode());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = rdfWriterFactory.getWriter(baos);
		writer.getWriterConfig().set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, false);
		writer.startRDF();
		writer.handleStatement(s1);
		writer.endRDF();

		String content = baos.toString();
		String[] lines = content.split("\n");
		Assert.assertEquals("Unexpected number of lines.", 1, lines.length);
		Assert.assertTrue(lines[0].startsWith(
				"<http://test.example.org/test/subject/1> <http://other.example.com/test/predicate/1> \"test literal\"^^<http://www.w3.org/2001/XMLSchema#string> _:"));
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting[] {
				BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL,
				NTriplesWriterSettings.ESCAPE_UNICODE
		};
	}
}
