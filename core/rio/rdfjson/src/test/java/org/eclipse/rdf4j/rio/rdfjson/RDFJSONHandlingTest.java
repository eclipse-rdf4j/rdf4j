/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfjson;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.AbstractParserHandlingTest;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;

/**
 * Test for error handling by RDFJSON Parser.
 *
 * @author Peter Ansell
 */
public class RDFJSONHandlingTest extends AbstractParserHandlingTest {

	@Override
	protected InputStream getRDFLangStringWithNoLanguageStream(Model model) throws Exception {
		InputStream RDFLangStringWithNoLanguageStatements = new FileInputStream(
				"src/test/resources/testcases/rdfjson/rdfjson-RDF-langString-no-language-test.rj");
		return RDFLangStringWithNoLanguageStatements;
	}

	@Override
	protected RDFParser getParser() {
		return new RDFJSONParser();
	}

	@Override
	protected RDFWriter createWriter(OutputStream output) {
		return new RDFJSONWriter(output, RDFFormat.RDFJSON);
	}
}
