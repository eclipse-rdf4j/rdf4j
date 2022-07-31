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

import org.elasticsearch.client.Client;

/**
 * Used by the user to provide an Elasticsearch Client to the ElasticsearchStore instead of providing host, port,
 * cluster information. The client provided by the user is not closed by the ElasticsearchStore.
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class UserProvidedClientProvider implements ClientProvider {

	final private Client client;

	transient boolean closed;

	public UserProvidedClientProvider(Client client) {
		this.client = client;
	}

	@Override
	public Client getClient() {
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
		}
	}
}
