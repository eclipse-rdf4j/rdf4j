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
package org.eclipse.rdf4j.testsuite.rio.ntriples;

import java.io.InputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.testsuite.rio.FailureMode;
import org.eclipse.rdf4j.testsuite.rio.NegativeParserTest;
import org.eclipse.rdf4j.testsuite.rio.PositiveParserTest;

import junit.framework.TestSuite;

/**
 * JUnit test for the N-Triples parser that uses the tests that are available
 * <a href="http://www.w3.org/2013/N-TriplesTests/">online</a>.
 */
public abstract class AbstractNTriplesParserTest {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Base directory for W3C N-Triples tests
	 */
	private static final String TEST_W3C_FILE_BASE_PATH = "/testcases/ntriples/";

	private static final String TEST_W3C_MANIFEST_URL = TEST_W3C_FILE_BASE_PATH + "manifest.ttl";

	private static final String TEST_W3C_MANIFEST_URI_BASE = "http://www.w3.org/2013/N-TriplesTests/manifest.ttl#";

	private static final String TEST_W3C_TEST_URI_BASE = "http://www.w3.org/2013/N-TriplesTests/";

	/*---------*
	 * Methods *
	 *---------*/

	public TestSuite createTestSuite() throws Exception {
		// Create test suite
		TestSuite suite = new TestSuite(this.getClass().getName());

		// Add the manifest for W3C test cases to a repository and query it
		Repository w3cRepository = new SailRepository(new MemoryStore());
		try (RepositoryConnection w3cCon = w3cRepository.getConnection()) {
			InputStream inputStream = this.getClass().getResourceAsStream(TEST_W3C_MANIFEST_URL);
			w3cCon.add(inputStream, TEST_W3C_MANIFEST_URI_BASE, RDFFormat.TURTLE);

			parsePositiveNTriplesSyntaxTests(suite, TEST_W3C_FILE_BASE_PATH, TEST_W3C_TEST_URI_BASE, w3cCon);
			parseNegativeNTriplesSyntaxTests(suite, TEST_W3C_FILE_BASE_PATH, TEST_W3C_TEST_URI_BASE, w3cCon);
		}
		w3cRepository.shutDown();

		return suite;
	}

	private void parsePositiveNTriplesSyntaxTests(TestSuite suite, String fileBasePath, String testLocationBaseUri,
			RepositoryConnection con) throws Exception {
		StringBuilder positiveQuery = new StringBuilder();
		positiveQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		positiveQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		positiveQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		positiveQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		positiveQuery.append(" WHERE { \n");
		positiveQuery.append("     ?test a rdft:TestNTriplesPositiveSyntax . ");
		positiveQuery.append("     ?test mf:name ?testName . ");
		positiveQuery.append("     ?test mf:action ?inputURL . ");
		positiveQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, positiveQuery.toString()).evaluate();

		// Add all positive parser tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			IRI nextTestUri = (IRI) bindingSet.getValue("test");
			String nextTestName = ((Literal) bindingSet.getValue("testName")).getLabel();
			String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString(), testLocationBaseUri);
			String nextInputURL = fileBasePath + nextTestFile;

			String nextBaseUrl = testLocationBaseUri + nextTestFile;

			suite.addTest(new PositiveParserTest(nextTestUri, nextTestName, nextInputURL, null, nextBaseUrl,
					createRDFParser(), createRDFParser()));
		}

		queryResult.close();

	}

	private void parseNegativeNTriplesSyntaxTests(TestSuite suite, String fileBasePath, String testLocationBaseUri,
			RepositoryConnection con) throws Exception {
		StringBuilder negativeQuery = new StringBuilder();
		negativeQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		negativeQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		negativeQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		negativeQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		negativeQuery.append(" WHERE { \n");
		negativeQuery.append("     ?test a rdft:TestNTriplesNegativeSyntax . ");
		negativeQuery.append("     ?test mf:name ?testName . ");
		negativeQuery.append("     ?test mf:action ?inputURL . ");
		negativeQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, negativeQuery.toString()).evaluate();

		// Add all negative parser tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			IRI nextTestUri = (IRI) bindingSet.getValue("test");
			String nextTestName = ((Literal) bindingSet.getValue("testName")).toString();
			String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString(), testLocationBaseUri);
			String nextInputURL = fileBasePath + nextTestFile;

			String nextBaseUrl = testLocationBaseUri + nextTestFile;

			suite.addTest(new NegativeParserTest(nextTestUri, nextTestName, nextInputURL, nextBaseUrl,
					createRDFParser(), FailureMode.DO_NOT_IGNORE_FAILURE));
		}

		queryResult.close();

	}

	protected abstract RDFParser createRDFParser();

	private String removeBase(String baseUrl, String redundantBaseUrl) {
		if (baseUrl.startsWith(redundantBaseUrl)) {
			return baseUrl.substring(redundantBaseUrl.length());
		}

		return baseUrl;
	}
}
