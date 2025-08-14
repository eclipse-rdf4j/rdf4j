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
package org.eclipse.rdf4j.rio.trig;

import org.eclipse.rdf4j.testsuite.rio.trig.TriGParserTestCase;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TriGParserTest {

	public static Test suite() throws Exception {
		final TestSuite suite = new TestSuite();
		suite.addTest(TriG11ParserTest.suite());
//		suite.addTest(TriG12ParserTest.suite());
		return suite;
	}

	static class TriG11ParserTest extends TriGParserTestCase {
		public static Test suite() throws Exception {
			return new TriG11ParserTest().createTestSuite();
		}
	}

	static class TriG12ParserTest extends TriGParserTestCase {
		protected static final String TESTS_W3C_BASE_URL = "https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-trig/";
		protected static final String TEST_W3C_FILE_BASE_PATH = "/testcases/trig/rdf12/";

		public TriG12ParserTest() {
			super(TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL);
		}

		public static Test suite() throws Exception {
			return new TriG12ParserTest().createTestSuite();
		}
	}
}
