/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.Assume;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers support for Elasticsearch integration tests.
 */
public abstract class ElasticsearchTestContainerSupport {

	private static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:7.15.2";
	private static final String CLUSTER_NAME = "rdf4j-test-cluster";
	private static final int TRANSPORT_PORT = 9300;
	private static final TimeValue HEALTH_TIMEOUT = TimeValue.timeValueSeconds(30);

	private static final Object CONTAINER_LOCK = new Object();
	private static ElasticsearchContainer container;
	private static boolean dockerAvailable = true;
	private static boolean shutdownHookRegistered;

	private static ElasticsearchContainer createContainer() {
		DockerImageName imageName = DockerImageName.parse(ELASTICSEARCH_IMAGE)
				.asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

		ElasticsearchContainer container = new ElasticsearchContainer(imageName)
				.withEnv("cluster.name", CLUSTER_NAME)
				.withEnv("discovery.type", "single-node")
				.withEnv("xpack.security.enabled", "false")
				.withEnv("xpack.security.transport.ssl.enabled", "false")
				.withEnv("xpack.security.http.ssl.enabled", "false")
				.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
				.withStartupTimeout(Duration.ofMinutes(2));
		container.setStartupAttempts(3);
		return container;
	}

	protected TransportClient createTransportClient() throws UnknownHostException {
		ElasticsearchContainer elasticsearchContainer = getOrStartContainer();

		Settings settings = Settings.builder()
				.put("cluster.name", CLUSTER_NAME)
				.put("client.transport.sniff", false)
				.build();

		TransportAddress address = new TransportAddress(
				InetAddress.getByName(elasticsearchContainer.getHost()),
				elasticsearchContainer.getMappedPort(TRANSPORT_PORT));

		TransportClient client = new PreBuiltTransportClient(settings)
				.addTransportAddress(address);

		waitForClusterReadiness(client);
		return client;
	}

	protected String getClusterName() {
		return CLUSTER_NAME;
	}

	protected void waitForClusterReadiness(TransportClient client) {
		long deadline = System.nanoTime() + HEALTH_TIMEOUT.nanos();
		NoNodeAvailableException lastException = null;
		while (System.nanoTime() < deadline) {
			try {
				ClusterHealthResponse health = client.admin()
						.cluster()
						.prepareHealth()
						.setWaitForYellowStatus()
						.setTimeout(TimeValue.timeValueSeconds(30))
						.get();
				if (!health.isTimedOut()) {
					return;
				}
			} catch (NoNodeAvailableException e) {
				lastException = e;
			}

			try {
				Thread.sleep(1_000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while waiting for Elasticsearch testcontainer", e);
			}
		}

		if (lastException != null) {
			throw new IllegalStateException("Elasticsearch test container not reachable", lastException);
		}
		throw new IllegalStateException("Elasticsearch cluster did not reach yellow status within timeout");
	}

	protected static void closeQuietly(TransportClient client) {
		if (client != null) {
			client.close();
		}
	}

	private static ElasticsearchContainer getOrStartContainer() {
		if (!dockerAvailable) {
			Assume.assumeTrue("Docker not available for Elasticsearch tests", false);
		}
		synchronized (CONTAINER_LOCK) {
			if (container == null) {
				ElasticsearchContainer candidate = createContainer();
				try {
					candidate.start();
					container = candidate;
					afterContainerStarted();
					System.out.println("ES Started");
				} catch (Throwable t) {
					dockerAvailable = false;
					Assume.assumeNoException("Docker not available for Elasticsearch tests", t);
				}
			}
			return container;
		}
	}

	private static void afterContainerStarted() {
		if (!shutdownHookRegistered) {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				synchronized (CONTAINER_LOCK) {
					if (container != null && container.isRunning()) {
						container.stop();
						System.out.println("ES Stopped");
					}
				}
			}));
			shutdownHookRegistered = true;
		}
	}
}
