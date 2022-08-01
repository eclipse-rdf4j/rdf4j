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
package org.eclipse.rdf4j.repository.sparql;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.http.HTTPMemServer;
import org.eclipse.rdf4j.testsuite.repository.RepositoryTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * @author Jeen Broekstra
 */
public class SPARQLRepositoryTest extends RepositoryTest {

	private static HTTPMemServer server;

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

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		// overwrite bnode test values as SPARQL endpoints do not generally work
		// well with bnodes
		bob = testRepository.getValueFactory().createIRI("urn:x-local:bob");
		alice = testRepository.getValueFactory().createIRI("urn:x-local:alice");
		alexander = testRepository.getValueFactory().createIRI("urn:x-local:alexander");

	}

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop();
		server = null;
	}

	@Override
	protected Repository createRepository() throws Exception {
		return new SPARQLRepository(HTTPMemServer.REPOSITORY_URL,
				Protocol.getStatementsLocation(HTTPMemServer.REPOSITORY_URL));

	}

}
