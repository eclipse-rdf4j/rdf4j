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

package org.eclipse.rdf4j.sparqlbuilder.rdf;

import org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlOperator;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphName;

/**
 * Denotes an RDF IRI
 *
 * @see <a href="http://www.w3.org/TR/2014/NOTE-rdf11-primer-20140624/#section-IRI"> RDF IRIs</a>
 */
public interface Iri extends RdfResource, RdfPredicate, GraphName, SparqlOperator {
}
