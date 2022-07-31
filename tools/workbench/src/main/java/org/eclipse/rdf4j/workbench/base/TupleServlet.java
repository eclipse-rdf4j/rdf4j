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
package org.eclipse.rdf4j.workbench.base;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;

public abstract class TupleServlet extends TransformationServlet {

	protected String xsl;

	protected String[] variables;

	public TupleServlet(String xsl, String... variables) {
		super();
		this.xsl = xsl;
		this.variables = variables;
	}

	@Override
	protected void service(WorkbenchRequest req, HttpServletResponse resp, String xslPath) throws Exception {
		TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
		try (RepositoryConnection con = repository.getConnection()) {
			con.setParserConfig(NON_VERIFYING_PARSER_CONFIG);
			for (Namespace ns : Iterations.asList(con.getNamespaces())) {
				builder.prefix(ns.getPrefix(), ns.getName());
			}
			if (xsl != null) {
				builder.transform(xslPath, xsl);
			}
			builder.start(variables);
			builder.link(List.of("info"));
			this.service(req, resp, builder, con);
			builder.end();
		}
	}

	protected void service(WorkbenchRequest req, HttpServletResponse resp, TupleResultBuilder builder,
			RepositoryConnection con) throws Exception {
		service(builder, con);
	}

	protected void service(TupleResultBuilder builder, RepositoryConnection con) throws Exception {
	}
}
