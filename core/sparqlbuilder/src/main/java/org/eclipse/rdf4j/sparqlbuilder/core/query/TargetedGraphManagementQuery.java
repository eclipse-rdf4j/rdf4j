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

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

@SuppressWarnings("javadoc")
public abstract class TargetedGraphManagementQuery<T extends TargetedGraphManagementQuery<T>>
		extends GraphManagementQuery<TargetedGraphManagementQuery<T>> {
	private static final String GRAPH = "GRAPH";
	private static final String DEFAULT = "DEFAULT";
	private static final String NAMED = "NAMED";
	private static final String ALL = "ALL";

	private String target = DEFAULT;
	private Optional<Iri> graph = Optional.empty();

	/**
	 * Specify which graph to target
	 *
	 * @param graph the IRI identifying the graph to target
	 *
	 * @return this query instance
	 */
	@SuppressWarnings("unchecked")
	public T graph(Iri graph) {
		this.graph = Optional.ofNullable(graph);

		return (T) this;
	}

	/**
	 * Specify which graph to target
	 *
	 * @param graph the IRI identifying the graph to target
	 *
	 * @return this query instance
	 */
	@SuppressWarnings("unchecked")
	public T graph(IRI graph) {
		return graph(iri(graph));
	}

	/**
	 * Target the default graph
	 *
	 * @return this query instance
	 */
	public T def() {
		return target(DEFAULT);
	}

	/**
	 * Target all named graphs
	 *
	 * @return this query instance
	 */
	public T named() {
		return target(NAMED);
	}

	/**
	 * Target all graphs
	 *
	 * @return this query instance
	 */
	public T all() {
		return target(ALL);
	}

	@SuppressWarnings("unchecked")
	private T target(String target) {
		this.target = target;
		graph = Optional.empty();

		return (T) this;
	}

	protected abstract String getQueryActionString();

	@Override
	public String getQueryString() {
		StringBuilder query = new StringBuilder();

		query.append(getQueryActionString()).append(" ");

		appendSilent(query);

		String targetString = graph.map(iri -> GRAPH + " " + iri.getQueryString()).orElse(target);
		query.append(targetString);

		return query.toString();
	}
}
