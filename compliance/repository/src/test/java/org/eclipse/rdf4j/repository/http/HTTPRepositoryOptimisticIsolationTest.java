/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.http;

import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryFactory;
import org.eclipse.rdf4j.testsuite.repository.OptimisticIsolationTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author jeen
 *
 */
public class HTTPRepositoryOptimisticIsolationTest extends OptimisticIsolationTest {

	private static HTTPMemServer server;

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");

		server = new HTTPMemServer();
		try {
			server.start();
		} catch (Exception e) {
			server.stop();
			throw e;
		}

		setRepositoryFactory(new HTTPRepositoryFactory() {

			@Override
			public RepositoryImplConfig getConfig() {
				return new HTTPRepositoryConfig(HTTPMemServer.REPOSITORY_URL);
			}

		});
	}

	@AfterClass
	public static void tearDown() throws Exception {
		setRepositoryFactory(null);
		server.stop();
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

}
