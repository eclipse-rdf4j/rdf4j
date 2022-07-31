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

/**
 * Denotes an orederable SPARQL query element (can be used in a <code>ORDER BY</code> clause)
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy"> SPARQL Order By Clause</a>
 */
public interface Orderable extends QueryElement {
	/**
	 * @return an ascending {@link OrderCondition} instance for this {@link Orderable} object
	 */
	default OrderCondition asc() {
		return SparqlBuilder.asc(this);
	}

	/**
	 * @return an descending {@link OrderCondition} instance for this {@link Orderable} object
	 */
	default OrderCondition desc() {
		return SparqlBuilder.desc(this);
	}
}
