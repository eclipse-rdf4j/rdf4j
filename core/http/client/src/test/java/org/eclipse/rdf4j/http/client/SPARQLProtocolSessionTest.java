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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.http.client.spi.HttpHeader;
import org.eclipse.rdf4j.http.client.spi.HttpResponse;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClients;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLStarResultsXMLWriter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;
import org.mockserver.model.MediaType;

/**
 * Unit tests for {@link SPARQLProtocolSession}
 *
 * @author Jeen Broekstra
 */
@ExtendWith(MockServerExtension.class)
public class SPARQLProtocolSessionTest {
	SPARQLProtocolSession sparqlSession;
	SharedHttpClientSessionManager sessionManager;

	String serverURL;
	String repositoryID = "test";
	String factoryName;

	static Stream<String> httpClientFactories() {
		return Stream.of("jdk", "apache5");
	}

	SPARQLProtocolSession createProtocolSession() {
		RDF4JHttpClient httpClient = RDF4JHttpClients.factory(factoryName).create();
		sessionManager = new SharedHttpClientSessionManager(httpClient, Executors.newCachedThreadPool());
		SPARQLProtocolSession session = sessionManager.createRDF4JProtocolSession(serverURL);
		session.setQueryURL(Protocol.getRepositoryLocation(serverURL, repositoryID));
		session.setUpdateURL(
				Protocol.getStatementsLocation(Protocol.getRepositoryLocation(serverURL, repositoryID)));
		return session;
	}

	@BeforeEach
	public void setUp(MockServerClient client) {
		serverURL = "http://localhost:" + client.getPort() + "/rdf4j-server";
		client.reset();
	}

	@AfterEach
	public void tearDown() {
		if (sparqlSession != null) {
			sparqlSession.close();
			sparqlSession = null;
		}
		if (sessionManager != null) {
			sessionManager.shutDown();
			sessionManager = null;
		}
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testConnectionTimeoutRetry(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();

		Assumptions.assumeTrue("apache5".equals(factoryName),
				"Retry on HTTP 408 is only supported by the Apache HC5 client");

		// Simulate that the server wants to close the connection after idle timeout
		// But instead of just shutting down the connection it sends `408` once and then
		// shuts down the connection.
		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test"),
				Times.once()
		)
				.respond(
						response()
								.withStatusCode(408)
				);
		// When the request is retried (with a refreshed connection) server sends `200`
		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test"),
				Times.once()
		)
				.respond(
						response()
								.withBody(readFileToString("repository-list.xml"))
								.withContentType(MediaType.parse(TupleQueryResultFormat.SPARQL.getDefaultMIMEType()))
				);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TupleQueryResultHandler handler = Mockito.spy(new SPARQLStarResultsJSONWriter(out));
		// We only send the query once, internally the retry handler makes sure the first 408 response causes
		// a retry. From user perspective it just looks like everything went fine, the closed connection is gracefully
		// refreshed.
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);
		assertThat(out.toString()).startsWith("{");
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testConnectionPoolTimeoutRetry(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();

		Assumptions.assumeTrue("apache5".equals(factoryName),
				"Retry on HTTP 408 is only supported by the Apache HC5 client");

		// Let 2 connections succeed, this is just so we can fill the connection pool with more than one connection
		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test"),
				Times.exactly(2)
		)
				.respond(
						response()
								.withBody(readFileToString("repository-list.xml"))
								.withContentType(MediaType.parse(TupleQueryResultFormat.SPARQL.getDefaultMIMEType()))
				);

		// Next, simulate that both connections in the pool were idled out on the server and upon sending a request
		// on them the server returns a 408 for both of them
		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test"),
				Times.exactly(2)
		)
				.respond(
						response()
								.withStatusCode(408)
				);

		// When both connections in the pool were cleaned up the next try goes through ok
		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test"),
				Times.once()
		)
				.respond(
						response()
								.withBody(readFileToString("repository-list.xml"))
								.withContentType(MediaType.parse(TupleQueryResultFormat.SPARQL.getDefaultMIMEType()))
				);

		// First fill the pool with 2 connections
		ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		TupleQueryResultHandler handler1 = Mockito.spy(new SPARQLStarResultsJSONWriter(out1));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1,
				handler1);
		ByteArrayOutputStream out2 = new ByteArrayOutputStream();
		TupleQueryResultHandler handler2 = Mockito.spy(new SPARQLStarResultsJSONWriter(out2));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1,
				handler2);
		assertThat(out1.toString()).startsWith("{");
		assertThat(out2.toString()).startsWith("{");

		// When trying another `sendTupleQuery` the 2 pooled connections fail with a 408. Both are cleaned up
		// and finally a fresh connection is opened and goes through successfully
		ByteArrayOutputStream out3 = new ByteArrayOutputStream();
		TupleQueryResultHandler handler3 = Mockito.spy(new SPARQLStarResultsJSONWriter(out3));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1,
				handler3);
		assertThat(out3.toString()).startsWith("{");
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testTupleQuery_NoPassthrough(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();

		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test"),
				Times.once()
		)
				.respond(
						response()
								.withBody(readFileToString("repository-list.xml"))
								.withContentType(MediaType.parse(TupleQueryResultFormat.SPARQL.getDefaultMIMEType()))
				);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TupleQueryResultHandler handler = Mockito.spy(new SPARQLStarResultsJSONWriter(out));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);

		// If not passed through, the QueryResultWriter methods should have been invoked
		verify(handler, times(1)).startQueryResult(anyList());

		// check that the OutputStream received content in JSON format
		assertThat(out.toString()).startsWith("{");
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testTupleQuery_Passthrough(String factoryName, MockServerClient client) throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();

		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test"),
				Times.once()
		)
				.respond(
						response()
								.withBody(readFileToString("repository-list.xml"))
								.withContentType(MediaType.parse(TupleQueryResultFormat.SPARQL.getDefaultMIMEType()))
				);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SPARQLStarResultsXMLWriter handler = Mockito.spy(new SPARQLStarResultsXMLWriter(out));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);

		// SPARQL-star XML sink should accept SPARQL/XML data and pass directly to OutputStream
		verify(handler, never()).startQueryResult(anyList());

		// check that the OutputStream received content in XML format
		assertThat(out.toString()).startsWith("<");
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("httpClientFactories")
	public void testTupleQuery_Passthrough_ConfiguredFalse(String factoryName, MockServerClient client)
			throws Exception {
		this.factoryName = factoryName;
		this.sparqlSession = createProtocolSession();

		client.when(
				request()
						.withMethod("POST")
						.withPath("/rdf4j-server/repositories/test"),
				Times.once()
		)
				.respond(
						response()
								.withBody(readFileToString("repository-list.xml"))
								.withContentType(MediaType.parse(TupleQueryResultFormat.SPARQL.getDefaultMIMEType()))
				);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SPARQLStarResultsXMLWriter handler = Mockito.spy(new SPARQLStarResultsXMLWriter(out));
		sparqlSession.setPassThroughEnabled(false);
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);

		// If not passed through, the QueryResultWriter methods should have been invoked
		verify(handler, times(1)).startQueryResult(anyList());

		// check that the OutputStream received content in XML format
		assertThat(out.toString()).startsWith("<");
	}

	@Test
	public void getContentTypeSerialisationTest() {
		{
			HttpResponse httpResponse = withContentType("application/shacl-validation-report+n-quads");
			RDFFormat format = SPARQLProtocolSession.getContentTypeSerialisation(httpResponse);

			assertThat(format).isEqualTo(RDFFormat.NQUADS);
		}

		{
			HttpResponse httpResponse = withContentType("application/shacl-validation-report+ld+json");
			RDFFormat format = SPARQLProtocolSession.getContentTypeSerialisation(httpResponse);

			assertThat(format).isEqualTo(RDFFormat.JSONLD);
		}

		{
			HttpResponse httpResponse = withContentType("text/shacl-validation-report+turtle");
			RDFFormat format = SPARQLProtocolSession.getContentTypeSerialisation(httpResponse);

			assertThat(format).isEqualTo(RDFFormat.TURTLE);
		}
	}

	/* private methods */

	private HttpResponse withContentType(String contentType) {
		HttpHeader header = HttpHeader.of("Content-Type", contentType);
		return new HttpResponse() {
			@Override
			public int getStatusCode() {
				return 200;
			}

			@Override
			public String getReasonPhrase() {
				return "OK";
			}

			@Override
			public List<HttpHeader> getHeaders() {
				return List.of(header);
			}

			@Override
			public InputStream getBodyAsStream() throws IOException {
				return InputStream.nullInputStream();
			}

			@Override
			public void discard() throws IOException {
			}

			@Override
			public void close() {
			}
		};
	}

	protected String readFileToString(String fileName) throws IOException {
		return IOUtils.resourceToString("__files/" + fileName, StandardCharsets.UTF_8, getClass().getClassLoader());
	}
}
