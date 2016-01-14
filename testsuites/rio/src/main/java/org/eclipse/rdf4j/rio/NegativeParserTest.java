/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import java.io.InputStream;
import java.net.MalformedURLException;

import junit.framework.TestCase;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

public class NegativeParserTest extends TestCase {

	/*-----------*
	 * Variables *
	 *-----------*/
	private String inputURL;

	private String baseURL;

	private RDFParser targetParser;

	protected IRI testUri;

	protected FailureMode failureMode;

	protected boolean didIgnoreFailure;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NegativeParserTest(IRI testUri, String caseURI, String inputURL, String baseURL,
			RDFParser targetParser, FailureMode failureMode)
		throws MalformedURLException
	{
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
			
			System.err.println("test: " + inputURL);

			targetParser.setParseErrorListener(el);

			targetParser.parse(in, baseURL);
			in.close();

			if (failureMode.ignoreFailure()) {
				this.didIgnoreFailure = true;
				System.err.println("Ignoring Negative Parser Test that does not report an expected error: "
						+ inputURL);
			}
			else {
				this.didIgnoreFailure = false;
				fail("Parser parses erroneous data without reporting errors");
			}
		}
		catch (RDFParseException e) {
			// This is expected as the input file is incorrect RDF
		}
		catch (Exception e) {
			fail("Error: " + e.getMessage());
		}
	}

} // end inner class NegativeParserTest