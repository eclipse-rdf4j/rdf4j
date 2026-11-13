/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jelly;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.AbstractParserHandlingTest;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;

import eu.neverblink.jelly.convert.rdf4j.rio.JellyParserFactory;
import eu.neverblink.jelly.convert.rdf4j.rio.JellyWriterFactory;

/**
 * @author Piotr Sowiński
 */
public class JellyHandlingTest extends AbstractParserHandlingTest {

	@Override
	protected InputStream getRDFLangStringWithNoLanguageStream(Model model) throws Exception {
		String fileName = "src/test/resources/testcases/jelly/jelly-langString-no-language-test.jelly";
		InputStream file = new FileInputStream(fileName);
		long fileSize = new File(fileName).length();
		byte[] byteArray = new byte[(int) fileSize];

		file.read(byteArray);

		return new ByteArrayInputStream(byteArray);
	}

	@Override
	protected RDFParser getParser() {
		return new JellyParserFactory().getParser();
	}

	@Override
	protected RDFWriter createWriter(OutputStream output) {
		return new JellyWriterFactory().getWriter(output);
	}
}
