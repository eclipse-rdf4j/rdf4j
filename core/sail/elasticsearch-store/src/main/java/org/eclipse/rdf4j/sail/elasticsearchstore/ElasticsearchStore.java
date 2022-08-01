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

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStore;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatementHelper;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An RDF4J SailStore persisted to Elasticsearch.
 * </p>
 *
 * <p>
 * This is an EXPERIMENTAL feature. Use at your own risk!
 * </p>
 *
 * <p>
 * There is no write-ahead logging, so a failure during a transaction may result in partially persisted changes.
 * </p>
 *
 *
 * @author Håvard Mikkelsen Ottestad
 */
@Experimental
public class ElasticsearchStore extends ExtensibleStore<ElasticsearchDataStructure, ElasticsearchNamespaceStore> {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStore.class);

	final ClientProvider clientProvider;
	private final AtomicBoolean shutdown = new AtomicBoolean(false);

	private String hostname;
	private int port;
	private String clusterName;
	private String index;

	public ElasticsearchStore(String hostname, int port, String clusterName, String index) {
		this(hostname, port, clusterName, index, true);
	}

	public ElasticsearchStore(String hostname, int port, String clusterName, String index, boolean cacheEnabled) {
		super(cacheEnabled);
		this.hostname = hostname;
		this.port = port;
		this.clusterName = clusterName;
		this.index = index;

		clientProvider = new SingletonClientProvider(hostname, port, clusterName);

		dataStructure = new ElasticsearchDataStructure(clientProvider, index);
		namespaceStore = new ElasticsearchNamespaceStore(clientProvider, index + "_namespaces");

		ReferenceQueue<ElasticsearchStore> objectReferenceQueue = new ReferenceQueue<>();
		startGarbageCollectionMonitoring(objectReferenceQueue, new PhantomReference<>(this, objectReferenceQueue),
				clientProvider);

	}

	public ElasticsearchStore(ClientProvider clientPool, String index) {
		this(clientPool, index, true);
	}

	public ElasticsearchStore(ClientProvider clientPool, String index, boolean cacheEnabled) {
		super(cacheEnabled);
		this.clientProvider = new UnclosableClientProvider(clientPool);

		dataStructure = new ElasticsearchDataStructure(this.clientProvider, index);
		namespaceStore = new ElasticsearchNamespaceStore(this.clientProvider, index + "_namespaces");

	}

	public ElasticsearchStore(Client client, String index) {
		this(client, index, true);
	}

	public ElasticsearchStore(Client client, String index, boolean cacheEnabled) {
		this(new UnclosableClientProvider(new UserProvidedClientProvider(client)), index, cacheEnabled);
	}

	@Override
	protected void initializeInternal() throws SailException {
		if (shutdown.get()) {
			throw new SailException("Can not be initialized after calling shutdown!");
		}
		waitForElasticsearch(10, ChronoUnit.MINUTES);

		super.initializeInternal();
	}

	@Override
	protected void shutDownInternal() throws SailException {
		if (shutdown.compareAndSet(false, true)) {
			super.shutDownInternal();
			try {
				clientProvider.close();
			} catch (Exception e) {
				throw new SailException(e);
			}
		}
	}

	public void waitForElasticsearch(int time, TemporalUnit timeUnit) {

		LocalDateTime tenMinFromNow = LocalDateTime.now().plus(time, timeUnit);

		logger.info("Waiting for Elasticsearch to start");

		while (true) {
			if (LocalDateTime.now().isAfter(tenMinFromNow)) {
				logger.error(
						"Could not connect to Elasticsearch after " + time + " " + timeUnit.toString() + " of trying!");

				try {
					clientProvider.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				throw new RuntimeException(
						"Could not connect to Elasticsearch after " + time + " " + timeUnit.toString() + " of trying!");

			}
			try {
				Client client = clientProvider.getClient();

				ClusterHealthResponse clusterHealthResponse = client.admin()
						.cluster()
						.health(new ClusterHealthRequest())
						.actionGet();
				ClusterHealthStatus status = clusterHealthResponse.getStatus();
				logger.info("Cluster status: {}", status.name());

				if (status.equals(ClusterHealthStatus.GREEN) || status.equals(ClusterHealthStatus.YELLOW)) {
					logger.info("Elasticsearch started!");

					return;

				}

			} catch (Throwable e) {
				logger.info("Unable to connect to elasticsearch cluster due to {}", e.getClass().getSimpleName());

				try {
					clientProvider.close();
				} catch (Exception e2) {
					throw new RuntimeException(e2);
				}
				e.printStackTrace();
			}

			logger.info(".");

			try {
				Thread.sleep(1000);
			} catch (InterruptedException ignored) {

			}
		}

	}

	// this code does some final safety cleanup when the user's ElasticsearchStore gets garbage collected
	private void startGarbageCollectionMonitoring(ReferenceQueue<ElasticsearchStore> referenceQueue,
			Reference<ElasticsearchStore> ref, ClientProvider clientProvider) {

		ExecutorService ex = Executors.newSingleThreadExecutor(r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			// this thread pool does not need to stick around if the all other threads are done
			t.setDaemon(true);
			return t;
		});

		ex.execute(() -> {
			while (referenceQueue.poll() != ref) {
				// don't hang forever
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// should never be interrupted
					break;
				}
			}

			if (ref.get() != null) {
				// we were apparently interrupted before the object was set to be finalized
				return;
			}

			if (!clientProvider.isClosed()) {
				logger.warn(
						"Closing ClientPool in ElasticsearchStore due to store having no references and shutdown() never being called()");
			}

			try {
				clientProvider.close();
			} catch (Exception ignored) {
				// ignoring any exception, since this cleanup is best effort
			}

		});
		// this is a soft operation, the thread pool will actually wait until the task above has completed
		ex.shutdown();
	}

	public void setElasticsearchScrollTimeout(int timeout) {
		dataStructure.setElasticsearchScrollTimeout(timeout);
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new ElasticsearchStoreConnection(this);
	}

	@Override
	public boolean isWritable() throws SailException {
		return true;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public String getClusterName() {
		return clusterName;
	}

	public String getIndex() {
		return index;
	}

	public void setElasticsearchBulkSize(int size) {
		dataStructure.setElasticsearchBulkSize(size);
	}

	@Override
	public ExtensibleStatementHelper getExtensibleStatementHelper() {
		return (ExtensibleStatementHelper) ElasticsearchValueFactory.getInstance();
	}
}
