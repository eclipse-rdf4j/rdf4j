/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.http;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.MediaType;

@ExtendWith(MockServerExtension.class)
public class HTTPRepositoryConnectionTest {

	static HTTPRepository testRepository;
	static RDF4JProtocolSession session;

	@BeforeAll
	static void configureMockServer(MockServerClient client) {
		client.when(
				request()
						.withMethod("GET")
						.withPath("/Socrates")
		)
				.respond(
						response()
								.withContentType(MediaType.parse(RDFFormat.TURTLE.getDefaultMIMEType()))
								.withBody("<http://example.org/Socrates> a <http://xmlns.com/foaf/0.1/Person> .")

				);

		client.when(
				request()
						.withMethod("GET")
						.withPath("/Socrates.ttl")
		)
				.respond(
						response()
								.withContentType(MediaType.WILDCARD)
								.withBody("<http://example.org/Socrates> a <http://xmlns.com/foaf/0.1/Person> .")

				);

		client.when(
				request()
						.withMethod("GET")
						.withPath("/Plato")
		)
				.respond(
						response()
								.withContentType(MediaType.WILDCARD)
								.withBody("<http://example.org/Socrates> a <http://xmlns.com/foaf/0.1/Person> .")

				);
	}

	@BeforeAll
	static void configureHTTPRepository(MockServerClient client) {
		session = mock(RDF4JProtocolSession.class);
		testRepository = mock(HTTPRepository.class);
	}

	@Test
	public void testAddFromURL_FormatFromMimetype(MockServerClient client) throws Exception {
		URL url = new URL("http://localhost:" + client.getPort() + "/Socrates");
		try (HTTPRepositoryConnection repoConn = new HTTPRepositoryConnection(testRepository, session)) {
			repoConn.add(url);
		}
		verify(session).upload(any(InputStream.class), eq(url.toExternalForm()), eq(RDFFormat.TURTLE), anyBoolean(),
				anyBoolean());
	}

	@Test
	public void testAddFromURL_FormatFromFilename(MockServerClient client) throws Exception {
		URL url = new URL("http://localhost:" + client.getPort() + "/Socrates.ttl");
		try (HTTPRepositoryConnection repoConn = new HTTPRepositoryConnection(testRepository, session)) {
			repoConn.add(url);
		}
		verify(session).upload(any(InputStream.class), eq(url.toExternalForm()), eq(RDFFormat.TURTLE), anyBoolean(),
				anyBoolean());
	}

	@Test
	public void testAddFromURL_FormatUndetermined(MockServerClient client) throws Exception {
		URL url = new URL("http://localhost:" + client.getPort() + "/Plato");
		try (HTTPRepositoryConnection repoConn = new HTTPRepositoryConnection(testRepository, session)) {
			assertThatExceptionOfType(UnsupportedRDFormatException.class).isThrownBy(() -> {
				repoConn.add(url);
			}).withMessageContaining("Could not find RDF format for URL: " + url.toExternalForm());
		}
	}

}
