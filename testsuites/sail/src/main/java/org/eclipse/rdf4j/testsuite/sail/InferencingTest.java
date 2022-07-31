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
package org.eclipse.rdf4j.testsuite.sail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class InferencingTest {

	private static final Logger logger = LoggerFactory.getLogger(InferencingTest.class);

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}
	/*-----------*
	 * Constants *
	 *-----------*/

	public static final String TEST_DIR_PREFIX = "/testcases/rdf-mt-inferencing";

	/*---------*
	 * Methods *
	 *---------*/

	public void runTest(String subdir, String testName, boolean isPositiveTest) throws Exception {
		final String name = subdir + "/" + testName;
		final String inputData = TEST_DIR_PREFIX + "/" + name + "-in.nt";
		final String outputData = TEST_DIR_PREFIX + "/" + name + "-out.nt";

		Collection<? extends Statement> entailedStatements = new HashSet<>();

		Sail sail = createSail();
		try (SailConnection con = sail.getConnection()) {
			con.begin();

			// clear the input store
			con.clear();
			con.commit();

			// Upload input data
			try (InputStream stream = getClass().getResourceAsStream(inputData)) {
				con.begin();
				Model m = Rio.parse(stream, inputData, RDFFormat.NTRIPLES);
				for (Statement st : m) {
					con.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
				}
				con.commit();

				entailedStatements = Iterations.addAll(con.getStatements(null, null, null, true), new HashSet<>());
			} catch (Exception e) {
				if (con.isActive()) {
					con.rollback();
				}
				logger.error("exception while uploading input data", e);
			}
		} finally {
			sail.shutDown();
		}

		Model expectedStatements;

		// Read output data
		try (InputStream stream = getClass().getResourceAsStream(outputData)) {
			expectedStatements = Rio.parse(stream, "",
					Rio.getParserFormatForFileName(outputData).orElse(RDFFormat.NTRIPLES));
		}

		// Check whether all expected statements are present in the entailment
		// closure set.
		boolean outputEntailed = Models.isSubset(expectedStatements, entailedStatements);

		if (isPositiveTest && !outputEntailed) {
			Model diff = new LinkedHashModelFactory().createEmptyModel();
			for (Statement st : entailedStatements) {
				if (!expectedStatements.contains(st)) {
					diff.add(st);
				}
			}

			File dumpFile = dumpStatements(name, diff);
			Assert.fail("Incomplete entailment, diff dumped to file " + dumpFile);
		} else if (!isPositiveTest && outputEntailed) {
			File dumpFile = dumpStatements(name, expectedStatements);
			Assert.fail("Erroneous entailment, unexpected statements dumped to file " + dumpFile);
		}
	}

	private File dumpStatements(String name, Collection<? extends Statement> statements) throws Exception {
		// Dump results to tmp file for debugging
		String tmpDir = System.getProperty("java.io.tmpdir");
		name = name.replace("/", "_");
		File tmpFile = new File(tmpDir, "junit-" + name + ".nt");
		tmpFile.createNewFile();

		try (OutputStream export = new BufferedOutputStream(new FileOutputStream(tmpFile))) {
			RDFWriter writer = Rio.createWriter(RDFFormat.NTRIPLES, export);

			writer.startRDF();
			for (Statement st : statements) {
				writer.handleStatement(st);
			}
			writer.endRDF();
		}

		return tmpFile;
	}

	/*----------------*
	 * Static methods *
	 *----------------*/

	@Test
	public void testSubClassOf001() throws Exception {
		runTest("subclassof", "test001", true);
	}

	@Test
	public void testSubClassOf002() throws Exception {
		runTest("subclassof", "test002", true);
	}

	@Test
	public void testSubClassOf003() throws Exception {
		runTest("subclassof", "test003", true);
	}

	@Test
	public void testSubClassOfError001() throws Exception {
		runTest("subclassof", "error001", false);
	}

	@Test
	public void testSubPropertyOf001() throws Exception {
		runTest("subpropertyof", "test001", true);
	}

	@Test
	public void testSubPropertyOf002() throws Exception {
		runTest("subpropertyof", "test002", true);
	}

	@Test
	public void testSubPropertyOf003() throws Exception {
		runTest("subpropertyof", "test003", true);
	}

	@Test
	public void testSubPropertyOf004() throws Exception {
		runTest("subpropertyof", "test004", true);
	}

	@Test
	public void testSubPropertyOfError001() throws Exception {
		runTest("subpropertyof", "error001", false);
	}

	@Test
	public void testDomain001() throws Exception {
		runTest("domain", "test001", true);
	}

	@Test
	public void testDomainError001() throws Exception {
		runTest("domain", "error001", false);
	}

	@Test
	public void testRange001() throws Exception {
		runTest("range", "test001", true);
	}

	@Test
	public void testRangeError001() throws Exception {
		runTest("range", "error001", false);
	}

	@Test
	public void testType001() throws Exception {
		runTest("type", "test001", true);
	}

	@Test
	public void testType002() throws Exception {
		runTest("type", "test002", true);
	}

	@Test
	public void testType003() throws Exception {
		runTest("type", "test003", true);
	}

	@Test
	public void testType004() throws Exception {
		runTest("type", "test004", true);
	}

	@Test
	public void testType005() throws Exception {
		runTest("type", "test005", true);
	}

	@Test
	public void testType006() throws Exception {
		runTest("type", "test006", true);
	}

	@Test
	public void testTypeError001() throws Exception {
		runTest("type", "error001", false);
	}

	@Test
	public void testTypeError002() throws Exception {
		runTest("type", "error002", false);
	}

	/**
	 * Gets an instance of the Sail that should be tested.
	 *
	 * @return a SailRepo.
	 */
	protected abstract Sail createSail();

}
