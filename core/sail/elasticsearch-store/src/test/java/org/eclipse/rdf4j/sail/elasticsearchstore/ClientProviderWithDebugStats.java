package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientProviderWithDebugStats implements ClientProvider {

	transient private ClientWithStats client;
	private transient boolean closed = false;
	private String hostname;
	private int port;
	private String clusterName;
	private long getClientCalls;

	public ClientProviderWithDebugStats(String hostname, int port, String clusterName) {
		this.hostname = hostname;
		this.port = port;
		this.clusterName = clusterName;

		try {
			Settings settings = Settings.builder().put("cluster.name", clusterName).build();
			TransportClient client = new PreBuiltTransportClient(settings);
			client.addTransportAddress(new TransportAddress(InetAddress.getByName(hostname), port));
			this.client = new ClientWithStats(client);
		} catch (UnknownHostException e) {
			throw new SailException(e);
		}
	}

	@Override
	synchronized public Client getClient() {
		getClientCalls++;
		if (client != null) {
			return client;
		}

		synchronized (this) {
			if (closed) {
				throw new IllegalStateException("Elasticsearch Client Provider is closed!");
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

	public long getGetClientCalls() {
		return getClientCalls;
	}

	public long getBulkCalls() {
		return client.bulkCalls;
	}
}
