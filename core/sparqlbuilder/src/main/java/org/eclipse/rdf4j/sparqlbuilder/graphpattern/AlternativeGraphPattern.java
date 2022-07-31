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

package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import org.eclipse.rdf4j.sparqlbuilder.core.QueryElementCollection;

/**
 * A SPARQL Alternative Graph Pattern.
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#alternatives"> SPARQL Alternative Graph
 *      Patterns</a>
 */
class AlternativeGraphPattern extends QueryElementCollection<GroupGraphPattern> implements GraphPattern {
	private static final String UNION = "UNION";
	private static final String DELIMETER = " " + UNION + " ";

	AlternativeGraphPattern() {
		super(DELIMETER);
	}

	AlternativeGraphPattern(GraphPattern original) {
		super(DELIMETER);

		if (original instanceof AlternativeGraphPattern) {
			copy((AlternativeGraphPattern) original);
		} else if (original != null && !original.isEmpty()) {
			elements.add(new GroupGraphPattern(original));
		}
	}

	private void copy(AlternativeGraphPattern original) {
		this.elements = original.elements;
	}

	@Override
	public AlternativeGraphPattern union(GraphPattern... patterns) {
		addElements(GraphPatterns::extractOrConvertToGGP, patterns);

		return this;
	}
}
