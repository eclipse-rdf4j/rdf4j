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
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateServlet extends TransformationServlet {

	private final Logger logger = LoggerFactory.getLogger(UpdateServlet.class);

	@Override
	public String[] getCookieNames() {
		return new String[] { "Content-Type" };
	}

	@Override
	protected void doPost(WorkbenchRequest req, HttpServletResponse resp, String xslPath)
			throws Exception, IOException {
		// All POST requests are expected to contain a SPARQL/Update 'update' parameter.
		try {
			String updateString = req.getParameter("update");
			executeUpdate(updateString);
			resp.sendRedirect("summary");
		} catch (BadRequestException exc) {
			logger.warn(exc.toString(), exc);
			TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
			builder.transform(xslPath, "update.xsl");
			builder.start("error-message", "update");
			builder.link(Arrays.asList(INFO, "namespaces"));

			String updateString = req.getParameter("update");
			builder.result(exc.getMessage(), updateString);
			builder.end();
		}
	}

	private void executeUpdate(String updateString) throws Exception {
		try (RepositoryConnection con = repository.getConnection()) {
			try {
				con
						.prepareUpdate(QueryLanguage.SPARQL, updateString)
						.execute();
			} catch (RepositoryException | MalformedQueryException | UpdateExecutionException e) {
				throw new BadRequestException(e.getMessage());
			}
		}
	}

	@Override
	public void service(TupleResultBuilder builder, String xslPath)
			throws RepositoryException, QueryResultHandlerException {
		// All GET requests are assumed to be to present the update editor page.
		builder.transform(xslPath, "update.xsl");
		builder.start();
		builder.link(Arrays.asList(INFO, "namespaces"));
		builder.end();
	}

}
