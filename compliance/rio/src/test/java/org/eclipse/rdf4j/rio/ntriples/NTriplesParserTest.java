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
package org.eclipse.rdf4j.rio.ntriples;

import org.eclipse.rdf4j.testsuite.rio.ntriples.AbstractNTriplesParserTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class NTriplesParserTest {

	public static Test suite() throws Exception {
		final TestSuite suite = new TestSuite();
		suite.addTest(NTriples11ParserTest.suite());
		suite.addTest(NTriples12ParserTest.suite());
		return suite;
	}

	static class NTriples11ParserTest extends AbstractNTriplesParserTest {
		public static Test suite() throws Exception {
			return new NTriples11ParserTest().createTestSuite();
		}
	}

	static class NTriples12ParserTest extends AbstractNTriplesParserTest {
		protected static final String TESTS_W3C_BASE_URL = "https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-n-triples/";
		protected static final String TEST_W3C_FILE_BASE_PATH_RDF12 = "/testcases/ntriples/rdf12/";

		private NTriples12ParserTest() {
			super(TEST_W3C_FILE_BASE_PATH_RDF12, TESTS_W3C_BASE_URL);
		}

		public static Test suite() throws Exception {
			return new NTriples12ParserTest().createTestSuite();
		}
	}
}
