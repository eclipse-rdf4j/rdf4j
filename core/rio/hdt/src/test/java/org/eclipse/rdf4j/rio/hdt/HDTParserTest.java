/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.InputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Bart Hanssens
 */
public class HDTParserTest {
	private RDFParser parser;

	@Before
	public void setUp() throws Exception {
		parser = Rio.createParser(RDFFormat.HDT);
		parser.setParseLocationListener((line, col) -> System.err.println("byte " + line));
	}

	@Test
	public void parseSimpleSPO() {
		Model m = new LinkedHashModel();

		try (InputStream is = HDTParserTest.class.getResourceAsStream("/test.hdt")) {
			parser.setRDFHandler(new StatementCollector(m));
			parser.parse(is, "");
			assertEquals("Number of statements does not match", 43, m.size());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void parseSimplePOS() {
		Model m = new LinkedHashModel();

		try (InputStream is = HDTParserTest.class.getResourceAsStream("/test-pos.hdt")) {
			parser.setRDFHandler(new StatementCollector(m));
			parser.parse(is, "");
			assertEquals("Number of statements does not match", 43, m.size());
			fail("Unsupported not caught");
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Triples section: order 4, but only SPO order is supported");
		}
	}
}
