/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class RDFXMLParserTest {

	private ValueFactory vf;

	private RDFParser parser;

	private StatementCollector sc;

	private ParseErrorCollector el;

	@Before
	public void setUp()
		throws Exception
	{
		vf = SimpleValueFactory.getInstance();
		parser = new RDFXMLParser();
		sc = new StatementCollector();
		parser.setRDFHandler(sc);
		el = new ParseErrorCollector();
		parser.setParseErrorListener(el);
	}

	@Test
	public void rdfXmlLoadedFromInsideAJarResolvesRelativeUris()
		throws Exception
	{
		URL zipfileUrl = this.getClass().getResource(
				"/org/eclipse/rdf4j/rio/rdfxml/sample-with-rdfxml-data.zip");

		assertNotNull("The sample-data.zip file must be present for this test", zipfileUrl);

		String url = "jar:" + zipfileUrl + "!/index.rdf";

		try (final InputStream in = new URL(url).openStream();) {
			parser.parse(in, url);
		}

		Collection<Statement> stmts = sc.getStatements();

		assertThat(stmts, Matchers.<Statement> iterableWithSize(3));

		Iterator<Statement> iter = stmts.iterator();

		Statement stmt1 = iter.next();
		Statement stmt2 = iter.next();

		assertEquals(vf.createIRI("http://www.example.com/#"), stmt1.getSubject());
		assertEquals(vf.createIRI("http://www.example.com/ns/#document-about"), stmt1.getPredicate());
		assertTrue(stmt1.getObject() instanceof IRI);

		IRI res = (IRI)stmt1.getObject();

		String resourceUrl = res.stringValue();

		assertThat(resourceUrl, CoreMatchers.startsWith("jar:" + zipfileUrl + "!"));

		URL javaUrl = new URL(resourceUrl);
		assertEquals("jar", javaUrl.getProtocol());

		try (InputStream uc = javaUrl.openStream();) {
			assertEquals("The resource stream should be empty", -1, uc.read());
		}

		assertEquals(res, stmt2.getSubject());
		assertEquals(DC.TITLE, stmt2.getPredicate());
		assertEquals(vf.createLiteral("Empty File"), stmt2.getObject());
	}

	@Test
	public void testRDFXMLWhitespace()
		throws Exception
	{
		try (final InputStream in = this.getClass().getResourceAsStream(
				"/org/eclipse/rdf4j/rio/rdfxml/rdfxml-whitespace-literal.rdf");)
		{
			parser.parse(in, "");
		}
		Statement stmt1 = sc.getStatements().iterator().next();
		assertEquals(1, sc.getStatements().size());
		assertEquals(RDFS.LABEL, stmt1.getPredicate());
		assertEquals(vf.createLiteral("  Literal with whitespace  "), stmt1.getObject());
	}

	@Test
	public void testFatalErrorPrologContent()
		throws Exception
	{
		// Temporarily override System.err to verify that nothing is being printed to it for this test
		PrintStream oldErr = System.err;
		ByteArrayOutputStream tempErr = new ByteArrayOutputStream();
		System.setErr(new PrintStream(tempErr));
		PrintStream oldOut = System.out;
		ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
		System.setOut(new PrintStream(tempOut));
		try (final InputStream in = this.getClass().getResourceAsStream(
				"/org/eclipse/rdf4j/rio/rdfxml/not-an-rdfxml-file.rdf");)
		{
			parser.parse(in, "");
		}
		catch (RDFParseException e) {
			// FIXME exact error message is locale-dependent. Just fall through, error is expected. See #280.
			//			assertEquals("Content is not allowed in prolog. [line 1, column 1]", e.getMessage());
		}
		finally {
			// Reset System Error output to ensure that we don't interfere with other tests
			System.setErr(oldErr);
			// Reset System Out output to ensure that we don't interfere with other tests
			System.setOut(oldOut);
		}
		// Verify nothing was printed to System.err during test
		assertEquals(0, tempErr.size());
		// Verify nothing was printed to System.out during test
		assertEquals(0, tempOut.size());
		assertEquals(0, el.getWarnings().size());
		assertEquals(0, el.getErrors().size());
		assertEquals(1, el.getFatalErrors().size());
		assertEquals("[Rio fatal] Content is not allowed in prolog. (1, 1)", el.getFatalErrors().get(0));
	}
}
