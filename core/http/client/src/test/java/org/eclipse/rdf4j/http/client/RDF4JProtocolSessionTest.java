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
package org.eclipse.rdf4j.http.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClients;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

/**
 * Unit tests for {@link RDF4JProtocolSession}
 *
 * @author Jeen Broekstra
 */
@ExtendWith(MockServerExtension.class)
public class RDF4JProtocolSessionTest extends SPARQLProtocolSessionTest {

	private final String testHeader = "X-testing-header";
	private final String testValue = "foobar";

	RDF4JProtocolSession getRDF4JSession() {
		return (RDF4JProtocolSession) sparqlSession;
	}

	@Override
	RDF4JProtocolSession createProtocolSession() {
		RDF4JHttpClient httpClient = RDF4JHttpClients.factory(factoryName).create();
		RDF4JProtocolSession session = new SharedHttpClientSessionManager(httpClient, Executors.newCachedThreadPool())
				.createRDF4JProtocolSession(serverURL);
		session.setRepository(Protocol.getRepositoryLocation(serverURL, repositoryID));
		HashMap<String, String> additionalHeaders = new HashMap<>();
		additionalHeaders.put(testHeader, testValue);
		session.setAdditionalHttpHeaders(additionalHeaders);
		return session;
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testCreateRepositoryExecutesPut(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();

		client.when(
				request()
						.withMethod("PUT")
						.withPath("/rdf4j-server/repositories/test"),
				Times.once()
		)
				.respond(
						response()
				);
		RepositoryConfig config = new RepositoryConfig("test");
		getRDF4JSession().createRepository(config);
		client.verify(
				request()
						.withMethod("PUT")
						.withPath("/rdf4j-server/repositories/test")
						.withHeader(testHeader, testValue)
		);
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testCreateRepositoryFollowsRedirectOnPut(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();
		Assumptions.assumeTrue("apache5".equals(factoryName),
				"Method-preserving redirect on PUT is only supported by the Apache HC5 client");

		// Simulate reverse-proxy forcing redirect on state-changing PUT
		String originalPath = "/rdf4j-server/repositories/test";
		String redirectedPath = "/https/rdf4j-server/repositories/test";
		String redirectLocation = "http://localhost:" + client.getPort() + redirectedPath;

		// First request responds with 301 and Location header
		client.when(
				request()
						.withMethod("PUT")
						.withPath(originalPath),
				Times.once()
		)
				.respond(
						response()
								.withStatusCode(301)
								.withHeader("Location", redirectLocation)
				);

		// Redirect target responds successfully
		client.when(
				request()
						.withMethod("PUT")
						.withPath(redirectedPath),
				Times.once()
		)
				.respond(
						response()
				);

		RepositoryConfig config = new RepositoryConfig("test");

		// Expected: client should follow the 301 redirect and succeed without throwing
		getRDF4JSession().createRepository(config);

		// Verify both the original and redirected requests were made with additional headers preserved
		client.verify(
				request()
						.withMethod("PUT")
						.withPath(originalPath)
						.withHeader(testHeader, testValue)
		);
		client.verify(
				request()
						.withMethod("PUT")
						.withPath(redirectedPath)
						.withHeader(testHeader, testValue)
		);
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testRemoveDataTransactionFollowsRedirectOnDelete(String factoryName, MockServerClient client)
			throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();
		Assumptions.assumeTrue("apache5".equals(factoryName),
				"Method-preserving redirect on PUT is only supported by the Apache HC5 client");

		// Start transaction and get transaction URL
		String transactionStartUrl = Protocol.getTransactionsLocation(getRDF4JSession().getRepositoryURL());
		HttpRequest transactionCreateRequest = request()
				.withMethod("POST")
				.withPath("/rdf4j-server/repositories/test/transactions");
		client.when(transactionCreateRequest, Times.once())
				.respond(response().withStatusCode(201).withHeader("Location", transactionStartUrl + "/1"));

		// First attempt: PUT .../transactions/1?action=DELETE responds with 301 and Location header
		String originalPath = "/rdf4j-server/repositories/test/transactions/1";
		String redirectedPath = "/https/rdf4j-server/repositories/test/transactions/1";
		String redirectLocation = "http://localhost:" + client.getPort() + redirectedPath + "?action=DELETE";

		client.when(
				request()
						.withMethod("PUT")
						.withPath(originalPath)
						.withQueryStringParameter("action", "DELETE"),
				Times.once())
				.respond(response().withStatusCode(301).withHeader("Location", redirectLocation));

		// Redirect target responds successfully (204 No Content)
		client.when(
				request()
						.withMethod("PUT")
						.withPath(redirectedPath)
						.withQueryStringParameter("action", "DELETE"),
				Times.once())
				.respond(response().withStatusCode(204));

		// Begin transaction, then attempt removeData (DELETE action) which should follow redirect
		getRDF4JSession().beginTransaction(IsolationLevels.SERIALIZABLE);
		ByteArrayInputStream data = new ByteArrayInputStream("<s> <p> <o> .".getBytes(StandardCharsets.UTF_8));
		getRDF4JSession().removeData(data, null, RDFFormat.NTRIPLES);

		// Verify original and redirected requests occurred with header preserved
		client.verify(
				request()
						.withMethod("PUT")
						.withPath(originalPath)
						.withQueryStringParameter("action", "DELETE")
						.withHeader(testHeader, testValue)
		);
		client.verify(
				request()
						.withMethod("PUT")
						.withPath(redirectedPath)
						.withQueryStringParameter("action", "DELETE")
						.withHeader(testHeader, testValue)
		);
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testUpdateRepositoryExecutesPost(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();

		RepositoryConfig config = new RepositoryConfig("test");

		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test/config"),
				Times.once()
		)
				.respond(
						response()
				);

		getRDF4JSession().updateRepository(config);

		client.verify(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test/config")
						.withHeader(testHeader, testValue)
		);
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testSize(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();

		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories/test/size"),
				Times.once()
		)
				.respond(
						response()
								.withBody("8")
				);

		assertThat(getRDF4JSession().size()).isEqualTo(8);
		client.verify(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories/test/size")
						.withHeader(testHeader, testValue)
		);
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testGetRepositoryConfig(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();

		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories/test/config"),
				Times.once()
		)
				.respond(
						response()
								.withBody(readFileToString("repository-config.nt"))
								.withContentType(MediaType.parse(RDFFormat.NTRIPLES.getDefaultMIMEType()))
				);

		StatementCollector collector = new StatementCollector();
		getRDF4JSession().getRepositoryConfig(collector);
		assertThat(collector.getStatements())
				.isNotEmpty();

		client.verify(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories/test/config")
						.withHeader(testHeader, testValue)
		);
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testRepositoryList(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();

		client.when(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories"),
				Times.once()
		)
				.respond(
						response()
								.withBody(readFileToString("repository-list.xml"))
								.withContentType(MediaType.parse(TupleQueryResultFormat.SPARQL.getDefaultMIMEType()))
				);

		assertThat(getRDF4JSession().getRepositoryList().getBindingNames()).contains("id");
		client.verify(
				request()
						.withMethod("GET")
						.withPath("/rdf4j-server/repositories")
						.withHeader(testHeader, testValue)
		);
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testClose(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		// re-init protocol session with cache-timeout set
		System.setProperty(Protocol.CACHE_TIMEOUT_PROPERTY, "1");
		sparqlSession = createProtocolSession();

		String transactionStartUrl = Protocol.getTransactionsLocation(getRDF4JSession().getRepositoryURL());

		HttpRequest transactionCreateRequest = request()
				.withMethod("POST")
				.withPath("/rdf4j-server/repositories/test/transactions");
		HttpRequest transactionPingRequest = request()
				.withMethod("POST")
				.withPath("/rdf4j-server/repositories/test/transactions/1")
				.withQueryStringParameter("action", "PING");
		client.when(transactionCreateRequest, Times.once())
				.respond(
						response()
								.withStatusCode(201)
								.withHeader("Location", transactionStartUrl + "/1")
				);
		client.when(transactionPingRequest)
				.respond(
						response()
								.withBody("2000")
				);

		getRDF4JSession().beginTransaction(IsolationLevels.SERIALIZABLE);
		Thread.sleep(2000);

		client.verify(
				transactionPingRequest,
				VerificationTimes.exactly(2)
		);

		getRDF4JSession().close();
		sparqlSession = null;
		Thread.sleep(1000);

		// we should not have received any further pings after the session was closed.
		client.verify(
				transactionPingRequest,
				VerificationTimes.exactly(2)
		);
	}
}
