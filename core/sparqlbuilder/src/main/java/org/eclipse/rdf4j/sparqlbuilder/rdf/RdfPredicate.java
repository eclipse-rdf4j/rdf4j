/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.rdf;

import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;

/**
 * Denotes an element that can represent a predicate in a {@link TriplePattern}
 */
public interface RdfPredicate extends QueryElement {
	/**
	 * The built-in predicate shortcut for <code>rdf:type</code>
	 *
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#abbrevRdfType"> RDF Type abbreviation</a>
	 */
	public static RdfPredicate a = () -> "a";
}
