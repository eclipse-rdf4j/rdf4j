/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.nquads;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RepositoryUtil;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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
	public void setUp()
		throws Exception
	{
		parser = rdfParserFactory.getParser();
		vf = SimpleValueFactory.getInstance();
	}

	@After
	public void tearDown() {
		parser = null;
		writer = null;
	}

	public void testWrite()
		throws RepositoryException, RDFParseException, IOException, RDFHandlerException
	{
		Repository rep1 = new SailRepository(new MemoryStore());
		rep1.initialize();

		RepositoryConnection con1 = rep1.getConnection();

		URL ciaScheme = this.getClass().getResource("/cia-factbook/CIA-onto-enhanced.rdf");
		URL ciaFacts = this.getClass().getResource("/cia-factbook/CIA-facts-enhanced.rdf");

		con1.add(
				ciaScheme,
				ciaScheme.toExternalForm(),
				Rio.getParserFormatForFileName(ciaScheme.toExternalForm()).orElseThrow(
						Rio.unsupportedFormat(ciaScheme.toExternalForm())));
		con1.add(
				ciaFacts,
				ciaFacts.toExternalForm(),
				Rio.getParserFormatForFileName(ciaFacts.toExternalForm()).orElseThrow(
						Rio.unsupportedFormat(ciaFacts.toExternalForm())));

		StringWriter writer = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);
		con1.export(rdfWriter);

		con1.close();

		Repository rep2 = new SailRepository(new MemoryStore());
		rep2.initialize();

		RepositoryConnection con2 = rep2.getConnection();

		con2.add(new StringReader(writer.toString()), "foo:bar", RDFFormat.NQUADS);
		con2.close();

		Assert.assertTrue("result of serialization and re-upload should be equal to original",
				RepositoryUtil.equals(rep1, rep2));
	}

	@Test
	public void testReadWrite()
		throws RDFHandlerException, IOException, RDFParseException
	{
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
	public void testNoContext()
		throws RDFHandlerException
	{
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
	public void testNoContextAddXSDString()
		throws RDFHandlerException
	{
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
	public void testBlankNodeContext()
		throws RDFHandlerException
	{
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
		Assert.assertTrue(lines[0].startsWith("<http://test.example.org/test/subject/1> <http://other.example.com/test/predicate/1> \"test literal\" _:"));
	}

	@Test
	public void testBlankNodeContextAddXSDString()
		throws RDFHandlerException
	{
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
		Assert.assertTrue(lines[0].startsWith("<http://test.example.org/test/subject/1> <http://other.example.com/test/predicate/1> \"test literal\"^^<http://www.w3.org/2001/XMLSchema#string> _:"));
	}
}
