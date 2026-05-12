/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.rio.rdfxml;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.eclipse.rdf4j.testsuite.rio.AbstractParserTestSuite;

/**
 * JUnit test for the RDF/XML parser that uses the tests that are available online from the W3C RDF test suite.
 */
public abstract class RDFXMLParser12TestCase extends AbstractParserTestSuite {

	public static String RDF_XML_TESTS_W3C_BASE_URL = "https://w3c.github.io/rdf-tests/rdf/rdf11/rdf-xml/";

	protected static String TEST_W3C_FILE_BASE_PATH = "/testcases/rdfxml/rdf11/";

	public RDFXMLParser12TestCase() {
		this(TEST_W3C_FILE_BASE_PATH, RDF_XML_TESTS_W3C_BASE_URL);
	}

	public RDFXMLParser12TestCase(String testFileBasePath, String testBaseURL) {
		super(testFileBasePath, testBaseURL, RDFFormat.RDFXML, "XML");
	}

	@Override
	protected RDFParser createRDFBaseParser() {
		return new NTriplesParser();
	}

	@Override
	protected String computeSubManifestFilePath(String subManifestFile) {
		String subManifestFilePath = "";
		if (subManifestFile.contains("rdf11")) {
			final String relativePath = subManifestFile.substring(RDF_XML_TESTS_W3C_BASE_URL.length());
			subManifestFilePath = testFileBasePath + relativePath;
		} else if (subManifestFile.startsWith(testBaseURL)) {
			final String relativePath = subManifestFile.substring(testBaseURL.length());
			subManifestFilePath = testFileBasePath + relativePath;
		}
		return subManifestFilePath;
	}

}
