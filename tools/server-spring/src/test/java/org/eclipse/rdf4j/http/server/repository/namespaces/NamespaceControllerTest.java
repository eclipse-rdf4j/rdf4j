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
package org.eclipse.rdf4j.http.server.repository.namespaces;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

class NamespaceControllerTest {
	private final static String REPO_ID = "test-repo";
	private final NamespaceController controller = new NamespaceController();

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@BeforeEach
	public void setUp() throws Exception {
		request = new MockHttpServletRequest();
		request.setAttribute("repositoryID", REPO_ID);
		request.setAttribute("repository", new SailRepository(new MemoryStore()));
		request.setMethod(HttpMethod.PUT.name());
		response = new MockHttpServletResponse();
	}

	@ParameterizedTest
	@EmptySource
	@ValueSource(strings = { "a", "1", "rdf", "rdf4j", "22" })
	void addNamespace_prefix_ok(String prefix) throws Exception {
		// Arrange
		request.setRequestURI("/repositories/" + REPO_ID + "/namespaces/" + prefix);
		request.setPathInfo(REPO_ID + "/namespaces/" + prefix);
		request.setContent("http://www.w3.org/1999/02/22-rdf-syntax-ns#".getBytes(UTF_8));

		// Act
		final ModelAndView result = controller.handleRequest(request, response);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getView()).isInstanceOf(EmptySuccessView.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "  ", "\t", "\n", "-", "rdf 4j", "rdf-4j" })
	void addNamespace_prefix_invalid(String prefix) {
		// Arrange
		request.setRequestURI("/repositories/" + REPO_ID + "/namespaces/" + prefix);
		request.setPathInfo(REPO_ID + "/namespaces/" + prefix);
		request.setContent("http://www.w3.org/1999/02/22-rdf-syntax-ns#".getBytes(UTF_8));

		// Act & Assert
		assertThatThrownBy(() -> controller.handleRequest(request, response)).isInstanceOf(ClientHTTPException.class)
				.hasMessageContaining("Prefix not alphanumeric");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
			"http://www.w3.org/2001/XMLSchema-instance",
			"http://purl.org/dc/elements/1.1/",
			"http://rdfs.org/ns/void#",
			"https://rdfs.org/ns/void#",
			"ftp://rdfs.org/ns/void",
			"ftps://rdfs.org/ns/void",
			"http://example.org/with%20whitespace",
			"http://",
			"tttt://tttt.ttt",
			"t://tttt",
			"t:tttt",
			"t:"
	})
	void addNamespace_namespaceUri_ok(String namespaceUrl) throws Exception {
		// Arrange
		request.setRequestURI("/repositories/" + REPO_ID + "/namespaces/rdf4j");
		request.setPathInfo(REPO_ID + "/namespaces/rdf4j");
		request.setContent(namespaceUrl.getBytes(UTF_8));

		// Act
		final ModelAndView result = controller.handleRequest(request, response);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getView()).isInstanceOf(EmptySuccessView.class);
	}

	@ParameterizedTest
	@EmptySource
	@ValueSource(strings = { "  ", "\t", "\n" })
	void addNamespace_namespaceUri_empty(String namespaceUrl) {
		// Arrange
		request.setRequestURI("/repositories/" + REPO_ID + "/namespaces/rdf4j");
		request.setPathInfo(REPO_ID + "/namespaces/rdf4j");
		request.setContent(namespaceUrl.getBytes(UTF_8));

		// Act & Assert
		assertThatThrownBy(() -> controller.handleRequest(request, response)).isInstanceOf(ClientHTTPException.class)
				.hasMessageContaining("No namespace name found in request body");
	}

	@ParameterizedTest
	@ValueSource(strings = { "wwww3org", "httpwwww3org", "www.rdf4j.org/ns/void", "t", ":", " :", "\n:\n",
			"http://example.org/with whitespace" })
	void addNamespace_namespaceUri_invalid(String namespaceUrl) {
		// Arrange
		request.setRequestURI("/repositories/" + REPO_ID + "/namespaces/rdf4j");
		request.setPathInfo(REPO_ID + "/namespaces/rdf4j");
		request.setContent(namespaceUrl.getBytes(UTF_8));

		// Act & Assert
		assertThatThrownBy(() -> controller.handleRequest(request, response)).isInstanceOf(ClientHTTPException.class)
				.hasMessageContaining("Namespace not valid");
	}
}
