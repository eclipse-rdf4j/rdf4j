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
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.List;

public class ElasticsearchStore extends AbstractNotifyingSail implements FederatedServiceResolverClient {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStore.class);

	private ElasticsearchSailStore sailStore;
	private String hostname;
	private int port;

	public ElasticsearchStore(String hostname, int port, String index) {

		sailStore = new ElasticsearchSailStore(hostname, port, index);
		this.hostname = hostname;
		this.port = port;
	}

	@Override
	protected void initializeInternal() throws SailException {
		waitForElasticsearch(10, ChronoUnit.MINUTES);
		sailStore.init();
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		return Collections.singletonList(IsolationLevels.NONE);
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		return IsolationLevels.NONE;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {

	}

	@Override
	protected void shutDownInternal() throws SailException {
		sailStore.close();

	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new ElasticsearchStoreConnection(this, sailStore, getEvaluationStrategyFactory());
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
				logger.error("Could not connect to Elasticsearch after 10 minutes of trying!");

				throw new RuntimeException("Could not connect to Elasticsearch after 10 minutes of trying!");

			}
			try {
				Settings settings = Settings.builder().put("cluster.name", "cluster1").build();
				ClusterHealthResponse clusterHealthResponse;
				try (TransportClient client = new PreBuiltTransportClient(settings)) {
					client.addTransportAddress(new TransportAddress(InetAddress.getByName(hostname), port));

					ClusterHealthRequest request = new ClusterHealthRequest();

					clusterHealthResponse = client.admin().cluster().health(request).actionGet();
					ClusterHealthStatus status = clusterHealthResponse.getStatus();
					logger.info("Cluster status: {}", status.name());

					if (status.equals(ClusterHealthStatus.GREEN) || status.equals(ClusterHealthStatus.YELLOW)) {
						logger.info("Elasticsearch started!");
						return;

					}
				}

			} catch (Throwable e) {
				logger.info("Unable to connect to elasticsearch cluster due to {}", e.getClass().getSimpleName());
				e.printStackTrace();
			}

			logger.info(".");

			try {
				Thread.sleep(1000);
			} catch (InterruptedException ignored) {

			}
		}

	}
}
