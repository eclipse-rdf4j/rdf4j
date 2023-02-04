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
package org.eclipse.rdf4j.rio.hdt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Bart Hanssens
 */
public class HDTParserTest {
	private RDFParser parser;

	@BeforeEach
	public void setUp() throws Exception {
		parser = Rio.createParser(RDFFormat.HDT);
		parser.setParseLocationListener((line, col) -> System.err.println("byte " + line));
	}

	@Test
	public void parseSimpleSPO() {
		// load original N-Triples file
		Model orig = new LinkedHashModel();
		try (InputStream is = HDTParserTest.class.getResourceAsStream("/test-orig.nt")) {
			RDFParser nt = Rio.createParser(RDFFormat.NTRIPLES);
			nt.setRDFHandler(new StatementCollector(orig));
			nt.parse(is, "");
		} catch (Exception e) {
			fail(e.getMessage());
		}

		Model m = new LinkedHashModel();
		try (InputStream is = HDTParserTest.class.getResourceAsStream("/test.hdt")) {
			parser.setRDFHandler(new StatementCollector(m));
			parser.parse(is, "");
			assertEquals(43, m.size(), "Number of statements does not match");
		} catch (Exception e) {
			fail(e.getMessage());
		}

		orig.removeAll(m);
		assertEquals(0, orig.size(), "HDT model does not match original NT file");
	}

	@Test
	public void parseSimplePOS() {
		Model m = new LinkedHashModel();

		try (InputStream is = HDTParserTest.class.getResourceAsStream("/test-pos.hdt")) {
			parser.setRDFHandler(new StatementCollector(m));
			parser.parse(is, "");
			assertEquals(43, m.size(), "Number of statements does not match");
			fail("Unsupported not caught");
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Triples section: order 4, but only SPO order is supported");
		}
	}
}
