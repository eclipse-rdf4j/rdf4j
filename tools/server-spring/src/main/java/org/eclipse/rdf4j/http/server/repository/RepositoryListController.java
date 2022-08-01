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
package org.eclipse.rdf4j.http.server.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterRegistry;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Handles requests for the list of repositories available on this server.
 *
 * @author Herko ter Horst
 */
public class RepositoryListController extends AbstractController {

	private RepositoryManager repositoryManager;

	public RepositoryListController() throws ApplicationContextException {
		setSupportedMethods(new String[] { METHOD_GET, METHOD_HEAD });
	}

	public void setRepositoryManager(RepositoryManager repMan) {
		repositoryManager = repMan;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		Map<String, Object> model = new HashMap<>();

		if (METHOD_GET.equals(request.getMethod())) {
			ValueFactory vf = SimpleValueFactory.getInstance();

			try {
				List<String> bindingNames = new ArrayList<>();
				List<BindingSet> bindingSets = new ArrayList<>();

				// Determine the repository's URI
				StringBuffer requestURL = request.getRequestURL();
				if (requestURL.charAt(requestURL.length() - 1) != '/') {
					requestURL.append('/');
				}
				String namespace = requestURL.toString();

				repositoryManager.getAllRepositoryInfos(false).forEach(info -> {
					QueryBindingSet bindings = new QueryBindingSet();
					bindings.addBinding("uri", vf.createIRI(namespace, info.getId()));
					bindings.addBinding("id", vf.createLiteral(info.getId()));
					if (info.getDescription() != null) {
						bindings.addBinding("title", vf.createLiteral(info.getDescription()));
					}
					bindings.addBinding("readable", vf.createLiteral(info.isReadable()));
					bindings.addBinding("writable", vf.createLiteral(info.isWritable()));
					bindingSets.add(bindings);
				});

				bindingNames.add("uri");
				bindingNames.add("id");
				bindingNames.add("title");
				bindingNames.add("readable");
				bindingNames.add("writable");
				model.put(QueryResultView.QUERY_RESULT_KEY, new IteratingTupleQueryResult(bindingNames, bindingSets));
			} catch (RepositoryException e) {
				throw new ServerHTTPException(e.getMessage(), e);
			}
		}

		TupleQueryResultWriterFactory factory = ProtocolUtil.getAcceptableService(request, response,
				TupleQueryResultWriterRegistry.getInstance());

		model.put(QueryResultView.FILENAME_HINT_KEY, "repositories");
		model.put(QueryResultView.FACTORY_KEY, factory);
		model.put(QueryResultView.HEADERS_ONLY, METHOD_HEAD.equals(request.getMethod()));

		return new ModelAndView(TupleQueryResultView.getInstance(), model);
	}
}
