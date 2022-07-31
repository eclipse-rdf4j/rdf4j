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
package org.eclipse.rdf4j.workbench.commands;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.QueryStorage;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;

/**
 * Servlet that provides a page to access saved queries.
 *
 * @author Dale Visser
 */
public class SavedQueriesServlet extends TransformationServlet {

	private QueryStorage storage;

	@Override
	public String[] getCookieNames() {
		return new String[] { QueryServlet.LIMIT, "queryLn", "infer", "total_result_count" };
	}

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		try {
			this.storage = QueryStorage.getSingletonInstance(this.appConfig);
		} catch (RepositoryException | IOException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void service(final WorkbenchRequest req, final HttpServletResponse resp, final String xslPath)
			throws IOException, RDF4JException, BadRequestException {
		final TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
		builder.transform(xslPath, "saved-queries.xsl");
		builder.start();
		builder.link(List.of(INFO));
		this.getSavedQueries(req, builder);
		builder.end();
	}

	@Override
	protected void doPost(final WorkbenchRequest wreq, final HttpServletResponse resp, final String xslPath)
			throws BadRequestException, IOException, RDF4JException {
		final String urn = wreq.getParameter("delete");
		if (null == urn || urn.isEmpty()) {
			throw new BadRequestException("Expected POST to contain a 'delete=' parameter.");
		}
		final boolean accessible = storage.checkAccess((HTTPRepository) this.repository);
		if (accessible) {
			String userName = wreq.getParameter(SERVER_USER);
			if (null == userName) {
				userName = "";
			}
			final IRI queryURI = SimpleValueFactory.getInstance().createIRI(urn);
			if (storage.canChange(queryURI, userName)) {
				storage.deleteQuery(queryURI, userName);
			} else {
				throw new BadRequestException("User '" + userName + "' may not delete query id " + urn);
			}
		}
		this.service(wreq, resp, xslPath);
	}

	private void getSavedQueries(final WorkbenchRequest req, final TupleResultBuilder builder)
			throws RDF4JException, BadRequestException {
		final HTTPRepository repo = (HTTPRepository) this.repository;
		String user = req.getParameter(SERVER_USER);
		if (null == user) {
			user = "";
		}
		if (!storage.checkAccess(repo)) {
			throw new BadRequestException(
					"User '" + user + "' not authorized to access repository '" + repo.getRepositoryURL() + "'");
		}
		storage.selectSavedQueries(repo, user, builder);
	}
}
