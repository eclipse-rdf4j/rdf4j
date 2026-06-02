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
package org.eclipse.rdf4j.testsuite.rio.trig;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.nquads.NQuadsParser;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.eclipse.rdf4j.rio.trig.TriGParser;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.eclipse.rdf4j.testsuite.rio.AbstractParserTestSuite;

/**
 * JUnit test for the Turtle parser that uses the tests that are available
 * <a href="http://www.w3.org/2013/TurtleTests/">online</a>.
 */
public abstract class TriGParserTestCase extends AbstractParserTestSuite {
	protected static String TESTS_W3C_BASE_URL = "http://www.w3.org/2013/TriGTests/";
	protected static String TEST_W3C_FILE_BASE_PATH = "/testcases/trig/rdf11/";

	public TriGParserTestCase() {
		this(TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL);
	}

	public TriGParserTestCase(String testFileBasePath, String testBaseURL) {
		super(testFileBasePath, testBaseURL, RDFFormat.TRIG, "Trig");
	}

	@Override
	protected RDFParser createRDFParser() {
		return new TriGParser();
	}

	@Override
	protected RDFParser createRDFBaseParser() {
		return new NQuadsParser();
	}

	// for backwards compatibility
	protected RDFParser createTriGParser() {
		return createRDFParser();
	}

	// for backwards compatibility
	protected RDFParser createNQuadsParser() {
		return createRDFBaseParser();
	}
}
