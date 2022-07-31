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

import java.util.Collections;
import java.util.function.Function;

import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * Represents a collection of triple patterns
 */
public class TriplesTemplate extends StandardQueryElementCollection<TriplePattern> {
	private static final Function<String, String> WRAPPER = SparqlBuilderUtils::getBracedString;

	TriplesTemplate(TriplePattern... triples) {
		super("\n");
		setWrapperMethod(WRAPPER);
		and(triples);
	}

	/**
	 * add triples to this template
	 *
	 * @param triples the triples to add
	 *
	 * @return this TriplesTemplate instance
	 */
	public TriplesTemplate and(TriplePattern... triples) {
		Collections.addAll(elements, triples);

		return this;
	}
}
