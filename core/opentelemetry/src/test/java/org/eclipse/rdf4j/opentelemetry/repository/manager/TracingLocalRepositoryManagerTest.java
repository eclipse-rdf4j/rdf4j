/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.opentelemetry.repository.manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;

class TracingLocalRepositoryManagerTest {

	private static final String REPOSITORY_ID = "test-repo";

	@RegisterExtension
	static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

	private LocalRepositoryManager manager;

	@AfterEach
	void tearDown() {
		System.clearProperty(OpenTelemetrySettings.ENABLED_PROPERTY);
		if (manager != null) {
			manager.shutDown();
		}
	}

	private LocalRepositoryManager createManager(File baseDir) {
		RDF4JOpenTelemetryConfig config = RDF4JOpenTelemetryConfig.builder()
				.openTelemetry(otelTesting.getOpenTelemetry())
				.build();
		manager = new TracingLocalRepositoryManager(baseDir, config);
		manager.init();
		manager.addRepositoryConfig(
				new RepositoryConfig(REPOSITORY_ID, new SailRepositoryConfig(new MemoryStoreConfig())));
		return manager;
	}

	@Test
	void disabled_returnsPlainRepository(@TempDir File baseDir) {
		createManager(baseDir);

		Repository repository = manager.getRepository(REPOSITORY_ID);

		try (RepositoryConnection conn = repository.getConnection()) {
			BooleanQuery query = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK { ?s ?p ?o }");
			query.evaluate();
		}

		assertThat(otelTesting.getSpans()).isEmpty();
	}

	@Test
	void enabled_wrapsRepositoryAndRecordsSpans(@TempDir File baseDir) {
		System.setProperty(OpenTelemetrySettings.ENABLED_PROPERTY, "true");
		createManager(baseDir);

		Repository repository = manager.getRepository(REPOSITORY_ID);

		assertThat(repository.getClass().getName())
				.isEqualTo("org.eclipse.rdf4j.opentelemetry.repository.TracingRepository");

		try (RepositoryConnection conn = repository.getConnection()) {
			BooleanQuery query = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK { ?s ?p ?o }");
			query.evaluate();
		}

		List<SpanData> spans = otelTesting.getSpans();
		assertThat(spans).hasSize(1);
		assertThat(spans.get(0).getName()).isEqualTo("ASK " + REPOSITORY_ID);
	}

	@Test
	void enabled_returnsSameCachedInstanceOnRepeatedLookup(@TempDir File baseDir) {
		System.setProperty(OpenTelemetrySettings.ENABLED_PROPERTY, "true");
		createManager(baseDir);

		Repository first = manager.getRepository(REPOSITORY_ID);
		Repository second = manager.getRepository(REPOSITORY_ID);

		assertThat(first).isSameAs(second);
	}
}
