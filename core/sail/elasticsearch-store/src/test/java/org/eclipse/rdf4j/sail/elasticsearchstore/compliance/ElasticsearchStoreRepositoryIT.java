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

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStoreTestContainerSupport;
import org.eclipse.rdf4j.sail.elasticsearchstore.SingletonClientProvider;
import org.eclipse.rdf4j.sail.elasticsearchstore.TestHelpers;
import org.eclipse.rdf4j.testsuite.repository.RepositoryTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ElasticsearchStoreRepositoryIT extends RepositoryTest {

	@Container
	private static final GenericContainer<?> elasticsearch = ElasticsearchStoreTestContainerSupport.getContainer();

	private static SingletonClientProvider clientPool;

	@BeforeAll
	public static void beforeClass() {
		TestHelpers.openClient();
		clientPool = new SingletonClientProvider(TestHelpers.HOST, TestHelpers.PORT, TestHelpers.CLUSTER);
	}

	@AfterAll
	public static void afterClass() throws Exception {
		clientPool.close();
		TestHelpers.closeClient();
	}

	@Override
	protected Repository createRepository() {
		SailRepository sailRepository = new SailRepository(
				new ElasticsearchStore(clientPool, "index1"));
		return sailRepository;
	}

	@Disabled
	@Override
	public void testShutdownFollowedByInit() {
		// ignore test
	}

}
