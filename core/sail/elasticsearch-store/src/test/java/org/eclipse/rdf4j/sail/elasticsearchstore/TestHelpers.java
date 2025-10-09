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
package org.eclipse.rdf4j.sail.elasticsearchstore;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.junit.jupiter.api.Assumptions;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared helper for Elasticsearch store integration tests.
 */
public final class TestHelpers {

	private static final String CLUSTER = "rdf4j-elasticsearch-store-test";
	private static final int DEFAULT_HTTP_PORT = 9200;
	private static final int DEFAULT_TRANSPORT_PORT = 9300;
	private static final DockerImageName ELASTICSEARCH_IMAGE = DockerImageName
			.parse("docker.elastic.co/elasticsearch/elasticsearch:7.15.2")
			.asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

	private static ElasticsearchContainer container;
	private static RestHighLevelClient client;

	private TestHelpers() {
		// static helper
	}

	public static synchronized boolean openClient() {
		if (client != null) {
			return true;
		}

		if (!ensureContainerStarted()) {
			return false;
		}

		client = new RestHighLevelClient(RestClient.builder(
				new HttpHost(container.getHost(), container.getMappedPort(DEFAULT_HTTP_PORT), "http")));

		if (!waitForClusterReadiness()) {
			closeQuietly();
			return false;
		}

		return true;
	}

	private static boolean ensureContainerStarted() {
		if (container != null) {
			return true;
		}

		ElasticsearchContainer candidate = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
				.withEnv("cluster.name", CLUSTER)
				.withEnv("discovery.type", "single-node")
				.withEnv("xpack.security.enabled", "false")
				.withEnv("xpack.security.transport.ssl.enabled", "false")
				.withEnv("xpack.security.http.ssl.enabled", "false")
				.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
				.withStartupTimeout(Duration.ofMinutes(2));
		candidate.setStartupAttempts(3);

		try {
			candidate.start();
			container = candidate;
			return true;
		} catch (Throwable t) {
			container = null;
			return false;
		}
	}

	private static boolean waitForClusterReadiness() {
		long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(2);
		ClusterHealthRequest request = new ClusterHealthRequest().waitForYellowStatus();
		request.timeout(TimeValue.timeValueSeconds(30));
		Exception last = null;

		while (System.nanoTime() < deadline) {
			try {
				ClusterHealthResponse response = client.cluster().health(request, RequestOptions.DEFAULT);
				if (!response.isTimedOut()) {
					return true;
				}
			} catch (Exception e) {
				last = e;
			}

			try {
				Thread.sleep(1_000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				closeQuietly();
				throw new IllegalStateException("Interrupted while waiting for Elasticsearch test container", e);
			}
		}

		closeQuietly();
		return false;
	}

	private static synchronized void closeQuietly() {
		if (client != null) {
			try {
				client.close();
			} catch (IOException ignored) {
			}
			client = null;
		}
		if (container != null) {
			container.stop();
			container = null;
		}
	}

	public static synchronized RestHighLevelClient getClient() {
		if (!openClient()) {
			throw new IllegalStateException("Elasticsearch test container not started");
		}
		return client;
	}

	public static synchronized void closeClient() throws IOException {
		if (client != null) {
			client.close();
			client = null;
		}
		if (container != null) {
			container.stop();
			container = null;
		}
	}

	public static synchronized String getHost() {
		if (!openClient()) {
			throw new IllegalStateException("Elasticsearch test container not started");
		}
		return container.getHost();
	}

	public static synchronized int getHttpPort() {
		if (!openClient()) {
			throw new IllegalStateException("Elasticsearch test container not started");
		}
		return container.getMappedPort(DEFAULT_HTTP_PORT);
	}

	public static synchronized int getTransportPort() {
		if (!openClient()) {
			throw new IllegalStateException("Elasticsearch test container not started");
		}
		return container.getMappedPort(DEFAULT_TRANSPORT_PORT);
	}

	public static String getClusterName() {
		return CLUSTER;
	}

	public static ElasticsearchStore createStore(String index) {
		return new ElasticsearchStore(getHost(), getTransportPort(), getClusterName(), index);
	}
}
