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
 * Denotes a groupable SPARQL query element (can be used in a <code>GROUP BY</code> clause)
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#groupby"> SPARQL Group By Clause</a>
 */
public interface Groupable extends QueryElement {
}
