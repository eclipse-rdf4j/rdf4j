/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractElasticsearchStoreIT {

	private static final Logger logger = LoggerFactory.getLogger(AbstractElasticsearchStoreIT.class);

	@BeforeAll
	public static void beforeClass() {
		TestHelpers.openClient();
	}

	@AfterAll
	public static void afterClass() throws IOException {
		TestHelpers.closeClient();
	}

	@AfterEach
	public void after() throws IOException {
		TestHelpers.getClient().indices().refresh(Requests.refreshRequest("*"), RequestOptions.DEFAULT);
		printAllDocs();
		deleteAllIndexes();
	}

	protected void printAllDocs() throws IOException {
		for (String index : getIndexes()) {
			if (!index.equals(".geoip_databases")) {
				logger.info("INDEX: " + index);
				SearchResponse res = TestHelpers.getClient()
						.search(Requests.searchRequest(index), RequestOptions.DEFAULT);
				SearchHits hits = res.getHits();
				for (SearchHit hit : hits) {
					logger.info(" doc " + hit.getSourceAsString());
				}
			}
		}
	}

	protected void deleteAllIndexes() throws IOException {
		for (String index : getIndexes()) {
			if (!index.equals(".geoip_databases")) {
				logger.info("deleting index: " + index);
				TestHelpers.getClient().indices().delete(Requests.deleteIndexRequest(index), RequestOptions.DEFAULT);
			}
		}
	}

	protected String[] getIndexes() {
		Settings settings = Settings.builder().put("cluster.name", TestHelpers.CLUSTER).build();
		try (TransportClient client = new PreBuiltTransportClient(settings)) {
			client.addTransportAddress(
					new TransportAddress(InetAddress.getByName("localhost"), TestHelpers.PORT));

			return client.admin()
					.indices()
					.getIndex(new GetIndexRequest())
					.actionGet()
					.getIndices();
		} catch (UnknownHostException e) {
			throw new IllegalStateException(e);
		}
	}
}
