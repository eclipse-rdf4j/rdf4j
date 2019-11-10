package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.elasticsearch.client.Client;

import java.io.Closeable;

public interface ClientPool extends AutoCloseable {

	Client getClient();

}
