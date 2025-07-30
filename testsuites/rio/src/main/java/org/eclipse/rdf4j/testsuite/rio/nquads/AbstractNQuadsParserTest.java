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
package org.eclipse.rdf4j.testsuite.rio.nquads;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.nquads.NQuadsParser;
import org.eclipse.rdf4j.testsuite.rio.AbstractParserTestSuite;

/**
 * JUnit test for the N-Quads parser that uses the tests that are available
 * <a href="http://www.w3.org/2013/N-QuadsTests/">online</a>.
 */
public abstract class AbstractNQuadsParserTest extends AbstractParserTestSuite {
	protected static final String TESTS_W3C_BASE_URL = "http://www.w3.org/2013/N-QuadsTests/";
	protected static final String TEST_W3C_FILE_BASE_PATH = "/testcases/nquads/rdf11/";

	public AbstractNQuadsParserTest() {
		this(TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL);
	}

	public AbstractNQuadsParserTest(String testFileBasePath, String testBaseURL) {
		super(testFileBasePath, testBaseURL, RDFFormat.NQUADS, "NQuads");
	}

	@Override
	protected RDFParser createRDFParser() {
		return new NQuadsParser();
	}
}
