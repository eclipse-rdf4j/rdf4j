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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.Reader;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
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
		public void parse(InputStream in, String baseURI) throws RDFParseException, RDFHandlerException {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public void parse(Reader reader, String baseURI) throws RDFParseException, RDFHandlerException {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public Resource getBNode() {
			return createNode();
		}

		public Resource getBNode(String id) {
			return createNode(id);
		}

		public void setNamespace(String prefix, String namespace) {
			super.setNamespace(prefix, namespace);
		}

		public String getNamespace(String prefix) throws RDFParseException {
			return super.getNamespace(prefix);
		}
	}

	@BeforeEach
	public void setUp() {
		parser = new MyRDFParser();
	}

	@Test
	public void testSkolemOrigin() {
		parser.getParserConfig().set(BasicParserSettings.SKOLEMIZE_ORIGIN, "http://www.example.com");

		assertTrue(parser.getBNode().toString().startsWith("http://www.example.com"));
		assertTrue(parser.getBNode("12").toString().startsWith("http://www.example.com"));
	}

	@Test
	public void testSkolemOriginReset() {
		parser.getParserConfig().set(BasicParserSettings.SKOLEMIZE_ORIGIN, "http://www.example.com");
		parser.getParserConfig().set(BasicParserSettings.SKOLEMIZE_ORIGIN, "");

		assertFalse(parser.getBNode().toString().startsWith("http://www.example.com"));
		assertTrue(parser.getBNode().toString().startsWith("_"));
		assertTrue(parser.getBNode("12").toString().endsWith("12"));
	}

	@Test
	public void testNodeIdHashing() {
		// node ids look like "genid_.*-suffix
		assertThat(parser.createNode("someid").stringValue())
				.endsWith("-someid");

		// some long id (length > 32) => suffix is hashed
		String longNodeId = "someverylongnodeidwithmorethan32characters";
		assertThat(parser.createNode(longNodeId).stringValue())
				.endsWith("2A372A91878F0980C8F53341D2D8A944");
	}

	@Test
	public void testSetNamespace_DuplicateWithSameNamespace() {
		// Test that setting the same namespace twice is allowed (last one wins)
		parser.setNamespace("foaf", "http://xmlns.com/foaf/0.1/");
		parser.setNamespace("foaf", "http://xmlns.com/foaf/0.1/");

		// Should not throw an exception - namespace should be available
		String namespace = parser.getNamespace("foaf");
		assertThat(namespace).isEqualTo("http://xmlns.com/foaf/0.1/");
	}

	@Test
	public void testSetNamespace_DuplicateWithDifferentNamespace() {
		// Test that setting different namespaces for same prefix overwrites (existing behavior)
		parser.setNamespace("foaf", "http://xmlns.com/foaf/0.1/");
		parser.setNamespace("foaf", "http://example.org/different/");

		// Should overwrite - current behavior of AbstractRDFParser
		String namespace = parser.getNamespace("foaf");
		assertThat(namespace).isEqualTo("http://example.org/different/");
	}
}
