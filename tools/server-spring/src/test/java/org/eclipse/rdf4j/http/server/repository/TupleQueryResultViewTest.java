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
package org.eclipse.rdf4j.http.server.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.http.client.QueryCircuitBreaker;
import org.eclipse.rdf4j.http.client.QueryCircuitBreakerHandle;
import org.eclipse.rdf4j.http.client.QueryPressureState;
import org.eclipse.rdf4j.http.client.QueryResponseHeartbeat;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriterFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

public class TupleQueryResultViewTest {

	private static TupleQueryResultView view = TupleQueryResultView.getInstance();

	@AfterEach
	public void tearDown() throws Exception {
		resetGlobalBreaker();
	}

	@Test
	public void testRender_QueryEvaluationError1() throws Exception {
		var request = new MockHttpServletRequest();
		var response = new MockHttpServletResponse();

		TupleQueryResult queryResult = mock(TupleQueryResult.class);
		when(queryResult.hasNext()).thenThrow(QueryEvaluationException.class);

		Map<String, Object> model = new HashMap<>();
		model.put(TupleQueryResultView.FACTORY_KEY, new SPARQLResultsJSONWriterFactory());
		model.put(TupleQueryResultView.QUERY_RESULT_KEY, queryResult);

		view.render(model, request, response);

		assertThat(response.getStatus()).isEqualTo(500);
	}

	@Test
	public void testRender_ClosesBreakerHandleWhenConnectionCloseFails() throws Exception {
		resetGlobalBreaker();

		var request = new MockHttpServletRequest();
		var response = new MockHttpServletResponse();

		TupleQueryResult queryResult = mock(TupleQueryResult.class);
		when(queryResult.hasNext()).thenReturn(false);

		RepositoryConnection connection = mock(RepositoryConnection.class);
		doThrow(new RepositoryException("close failed")).when(connection).close();

		QueryCircuitBreaker breaker = QueryCircuitBreaker.getInstance();
		QueryCircuitBreakerHandle handle = breaker.register(QueryCircuitBreakerHandle.Source.SERVER, "repo",
				"select * where { ?s ?p ?o }");

		Map<String, Object> model = new HashMap<>();
		model.put(TupleQueryResultView.FACTORY_KEY, new SPARQLResultsJSONWriterFactory());
		model.put(TupleQueryResultView.QUERY_RESULT_KEY, queryResult);
		model.put(TupleQueryResultView.CONNECTION_KEY, connection);
		model.put(TupleQueryResultView.BREAKER_HANDLE_KEY, handle);

		try {
			assertThatThrownBy(() -> view.render(model, request, response))
					.isInstanceOf(RepositoryException.class)
					.hasMessage("close failed");
			assertThat(breaker.snapshotStatus().getActiveQueryCount()).isZero();
			verify(connection).close();
		} finally {
			breaker.complete(handle);
		}
	}

	@Test
	public void testRender_QueryEvaluationErrorBeforeHeartbeatKeepsTheErrorStatus() throws Exception {
		var request = new MockHttpServletRequest();
		var response = new MockHttpServletResponse();
		TupleQueryResult queryResult = mock(TupleQueryResult.class);
		when(queryResult.getBindingNames()).thenReturn(List.of());
		when(queryResult.hasNext()).thenThrow(new QueryEvaluationException("lazy failure"));

		QueryResponseHeartbeat heartbeat = new QueryResponseHeartbeat(response.getOutputStream(), Duration.ofHours(1),
				() -> {
				});
		var writer = new SPARQLResultsJSONWriterFactory().getWriter(heartbeat.getOutputStream());
		assertThat(heartbeat.start(writer)).isTrue();
		Map<String, Object> model = new HashMap<>();
		model.put(TupleQueryResultView.FACTORY_KEY, new SPARQLResultsJSONWriterFactory());
		model.put(TupleQueryResultView.QUERY_RESULT_KEY, queryResult);
		model.put(TupleQueryResultView.RESPONSE_HEARTBEAT_KEY, heartbeat);
		model.put(TupleQueryResultView.RESPONSE_WRITER_KEY, writer);

		view.render(model, request, response);

		assertThat(response.getStatus()).isEqualTo(500);
	}

	@Test
	public void testRender_QueryEvaluationErrorAfterHeartbeatDoesNotAppendAnErrorBody() throws Exception {
		var request = new MockHttpServletRequest();
		var response = new FlushObservingResponse();
		TupleQueryResult queryResult = mock(TupleQueryResult.class);
		when(queryResult.getBindingNames()).thenReturn(List.of());
		when(queryResult.hasNext()).thenThrow(new QueryEvaluationException("lazy failure"));

		QueryResponseHeartbeat heartbeat = new QueryResponseHeartbeat(response.getOutputStream(), Duration.ofMillis(1),
				() -> {
				});
		var writer = new SPARQLResultsJSONWriterFactory().getWriter(heartbeat.getOutputStream());
		assertThat(heartbeat.start(writer)).isTrue();
		assertThat(response.heartbeatFlush.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(heartbeat.hasProbed()).isTrue();
		response.flushBuffer();
		Map<String, Object> model = new HashMap<>();
		model.put(TupleQueryResultView.FACTORY_KEY, new SPARQLResultsJSONWriterFactory());
		model.put(TupleQueryResultView.QUERY_RESULT_KEY, queryResult);
		model.put(TupleQueryResultView.RESPONSE_HEARTBEAT_KEY, heartbeat);
		model.put(TupleQueryResultView.RESPONSE_WRITER_KEY, writer);

		assertThatThrownBy(() -> view.render(model, request, response))
				.isInstanceOf(QueryEvaluationException.class)
				.hasMessage("lazy failure");

		assertThat(response.getContentAsByteArray()).isNotEmpty();
		assertThat(response.getContentAsByteArray()).endsWith((byte) 0);
	}

	private static final class FlushObservingResponse extends MockHttpServletResponse {
		private final CountDownLatch heartbeatFlush = new CountDownLatch(1);
		private final ServletOutputStream output = new ServletOutputStream() {
			private final ServletOutputStream delegate = FlushObservingResponse.super.getOutputStream();

			@Override
			public void write(int value) throws java.io.IOException {
				delegate.write(value);
			}

			@Override
			public void flush() throws java.io.IOException {
				delegate.flush();
				heartbeatFlush.countDown();
			}

			@Override
			public boolean isReady() {
				return delegate.isReady();
			}

			@Override
			public void setWriteListener(WriteListener writeListener) {
				delegate.setWriteListener(writeListener);
			}
		};

		@Override
		public ServletOutputStream getOutputStream() {
			return output;
		}
	}

	private void resetGlobalBreaker() throws Exception {
		QueryCircuitBreaker breaker = QueryCircuitBreaker.getInstance();
		Field activeHandlesField = QueryCircuitBreaker.class.getDeclaredField("activeHandles");
		activeHandlesField.setAccessible(true);
		((Map<?, ?>) activeHandlesField.get(breaker)).clear();
		resetCounter(breaker, "handleSequence");
		resetCounter(breaker, "rejectCount");
		resetCounter(breaker, "cancelCount");

		Field currentStateField = QueryCircuitBreaker.class.getDeclaredField("currentState");
		currentStateField.setAccessible(true);
		currentStateField.set(breaker, QueryPressureState.NORMAL);

		Field lastCancelAtField = QueryCircuitBreaker.class.getDeclaredField("lastCancelAt");
		lastCancelAtField.setAccessible(true);
		lastCancelAtField.setLong(breaker, Long.MIN_VALUE);

		Class<?> transitionClass = Class.forName("org.eclipse.rdf4j.http.client.QueryCircuitBreaker$Transition");
		Method initialMethod = transitionClass.getDeclaredMethod("initial");
		initialMethod.setAccessible(true);
		Object initialTransition = initialMethod.invoke(null);

		Field lastTransitionField = QueryCircuitBreaker.class.getDeclaredField("lastTransition");
		lastTransitionField.setAccessible(true);
		lastTransitionField.set(breaker, initialTransition);
	}

	private void resetCounter(QueryCircuitBreaker breaker, String fieldName) throws Exception {
		Field field = QueryCircuitBreaker.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		((AtomicLong) field.get(breaker)).set(0);
	}
}
