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
package org.eclipse.rdf4j.query;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

/**
 * Represents a dataset against which operations can be evaluated. A dataset consists of a default graph for read and
 * using operations, which is the <a href="http://www.w3.org/TR/rdf-mt/#defmerge">RDF merge</a> of one or more graphs, a
 * set of named graphs, and a single update graph for INSERT and DELETE. See
 * <a href="http://www.w3.org/TR/rdf-sparql-query/#rdfDataset">SPARQL Query Language for RDF</a> for more info.
 *
 * @author Simon Schenk
 * @author Arjohn Kampman
 * @author James Leigh
 */
public interface Dataset {

	/**
	 * Gets the default remove graph URIs of this dataset. An empty set indicates the the store's default behaviour
	 * should be used, if not otherwise indicated in the operation.
	 */
	Set<IRI> getDefaultRemoveGraphs();

	/**
	 * Gets the default insert graph URI of this dataset. An null value indicates that the store's default behaviour
	 * should be used, if not otherwise indicated in the operation.
	 */
	IRI getDefaultInsertGraph();

	/**
	 * Gets the default graph URIs of this dataset. An empty default graph set and a non-empty named graph set indicates
	 * that the default graph is an empty graph. However, if both the default graph set and the named graph set are
	 * empty, that indicates that the store's default behaviour should be used.
	 */
	Set<IRI> getDefaultGraphs();

	/**
	 * Gets the named graph URIs of this dataset. An empty named graph set and a non-empty default graph set indicates
	 * that there are no named graphs. However, if both the default graph set and the named graph set are empty, that
	 * indicates that the store's default behaviour should be used.
	 */
	Set<IRI> getNamedGraphs();
}
