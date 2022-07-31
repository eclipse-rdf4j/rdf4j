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
package org.eclipse.rdf4j.http.server.repository.namespaces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.QueryResultView;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.http.server.repository.TupleQueryResultView;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterRegistry;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests for the list of namespace definitions for a repository.
 *
 * @author Herko ter Horst
 */
public class NamespacesController extends AbstractController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public NamespacesController() throws ApplicationContextException {
		setSupportedMethods(new String[] { METHOD_GET, METHOD_HEAD, "DELETE" });
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String reqMethod = request.getMethod();
		if (METHOD_GET.equals(reqMethod)) {
			logger.info("GET namespace list");
			return getExportNamespacesResult(request, response);
		}
		if (METHOD_HEAD.equals(reqMethod)) {
			logger.info("HEAD namespace list");
			return getExportNamespacesResult(request, response);
		} else if ("DELETE".equals(reqMethod)) {
			logger.info("DELETE namespaces");
			return getClearNamespacesResult(request, response);
		}

		throw new ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed: " + reqMethod);
	}

	private ModelAndView getExportNamespacesResult(HttpServletRequest request, HttpServletResponse response)
			throws ClientHTTPException, ServerHTTPException {
		final boolean headersOnly = METHOD_HEAD.equals(request.getMethod());

		Map<String, Object> model = new HashMap<>();
		if (!headersOnly) {
			List<String> columnNames = Arrays.asList("prefix", "namespace");
			List<BindingSet> namespaces = new ArrayList<>();

			try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
				final ValueFactory vf = repositoryCon.getValueFactory();
				try {
					try (CloseableIteration<? extends Namespace, RepositoryException> iter = repositoryCon
							.getNamespaces()) {
						while (iter.hasNext()) {
							Namespace ns = iter.next();

							Literal prefix = vf.createLiteral(ns.getPrefix());
							Literal namespace = vf.createLiteral(ns.getName());

							BindingSet bindingSet = new ListBindingSet(columnNames, prefix, namespace);
							namespaces.add(bindingSet);
						}
					}
				} catch (RepositoryException e) {
					throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
				}
			}
			model.put(QueryResultView.QUERY_RESULT_KEY, new IteratingTupleQueryResult(columnNames, namespaces));
		}

		TupleQueryResultWriterFactory factory = ProtocolUtil.getAcceptableService(request, response,
				TupleQueryResultWriterRegistry.getInstance());

		model.put(QueryResultView.FILENAME_HINT_KEY, "namespaces");
		model.put(QueryResultView.HEADERS_ONLY, headersOnly);
		model.put(QueryResultView.FACTORY_KEY, factory);

		return new ModelAndView(TupleQueryResultView.getInstance(), model);
	}

	private ModelAndView getClearNamespacesResult(HttpServletRequest request, HttpServletResponse response)
			throws ServerHTTPException {
		try (RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request)) {
			try {
				repositoryCon.clearNamespaces();
			} catch (RepositoryException e) {
				throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
			}

			return new ModelAndView(EmptySuccessView.getInstance());
		}
	}
}
