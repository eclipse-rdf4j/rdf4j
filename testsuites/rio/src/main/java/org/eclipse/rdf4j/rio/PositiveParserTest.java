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
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

public class PositiveParserTest extends TestCase {

	/*-----------*
	 * Variables *
	 *-----------*/

	private String inputURL;

	private String outputURL;

	private String baseURL;

	private RDFParser targetParser;

	private RDFParser ntriplesParser;

	protected IRI testUri;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public PositiveParserTest(IRI testUri, String testName, String inputURL, String outputURL,
			String baseURL, RDFParser targetParser, RDFParser ntriplesParser)
		throws MalformedURLException
	{
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
	protected void runTest()
		throws Exception
	{
		// Parse input data
		// targetParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

		Set<Statement> inputCollection = new LinkedHashSet<Statement>();
		StatementCollector inputCollector = new StatementCollector(inputCollection);
		targetParser.setRDFHandler(inputCollector);

		InputStream in = this.getClass().getResourceAsStream(inputURL);
		assertNotNull("Test resource was not found: inputURL=" + inputURL, in);

		System.err.println("test: " + inputURL);

		ParseErrorCollector el = new ParseErrorCollector();
		targetParser.setParseErrorListener(el);

		try {
			targetParser.parse(in, baseURL);
		}
		finally {
			in.close();

			if (!el.getFatalErrors().isEmpty()) {
				System.err.println("[Turtle] Input file had fatal parsing errors: ");
				System.err.println(el.getFatalErrors());
			}

			if (!el.getErrors().isEmpty()) {
				System.err.println("[Turtle] Input file had parsing errors: ");
				System.err.println(el.getErrors());
			}

			if (!el.getWarnings().isEmpty()) {
				System.err.println("[Turtle] Input file had parsing warnings: ");
				System.err.println(el.getWarnings());
			}
		}

		if (outputURL != null) {
			// Parse expected output data
			ntriplesParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

			Set<Statement> outputCollection = new LinkedHashSet<Statement>();
			StatementCollector outputCollector = new StatementCollector(outputCollection);
			ntriplesParser.setRDFHandler(outputCollector);

			in = this.getClass().getResourceAsStream(outputURL);
			try {
				ntriplesParser.parse(in, baseURL);
			}
			finally {
				in.close();
			}

			// Check equality of the two models
			if (!Models.isomorphic(inputCollection, outputCollection)) {
				System.err.println("===models not equal===");
				System.err.println("Expected: " + outputCollection);
				System.err.println("Actual  : " + inputCollection);
				System.err.println("======================");

				fail("models not equal");
			}
		}
	}

} // end inner class PositiveParserTest