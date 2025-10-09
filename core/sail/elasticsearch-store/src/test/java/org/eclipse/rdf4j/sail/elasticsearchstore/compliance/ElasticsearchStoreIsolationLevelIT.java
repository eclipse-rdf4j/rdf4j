/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore.compliance;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.eclipse.rdf4j.sail.elasticsearchstore.SingletonClientProvider;
import org.eclipse.rdf4j.sail.elasticsearchstore.TestHelpers;
import org.eclipse.rdf4j.testsuite.sail.SailIsolationLevelTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * An extension of {@link SailIsolationLevelTest} for testing the class
 * {@link org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore}.
 */
public class ElasticsearchStoreIsolationLevelIT extends SailIsolationLevelTest {

	private static SingletonClientProvider clientPool;
	private static boolean dockerAvailable;

	@BeforeAll
	public static void beforeClass() {
		SailIsolationLevelTest.setUpClass();
		dockerAvailable = TestHelpers.openClient();
		if (!dockerAvailable) {
			return;
		}
		clientPool = new SingletonClientProvider(TestHelpers.getHost(), TestHelpers.getTransportPort(),
				TestHelpers.getClusterName());
	}

	@BeforeEach
	public void requireDocker() {
		Assumptions.assumeTrue(dockerAvailable, "Docker not available for Elasticsearch tests");
	}

	@AfterAll
	public static void afterClass2() throws Exception {
		SailIsolationLevelTest.afterClass();
		if (clientPool != null) {
			clientPool.close();
			clientPool = null;
		}
		if (dockerAvailable) {
			TestHelpers.closeClient();
		}
	}

	@Override
	protected Sail createSail() throws SailException {
		NotifyingSail sail = new ElasticsearchStore(clientPool, "index1");
		try (NotifyingSailConnection connection = sail.getConnection()) {
			connection.begin();
			connection.clear();
			connection.commit();
		}
		return sail;
	}

}
