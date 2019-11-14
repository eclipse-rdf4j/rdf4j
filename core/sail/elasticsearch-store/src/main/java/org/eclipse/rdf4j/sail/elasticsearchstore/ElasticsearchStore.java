/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class ElasticsearchStore extends AbstractNotifyingSail implements FederatedServiceResolverClient {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStore.class);

	private final ElasticsearchSailStore sailStore;

	final ClientPool clientPool;

	public ElasticsearchStore(String hostname, int port, String index) {

		clientPool = new ClientPoolImpl(hostname, port);

		sailStore = new ElasticsearchSailStore(hostname, port, index, clientPool);

		ReferenceQueue<ElasticsearchStore> objectReferenceQueue = new ReferenceQueue<>();
		startGarbageCollectionMonitoring(objectReferenceQueue, new PhantomReference<>(this, objectReferenceQueue),
				clientPool);

	}

	ElasticsearchSailStore getSailStore() {
		return sailStore;
	}

	@Override
	protected void initializeInternal() throws SailException {
		waitForElasticsearch(10, ChronoUnit.MINUTES);
		sailStore.init();
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		return Arrays.asList(IsolationLevels.NONE, IsolationLevels.READ_UNCOMMITTED, IsolationLevels.READ_COMMITTED);
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		return IsolationLevels.READ_COMMITTED;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {

	}

	@Override
	protected void shutDownInternal() throws SailException {
		sailStore.close();
		try {
			clientPool.close();
		} catch (Exception e) {
			throw new SailException(e);
		}

	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new ElasticsearchStoreConnection(this);
	}

	@Override
	public boolean isWritable() throws SailException {
		return false;
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	private EvaluationStrategyFactory evalStratFactory;

	public synchronized EvaluationStrategyFactory getEvaluationStrategyFactory() {
		if (evalStratFactory == null) {
			evalStratFactory = new StrictEvaluationStrategyFactory(getFederatedServiceResolver());
		}
		evalStratFactory.setQuerySolutionCacheThreshold(0);
		return evalStratFactory;
	}

	/**
	 * independent life cycle
	 */
	private FederatedServiceResolver serviceResolver;

	/**
	 * dependent life cycle
	 */
	private SPARQLServiceResolver dependentServiceResolver;

	public synchronized FederatedServiceResolver getFederatedServiceResolver() {
		if (serviceResolver == null) {
			if (dependentServiceResolver == null) {
				dependentServiceResolver = new SPARQLServiceResolver();
			}
			setFederatedServiceResolver(dependentServiceResolver);
		}
		return serviceResolver;
	}

	public void setEvaluationStrategyFactory(EvaluationStrategyFactory evalStratFactory) {
		this.evalStratFactory = evalStratFactory;

	}

	public void waitForElasticsearch(int time, TemporalUnit timeUnit) {

		LocalDateTime tenMinFromNow = LocalDateTime.now().plus(time, timeUnit);

		logger.info("Waiting for Elasticsearch to start");

		while (true) {
			if (LocalDateTime.now().isAfter(tenMinFromNow)) {
				logger.error(
						"Could not connect to Elasticsearch after " + time + " " + timeUnit.toString() + " of trying!");

				try {
					clientPool.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				throw new RuntimeException(
						"Could not connect to Elasticsearch after " + time + " " + timeUnit.toString() + " of trying!");

			}
			try {
				Client client = clientPool.getClient();

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
					clientPool.close();
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
			Reference<ElasticsearchStore> ref, ClientPool clientPool) {

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

			if (!clientPool.isClosed()) {
				logger.warn(
						"Closing ClientPool in ElasticsearchStore due to store having no references and shutdown() never being called()");
			}

			try {
				clientPool.close();
			} catch (Exception ignored) {
				// ignoring any exception, since this cleanup is best effort
			}

		});
		// this is a soft operation, the thread pool will actually wait until the task above has completed
		ex.shutdown();
	}

	public void setElasticsearchScrollTimeout(int timeout) {
		sailStore.setElasticsearchScrollTimeout(timeout);
	}

}
