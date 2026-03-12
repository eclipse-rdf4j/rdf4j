/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.handler;

import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.QueryResultView;
import org.eclipse.rdf4j.http.server.repository.resolver.RepositoryResolver;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

/**
 * A base implementation to handle an HTTP query request.
 */
public abstract class AbstractQueryRequestHandler implements QueryRequestHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final RepositoryResolver repositoryResolver;

	private final AsyncExplainRegistry asyncExplainRegistry = new AsyncExplainRegistry();

	public AbstractQueryRequestHandler(RepositoryResolver repositoryResolver) {
		this.repositoryResolver = repositoryResolver;
	}

	@Override
	public boolean handleCancelExplain(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!RequestMethod.POST.name().equals(request.getMethod())
				|| request.getParameter(Protocol.CANCEL_EXPLAIN_PARAM_NAME) == null) {
			return false;
		}

		String explainRequestId = request.getParameter(Protocol.EXPLAIN_REQUEST_ID_PARAM_NAME);
		if (explainRequestId == null || explainRequestId.trim().isEmpty()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Missing parameter: " + Protocol.EXPLAIN_REQUEST_ID_PARAM_NAME);
			return true;
		}

		asyncExplainRegistry.cancel(explainRequestId.trim());
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
		return true;
	}

	@Override
	public ModelAndView handleQueryRequest(
			HttpServletRequest request, RequestMethod requestMethod,
			HttpServletResponse response
	) throws HTTPException, IOException {

		RepositoryConnection repositoryCon = null;
		Object queryResponse = null;

		try {
			Repository repository = repositoryResolver.getRepository(request);
			repositoryCon = repositoryResolver.getRepositoryConnection(request, repository);

			String queryString = getQueryString(request, requestMethod);

			logQuery(requestMethod, queryString);

			Query query = getQuery(request, repositoryCon, queryString);

			boolean headersOnly = requestMethod == RequestMethod.HEAD;
			long limit = getLimit(request);
			long offset = getOffset(request);
			boolean distinct = isDistinct(request);
			final Optional<Explanation.Level> explainLevel = getExplain(request);
			final Optional<String> explainRequestId = getExplainRequestId(request);

			if (!headersOnly && explainLevel.isPresent() && explainRequestId.isPresent()) {
				return handleAsyncExplainRequest(request, response, repositoryCon, query, explainLevel.get(),
						explainRequestId.get());
			}

			try {
				if (!headersOnly) {
					// explain param is present, return the query explanation
					if (explainLevel.isPresent()) {
						try {
							Explanation explanation = explainQuery(query, explainLevel.get());
							return getExplainQueryResponse(request, response, explanation);
						} finally {
							// explanation is fully evaluated at this point, so we can safely close the connection
							// before returning the response
							repositoryCon.close();
						}
					}
					queryResponse = evaluateQuery(query, limit, offset, distinct);
				}

				FileFormatServiceRegistry<? extends FileFormat, ?> registry = getResultWriterFor(query);
				if (registry == null) {
					throw new UnsupportedOperationException(
							"Unknown result writer for query of type: " + query.getClass().getName());
				}

				View view = getViewFor(query);
				if (view == null) {
					throw new UnsupportedOperationException(
							"Unknown view for query of type: " + query.getClass().getName());
				}

				return getModelAndView(request, response, headersOnly, repositoryCon, view, queryResponse, registry);

			} catch (QueryInterruptedException e) {
				logger.info("Query interrupted", e);
				throw new ServerHTTPException(SC_SERVICE_UNAVAILABLE, "Query evaluation took too long");

			} catch (QueryEvaluationException e) {
				logger.info("Query evaluation error", e);
				if (e.getCause() != null && e.getCause() instanceof HTTPException) {
					// custom signal from the backend, throw as HTTPException
					// directly (see SES-1016).
					throw (HTTPException) e.getCause();
				} else {
					throw new ServerHTTPException("Query evaluation error: " + e.getMessage());
				}
			}

		} catch (Exception e) {
			// only close the response & connection when an exception occurs. Otherwise, the QueryResultView will take
			// care of closing it.
			try {
				if (queryResponse instanceof AutoCloseable) {
					((AutoCloseable) queryResponse).close();
				}
			} catch (Exception qre) {
				logger.warn("Query response closing error", qre);
			} finally {
				try {
					if (repositoryCon != null) {
						repositoryCon.close();
					}
				} catch (Exception qre) {
					logger.warn("Connection closing error", qre);
				}
			}
			throw e;
		}

	}

	protected Explanation explainQuery(final Query query, final Explanation.Level explainLevel)
			throws ServerHTTPException {
		throw new ServerHTTPException("unimplemented explainQuery feature");
	}

	private ModelAndView handleAsyncExplainRequest(HttpServletRequest request, HttpServletResponse response,
			RepositoryConnection repositoryCon, Query query, Explanation.Level explainLevel, String explainRequestId)
			throws IOException {
		AsyncContext asyncContext = request.startAsync();
		final AsyncExplainRegistry.Handle handle;
		try {
			handle = asyncExplainRegistry.register(explainRequestId, asyncContext);
		} catch (IllegalStateException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			asyncContext.complete();
			return null;
		}

		addAsyncExplainCleanupListener(explainRequestId, asyncContext, handle);
		asyncExplainRegistry.execute(() -> executeAsyncExplain(handle, query, explainLevel, repositoryCon));
		return null;
	}

	private void executeAsyncExplain(AsyncExplainRegistry.Handle handle, Query query, Explanation.Level explainLevel,
			RepositoryConnection repositoryCon) {
		try (RepositoryConnection connection = repositoryCon) {
			handle.attach(Thread.currentThread(), connection);
			if (!handle.isActive()) {
				return;
			}

			Explanation explanation = explainQuery(query, explainLevel);
			if (handle.isActive()) {
				renderAsyncExplainResponse(handle, explanation);
			}
		} catch (QueryInterruptedException e) {
			logger.info("Query interrupted", e);
			sendAsyncError(handle, SC_SERVICE_UNAVAILABLE, "Query evaluation took too long");
		} catch (QueryEvaluationException e) {
			logger.info("Query evaluation error", e);
			if (e.getCause() instanceof HTTPException) {
				sendAsyncHttpError(handle, (HTTPException) e.getCause());
			} else {
				sendAsyncError(handle, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Query evaluation error: " + e.getMessage());
			}
		} catch (HTTPException e) {
			sendAsyncHttpError(handle, e);
		} catch (Exception e) {
			logger.info("Async explain error", e);
			sendAsyncError(handle, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} finally {
			asyncExplainRegistry.complete(handle);
		}
	}

	private void renderAsyncExplainResponse(AsyncExplainRegistry.Handle handle, Explanation explanation)
			throws Exception {
		HttpServletRequest request = (HttpServletRequest) handle.getAsyncContext().getRequest();
		HttpServletResponse response = (HttpServletResponse) handle.getAsyncContext().getResponse();
		ModelAndView modelAndView = getExplainQueryResponse(request, response, explanation);
		if (modelAndView == null) {
			return;
		}
		View view = modelAndView.getView();
		if (view == null) {
			return;
		}
		view.render(modelAndView.getModel(), request, response);
	}

	private void sendAsyncHttpError(AsyncExplainRegistry.Handle handle, HTTPException exception) {
		sendAsyncError(handle, exception.getStatusCode(), exception.getMessage());
	}

	private void sendAsyncError(AsyncExplainRegistry.Handle handle, int statusCode, String message) {
		if (!handle.isActive()) {
			return;
		}
		try {
			((HttpServletResponse) handle.getAsyncContext().getResponse()).sendError(statusCode, message);
		} catch (IOException e) {
			logger.debug("Unable to write async explain error for request {}", handle.getExplainRequestId(), e);
		}
	}

	private void addAsyncExplainCleanupListener(String explainRequestId, AsyncContext asyncContext,
			AsyncExplainRegistry.Handle handle) {
		asyncContext.addListener(new AsyncListener() {
			@Override
			public void onComplete(AsyncEvent event) {
				asyncExplainRegistry.forget(handle);
			}

			@Override
			public void onTimeout(AsyncEvent event) {
				asyncExplainRegistry.cancel(explainRequestId);
			}

			@Override
			public void onError(AsyncEvent event) {
				asyncExplainRegistry.cancel(explainRequestId);
			}

			@Override
			public void onStartAsync(AsyncEvent event) {
				// no-op
			}
		});
	}

	protected abstract ModelAndView getExplainQueryResponse(
			final HttpServletRequest request, final HttpServletResponse response, final Explanation explanation);

	abstract protected Object evaluateQuery(Query query, long limit, long offset, boolean distinct)
			throws ClientHTTPException;

	abstract protected View getViewFor(Query query);

	abstract protected FileFormatServiceRegistry<? extends FileFormat, ?> getResultWriterFor(Query query);

	abstract protected String getQueryString(HttpServletRequest request, RequestMethod requestMethod)
			throws HTTPException;

	abstract protected Query getQuery(HttpServletRequest request, RepositoryConnection repositoryCon,
			String queryString) throws IOException, HTTPException;

	protected ModelAndView getModelAndView(
			HttpServletRequest request, HttpServletResponse response,
			boolean headersOnly, RepositoryConnection repositoryCon, View view, Object queryResult,
			FileFormatServiceRegistry<? extends FileFormat, ?> registry
	) throws ClientHTTPException {
		Map<String, Object> model = new HashMap<>();
		model.put(QueryResultView.FILENAME_HINT_KEY, "query-result");
		model.put(QueryResultView.QUERY_RESULT_KEY, queryResult);
		model.put(QueryResultView.FACTORY_KEY, ProtocolUtil.getAcceptableService(request, response, registry));
		model.put(QueryResultView.HEADERS_ONLY, headersOnly);
		model.put(QueryResultView.CONNECTION_KEY, repositoryCon);

		return new ModelAndView(view, model);
	}

	protected boolean isDistinct(HttpServletRequest request) throws ClientHTTPException {
		return getParam(request, Protocol.DISTINCT_PARAM_NAME, false, Boolean.TYPE);
	}

	protected long getOffset(HttpServletRequest request) throws ClientHTTPException {
		return getParam(request, Protocol.OFFSET_PARAM_NAME, 0L, Long.TYPE);
	}

	protected long getLimit(HttpServletRequest request) throws ClientHTTPException {
		return getParam(request, Protocol.LIMIT_PARAM_NAME, 0L, Long.TYPE);
	}

	protected Optional<Explanation.Level> getExplain(HttpServletRequest request) throws ClientHTTPException {
		final String explainString = request.getParameter(Protocol.EXPLAIN_PARAM_NAME);
		if (explainString == null) {
			return Optional.empty();
		}
		try {
			final Explanation.Level level = Explanation.Level.valueOf(explainString);
			return Optional.of(level);
		} catch (final IllegalArgumentException e) {
			throw new ClientHTTPException("Invalid explanation level: " + explainString, e);
		}
	}

	protected Optional<String> getExplainRequestId(HttpServletRequest request) {
		String explainRequestId = request.getParameter(Protocol.EXPLAIN_REQUEST_ID_PARAM_NAME);
		if (explainRequestId == null) {
			return Optional.empty();
		}
		String normalizedExplainRequestId = explainRequestId.trim();
		if (normalizedExplainRequestId.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(normalizedExplainRequestId);
	}

	<T> T getParam(HttpServletRequest request, String distinctParamName, T defaultValue, Class<T> clazz)
			throws ClientHTTPException {
		if (clazz == Boolean.TYPE) {
			return (T) (Boolean) ProtocolUtil.parseBooleanParam(request, distinctParamName, (Boolean) defaultValue);
		}
		if (clazz == Long.TYPE) {
			return (T) (Long) ProtocolUtil.parseLongParam(request, distinctParamName, (Long) defaultValue);
		}
		throw new UnsupportedOperationException("Class not supported: " + clazz);
	}

	private void logQuery(RequestMethod requestMethod, String queryString) {
		if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
			int queryHashCode = queryString.hashCode();

			switch (requestMethod) {
			case GET:
				logger.info("GET query {}", queryHashCode);
				break;
			case HEAD:
				logger.info("HEAD query {}", queryHashCode);
				break;
			case POST:
				logger.info("POST query {}", queryHashCode);
				break;
			}

			logger.debug("query {} = {}", queryHashCode, queryString);
		}
	}

}
