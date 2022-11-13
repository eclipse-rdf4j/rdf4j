/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.spring.repository.remote;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.spring.support.ConfigurationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

class RemoteRepositoryConfigTest {

	private final RemoteRepositoryConfig remoteRepositoryConfig = new RemoteRepositoryConfig();
	private static final WireMockServer wireMockServer = new WireMockServer(options()
			.dynamicPort()
			.usingFilesUnderClasspath("src/test/resources/"));

	@BeforeAll
	static void setUp() {
		wireMockServer.start();
		wireMockServer.stubFor(get(urlEqualTo("/repositories"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/sparql-results+json;charset=UTF-8")
						.withBodyFile("repositories.srj")));
	}

	@Test
	void getRemoteRepository() {
		// Arrange
		RemoteRepositoryProperties properties = new RemoteRepositoryProperties();
		properties.setManagerUrl(wireMockServer.baseUrl());
		properties.setName("test-repo");

		// Act
		Repository repository = remoteRepositoryConfig.getRemoteRepository(properties);

		// Assert
		assertThat(repository).isNotNull();
		wireMockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/repositories"))
				.withoutHeader("Authorization"));
	}

	@Test
	void getRemoteRepositoryWithUsernameAndPassword() {
		// Arrange
		RemoteRepositoryProperties properties = new RemoteRepositoryProperties();
		properties.setManagerUrl(wireMockServer.baseUrl());
		properties.setName("test-repo");
		properties.setUsername("admin");
		properties.setPassword("1234");

		// Act
		Repository repository = remoteRepositoryConfig.getRemoteRepository(properties);

		// Assert
		assertThat(repository).isNotNull();
		wireMockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/repositories"))
				.withHeader("Authorization", equalTo("Basic YWRtaW46MTIzNA==")));
	}

	@Test
	void getRemoteRepository_error() {
		// Arrange
		RemoteRepositoryProperties properties = new RemoteRepositoryProperties();
		properties.setManagerUrl("https://unknown-host:8888");
		properties.setName("test-repo");

		// Act & Assert
		assertThatExceptionOfType(ConfigurationException.class)
				.isThrownBy(() -> remoteRepositoryConfig.getRemoteRepository(properties));
	}

	@AfterAll
	static void tearDown() {
		wireMockServer.stop();
	}
}
