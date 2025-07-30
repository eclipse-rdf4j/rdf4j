/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.rio;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class CanonicalizationTest extends TestCase {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final String inputURL;

	private final String outputURL;

	private final String baseURL;

	private final RDFParser parser;

	private final RDFFormat format;

	protected IRI testUri;

	private static final Logger logger = LoggerFactory.getLogger(CanonicalizationTest.class);

	/*--------------*
	 * Constructors *
	 *--------------*/

	public CanonicalizationTest(
			final IRI testUri, final String testName, final String inputURL, final String outputURL,
			final String baseURL,
			final RDFParser parser, final RDFFormat format) {
		super(testName);
		this.testUri = testUri;
		this.inputURL = inputURL;
		this.outputURL = outputURL;
		this.baseURL = baseURL;
		this.parser = parser;
		this.format = format;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void runTest() throws Exception {
		final Set<Statement> inputCollection = new LinkedHashSet<>();
		final StatementCollector inputCollector = new StatementCollector(inputCollection);
		parser.setRDFHandler(inputCollector);

		InputStream in = this.getClass().getResourceAsStream(inputURL);
		assertNotNull("Test resource was not found: inputURL=" + inputURL, in);

		logger.debug("test: " + inputURL);

		final ParseErrorCollector el = new ParseErrorCollector();
		parser.setParseErrorListener(el);

		parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

		try {
			parser.parse(in, baseURL);
		} finally {
			in.close();

			if (!el.getFatalErrors().isEmpty()) {
				logger.error("Input file had fatal parsing errors: \n" + el.getFatalErrors());
			}

			if (!el.getErrors().isEmpty()) {
				logger.error("Input file had parsing errors: \n" + el.getErrors());
			}

			if (!el.getWarnings().isEmpty()) {
				logger.warn("Input file had parsing warnings: \n" + el.getWarnings());
			}
		}

		final String canonicalOutput = new String(this.getClass().getResourceAsStream(outputURL).readAllBytes());

		final StringWriter stringWriter = new StringWriter();
		Rio.write(inputCollection, stringWriter, format);

		if (!stringWriter.toString().equals(canonicalOutput)) {
			logger.error("Writer output does not match canonical output:\n"
					+ "Expected: " + canonicalOutput
					+ "Actual:   " + stringWriter.toString());
			fail("Input did not produce canonical output");
		}
	}

}
