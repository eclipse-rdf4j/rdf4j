/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Bart Hanssens
 */
public class AbstractRDFParserTest {
	private MyRDFParser parser;

	private class MyRDFParser extends AbstractRDFParser {
		@Override
		public RDFFormat getRDFFormat() {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public void parse(InputStream in, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public void parse(Reader reader, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public Resource getBNode() {
			return createNode();
		}

		public Resource getBNode(String id) {
			return createNode(id);
		}
	}

	@Before
	public void setUp() throws Exception {
		parser = new MyRDFParser();
	}

	@Test
	public void testSkolemOrigin() throws Exception {
		parser.getParserConfig().set(BasicParserSettings.SKOLEMIZE_ORIGIN, "http://www.example.com");

		assertTrue(parser.getBNode().toString().startsWith("http://www.example.com"));
		assertTrue(parser.getBNode("12").toString().startsWith("http://www.example.com"));
	}

	@Test
	public void testSkolemOriginReset() throws Exception {
		parser.getParserConfig().set(BasicParserSettings.SKOLEMIZE_ORIGIN, "http://www.example.com");
		parser.getParserConfig().set(BasicParserSettings.SKOLEMIZE_ORIGIN, "");

		assertFalse(parser.getBNode().toString().startsWith("http://www.example.com"));
		assertTrue(parser.getBNode().toString().startsWith("_"));
		assertTrue(parser.getBNode("12").toString().endsWith("12"));
	}
}
