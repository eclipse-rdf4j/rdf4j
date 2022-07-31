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

import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.workbench.base.TupleServlet;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;

public class TypesServlet extends TupleServlet {

	public TypesServlet() {
		super("types.xsl", "type");
	}

	private static final String DISTINCT_TYPE = "SELECT DISTINCT ?type WHERE { ?subj a ?type }";

	@Override
	protected void service(TupleResultBuilder builder, RepositoryConnection con) throws Exception {
		TupleQuery query = con.prepareTupleQuery(SPARQL, DISTINCT_TYPE);
		try (TupleQueryResult result = query.evaluate()) {
			while (result.hasNext()) {
				builder.result(result.next().getValue("type"));
			}
		}
	}

}
