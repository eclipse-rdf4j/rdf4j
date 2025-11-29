/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.elasticsearch.client.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

public class ClientProviderWithDebugStats implements ClientProvider {

	private transient RestClient lowLevelClient;
	private transient ElasticsearchTransport transport;
	private transient ElasticsearchClient client;
	private transient boolean closed = false;
	private final AtomicLong getClientCalls = new AtomicLong();
	private final AtomicLong bulkCalls = new AtomicLong();

	public ClientProviderWithDebugStats(String hostname, int port, String clusterName) {
		lowLevelClient = RestClient.builder(new org.apache.http.HttpHost(hostname, port, "http"))
				.setHttpClientConfigCallback(this::configureHttpClient)
				.build();
		transport = new RestClientTransport(lowLevelClient, new JacksonJsonpMapper());
		client = new ElasticsearchClient(transport);
	}

	private HttpAsyncClientBuilder configureHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
		return httpClientBuilder.addInterceptorLast((HttpRequest request, HttpContext context) -> {
			if (request instanceof HttpUriRequest) {
				String uri = ((HttpUriRequest) request).getURI().getPath();
				if (uri != null && uri.contains("_bulk")) {
					bulkCalls.incrementAndGet();
				}
			}
		});
	}

	@Override
	public synchronized ElasticsearchClient getClient() {
		getClientCalls.incrementAndGet();
		if (client != null) {
			return client;
		}

		if (closed) {
			throw new IllegalStateException("Elasticsearch Client Provider is closed!");
		}

		return client;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public synchronized void close() {
		if (!closed) {
			closed = true;
			try {
				if (lowLevelClient != null) {
					lowLevelClient.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				lowLevelClient = null;
				transport = null;
				client = null;
			}
		}
	}

	public long getGetClientCalls() {
		return getClientCalls.get();
	}

	public long getBulkCalls() {
		return bulkCalls.get();
	}
}
