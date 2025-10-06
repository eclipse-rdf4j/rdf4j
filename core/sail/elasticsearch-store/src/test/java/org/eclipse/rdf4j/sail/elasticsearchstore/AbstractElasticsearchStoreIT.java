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

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractElasticsearchStoreIT {

	private static final Logger logger = LoggerFactory.getLogger(AbstractElasticsearchStoreIT.class);

	private static boolean dockerAvailable;

	@BeforeAll
	public static void beforeClass() {
		dockerAvailable = TestHelpers.openClient();
	}

	@AfterAll
	public static void afterClass() throws IOException {
		if (dockerAvailable) {
			TestHelpers.closeClient();
		}
	}

	@BeforeEach
	public void requireDocker() {
		Assumptions.assumeTrue(dockerAvailable, "Docker not available for Elasticsearch tests");
	}

	@AfterEach
	public void after() throws IOException {
		if (!dockerAvailable) {
			return;
		}
		TestHelpers.getClient().indices().refresh(new RefreshRequest("*"), RequestOptions.DEFAULT);
		printAllDocs();
		deleteAllIndexes();
	}

	protected void printAllDocs() throws IOException {
		for (String index : getIndexes()) {
			if (!index.equals(".geoip_databases")) {
				logger.info("INDEX: " + index);
				SearchResponse res = TestHelpers.getClient()
						.search(new SearchRequest(index), RequestOptions.DEFAULT);
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
				TestHelpers.getClient().getLowLevelClient().performRequest(new Request("DELETE", "/" + index));
			}
		}
	}

	protected String[] getIndexes() throws IOException {
		if (!dockerAvailable) {
			return new String[0];
		}
		GetIndexRequest request = new GetIndexRequest("*");
		try {
			if (!TestHelpers.getClient().indices().exists(request, RequestOptions.DEFAULT)) {
				return new String[0];
			}
			GetIndexResponse response = TestHelpers.getClient().indices().get(request, RequestOptions.DEFAULT);
			return response.getIndices();
		} catch (ElasticsearchStatusException e) {
			return new String[0];
		}
	}
}
