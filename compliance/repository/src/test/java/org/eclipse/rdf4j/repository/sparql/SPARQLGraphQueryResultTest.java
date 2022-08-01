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
import org.eclipse.rdf4j.testsuite.repository.GraphQueryResultTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Jeen Broekstra
 */
public class SPARQLGraphQueryResultTest extends GraphQueryResultTest {

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

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop();
		server = null;
	}

	@Override
	protected Repository newRepository() throws Exception {
		return new SPARQLRepository(HTTPMemServer.REPOSITORY_URL,
				Protocol.getStatementsLocation(HTTPMemServer.REPOSITORY_URL));

	}

}
