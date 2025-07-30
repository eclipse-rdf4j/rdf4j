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
package org.eclipse.rdf4j.rio.turtle;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.eclipse.rdf4j.testsuite.rio.AbstractParserTestSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TurtleParserTest {

	public static Test suite() throws Exception {
		final TestSuite suite = new TestSuite(TurtleParserTest.class);
		suite.addTest(Turtle11ParserTest.suite());
		suite.addTest(Turtle12ParserTest.suite());
		return suite;
	}

	static class Turtle12ParserTest extends AbstractParserTestSuite {

		protected static final String TESTS_W3C_BASE_URL = "https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/";
		protected static final String TEST_W3C_FILE_BASE_PATH_RDF12 = "/testcases/turtle/rdf12/";

		private Turtle12ParserTest() {
			super(TEST_W3C_FILE_BASE_PATH_RDF12, TESTS_W3C_BASE_URL, RDFFormat.TURTLE, "Turtle");
		}

		public static Test suite() throws Exception {
			return new Turtle12ParserTest().createTestSuite();
		}

		@Override
		protected RDFParser createRDFParser() {
			return new TurtleParser();
		}

		@Override
		protected RDFParser createRDFBaseParser() {
			return new NTriplesParser();
		}
	}

	static class Turtle11ParserTest extends AbstractParserTestSuite {
		protected static final String TESTS_W3C_BASE_URL = "https://w3c.github.io/rdf-tests/rdf/rdf11/rdf-turtle/";
		protected static final String TEST_W3C_FILE_BASE_PATH = "/testcases/turtle/rdf11/";

		public Turtle11ParserTest() {
			super(TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL, RDFFormat.TURTLE, "Turtle");
		}

		public static Test suite() throws Exception {
			return new Turtle11ParserTest().createTestSuite();
		}

		@Override
		protected RDFParser createRDFParser() {
			return new TurtleParser();
		}

		@Override
		protected RDFParser createRDFBaseParser() {
			return new NTriplesParser();
		}
	}
}
