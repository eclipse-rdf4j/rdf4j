/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.manifest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarFile;

import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.common.io.ZipUtil;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Functionality for creating a JUnit test suite out of a W3C Working
 * Group-style manifest for SPARQL query and update tests.
 * 
 * @author Jeen Broekstra
 */
public class SPARQL11ManifestTest {

	static final Logger logger = LoggerFactory.getLogger(SPARQL11ManifestTest.class);

	private static File tmpDir;

	/**
	 * Creates a new {@link TestSuite} for executiong of {@link SPARQLQueryTest}
	 * s.
	 * 
	 * @param factory
	 *        a factory class that creates each individual test case.
	 * @param officialWorkingGroupTests
	 *        indicates whether to use the official W3C working group tests, or
	 *        Sesame's own set of tests.
	 * @param approvedTestsOnly
	 *        if <code>true</code>, use working group-approved tests only. Has no
	 *        influence when officialWorkingGroup tests is set to
	 *        <code>false</code>.
	 * @param useRemoteTests
	 *        if set to <code>true</code>, use manifests and tests located at
	 *        <code>http://www.w3.org/2009/sparql/docs/tests/data-sparql11/</code>
	 *        , instead of local copies.
	 * @param excludedSubdirs
	 *        an (optionally empty) list of subdirectories to exclude from
	 *        testing. If specified, test cases in one of the supplied subdirs
	 *        will not be executed. If left empty, all tests will be executed.
	 * @return a TestSuite.
	 * @throws Exception
	 */
	public static TestSuite suite(SPARQLQueryTest.Factory factory, boolean officialWorkingGroupTests,
			boolean approvedTestsOnly, boolean useRemoteTests, String... excludedSubdirs)
		throws Exception
	{
		final String manifestFile = getManifestFile(officialWorkingGroupTests, useRemoteTests);

		TestSuite suite = new TestSuite(factory.getClass().getName()) {

			@Override
			public void run(TestResult result) {
				try {
					super.run(result);
				}
				finally {
					if (tmpDir != null) {
						try {
							FileUtil.deleteDir(tmpDir);
						}
						catch (IOException e) {
							System.err.println("Unable to clean up temporary directory '" + tmpDir + "': "
									+ e.getMessage());
						}
					}
				}
			}
		};

		Repository manifestRep = new SailRepository(new MemoryStore());
		manifestRep.initialize();
		RepositoryConnection con = manifestRep.getConnection();

		addTurtle(con, new URL(manifestFile), manifestFile);

		String query = " PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> "
				+ "PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> "
				+ "SELECT DISTINCT ?manifestFile "
				+ "WHERE { [] mf:include [ rdf:rest*/rdf:first ?manifestFile ] . }   ";

		TupleQueryResult manifestResults = con.prepareTupleQuery(QueryLanguage.SPARQL, query, manifestFile).evaluate();

		while (manifestResults.hasNext()) {
			BindingSet bindingSet = manifestResults.next();
			String subManifestFile = bindingSet.getValue("manifestFile").stringValue();

			if (includeSubManifest(subManifestFile, excludedSubdirs)) {
				suite.addTest(SPARQLQueryTest.suite(subManifestFile, factory, approvedTestsOnly));
			}
		}

		manifestResults.close();
		con.close();
		manifestRep.shutDown();

		logger.info("Created aggregated test suite with " + suite.countTestCases() + " test cases.");
		return suite;
	}

	public static TestSuite suite(SPARQLUpdateConformanceTest.Factory factory,
			boolean officialWorkingGroupTests, boolean approvedTestsOnly, boolean useRemote,
			String... excludedSubdirs)
		throws Exception
	{
		final String manifestFile = getManifestFile(officialWorkingGroupTests, useRemote);

		TestSuite suite = new TestSuite(factory.getClass().getName()) {

			@Override
			public void run(TestResult result) {
				try {
					super.run(result);
				}
				finally {
					if (tmpDir != null) {
						try {
							FileUtil.deleteDir(tmpDir);
						}
						catch (IOException e) {
							System.err.println("Unable to clean up temporary directory '" + tmpDir + "': "
									+ e.getMessage());
						}
					}
				}
			}
		};

		Repository manifestRep = new SailRepository(new MemoryStore());
		manifestRep.initialize();
		RepositoryConnection con = manifestRep.getConnection();

		addTurtle(con, new URL(manifestFile), manifestFile);

		String query = " PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> "
				+ "PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> "
				+ "SELECT DISTINCT ?manifestFile "
				+ "WHERE { [] mf:include [ rdf:rest*/rdf:first ?manifestFile ] . }   ";
		
		TupleQueryResult manifestResults = con.prepareTupleQuery(QueryLanguage.SPARQL, query, manifestFile).evaluate();

		while (manifestResults.hasNext()) {
			BindingSet bindingSet = manifestResults.next();
			String subManifestFile = bindingSet.getValue("manifestFile").stringValue();

			if (includeSubManifest(subManifestFile, excludedSubdirs)) {
				suite.addTest(SPARQLUpdateConformanceTest.suite(subManifestFile, factory, approvedTestsOnly));
			}
		}

		manifestResults.close();
		con.close();
		manifestRep.shutDown();

		logger.info("Created aggregated test suite with " + suite.countTestCases() + " test cases.");
		return suite;
	}

	private static String getManifestFile(boolean officialWorkingGroupTests, boolean useRemote) {
		String manifestFile = null;
		if (useRemote) {
			manifestFile = "http://www.w3.org/2009/sparql/docs/tests/data-sparql11/manifest-all.ttl";
		}
		else {
			URL url = null;
			if (officialWorkingGroupTests) {
				url = SPARQL11ManifestTest.class.getResource("/testcases-sparql-1.1-w3c/manifest-all.ttl");
			}
			else {
				url = SPARQL11ManifestTest.class.getResource("/testcases-sparql-1.1/manifest-evaluation.ttl");
			}

			if ("jar".equals(url.getProtocol())) {
				// Extract manifest files to a temporary directory
				try {
					tmpDir = FileUtil.createTempDir("sparql11-test-evaluation");

					JarURLConnection con = (JarURLConnection)url.openConnection();
					JarFile jar = con.getJarFile();

					ZipUtil.extract(jar, tmpDir);

					File localFile = new File(tmpDir, con.getEntryName());
					manifestFile = localFile.toURI().toURL().toString();
				}
				catch (IOException e) {
					throw new AssertionError(e);
				}
			}
			else {
				manifestFile = url.toString();
			}
		}
		return manifestFile;
	}

	/**
	 * Verifies if the selected subManifest occurs in the supplied list of
	 * excluded subdirs.
	 * 
	 * @param subManifestFile
	 *        the url of a sub-manifest
	 * @param excludedSubdirs
	 *        an array of directory names. May be null.
	 * @return <code>false</code> if the supplied list of excluded subdirs is not
	 *         empty and contains a match for the supplied sub-manifest,
	 *         <code>true</code> otherwise.
	 */
	private static boolean includeSubManifest(String subManifestFile, String[] excludedSubdirs) {
		boolean result = true;

		if (excludedSubdirs != null && excludedSubdirs.length > 0) {
			int index = subManifestFile.lastIndexOf("/");
			String path = subManifestFile.substring(0, index);
			String sd = path.substring(path.lastIndexOf("/") + 1);

			for (String subdir : excludedSubdirs) {
				if (sd.equals(subdir)) {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	static void addTurtle(RepositoryConnection con, URL url, String baseURI, Resource... contexts)
		throws IOException, RepositoryException, RDFParseException
	{
		if (baseURI == null) {
			baseURI = url.toExternalForm();
		}

		InputStream in = url.openStream();

		try {
			OpenRDFUtil.verifyContextNotNull(contexts);
			final ValueFactory vf = con.getRepository().getValueFactory();
			RDFParser rdfParser = new TurtleParser();
			rdfParser.setValueFactory(vf);

			rdfParser.setVerifyData(false);
			rdfParser.setStopAtFirstError(true);
			rdfParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

			RDFInserter rdfInserter = new RDFInserter(con);
			rdfInserter.enforceContext(contexts);
			rdfParser.setRDFHandler(rdfInserter);

			con.begin();
			try {
				rdfParser.parse(in, baseURI);
				con.commit();
			}
			catch (RDFHandlerException e) {
				con.rollback();
				// RDFInserter only throws wrapped RepositoryExceptions
				throw (RepositoryException)e.getCause();
			}
			catch (RuntimeException e) {
				con.rollback();
			}
		}
		finally {
			in.close();
		}
	}
}
