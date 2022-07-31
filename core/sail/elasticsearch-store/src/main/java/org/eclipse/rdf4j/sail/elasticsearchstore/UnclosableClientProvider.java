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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UnclosableClientProvider implements ClientProvider {

	private static final Logger logger = LoggerFactory.getLogger(UnclosableClientProvider.class);

	private final ClientProvider clientPool;

	public UnclosableClientProvider(ClientProvider clientPool) {
		this.clientPool = clientPool;
	}

	@Override
	public Client getClient() {
		return clientPool.getClient();
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void close() throws Exception {

		logger.debug("Client was provided by user and was not closed.");
		// no op
	}
}
