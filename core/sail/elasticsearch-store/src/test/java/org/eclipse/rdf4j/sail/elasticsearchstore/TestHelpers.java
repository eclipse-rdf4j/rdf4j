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

import java.io.IOException;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

public class TestHelpers {
	public static String CLUSTER = "test";
	public static int PORT = 9200;
	public static String HOST = "localhost";

	private static RestClient LOW_LEVEL_CLIENT;
	private static ElasticsearchTransport TRANSPORT;
	private static ElasticsearchClient CLIENT;

	public static synchronized void openClient() {
		if (CLIENT != null) {
			return;
		}

		ElasticsearchStoreTestContainerSupport.start();

		CLUSTER = ElasticsearchStoreTestContainerSupport.getClusterName();
		PORT = ElasticsearchStoreTestContainerSupport.getHttpPort();
		HOST = ElasticsearchStoreTestContainerSupport.getHost();

		LOW_LEVEL_CLIENT = RestClient
				.builder(new HttpHost(HOST, ElasticsearchStoreTestContainerSupport.getHttpPort(), "http"))
				.build();
		TRANSPORT = new RestClientTransport(LOW_LEVEL_CLIENT, new JacksonJsonpMapper());
		CLIENT = new ElasticsearchClient(TRANSPORT);
	}

	public static ElasticsearchClient getClient() {
		return CLIENT;
	}

	public static void closeClient() throws IOException {
		if (LOW_LEVEL_CLIENT != null) {
			LOW_LEVEL_CLIENT.close();
			LOW_LEVEL_CLIENT = null;
		}
		TRANSPORT = null;
		CLIENT = null;
	}

}
