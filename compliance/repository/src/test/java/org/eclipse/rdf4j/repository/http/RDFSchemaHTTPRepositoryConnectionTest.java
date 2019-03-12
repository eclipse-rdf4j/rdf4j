/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.http;

import static org.junit.Assert.fail;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.repository.RDFSchemaRepositoryConnectionTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnectionTest;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class RDFSchemaHTTPRepositoryConnectionTest extends RDFSchemaRepositoryConnectionTest {

	private static HTTPMemServer server;

	public RDFSchemaHTTPRepositoryConnectionTest(IsolationLevel level) {
		super(level);
	}

	@BeforeClass
	public static void startServer() throws Exception {
		server = new HTTPMemServer();
		try {
			server.start();
		} catch (Exception e) {
			server.stop();
			throw e;
		}
	}

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop();
	}

	@Override
	protected Repository createRepository() {
		return new HTTPRepository(HTTPMemServer.INFERENCE_REPOSITORY_URL);
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testTransactionIsolationForRead() throws Exception {
		System.err.println("temporarily disabled testTransactionIsolationForRead() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testTransactionIsolationForReadWithDeleteOperation() throws Exception {
		System.err.println(
				"temporarily disabled testTransactionIsolationForReadWithDeleteOperation() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testTransactionIsolation() throws Exception {
		System.err.println("temporarily disabled testTransactionIsolation() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testAutoCommit() throws Exception {
		System.err.println("temporarily disabled testAutoCommit() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testRollback() throws Exception {
		System.err.println("temporarily disabled testRollback() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testEmptyCommit() throws Exception {
		System.err.println("temporarily disabled testEmptyCommit() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testEmptyRollback() throws Exception {
		System.err.println("temporarily disabled testEmptyRollback() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testSizeCommit() throws Exception {
		System.err.println("temporarily disabled testSizeCommit() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testSizeRollback() throws Exception {
		System.err.println("temporarily disabled testSizeRollback() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testGetContextIDs() throws Exception {
		System.err.println("temporarily disabled testGetContextIDs() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testInferencerQueryDuringTransaction() throws Exception {
		System.err.println("temporarily disabled testInferencerDuringTransaction() for HTTPRepository");
	}

	@Ignore("temporarily disabled for HTTPRepository")
	@Test
	@Override
	public void testInferencerTransactionIsolation() throws Exception {
		System.err.println("temporarily disabled testInferencerTransactionIsolation() for HTTPRepository");
	}

	@Test
	@Override
	public void testAddMalformedLiteralsDefaultConfig() throws Exception {
		try {
			testCon.add(RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "malformed-literals.ttl"),
					"", RDFFormat.TURTLE);
		} catch (RDF4JException e) {
			fail("upload of malformed literals should not fail with error in default configuration for HTTPRepository");
		}
	}

	@Test
	@Override
	@Ignore("See SES-1833")
	public void testAddMalformedLiteralsStrictConfig() throws Exception {
		System.err.println("SES-1833: temporarily disabled testAddMalformedLiteralsStrictConfig() for HTTPRepository");
	}

}
