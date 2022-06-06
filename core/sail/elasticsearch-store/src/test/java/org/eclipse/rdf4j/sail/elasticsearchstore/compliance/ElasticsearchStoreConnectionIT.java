/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore.compliance;

import java.io.File;
import java.io.IOException;

import org.assertj.core.util.Files;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.eclipse.rdf4j.sail.elasticsearchstore.SingletonClientProvider;
import org.eclipse.rdf4j.sail.elasticsearchstore.TestHelpers;
import org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized;

public class ElasticsearchStoreConnectionIT extends RepositoryConnectionTest {

	public ElasticsearchStoreConnectionIT(IsolationLevel level) {
		super(level);
	}

	private static final File installLocation = Files.newTemporaryFolder();
	private static ElasticsearchClusterRunner runner;
	private static SingletonClientProvider clientPool;

	@BeforeClass
	public static void beforeClass() throws IOException, InterruptedException {
		runner = TestHelpers.startElasticsearch(installLocation);
		clientPool = new SingletonClientProvider("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		clientPool.close();
		TestHelpers.stopElasticsearch(runner);
	}

	@Parameterized.Parameters(name = "{0}")
	public static IsolationLevel[] parameters() {
		return new IsolationLevel[] {
				IsolationLevels.NONE,
				IsolationLevels.READ_UNCOMMITTED,
				IsolationLevels.READ_COMMITTED
		};
	}

	@Override
	protected Repository createRepository() {
		return new SailRepository(
				new ElasticsearchStore(clientPool, "index1"));
	}
}
