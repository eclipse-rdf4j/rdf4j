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
 * The SPARQL Insert Data Query
 *
 * @see <a href="https://www.w3.org/TR/sparql11-update/#insertData"> SPARQL INSERT DATA Query</a>
 *
 */
public class InsertDataQuery extends UpdateDataQuery<InsertDataQuery> {
	private static final String INSERT_DATA = "INSERT DATA";

	/**
	 * Add triples to be inserted
	 *
	 * @param triples the triples to add to this insert data query
	 *
	 * @return this Insert Data query instance
	 */
	public InsertDataQuery insertData(TriplePattern... triples) {
		return addTriples(triples);
	}

	/**
	 * Set this query's triples template
	 *
	 * @param triplesTemplate the {@link TriplesTemplate} instance to set
	 *
	 * @return this instance
	 */
	public InsertDataQuery insertData(TriplesTemplate triplesTemplate) {
		return setTriplesTemplate(triplesTemplate);
	}

	/**
	 * Specify a graph to insert the data into
	 *
	 * @param graph the identifier of the graph
	 *
	 * @return this Insert Data query instance
	 */
	public InsertDataQuery into(GraphName graph) {
		return graph(graph);
	}

	@Override
	public String getPrefix() {
		return INSERT_DATA;
	}
}
