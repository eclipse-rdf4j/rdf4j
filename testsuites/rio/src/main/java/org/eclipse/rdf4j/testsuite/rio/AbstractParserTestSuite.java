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

import java.io.IOException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestSuite;

public abstract class AbstractParserTestSuite {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final String testFileBasePath;

	private final String testManifestURL;

	private final String testManifestURIBase;

	private final String testBaseURL;

	private final RDFFormat format;

	private final String formatString;

	private final Logger logger = LoggerFactory.getLogger(AbstractParserTestSuite.class);

	protected AbstractParserTestSuite(final String testFileBasePath, final String testBaseURL, final RDFFormat format,
			final String formatString) {
		this.testFileBasePath = testFileBasePath;
		this.testBaseURL = testBaseURL;
		testManifestURL = testFileBasePath + "manifest.ttl";
		testManifestURIBase = testBaseURL + "manifest.ttl#";
		this.formatString = formatString;
		this.format = format;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public TestSuite createTestSuite() throws Exception {
		// Create test suite
		TestSuite suite = new TestSuite(this.getClass().getName());

		// Add the manifest for W3C test cases to a repository and query it
		Repository w3cRepository = new SailRepository(new MemoryStore());
		try (RepositoryConnection w3cCon = w3cRepository.getConnection()) {
			InputStream inputStream = this.getClass().getResourceAsStream(testManifestURL);
			w3cCon.add(inputStream, testManifestURIBase, RDFFormat.TURTLE);

			parseSubManifests(w3cCon);

			parsePositiveSyntaxTests(suite, w3cCon);
			parseNegativeSyntaxTests(suite, w3cCon);
			parseCanonicalizationTests(suite, w3cCon);
			parsePositiveEvalTests(suite, w3cCon);
			parseNegativeEvalTests(suite, w3cCon);
		}
		w3cRepository.shutDown();

		return suite;
	}

	private void parseSubManifests(RepositoryConnection con) throws IOException {
		final String manifestQuery = "PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> "
				+ "SELECT DISTINCT ?manifestFile "
				+ "WHERE { [] mf:include [ rdf:rest*/rdf:first ?manifestFile ] . }   ";

		final TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, manifestQuery).evaluate();

		for (final BindingSet bindingSet : queryResult) {
			final String subManifestFile = bindingSet.getValue("manifestFile").stringValue();

			String subManifestFilePath = "";
			if (subManifestFile.startsWith(testBaseURL)) {
				final String relativePath = subManifestFile.substring(testBaseURL.length());
				subManifestFilePath = testFileBasePath + relativePath;
			}

			final InputStream inputStream = this.getClass().getResourceAsStream(subManifestFilePath);

			con.add(inputStream, subManifestFile, RDFFormat.TURTLE);
		}
	}

	private void parsePositiveSyntaxTests(final TestSuite suite, final RepositoryConnection con) {
		StringBuilder positiveQuery = new StringBuilder();
		positiveQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		positiveQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		positiveQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		positiveQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		positiveQuery.append(" WHERE { \n");
		positiveQuery.append("     ?test a rdft:Test" + formatString + "PositiveSyntax . ");
		positiveQuery.append("     ?test mf:name ?testName . ");
		positiveQuery.append("     ?test mf:action ?inputURL . ");
		positiveQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, positiveQuery.toString()).evaluate();

		// Add all positive parser tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			IRI nextTestUri = (IRI) bindingSet.getValue("test");
			String nextTestName = ((Literal) bindingSet.getValue("testName")).getLabel();
			String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString(), testBaseURL);
			String nextInputURL = testFileBasePath + nextTestFile;

			String nextBaseUrl = testBaseURL + nextTestFile;

			suite.addTest(new PositiveParserTest(nextTestUri, nextTestName, nextInputURL, null, nextBaseUrl,
					createRDFParser(), createRDFParser()));
		}

		queryResult.close();

	}

	private void parseNegativeSyntaxTests(TestSuite suite, RepositoryConnection con) {
		StringBuilder negativeQuery = new StringBuilder();
		negativeQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		negativeQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		negativeQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		negativeQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		negativeQuery.append(" WHERE { \n");
		negativeQuery.append("     ?test a rdft:Test" + formatString + "NegativeSyntax . ");
		negativeQuery.append("     ?test mf:name ?testName . ");
		negativeQuery.append("     ?test mf:action ?inputURL . ");
		negativeQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, negativeQuery.toString()).evaluate();

		// Add all negative parser tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			IRI nextTestUri = (IRI) bindingSet.getValue("test");
			String nextTestName = ((Literal) bindingSet.getValue("testName")).toString();
			String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString(), testBaseURL);
			String nextInputURL = testFileBasePath + nextTestFile;

			String nextBaseUrl = testBaseURL + nextTestFile;

			suite.addTest(new NegativeParserTest(nextTestUri, nextTestName, nextInputURL, nextBaseUrl,
					createRDFParser(), FailureMode.DO_NOT_IGNORE_FAILURE));
		}

		queryResult.close();

	}

	private void parseCanonicalizationTests(TestSuite suite, RepositoryConnection con) {
		StringBuilder canonicalizationQuery = new StringBuilder();
		canonicalizationQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		canonicalizationQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		canonicalizationQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		canonicalizationQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		canonicalizationQuery.append(" WHERE { \n");
		canonicalizationQuery.append("     ?test a rdft:Test" + formatString + "PositiveC14N . ");
		canonicalizationQuery.append("     ?test mf:name ?testName . ");
		canonicalizationQuery.append("     ?test mf:action ?inputURL . ");
		canonicalizationQuery.append("     ?test mf:result ?outputURL . ");
		canonicalizationQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, canonicalizationQuery.toString())
				.evaluate();

		// Add all canonicalization tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			IRI nextTestUri = (IRI) bindingSet.getValue("test");
			String nextTestName = ((Literal) bindingSet.getValue("testName")).getLabel();
			String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString(), testBaseURL);
			String nextInputURL = testFileBasePath + nextTestFile;
			String nextOutputURL = testFileBasePath
					+ removeBase(((IRI) bindingSet.getValue("outputURL")).toString(), testBaseURL);

			String nextBaseUrl = testBaseURL + nextTestFile;

			suite.addTest(new CanonicalizationTest(nextTestUri, nextTestName, nextInputURL, nextOutputURL, nextBaseUrl,
					createRDFParser(), format));
		}

		queryResult.close();

	}

	private void parsePositiveEvalTests(TestSuite suite, RepositoryConnection con) {
		StringBuilder positiveEvalQuery = new StringBuilder();
		positiveEvalQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		positiveEvalQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		positiveEvalQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		positiveEvalQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		positiveEvalQuery.append(" WHERE { \n");
		positiveEvalQuery.append("     ?test a rdft:Test" + formatString + "Eval . ");
		positiveEvalQuery.append("     ?test mf:name ?testName . ");
		positiveEvalQuery.append("     ?test mf:action ?inputURL . ");
		positiveEvalQuery.append("     ?test mf:result ?outputURL . ");
		positiveEvalQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, positiveEvalQuery.toString())
				.evaluate();

		// Add all canonicalization tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			IRI nextTestUri = (IRI) bindingSet.getValue("test");
			String nextTestName = ((Literal) bindingSet.getValue("testName")).getLabel();
			String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString(), testBaseURL);
			String nextInputURL = testFileBasePath + nextTestFile;
			String nextOutputURL = testFileBasePath
					+ removeBase(((IRI) bindingSet.getValue("outputURL")).toString(), testBaseURL);

			String nextBaseUrl = testBaseURL + nextTestFile;

			if (nextTestName.contains("CARRIAGE_RETURN")) {
				// FIXME: RDF4J seems not to preserve the CARRIAGE_RETURN character
				// right now
				logger.warn("Ignoring TriG Positive Parser Eval Test: " + nextInputURL);
				continue;
			} else if (nextTestName.contains("UTF8_boundaries")
					|| nextTestName.contains("PN_CHARS_BASE_character_boundaries")) {
				// FIXME: UTF8 support not implemented yet
				logger.warn("Ignoring TriG Positive Parser Eval Test: " + nextInputURL);
				continue;
			}

			suite.addTest(new PositiveParserTest(nextTestUri, nextTestName, nextInputURL, nextOutputURL, nextBaseUrl,
					createRDFParser(), createRDFBaseParser()));
		}

		queryResult.close();

	}

	private void parseNegativeEvalTests(TestSuite suite, RepositoryConnection con) {
		StringBuilder negativeEvalQuery = new StringBuilder();
		negativeEvalQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		negativeEvalQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		negativeEvalQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		negativeEvalQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		negativeEvalQuery.append(" WHERE { \n");
		negativeEvalQuery.append("     ?test a rdft:Test" + formatString + "NegativeEval . ");
		negativeEvalQuery.append("     ?test mf:name ?testName . ");
		negativeEvalQuery.append("     ?test mf:action ?inputURL . ");
		negativeEvalQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, negativeEvalQuery.toString())
				.evaluate();

		// Add all canonicalization tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			IRI nextTestUri = (IRI) bindingSet.getValue("test");
			String nextTestName = ((Literal) bindingSet.getValue("testName")).getLabel();
			String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString(), testBaseURL);
			String nextInputURL = testFileBasePath + nextTestFile;

			String nextBaseUrl = testBaseURL + nextTestFile;

			suite.addTest(new NegativeParserTest(nextTestUri, nextTestName, nextInputURL, nextBaseUrl,
					createRDFParser(), FailureMode.DO_NOT_IGNORE_FAILURE));
		}

		queryResult.close();

	}

	protected abstract RDFParser createRDFParser();

	protected RDFParser createRDFBaseParser() {
		return null;
	}

	private String removeBase(final String baseUrl, final String redundantBaseUrl) {
		if (baseUrl.startsWith(redundantBaseUrl)) {
			return baseUrl.substring(redundantBaseUrl.length());
		}

		return baseUrl;
	}
}
