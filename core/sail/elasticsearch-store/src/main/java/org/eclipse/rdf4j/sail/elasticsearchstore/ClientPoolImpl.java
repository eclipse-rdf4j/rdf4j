/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ClientPoolImpl implements ClientPool {

	transient private Client client;
	private transient boolean closed = false;
	final private String hostname;
	final private int port;

	ClientPoolImpl(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}

	@Override
	public Client getClient() {
		if (client != null) {
			return client;
		}

		synchronized (this) {
			if (closed) {
				throw new IllegalStateException("Elasticsearch Client pool is closed!");
			}
			try {
				Settings settings = Settings.builder().put("cluster.name", "cluster1").build();
				TransportClient client = new PreBuiltTransportClient(settings);
				client.addTransportAddress(new TransportAddress(InetAddress.getByName(hostname), port));
				this.client = client;
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
		}

		return client;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	synchronized public void close() throws Exception {
		if (!closed) {
			closed = true;
			if (client != null) {
				client.close();
				client = null;
			}
		}
	}
}
