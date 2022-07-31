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

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveServlet extends TransformationServlet {

	private final Logger logger = LoggerFactory.getLogger(RemoveServlet.class);

	@Override
	protected void doPost(WorkbenchRequest req, HttpServletResponse resp, String xslPath)
			throws IOException, RepositoryException, QueryResultHandlerException {
		String objectParameter = req.getParameter("obj");
		try {
			try (RepositoryConnection con = repository.getConnection()) {
				Resource subj = req.getResource("subj");
				IRI pred = req.getURI("pred");
				Value obj = req.getValue("obj");
				if (subj == null && pred == null && obj == null && !req.isParameterPresent(CONTEXT)) {
					throw new BadRequestException("No values");
				}
				remove(con, subj, pred, obj, req);
				// HACK: HTML sends \r\n, but SAX strips out the \r, try both ways
				if (obj instanceof Literal && obj.stringValue().contains("\r\n")) {
					obj = Protocol.decodeValue(objectParameter.replace("\r\n", "\n"), con.getValueFactory());
					remove(con, subj, pred, obj, req);
				}
			} catch (ClassCastException exc) {
				throw new BadRequestException(exc.getMessage(), exc);
			}
			resp.sendRedirect("summary");
		} catch (BadRequestException exc) {
			logger.warn(exc.toString(), exc);
			TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
			builder.transform(xslPath, "remove.xsl");
			builder.start("error-message", "subj", "pred", "obj", CONTEXT);
			builder.link(List.of(INFO));
			builder.result(exc.getMessage(), req.getParameter("subj"), req.getParameter("pred"), objectParameter,
					req.getParameter(CONTEXT));
			builder.end();
		}
	}

	private void remove(RepositoryConnection con, Resource subj, IRI pred, Value obj, WorkbenchRequest req)
			throws BadRequestException, RepositoryException {
		if (req.isParameterPresent(CONTEXT)) {
			Resource ctx = req.getResource(CONTEXT);
			if (subj == null && pred == null && obj == null) {
				con.clear(ctx);
			} else {
				con.remove(subj, pred, obj, ctx);
			}
		} else {
			con.remove(subj, pred, obj);
		}
	}

	@Override
	public void service(TupleResultBuilder builder, String xslPath)
			throws RepositoryException, QueryResultHandlerException {
		builder.transform(xslPath, "remove.xsl");
		builder.start();
		builder.link(List.of(INFO));
		builder.end();
	}

}
