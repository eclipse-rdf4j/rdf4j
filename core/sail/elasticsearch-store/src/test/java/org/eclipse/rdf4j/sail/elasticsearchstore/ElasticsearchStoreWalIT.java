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

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Tests transaction failures that the Write-Ahead-Log should be able to recover from
public class ElasticsearchStoreWalIT extends AbstractElasticsearchStoreIT {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStoreWalIT.class);

	/*
	 * @After public void after() throws IOException { client.indices().refresh(Requests.refreshRequest("*"),
	 * RequestOptions.DEFAULT); deleteAllIndexes(); }
	 */

	@Disabled // No WAL implemented yet
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
				new ElasticsearchStore("localhost", TestHelpers.PORT, TestHelpers.CLUSTER, "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			long size = connection.size();
			System.out.println(size);
			assertEquals("Since transaction failed there should be no statements in the store", 0, size);

		}

	}

	private void failedTransactionAdd(int count) {
		ClientProviderWithDebugStats clientProvider = new ClientProviderWithDebugStats("localhost",
				TestHelpers.PORT, TestHelpers.CLUSTER);

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

	@Disabled // No WAL implemented yet
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
				new ElasticsearchStore("localhost", TestHelpers.PORT, TestHelpers.CLUSTER, "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			long size = connection.size();
			System.out.println(size);
			assertEquals("Since transaction failed there should be no statements in the store", count, size);

		}

	}

	private void fill(int count) {
		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.PORT, TestHelpers.CLUSTER, "testindex"));

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
				TestHelpers.PORT, TestHelpers.CLUSTER);

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
