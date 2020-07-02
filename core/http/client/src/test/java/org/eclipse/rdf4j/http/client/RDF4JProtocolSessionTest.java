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
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.lessThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Unit tests for {@link RDF4JProtocolSession}
 *
 * @author Jeen Broekstra
 */
public class RDF4JProtocolSessionTest {

	@ClassRule
	public static WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

	private RDF4JProtocolSession subject;

	private String testHeader = "X-testing-header";
	private String testValue = "foobar";

	private String serverURL = "http://localhost:" + wireMockRule.port() + "/rdf4j-server";
	private String repositoryID = "test";

	@Before
	public void setUp() throws Exception {
		subject = new SharedHttpClientSessionManager().createRDF4JProtocolSession(serverURL);
		subject.setRepository(Protocol.getRepositoryLocation(serverURL, repositoryID));
		HashMap<String, String> additionalHeaders = new HashMap<>();
		additionalHeaders.put(testHeader, testValue);
		subject.setAdditionalHttpHeaders(additionalHeaders);
	}

	@Test
	public void testCreateRepositoryExecutesPut() throws Exception {
		stubFor(put(urlEqualTo("/rdf4j-server/repositories/test")).willReturn(aResponse().withStatus(200)));
		RepositoryConfig config = new RepositoryConfig("test");
		subject.createRepository(config);
		verify(putRequestedFor(urlEqualTo("/rdf4j-server/repositories/test")));
		verifyHeader("/rdf4j-server/repositories/test");
	}

	@Test
	public void testUpdateRepositoryExecutesPost() throws Exception {
		RepositoryConfig config = new RepositoryConfig("test");

		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test/config")).willReturn(aResponse().withStatus(200)));

		subject.updateRepository(config);

		verify(postRequestedFor(urlEqualTo("/rdf4j-server/repositories/test/config")));
		verifyHeader("/rdf4j-server/repositories/test/config");
	}

	@Test
	public void testSize() throws Exception {
		stubFor(get(urlEqualTo("/rdf4j-server/repositories/test/size"))
				.willReturn(aResponse().withStatus(200).withBody("8")));

		assertThat(subject.size()).isEqualTo(8);
		verifyHeader("/rdf4j-server/repositories/test/size");
	}

	@Test
	public void testGetRepositoryConfig() throws Exception {
		ArgumentCaptor<HttpGet> method = ArgumentCaptor.forClass(HttpGet.class);

		Header h = new BasicHeader("Content-Type", RDFFormat.NTRIPLES.getDefaultMIMEType());
		stubFor(get(urlEqualTo("/rdf4j-server/repositories/test/config"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", RDFFormat.NTRIPLES.getDefaultMIMEType())
						.withBodyFile("repository-config.nt")));

		subject.getRepositoryConfig(new StatementCollector());

		verify(getRequestedFor(urlEqualTo("/rdf4j-server/repositories/test/config")));

		verifyHeader("/rdf4j-server/repositories/test/config");
	}

	@Test
	public void testRepositoryList() throws Exception {
		stubFor(get(urlEqualTo("/rdf4j-server/repositories"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBodyFile("repository-list.xml")));

		assertThat(subject.getRepositoryList().getBindingNames()).contains("id");
		verifyHeader("/rdf4j-server/repositories");
	}

	@Test
	public void testClose() throws Exception {
		System.setProperty(Protocol.CACHE_TIMEOUT_PROPERTY, "1");
		subject = new SharedHttpClientSessionManager().createRDF4JProtocolSession(serverURL);
		subject.setRepository(Protocol.getRepositoryLocation(serverURL, repositoryID));

		String transactionStartUrl = Protocol.getTransactionsLocation(subject.getRepositoryURL());

		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test/transactions"))
				.willReturn(aResponse().withStatus(201).withHeader("Location", transactionStartUrl + "/1")));
		stubFor(post("/rdf4j-server/repositories/test/transactions/1?action=PING")
				.willReturn(aResponse().withStatus(200).withBody("2000")));

		subject.beginTransaction(IsolationLevels.SERIALIZABLE);
		Thread.sleep(2000);

		verify(moreThanOrExactly(2),
				postRequestedFor(urlEqualTo("/rdf4j-server/repositories/test/transactions/1?action=PING")));

		subject.close();
		Thread.sleep(1000);

		// we should not have received any further pings after the session was closed.
		verify(lessThanOrExactly(3),
				postRequestedFor(urlEqualTo("/rdf4j-server/repositories/test/transactions/1?action=PING")));
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
