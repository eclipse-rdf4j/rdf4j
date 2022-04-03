/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.handler;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;

import static org.eclipse.rdf4j.http.protocol.Protocol.BINDING_PREFIX;
import static org.eclipse.rdf4j.http.protocol.Protocol.DEFAULT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INCLUDE_INFERRED_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.NAMED_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_LANGUAGE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_PARAM_NAME;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.common.webapp.util.HttpServerUtil;
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
import org.eclipse.rdf4j.http.server.repository.resolver.RepositoryResolver;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriterRegistry;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterRegistry;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

public class DefaultQueryRequestHandler implements QueryRequestHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final RepositoryResolver repositoryResolver;

	public DefaultQueryRequestHandler(RepositoryResolver repositoryResolver) {
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

	protected boolean isDistinct(HttpServletRequest request) throws ClientHTTPException {
		return getParam(request, Protocol.DISTINCT_PARAM_NAME, false, Boolean.TYPE);
	}

	protected long getOffset(HttpServletRequest request) throws ClientHTTPException {
		return getParam(request, Protocol.OFFSET_PARAM_NAME, 0L, Long.TYPE);
	}

	protected long getLimit(HttpServletRequest request) throws ClientHTTPException {
		return getParam(request, Protocol.LIMIT_PARAM_NAME, 0L, Long.TYPE);
	}

	private <T> T getParam(HttpServletRequest request, String distinctParamName, T defaultValue, Class<T> clazz)
			throws ClientHTTPException {
		if (clazz == Boolean.TYPE) {
			return (T) (Boolean) ProtocolUtil.parseBooleanParam(request, distinctParamName, (Boolean) defaultValue);
		}
		if (clazz == Long.TYPE) {
			return (T) (Long) ProtocolUtil.parseLongParam(request, distinctParamName, (Long) defaultValue);
		}
		throw new UnsupportedOperationException("Class not supported: " + clazz);
	}

	protected Object evaluateQuery(Query query, long limit, long offset, boolean distinct) throws ClientHTTPException {
		if (query instanceof TupleQuery) {
			return evaluateQuery((TupleQuery) query, limit, offset, distinct);
		} else if (query instanceof GraphQuery) {
			return evaluateQuery((GraphQuery) query, limit, offset, distinct);
		} else if (query instanceof BooleanQuery) {
			return evaluateQuery((BooleanQuery) query, limit, offset, distinct);
		} else {
			throw new ClientHTTPException(SC_BAD_REQUEST,
					"Unsupported query type: " + query.getClass().getName());
		}
	}

	protected Boolean evaluateQuery(BooleanQuery query, long limit, long offset, boolean distinct) {
		return query.evaluate();
	}

	protected GraphQueryResult evaluateQuery(GraphQuery query, long limit, long offset, boolean distinct) {
		GraphQueryResult qqr = distinct ? QueryResults.distinctResults(query.evaluate()) : query.evaluate();
		return QueryResults.limitResults(qqr, limit, offset);
	}

	protected TupleQueryResult evaluateQuery(TupleQuery query, long limit, long offset, boolean distinct) {
		TupleQueryResult tqr = distinct ? QueryResults.distinctResults(query.evaluate()) : query.evaluate();
		return QueryResults.limitResults(tqr, limit, offset);
	}

	@Override
	public View getViewFor(Query query) {
		if (query instanceof TupleQuery) {
			return TupleQueryResultView.getInstance();
		} else if (query instanceof GraphQuery) {
			return GraphQueryResultView.getInstance();
		} else if (query instanceof BooleanQuery) {
			return BooleanQueryResultView.getInstance();
		}

		return null;

	}

	@Override
	public FileFormatServiceRegistry<? extends FileFormat, ?> getResultWriterFor(Query query) {
		if (query instanceof TupleQuery) {
			return TupleQueryResultWriterRegistry.getInstance();
		} else if (query instanceof GraphQuery) {
			return RDFWriterRegistry.getInstance();
		} else if (query instanceof BooleanQuery) {
			return BooleanQueryResultWriterRegistry.getInstance();
		}

		return null;
	}

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

	@Override
	public String getQueryString(HttpServletRequest request, RequestMethod requestMethod) throws HTTPException {

		String queryString;
		if (requestMethod == RequestMethod.POST) {
			String mimeType = HttpServerUtil.getMIMEType(request.getContentType());

			if (Protocol.SPARQL_QUERY_MIME_TYPE.equals(mimeType)) {
				// The query should be the entire body
				try {
					queryString = IOUtils.toString(request.getReader());
				} catch (IOException e) {
					throw new HTTPException(HttpStatus.SC_BAD_REQUEST, "Error reading request message body", e);
				}
			} else {
				throw new ClientHTTPException(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported MIME type: " + mimeType);
			}
		} else {
			queryString = request.getParameter(QUERY_PARAM_NAME);
		}

		if (queryString == null) {
			throw new ClientHTTPException(SC_BAD_REQUEST, "Missing parameter: " + QUERY_PARAM_NAME);
		}

		return queryString;
	}

	@Override
	public Query getQuery(HttpServletRequest request,
			RepositoryConnection repositoryCon, String queryString) throws IOException, HTTPException {

		QueryLanguage queryLn = getQueryLanguage(request.getParameter(QUERY_LANGUAGE_PARAM_NAME));
		String baseIRI = request.getParameter(Protocol.BASEURI_PARAM_NAME);

		try {
			Query query = repositoryCon.prepareQuery(queryLn, queryString, baseIRI);

			setInferred(request, query);
			setQueryTimeout(request, query);
			setDataset(request, repositoryCon, query);
			setBindings(request, query, repositoryCon.getValueFactory());

			return query;

		} catch (UnsupportedQueryLanguageException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.UNSUPPORTED_QUERY_LANGUAGE, queryLn.getName());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		} catch (MalformedQueryException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		} catch (RepositoryException e) {
			logger.error("Repository error", e);
			throw new ClientHTTPException(SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}

	}

	protected void setDataset(HttpServletRequest request, RepositoryConnection repositoryCon, Query query)
			throws ClientHTTPException {
		// build a dataset, if specified
		String[] defaultGraphIRIs = request.getParameterValues(DEFAULT_GRAPH_PARAM_NAME);
		String[] namedGraphIRIs = request.getParameterValues(NAMED_GRAPH_PARAM_NAME);

		if (defaultGraphIRIs != null || namedGraphIRIs != null) {
			Dataset dataset = getDataset(repositoryCon.getValueFactory(), defaultGraphIRIs, namedGraphIRIs);
			query.setDataset(dataset);
		}
	}

	protected void setQueryTimeout(HttpServletRequest request, Query query) throws ClientHTTPException {
		int maxQueryTime = ProtocolUtil.parseTimeoutParam(request);
		if (maxQueryTime > 0) {
			query.setMaxQueryTime(maxQueryTime);
		}
	}

	protected void setInferred(HttpServletRequest request, Query query) throws ClientHTTPException {
		// determine if inferred triples should be included in query evaluation
		boolean includeInferred = getParam(request, INCLUDE_INFERRED_PARAM_NAME, true, Boolean.TYPE);
		query.setIncludeInferred(includeInferred);
	}

	protected void setBindings(HttpServletRequest request, Query query, ValueFactory valueFactory)
			throws ClientHTTPException {
		// determine if any variable bindings have been set on this query.
		Enumeration<String> parameterNames = request.getParameterNames();

		while (parameterNames.hasMoreElements()) {
			String parameterName = parameterNames.nextElement();

			if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
				String bindingName = parameterName.substring(BINDING_PREFIX.length());
				Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName, valueFactory);
				query.setBinding(bindingName, bindingValue);
			}
		}
	}

	protected SimpleDataset getDataset(ValueFactory valueFactory, String[] defaultGraphIRIs, String[] namedGraphIRIs)
			throws ClientHTTPException {

		SimpleDataset dataset = new SimpleDataset();

		if (defaultGraphIRIs != null) {
			for (String defaultGraphIRI : defaultGraphIRIs) {
				try {
					IRI iri = createIRIOrNull(valueFactory, defaultGraphIRI);
					dataset.addDefaultGraph(iri);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal IRI for default graph: " + defaultGraphIRI);
				}
			}
		}

		if (namedGraphIRIs != null) {
			for (String namedGraphIRI : namedGraphIRIs) {
				try {
					IRI iri = createIRIOrNull(valueFactory, namedGraphIRI);
					dataset.addNamedGraph(iri);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal IRI for named graph: " + namedGraphIRI);
				}
			}
		}

		return dataset;
	}

	private QueryLanguage getQueryLanguage(String queryLanguageParamName) throws ClientHTTPException {
		if (queryLanguageParamName != null) {
			logger.debug("query language param = {}", queryLanguageParamName);

			QueryLanguage queryLn = QueryLanguage.valueOf(queryLanguageParamName);
			if (queryLn == null) {
				throw new ClientHTTPException(SC_BAD_REQUEST, "Unknown query language: " + queryLanguageParamName);
			}
			return queryLn;
		} else {
			return QueryLanguage.SPARQL;
		}
	}

	private IRI createIRIOrNull(ValueFactory valueFactory, String graphIRI) {
		if ("null".equals(graphIRI)) {
			return null;
		}
		return valueFactory.createIRI(graphIRI);
	}

}
