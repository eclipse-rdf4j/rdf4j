/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Callable;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SpinSailTest {

	private static final String BASE_DIR = "/testcases/";

	@Rule
	public ExpectedException constraintException = ExpectedException.none();

	private Repository repo;

	private RepositoryConnection conn;

	@Before
	public void setup()
		throws RepositoryException
	{
		NotifyingSail baseSail = new MemoryStore();
		DedupingInferencer deduper = new DedupingInferencer(baseSail);
		ForwardChainingRDFSInferencer rdfsInferencer = new ForwardChainingRDFSInferencer(deduper);
		SpinSail spinSail = new SpinSail(rdfsInferencer);
		repo = new SailRepository(spinSail);
		repo.initialize();
		conn = repo.getConnection();
	}

	@After
	public void tearDown()
		throws RepositoryException
	{
		if (conn != null) {
			conn.close();
		}
		if (repo != null) {
			repo.shutDown();
		}
	}

	@Test
	public void testAskConstraint()
		throws Exception
	{
		constraintException.expectCause(isA(ConstraintViolationException.class));
		constraintException.expectMessage("Test constraint");
		loadStatements("testAskConstraint.ttl");
	}

	@Test
	public void testTemplateConstraint()
		throws Exception
	{
		constraintException.expectCause(isA(ConstraintViolationException.class));
		constraintException.expectMessage("Invalid number of values: 0");
		loadStatements("testTemplateConstraint.ttl");
	}

	@Test
	public void testConstructRule()
		throws Exception
	{
		loadStatements("testConstructRule.ttl");
		assertStatements("testConstructRule-expected.ttl");
	}

	@Test
	public void testUpdateTemplateRule()
		throws Exception
	{
		loadStatements("testUpdateTemplateRule.ttl");
		assertStatements("testUpdateTemplateRule-expected.ttl");
	}

	@Test
	public void testConstructor()
		throws Exception
	{
		loadStatements("testConstructor.ttl");
		assertStatements("testConstructor-expected.ttl");
	}

	@Test
	public void testAskFunctionConstraint()
		throws Exception
	{
		loadStatements("testAskFunctionConstraint.ttl");
	}

	@Test
	public void testEvalFunctionConstraint()
		throws Exception
	{
		loadStatements("testEvalFunctionConstraint.ttl");
	}

	@Test
	public void testConstructProperty()
		throws Exception
	{
		loadStatements("testConstructProperty.ttl");
		assertStatements("testConstructProperty-expected.ttl");
	}

	@Test
	public void testSelectProperty()
		throws Exception
	{
		loadStatements("testSelectProperty.ttl");
		assertStatements("testSelectProperty-expected.ttl");
	}

	@Test
	public void testMagicPropertyRule()
		throws Exception
	{
		loadStatements("testMagicPropertyRule.ttl");
		assertStatements("testMagicPropertyRule-expected.ttl");
	}

	@Test
	public void testMagicPropertyConstraint()
		throws Exception
	{
		loadStatements("testMagicPropertyConstraint.ttl");
	}

	@Test
	public void testMagicPropertyFunction()
		throws Exception
	{
		loadStatements("testMagicPropertyFunction.ttl");
		assertStatements("testMagicPropertyFunction-expected.ttl");
	}

	@Test
	public void testSpinxRule()
		throws Exception
	{
		loadStatements("testSpinxRule.ttl");
		assertStatements("testSpinxRule-expected.ttl");
	}

	@Test
	public void testTransactions()
		throws Exception
	{
		tx(new Callable<Void>() {

			public Void call()
				throws Exception
			{
				loadStatements("testTransactions-rule.ttl");
				return null;
			}
		});
		tx(new Callable<Void>() {

			public Void call()
				throws Exception
			{
				loadStatements("testTransactions-data.ttl");
				return null;
			}
		});
		tx(new Callable<Void>() {

			public Void call()
				throws Exception
			{
				assertStatements("testTransactions-expected.ttl");
				return null;
			}
		});
	}

	private <T> T tx(Callable<T> c)
		throws Exception
	{
		T result;
		conn.begin();
		try {
			result = c.call();
			conn.commit();
		}
		catch (Exception e) {
			conn.rollback();
			throw e;
		}
		return result;
	}

	private void loadStatements(String ttl)
		throws RepositoryException, RDFParseException, IOException
	{
		URL url = getClass().getResource(BASE_DIR + ttl);
		InputStream in = url.openStream();
		try {
			conn.add(in, url.toString(), RDFFormat.TURTLE);
		}
		finally {
			in.close();
		}
	}

	private void assertStatements(String ttl)
		throws RDFParseException, RDFHandlerException, IOException, RepositoryException
	{
		StatementCollector expected = new StatementCollector();
		RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
		parser.setRDFHandler(expected);
		URL url = getClass().getResource(BASE_DIR + ttl);
		InputStream rdfStream = url.openStream();
		parser.parse(rdfStream, url.toString());
		rdfStream.close();

		for (Statement stmt : expected.getStatements()) {
			assertTrue("Expected statement: " + stmt, conn.hasStatement(stmt, true));
		}
	}
}
