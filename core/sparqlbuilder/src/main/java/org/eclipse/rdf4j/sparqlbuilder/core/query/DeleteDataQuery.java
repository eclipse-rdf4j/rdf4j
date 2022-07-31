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

import org.eclipse.rdf4j.sparqlbuilder.core.TriplesTemplate;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphName;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;

/**
 * The SPARQL Delete Data Query
 *
 * @see <a href="https://www.w3.org/TR/sparql11-update/#deleteData"> SPARQL DELETE DATA Query</a>
 *
 */
public class DeleteDataQuery extends UpdateDataQuery<DeleteDataQuery> {
	private static final String DELETE_DATA = "DELETE DATA";

	/**
	 * Add triples to be deleted
	 *
	 * @param triples the triples to add to this delete data query
	 *
	 * @return this Delete Data query instance
	 */
	public DeleteDataQuery deleteData(TriplePattern... triples) {
		return addTriples(triples);
	}

	/**
	 * Set this query's triples template
	 *
	 * @param triplesTemplate the {@link TriplesTemplate} instance to set
	 *
	 * @return this instance
	 */
	public DeleteDataQuery deleteData(TriplesTemplate triplesTemplate) {
		return setTriplesTemplate(triplesTemplate);
	}

	/**
	 * Specify a graph to delete the data from
	 *
	 * @param graph the identifier of the graph
	 *
	 * @return this Delete Data query instance
	 */
	public DeleteDataQuery from(GraphName graph) {
		return graph(graph);
	}

	@Override
	protected String getPrefix() {
		return DELETE_DATA;
	}
}
