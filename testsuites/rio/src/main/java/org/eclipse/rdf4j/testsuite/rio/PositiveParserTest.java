/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.rio;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class PositiveParserTest extends TestCase {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final String inputURL;

	private String outputURL;

	private final String baseURL;

	private final RDFParser targetParser;

	private final RDFParser ntriplesParser;

	protected IRI testUri;

	private static final Logger logger = LoggerFactory.getLogger(PositiveParserTest.class);

	/*--------------*
	 * Constructors *
	 *--------------*/

	public PositiveParserTest(IRI testUri, String testName, String inputURL, String outputURL, String baseURL,
			RDFParser targetParser, RDFParser ntriplesParser) throws MalformedURLException {
		super(testName);
		this.testUri = testUri;
		this.inputURL = inputURL;
		if (outputURL != null) {
			this.outputURL = outputURL;
		}
		this.baseURL = baseURL;
		this.targetParser = targetParser;
		this.ntriplesParser = ntriplesParser;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void runTest() throws Exception {
		// Parse input data
		// targetParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

		Set<Statement> inputCollection = new LinkedHashSet<>();
		StatementCollector inputCollector = new StatementCollector(inputCollection);
		targetParser.setRDFHandler(inputCollector);

		InputStream in = this.getClass().getResourceAsStream(inputURL);
		assertNotNull("Test resource was not found: inputURL=" + inputURL, in);

		logger.debug("test: " + inputURL);

		ParseErrorCollector el = new ParseErrorCollector();
		targetParser.setParseErrorListener(el);

		try {
			targetParser.parse(in, baseURL);
		} finally {
			in.close();

			if (!el.getFatalErrors().isEmpty()) {
				logger.error("[Turtle] Input file had fatal parsing errors: \n" + el.getFatalErrors());
			}

			if (!el.getErrors().isEmpty()) {
				logger.error("[Turtle] Input file had parsing errors: \n" + el.getErrors());
			}

			if (!el.getWarnings().isEmpty()) {
				logger.warn("[Turtle] Input file had parsing warnings: \n" + el.getWarnings());
			}
		}

		if (outputURL != null) {
			// Parse expected output data
			Set<Statement> outputCollection = new LinkedHashSet<>();
			StatementCollector outputCollector = new StatementCollector(outputCollection);
			ntriplesParser.setRDFHandler(outputCollector);

			in = this.getClass().getResourceAsStream(outputURL);
			try {
				ntriplesParser.parse(in, baseURL);
			} finally {
				in.close();
			}

			// Check equality of the two models
			if (!Models.isomorphic(inputCollection, outputCollection)) {
				logger.error("===models not equal===\n"
						+ "Expected: " + outputCollection + "\n"
						+ "Actual  : " + inputCollection + "\n"
						+ "======================");

				fail("models not equal");
			}
		}
	}

} // end inner class PositiveParserTest
