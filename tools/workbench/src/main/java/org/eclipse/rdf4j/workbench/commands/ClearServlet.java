/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.commands;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClearServlet extends TransformationServlet {

	private final Logger logger = LoggerFactory.getLogger(ClearServlet.class);

	@Override
	protected void doPost(WorkbenchRequest req, HttpServletResponse resp, String xslPath)
			throws IOException, RepositoryException, QueryResultHandlerException {
		try {
			try (RepositoryConnection con = repository.getConnection()) {
				if (req.isParameterPresent(CONTEXT)) {
					con.clear(req.getResource(CONTEXT));
				} else {
					con.clear();
				}
			} catch (ClassCastException exc) {
				throw new BadRequestException(exc.getMessage(), exc);
			}
			resp.sendRedirect("summary");
		} catch (BadRequestException exc) {
			logger.warn(exc.toString(), exc);
			TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
			builder.transform(xslPath, "clear.xsl");
			builder.start("error-message", CONTEXT);
			builder.link(List.of(INFO));
			builder.result(exc.getMessage(), req.getParameter(CONTEXT));
			builder.end();
		}
	}

	@Override
	public void service(TupleResultBuilder builder, String xslPath)
			throws RepositoryException, QueryResultHandlerException {
		// TupleResultBuilder builder = new TupleResultBuilder(out);
		builder.transform(xslPath, "clear.xsl");
		builder.start();
		builder.link(List.of(INFO));
		builder.end();
	}

}
