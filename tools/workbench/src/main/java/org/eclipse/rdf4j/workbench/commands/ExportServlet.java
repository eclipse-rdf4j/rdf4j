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

import static org.eclipse.rdf4j.rio.RDFWriterRegistry.getInstance;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.workbench.base.TupleServlet;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;

public class ExportServlet extends TupleServlet {

	public ExportServlet() {
		super("export.xsl", "subject", "predicate", "object", "context");
	}

	@Override
	public String[] getCookieNames() {
		return new String[] { ExploreServlet.LIMIT, "Accept" };
	}

	@Override
	protected void service(WorkbenchRequest req, HttpServletResponse resp, String xslPath) throws Exception {
		if (req.isParameterPresent("Accept")) {
			String accept = req.getParameter("Accept");
			RDFFormat format = Rio.getWriterFormatForMIMEType(accept).orElseThrow(Rio.unsupportedFormat(accept));
			resp.setContentType(accept);
			String ext = format.getDefaultFileExtension();
			String attachment = "attachment; filename=export." + ext;
			resp.setHeader("Content-disposition", attachment);
			try (RepositoryConnection con = repository.getConnection()) {
				con.setParserConfig(NON_VERIFYING_PARSER_CONFIG);
				RDFWriterFactory factory = getInstance().get(format).orElseThrow(Rio.unsupportedFormat(format));
				if (format.getCharset() != null) {
					resp.setCharacterEncoding(format.getCharset().name());
				}
				con.export(factory.getWriter(resp.getOutputStream()));
			}
		} else {
			super.service(req, resp, xslPath);
		}
	}

	@Override
	protected void service(WorkbenchRequest req, HttpServletResponse resp, TupleResultBuilder builder,
			RepositoryConnection con) throws Exception {
		int limit = ExploreServlet.LIMIT_DEFAULT;
		if (req.getInt(ExploreServlet.LIMIT) > 0) {
			limit = req.getInt(ExploreServlet.LIMIT);
		}
		try (RepositoryResult<Statement> result = con.getStatements(null, null, null, false)) {
			for (int i = 0; result.hasNext() && (i < limit || limit < 1); i++) {
				Statement st = result.next();
				builder.result(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			}
		}
	}

}
