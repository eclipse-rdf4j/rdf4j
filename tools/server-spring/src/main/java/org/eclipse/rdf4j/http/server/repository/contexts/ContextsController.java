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
package org.eclipse.rdf4j.http.server.repository.contexts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.QueryResultView;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.http.server.repository.TupleQueryResultView;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterRegistry;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests for the list of contexts in a repository.
 *
 * @author Herko ter Horst
 */
public class ContextsController extends AbstractController {

	public ContextsController() throws ApplicationContextException {
		setSupportedMethods(METHOD_GET, METHOD_HEAD);
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		Map<String, Object> model = new HashMap<>();
		TupleQueryResultWriterFactory factory = ProtocolUtil.getAcceptableService(request, response,
				TupleQueryResultWriterRegistry.getInstance());

		if (METHOD_GET.equals(request.getMethod())) {
			List<String> columnNames = List.of("contextID");
			List<BindingSet> contexts = new ArrayList<>();
			RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request);
			try {
				try (CloseableIteration<? extends Resource, RepositoryException> contextIter = repositoryCon
						.getContextIDs()) {
					while (contextIter.hasNext()) {
						BindingSet bindingSet = new ListBindingSet(columnNames, contextIter.next());
						contexts.add(bindingSet);
					}
				}
				model.put(QueryResultView.QUERY_RESULT_KEY, new IteratingTupleQueryResult(columnNames, contexts));
				model.put(QueryResultView.FILENAME_HINT_KEY, "contexts");
				model.put(QueryResultView.FACTORY_KEY, factory);
				model.put(QueryResultView.HEADERS_ONLY, METHOD_HEAD.equals(request.getMethod()));
				model.put(QueryResultView.CONNECTION_KEY, repositoryCon);

			} catch (RepositoryException e) {
				// normally the QueryResultView closes the connection, but not if an exception occurred
				repositoryCon.close();
				throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
			}
		}
		return new ModelAndView(TupleQueryResultView.getInstance(), model);
	}
}
