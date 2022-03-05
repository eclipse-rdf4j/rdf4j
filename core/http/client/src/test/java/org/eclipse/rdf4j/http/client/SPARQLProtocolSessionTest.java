/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

/**
 * Unit tests for {@link SPARQLProtocolSession}
 *
 * @author Jeen Broekstra
 */
public class SPARQLProtocolSessionTest {

	public static WireMockServer wireMockServer;

	@BeforeAll
	public static void setPort() {
		wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
		wireMockServer.start();
		configureFor(wireMockServer.port());
	}

	@AfterAll
	public static void closeMockPort() {
		wireMockServer.stop();
	}

	SPARQLProtocolSession sparqlSession;

	String testHeader = "X-testing-header";
	String testValue = "foobar";

	String serverURL = "http://localhost:" + wireMockServer.port() + "/rdf4j-server";
	String repositoryID = "test";

	SPARQLProtocolSession createProtocolSession() {
		SPARQLProtocolSession session = new SharedHttpClientSessionManager().createRDF4JProtocolSession(serverURL);
		session.setQueryURL(Protocol.getRepositoryLocation(serverURL, repositoryID));
		session.setUpdateURL(
				Protocol.getStatementsLocation(Protocol.getRepositoryLocation(serverURL, repositoryID)));
		HashMap<String, String> additionalHeaders = new HashMap<>();
		additionalHeaders.put(testHeader, testValue);
		session.setAdditionalHttpHeaders(additionalHeaders);
		return session;
	}

	@BeforeEach
	public void setUp() throws Exception {
		sparqlSession = createProtocolSession();
	}

	@Test
	public void testConnectionTimeoutRetry() throws Exception {
		// Simulate that the server wants to close the connection after idle timeout
		// But instead of just shutting down the connection it sends `408` once and then
		// shuts down the connection.
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("Connection Timeout")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse().withStatus(408)
						.withHeader(HttpHeaders.CONNECTION, "close")
						.withStatusMessage("Server closed inactive connection"))
				.willSetStateTo("Connection closed"));

		// When the request is retried (with a refreshed connection) server sends `200`
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("Connection Timeout")
				.whenScenarioStateIs("Connection closed")
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBodyFile("repository-list.xml"))
				.willSetStateTo("Connection reopened"));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TupleQueryResultHandler handler = Mockito.spy(new SPARQLStarResultsJSONWriter(out));
		// We only send the query once, internally the retry handler makes sure the first 408 response causes
		// a retry. From user perspective it just looks like everything went fine, the closed connection is gracefully
		// refreshed.
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);
		assertThat(out.toString()).startsWith("{");
	}

	@Test
	public void testConnectionPoolTimeoutRetry() throws Exception {
		// Let 2 connections succeed, this is just so we can fill the connection pool with more than one connection
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("Pooled Connection Timeout")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBodyFile("repository-list.xml"))
				.willSetStateTo("Connection1 Ok"));
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("Pooled Connection Timeout")
				.whenScenarioStateIs("Connection1 Ok")
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBodyFile("repository-list.xml"))
				.willSetStateTo("Pooled Connections Ok"));

		// Next, simulate that both connections in the pool were idled out on the server and upon sending a request
		// on them the server returns a 408 for both of them
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("Pooled Connection Timeout")
				.whenScenarioStateIs("Pooled Connections Ok")
				.willReturn(aResponse().withStatus(408)
						.withHeader(HttpHeaders.CONNECTION, "close")
						.withStatusMessage("Server closed inactive connection"))
				.willSetStateTo("Connection1 Closed"));
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("Pooled Connection Timeout")
				.whenScenarioStateIs("Connection1 Closed")
				.willReturn(aResponse().withStatus(408)
						.withHeader(HttpHeaders.CONNECTION, "close")
						.withStatusMessage("Server closed inactive connection"))
				.willSetStateTo("Pooled Connections Closed"));

		// When both connections in the pool were cleaned up the next try goes through ok
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("Pooled Connection Timeout")
				.whenScenarioStateIs("Pooled Connections Closed")
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBodyFile("repository-list.xml"))
				.willSetStateTo("Connection reopened"));

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
	public void testTupleQuery_NoPassthrough() throws Exception {
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBodyFile("repository-list.xml")));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TupleQueryResultHandler handler = Mockito.spy(new SPARQLStarResultsJSONWriter(out));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);

		// If not passed through, the QueryResultWriter methods should have been invoked
		verify(handler, times(1)).startQueryResult(anyList());

		// check that the OutputStream received content in JSON format
		assertThat(out.toString()).startsWith("{");
	}

	@Test
	public void testTupleQuery_Passthrough() throws Exception {
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBodyFile("repository-list.xml")));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SPARQLStarResultsXMLWriter handler = Mockito.spy(new SPARQLStarResultsXMLWriter(out));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);

		// SPARQL-star XML sink should accept SPARQL/XML data and pass directly to OutputStream
		verify(handler, never()).startQueryResult(anyList());

		// check that the OutputStream received content in XML format
		assertThat(out.toString()).startsWith("<");
	}

	@Test
	public void testTupleQuery_Passthrough_ConfiguredFalse() throws Exception {
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBodyFile("repository-list.xml")));

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

	private void verifyHeader(String path) {
		verify(anyRequestedFor(urlEqualTo(path)).withHeader(testHeader, containing(testValue)));
	}
}
