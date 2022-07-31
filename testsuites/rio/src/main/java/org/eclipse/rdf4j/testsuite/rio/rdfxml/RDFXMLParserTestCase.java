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
package org.eclipse.rdf4j.testsuite.rio.rdfxml;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.xml.sax.SAXException;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit test for the RDF/XML parser that uses the test manifest that is available
 * <a href="http://www.w3.org/2000/10/rdf-tests/rdfcore/Manifest.rdf">online</a>.
 */
public abstract class RDFXMLParserTestCase {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String W3C_TESTS_DIR = "http://www.w3.org/2000/10/rdf-tests/rdfcore/";

	private static final String LOCAL_TESTS_DIR = "/testcases/rdfxml/";

	private static final String W3C_MANIFEST_FILE = W3C_TESTS_DIR + "Manifest.rdf";

	/*--------------------*
	 * Static initializer *
	 *--------------------*/

	public TestSuite createTestSuite() throws Exception {
		// Create an RDF repository for the manifest data
		Repository repository = new SailRepository(new MemoryStore());
		// Create test suite
		TestSuite suite = new TestSuite(RDFXMLParserTestCase.class.getName());

		try (RepositoryConnection con = repository.getConnection()) {

			// Add W3C's manifest
			URL w3cManifest = resolveURL(W3C_MANIFEST_FILE);
			con.add(w3cManifest, base(W3C_MANIFEST_FILE), RDFFormat.RDFXML);

			// Add all positive parser tests
			String query = ""
					+ "PREFIX test: <http://www.w3.org/2000/10/rdf-tests/rdfcore/testSchema#>"
					+ " SELECT ?TESTCASE ?INPUT ?OUTPUT "
					+ " WHERE { ?TESTCASE a test:PositiveParserTest; "
					+ "                test:inputDocument ?INPUT; "
					+ "                test:outputDocument ?OUTPUT; "
					+ "                test:status \"APPROVED\" .} ";

			try (var queryResult = con.prepareTupleQuery(query).evaluate()) {
				while (queryResult.hasNext()) {
					BindingSet bindingSet = queryResult.next();
					String caseURI = bindingSet.getValue("TESTCASE").toString();
					String inputURL = bindingSet.getValue("INPUT").toString();
					String outputURL = bindingSet.getValue("OUTPUT").toString();
					suite.addTest(new PositiveParserTest(caseURI, inputURL, outputURL));
				}
			}
			// Add all negative parser tests
			query = ""
					+ "PREFIX test: <http://www.w3.org/2000/10/rdf-tests/rdfcore/testSchema#>"
					+ " SELECT ?TESTCASE ?INPUT ?OUTPUT "
					+ " WHERE { ?TESTCASE a test:NegativeParserTest; "
					+ "                test:inputDocument ?INPUT; "
					+ "                test:outputDocument ?OUTPUT; "
					+ "                test:status \"APPROVED\" .} ";
			try (var queryResult = con.prepareTupleQuery(query).evaluate()) {
				while (queryResult.hasNext()) {
					BindingSet bindingSet = queryResult.next();
					String caseURI = bindingSet.getValue("TESTCASE").toString();
					String inputURL = bindingSet.getValue("INPUT").toString();
					suite.addTest(new NegativeParserTest(caseURI, inputURL));
				}
			}
		}
		repository.shutDown();

		return suite;
	}

	private static URL resolveURL(String urlString) throws MalformedURLException {
		if (urlString.startsWith(W3C_TESTS_DIR)) {
			// resolve to local copy
			urlString = LOCAL_TESTS_DIR + "w3c-approved/" + urlString.substring(W3C_TESTS_DIR.length());
		}

		if (urlString.startsWith("/")) {
			return RDFXMLParserTestCase.class.getResource(urlString);
		} else {
			return url(urlString);
		}
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

		private final String outputURL;

		/*--------------*
		 * Constructors *
		 *--------------*/

		public PositiveParserTest(String caseURI, String inputURL, String outputURL) {
			super(caseURI);
			this.inputURL = inputURL;
			this.outputURL = outputURL;
		}

		/*---------*
		 * Methods *
		 *---------*/

		@Override
		protected void runTest() throws Exception {
			// Parse input data
			RDFParser rdfxmlParser = createRDFParser();
			rdfxmlParser.setValueFactory(new CanonXMLValueFactory());
			rdfxmlParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);
			rdfxmlParser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);

			Set<Statement> inputCollection = new LinkedHashSet<>();
			StatementCollector inputCollector = new StatementCollector(inputCollection);
			rdfxmlParser.setRDFHandler(inputCollector);

			try (InputStream in = resolveURL(inputURL).openStream()) {
				rdfxmlParser.parse(in, base(inputURL));
			}

			// Parse expected output data
			NTriplesParser ntriplesParser = new NTriplesParser();
			ntriplesParser.setValueFactory(new CanonXMLValueFactory());
			ntriplesParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);
			ntriplesParser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);

			Set<Statement> outputCollection = new LinkedHashSet<>();
			StatementCollector outputCollector = new StatementCollector(outputCollection);
			ntriplesParser.setRDFHandler(outputCollector);

			try (InputStream in = resolveURL(outputURL).openStream()) {
				ntriplesParser.parse(in, base(inputURL));
			}

			// Check equality of the two models
			if (!Models.isomorphic(inputCollection, outputCollection)) {

				String expected = outputCollection.stream().map(Objects::toString).collect(Collectors.joining());
				String actual = inputCollection.stream().map(Objects::toString).collect(Collectors.joining("\n"));

				assertEquals(expected, actual);

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

		/*--------------*
		 * Constructors *
		 *--------------*/

		public NegativeParserTest(String caseURI, String inputURL) {
			super(caseURI);
			this.inputURL = inputURL;
		}

		/*---------*
		 * Methods *
		 *---------*/

		@Override
		protected void runTest() {
			try {
				// Try parsing the input; this should result in an error being
				// reported.
				RDFParser rdfxmlParser = createRDFParser();
				rdfxmlParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
				rdfxmlParser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
				rdfxmlParser.getParserConfig().set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, true);

				rdfxmlParser.setRDFHandler(new StatementCollector());

				InputStream in = resolveURL(inputURL).openStream();
				rdfxmlParser.parse(in, base(inputURL));
				in.close();

				fail("Parser parses erroneous data without reporting errors");
			} catch (RDFParseException e) {
				// This is expected as the input file is incorrect RDF
			} catch (Exception e) {
				fail("Error: " + e.getMessage());
			}
		}

	} // end inner class NegativeParserTest

	private static class CanonXMLValueFactory extends SimpleValueFactory {

		private final Canonicalizer c14n;

		public CanonXMLValueFactory() throws InvalidCanonicalizerException, ParserConfigurationException {
			org.apache.xml.security.Init.init();

			c14n = Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
		}

		@Override
		public Literal createLiteral(String value, IRI datatype) {
			if (RDF.XMLLITERAL.equals(datatype)) {
				value = canonicalizeXmlLiteral(value);
			}

			return super.createLiteral(value, datatype);
		}

		@Override
		public Literal createLiteral(String value, CoreDatatype datatype) {
			if (datatype == CoreDatatype.RDF.XMLLITERAL) {
				value = canonicalizeXmlLiteral(value);
			}

			return super.createLiteral(value, datatype);
		}

		@Override
		public Literal createLiteral(String value, IRI datatype, CoreDatatype coreDatatype) {
			if (coreDatatype == CoreDatatype.RDF.XMLLITERAL) {
				assert RDF.XMLLITERAL.equals(datatype);
				value = canonicalizeXmlLiteral(value);
			} else {
				assert !RDF.XMLLITERAL.equals(datatype);
			}

			return super.createLiteral(value, datatype, coreDatatype);
		}

		private String canonicalizeXmlLiteral(String value) {
			// Canonicalize the literal value
			try {
				return new String(c14n.canonicalize(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
			} catch (ParserConfigurationException | IOException e) {
				throw new RuntimeException(e);
			} catch (CanonicalizationException | SAXException e) {
				return value;
			}
		}

	}

	private static URL url(String uri) throws MalformedURLException {
		return new URL(uri);
	}

	private static String base(String uri) {
		return uri;
	}
}
