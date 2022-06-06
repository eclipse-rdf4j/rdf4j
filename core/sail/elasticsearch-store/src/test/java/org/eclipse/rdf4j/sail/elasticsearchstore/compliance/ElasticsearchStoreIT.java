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
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.eclipse.rdf4j.sail.elasticsearchstore.SingletonClientProvider;
import org.eclipse.rdf4j.sail.elasticsearchstore.TestHelpers;
import org.eclipse.rdf4j.testsuite.sail.RDFNotifyingStoreTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * An extension of RDFStoreTest for testing the class
 * <var>org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore</var>.
 */
public class ElasticsearchStoreIT extends RDFNotifyingStoreTest {

	/*---------*
	 * Methods *
	 *---------*/

	private static final File installLocation = Files.newTemporaryFolder();
	private static ElasticsearchClusterRunner runner;
	static SingletonClientProvider clientPool;

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
	protected NotifyingSail createSail() throws SailException {
		NotifyingSail sail = new ElasticsearchStore(clientPool, "index1");
		try (NotifyingSailConnection connection = sail.getConnection()) {
			connection.begin();
			connection.clear();
			connection.commit();
		}
		return sail;
	}
}
