/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;

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
import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.common.webapp.util.HttpServerUtil;
import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
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
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.SystemRepository;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles queries and admin (delete) operations on a repository and renders the
 * results in a format suitable to the type of operation.
 * 
 * @author Herko ter Horst
 */
public class RepositoryController extends AbstractController {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private RepositoryManager repositoryManager;

	private static final String METHOD_DELETE = "DELETE";

	public RepositoryController()
		throws ApplicationContextException
	{
		setSupportedMethods(new String[] { METHOD_GET, METHOD_POST, METHOD_DELETE, METHOD_HEAD });
	}

	public void setRepositoryManager(RepositoryManager repMan) {
		repositoryManager = repMan;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
		throws Exception
	{
		String reqMethod = request.getMethod();
		String queryStr = request.getParameter(QUERY_PARAM_NAME);
		
		if (METHOD_POST.equals(reqMethod)) {
			String mimeType = HttpServerUtil.getMIMEType(request.getContentType());
			
			if (!(Protocol.FORM_MIME_TYPE.equals(mimeType) || Protocol.SPARQL_QUERY_MIME_TYPE.equals(mimeType))) {
				throw new ClientHTTPException(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported MIME type: " + mimeType);
			}
			
			if (Protocol.SPARQL_QUERY_MIME_TYPE.equals(mimeType)) {
				// The query should be the entire body
				try {
					queryStr = IOUtils.toString(request.getReader());
				}
				catch (IOException e) {
					throw new HTTPException(HttpStatus.SC_BAD_REQUEST, "Error reading request message body", e);
				}
				if (queryStr.isEmpty()) queryStr = null;
			}
		}
		else if (METHOD_DELETE.equals(reqMethod)) {
			String repId = RepositoryInterceptor.getRepositoryID(request);
			logger.info("DELETE request invoked for repository '" + repId + "'");

			if (queryStr != null) {
				logger.warn("query supplied on repository delete request, aborting delete");
				throw new HTTPException(HttpStatus.SC_BAD_REQUEST,
						"Repository delete error: query supplied with request");
			}

			if (SystemRepository.ID.equals(repId)) {
				logger.warn("attempted delete of SYSTEM repository, aborting");
				throw new HTTPException(HttpStatus.SC_FORBIDDEN, "SYSTEM Repository can not be deleted");
			}

			try {
				// we need to forcibly close the default repository connection
				// opened for this repository by
				// the interceptor.
				RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request);
				synchronized (repositoryCon) {
					repositoryCon.close();
				}

				boolean success = repositoryManager.removeRepository(repId);
				if (success) {
					logger.info("DELETE request successfully completed");
					return new ModelAndView(EmptySuccessView.getInstance());
				}
				else {
					logger.error("error while attempting to delete repository '" + repId + "'");
					throw new HTTPException(HttpStatus.SC_BAD_REQUEST,
							"could not locate repository configuration for repository '" + repId + "'.");
				}
			}
			catch (OpenRDFException e) {
				logger.error("error while attempting to delete repository '" + repId + "'", e);
				throw new ServerHTTPException("Repository delete error: " + e.getMessage(), e);
			}
		}

		Repository repository = RepositoryInterceptor.getRepository(request);

		int qryCode = 0;
		if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
			qryCode = String.valueOf(queryStr).hashCode();
		}

		boolean headersOnly = false;
		if (METHOD_GET.equals(reqMethod)) {
			logger.info("GET query {}", qryCode);
		}
		else if (METHOD_HEAD.equals(reqMethod)) {
			logger.info("HEAD query {}", qryCode);
			headersOnly = true;
		}
		else if (METHOD_POST.equals(reqMethod)) {
			logger.info("POST query {}", qryCode);
		}

		logger.debug("query {} = {}", qryCode, queryStr);

		if (queryStr != null) {
			RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request);
			synchronized (repositoryCon) {
				Query query = getQuery(repository, repositoryCon, queryStr, request, response);

				View view;
				Object queryResult;
				FileFormatServiceRegistry<? extends FileFormat, ?> registry;

				try {
					if (query instanceof TupleQuery) {
						TupleQuery tQuery = (TupleQuery)query;

						queryResult = headersOnly ? null : tQuery.evaluate();
						registry = TupleQueryResultWriterRegistry.getInstance();
						view = TupleQueryResultView.getInstance();
					}
					else if (query instanceof GraphQuery) {
						GraphQuery gQuery = (GraphQuery)query;

						queryResult = headersOnly ? null : gQuery.evaluate();
						registry = RDFWriterRegistry.getInstance();
						view = GraphQueryResultView.getInstance();
					}
					else if (query instanceof BooleanQuery) {
						BooleanQuery bQuery = (BooleanQuery)query;

						queryResult = headersOnly ? null : bQuery.evaluate();
						registry = BooleanQueryResultWriterRegistry.getInstance();
						view = BooleanQueryResultView.getInstance();
					}
					else {
						throw new ClientHTTPException(SC_BAD_REQUEST, "Unsupported query type: "
								+ query.getClass().getName());
					}
				}
				catch (QueryInterruptedException e) {
					logger.info("Query interrupted", e);
					throw new ServerHTTPException(SC_SERVICE_UNAVAILABLE, "Query evaluation took too long");
				}
				catch (QueryEvaluationException e) {
					logger.info("Query evaluation error", e);
					if (e.getCause() != null && e.getCause() instanceof HTTPException) {
						// custom signal from the backend, throw as HTTPException
						// directly (see SES-1016).
						throw (HTTPException)e.getCause();
					}
					else {
						throw new ServerHTTPException("Query evaluation error: " + e.getMessage());
					}
				}
				Object factory = ProtocolUtil.getAcceptableService(request, response, registry);

				Map<String, Object> model = new HashMap<String, Object>();
				model.put(QueryResultView.FILENAME_HINT_KEY, "query-result");
				model.put(QueryResultView.QUERY_RESULT_KEY, queryResult);
				model.put(QueryResultView.FACTORY_KEY, factory);
				model.put(QueryResultView.HEADERS_ONLY, headersOnly);

				return new ModelAndView(view, model);
			}
		}
		else {
			throw new ClientHTTPException(SC_BAD_REQUEST, "Missing parameter: " + QUERY_PARAM_NAME);
		}
	}

	private Query getQuery(Repository repository, RepositoryConnection repositoryCon, String queryStr,
			HttpServletRequest request, HttpServletResponse response)
		throws IOException, ClientHTTPException
	{
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
			}
			catch (NumberFormatException e) {
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
						IRI uri = createURIOrNull(repository, defaultGraphURI);
						dataset.addDefaultGraph(uri);
					}
					catch (IllegalArgumentException e) {
						throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for default graph: "
								+ defaultGraphURI);
					}
				}
			}

			if (namedGraphURIs != null) {
				for (String namedGraphURI : namedGraphURIs) {
					try {
						IRI uri = createURIOrNull(repository, namedGraphURI);
						dataset.addNamedGraph(uri);
					}
					catch (IllegalArgumentException e) {
						throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for named graph: "
								+ namedGraphURI);
					}
				}
			}
		}

		try {
			result = repositoryCon.prepareQuery(queryLn, queryStr, baseURI);

			result.setIncludeInferred(includeInferred);

			if (maxQueryTime > 0) {
				result.setMaxQueryTime(maxQueryTime);
			}

			if (dataset != null) {
				result.setDataset(dataset);
			}

			// determine if any variable bindings have been set on this query.
			@SuppressWarnings("unchecked")
			Enumeration<String> parameterNames = request.getParameterNames();

			while (parameterNames.hasMoreElements()) {
				String parameterName = parameterNames.nextElement();

				if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length())
				{
					String bindingName = parameterName.substring(BINDING_PREFIX.length());
					Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
							repository.getValueFactory());
					result.setBinding(bindingName, bindingValue);
				}
			}
		}
		catch (UnsupportedQueryLanguageException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.UNSUPPORTED_QUERY_LANGUAGE, queryLn.getName());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		}
		catch (MalformedQueryException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		}
		catch (RepositoryException e) {
			logger.error("Repository error", e);
			response.sendError(SC_INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	private IRI createURIOrNull(Repository repository, String graphURI) {
		if ("null".equals(graphURI))
			return null;
		return repository.getValueFactory().createIRI(graphURI);
	}
}
