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
package org.eclipse.rdf4j.repository.http;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.testsuite.repository.RDFSchemaRepositoryConnectionTest;
import org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/* FIXME: are these tests really necessary, or are we just duplicating what is already tested locally? */
public class RDFSchemaHTTPRepositoryConnectionTest extends RDFSchemaRepositoryConnectionTest {

	private static HTTPMemServer server;

	@BeforeAll
	public static void startServer() throws Exception {
		server = new HTTPMemServer();
		try {
			server.start();
		} catch (Exception e) {
			server.stop();
			throw e;
		}
	}

	@AfterAll
	public static void stopServer() throws Exception {
		server.stop();
	}

	@Override
	protected Repository createRepository(File dataDir) {
		return new HTTPRepository(HTTPMemServer.INFERENCE_REPOSITORY_URL);
	}

	@Disabled("temporarily disabled for HTTPRepository")
	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testTransactionIsolation(IsolationLevel level) throws Exception {
		System.err.println("temporarily disabled testTransactionIsolation() for HTTPRepository");
	}

	@Disabled("temporarily disabled for HTTPRepository")
	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testAutoCommit(IsolationLevel level) throws Exception {
		System.err.println("temporarily disabled testAutoCommit() for HTTPRepository");
	}

	@Disabled("temporarily disabled for HTTPRepository")
	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testRollback(IsolationLevel level) throws Exception {
		System.err.println("temporarily disabled testRollback() for HTTPRepository");
	}

	@Disabled("temporarily disabled for HTTPRepository")
	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testEmptyCommit(IsolationLevel level) throws Exception {
		System.err.println("temporarily disabled testEmptyCommit() for HTTPRepository");
	}

	@Disabled("temporarily disabled for HTTPRepository")
	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testEmptyRollback(IsolationLevel level) throws Exception {
		System.err.println("temporarily disabled testEmptyRollback() for HTTPRepository");
	}

	@Disabled("temporarily disabled for HTTPRepository")
	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testSizeCommit(IsolationLevel level) throws Exception {
		System.err.println("temporarily disabled testSizeCommit() for HTTPRepository");
	}

	@Disabled("temporarily disabled for HTTPRepository")
	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testSizeRollback(IsolationLevel level) throws Exception {
		System.err.println("temporarily disabled testSizeRollback() for HTTPRepository");
	}

	@Disabled("temporarily disabled for HTTPRepository")
	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testGetContextIDs(IsolationLevel level) throws Exception {
		System.err.println("temporarily disabled testGetContextIDs() for HTTPRepository");
	}

	@Disabled("temporarily disabled for HTTPRepository")
	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testInferencerQueryDuringTransaction(IsolationLevel level) throws Exception {
		System.err.println("temporarily disabled testInferencerDuringTransaction() for HTTPRepository");
	}

	@Disabled("temporarily disabled for HTTPRepository")
	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testInferencerTransactionIsolation(IsolationLevel level) throws Exception {
		System.err.println("temporarily disabled testInferencerTransactionIsolation() for HTTPRepository");
	}

	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	public void testAddMalformedLiteralsDefaultConfig(IsolationLevel level) throws Exception {
		setupTest(level);

		try {
			testCon.add(RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "malformed-literals.ttl"),
					"", RDFFormat.TURTLE);
		} catch (RDF4JException e) {
			fail("upload of malformed literals should not fail with error in default configuration for HTTPRepository");
		}
	}

	@Override
	@ParameterizedTest
	@MethodSource("parameters")
	@Disabled
	public void testQueryDefaultGraph(IsolationLevel level) throws Exception {
		// ignore - schema caching inferencer uses different context handling
	}

	@Override
	@ParameterizedTest
	@MethodSource("parameters")
	@Disabled
	public void testDeleteDefaultGraph(IsolationLevel level) throws Exception {
		// ignore - schema caching inferencer uses different context handling
	}

	@Override
	@ParameterizedTest
	@MethodSource("parameters")
	@Disabled
	public void testContextStatementsNotDuplicated(IsolationLevel level) throws Exception {
		// ignore - schema caching inferencer uses different context handling
	}

	@Override
	@ParameterizedTest
	@MethodSource("parameters")
	@Disabled
	public void testContextStatementsNotDuplicated2(IsolationLevel level) throws Exception {
		// ignore - schema caching inferencer uses different context handling
	}

	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	@Disabled("See SES-1833")
	public void testAddMalformedLiteralsStrictConfig(IsolationLevel level) throws Exception {
		System.err.println("SES-1833: temporarily disabled testAddMalformedLiteralsStrictConfig() for HTTPRepository");
	}

}
