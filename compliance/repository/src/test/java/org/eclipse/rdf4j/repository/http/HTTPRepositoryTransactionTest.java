/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.http;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Test;

public class HTTPRepositoryTransactionTest {

	private static final String CACHE_TIMEOUT_PROPERTY = "rdf4j.server.txn.registry.timeout";

	private static HTTPMemServer server;

	private HTTPRepository testRepository;

	@Test
	public void testTimeout() throws Exception {
		try {
			System.setProperty(CACHE_TIMEOUT_PROPERTY, Integer.toString(2));
			server = new HTTPMemServer();
			try {
				server.start();
				testRepository = new HTTPRepository(HTTPMemServer.REPOSITORY_URL);
			} catch (Exception e) {
				server.stop();
				throw e;
			}
			try (RepositoryConnection connection = testRepository.getConnection()) {
				connection.begin();
				Thread.sleep(3000); // sleep for longer then the timeout
				connection.commit(); // was transaction removed due to timeout?
			}
			testRepository.shutDown();
		} finally {
			server.stop();
			System.clearProperty(CACHE_TIMEOUT_PROPERTY);
		}
	}

}
