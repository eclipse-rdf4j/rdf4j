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
import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.client.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class SingletonClientProvider implements ClientProvider {

	private transient RestClient lowLevelClient;
	private transient ElasticsearchTransport transport;
	private transient ElasticsearchClient client;
	private transient boolean closed = false;
	private final String hostname;
	private final int port;
	private final String clusterName;

	public SingletonClientProvider(String hostname, int port, String clusterName) {
		this.hostname = hostname;
		this.port = port;
		this.clusterName = clusterName;
	}

	@Override
	public ElasticsearchClient getClient() {
		if (client != null) {
			return client;
		}

		synchronized (this) {
			if (closed) {
				throw new IllegalStateException("Elasticsearch Client Provider is closed!");
			}

			lowLevelClient = RestClient.builder(new HttpHost(hostname, port, "http")).build();
			transport = new RestClientTransport(lowLevelClient, new JacksonJsonpMapper());
			client = new ElasticsearchClient(transport);

		}

		return client;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	synchronized public void close() {
		if (!closed) {
			closed = true;
			try {
				if (lowLevelClient != null) {
					lowLevelClient.close();
				}
			} catch (IOException e) {
				throw new SailException(e);
			} finally {
				lowLevelClient = null;
				transport = null;
				client = null;
			}
		}
	}
}
