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
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.eclipse.rdf4j.sail.elasticsearchstore.SingletonClientProvider;
import org.eclipse.rdf4j.sail.elasticsearchstore.TestHelpers;
import org.eclipse.rdf4j.testsuite.sail.SailConcurrencyTest;
import org.eclipse.rdf4j.testsuite.sail.SailInterruptTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * An extension of {@link SailConcurrencyTest} for testing the class
 * {@link org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore}.
 */
public class ElasticsearchStoreInterruptIT extends SailInterruptTest {

	private static SingletonClientProvider clientPool;

	@BeforeAll
	public static void beforeClass() {
		TestHelpers.openClient();
		clientPool = new SingletonClientProvider("localhost", TestHelpers.PORT, TestHelpers.CLUSTER);
	}

	@AfterAll
	public static void afterClass() throws Exception {
		clientPool.close();
		TestHelpers.closeClient();
	}

	@Override
	protected NotifyingSail createSail() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore(clientPool, "index1");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.clear();
			connection.commit();
		}
		return elasticsearchStore;
	}
}
