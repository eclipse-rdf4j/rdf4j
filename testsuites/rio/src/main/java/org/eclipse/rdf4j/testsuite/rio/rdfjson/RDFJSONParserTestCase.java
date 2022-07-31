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
package org.eclipse.rdf4j.testsuite.rio.rdfjson;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit test for the RDFJSON Parser.
 *
 * @author Peter Ansell
 */
public abstract class RDFJSONParserTestCase {

	/*-----------*
	 * Constants *
	 *-----------*/

	protected static String BASE_URL = "http://example/base/";

	private static final String TEST_FILE_BASE_PATH = "/testcases/rdfjson/";

	private static final String MANIFEST_GOOD_URL = "/testcases/rdfjson/manifest.ttl";

	/*--------------------*
	 * Static initializer *
	 *--------------------*/

	public TestSuite createTestSuite() throws Exception {
		// Create test suite
		TestSuite suite = new TestSuite(RDFJSONParserTestCase.class.getName());

		// Add the manifest for positive test cases to a repository and query it
		Repository repository = new SailRepository(new MemoryStore());
		try (RepositoryConnection con = repository.getConnection()) {

			InputStream inputStream = this.getClass().getResourceAsStream(MANIFEST_GOOD_URL);
			con.add(inputStream, BASE_URL, RDFFormat.TURTLE);

			StringBuilder positiveQuery = new StringBuilder();
			positiveQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
			positiveQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
			positiveQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
			positiveQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
			positiveQuery.append(" WHERE { \n");
			positiveQuery.append("     ?test a rdft:TestRDFJSONPositiveSyntax . ");
			positiveQuery.append("     ?test mf:name ?testName . ");
			positiveQuery.append("     ?test mf:action ?inputURL . ");
			positiveQuery.append(" }");

			try (TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, positiveQuery.toString())
					.evaluate()) {
				// Add all positive parser tests to the test suite
				while (queryResult.hasNext()) {
					BindingSet bindingSet = queryResult.next();
					String nextTestName = ((Literal) bindingSet.getValue("testName")).getLabel();
					String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString());
					String nextInputURL = TEST_FILE_BASE_PATH + nextTestFile;

					String nextBaseUrl = BASE_URL + nextTestFile;

					suite.addTest(new PositiveParserTest(nextTestName, nextInputURL, null, nextBaseUrl));
				}
			}

			StringBuilder negativeQuery = new StringBuilder();
			negativeQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
			negativeQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
			negativeQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
			negativeQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
			negativeQuery.append(" WHERE { \n");
			negativeQuery.append("     ?test a rdft:TestRDFJSONNegativeSyntax . ");
			negativeQuery.append("     ?test mf:name ?testName . ");
			negativeQuery.append("     ?test mf:action ?inputURL . ");
			negativeQuery.append(" }");

			try (var queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, negativeQuery.toString()).evaluate()) {

				// Add all negative parser tests to the test suite
				while (queryResult.hasNext()) {
					BindingSet bindingSet = queryResult.next();
					String nextTestName = ((Literal) bindingSet.getValue("testName")).toString();
					String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString());
					String nextInputURL = TEST_FILE_BASE_PATH + nextTestFile;

					String nextBaseUrl = BASE_URL + nextTestFile;

					suite.addTest(new NegativeParserTest(nextTestName, nextInputURL, nextBaseUrl));
				}
			}

			StringBuilder positiveEvalQuery = new StringBuilder();
			positiveEvalQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
			positiveEvalQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
			positiveEvalQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
			positiveEvalQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
			positiveEvalQuery.append(" WHERE { \n");
			positiveEvalQuery.append("     ?test a rdft:TestRDFJSONEval . ");
			positiveEvalQuery.append("     ?test mf:name ?testName . ");
			positiveEvalQuery.append("     ?test mf:action ?inputURL . ");
			positiveEvalQuery.append("     ?test mf:result ?outputURL . ");
			positiveEvalQuery.append(" }");

			try (var queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, positiveEvalQuery.toString())
					.evaluate()) {

				// Add all positive eval tests to the test suite
				while (queryResult.hasNext()) {
					BindingSet bindingSet = queryResult.next();
					String nextTestName = ((Literal) bindingSet.getValue("testName")).getLabel();
					String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString());
					String nextInputURL = TEST_FILE_BASE_PATH + nextTestFile;
					String nextOutputURL = TEST_FILE_BASE_PATH
							+ removeBase(((IRI) bindingSet.getValue("outputURL")).toString());

					String nextBaseUrl = BASE_URL + nextTestFile;

					suite.addTest(new PositiveParserTest(nextTestName, nextInputURL, nextOutputURL, nextBaseUrl));
				}
			}
		}
		repository.shutDown();

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

		private final String inputURL;

		private String outputURL;

		private final String baseURL;

		/*--------------*
		 * Constructors *
		 *--------------*/

		public PositiveParserTest(String testName, String inputURL, String outputURL, String baseURL)
				throws MalformedURLException {
			super(testName);
			this.inputURL = inputURL;
			if (outputURL != null) {
				this.outputURL = outputURL;
			}
			this.baseURL = baseURL;
		}

		/*---------*
		 * Methods *
		 *---------*/

		@Override
		protected void runTest() throws Exception {
			// Parse input data
			RDFParser rdfjsonParser = createRDFParser();

			Set<Statement> inputCollection = new LinkedHashSet<>();
			StatementCollector inputCollector = new StatementCollector(inputCollection);
			rdfjsonParser.setRDFHandler(inputCollector);

			InputStream in = this.getClass().getResourceAsStream(inputURL);
			rdfjsonParser.parse(in, baseURL);
			in.close();

			// Parse expected output data
			NTriplesParser ntriplesParser = new NTriplesParser();

			Set<Statement> outputCollection = new LinkedHashSet<>();
			StatementCollector outputCollector = new StatementCollector(outputCollection);
			ntriplesParser.setRDFHandler(outputCollector);

			if (outputURL != null) {
				// System.out.println(this.outputURL);
				//
				// NTriplesWriter nTriplesWriter = new NTriplesWriter(System.out);
				// nTriplesWriter.startRDF();
				// for(Statement nextStatment : inputCollection) {
				// nTriplesWriter.handleStatement(nextStatment);
				// }
				// nTriplesWriter.endRDF();

				in = this.getClass().getResourceAsStream(outputURL);
				ntriplesParser.parse(in, baseURL);
				in.close();

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

	/*--------------------------------*
	 * Inner class NegativeParserTest *
	 *--------------------------------*/

	private class NegativeParserTest extends TestCase {

		/*-----------*
		 * Variables *
		 *-----------*/

		private final String inputURL;

		private final String baseURL;

		/*--------------*
		 * Constructors *
		 *--------------*/

		public NegativeParserTest(String caseURI, String inputURL, String baseURL) throws MalformedURLException {
			super(caseURI);
			this.inputURL = inputURL;
			this.baseURL = baseURL;
		}

		/*---------*
		 * Methods *
		 *---------*/

		@Override
		protected void runTest() {
			try {
				// Try parsing the input; this should result in an error being
				// reported.
				RDFParser rdfjsonParser = createRDFParser();

				rdfjsonParser.setRDFHandler(new StatementCollector());

				InputStream in = this.getClass().getResourceAsStream(inputURL);
				rdfjsonParser.parse(in, baseURL);
				in.close();

				fail("Parser parses erroneous data without reporting errors");
			} catch (RDFParseException e) {
				// This is expected as the input file is incorrect RDF
			} catch (Exception e) {
				fail("Error: " + e.getMessage());
			}
		}

	} // end inner class NegativeParserTest

	/**
	 * @param baseUrl
	 * @return
	 */
	private String removeBase(String baseUrl) {
		if (baseUrl.startsWith(BASE_URL)) {
			return baseUrl.substring(BASE_URL.length());
		}

		return baseUrl;
	}

}
