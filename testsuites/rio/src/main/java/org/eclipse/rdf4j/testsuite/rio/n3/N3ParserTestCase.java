/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.rio.n3;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit test for the N3 parser that uses the tests that are available
 * <a href="http://www.w3.org/2000/10/swap/test/n3parser.tests">online</a>.
 */
public abstract class N3ParserTestCase {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String BASE_URL = "http://www.w3.org/2000/10/swap/test/";

	private static final String MANIFEST_URL = "http://www.w3.org/2000/10/swap/test/n3parser.tests";

	/*--------------------*
	 * Static initializer *
	 *--------------------*/

	public TestSuite createTestSuite() throws Exception {
		// Create test suite
		TestSuite suite = new TestSuite(N3ParserTestCase.class.getName());

		// Add the manifest to a repository and query it
		URL url = url(MANIFEST_URL);

		ValueFactory f = SimpleValueFactory.getInstance();

		String n3test = "http://www.w3.org/2004/11/n3test#";

		IRI POSITIVE_PARSER_TEST = f.createIRI(n3test, "PositiveParserTest");
		IRI NEGATIVE_PARSER_TEST = f.createIRI(n3test, "NegativeParserTest");

		IRI INPUT_DOCUMENT = f.createIRI(n3test, "inputDocument");
		IRI OUTPUT_DOCUMENT = f.createIRI(n3test, "outputDocument");

		try (InputStream in = url.openStream()) {
			Model manifest = Rio.parse(in, base(MANIFEST_URL), RDFFormat.TURTLE);

			// add all positive parser tests
			Set<Resource> positiveTests = manifest.filter(null, RDF.TYPE, POSITIVE_PARSER_TEST).subjects();
			for (Resource s : positiveTests) {
				String inputURL = Models.getProperty(manifest, s, INPUT_DOCUMENT).toString();
				String outputURL = Models.getProperty(manifest, s, OUTPUT_DOCUMENT).toString();
				suite.addTest(new PositiveParserTest(s.toString(), inputURL, outputURL));
			}

			// add all negative parser tests
			Set<Resource> negativeTests = manifest.filter(null, RDF.TYPE, NEGATIVE_PARSER_TEST).subjects();
			for (Resource s : negativeTests) {
				String inputURL = Models.getProperty(manifest, s, INPUT_DOCUMENT).toString();
				suite.addTest(new NegativeParserTest(s.toString(), inputURL));
			}
		}

		return suite;
	}

	protected abstract RDFParser createRDFParser();

	/*--------------------------------*
	 * Inner class PositiveParserTest *
	 *--------------------------------*/

	private class PositiveParserTest extends TestCase {

		/*-----------*
		 * Variables *
		 *-----------*/

		private final URL inputURL;

		private final URL outputURL;

		/*--------------*
		 * Constructors *
		 *--------------*/

		public PositiveParserTest(String testURI, String inputURL, String outputURL) throws MalformedURLException {
			super(testURI);
			this.inputURL = url(inputURL);
			this.outputURL = url(outputURL);
		}

		/*---------*
		 * Methods *
		 *---------*/

		@Override
		protected void runTest() throws Exception {
			// Parse input data
			RDFParser turtleParser = createRDFParser();

			Set<Statement> inputCollection = new LinkedHashSet<>();
			StatementCollector inputCollector = new StatementCollector(inputCollection);
			turtleParser.setRDFHandler(inputCollector);

			InputStream in = inputURL.openStream();
			turtleParser.parse(in, base(inputURL.toExternalForm()));
			in.close();

			// Parse expected output data
			NTriplesParser ntriplesParser = new NTriplesParser();

			Set<Statement> outputCollection = new LinkedHashSet<>();
			StatementCollector outputCollector = new StatementCollector(outputCollection);
			ntriplesParser.setRDFHandler(outputCollector);

			in = outputURL.openStream();
			ntriplesParser.parse(in, base(outputURL.toExternalForm()));
			in.close();

			// Check equality of the two models
			if (!Models.isomorphic(inputCollection, outputCollection)) {
				System.err.println("===models not equal===");
				// System.err.println("Expected: " + outputCollection);
				// System.err.println("Actual : " + inputCollection);
				// System.err.println("======================");

				List<Statement> missing = new LinkedList<>(outputCollection);
				missing.removeAll(inputCollection);

				List<Statement> unexpected = new LinkedList<>(inputCollection);
				unexpected.removeAll(outputCollection);

				if (!missing.isEmpty()) {
					System.err.println("Missing   : " + missing);
				}
				if (!unexpected.isEmpty()) {
					System.err.println("Unexpected: " + unexpected);
				}

				fail("models not equal");
			}
		}

	} // end inner class PositiveParserTest

	/*--------------------------------*
	 * Inner class NegativeParserTest *
	 *--------------------------------*/

	private class NegativeParserTest extends TestCase {

		/*-----------*
		 * Variables *
		 *-----------*/

		private final URL inputURL;

		/*--------------*
		 * Constructors *
		 *--------------*/

		public NegativeParserTest(String testURI, String inputURL) throws MalformedURLException {
			super(testURI);
			this.inputURL = url(inputURL);
		}

		/*---------*
		 * Methods *
		 *---------*/

		@Override
		protected void runTest() {
			try {
				// Try parsing the input; this should result in an error being
				// reported.
				RDFParser turtleParser = createRDFParser();

				turtleParser.setRDFHandler(new StatementCollector());

				InputStream in = inputURL.openStream();
				turtleParser.parse(in, base(inputURL.toExternalForm()));
				in.close();

				fail("Parser parses erroneous data without reporting errors");
			} catch (RDFParseException e) {
				// This is expected as the input file is incorrect RDF
			} catch (Exception e) {
				fail("Error: " + e.getMessage());
			}
		}

	} // end inner class NegativeParserTest

	private static URL url(String uri) throws MalformedURLException {
		return new URL(uri);
	}

	private static String base(String uri) {
		return uri;
	}
}
