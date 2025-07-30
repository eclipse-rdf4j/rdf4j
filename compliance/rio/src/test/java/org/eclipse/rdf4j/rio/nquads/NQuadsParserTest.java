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
package org.eclipse.rdf4j.rio.nquads;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.testsuite.rio.AbstractParserTestSuite;

public class NQuadsParserTest {

	public static Test suite() throws Exception {
		final TestSuite suite = new TestSuite(NQuadsParserTest.class);
		suite.addTest(NQuads11ParserTest.suite());
		suite.addTest(NQuads12ParserTest.suite());
		return suite;
	}

	static class NQuads11ParserTest extends AbstractParserTestSuite {

		private NQuads11ParserTest() {
			super("/testcases/nquads/rdf11/", "http://www.w3.org/2013/N-QuadsTests/", RDFFormat.NQUADS, "NQuads");
		}

		public static Test suite() throws Exception {
			return new NQuads11ParserTest().createTestSuite();
		}

		@Override
		protected RDFParser createRDFParser() {
			return new NQuadsParser();
		}
	}

	static class NQuads12ParserTest extends AbstractParserTestSuite {

		private NQuads12ParserTest() {
			super("/testcases/nquads/rdf12/", "http://www.w3.org/2013/N-QuadsTests/", RDFFormat.NQUADS, "NQuads");
		}

		public static Test suite() throws Exception {
			return new NQuads12ParserTest().createTestSuite();
		}

		@Override
		protected RDFParser createRDFParser() {
			return new NQuadsParser();
		}
	}
}
