/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public abstract class SeRQLParserTestCase extends TestCase {

	static final Logger logger = LoggerFactory.getLogger(SeRQLParserTestCase.class);

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String MANIFEST_FILE = "/testcases/SeRQL/syntax/manifest.ttl";

	/* Variables */

	private String queryFile;

	private Value result;

	/* constants */

	private static String MFX = "http://www.openrdf.org/test-manifest-extensions#";

	private static IRI MFX_CORRECT = SimpleValueFactory.getInstance().createIRI(MFX + "Correct");

	private static IRI MFX_PARSE_ERROR =  SimpleValueFactory.getInstance().createIRI(MFX + "ParseError");

	/* Constructors */

	public interface Factory {

		Test createTest(String name, String queryFile, Value result);
	}

	/**
	 * Creates a new SeRQL Parser test.
	 */
	public SeRQLParserTestCase(String name, String queryFile, Value result) {
		super(name);

		this.queryFile = queryFile;

		if (!(MFX_CORRECT.equals(result) || MFX_PARSE_ERROR.equals(result))) {
			throw new IllegalArgumentException("unknown result type: " + result);
		}
		this.result = result;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void runTest()
		throws Exception
	{
		// Read query from file
		InputStream stream = url(queryFile).openStream();
		String query = IOUtil.readString(new InputStreamReader(stream, "UTF-8"));
		stream.close();

		try {
			QueryParser parser = createParser();
			parser.parseQuery(query, null);
			if (MFX_PARSE_ERROR.equals(result)) {
				fail("Negative syntax test failed. Malformed query caused no error.");
			}
		}
		catch (MalformedQueryException e) {
			if (MFX_CORRECT.equals(result)) {
				fail("Positive syntax test failed: " + e.getMessage());
			}
			else {
				return;
			}
		}
	}

	protected abstract QueryParser createParser();

	/*--------------*
	 * Test methods *
	 *--------------*/

	public static Test suite(Factory factory)
		throws Exception
	{
		TestSuite suite = new TestSuite();
		suite.setName("SeRQL Syntax Tests");

		TestSuite positiveTests = new TestSuite();
		positiveTests.setName("Positive Syntax Tests");

		TestSuite negativeTests = new TestSuite();
		negativeTests.setName("Negative Syntax Tests");

		// Read manifest and create declared test cases
		Repository manifestRep = new SailRepository(new MemoryStore());
		manifestRep.initialize();
		RepositoryConnection con = manifestRep.getConnection();

		URL manifestURL = SeRQLParserTestCase.class.getResource(MANIFEST_FILE);
		RDFFormat format = Rio.getParserFormatForFileName(MANIFEST_FILE).orElse(RDFFormat.TURTLE);
		con.add(manifestURL, base(manifestURL.toExternalForm()), format);

		String query = "SELECT testName, query, result " + "FROM {} mf:name {testName}; "
				+ "        mf:action {query}; " + "        mf:result {result} " + "USING NAMESPACE "
				+ "  mf = <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>, "
				+ "  mfx = <http://www.openrdf.org/test-manifest-extensions#>, "
				+ "  qt = <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>";

		TupleQueryResult tests = con.prepareTupleQuery(QueryLanguage.SERQL, query).evaluate();
		while (tests.hasNext()) {
			BindingSet testBindings = tests.next();
			String testName = testBindings.getValue("testName").toString();
			String queryFile = testBindings.getValue("query").toString();
			Value result = testBindings.getValue("result");
			if (MFX_CORRECT.equals(result)) {
				positiveTests.addTest(factory.createTest(testName, queryFile, result));
			}
			else if (MFX_PARSE_ERROR.equals(result)) {
				negativeTests.addTest(factory.createTest(testName, queryFile, result));
			}
			else {
				logger.warn("Unexpected result value for test \"" + testName + "\": " + result);
			}
		}

		tests.close();
		con.close();
		manifestRep.shutDown();

		suite.addTest(positiveTests);
		suite.addTest(negativeTests);
		return suite;
	}

	private static URL url(String uri)
		throws MalformedURLException
	{
		if (!uri.startsWith("injar:"))
			return new URL(uri);
		int start = uri.indexOf(':') + 3;
		int end = uri.indexOf('/', start);
		String encoded = uri.substring(start, end);
		try {
			String jar = URLDecoder.decode(encoded, "UTF-8");
			return new URL("jar:" + jar + '!' + uri.substring(end));
		}
		catch (UnsupportedEncodingException e) {
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
		}
		catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
}
