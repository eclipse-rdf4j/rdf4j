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

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.workbench.base.TupleServlet;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;

public class ContextsServlet extends TupleServlet {

	public ContextsServlet() {
		super("contexts.xsl", "context");
	}

	@Override
	protected void service(TupleResultBuilder builder, RepositoryConnection con)
			throws RepositoryException, QueryResultHandlerException {
		for (Resource ctx : Iterations.asList(con.getContextIDs())) {
			builder.result(ctx);
		}
	}

}
