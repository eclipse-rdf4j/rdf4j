/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core.query;

import java.util.Optional;

import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * A SPARQL LOAD Query
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-update/#load"> SPARQL LOAD Query</a>
 */
public class LoadQuery extends GraphManagementQuery<LoadQuery> {
	private static final String LOAD = "LOAD";
	private static final String INTO_GRAPH = "INTO GRAPH";

	private Iri from;
	private Optional<Iri> to = Optional.empty();

	LoadQuery() {
	}

	/**
	 * Specify which graph to load form
	 * 
	 * @param from the IRI identifying the graph to load triples from
	 * 
	 * @return this LoadQuery instance
	 */
	public LoadQuery from(Iri from) {
		this.from = from;

		return this;
	}

	/**
	 * Specify which graph to load into, if not the default graph
	 * 
	 * @param to the IRI identifying the graph to load into
	 * 
	 * @return this LoadQuery instance
	 */
	public LoadQuery to(Iri to) {
		this.to = Optional.ofNullable(to);

		return this;
	}

	@Override
	public String getQueryString() {
		StringBuilder load = new StringBuilder();
		load.append(LOAD).append(" ");

		appendSilent(load);

		load.append(from.getQueryString());

		SparqlBuilderUtils.appendQueryElementIfPresent(to, load, " " + INTO_GRAPH + " ", null);

		return load.toString();
	}

}
