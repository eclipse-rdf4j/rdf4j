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
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.TupleQueryResultTest;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.eclipse.rdf4j.sail.elasticsearchstore.SingletonClientProvider;
import org.eclipse.rdf4j.sail.elasticsearchstore.TestHelpers;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

public class ElasticsearchStoreTupleQueryResultIT extends TupleQueryResultTest {

	private static EmbeddedElastic embeddedElastic;

	private static File installLocation = Files.newTemporaryFolder();
	private static SingletonClientProvider clientPool;

	@BeforeClass
	public static void beforeClass() throws IOException, InterruptedException {

		embeddedElastic = TestHelpers.startElasticsearch(installLocation);
		clientPool = new SingletonClientProvider("localhost", embeddedElastic.getTransportTcpPort(), "cluster1");

	}

	@AfterClass
	public static void afterClass() throws Exception {

		clientPool.close();
		TestHelpers.stopElasticsearch(embeddedElastic, installLocation);

	}

	@Override
	protected Repository newRepository() throws IOException {
		SailRepository sailRepository = new SailRepository(new ElasticsearchStore(clientPool, "index1"));
		return sailRepository;
	}
}
