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
package org.eclipse.rdf4j.testsuite.rio;

import java.io.InputStream;
import java.net.MalformedURLException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class NegativeParserTest extends TestCase {

	/*-----------*
	 * Variables *
	 *-----------*/
	private final String inputURL;

	private final String baseURL;

	private final RDFParser targetParser;

	protected IRI testUri;

	protected FailureMode failureMode;

	protected boolean didIgnoreFailure;

	private static final Logger logger = LoggerFactory.getLogger(NegativeParserTest.class);

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NegativeParserTest(IRI testUri, String caseURI, String inputURL, String baseURL, RDFParser targetParser,
			FailureMode failureMode) throws MalformedURLException {
		super(caseURI);
		this.testUri = testUri;
		this.inputURL = inputURL;
		this.baseURL = baseURL;
		this.targetParser = targetParser;
		this.failureMode = failureMode;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void runTest() {
		ParseErrorCollector el = new ParseErrorCollector();
		try {
			// Try parsing the input; this should result in an error being
			// reported.
			// targetParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

			targetParser.setRDFHandler(new StatementCollector());

			InputStream in = this.getClass().getResourceAsStream(inputURL);
			assertNotNull("Test resource was not found: inputURL=" + inputURL, in);

			logger.debug("test: " + inputURL);

			targetParser.setParseErrorListener(el);

			targetParser.parse(in, baseURL);
			in.close();

			if (failureMode.ignoreFailure()) {
				this.didIgnoreFailure = true;
				logger.warn("Ignoring Negative Parser Test that does not report an expected error: " + inputURL);
			} else {
				this.didIgnoreFailure = false;
				fail("Parser parses erroneous data without reporting errors");
			}
		} catch (RDFParseException e) {
			// This is expected as the input file is incorrect RDF
		} catch (Exception e) {
			fail("Error: " + e.getMessage());
		}
	}

} // end inner class NegativeParserTest
