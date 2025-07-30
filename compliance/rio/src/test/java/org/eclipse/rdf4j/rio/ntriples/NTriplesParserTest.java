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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.testsuite.rio.AbstractParserTestSuite;

public class NTriplesParserTest {

	public static Test suite() throws Exception {
		final TestSuite suite = new TestSuite(NTriplesParserTest.class);
		suite.addTest(NTriples11ParserTest.suite());
		suite.addTest(NTriples12ParserTest.suite());
		return suite;
	}

	static class NTriples11ParserTest extends AbstractParserTestSuite {

		private NTriples11ParserTest() {
			super("/testcases/ntriples/rdf11/", "http://www.w3.org/2013/N-TriplesTests/", RDFFormat.NTRIPLES, "NTriples");
		}

		public static Test suite() throws Exception {
			return new NTriples11ParserTest().createTestSuite();
		}

		@Override
		protected RDFParser createRDFParser() {
			return new NTriplesParser();
		}
	}

	static class NTriples12ParserTest extends AbstractParserTestSuite {

		private NTriples12ParserTest() {
			super("/testcases/ntriples/rdf12/", "http://www.w3.org/2013/N-TriplesTests/", RDFFormat.NTRIPLES, "NTriples");
		}

		public static Test suite() throws Exception {
			return new NTriples12ParserTest().createTestSuite();
		}

		@Override
		protected RDFParser createRDFParser() {
			return new NTriplesParser();
		}
	}
}
