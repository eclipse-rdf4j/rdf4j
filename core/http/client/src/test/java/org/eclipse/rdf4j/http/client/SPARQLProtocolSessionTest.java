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
import static org.mockserver.model.ConnectionOptions.connectionOptions;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.params.HttpParams;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLStarResultsXMLWriter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

	String serverURL;
	String repositoryID = "test";

	SPARQLProtocolSession createProtocolSession() {
		SPARQLProtocolSession session = new SharedHttpClientSessionManager().createRDF4JProtocolSession(serverURL);
		session.setQueryURL(Protocol.getRepositoryLocation(serverURL, repositoryID));
		session.setUpdateURL(
				Protocol.getStatementsLocation(Protocol.getRepositoryLocation(serverURL, repositoryID)));
		return session;
	}

	@BeforeEach
	public void setUp(MockServerClient client) throws Exception {
		serverURL = "http://localhost:" + client.getPort() + "/rdf4j-server";
		sparqlSession = createProtocolSession();
	}

	@Test
	public void testConnectionTimeoutRetry(MockServerClient client) throws Exception {
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
								.withConnectionOptions(connectionOptions().withCloseSocket(true))
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

	@Test
	public void testConnectionPoolTimeoutRetry(MockServerClient client) throws Exception {
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
								.withConnectionOptions(connectionOptions().withCloseSocket(true))
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

	@Test
	public void testTupleQuery_NoPassthrough(MockServerClient client) throws Exception {
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

	@Test
	public void testTupleQuery_Passthrough(MockServerClient client) throws Exception {
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

	@Test
	public void testTupleQuery_Passthrough_ConfiguredFalse(MockServerClient client) throws Exception {
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
		Header header = new Header() {
			@Override
			public String getName() {
				return null;
			}

			@Override
			public String getValue() {
				return null;
			}

			@Override
			public HeaderElement[] getElements() throws ParseException {

				HeaderElement[] elements = { new HeaderElement() {
					@Override
					public String getName() {
						return contentType;
					}

					@Override
					public String getValue() {
						return null;
					}

					@Override
					public NameValuePair[] getParameters() {
						return new NameValuePair[0];
					}

					@Override
					public NameValuePair getParameterByName(String name) {
						return null;
					}

					@Override
					public int getParameterCount() {
						return 0;
					}

					@Override
					public NameValuePair getParameter(int index) {
						return null;
					}
				} };
				return elements;
			}
		};

		return new HttpResponse() {
			@Override
			public ProtocolVersion getProtocolVersion() {
				return null;
			}

			@Override
			public boolean containsHeader(String name) {
				return false;
			}

			@Override
			public Header[] getHeaders(String name) {
				Header[] headers = { header };
				return headers;
			}

			@Override
			public Header getFirstHeader(String name) {
				return null;
			}

			@Override
			public Header getLastHeader(String name) {
				return null;
			}

			@Override
			public Header[] getAllHeaders() {
				return new Header[0];
			}

			@Override
			public void addHeader(Header header1) {

			}

			@Override
			public void addHeader(String name, String value) {

			}

			@Override
			public void setHeader(Header header1) {

			}

			@Override
			public void setHeader(String name, String value) {

			}

			@Override
			public void setHeaders(Header[] headers) {

			}

			@Override
			public void removeHeader(Header header1) {

			}

			@Override
			public void removeHeaders(String name) {

			}

			@Override
			public HeaderIterator headerIterator() {
				return null;
			}

			@Override
			public HeaderIterator headerIterator(String name) {
				return null;
			}

			@Override
			public HttpParams getParams() {
				return null;
			}

			@Override
			public void setParams(HttpParams params) {

			}

			@Override
			public StatusLine getStatusLine() {
				return null;
			}

			@Override
			public void setStatusLine(StatusLine statusline) {

			}

			@Override
			public void setStatusLine(ProtocolVersion ver, int code) {

			}

			@Override
			public void setStatusLine(ProtocolVersion ver, int code, String reason) {

			}

			@Override
			public void setStatusCode(int code) throws IllegalStateException {

			}

			@Override
			public void setReasonPhrase(String reason) throws IllegalStateException {

			}

			@Override
			public HttpEntity getEntity() {
				return null;
			}

			@Override
			public void setEntity(HttpEntity entity) {

			}

			@Override
			public Locale getLocale() {
				return null;
			}

			@Override
			public void setLocale(Locale loc) {

			}
		};
	}

	protected String readFileToString(String fileName) throws IOException {
		return IOUtils.resourceToString("__files/" + fileName, StandardCharsets.UTF_8, getClass().getClassLoader());
	}
}
