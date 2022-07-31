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

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;

public class NamespacesServlet extends TransformationServlet {

	@Override
	protected void doPost(WorkbenchRequest req, HttpServletResponse resp, String xslPath) throws Exception {
		try (RepositoryConnection con = repository.getConnection()) {
			String prefix = req.getParameter("prefix");
			String namespace = req.getParameter("namespace");
			if (namespace.length() > 0) {
				con.setNamespace(prefix, namespace);
			} else {
				con.removeNamespace(prefix);
			}
		}
		super.service(req, resp, xslPath);
	}

	@Override
	public void service(TupleResultBuilder builder, String xslPath)
			throws RepositoryException, QueryResultHandlerException {
		// TupleResultBuilder builder = new TupleResultBuilder(out);
		builder.transform(xslPath, "namespaces.xsl");
		try (RepositoryConnection con = repository.getConnection()) {
			con.setParserConfig(NON_VERIFYING_PARSER_CONFIG);
			builder.start("prefix", "namespace");
			builder.link(List.of(INFO));
			for (Namespace ns : Iterations.asList(con.getNamespaces())) {
				builder.result(ns.getPrefix(), ns.getName());
			}
			builder.end();
		}
	}

}
