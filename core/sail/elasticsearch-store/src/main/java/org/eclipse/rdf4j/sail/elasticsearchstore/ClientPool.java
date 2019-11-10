package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.elasticsearch.client.Client;

public interface ClientPool extends AutoCloseable {

	Client getClient();

	boolean isClosed();
}
