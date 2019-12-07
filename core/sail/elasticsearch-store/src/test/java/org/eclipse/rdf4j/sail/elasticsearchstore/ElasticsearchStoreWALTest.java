/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.assertj.core.util.Files;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// Tests transaction failures that the Write-Ahead-Log should be able to recover from
public class ElasticsearchStoreWALTest {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStoreWALTest.class);
	private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private static EmbeddedElastic embeddedElastic;

	private static File installLocation = Files.newTemporaryFolder();

	@BeforeClass
	public static void beforeClass() throws IOException, InterruptedException {

		embeddedElastic = TestHelpers.startElasticsearch(installLocation);
	}

	@AfterClass
	public static void afterClass() throws IOException {

		TestHelpers.stopElasticsearch(embeddedElastic, installLocation);
	}

	@After
	public void after() throws UnknownHostException {

		embeddedElastic.refreshIndices();

		deleteAllIndexes();

	}

	@Before
	public void before() throws UnknownHostException {
//		embeddedElastic.refreshIndices();
//
//		embeddedElastic.deleteIndices();

	}

	private void deleteAllIndexes() {
		for (String index : getIndexes()) {
			System.out.println("deleting: " + index);
			embeddedElastic.deleteIndex(index);

		}
	}

	private String[] getIndexes() {

		Settings settings = Settings.builder().put("cluster.name", "cluster1").build();
		try (TransportClient client = new PreBuiltTransportClient(settings)) {
			client.addTransportAddress(
					new TransportAddress(InetAddress.getByName("localhost"), embeddedElastic.getTransportTcpPort()));

			return client.admin()
					.indices()
					.getIndex(new GetIndexRequest())
					.actionGet()
					.getIndices();
		} catch (UnknownHostException e) {
			throw new IllegalStateException(e);
		}

	}

	@Ignore // No WAL implemented yet
	@Test
	public void testAddLargeDataset() {

		boolean transactionFaild = false;
		int count = 100000;
		try {
			failedTransactionAdd(count);
		} catch (Exception e) {
			System.out.println(e.getClass().getName());
			transactionFaild = true;
		}

		assertTrue(transactionFaild);

		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", embeddedElastic.getTransportTcpPort(), "cluster1", "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			long size = connection.size();
			System.out.println(size);
			assertEquals("Since transaction failed there should be no statements in the store", 0, size);

		}

	}

	private void failedTransactionAdd(int count) {
		ClientProviderWithDebugStats clientProvider = new ClientProviderWithDebugStats("localhost",
				embeddedElastic.getTransportTcpPort(), "cluster1");

		ElasticsearchStore es = new ElasticsearchStore(clientProvider, "testindex");
		SailRepository elasticsearchStore = new SailRepository(es);

		es.setElasticsearchBulkSize(1024);

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			connection.begin(IsolationLevels.READ_COMMITTED);
			for (int i = 0; i < count; i++) {
				connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i));
			}

			Thread thread = new Thread(() -> {
				try {
					while (clientProvider.getBulkCalls() < 3) {
						Thread.sleep(1);
					}
					clientProvider.close();
				} catch (Exception ignored) {

				}
			});
			thread.start();

			connection.commit();

		}

	}

	@Ignore // No WAL implemented yet
	@Test
	public void testRemoveLargeDataset() {

		int count = 100000;

		fill(count);

		boolean transactionFaild = false;
		try {
			failedTransactionRemove();
		} catch (Exception e) {
			System.out.println(e.getClass().getName());
			transactionFaild = true;
		}

		assertTrue(transactionFaild);

		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", embeddedElastic.getTransportTcpPort(), "cluster1", "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			long size = connection.size();
			System.out.println(size);
			assertEquals("Since transaction failed there should be no statements in the store", count, size);

		}

	}

	private void fill(int count) {
		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", embeddedElastic.getTransportTcpPort(), "cluster1", "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			connection.begin(IsolationLevels.READ_COMMITTED);
			for (int i = 0; i < count; i++) {
				connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i));
			}

			connection.commit();

		}
	}

	private void failedTransactionRemove() {
		ClientProviderWithDebugStats clientProvider = new ClientProviderWithDebugStats("localhost",
				embeddedElastic.getTransportTcpPort(), "cluster1");

		ElasticsearchStore es = new ElasticsearchStore(clientProvider, "testindex");
		SailRepository elasticsearchStore = new SailRepository(es);

		es.setElasticsearchBulkSize(1024);

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			connection.begin(IsolationLevels.READ_COMMITTED);

			connection.clear();

			long bulkCalls = clientProvider.getBulkCalls();
			Thread thread = new Thread(() -> {
				try {
					while (clientProvider.getBulkCalls() < bulkCalls + 3) {
						Thread.sleep(1);
					}
					clientProvider.close();
				} catch (Exception ignored) {

				}
			});
			thread.start();

			connection.commit();

		}

	}

}
