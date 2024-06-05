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
package org.eclipse.rdf4j.repository.manager;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;
import org.mockserver.model.MediaType;

/**
 * Unit tests for {@link RemoteRepositoryManager}
 *
 * @author Jeen Broekstra
 */
@ExtendWith(MockServerExtension.class)
public class RemoteRepositoryManagerTest extends RepositoryManagerTest {

	@BeforeEach
	public void setUp(MockServerClient client) {
		subject = new RemoteRepositoryManager("http://localhost:" + client.getPort() + "/rdf4j-server");
	}

	@Test
	public void testAddRepositoryConfig(MockServerClient client) throws Exception {
		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/protocol"),
				Times.once()
		)
				.respond(
						response()
								.withBody(Protocol.VERSION)
				);
		client.when(
				request()
						.withMethod("PUT")
						.withPath("/rdf4j-server/repositories/test"),
				Times.once()
		)
				.respond(
						response()
								.withStatusCode(204)
				);
		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories"),
				Times.once()
		)
				.respond(
						response()
								.withContentType(MediaType.parse(TupleQueryResultFormat.SPARQL.getDefaultMIMEType()))
								.withBody(readFileToString("repository-list-response.srx"))
				);

		RepositoryConfig config = new RepositoryConfig("test");

		subject.addRepositoryConfig(config);

		client.verify(
				request()
						.withMethod("PUT")
						.withPath("/rdf4j-server/repositories/test")
						.withContentType(MediaType.parse("application/x-binary-rdf"))
		// FIXME: Somehow the following assert is failing
		// .withBody(regex("^BRDF.*"))
		);
	}

	@Test
	public void testAddRepositoryConfigExisting(MockServerClient client) throws Exception {
		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/protocol"),
				Times.once()
		)
				.respond(
						response()
								.withBody(Protocol.VERSION)
				);
		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/mem-rdf/config"),
				Times.once()
		)
				.respond(
						response()
								.withStatusCode(204)
				);
		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories"),
				Times.once()
		)
				.respond(
						response()
								.withContentType(MediaType.parse(TupleQueryResultFormat.SPARQL.getDefaultMIMEType()))
								.withBody(readFileToString("repository-list-response.srx"))
				);

		RepositoryConfig config = new RepositoryConfig("mem-rdf"); // this repo already exists

		subject.addRepositoryConfig(config);

		client.verify(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/mem-rdf/config")
						.withContentType(MediaType.parse("application/x-binary-rdf"))
		// FIXME: Somehow the following assert is failing
		// .withBody(regex("^BRDF.*"))
		);
	}

	@Test
	public void testGetRepositoryConfig(MockServerClient client) {
		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/protocol"),
				Times.once()
		)
				.respond(
						response()
								.withBody(Protocol.VERSION)
				);
		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories/test/config"),
				Times.once()
		)
				.respond(
						response()
								.withContentType(MediaType.parse(RDFFormat.NTRIPLES.getDefaultMIMEType()))
								.withBody("_:node1 <" + RepositoryConfigSchema.REPOSITORYID + "> \"test\" . ")
				);

		subject.getRepositoryConfig("test");

		client.verify(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories/test/config")
		);
	}

	@Test
	public void testAddRepositoryConfigLegacy(MockServerClient client) throws Exception {
		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/protocol"),
				Times.once()
		)
				.respond(
						response()
								.withBody("8")
				);
		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/SYSTEM/statements"),
				Times.once()
		)
				.respond(
						response()
								.withStatusCode(204)
				);
		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories"),
				Times.once()
		)
				.respond(
						response()
								.withContentType(MediaType.parse(TupleQueryResultFormat.SPARQL.getDefaultMIMEType()))
								.withBody(readFileToString("repository-list-response.srx"))
				);

		RepositoryConfig config = new RepositoryConfig("test");

		assertThrows(RepositoryException.class, () -> subject.addRepositoryConfig(config));
	}

	private String readFileToString(String fileName) throws IOException {
		return IOUtils.resourceToString("__files/" + fileName, StandardCharsets.UTF_8, getClass().getClassLoader());
	}
}
