/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.shacl.manifest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.common.io.ZipUtil;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * Functionality for creating a JUnit test suite out of a W3C Working Group-style manifest for SHACL shape constraints
 * testsuite
 *
 * @author James Leigh
 */
public class SHACLManifestTestSuiteFactory {

	private static final String MF_INCLUDE = "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#include";
	private static final String MF_ENTRIES = "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#entries";
	private static final String MF_ACTION = "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#action";
	private static final String MF_RESULT = "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#result";
	private static final String MF_STATUS = "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#status";
	private static final String SHT_SHAPESGRAPH = "http://www.w3.org/ns/shacl-test#shapesGraph";
	private static final String SHT_DATAGRAPH = "http://www.w3.org/ns/shacl-test#dataGraph";
	private static final String SHT_PROPOSED = "http://www.w3.org/ns/shacl-test#proposed";
	private static final String SHT_APPROVED = "http://www.w3.org/ns/shacl-test#approved";
	private static final String SHT_FAILURE = "http://www.w3.org/ns/shacl-test#Failure";
	private static final String SH_CONFORMS = "http://www.w3.org/ns/shacl#conforms";

	private final Logger logger = LoggerFactory.getLogger(SHACLManifestTestSuiteFactory.class);

	private File tmpDir;

	public interface TestFactory {
		String getName();

		Test createSHACLTest(String testURI, String label, Model shapesGraph, Model dataGraph, boolean failure,
				boolean conforms);
	}

	/**
	 * Creates a new {@link TestSuite} for executiong of {@link AbstractSHACLTest} s.
	 *
	 * @param factory                   a factory class that creates each individual test case.
	 * @param officialWorkingGroupTests indicates whether to use the official W3C working group tests, or Sesame's own
	 *                                  set of tests.
	 * @param approvedTestsOnly         if <code>true</code>, use working group-approved tests only. Has no influence
	 *                                  when officialWorkingGroup tests is set to <code>false</code>.
	 * @param useRemoteTests            if set to <code>true</code>, use manifests and tests located at
	 *                                  <code>http://www.w3.org/2009/sparql/docs/tests/data-sparql11/</code> , instead
	 *                                  of local copies.
	 * @param excludedSubdirs           an (optionally empty) list of subdirectories to exclude from testing. If
	 *                                  specified, test cases in one of the supplied subdirs will not be executed. If
	 *                                  left empty, all tests will be executed.
	 * @return a TestSuite.
	 * @throws Exception
	 */
	public TestSuite createTestSuite(TestFactory factory, boolean officialWorkingGroupTests, boolean approvedTestsOnly,
			boolean useRemoteTests, String... excludedSubdirs) throws Exception {
		final String manifest = getManifestFile(officialWorkingGroupTests, useRemoteTests);

		TestSuite suite = new TestSuite(factory.getName()) {

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

		Map<String, TestSuite> tests = new LinkedHashMap<>();
		ValueFactory vf = SimpleValueFactory.getInstance();
		Model manifests = new LinkedHashModel();
		readTurtle(manifests, new URL(manifest), manifest, excludedSubdirs);
		for (Value included : manifests.filter(null, vf.createIRI(MF_INCLUDE), null).objects()) {
			String subManifestFile = included.stringValue();
			boolean hasEntries = manifests.contains((IRI) included, vf.createIRI(MF_ENTRIES), null);
			if (hasEntries && includeSubManifest(subManifestFile, excludedSubdirs)) {
				TestSuite suiteEntry = createSuiteEntry(subManifestFile, factory, approvedTestsOnly);
				if (tests.containsKey(suiteEntry.getName())) {
					for (int i = 0, n = suiteEntry.testCount(); i < n; i++) {
						tests.get(suiteEntry.getName()).addTest(suiteEntry.testAt(i));
					}
				} else {
					tests.put(suiteEntry.getName(), suiteEntry);
				}
			}
		}
		for (TestSuite testSuiets : tests.values()) {
			suite.addTest(testSuiets);
		}

		logger.info("Created aggregated test suite with " + suite.countTestCases() + " test cases.");
		return suite;
	}

	private String getManifestFile(boolean officialWorkingGroupTests, boolean useRemote) {
		if (useRemote) {
			return "https://raw.githubusercontent.com/w3c/data-shapes/gh-pages/data-shapes-test-suite/tests/manifest.ttl";
		}
		URL url = SHACLManifestTestSuiteFactory.class.getResource("/data-shapes-test-suite/tests/manifest.ttl");
		if ("jar".equals(url.getProtocol())) {
			// Extract manifest files to a temporary directory
			try {
				tmpDir = Files.createTempDirectory("data-shapes-test-evaluation").toFile();

				JarURLConnection con = (JarURLConnection) url.openConnection();
				JarFile jar = con.getJarFile();

				ZipUtil.extract(jar, tmpDir);

				File localFile = new File(tmpDir, con.getEntryName());
				return localFile.toURI().toURL().toString();
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		} else {
			return url.toString();
		}
	}

	private IRI readTurtle(Model manifests, URL url, String baseURI, String... excludedSubdirs)
			throws IOException, RDFParseException {
		if (baseURI == null) {
			baseURI = url.toExternalForm();
		}
		ParsedIRI baseIRI = ParsedIRI.create(baseURI);
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		IRI manifest = vf.createIRI(baseIRI.toString());
		int before = manifests.size();

		try (InputStream in = url.openStream()) {
			RDFParser rdfParser = new TurtleParser();

			ContextStatementCollector collection = new ContextStatementCollector(manifests, vf, manifest);
			rdfParser.setRDFHandler(collection);

			rdfParser.parse(in, baseIRI.toString());
			for (Map.Entry<String, String> e : collection.getNamespaces().entrySet()) {
				manifests.setNamespace(e.getKey(), e.getValue());
			}
		}
		if (before < manifests.size()) {
			for (Value included : new LinkedHashSet<>(
					manifests.filter(manifest, vf.createIRI(MF_INCLUDE), null).objects())) {
				String subManifestFile = included.stringValue();
				if (includeSubManifest(subManifestFile, excludedSubdirs)) {
					readTurtle(manifests, new URL(subManifestFile), subManifestFile, excludedSubdirs);
				}
			}
		}
		return manifest;
	}

	/**
	 * Verifies if the selected subManifest occurs in the supplied list of excluded subdirs.
	 *
	 * @param subManifestFile the url of a sub-manifest
	 * @param excludedSubdirs an array of directory names. May be null.
	 * @return <code>false</code> if the supplied list of excluded subdirs is not empty and contains a match for the
	 *         supplied sub-manifest, <code>true</code> otherwise.
	 */
	private boolean includeSubManifest(String subManifestFile, String[] excludedSubdirs) {
		boolean result = true;

		if (excludedSubdirs != null && excludedSubdirs.length > 0) {
			int index = subManifestFile.lastIndexOf('/');
			String path = subManifestFile.substring(0, index);
			String sd = path.substring(path.lastIndexOf('/') + 1);

			for (String subdir : excludedSubdirs) {
				if (sd.equals(subdir)) {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	private TestSuite createSuiteEntry(String manifestFileURL, TestFactory factory, boolean approvedOnly)
			throws Exception {
		logger.info("Building test suite for {}", manifestFileURL);

		// Read manifest and create declared test cases
		Model model = new LinkedHashModel();

		IRI manifest = readTurtle(model, new URL(manifestFileURL), manifestFileURL);

		TestSuite suite = new TestSuite(getManifestName(model, manifest));

		// Extract test case information from the manifest file. Note that we only
		// select those test cases that are mentioned in the list.
		Resource rdfList = getResource(model, manifest, MF_ENTRIES);
		for (Resource entry : getListEntries(model, rdfList)) {
			String label = getLiteral(model, entry, RDFS.LABEL.stringValue()).stringValue();
			Resource action = getResource(model, entry, MF_ACTION);
			Resource result = getResource(model, entry, MF_RESULT);
			Resource status = getResource(model, entry, MF_STATUS);
			Model shapesGraph = readTurtle(getResource(model, action, SHT_SHAPESGRAPH).stringValue());
			Model dataGraph = readTurtle(getResource(model, action, SHT_DATAGRAPH).stringValue());
			boolean failure = result.stringValue().equals(SHT_FAILURE);
			boolean conforms = !failure && getLiteral(model, result, SH_CONFORMS).booleanValue();

			logger.debug("found test case : {}", label);

			if (status.stringValue().equals(SHT_APPROVED)
					|| !approvedOnly && status.stringValue().equals(SHT_PROPOSED)) {
				Test test = factory.createSHACLTest(entry.stringValue(), label, shapesGraph, dataGraph, failure,
						conforms);
				if (test != null) {
					suite.addTest(test);
				}
			}
		}

		logger.info("Created test suite with " + suite.countTestCases() + " test cases.");
		return suite;
	}

	private String getManifestName(Model model, IRI manifest)
			throws QueryEvaluationException, RepositoryException, MalformedQueryException {
		// Try to extract suite name from manifest file
		String label = Models.objectString(model.getStatements(manifest, RDFS.LABEL, null)).orElse(null);
		if (label != null) {
			return label;
		}

		// Derive name from manifest URL
		String manifestFileURL = manifest.stringValue();
		int lastSlashIdx = manifestFileURL.lastIndexOf('/');
		int secLastSlashIdx = manifestFileURL.lastIndexOf('/', lastSlashIdx - 1);
		return manifestFileURL.substring(secLastSlashIdx + 1, lastSlashIdx);
	}

	private List<Resource> getListEntries(Model model, Resource rdfList) {
		if (rdfList == null || rdfList.equals(RDF.NIL)) {
			return new ArrayList<>();
		}
		Resource first = Models.objectResource(model.getStatements(rdfList, RDF.FIRST, null)).orElse(null);
		Resource rest = Models.objectResource(model.getStatements(rdfList, RDF.REST, null)).orElse(null);
		List<Resource> list = getListEntries(model, rest);
		list.add(0, first);
		return list;
	}

	private Resource getResource(Model model, Resource subject, String pred) {
		ValueFactory vf = SimpleValueFactory.getInstance();
		Optional<Resource> optional = Models.objectResource(model.getStatements(subject, vf.createIRI(pred), null));
		return optional.orElseThrow(Models.modelException("Missing " + subject + " " + pred));
	}

	private Literal getLiteral(Model model, Resource subject, String pred) {
		ValueFactory vf = SimpleValueFactory.getInstance();
		Optional<Literal> optional = Models.objectLiteral(model.getStatements(subject, vf.createIRI(pred), null));
		return optional.orElseThrow(Models.modelException("Missing " + subject + " " + pred));
	}

	private Model readTurtle(String url) throws RDFParseException, MalformedURLException, IOException {
		Model model = new LinkedHashModel();
		readTurtle(model, new URL(url), url);
		return model;
	}
}
