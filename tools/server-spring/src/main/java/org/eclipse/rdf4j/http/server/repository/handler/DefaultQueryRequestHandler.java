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
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;

import static org.eclipse.rdf4j.http.protocol.Protocol.BINDING_PREFIX;
import static org.eclipse.rdf4j.http.protocol.Protocol.DEFAULT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INCLUDE_INFERRED_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.NAMED_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_LANGUAGE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_PARAM_NAME;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

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
import org.eclipse.rdf4j.http.server.repository.BooleanQueryResultView;
import org.eclipse.rdf4j.http.server.repository.GraphQueryResultView;
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
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriterRegistry;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterRegistry;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.View;

public class DefaultQueryRequestHandler extends AbstractQueryRequestHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public DefaultQueryRequestHandler(RepositoryResolver repositoryResolver) {
		super(repositoryResolver);
	}

	@Override
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
	protected View getViewFor(Query query) {
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
	protected FileFormatServiceRegistry<? extends FileFormat, ?> getResultWriterFor(Query query) {
		if (query instanceof TupleQuery) {
			return TupleQueryResultWriterRegistry.getInstance();
		} else if (query instanceof GraphQuery) {
			return RDFWriterRegistry.getInstance();
		} else if (query instanceof BooleanQuery) {
			return BooleanQueryResultWriterRegistry.getInstance();
		}

		return null;
	}

	@Override
	protected String getQueryString(HttpServletRequest request, RequestMethod requestMethod) throws HTTPException {

		String queryString;
		if (requestMethod == RequestMethod.POST) {
			String mimeType = HttpServerUtil.getMIMEType(request.getContentType());

			switch (mimeType) {
			case Protocol.SPARQL_QUERY_MIME_TYPE:
				// The query should be the entire body
				try {
					queryString = IOUtils.toString(request.getReader());
				} catch (IOException e) {
					throw new HTTPException(HttpStatus.SC_BAD_REQUEST, "Error reading request message body", e);
				}
				break;
			case Protocol.FORM_MIME_TYPE:
				queryString = request.getParameter(QUERY_PARAM_NAME);
				break;
			default:
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
	protected Query getQuery(HttpServletRequest request,
			RepositoryConnection repositoryCon, String queryString) throws IOException, HTTPException {

		QueryLanguage queryLn = getQueryLanguage(request.getParameter(QUERY_LANGUAGE_PARAM_NAME));
		String baseIRI = request.getParameter(Protocol.BASEURI_PARAM_NAME);

		try {
			Query query = repositoryCon.prepareQuery(queryLn, queryString, baseIRI);

			setQueryParameters(request, repositoryCon, query);

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

	protected void setQueryParameters(HttpServletRequest request, RepositoryConnection repositoryCon, Query query)
			throws ClientHTTPException {
		// determine if inferred triples should be included in query evaluation
		query.setIncludeInferred(getIncludeInferred(request));

		int maxExecutionTime = getMaxExecutionTime(request);
		if (maxExecutionTime > 0) {
			query.setMaxExecutionTime(maxExecutionTime);
		}

		Dataset dataset = getDataset(request, repositoryCon.getValueFactory(), query);
		if (dataset != null) {
			query.setDataset(dataset);
		}

		// determine if any variable bindings have been set on this query.
		Enumeration<String> parameterNames = request.getParameterNames();

		while (parameterNames.hasMoreElements()) {
			String parameterName = parameterNames.nextElement();

			if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
				String bindingName = parameterName.substring(BINDING_PREFIX.length());
				Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
						repositoryCon.getValueFactory());
				query.setBinding(bindingName, bindingValue);
			}
		}
	}

	protected int getMaxExecutionTime(HttpServletRequest request) throws ClientHTTPException {
		return ProtocolUtil.parseTimeoutParam(request);
	}

	protected boolean getIncludeInferred(HttpServletRequest request) throws ClientHTTPException {
		return getParam(request, INCLUDE_INFERRED_PARAM_NAME, true, Boolean.TYPE);
	}

	protected SimpleDataset getDataset(HttpServletRequest request, ValueFactory valueFactory, Query query)
			throws ClientHTTPException {

		String[] defaultGraphIRIs = request.getParameterValues(DEFAULT_GRAPH_PARAM_NAME);
		String[] namedGraphIRIs = request.getParameterValues(NAMED_GRAPH_PARAM_NAME);

		if (defaultGraphIRIs == null && namedGraphIRIs == null) {
			return null;
		}

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

	protected QueryLanguage getQueryLanguage(String queryLanguageParamName) throws ClientHTTPException {
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
