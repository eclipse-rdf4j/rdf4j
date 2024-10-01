/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.transaction;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import static org.eclipse.rdf4j.http.protocol.Protocol.BINDING_PREFIX;
import static org.eclipse.rdf4j.http.protocol.Protocol.DEFAULT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INCLUDE_INFERRED_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.NAMED_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_LANGUAGE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_PARAM_NAME;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.BooleanQueryResultView;
import org.eclipse.rdf4j.http.server.repository.GraphQueryResultView;
import org.eclipse.rdf4j.http.server.repository.QueryResultView;
import org.eclipse.rdf4j.http.server.repository.TupleQueryResultView;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriterRegistry;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterRegistry;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

/**
 * @author jeen
 *
 */
public class QueryController extends AbstractActionController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public QueryController() throws ApplicationContextException {
		setSupportedMethods(METHOD_POST, "PUT");
	}

	@Override
	protected ModelAndView handleAction(HttpServletRequest request, HttpServletResponse response,
			Transaction transaction) throws Exception {
		logger.info("{} txn query request", request.getMethod());

		String queryStr;
		final String contentType = request.getContentType();
		if (contentType != null && contentType.contains(Protocol.SPARQL_QUERY_MIME_TYPE)) {
			Charset charset = getCharset(request);
			queryStr = IOUtils.toString(request.getInputStream(), charset);
		} else {
			queryStr = request.getParameter(QUERY_PARAM_NAME);
		}

		View view;
		Object queryResult;
		FileFormatServiceRegistry<? extends FileFormat, ?> registry;

		try {
			Query query = getQuery(transaction, queryStr, request, response);

			if (query instanceof TupleQuery) {
				TupleQuery tQuery = (TupleQuery) query;

				queryResult = transaction.evaluate(tQuery);
				registry = TupleQueryResultWriterRegistry.getInstance();
				view = TupleQueryResultView.getInstance();
			} else if (query instanceof GraphQuery) {
				GraphQuery gQuery = (GraphQuery) query;

				queryResult = transaction.evaluate(gQuery);
				registry = RDFWriterRegistry.getInstance();
				view = GraphQueryResultView.getInstance();
			} else if (query instanceof BooleanQuery) {
				BooleanQuery bQuery = (BooleanQuery) query;

				queryResult = transaction.evaluate(bQuery);
				registry = BooleanQueryResultWriterRegistry.getInstance();
				view = BooleanQueryResultView.getInstance();
			} else {
				throw new ClientHTTPException(SC_BAD_REQUEST, "Unsupported query type: " + query.getClass().getName());
			}
		} catch (QueryInterruptedException | InterruptedException | ExecutionException e) {
			if (e.getCause() != null && e.getCause() instanceof MalformedQueryException) {
				ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getCause().getMessage());
				throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
			} else {
				logger.info("Query interrupted", e);
				throw new ServerHTTPException(SC_SERVICE_UNAVAILABLE, "Query execution interrupted");
			}
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
		Object factory = ProtocolUtil.getAcceptableService(request, response, registry);

		Map<String, Object> model = new HashMap<>();
		model.put(QueryResultView.FILENAME_HINT_KEY, "query-result");
		model.put(QueryResultView.QUERY_RESULT_KEY, queryResult);
		model.put(QueryResultView.FACTORY_KEY, factory);
		model.put(QueryResultView.HEADERS_ONLY, false); // TODO needed for HEAD requests.

		logger.info("{} txn query request finished", request.getMethod());
		return new ModelAndView(view, model);
	}

	private Query getQuery(Transaction txn, String queryStr, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ClientHTTPException, InterruptedException, ExecutionException {
		Query result = null;

		// default query language is SPARQL
		QueryLanguage queryLn = QueryLanguage.SPARQL;

		String queryLnStr = request.getParameter(QUERY_LANGUAGE_PARAM_NAME);
		logger.debug("query language param = {}", queryLnStr);

		if (queryLnStr != null) {
			queryLn = QueryLanguage.valueOf(queryLnStr);

			if (queryLn == null) {
				throw new ClientHTTPException(SC_BAD_REQUEST, "Unknown query language: " + queryLnStr);
			}
		}

		String baseURI = request.getParameter(Protocol.BASEURI_PARAM_NAME);

		// determine if inferred triples should be included in query evaluation
		boolean includeInferred = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

		String timeout = request.getParameter(Protocol.TIMEOUT_PARAM_NAME);
		int maxQueryTime = 0;
		if (timeout != null) {
			try {
				maxQueryTime = Integer.parseInt(timeout);
			} catch (NumberFormatException e) {
				throw new ClientHTTPException(SC_BAD_REQUEST, "Invalid timeout value: " + timeout);
			}
		}

		// build a dataset, if specified
		String[] defaultGraphURIs = request.getParameterValues(DEFAULT_GRAPH_PARAM_NAME);
		String[] namedGraphURIs = request.getParameterValues(NAMED_GRAPH_PARAM_NAME);

		SimpleDataset dataset = null;
		if (defaultGraphURIs != null || namedGraphURIs != null) {
			dataset = new SimpleDataset();

			if (defaultGraphURIs != null) {
				for (String defaultGraphURI : defaultGraphURIs) {
					try {
						IRI uri = null;
						if (!"null".equals(defaultGraphURI)) {
							uri = SimpleValueFactory.getInstance().createIRI(defaultGraphURI);
						}
						dataset.addDefaultGraph(uri);
					} catch (IllegalArgumentException e) {
						throw new ClientHTTPException(SC_BAD_REQUEST,
								"Illegal URI for default graph: " + defaultGraphURI);
					}
				}
			}

			if (namedGraphURIs != null) {
				for (String namedGraphURI : namedGraphURIs) {
					try {
						IRI uri = null;
						if (!"null".equals(namedGraphURI)) {
							uri = SimpleValueFactory.getInstance().createIRI(namedGraphURI);
						}
						dataset.addNamedGraph(uri);
					} catch (IllegalArgumentException e) {
						throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for named graph: " + namedGraphURI);
					}
				}
			}
		}

		try {
			result = txn.prepareQuery(queryLn, queryStr, baseURI);
			result.setIncludeInferred(includeInferred);

			if (maxQueryTime > 0) {
				result.setMaxExecutionTime(maxQueryTime);
			}

			if (dataset != null) {
				result.setDataset(dataset);
			}

			// determine if any variable bindings have been set on this query.
			@SuppressWarnings("unchecked")
			Enumeration<String> parameterNames = request.getParameterNames();

			while (parameterNames.hasMoreElements()) {
				String parameterName = parameterNames.nextElement();

				if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
					String bindingName = parameterName.substring(BINDING_PREFIX.length());
					Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
							SimpleValueFactory.getInstance());
					result.setBinding(bindingName, bindingValue);
				}
			}
		} catch (UnsupportedQueryLanguageException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.UNSUPPORTED_QUERY_LANGUAGE, queryLn.getName());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		} catch (MalformedQueryException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		} catch (RepositoryException e) {
			logger.error("Repository error", e);
			response.sendError(SC_INTERNAL_SERVER_ERROR);
		}

		return result;
	}
}
