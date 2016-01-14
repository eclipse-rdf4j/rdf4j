/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RepositoryUtil;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public abstract class InferencingTest {

	/*-----------*
	 * Constants *
	 *-----------*/

	public static final String TEST_DIR_PREFIX = "/testcases/rdf-mt-inferencing";

	/*---------*
	 * Methods *
	 *---------*/

	public void runTest(Sail sailStack, String subdir, String testName, boolean isPositiveTest)
		throws Exception
	{
		final String name = subdir + "/" + testName;
		final String inputData = TEST_DIR_PREFIX + "/" + name + "-in.nt";
		final String outputData = TEST_DIR_PREFIX + "/" + name + "-out.nt";

		Collection<? extends Statement> entailedStatements = null;
		Collection<? extends Statement> expectedStatements = null;

		Repository repository = new SailRepository(sailStack);
		repository.initialize();

		RepositoryConnection con = repository.getConnection();
		con.begin();

		// clear the input store
		con.clear();
		con.commit();

		// Upload input data
		InputStream stream = getClass().getResourceAsStream(inputData);
		try {
			con.begin();
			con.add(stream, inputData, RDFFormat.NTRIPLES);
			con.commit();

			entailedStatements = Iterations.addAll(con.getStatements(null, null, null, true),
					new HashSet<Statement>());
		}
		catch (Exception e) {
			if (con.isActive()) {
				con.rollback();
			}
		}
		finally {
			stream.close();
			con.close();
		}

		// Upload output data
		Repository outputRepository = new SailRepository(new MemoryStore());
		outputRepository.initialize();
		con = outputRepository.getConnection();

		stream = getClass().getResourceAsStream(outputData);
		try {
			con.begin();
			con.add(stream, outputData, RDFFormat.NTRIPLES);
			con.commit();

			expectedStatements = Iterations.addAll(con.getStatements(null, null, null, false),
					new HashSet<Statement>());
		}
		catch (Exception e) {
			if (con.isActive()) {
				con.rollback();
			}
		}
		finally {
			stream.close();
			con.close();
			outputRepository.shutDown();
			repository.shutDown();
		}

		// Check whether all expected statements are present in the entailment
		// closure set.
		boolean outputEntailed = Models.isSubset(expectedStatements, entailedStatements);

		if (isPositiveTest && !outputEntailed) {
			File dumpFile = dumpStatements(name,
					RepositoryUtil.difference(expectedStatements, entailedStatements));

			fail("Incomplete entailment, difference between expected and entailed dumped to file " + dumpFile);
		}
		else if (!isPositiveTest && outputEntailed) {
			File dumpFile = dumpStatements(name, expectedStatements);
			fail("Erroneous entailment, unexpected statements dumped to file " + dumpFile);
		}
	}

	private File dumpStatements(String name, Collection<? extends Statement> statements)
		throws Exception
	{
		// Dump results to tmp file for debugging
		String tmpDir = System.getProperty("java.io.tmpdir");
		File tmpFile = new File(tmpDir, "junit-" + name + ".nt");

		OutputStream export = new FileOutputStream(tmpFile);
		try {
			RDFWriter writer = Rio.createWriter(RDFFormat.NTRIPLES, export);

			writer.startRDF();
			for (Statement st : statements) {
				writer.handleStatement(st);
			}
			writer.endRDF();
		}
		finally {
			export.close();
		}

		return tmpFile;
	}

	/*----------------*
	 * Static methods *
	 *----------------*/

	@Test
	public void testSubClassOf001()
		throws Exception
	{
		runTest(createSail(), "subclassof", "test001", true);
	}

	@Test
	public void testSubClassOf002()
		throws Exception
	{
		runTest(createSail(), "subclassof", "test002", true);
	}

	@Test
	public void testSubClassOf003()
		throws Exception
	{
		runTest(createSail(), "subclassof", "test003", true);
	}

	@Test
	public void testSubClassOfError001()
		throws Exception
	{
		runTest(createSail(), "subclassof", "error001", false);
	}

	@Test
	public void testSubPropertyOf001()
		throws Exception
	{
		runTest(createSail(), "subpropertyof", "test001", true);
	}

	@Test
	public void testSubPropertyOf002()
		throws Exception
	{
		runTest(createSail(), "subpropertyof", "test002", true);
	}

	@Test
	public void testSubPropertyOf003()
		throws Exception
	{
		runTest(createSail(), "subpropertyof", "test003", true);
	}

	@Test
	public void testSubPropertyOfError001()
		throws Exception
	{
		runTest(createSail(), "subpropertyof", "error001", false);
	}

	@Test
	public void testDomain001()
		throws Exception
	{
		runTest(createSail(), "domain", "test001", true);
	}

	@Test
	public void testDomainError001()
		throws Exception
	{
		runTest(createSail(), "domain", "error001", false);
	}

	@Test
	public void testRange001()
		throws Exception
	{
		runTest(createSail(), "range", "test001", true);
	}

	@Test
	public void testRangeError001()
		throws Exception
	{
		runTest(createSail(), "range", "error001", false);
	}

	@Test
	public void testType001()
		throws Exception
	{
		runTest(createSail(), "type", "test001", true);
	}

	@Test
	public void testType002()
		throws Exception
	{
		runTest(createSail(), "type", "test002", true);
	}

	@Test
	public void testType003()
		throws Exception
	{
		runTest(createSail(), "type", "test003", true);
	}

	@Test
	public void testType004()
		throws Exception
	{
		runTest(createSail(), "type", "test004", true);
	}

	@Test
	public void testType005()
		throws Exception
	{
		runTest(createSail(), "type", "test005", true);
	}

	@Test
	public void testTypeError001()
		throws Exception
	{
		runTest(createSail(), "type", "error001", false);
	}

	@Test
	public void testTypeError002()
		throws Exception
	{
		runTest(createSail(), "type", "error002", false);
	}

	/**
	 * Gets an instance of the Sail that should be tested. The returned
	 * repository must not be initialized.
	 * 
	 * @return an uninitialized Sail.
	 */
	protected abstract Sail createSail();

}
