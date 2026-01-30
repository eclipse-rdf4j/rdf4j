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
package org.eclipse.rdf4j.testsuite.rio.ntriples;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.eclipse.rdf4j.testsuite.rio.AbstractParserTestSuite;

/**
 * JUnit test for the N-Triples parser that uses the tests that are available
 * <a href="http://www.w3.org/2013/N-TriplesTests/">online</a>.
 */
public abstract class AbstractNTriplesParserTest extends AbstractParserTestSuite {
	protected static final String TESTS_W3C_BASE_URL = "http://www.w3.org/2013/N-TriplesTests/";
	protected static final String TEST_W3C_FILE_BASE_PATH = "/testcases/ntriples/rdf11/";

	public AbstractNTriplesParserTest() {
		this(TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL);
	}

	public AbstractNTriplesParserTest(String testFileBasePath, String testBaseURL) {
		super(testFileBasePath, testBaseURL, RDFFormat.NTRIPLES, "NTriples");
	}

	@Override
	protected RDFParser createRDFParser() {
		return new NTriplesParser();
	}
}
