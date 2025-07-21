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
package org.eclipse.rdf4j.testsuite.rio.turtle;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.parser.sparql.BaseDeclProcessor;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.testsuite.rio.FailureMode;
import org.eclipse.rdf4j.testsuite.rio.NegativeParserTest;
import org.eclipse.rdf4j.testsuite.rio.PositiveParserTest;

import junit.framework.TestSuite;

import javax.swing.text.html.parser.Parser;

/**
 * JUnit test for the Turtle 1.2 parser that uses the tests that are available
 * <a href="https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/index.html">online</a>.
 */
public abstract class Turtle12ParserTestCase {

    /*-----------*
     * Constants *
     *-----------*/

    /**
     * Base URL for W3C Tutle tests.
     */
    protected static final String TESTS_W3C_BASE_URL = "https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/";

    /**
     * Base directory for W3C Turtle tests
     */
    protected static final String TEST_W3C_FILE_BASE_PATH = "/testcases/turtle/tests-12-ttl/";

    protected static final String TEST_W3C_MANIFEST_URL = TEST_W3C_FILE_BASE_PATH + "manifest.ttl";

    protected static final String TEST_W3C_MANIFEST_URI_BASE = "https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/manifest.ttl#";

    protected static final String TEST_W3C_TEST_URI_BASE = "https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-turtle/";

    public TestSuite createTestSuite() throws Exception {
        // Create test suite
        TestSuite suite = new TestSuite(Turtle12ParserTestCase.class.getName());

        // Add the manifest for W3C test cases to a repository and query it
        Repository w3cRepository = new SailRepository(new MemoryStore());
        try (RepositoryConnection w3cCon = w3cRepository.getConnection()) {
            InputStream inputStream = this.getClass().getResourceAsStream(TEST_W3C_MANIFEST_URL);
            w3cCon.add(inputStream, TEST_W3C_MANIFEST_URI_BASE, RDFFormat.TURTLE);

            parseSubManifests(w3cCon);

            parsePositiveTurtleSyntaxTests(suite, TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL, TEST_W3C_TEST_URI_BASE,
                    w3cCon);
            parseNegativeTurtleSyntaxTests(suite, TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL, TEST_W3C_TEST_URI_BASE,
                    w3cCon);
            parsePositiveTurtleEvalTests(suite, TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL, TEST_W3C_TEST_URI_BASE,
                    w3cCon);
            parseNegativeTurtleEvalTests(suite, TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL, TEST_W3C_TEST_URI_BASE,
                    w3cCon);
        }
        w3cRepository.shutDown();

        return suite;
    }

    private void parseSubManifests (RepositoryConnection con) throws IOException {
        String manifestQuery = "PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> "
                + "SELECT DISTINCT ?manifestFile "
                + "WHERE { [] mf:include [ rdf:rest*/rdf:first ?manifestFile ] . }   ";

        TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, manifestQuery).evaluate();

        for (BindingSet bindingSet : queryResult) {
            String subManifestFile = bindingSet.getValue("manifestFile").stringValue();

            String subManifestFilePath = "";
            if (subManifestFile.startsWith(TESTS_W3C_BASE_URL)) {
                subManifestFilePath = TEST_W3C_FILE_BASE_PATH + subManifestFile.substring(TESTS_W3C_BASE_URL.length());
            }

            InputStream inputStream = this.getClass().getResourceAsStream(subManifestFilePath);
            con.add(inputStream, subManifestFile, RDFFormat.TURTLE);
        }
    }

    protected void parsePositiveTurtleSyntaxTests(TestSuite suite, String fileBasePath, String testBaseUrl,
                                                  String testLocationBaseUri, RepositoryConnection con) {
        StringBuilder positiveQuery = new StringBuilder();
        positiveQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
        positiveQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
        positiveQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
        positiveQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
        positiveQuery.append(" WHERE { \n");
        positiveQuery.append("     ?test a rdft:TestTurtlePositiveSyntax . ");
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

            String nextBaseUrl = testBaseUrl + nextTestFile;

            suite.addTest(new PositiveParserTest(nextTestUri, nextTestName, nextInputURL, null, nextBaseUrl,
                    createTurtleParser(), createNTriplesParser()));
        }

        queryResult.close();

    }

    protected void parseNegativeTurtleSyntaxTests(TestSuite suite, String fileBasePath, String testBaseUrl,
                                                  String manifestBaseUrl, RepositoryConnection con) {
        StringBuilder negativeQuery = new StringBuilder();
        negativeQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
        negativeQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
        negativeQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
        negativeQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
        negativeQuery.append(" WHERE { \n");
        negativeQuery.append("     ?test a rdft:TestTurtleNegativeSyntax . ");
        negativeQuery.append("     ?test mf:name ?testName . ");
        negativeQuery.append("     ?test mf:action ?inputURL . ");
        negativeQuery.append(" }");

        TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, negativeQuery.toString()).evaluate();

        // Add all negative parser tests to the test suite
        while (queryResult.hasNext()) {
            BindingSet bindingSet = queryResult.next();
            IRI nextTestUri = (IRI) bindingSet.getValue("test");
            String nextTestName = ((Literal) bindingSet.getValue("testName")).getLabel();
            String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString(), manifestBaseUrl);
            String nextInputURL = fileBasePath + nextTestFile;

            String nextBaseUrl = testBaseUrl + nextTestFile;

            suite.addTest(new NegativeParserTest(nextTestUri, nextTestName, nextInputURL, nextBaseUrl,
                    createTurtleParser(), FailureMode.DO_NOT_IGNORE_FAILURE));
        }

        queryResult.close();

    }

    protected void parsePositiveTurtleEvalTests(TestSuite suite, String fileBasePath, String testBaseUrl,
                                                String manifestBaseUrl, RepositoryConnection con) {
        StringBuilder positiveEvalQuery = new StringBuilder();
        positiveEvalQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
        positiveEvalQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
        positiveEvalQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
        positiveEvalQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
        positiveEvalQuery.append(" WHERE { \n");
        positiveEvalQuery.append("     ?test a rdft:TestTurtleEval . ");
        positiveEvalQuery.append("     ?test mf:name ?testName . ");
        positiveEvalQuery.append("     ?test mf:action ?inputURL . ");
        positiveEvalQuery.append("     ?test mf:result ?outputURL . ");
        positiveEvalQuery.append(" }");

        TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, positiveEvalQuery.toString())
                .evaluate();

        // Add all positive eval tests to the test suite
        while (queryResult.hasNext()) {
            BindingSet bindingSet = queryResult.next();
            IRI nextTestUri = (IRI) bindingSet.getValue("test");
            String nextTestName = ((Literal) bindingSet.getValue("testName")).getLabel();
            String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString(), manifestBaseUrl);
            String nextInputURL = fileBasePath + nextTestFile;
            String nextOutputURL = fileBasePath
                    + removeBase(((IRI) bindingSet.getValue("outputURL")).toString(), manifestBaseUrl);

            String nextBaseUrl = testBaseUrl + nextTestFile;

            suite.addTest(new PositiveParserTest(nextTestUri, nextTestName, nextInputURL, nextOutputURL, nextBaseUrl,
                    createTurtleParser(), createNTriplesParser()));
        }

        queryResult.close();
    }

    protected void parseNegativeTurtleEvalTests(TestSuite suite, String fileBasePath, String testBaseUrl,
                                                String manifestBaseUrl, RepositoryConnection con) {
        StringBuilder negativeEvalQuery = new StringBuilder();
        negativeEvalQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
        negativeEvalQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
        negativeEvalQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
        negativeEvalQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
        negativeEvalQuery.append(" WHERE { \n");
        negativeEvalQuery.append("     ?test a rdft:TestTurtleNegativeEval . ");
        negativeEvalQuery.append("     ?test mf:name ?testName . ");
        negativeEvalQuery.append("     ?test mf:action ?inputURL . ");
        negativeEvalQuery.append(" }");

        TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, negativeEvalQuery.toString())
                .evaluate();

        // Add all negative eval tests to the test suite
        while (queryResult.hasNext()) {
            BindingSet bindingSet = queryResult.next();
            IRI nextTestUri = (IRI) bindingSet.getValue("test");
            String nextTestName = ((Literal) bindingSet.getValue("testName")).getLabel();
            String nextTestFile = removeBase(((IRI) bindingSet.getValue("inputURL")).toString(), manifestBaseUrl);
            String nextInputURL = fileBasePath + nextTestFile;

            String nextBaseUrl = testBaseUrl + nextTestFile;

            suite.addTest(new NegativeParserTest(nextTestUri, nextTestName, nextInputURL, nextBaseUrl,
                    createTurtleParser(), FailureMode.DO_NOT_IGNORE_FAILURE));
        }

        queryResult.close();
    }

    /**
     * @return An implementation of a Turtle parser to test compliance with the Turtle Test Suite Turtle tests.
     */
    protected abstract RDFParser createTurtleParser();

    /**
     * @return An implementation of an N-Triples parser to test compliance with the Turtle Test Suite N-Triples tests.
     */
    protected abstract RDFParser createNTriplesParser();

    private String removeBase(String baseUrl, String redundantBaseUrl) {
        if (baseUrl.startsWith(redundantBaseUrl)) {
            return baseUrl.substring(redundantBaseUrl.length());
        }

        return baseUrl;
    }

}
