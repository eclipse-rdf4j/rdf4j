/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * A SPARQL CREATE Query
 *
 * @see <a href="https://www.w3.org/TR/sparql11-update/#create"> SPARQL CREATE Query</a>
 */
public class CreateQuery extends GraphManagementQuery<CreateQuery> {
	private static final String CREATE = "CREATE";
	private static final String GRAPH = "GRAPH";

	private Iri graph;

	CreateQuery() {
	}

	/**
	 * Specify the graph to create
	 *
	 * @param graph the IRI identifier for the new graph
	 *
	 * @return this CreateQuery instance
	 */
	public CreateQuery graph(Iri graph) {
		this.graph = graph;

		return this;
	}

	public CreateQuery graph(IRI graph) {
		return graph(iri(graph));
	}

	@Override
	public String getQueryString() {
		StringBuilder create = new StringBuilder();

		create.append(CREATE).append(" ");

		appendSilent(create);

		create.append(GRAPH).append(" ").append(graph.getQueryString());

		return create.toString();
	}

}
