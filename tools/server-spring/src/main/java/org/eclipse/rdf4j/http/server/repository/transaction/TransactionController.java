/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.transaction;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import static org.eclipse.rdf4j.http.protocol.Protocol.BINDING_PREFIX;
import static org.eclipse.rdf4j.http.protocol.Protocol.CONTEXT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.DEFAULT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INCLUDE_INFERRED_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INSERT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.NAMED_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.OBJECT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.PREDICATE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_LANGUAGE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.REMOVE_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.SUBJECT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.USING_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.USING_NAMED_GRAPH_PARAM_NAME;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.Action;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.BooleanQueryResultView;
import org.eclipse.rdf4j.http.server.repository.GraphQueryResultView;
import org.eclipse.rdf4j.http.server.repository.QueryResultView;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.http.server.repository.TupleQueryResultView;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
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
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriterRegistry;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterRegistry;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests for transaction creation on a repository.
 *
 * @author Jeen Broekstra
 */
public class TransactionController extends AbstractController implements DisposableBean {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public TransactionController() throws ApplicationContextException {
		setSupportedMethods(new String[] { METHOD_POST, "PUT", "DELETE" });
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		ModelAndView result;

		String reqMethod = request.getMethod();
		UUID transactionId = getTransactionID(request);
		logger.debug("transaction id: {}", transactionId);
		logger.debug("request content type: {}", request.getContentType());

		Transaction transaction = ActiveTransactionRegistry.INSTANCE.getTransaction(transactionId);

		if (transaction == null) {
			logger.warn("could not find transaction for transaction id {}", transactionId);
			throw new ClientHTTPException(SC_BAD_REQUEST,
					"unable to find registered transaction for transaction id '" + transactionId + "'");
		}

		// if no action is specified in the request, it's a rollback (since it's
		// the only txn operation that does not require the action parameter).
		final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
		final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
		switch (action) {
		case QUERY:
			// TODO SES-2238 note that we allow POST requests for backward
			// compatibility reasons with earlier
			// 2.8.x releases, even though according to the protocol spec only
			// PUT is allowed.
			if ("PUT".equals(reqMethod) || METHOD_POST.equals(reqMethod)) {
				logger.info("{} txn query request", reqMethod);
				result = processQuery(transaction, request, response);
				logger.info("{} txn query request finished", reqMethod);
			} else {
				throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
						"Method not allowed: " + reqMethod);
			}
			break;
		case GET:
			if ("PUT".equals(reqMethod) || METHOD_POST.equals(reqMethod)) {
				logger.info("{} txn get/export statements request", reqMethod);
				result = getExportStatementsResult(transaction, request, response);
				logger.info("{} txn get/export statements request finished", reqMethod);
			} else {
				throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
						"Method not allowed: " + reqMethod);
			}
			break;
		case SIZE:
			if ("PUT".equals(reqMethod) || METHOD_POST.equals(reqMethod)) {
				logger.info("{} txn size request", reqMethod);
				result = getSize(transaction, request, response);
				logger.info("{} txn size request finished", reqMethod);
			} else {
				throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
						"Method not allowed: " + reqMethod);
			}
			break;
		case PING:
			String text = Long.toString(ActiveTransactionRegistry.INSTANCE.getTimeout(TimeUnit.MILLISECONDS));
			Map<String, String> model = Collections.singletonMap(SimpleResponseView.CONTENT_KEY, text);
			result = new ModelAndView(SimpleResponseView.getInstance(), model);
			break;
		default:
			// TODO Action.ROLLBACK check is for backward compatibility with
			// older 2.8.x releases only. It's not in the protocol spec.
			if ("DELETE".equals(reqMethod)
					|| (action.equals(Action.ROLLBACK) && ("PUT".equals(reqMethod) || METHOD_POST.equals(reqMethod)))) {
				logger.info("transaction rollback");
				try {
					transaction.rollback();
				} finally {
					try {
						transaction.close();
					} finally {
						ActiveTransactionRegistry.INSTANCE.deregister(transaction);
					}
				}
				result = new ModelAndView(EmptySuccessView.getInstance());
				logger.info("transaction rollback request finished.");
			} else if ("PUT".equals(reqMethod) || METHOD_POST.equals(reqMethod)) {
				// TODO filter for appropriate PUT operations
				logger.info("{} txn operation", reqMethod);
				result = processModificationOperation(transaction, action, request, response);
				logger.info("PUT txn operation request finished.");
			} else {
				throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
						"Method not allowed: " + reqMethod);
			}
			break;
		}
		if (!(transaction.isClosed() || transaction.isComplete())) {
			ActiveTransactionRegistry.INSTANCE.active(transaction);
		}
		return result;
	}

	private UUID getTransactionID(HttpServletRequest request) throws ClientHTTPException {
		String pathInfoStr = request.getPathInfo();

		UUID txnID = null;

		if (pathInfoStr != null && !pathInfoStr.equals("/")) {
			String[] pathInfo = pathInfoStr.substring(1).split("/");
			// should be of the form: /<Repository>/transactions/<txnID>
			if (pathInfo.length == 3) {
				try {
					txnID = UUID.fromString(pathInfo[2]);
					logger.debug("txnID is '{}'", txnID);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "not a valid transaction id: " + pathInfo[2]);
				}
			} else {
				logger.warn("could not determine transaction id from path info {} ", pathInfoStr);
			}
		}

		return txnID;
	}

	private ModelAndView processModificationOperation(Transaction transaction, Action action,
			HttpServletRequest request, HttpServletResponse response) throws IOException, HTTPException {
		ProtocolUtil.logRequestParameters(request);

		Map<String, Object> model = new HashMap<>();

		String baseURI = request.getParameter(Protocol.BASEURI_PARAM_NAME);
		if (baseURI == null) {
			baseURI = "";
		}

		final Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME,
				SimpleValueFactory.getInstance());

		final boolean preserveNodeIds = ProtocolUtil.parseBooleanParam(request, Protocol.PRESERVE_BNODE_ID_PARAM_NAME,
				false);

		try {
			RDFFormat format;
			switch (action) {
			case ADD:
				format = Rio.getParserFormatForMIMEType(request.getContentType())
						.orElseThrow(Rio.unsupportedFormat(request.getContentType()));
				transaction.add(request.getInputStream(), baseURI, format, preserveNodeIds, contexts);
				break;
			case DELETE:
				format = Rio.getParserFormatForMIMEType(request.getContentType())
						.orElseThrow(Rio.unsupportedFormat(request.getContentType()));
				transaction.delete(format, request.getInputStream(), baseURI);
				break;
			case UPDATE:
				return getSparqlUpdateResult(transaction, request, response);
			case PREPARE:
				transaction.prepare();
				break;
			case COMMIT:
				transaction.commit();
				// If commit fails with an exception, deregister should be skipped so the user
				// has a chance to do a proper rollback. See #725.
				ActiveTransactionRegistry.INSTANCE.deregister(transaction);
				break;
			default:
				logger.warn("transaction modification action '{}' not recognized", action);
				throw new ClientHTTPException("modification action not recognized: " + action);
			}

			model.put(SimpleResponseView.SC_KEY, HttpServletResponse.SC_OK);
			return new ModelAndView(SimpleResponseView.getInstance(), model);
		} catch (Exception e) {
			if (e instanceof ClientHTTPException) {
				throw (ClientHTTPException) e;
			} else {
				throw new ServerHTTPException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Transaction handling error: " + e.getMessage(), e);
			}
		}
	}

	private ModelAndView getSize(Transaction transaction, HttpServletRequest request, HttpServletResponse response)
			throws HTTPException {
		ProtocolUtil.logRequestParameters(request);

		Map<String, Object> model = new HashMap<>();
		final boolean headersOnly = METHOD_HEAD.equals(request.getMethod());

		if (!headersOnly) {
			Repository repository = RepositoryInterceptor.getRepository(request);

			ValueFactory vf = repository.getValueFactory();
			Resource[] contexts = ProtocolUtil.parseContextParam(request, Protocol.CONTEXT_PARAM_NAME, vf);

			long size;

			try {
				size = transaction.getSize(contexts);
			} catch (RepositoryException | InterruptedException | ExecutionException e) {
				throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
			}
			model.put(SimpleResponseView.CONTENT_KEY, String.valueOf(size));
		}

		return new ModelAndView(SimpleResponseView.getInstance(), model);
	}

	/**
	 * Get all statements and export them as RDF.
	 *
	 * @return a model and view for exporting the statements.
	 */
	private ModelAndView getExportStatementsResult(Transaction transaction, HttpServletRequest request,
			HttpServletResponse response) throws ClientHTTPException {
		ProtocolUtil.logRequestParameters(request);

		ValueFactory vf = SimpleValueFactory.getInstance();

		Resource subj = ProtocolUtil.parseResourceParam(request, SUBJECT_PARAM_NAME, vf);
		IRI pred = ProtocolUtil.parseURIParam(request, PREDICATE_PARAM_NAME, vf);
		Value obj = ProtocolUtil.parseValueParam(request, OBJECT_PARAM_NAME, vf);
		Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);
		boolean useInferencing = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

		RDFWriterFactory rdfWriterFactory = ProtocolUtil.getAcceptableService(request, response,
				RDFWriterRegistry.getInstance());

		Map<String, Object> model = new HashMap<>();
		model.put(TransactionExportStatementsView.SUBJECT_KEY, subj);
		model.put(TransactionExportStatementsView.PREDICATE_KEY, pred);
		model.put(TransactionExportStatementsView.OBJECT_KEY, obj);
		model.put(TransactionExportStatementsView.CONTEXTS_KEY, contexts);
		model.put(TransactionExportStatementsView.USE_INFERENCING_KEY, Boolean.valueOf(useInferencing));
		model.put(TransactionExportStatementsView.FACTORY_KEY, rdfWriterFactory);
		model.put(TransactionExportStatementsView.HEADERS_ONLY, METHOD_HEAD.equals(request.getMethod()));

		model.put(TransactionExportStatementsView.TRANSACTION_KEY, transaction);
		return new ModelAndView(TransactionExportStatementsView.getInstance(), model);
	}

	/**
	 * Evaluates a query on the given connection and returns the resulting {@link QueryResultView}. The
	 * {@link QueryResultView} will take care of correctly releasing the connection back to the
	 * {@link ActiveTransactionRegistry}, after fully rendering the query result for sending over the wire.
	 */
	private ModelAndView processQuery(Transaction txn, HttpServletRequest request, HttpServletResponse response)
			throws IOException, HTTPException {
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
			Query query = getQuery(txn, queryStr, request, response);

			if (query instanceof TupleQuery) {
				TupleQuery tQuery = (TupleQuery) query;

				queryResult = txn.evaluate(tQuery);
				registry = TupleQueryResultWriterRegistry.getInstance();
				view = TupleQueryResultView.getInstance();
			} else if (query instanceof GraphQuery) {
				GraphQuery gQuery = (GraphQuery) query;

				queryResult = txn.evaluate(gQuery);
				registry = RDFWriterRegistry.getInstance();
				view = GraphQueryResultView.getInstance();
			} else if (query instanceof BooleanQuery) {
				BooleanQuery bQuery = (BooleanQuery) query;

				queryResult = txn.evaluate(bQuery);
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
		model.put(QueryResultView.HEADERS_ONLY, false); // TODO needed for HEAD
		// requests.
		return new ModelAndView(view, model);
	}

	private static Charset getCharset(HttpServletRequest request) {
		return request.getCharacterEncoding() != null ? Charset.forName(request.getCharacterEncoding())
				: StandardCharsets.UTF_8;
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

	private ModelAndView getSparqlUpdateResult(Transaction transaction, HttpServletRequest request,
			HttpServletResponse response) throws ServerHTTPException, ClientHTTPException, HTTPException {
		String sparqlUpdateString;
		final String contentType = request.getContentType();
		if (contentType != null && contentType.contains(Protocol.SPARQL_UPDATE_MIME_TYPE)) {
			try {
				Charset charset = getCharset(request);
				sparqlUpdateString = IOUtils.toString(request.getInputStream(), charset);
			} catch (IOException e) {
				logger.warn("error reading sparql update string from request body", e);
				throw new ClientHTTPException(SC_BAD_REQUEST,
						"could not read SPARQL update string from body: " + e.getMessage());
			}
		} else {
			sparqlUpdateString = request.getParameter(Protocol.UPDATE_PARAM_NAME);
		}

		if (null == sparqlUpdateString) {
			throw new ClientHTTPException(SC_NOT_ACCEPTABLE, "Could not read SPARQL update string from body.");
		}

		logger.debug("SPARQL update string: {}", sparqlUpdateString);

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

		// build a dataset, if specified
		String[] defaultRemoveGraphURIs = request.getParameterValues(REMOVE_GRAPH_PARAM_NAME);
		String[] defaultInsertGraphURIs = request.getParameterValues(INSERT_GRAPH_PARAM_NAME);
		String[] defaultGraphURIs = request.getParameterValues(USING_GRAPH_PARAM_NAME);
		String[] namedGraphURIs = request.getParameterValues(USING_NAMED_GRAPH_PARAM_NAME);

		SimpleDataset dataset = new SimpleDataset();

		if (defaultRemoveGraphURIs != null) {
			for (String graphURI : defaultRemoveGraphURIs) {
				try {
					IRI uri = null;
					if (!"null".equals(graphURI)) {
						uri = SimpleValueFactory.getInstance().createIRI(graphURI);
					}
					dataset.addDefaultRemoveGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for default remove graph: " + graphURI);
				}
			}
		}

		if (defaultInsertGraphURIs != null && defaultInsertGraphURIs.length > 0) {
			String graphURI = defaultInsertGraphURIs[0];
			try {
				IRI uri = null;
				if (!"null".equals(graphURI)) {
					uri = SimpleValueFactory.getInstance().createIRI(graphURI);
				}
				dataset.setDefaultInsertGraph(uri);
			} catch (IllegalArgumentException e) {
				throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for default insert graph: " + graphURI);
			}
		}

		if (defaultGraphURIs != null) {
			for (String defaultGraphURI : defaultGraphURIs) {
				try {
					IRI uri = null;
					if (!"null".equals(defaultGraphURI)) {
						uri = SimpleValueFactory.getInstance().createIRI(defaultGraphURI);
					}
					dataset.addDefaultGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for default graph: " + defaultGraphURI);
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

		try {
			// determine if any variable bindings have been set on this update.
			@SuppressWarnings("unchecked")
			Enumeration<String> parameterNames = request.getParameterNames();

			Map<String, Value> bindings = new HashMap<>();
			while (parameterNames.hasMoreElements()) {
				String parameterName = parameterNames.nextElement();

				if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
					String bindingName = parameterName.substring(BINDING_PREFIX.length());
					Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
							SimpleValueFactory.getInstance());
					bindings.put(bindingName, bindingValue);
				}
			}

			transaction.executeUpdate(queryLn, sparqlUpdateString, baseURI, includeInferred, dataset, bindings);

			return new ModelAndView(EmptySuccessView.getInstance());
		} catch (UpdateExecutionException | InterruptedException | ExecutionException | RepositoryException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new ServerHTTPException("Repository update error: " + e.getMessage(), e);
			}
		}
		// custom signal from the backend, throw as HTTPException directly
		// (see SES-1016).
		catch (MalformedQueryException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		}
	}

	// Comes from disposableBean interface so to be able to stop the ActiveTransactionRegistry scheduler
	@Override
	public void destroy()
			throws Exception {
		ActiveTransactionRegistry.INSTANCE.destroyScheduler();
	}

}
