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

package org.eclipse.rdf4j.sparqlbuilder.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * A SPARQL dataset specification
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#rdfDataset"> RDF Datasets</a>
 */
public class Dataset extends StandardQueryElementCollection<From> {
	/**
	 * Add graph references to this dataset
	 *
	 * @param graphs the datasets to add
	 * @return this object
	 */
	public Dataset from(From... graphs) {
		addElements(graphs);

		return this;
	}

	/**
	 * Add unnamed graph references to this dataset
	 *
	 * @param iris the IRI's of the graphs to add
	 * @return this
	 */
	public Dataset from(Iri... iris) {
		addElements(SparqlBuilder::from, iris);

		return this;
	}

	public Dataset from(IRI... iris) {
		addElements(SparqlBuilder::from, iris);

		return this;
	}
}
