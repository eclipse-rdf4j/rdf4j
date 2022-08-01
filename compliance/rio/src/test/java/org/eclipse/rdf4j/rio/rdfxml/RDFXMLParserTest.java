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

import org.eclipse.rdf4j.testsuite.rio.rdfxml.RDFXMLParserTestCase;

import junit.framework.Test;

/**
 * JUnit test for the RDF/XML parser that uses the test manifest that is available
 * <a href="http://www.w3.org/2000/10/rdf-tests/rdfcore/Manifest.rdf">online</a>.
 */
public class RDFXMLParserTest extends RDFXMLParserTestCase {

	public static Test suite() throws Exception {
		return new RDFXMLParserTest().createTestSuite();
	}

	@Override
	protected RDFXMLParser createRDFParser() {
		RDFXMLParser rdfxmlParser = new RDFXMLParser();
		rdfxmlParser.setParseStandAloneDocuments(true);
		return rdfxmlParser;
	}
}
