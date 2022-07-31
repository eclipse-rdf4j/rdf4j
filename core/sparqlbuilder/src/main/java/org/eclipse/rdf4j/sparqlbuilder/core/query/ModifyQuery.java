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
import org.eclipse.rdf4j.sparqlbuilder.core.QueryPattern;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.TriplesTemplate;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphName;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * The SPARQL Modify Queries
 *
 * @see <a href="https://www.w3.org/TR/sparql11-update/#deleteInsert"> SPARQL DELETE/INSERT Query</a>
 */
public class ModifyQuery extends UpdateQuery<ModifyQuery> {
	private static final String INSERT = "INSERT";
	private static final String DELETE = "DELETE";
	private static final String WITH = "WITH";
	private static final String USING = "USING";
	private static final String NAMED = "NAMED";

	private Optional<Iri> with = Optional.empty();
	private Optional<Iri> using = Optional.empty();
	private boolean usingNamed = false;

	private Optional<TriplesTemplate> deleteTriples = Optional.empty();
	private Optional<TriplesTemplate> insertTriples = Optional.empty();
	private Optional<GraphName> deleteGraph = Optional.empty();
	private Optional<GraphName> insertGraph = Optional.empty();

	private final QueryPattern where = SparqlBuilder.where();

	ModifyQuery() {
	}

	/**
	 * Define the graph that will be modified or matched against in the absence of more explicit graph definitions
	 *
	 * @param iri the IRI identifying the desired graph
	 *
	 * @return this modify query instance
	 */
	public ModifyQuery with(Iri iri) {
		with = Optional.ofNullable(iri);

		return this;
	}

	/**
	 * Define the graph that will be modified or matched against in the absence of more explicit graph definitions
	 *
	 * @param iri the IRI identifying the desired graph
	 *
	 * @return this modify query instance
	 */
	public ModifyQuery with(IRI iri) {
		return with(iri(iri));
	}

	/**
	 * Specify triples to delete (or leave empty for DELETE WHERE shortcut)
	 *
	 * @param triples the triples to delete
	 *
	 * @return this modify query instance
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#deleteWhere"> SPARQL DELETE WHERE shortcut</a>
	 */
	public ModifyQuery delete(TriplePattern... triples) {
		deleteTriples = SparqlBuilderUtils.getOrCreateAndModifyOptional(deleteTriples, SparqlBuilder::triplesTemplate,
				tt -> tt.and(triples));

		return this;
	}

	/**
	 * Specify the graph to delete triples from
	 *
	 * @param graphName the identifier of the graph
	 *
	 * @return this modify query instance
	 */
	public ModifyQuery from(GraphName graphName) {
		this.deleteGraph = Optional.ofNullable(graphName);

		return this;
	}

	/**
	 * Specify triples to insert
	 *
	 * @param triples the triples to insert
	 *
	 * @return this modify query instance
	 */
	public ModifyQuery insert(TriplePattern... triples) {
		insertTriples = SparqlBuilderUtils.getOrCreateAndModifyOptional(insertTriples, SparqlBuilder::triplesTemplate,
				tt -> tt.and(triples));

		return this;
	}

	/**
	 * Specify the graph to insert triples into
	 *
	 * @param graphName the identifier of the graph
	 *
	 * @return this modify query instance
	 */
	public ModifyQuery into(GraphName graphName) {
		insertGraph = Optional.ofNullable(graphName);

		return this;
	}

	/**
	 * Specify the graph used when evaluating the WHERE clause
	 *
	 * @param iri the IRI identifying the desired graph
	 *
	 * @return this modify query instance
	 */
	public ModifyQuery using(Iri iri) {
		using = Optional.ofNullable(iri);

		return this;
	}

	/**
	 * Specify the graph used when evaluating the WHERE clause
	 *
	 * @param iri the IRI identifying the desired graph
	 *
	 * @return this modify query instance
	 */
	public ModifyQuery using(IRI iri) {
		return using(iri(iri));
	}

	/**
	 * Specify a named graph to use to when evaluating the WHERE clause
	 *
	 * @param iri the IRI identifying the desired graph
	 *
	 * @return this modify query instance
	 */
	public ModifyQuery usingNamed(Iri iri) {
		usingNamed = true;

		return using(iri);
	}

	/**
	 * Specify a named graph to use to when evaluating the WHERE clause
	 *
	 * @param iri the IRI identifying the desired graph
	 *
	 * @return this modify query instance
	 */
	public ModifyQuery usingNamed(IRI iri) {
		return usingNamed(iri(iri));
	}

	/**
	 * Add graph patterns to this query's query pattern
	 *
	 * @param patterns the patterns to add
	 *
	 * @return this modify query instance
	 */
	public ModifyQuery where(GraphPattern... patterns) {
		where.where(patterns);

		return this;
	}

	@Override
	protected String getQueryActionString() {
		StringBuilder modifyQuery = new StringBuilder();

		with.ifPresent(withIri -> modifyQuery.append(WITH).append(" ").append(withIri.getQueryString()).append("\n"));

		deleteTriples.ifPresent(delTriples -> {
			modifyQuery.append(DELETE).append(" ");

			// DELETE WHERE shortcut
			// https://www.w3.org/TR/sparql11-update/#deleteWhere
			if (!delTriples.isEmpty()) {
				appendNamedTriplesTemplates(modifyQuery, deleteGraph, delTriples);
			}
			modifyQuery.append("\n");
		});

		insertTriples.ifPresent(insTriples -> {
			modifyQuery.append(INSERT).append(" ");
			appendNamedTriplesTemplates(modifyQuery, insertGraph, insTriples);
			modifyQuery.append("\n");
		});

		using.ifPresent(usingIri -> {
			modifyQuery.append(USING).append(" ");

			if (usingNamed) {
				modifyQuery.append(NAMED).append(" ");
			}

			modifyQuery.append(usingIri.getQueryString());
			modifyQuery.append("\n");
		});

		modifyQuery.append(where.getQueryString());

		return modifyQuery.toString();
	}
}
