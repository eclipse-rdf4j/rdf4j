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
import org.eclipse.rdf4j.sail.elasticsearchstore.SingletonClientProvider;
import org.eclipse.rdf4j.sail.elasticsearchstore.TestHelpers;
import org.eclipse.rdf4j.testsuite.repository.SparqlOrderByTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class ElasticsearchStoreSparqlOrderByIT extends SparqlOrderByTest {

	private static SingletonClientProvider clientPool;
	private static boolean dockerAvailable;

	@BeforeAll
	public static void beforeClass() {
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
	public static void afterClass() throws Exception {
		if (clientPool != null) {
			clientPool.close();
			clientPool = null;
		}
		if (dockerAvailable) {
			TestHelpers.closeClient();
		}
	}

	@Override
	protected Repository newRepository() {
		SailRepository sailRepository = new SailRepository(
				new ElasticsearchStore(clientPool, "index1"));
		return sailRepository;
	}

}
