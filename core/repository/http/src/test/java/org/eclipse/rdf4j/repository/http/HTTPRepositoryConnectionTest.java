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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.http.client.QueryExplanationRequestContext;
import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.explanation.Explanation;
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

	@Test
	public void testTupleQueryExplainDoesNotThrowUnsupportedOperation() throws Exception {
		HTTPRepositoryConnection connection = mock(HTTPRepositoryConnection.class);
		RDF4JProtocolSession explainSession = mock(RDF4JProtocolSession.class);
		Explanation explanation = mock(Explanation.class);
		when(connection.getSesameSession()).thenReturn(explainSession);
		when(explainSession.sendQueryExplanation(any(), any(), any(), any(), anyBoolean(), anyInt(), any(), any()))
				.thenReturn(explanation);
		HTTPTupleQuery query = new HTTPTupleQuery(connection, QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }",
				null);

		assertThatCode(() -> query.explain(Explanation.Level.Optimized)).doesNotThrowAnyException();
		verify(connection).flushTransactionState(Protocol.Action.QUERY);
		verify(explainSession).sendQueryExplanation(eq(QueryLanguage.SPARQL), any(), isNull(), isNull(),
				eq(true), eq(0), eq(Explanation.Level.Optimized));
	}

	@Test
	public void testTupleQueryExplainRegistersActiveSessionForCancellation() throws Exception {
		RDF4JProtocolSession activeSession = mock(RDF4JProtocolSession.class);
		RDF4JProtocolSession fallbackSession = mock(RDF4JProtocolSession.class);
		Explanation explanation = mock(Explanation.class);
		TestHTTPRepository repository = new TestHTTPRepository(fallbackSession);
		HTTPRepositoryConnection connection = new HTTPRepositoryConnection(repository, activeSession);
		HTTPTupleQuery query = new HTTPTupleQuery(connection, QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }",
				null);
		CountDownLatch explainStarted = new CountDownLatch(1);
		CountDownLatch allowExplainToFinish = new CountDownLatch(1);
		ExecutorService executor = Executors.newSingleThreadExecutor();

		repository.init();
		when(activeSession.sendQueryExplanation(any(), any(), any(), any(), anyBoolean(), anyInt(), any()))
				.thenAnswer(invocation -> {
					explainStarted.countDown();
					assertThat(allowExplainToFinish.await(5, TimeUnit.SECONDS)).isTrue();
					return explanation;
				});

		try {
			Future<Explanation> future = executor.submit(() -> {
				try (QueryExplanationRequestContext.Activation ignored = QueryExplanationRequestContext.activate(
						"req-123")) {
					return query.explain(Explanation.Level.Optimized);
				}
			});

			assertThat(explainStarted.await(5, TimeUnit.SECONDS)).isTrue();
			repository.cancelQueryExplanation("req-123");
			allowExplainToFinish.countDown();
			assertThat(future.get(5, TimeUnit.SECONDS)).isSameAs(explanation);

			verify(activeSession).cancelQueryExplanation("req-123");
			verify(fallbackSession, never()).cancelQueryExplanation(anyString());
		} finally {
			executor.shutdownNow();
			connection.close();
			repository.shutDown();
		}
	}

	private static final class TestHTTPRepository extends HTTPRepository {
		private final RDF4JProtocolSession fallbackSession;

		private TestHTTPRepository(RDF4JProtocolSession fallbackSession) {
			super("http://localhost/rdf4j-server", "test");
			this.fallbackSession = fallbackSession;
		}

		@Override
		protected RDF4JProtocolSession createHTTPClient() {
			return fallbackSession;
		}

		@Override
		boolean useCompatibleMode() {
			return false;
		}
	}

}
