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
package org.eclipse.rdf4j.rio.turtle;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.AbstractParserHandlingTest;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;

/**
 * Test for error handling by Turtle Parser.
 *
 * @author Peter Ansell
 */
public class TurtleHandlingTest extends AbstractParserHandlingTest {
	@Override
	protected InputStream getRDFLangStringWithNoLanguageStream(Model model) throws Exception {
		InputStream RDFLangStringWithNoLanguageStatements = new FileInputStream(
				"src/test/resources/testcases/turtle/turtle-RDF-langString-no-language-test.ttl");
		return RDFLangStringWithNoLanguageStatements;
	}

	@Override
	protected RDFParser getParser() {
		return new TurtleParser();
	}

	@Override
	protected RDFWriter createWriter(OutputStream output) {
		return new TurtleWriter(output);
	}
}
