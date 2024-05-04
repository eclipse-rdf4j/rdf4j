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
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;

import static org.eclipse.rdf4j.http.protocol.Protocol.BINDING_PREFIX;
import static org.eclipse.rdf4j.http.protocol.Protocol.INCLUDE_INFERRED_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INSERT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_LANGUAGE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.REMOVE_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.USING_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.USING_NAMED_GRAPH_PARAM_NAME;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
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
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author jeen
 *
 */
public class UpdateController extends AbstractActionController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected ModelAndView handleAction(HttpServletRequest request, HttpServletResponse response,
			Transaction transaction)
			throws Exception {

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
}
