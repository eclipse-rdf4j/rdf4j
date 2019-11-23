package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.elasticsearch.client.Client;

public class UnclosableClientPool implements ClientPool {
	private final ClientPoolImpl clientPool;

	public UnclosableClientPool(ClientPoolImpl clientPool) {
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
		// no op
	}
}
