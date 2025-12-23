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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.spring.support.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;
import org.mockserver.model.MediaType;
import org.mockserver.model.NottableString;
import org.mockserver.verify.VerificationTimes;

@ExtendWith(MockServerExtension.class)
class RemoteRepositoryConfigTest {

	private final RemoteRepositoryConfig remoteRepositoryConfig = new RemoteRepositoryConfig();

	@BeforeEach
	void setUp(MockServerClient client) throws Exception {
		client.when(
				request()
						.withMethod("GET")
						.withPath("/repositories"),
				Times.once()
		)
				.respond(
						response()
								.withContentType(MediaType.parse("application/sparql-results+json;charset=UTF-8"))
								.withBody(readFileToString("repositories.srj"))
				);
	}

	@Test
	void getRemoteRepository(MockServerClient client) {
		// Arrange
		RemoteRepositoryProperties properties = new RemoteRepositoryProperties();
		properties.setManagerUrl("http://localhost:" + client.getPort());
		properties.setName("test-repo");

		// Act
		Repository repository = remoteRepositoryConfig.getRemoteRepository(properties);

		// Assert
		assertThat(repository).isNotNull();
		client.verify(
				request()
						.withMethod("GET")
						.withPath("/repositories")
						.withHeader(NottableString.not("Authorization")),
				VerificationTimes.once()
		);
	}

	@Test
	void getRemoteRepositoryWithUsernameAndPassword(MockServerClient client) {
		// Arrange
		RemoteRepositoryProperties properties = new RemoteRepositoryProperties();
		properties.setManagerUrl("http://localhost:" + client.getPort());
		properties.setName("test-repo");
		properties.setUsername("admin");
		properties.setPassword("1234");

		// Act
		Repository repository = remoteRepositoryConfig.getRemoteRepository(properties);

		// Assert
		assertThat(repository).isNotNull();
		client.verify(
				request()
						.withMethod("GET")
						.withPath("/repositories")
						.withHeader("Authorization", "Basic YWRtaW46MTIzNA=="),
				VerificationTimes.once()
		);
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

	private String readFileToString(String fileName) throws IOException {
		return IOUtils.resourceToString("__files/" + fileName, StandardCharsets.UTF_8, getClass().getClassLoader());
	}
}
