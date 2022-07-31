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
package org.eclipse.rdf4j.query.impl;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;

/**
 * @author Jeen Broekstra
 */
public abstract class AbstractParserUpdate extends AbstractUpdate {

	private final ParsedUpdate parsedUpdate;

	protected AbstractParserUpdate(ParsedUpdate parsedUpdate) {
		this.parsedUpdate = parsedUpdate;
	}

	public ParsedUpdate getParsedUpdate() {
		return parsedUpdate;
	}

	@Override
	public String toString() {
		return parsedUpdate.toString();
	}

	/**
	 * Determines the active dataset by appropriately merging the pre-set dataset and the dataset defined in the SPARQL
	 * operation itself. If the SPARQL operation contains WITH, USING, or USING NAMED clauses, these should override
	 * whatever is preset.
	 *
	 * @param sparqlDefinedDataset the dataset as defined in the SPARQL update itself.
	 * @return a {@link Dataset} comprised of a merge between the pre-set dataset and the SPARQL-defined dataset.
	 */
	protected Dataset getMergedDataset(Dataset sparqlDefinedDataset) {
		if (sparqlDefinedDataset == null) {
			return dataset;
		} else if (dataset == null) {
			return sparqlDefinedDataset;
		}

		final DatasetImpl mergedDataset = new DatasetImpl();

		final boolean hasWithClause = sparqlDefinedDataset.getDefaultInsertGraph() != null;
		final Set<IRI> sparqlDefaultGraphs = sparqlDefinedDataset.getDefaultGraphs();

		if (hasWithClause) {
			// a WITH-clause in the update, we need to define the default
			// graphs, the default insert graph and default remove graphs
			// by means of the update itself.
			for (IRI graphURI : sparqlDefaultGraphs) {
				mergedDataset.addDefaultGraph(graphURI);
			}

			mergedDataset.setDefaultInsertGraph(sparqlDefinedDataset.getDefaultInsertGraph());

			for (IRI drg : sparqlDefinedDataset.getDefaultRemoveGraphs()) {
				mergedDataset.addDefaultRemoveGraph(drg);
			}

			// if the preset dataset specifies any named graphs, we include
			// these, to ensure that any GRAPH clauses
			// in the update can override the WITH clause.
			for (IRI graphURI : dataset.getNamedGraphs()) {
				mergedDataset.addNamedGraph(graphURI);
			}

			// we're done here.
			return mergedDataset;
		}

		mergedDataset.setDefaultInsertGraph(dataset.getDefaultInsertGraph());
		for (IRI graphURI : dataset.getDefaultRemoveGraphs()) {
			mergedDataset.addDefaultRemoveGraph(graphURI);
		}

		// if there are default graphs in the SPARQL update but it's not a WITH
		// clause, it's a USING clause
		final boolean hasUsingClause = !hasWithClause && sparqlDefaultGraphs != null ? sparqlDefaultGraphs.size() > 0
				: false;

		final Set<IRI> sparqlNamedGraphs = sparqlDefinedDataset.getNamedGraphs();
		final boolean hasUsingNamedClause = sparqlNamedGraphs != null ? sparqlNamedGraphs.size() > 0 : false;

		if (hasUsingClause) {
			// one or more USING-clauses in the update itself, we need to
			// define the default graphs by means of the update itself
			for (IRI graphURI : sparqlDefaultGraphs) {
				mergedDataset.addDefaultGraph(graphURI);
			}
		} else {
			for (IRI graphURI : dataset.getDefaultGraphs()) {
				mergedDataset.addDefaultGraph(graphURI);
			}
		}

		if (hasUsingNamedClause) {
			// one or more USING NAMED-clauses in the update, we need to
			// define the named graphs by means of the update itself.
			for (IRI graphURI : sparqlNamedGraphs) {
				mergedDataset.addNamedGraph(graphURI);
			}
		} else if (!hasUsingClause) {
			// we only merge in the pre-specified named graphs if the SPARQL
			// update itself did not have any USING clauses. This is to ensure
			// that a GRAPH-clause in the update can not override a USING
			// clause.
			for (IRI graphURI : dataset.getNamedGraphs()) {
				mergedDataset.addNamedGraph(graphURI);
			}
		}

		return mergedDataset;
	}
}
