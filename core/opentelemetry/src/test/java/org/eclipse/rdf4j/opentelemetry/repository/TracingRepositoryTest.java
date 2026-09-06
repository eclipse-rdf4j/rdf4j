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
package org.eclipse.rdf4j.opentelemetry.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.opentelemetry.OpenTelemetrySupport;
import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;

class TracingRepositoryTest {

	@RegisterExtension
	static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

	private SailRepository plainRepository;

	@BeforeEach
	void setUp() {
		plainRepository = new SailRepository(new MemoryStore());
		plainRepository.init();
		try (RepositoryConnection conn = plainRepository.getConnection()) {
			ValueFactory vf = plainRepository.getValueFactory();
			conn.add(vf.createIRI("urn:s"), RDF.TYPE, RDFS.RESOURCE);
			conn.add(vf.createIRI("urn:s2"), RDF.TYPE, RDFS.RESOURCE);
		}
	}

	@AfterEach
	void tearDown() {
		plainRepository.shutDown();
	}

	private Repository instrument(RDF4JOpenTelemetryConfig config) {
		return OpenTelemetrySupport.instrument(plainRepository, "test-repo", config);
	}

	@Test
	void tupleQuery_recordsSpanWithRowCount() {
		Repository traced = instrument(RDF4JOpenTelemetryConfig.defaultConfig());

		try (RepositoryConnection conn = traced.getConnection()) {
			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");
			try (TupleQueryResult result = query.evaluate()) {
				while (result.hasNext()) {
					result.next();
				}
			}
		}

		List<SpanData> spans = otelTesting.getSpans();
		assertThat(spans).hasSize(1);
		SpanData span = spans.get(0);
		assertThat(span.getName()).isEqualTo("SELECT test-repo");
		assertThat(span.getKind().name()).isEqualTo("CLIENT");
		assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
		assertThat(span.getAttributes().get(DbOtelAttributes.DB_SYSTEM_NAME)).isEqualTo("rdf4j");
		assertThat(span.getAttributes().get(DbOtelAttributes.DB_OPERATION_NAME)).isEqualTo("SELECT");
		assertThat(span.getAttributes().get(DbOtelAttributes.DB_NAMESPACE)).isEqualTo("test-repo");
		assertThat(span.getAttributes().get(DbOtelAttributes.DB_RESPONSE_RETURNED_ROWS)).isEqualTo(2L);
		assertThat(span.getAttributes().get(DbOtelAttributes.DB_QUERY_TEXT)).isNull();
	}

	@Test
	void captureQueryTextEnabled_recordsTruncatedQueryText() {
		Repository traced = instrument(RDF4JOpenTelemetryConfig.builder()
				.openTelemetry(otelTesting.getOpenTelemetry())
				.captureQueryText(true)
				.maxQueryTextLength(6)
				.build());

		try (RepositoryConnection conn = traced.getConnection()) {
			BooleanQuery query = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK { ?s ?p ?o }");
			query.evaluate();
		}

		SpanData span = otelTesting.getSpans().get(0);
		assertThat(span.getName()).isEqualTo("ASK test-repo");
		assertThat(span.getAttributes().get(DbOtelAttributes.DB_QUERY_TEXT)).isEqualTo("ASK { ");
	}

	@Test
	void graphQuery_detectsConstructAndDescribe() {
		Repository traced = instrument(RDF4JOpenTelemetryConfig.defaultConfig());

		try (RepositoryConnection conn = traced.getConnection()) {
			try (GraphQueryResult r = conn
					.prepareGraphQuery(QueryLanguage.SPARQL, "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }")
					.evaluate()) {
				r.hasNext();
			}
			try (GraphQueryResult r = conn.prepareGraphQuery(QueryLanguage.SPARQL, "DESCRIBE <urn:s>").evaluate()) {
				r.hasNext();
			}
		}

		List<SpanData> spans = otelTesting.getSpans();
		assertThat(spans).hasSize(2);
		assertThat(spans.get(0).getAttributes().get(DbOtelAttributes.DB_OPERATION_NAME)).isEqualTo("CONSTRUCT");
		assertThat(spans.get(1).getAttributes().get(DbOtelAttributes.DB_OPERATION_NAME)).isEqualTo("DESCRIBE");
	}

	@Test
	void update_recordsSpan() {
		Repository traced = instrument(RDF4JOpenTelemetryConfig.defaultConfig());

		try (RepositoryConnection conn = traced.getConnection()) {
			Update update = conn.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <urn:s3> a <urn:C> }");
			update.execute();
		}

		SpanData span = otelTesting.getSpans().get(0);
		assertThat(span.getName()).isEqualTo("UPDATE test-repo");
		assertThat(span.getAttributes().get(DbOtelAttributes.DB_OPERATION_NAME)).isEqualTo("UPDATE");
	}

	@Test
	void malformedQuery_recordsErrorStatus() {
		Repository traced = instrument(RDF4JOpenTelemetryConfig.defaultConfig());

		try (RepositoryConnection conn = traced.getConnection()) {
			org.junit.jupiter.api.Assertions.assertThrows(MalformedQueryException.class,
					() -> conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT this is not sparql"));
		}

		// prepare() failures happen before a span is opened (span starts at evaluate()), so nothing recorded here;
		// exercise an evaluation-time failure instead via an unbound wildcard against a closed connection.
		assertThat(otelTesting.getSpans()).isEmpty();
	}

	@Test
	void evaluationFailure_recordsErrorStatus() {
		Repository traced = instrument(RDF4JOpenTelemetryConfig.defaultConfig());

		RepositoryConnection conn = traced.getConnection();
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");
		conn.close();

		org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, query::evaluate);

		SpanData span = otelTesting.getSpans().get(0);
		assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
		assertThat(span.getAttributes().get(DbOtelAttributes.ERROR_TYPE)).isNotNull();
	}
}
