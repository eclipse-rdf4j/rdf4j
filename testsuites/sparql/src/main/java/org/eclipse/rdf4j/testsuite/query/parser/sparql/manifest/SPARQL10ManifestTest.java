/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;
import java.util.jar.JarFile;

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

import junit.framework.TestResult;
import junit.framework.TestSuite;

@Deprecated
public class SPARQL10ManifestTest {

	static final Logger logger = LoggerFactory.getLogger(SPARQL10ManifestTest.class);

	private static final boolean REMOTE = false;

	public static TestSuite suite(SPARQLQueryTest.Factory factory) throws Exception {
		final String manifestFile;
		final File tmpDir;

		if (REMOTE) {
			manifestFile = "http://www.w3.org/2001/sw/DataAccess/tests/data-r2/manifest-evaluation.ttl";
			tmpDir = null;
		} else {
			URL url = SPARQL10ManifestTest.class
					.getResource("/testcases-sparql-1.0-w3c/data-r2/manifest-evaluation.ttl");

			if ("jar".equals(url.getProtocol())) {
				// Extract manifest files to a temporary directory
				try {
					tmpDir = Files.createTempDirectory("sparql-evaluation").toFile();

					JarURLConnection con = (JarURLConnection) url.openConnection();
					JarFile jar = con.getJarFile();

					ZipUtil.extract(jar, tmpDir);

					File localFile = new File(tmpDir, con.getEntryName());
					manifestFile = localFile.toURI().toURL().toString();
				} catch (IOException e) {
					throw new AssertionError(e);
				}
			} else {
				manifestFile = url.toString();
				tmpDir = null;
			}
		}

		TestSuite suite = new TestSuite(factory.getClass().getName()) {

			@Override
			public void run(TestResult result) {
				try {
					super.run(result);
				} finally {
					if (tmpDir != null) {
						try {
							FileUtil.deleteDir(tmpDir);
						} catch (IOException e) {
							System.err.println(
									"Unable to clean up temporary directory '" + tmpDir + "': " + e.getMessage());
						}
					}
				}
			}
		};

		Repository manifestRep = new SailRepository(new MemoryStore());
		try (RepositoryConnection con = manifestRep.getConnection()) {

			addTurtle(con, new URL(manifestFile), manifestFile);

			String query = ""
					+ "PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n "
					+ "PRFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>"
					+ "SELECT DISTINCT ?manifestFile\n"
					+ "WHERE { ?x rdf:first ?manifestFile .} ";

			TupleQueryResult manifestResults = con.prepareTupleQuery(QueryLanguage.SPARQL, query, manifestFile)
					.evaluate();

			while (manifestResults.hasNext()) {
				BindingSet bindingSet = manifestResults.next();
				String subManifestFile = bindingSet.getValue("manifestFile").stringValue();
				suite.addTest(SPARQLQueryTest.suite(subManifestFile, factory));
			}

			manifestResults.close();
		}
		manifestRep.shutDown();

		logger.info("Created aggregated test suite with " + suite.countTestCases() + " test cases.");
		return suite;
	}

	static void addTurtle(RepositoryConnection con, URL url, String baseURI, Resource... contexts)
			throws IOException, RepositoryException, RDFParseException, RDFHandlerException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
		if (baseURI == null) {
			baseURI = url.toExternalForm();
		}

		try (InputStream in = url.openStream()) {
			final ValueFactory vf = con.getRepository().getValueFactory();
			RDFParser rdfParser = new TurtleParser();
			rdfParser.setValueFactory(vf);

			RDFInserter rdfInserter = new RDFInserter(con);
			rdfInserter.enforceContext(contexts);
			rdfParser.setRDFHandler(rdfInserter);

			con.begin();

			try {
				rdfParser.parse(in, baseURI);
				con.commit();
			} catch (RDFHandlerException e) {
				if (con.isActive()) {
					con.rollback();
				}
				if (e.getCause() != null && e.getCause() instanceof RepositoryException) {
					// RDFInserter only throws wrapped RepositoryExceptions
					throw (RepositoryException) e.getCause();
				} else {
					throw e;
				}

			} catch (RuntimeException e) {
				if (con.isActive()) {
					con.rollback();
				}
				throw e;
			}
		}
	}
}
