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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class SingletonClientProvider implements ClientProvider {

	transient private Client client;
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
	public Client getClient() {
		if (client != null) {
			return client;
		}

		synchronized (this) {
			if (closed) {
				throw new IllegalStateException("Elasticsearch Client Provider is closed!");
			}

			try {
				Settings settings = Settings.builder().put("cluster.name", clusterName).build();
				TransportClient client = new PreBuiltTransportClient(settings);
				client.addTransportAddress(new TransportAddress(InetAddress.getByName(hostname), port));
				this.client = client;
			} catch (UnknownHostException e) {
				throw new SailException(e);
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
				Client temp = client;
				client = null;
				temp.close();
			}
		}
	}
}
