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
package org.eclipse.rdf4j.sail.elasticsearchstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchStoreIT extends AbstractElasticsearchStoreIT {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStoreIT.class);

	@Test
	public void testInstantiate() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost",
				TestHelpers.PORT, TestHelpers.CLUSTER, "testindex");
		elasticsearchStore.shutDown();
	}

	@Test
	public void testGetConnection() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost",
				TestHelpers.PORT, TestHelpers.CLUSTER, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
		}
		elasticsearchStore.shutDown();

	}

	@Test
	public void testSailRepository() {
		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.PORT, TestHelpers.CLUSTER, "testindex"));
		elasticsearchStore.shutDown();
	}

	@Test
	public void testGetSailRepositoryConnection() {
		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.PORT, TestHelpers.CLUSTER, "testindex"));
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
		}
		elasticsearchStore.shutDown();
	}

	@Test
	public void testShutdownAndRecreate() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost",
				TestHelpers.PORT, TestHelpers.CLUSTER, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();
		elasticsearchStore = new ElasticsearchStore("localhost", TestHelpers.PORT, TestHelpers.CLUSTER,
				"testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();

	}

	@Test
	public void testShutdownAndReinit() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost",
				TestHelpers.PORT, TestHelpers.CLUSTER, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();

		assertThrows(SailException.class, () -> elasticsearchStore.getConnection());
	}

	@Test
	public void testAddRemoveData() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost",
				TestHelpers.PORT, TestHelpers.CLUSTER, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
			connection.begin(IsolationLevels.NONE);
			connection.removeStatements(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

			List<? extends Statement> statements = Iterations.asList(connection.getStatements(null, null, null, true));
			assertEquals(0, statements.size());

		}
		elasticsearchStore.shutDown();

	}

	@Test
	public void testAddLargeDataset() {
		StopWatch stopWatch = StopWatch.createStarted();
		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.PORT, TestHelpers.CLUSTER, "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			stopWatch.stop();

			ElasticsearchStoreTransactionsIT.logTime(stopWatch, "Creating repo and getting connection",
					TimeUnit.SECONDS);

			stopWatch = StopWatch.createStarted();
			connection.begin(IsolationLevels.NONE);
			int count = 100000;
			for (int i = 0; i < count; i++) {
				connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i));
			}
			connection.commit();
			stopWatch.stop();
			ElasticsearchStoreTransactionsIT.logTime(stopWatch, "Adding data", TimeUnit.SECONDS);

			stopWatch = StopWatch.createStarted();
			assertEquals(count, connection.size());
			stopWatch.stop();
			ElasticsearchStoreTransactionsIT.logTime(stopWatch, "Getting size", TimeUnit.SECONDS);

		}

	}

	@Test
	public void testGC() {

		ClientProvider clientProvider = initElasticsearchStoreForGcTest();

		for (int i = 0; i < 100 && !clientProvider.isClosed(); i++) {
			System.gc();
			try {
				Thread.sleep(i * 100);
			} catch (InterruptedException ignored) {
			}
		}

		assertTrue(clientProvider.isClosed());

	}

	private ClientProvider initElasticsearchStoreForGcTest() {
		ElasticsearchStore sail = new ElasticsearchStore("localhost", TestHelpers.PORT, TestHelpers.CLUSTER,
				"testindex");

		ClientProvider clientProvider = sail.clientProvider;
		SailRepository elasticsearchStore = new SailRepository(sail);

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("label"));
		}
		return clientProvider;
	}

	@Test
	public void testNamespacePersistenc() {

		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.PORT, TestHelpers.CLUSTER, "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
			connection.commit();
		}

		elasticsearchStore.shutDown();
		elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.PORT, TestHelpers.CLUSTER, "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			String namespace = connection.getNamespace(SHACL.PREFIX);
			assertEquals(SHACL.NAMESPACE, namespace);
		}
	}

}
