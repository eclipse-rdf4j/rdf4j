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

import java.util.Optional;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.TriplesTemplate;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphName;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;

@SuppressWarnings("unchecked")
abstract class UpdateDataQuery<T extends UpdateDataQuery<T>> extends UpdateQuery<T> {
	protected TriplesTemplate triplesTemplate = SparqlBuilder.triplesTemplate();
	protected Optional<GraphName> graphName = Optional.empty();

	protected T addTriples(TriplePattern... triples) {
		triplesTemplate.and(triples);

		return (T) this;
	}

	protected T setTriplesTemplate(TriplesTemplate triplesTemplate) {
		this.triplesTemplate = triplesTemplate;

		return (T) this;
	}

	public T graph(GraphName graph) {
		graphName = Optional.ofNullable(graph);

		return (T) this;
	}

	protected abstract String getPrefix();

	@Override
	protected String getQueryActionString() {
		StringBuilder updateDataQuery = new StringBuilder();

		updateDataQuery.append(getPrefix()).append(" ");
		appendNamedTriplesTemplates(updateDataQuery, graphName, triplesTemplate);

		return updateDataQuery.toString();
	}
}
