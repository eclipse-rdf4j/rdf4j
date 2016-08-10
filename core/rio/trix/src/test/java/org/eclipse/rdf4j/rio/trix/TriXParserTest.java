/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.trix;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Before;
import org.junit.Test;

public class TriXParserTest {

	private ValueFactory vf;

	private RDFParser parser;

	private StatementCollector sc;

	private ParseErrorCollector el;

	@Before
	public void setUp()
		throws Exception
	{
		vf = SimpleValueFactory.getInstance();
		parser = new TriXParser();
		sc = new StatementCollector();
		parser.setRDFHandler(sc);
		el = new ParseErrorCollector();
		parser.setParseErrorListener(el);
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
				"/org/eclipse/rdf4j/rio/trix/not-a-trix-file.trix");)
		{
			parser.parse(in, "");
		}
		catch (RDFParseException e) {
			// FIXME exact error message is locale-dependent. Just fall through, error is expected. See #280.
			// assertEquals("Content is not allowed in prolog. [line 1, column 1]", e.getMessage());
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
