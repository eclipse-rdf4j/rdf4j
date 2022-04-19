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
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.eclipse.rdf4j.sail.elasticsearchstore.SingletonClientProvider;
import org.eclipse.rdf4j.sail.elasticsearchstore.TestHelpers;
import org.eclipse.rdf4j.testsuite.repository.SparqlRegexTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class ElasticsearchStoreSparqlRegexIT extends SparqlRegexTest {

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

	@Override
	protected Repository newRepository() throws IOException {
		SailRepository sailRepository = new SailRepository(
				new ElasticsearchStore(clientPool, "index1"));
		return sailRepository;
	}
}
