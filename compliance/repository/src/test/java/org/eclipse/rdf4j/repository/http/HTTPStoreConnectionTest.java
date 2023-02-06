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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.StringReader;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class HTTPStoreConnectionTest extends RepositoryConnectionTest {

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
		return new HTTPRepository(HTTPMemServer.REPOSITORY_URL);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testContextInTransactionAdd(IsolationLevel level) throws Exception {
		setupTest(level);

		StringReader stringReader = new StringReader("<urn:1> <urn:1> <urn:1>.");
		testCon.begin();
		IRI CONTEXT = testCon.getValueFactory().createIRI("urn:context");
		testCon.add(stringReader, "urn:baseUri", RDFFormat.NTRIPLES, CONTEXT);
		testCon.commit();

		IRI iri = testCon.getValueFactory().createIRI("urn:1");
		assertTrue(testCon.hasStatement(iri, iri, iri, false, CONTEXT));
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testUpdateExecution(IsolationLevel level) throws Exception {
		setupTest(level);

		IRI foobar = vf.createIRI("foo:bar");

		String sparql = "INSERT DATA { <foo:bar> <foo:bar> <foo:bar> . } ";

		Update update = testCon.prepareUpdate(QueryLanguage.SPARQL, sparql);

		update.execute();

		assertTrue(testCon.hasStatement(foobar, foobar, foobar, true));

		testCon.clear();

		assertFalse(testCon.hasStatement(foobar, foobar, foobar, true));

		testCon.begin();
		update.execute();
		testCon.commit();

		assertTrue(testCon.hasStatement(foobar, foobar, foobar, true));

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

	@ParameterizedTest
	@MethodSource("parameters")
	@Override
	@Disabled("See SES-1833")
	public void testAddMalformedLiteralsStrictConfig(IsolationLevel level) throws Exception {
		System.err.println("SES-1833: temporarily disabled testAddMalformedLiteralsStrictConfig() for HTTPRepository");
	}

}
