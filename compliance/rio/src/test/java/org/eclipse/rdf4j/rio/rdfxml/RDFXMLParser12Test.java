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
package org.eclipse.rdf4j.rio.rdfxml;

import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.testsuite.rio.rdfxml.RDFXMLParser12TestCase;

import junit.framework.Test;

public class RDFXMLParser12Test extends RDFXMLParser12TestCase {

	protected static final String TESTS_W3C_BASE_URL = "https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-xml/";

	protected static final String TEST_W3C_FILE_BASE_PATH_RDF12 = "/testcases/rdfxml/rdf12/";

	private RDFXMLParser12Test() {
		super(TEST_W3C_FILE_BASE_PATH_RDF12, TESTS_W3C_BASE_URL);
	}

	public static Test suite() throws Exception {
		return new RDFXMLParser12Test().createTestSuite();
	}

	@Override
	protected RDFParser createRDFParser() {
		RDFXMLParser parser = new RDFXMLParser();
		parser.setParseStandAloneDocuments(true);
		return parser;
	}
}
