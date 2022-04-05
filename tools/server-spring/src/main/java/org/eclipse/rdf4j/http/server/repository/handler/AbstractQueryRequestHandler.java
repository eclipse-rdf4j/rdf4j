/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.handler;

import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

	public AbstractQueryRequestHandler(RepositoryResolver repositoryResolver) {
		this.repositoryResolver = repositoryResolver;
	}

	@Override
	public ModelAndView handleQueryRequest(HttpServletRequest request, RequestMethod requestMethod,
			HttpServletResponse response) throws HTTPException, IOException {

		RepositoryConnection repositoryCon = null;

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

			try {

				Object queryResponse;

				if (headersOnly) {
					queryResponse = null;
				} else {
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
			// only close the connection when an exception occurs. Otherwise, the QueryResultView will take care of
			// closing it.
			if (repositoryCon != null) {
				repositoryCon.close();
			}
			throw e;
		}

	}

	abstract protected Object evaluateQuery(Query query, long limit, long offset, boolean distinct)
			throws ClientHTTPException;

	abstract protected View getViewFor(Query query);

	abstract protected FileFormatServiceRegistry<? extends FileFormat, ?> getResultWriterFor(Query query);

	abstract protected String getQueryString(HttpServletRequest request, RequestMethod requestMethod)
			throws HTTPException;

	abstract protected Query getQuery(HttpServletRequest request, RepositoryConnection repositoryCon,
			String queryString) throws IOException, HTTPException;

	protected ModelAndView getModelAndView(HttpServletRequest request, HttpServletResponse response,
			boolean headersOnly, RepositoryConnection repositoryCon, View view, Object queryResult,
			FileFormatServiceRegistry<? extends FileFormat, ?> registry) throws ClientHTTPException {
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
