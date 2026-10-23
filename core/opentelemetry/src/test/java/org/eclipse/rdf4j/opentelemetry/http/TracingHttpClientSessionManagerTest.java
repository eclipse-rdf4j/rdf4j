/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.opentelemetry.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.MediaType;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;

@ExtendWith(MockServerExtension.class)
class TracingHttpClientSessionManagerTest {

	@RegisterExtension
	static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

	private String serverURL;

	private TracingHttpClientSessionManager sessionManager;

	@BeforeEach
	void setUp(MockServerClient client) {
		client.reset();
		serverURL = "http://localhost:" + client.getPort();
	}

	@AfterEach
	void tearDown() {
		if (sessionManager != null) {
			sessionManager.shutDown();
		}
	}

	private TracingSPARQLProtocolSession createSession(RDF4JOpenTelemetryConfig config) {
		sessionManager = new TracingHttpClientSessionManager(config);
		SPARQLProtocolSession session = sessionManager.createSPARQLProtocolSession(serverURL + "/query",
				serverURL + "/update");
		return (TracingSPARQLProtocolSession) session;
	}

	@Test
	void successfulQuery_recordsSpanWithHttpAttributesAndPropagatesTraceContext(MockServerClient client)
			throws Exception {
		client.when(request().withPath("/query"))
				.respond(response().withStatusCode(200).withBody("true", MediaType.parse("text/boolean")));

		RDF4JOpenTelemetryConfig config = RDF4JOpenTelemetryConfig.builder()
				.openTelemetry(otelTesting.getOpenTelemetry())
				.build();
		TracingSPARQLProtocolSession session = createSession(config);

		boolean result = session.sendBooleanQuery(QueryLanguage.SPARQL, "ASK { ?s ?p ?o }", null, false);
		assertThat(result).isTrue();

		List<SpanData> spans = otelTesting.getSpans();
		assertThat(spans).hasSize(1);
		SpanData span = spans.get(0);
		assertThat(span.getName()).isEqualTo("SPARQL query");
		assertThat(span.getKind().name()).isEqualTo("CLIENT");
		assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
		assertThat(span.getAttributes().get(SparqlOtelAttributes.HTTP_RESPONSE_STATUS_CODE)).isEqualTo(200L);
		assertThat(span.getAttributes().get(SparqlOtelAttributes.SPARQL_OPERATION)).isEqualTo("query");
		// query text is not captured by default
		assertThat(span.getAttributes().get(SparqlOtelAttributes.SPARQL_QUERY)).isNull();

		HttpRequest[] recorded = client.retrieveRecordedRequests(request().withPath("/query"));
		assertThat(recorded).hasSize(1);
		String traceparent = recorded[0].getFirstHeader("traceparent");
		assertThat(traceparent).isNotBlank();
		assertThat(traceparent).contains(span.getTraceId());
	}

	@Test
	void captureQueryTextEnabled_recordsTruncatedQueryText(MockServerClient client) throws Exception {
		client.when(request().withPath("/query"))
				.respond(response().withStatusCode(200).withBody("true", MediaType.parse("text/boolean")));

		RDF4JOpenTelemetryConfig config = RDF4JOpenTelemetryConfig.builder()
				.openTelemetry(otelTesting.getOpenTelemetry())
				.captureQueryText(true)
				.maxQueryTextLength(5)
				.build();
		TracingSPARQLProtocolSession session = createSession(config);

		session.sendBooleanQuery(QueryLanguage.SPARQL, "ASK { ?s ?p ?o }", null, false);

		SpanData span = otelTesting.getSpans().get(0);
		assertThat(span.getAttributes().get(SparqlOtelAttributes.SPARQL_QUERY)).isEqualTo("ASK {");
	}

	@Test
	void serverError_recordsErrorStatusAndPropagatesException(MockServerClient client) {
		client.when(request().withPath("/query")).respond(response().withStatusCode(500));

		RDF4JOpenTelemetryConfig config = RDF4JOpenTelemetryConfig.builder()
				.openTelemetry(otelTesting.getOpenTelemetry())
				.build();
		TracingSPARQLProtocolSession session = createSession(config);

		org.junit.jupiter.api.Assertions.assertThrows(RepositoryException.class,
				() -> session.sendBooleanQuery(QueryLanguage.SPARQL, "ASK { ?s ?p ?o }", null, false));

		SpanData span = otelTesting.getSpans().get(0);
		assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
	}
}
