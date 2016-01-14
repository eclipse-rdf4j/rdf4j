/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterRegistry;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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

	private static final String REPOSITORY_LIST_QUERY;

	static {
		StringBuilder query = new StringBuilder(256);
		query.append("SELECT id, title, \"true\"^^xsd:boolean as \"readable\", \"true\"^^xsd:boolean as \"writable\"");
		query.append("FROM {} rdf:type {sys:Repository};");
		query.append("        [rdfs:label {title}];");
		query.append("        sys:repositoryID {id} ");
		query.append("USING NAMESPACE sys = <http://www.openrdf.org/config/repository#>");
		REPOSITORY_LIST_QUERY = query.toString();
	}

	private RepositoryManager repositoryManager;

	public RepositoryListController()
		throws ApplicationContextException
	{
		setSupportedMethods(new String[] { METHOD_GET, METHOD_HEAD });
	}

	public void setRepositoryManager(RepositoryManager repMan) {
		repositoryManager = repMan;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
		throws Exception
	{
		Map<String, Object> model = new HashMap<String, Object>();

		if (METHOD_GET.equals(request.getMethod())) {
			Repository systemRepository = repositoryManager.getSystemRepository();
			ValueFactory vf = systemRepository.getValueFactory();

			try {
				RepositoryConnection con = systemRepository.getConnection();
				try {
					// FIXME: The query result is cached here as we need to close the
					// connection before returning. Would be much better to stream
					// the
					// query result directly to the client.

					List<String> bindingNames = new ArrayList<String>();
					List<BindingSet> bindingSets = new ArrayList<BindingSet>();

					TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SERQL,
							REPOSITORY_LIST_QUERY).evaluate();
					try {
						// Determine the repository's URI
						StringBuffer requestURL = request.getRequestURL();
						if (requestURL.charAt(requestURL.length() - 1) != '/') {
							requestURL.append('/');
						}
						String namespace = requestURL.toString();

						while (queryResult.hasNext()) {
							QueryBindingSet bindings = new QueryBindingSet(queryResult.next());

							String id = bindings.getValue("id").stringValue();
							bindings.addBinding("uri", vf.createIRI(namespace, id));

							bindingSets.add(bindings);
						}

						bindingNames.add("uri");
						bindingNames.addAll(queryResult.getBindingNames());
					}
					finally {
						queryResult.close();
					}
					model.put(QueryResultView.QUERY_RESULT_KEY,
							new IteratingTupleQueryResult(bindingNames, bindingSets));

				}
				finally {
					con.close();
				}
			}
			catch (RepositoryException e) {
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
