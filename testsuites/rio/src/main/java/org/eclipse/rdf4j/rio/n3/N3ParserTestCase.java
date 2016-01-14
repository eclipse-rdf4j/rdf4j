/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.n3;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestSuite;

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

/**
 * JUnit test for the N3 parser that uses the tests that are available <a
 * href="http://www.w3.org/2000/10/swap/test/n3parser.tests">online</a>.
 */
public abstract class N3ParserTestCase {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static String BASE_URL = "http://www.w3.org/2000/10/swap/test/";

	private static String MANIFEST_URL = "http://www.w3.org/2000/10/swap/test/n3parser.tests";

	/*--------------------*
	 * Static initializer *
	 *--------------------*/

	public TestSuite createTestSuite()
		throws Exception
	{
		// Create test suite
		TestSuite suite = new TestSuite(N3ParserTestCase.class.getName());

		// Add the manifest to a repository and query it
		Repository repository = new SailRepository(new MemoryStore());
		repository.initialize();
		RepositoryConnection con = repository.getConnection();

		URL url = url(MANIFEST_URL);
		con.add(url, base(MANIFEST_URL), RDFFormat.TURTLE);

		// Add all positive parser tests to the test suite
		String query = "SELECT testURI, inputURL, outputURL "
				+ "FROM {testURI} rdf:type {n3test:PositiveParserTest}; "
				+ "               n3test:inputDocument {inputURL}; "
				+ "               n3test:outputDocument {outputURL} "
				+ "USING NAMESPACE n3test = <http://www.w3.org/2004/11/n3test#>";

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SERQL, query).evaluate();
		while(queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			String testURI = bindingSet.getValue("testURI").toString();
			String inputURL = bindingSet.getValue("inputURL").toString();
			String outputURL = bindingSet.getValue("outputURL").toString();

			suite.addTest(new PositiveParserTest(testURI, inputURL, outputURL));
		}

		queryResult.close();

		// Add all negative parser tests to the test suite
		query = "SELECT testURI, inputURL "
				+ "FROM {testURI} rdf:type {n3test:NegativeParserTest}; "
				+ "               n3test:inputDocument {inputURL} "
				+ "USING NAMESPACE n3test = <http://www.w3.org/2004/11/n3test#>";

		queryResult = con.prepareTupleQuery(QueryLanguage.SERQL, query).evaluate();

		while(queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			String testURI = bindingSet.getValue("testURI").toString();
			String inputURL = bindingSet.getValue("inputURL").toString();

			suite.addTest(new NegativeParserTest(testURI, inputURL));
		}
		queryResult.close();
		con.close();
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

		private URL inputURL;

		private URL outputURL;

		/*--------------*
		 * Constructors *
		 *--------------*/

		public PositiveParserTest(String testURI, String inputURL, String outputURL)
			throws MalformedURLException
		{
			super(testURI);
			this.inputURL = url(inputURL);
			this.outputURL = url(outputURL);
		}

		/*---------*
		 * Methods *
		 *---------*/

		@Override
		protected void runTest()
			throws Exception
		{
			// Parse input data
			RDFParser turtleParser = createRDFParser();
			turtleParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

			Set<Statement> inputCollection = new LinkedHashSet<Statement>();
			StatementCollector inputCollector = new StatementCollector(inputCollection);
			turtleParser.setRDFHandler(inputCollector);

			InputStream in = inputURL.openStream();
			turtleParser.parse(in, base(inputURL.toExternalForm()));
			in.close();

			// Parse expected output data
			NTriplesParser ntriplesParser = new NTriplesParser();
			ntriplesParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

			Set<Statement> outputCollection = new LinkedHashSet<Statement>();
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

				List<Statement> missing = new LinkedList<Statement>(outputCollection);
				missing.removeAll(inputCollection);

				List<Statement> unexpected = new LinkedList<Statement>(inputCollection);
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

		private URL inputURL;

		/*--------------*
		 * Constructors *
		 *--------------*/

		public NegativeParserTest(String testURI, String inputURL)
			throws MalformedURLException
		{
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
				turtleParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

				turtleParser.setRDFHandler(new StatementCollector());

				InputStream in = inputURL.openStream();
				turtleParser.parse(in, base(inputURL.toExternalForm()));
				in.close();

				fail("Parser parses erroneous data without reporting errors");
			}
			catch (RDFParseException e) {
				// This is expected as the input file is incorrect RDF
			}
			catch (Exception e) {
				fail("Error: " + e.getMessage());
			}
		}

	} // end inner class NegativeParserTest

	private static URL url(String uri)
			throws MalformedURLException {
		if (!uri.startsWith("injar:"))
			return new URL(uri);
		int start = uri.indexOf(':') + 3;
		int end = uri.indexOf('/', start);
		String encoded = uri.substring(start, end);
		try {
			String jar = URLDecoder.decode(encoded, "UTF-8");
			return new URL("jar:" + jar + '!' + uri.substring(end));
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private static String base(String uri) {
		if (!uri.startsWith("jar:"))
			return uri;
		int start = uri.indexOf(':') + 1;
		int end = uri.lastIndexOf('!');
		String jar = uri.substring(start, end);
		try {
			String encoded = URLEncoder.encode(jar, "UTF-8");
			return "injar://" + encoded + uri.substring(end + 1);
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
}
