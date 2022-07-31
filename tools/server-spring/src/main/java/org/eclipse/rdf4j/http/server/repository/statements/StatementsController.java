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
package org.eclipse.rdf4j.http.server.repository.statements;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;

import static org.eclipse.rdf4j.http.protocol.Protocol.BASEURI_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.BINDING_PREFIX;
import static org.eclipse.rdf4j.http.protocol.Protocol.CONTEXT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INCLUDE_INFERRED_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INSERT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.OBJECT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.PREDICATE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_LANGUAGE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.REMOVE_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.SUBJECT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.USING_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.USING_NAMED_GRAPH_PARAM_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.webapp.util.HttpServerUtil;
import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.http.protocol.transaction.TransactionReader;
import org.eclipse.rdf4j.http.protocol.transaction.operations.TransactionOperation;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Handles requests for manipulating the statements in a repository.
 *
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class StatementsController extends AbstractController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public StatementsController() throws ApplicationContextException {
		setSupportedMethods(new String[] { METHOD_GET, METHOD_POST, METHOD_HEAD, "PUT", "DELETE" });
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		ModelAndView result;

		Repository repository = RepositoryInterceptor.getRepository(request);

		String reqMethod = request.getMethod();

		if (METHOD_GET.equals(reqMethod)) {
			logger.info("GET statements");
			result = getExportStatementsResult(repository, request, response);
		} else if (METHOD_HEAD.equals(reqMethod)) {
			logger.info("HEAD statements");
			result = getExportStatementsResult(repository, request, response);
		} else if (METHOD_POST.equals(reqMethod)) {
			String mimeType = HttpServerUtil.getMIMEType(request.getContentType());

			if (Protocol.TXN_MIME_TYPE.equals(mimeType)) {
				logger.info("POST transaction to repository");
				result = getTransactionResultResult(repository, request, response);
			} else if (Protocol.SPARQL_UPDATE_MIME_TYPE.equals(mimeType)
					|| request.getParameterMap().containsKey(Protocol.UPDATE_PARAM_NAME)) {
				logger.info("POST SPARQL update request to repository");
				result = getSparqlUpdateResult(repository, request, response);
			} else {
				logger.info("POST data to repository");
				result = getAddDataResult(repository, request, response, false);
			}
		} else if ("PUT".equals(reqMethod)) {
			logger.info("PUT data in repository");
			result = getAddDataResult(repository, request, response, true);
		} else if ("DELETE".equals(reqMethod)) {
			logger.info("DELETE data from repository");
			result = getDeleteDataResult(repository, request, response);
		} else {
			throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
					"Method not allowed: " + reqMethod);
		}

		return result;
	}

	private ModelAndView getSparqlUpdateResult(Repository repository, HttpServletRequest request,
			HttpServletResponse response) throws ServerHTTPException, ClientHTTPException, HTTPException {
		ProtocolUtil.logRequestParameters(request);

		String mimeType = HttpServerUtil.getMIMEType(request.getContentType());

		String sparqlUpdateString;
		if (Protocol.SPARQL_UPDATE_MIME_TYPE.equals(mimeType)) {
			// The query should be the entire body
			try {
				sparqlUpdateString = IOUtils.toString(request.getReader());
			} catch (IOException e) {
				throw new ClientHTTPException(SC_BAD_REQUEST, "Error reading request message body", e);
			}
		} else {
			sparqlUpdateString = request.getParameterValues(Protocol.UPDATE_PARAM_NAME)[0];
		}

		if (sparqlUpdateString.isEmpty()) {
			throw new ClientHTTPException("Updates must be non-empty");
		}

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

		SimpleDataset dataset = null;
		if (defaultRemoveGraphURIs != null || defaultInsertGraphURIs != null || defaultGraphURIs != null
				|| namedGraphURIs != null) {
			dataset = new SimpleDataset();
		}

		if (defaultRemoveGraphURIs != null) {
			for (String graphURI : defaultRemoveGraphURIs) {
				try {
					IRI uri = createURIOrNull(repository, graphURI);
					dataset.addDefaultRemoveGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for default remove graph: " + graphURI);
				}
			}
		}

		if (defaultInsertGraphURIs != null && defaultInsertGraphURIs.length > 0) {
			String graphURI = defaultInsertGraphURIs[0];
			try {
				IRI uri = createURIOrNull(repository, graphURI);
				dataset.setDefaultInsertGraph(uri);
			} catch (IllegalArgumentException e) {
				throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for default insert graph: " + graphURI);
			}
		}

		if (defaultGraphURIs != null) {
			for (String defaultGraphURI : defaultGraphURIs) {
				try {
					IRI uri = createURIOrNull(repository, defaultGraphURI);
					dataset.addDefaultGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for default graph: " + defaultGraphURI);
				}
			}
		}

		if (namedGraphURIs != null) {
			for (String namedGraphURI : namedGraphURIs) {
				try {
					IRI uri = createURIOrNull(repository, namedGraphURI);
					dataset.addNamedGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for named graph: " + namedGraphURI);
				}
			}
		}

		final int maxQueryTime = ProtocolUtil.parseTimeoutParam(request);
		try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
			Update update = repositoryCon.prepareUpdate(queryLn, sparqlUpdateString, baseURI);

			update.setIncludeInferred(includeInferred);
			update.setMaxExecutionTime(maxQueryTime);

			if (dataset != null) {
				update.setDataset(dataset);
			}

			// determine if any variable bindings have been set on this
			// update.
			@SuppressWarnings("unchecked")
			Enumeration<String> parameterNames = request.getParameterNames();

			while (parameterNames.hasMoreElements()) {
				String parameterName = parameterNames.nextElement();

				if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
					String bindingName = parameterName.substring(BINDING_PREFIX.length());
					Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
							repository.getValueFactory());
					update.setBinding(bindingName, bindingValue);
				}
			}

			update.execute();

			return new ModelAndView(EmptySuccessView.getInstance());
		} catch (QueryInterruptedException e) {
			throw new ServerHTTPException(SC_SERVICE_UNAVAILABLE, "update execution took too long");
		} catch (UpdateExecutionException | RepositoryException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException
				// directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new ServerHTTPException("Repository update error: " + e.getMessage(), e);
			}
		}
		// custom signal from the backend, throw as HTTPException
		// directly
		// (see SES-1016).
		catch (MalformedQueryException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		}
	}

	private IRI createURIOrNull(Repository repository, String graphURI) {
		if ("null".equals(graphURI)) {
			return null;
		}
		return repository.getValueFactory().createIRI(graphURI);
	}

	/**
	 * Get all statements and export them as RDF.
	 *
	 * @return a model and view for exporting the statements.
	 */
	private ModelAndView getExportStatementsResult(Repository repository, HttpServletRequest request,
			HttpServletResponse response) throws ClientHTTPException {
		ProtocolUtil.logRequestParameters(request);

		ValueFactory vf = repository.getValueFactory();

		Resource subj = ProtocolUtil.parseResourceParam(request, SUBJECT_PARAM_NAME, vf);
		IRI pred = ProtocolUtil.parseURIParam(request, PREDICATE_PARAM_NAME, vf);
		Value obj = ProtocolUtil.parseValueParam(request, OBJECT_PARAM_NAME, vf);
		Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);
		boolean useInferencing = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

		RDFWriterFactory rdfWriterFactory = ProtocolUtil.getAcceptableService(request, response,
				RDFWriterRegistry.getInstance());

		Map<String, Object> model = new HashMap<>();
		model.put(ExportStatementsView.SUBJECT_KEY, subj);
		model.put(ExportStatementsView.PREDICATE_KEY, pred);
		model.put(ExportStatementsView.OBJECT_KEY, obj);
		model.put(ExportStatementsView.CONTEXTS_KEY, contexts);
		model.put(ExportStatementsView.USE_INFERENCING_KEY, Boolean.valueOf(useInferencing));
		model.put(ExportStatementsView.FACTORY_KEY, rdfWriterFactory);
		model.put(ExportStatementsView.HEADERS_ONLY, METHOD_HEAD.equals(request.getMethod()));
		return new ModelAndView(ExportStatementsView.getInstance(), model);
	}

	/**
	 * Process several actions as a transaction.
	 */
	private ModelAndView getTransactionResultResult(Repository repository, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ClientHTTPException, ServerHTTPException, HTTPException {
		InputStream in = request.getInputStream();
		try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
			logger.debug("Processing transaction...");

			TransactionReader reader = new TransactionReader();
			Iterable<? extends TransactionOperation> txn = reader.parse(in);

			repositoryCon.begin();

			for (TransactionOperation op : txn) {
				op.execute(repositoryCon);
			}

			repositoryCon.commit();
			logger.debug("Transaction processed ");

			return new ModelAndView(EmptySuccessView.getInstance());
		} catch (SAXParseException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_DATA, e.getMessage());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		} catch (SAXException e) {
			throw new ServerHTTPException("Failed to parse transaction data: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new ServerHTTPException("Failed to read data: " + e.getMessage(), e);
		} catch (RepositoryException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException
				// directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new ServerHTTPException("Repository update error: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Upload data to the repository.
	 */
	private ModelAndView getAddDataResult(Repository repository, HttpServletRequest request,
			HttpServletResponse response, boolean replaceCurrent)
			throws IOException, ServerHTTPException, ClientHTTPException, HTTPException {
		ProtocolUtil.logRequestParameters(request);

		String mimeType = HttpServerUtil.getMIMEType(request.getContentType());

		RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(mimeType)
				.orElseThrow(
						() -> new ClientHTTPException(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported MIME type: " + mimeType));

		ValueFactory vf = repository.getValueFactory();

		Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);
		IRI baseURI = ProtocolUtil.parseURIParam(request, BASEURI_PARAM_NAME, vf);
		final boolean preserveNodeIds = ProtocolUtil.parseBooleanParam(request, Protocol.PRESERVE_BNODE_ID_PARAM_NAME,
				false);

		String baseURIString = null;
		if (baseURI != null) {
			baseURIString = baseURI.toString();
		}

		InputStream in = request.getInputStream();
		try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
			repositoryCon.begin();

			if (preserveNodeIds) {
				repositoryCon.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			}

			if (replaceCurrent) {
				repositoryCon.clear(contexts);
			}
			repositoryCon.add(in, baseURIString, rdfFormat, contexts);

			repositoryCon.commit();

			return new ModelAndView(EmptySuccessView.getInstance());
		} catch (UnsupportedRDFormatException e) {
			throw new ClientHTTPException(SC_UNSUPPORTED_MEDIA_TYPE,
					"No RDF parser available for format " + rdfFormat.getName());
		} catch (RDFParseException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_DATA, e.getMessage());
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		} catch (IOException e) {
			throw new ServerHTTPException("Failed to read data: " + e.getMessage(), e);
		} catch (RepositoryException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException
				// directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new ServerHTTPException("Repository update error: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Delete data from the repository.
	 */
	private ModelAndView getDeleteDataResult(Repository repository, HttpServletRequest request,
			HttpServletResponse response) throws ServerHTTPException, ClientHTTPException, HTTPException {
		ProtocolUtil.logRequestParameters(request);

		ValueFactory vf = repository.getValueFactory();

		Resource subj = ProtocolUtil.parseResourceParam(request, SUBJECT_PARAM_NAME, vf);
		IRI pred = ProtocolUtil.parseURIParam(request, PREDICATE_PARAM_NAME, vf);
		Value obj = ProtocolUtil.parseValueParam(request, OBJECT_PARAM_NAME, vf);
		Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);

		try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
			repositoryCon.remove(subj, pred, obj, contexts);

			return new ModelAndView(EmptySuccessView.getInstance());
		} catch (RepositoryException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException
				// directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new ServerHTTPException("Repository update error: " + e.getMessage(), e);
			}
		}
	}
}
