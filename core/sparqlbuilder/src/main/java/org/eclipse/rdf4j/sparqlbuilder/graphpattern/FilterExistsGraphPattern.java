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

import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * A SPARQL Graph Pattern Filter
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#neg-pattern"> Filtering using Graph Pattern</a>
 */
class FilterExistsGraphPattern extends GroupGraphPattern {
	private static final String FILTER = "FILTER";
	private static final String NOT = "NOT";
	private static final String EXISTS = "EXISTS";

	private boolean exists = true;

	FilterExistsGraphPattern() {
		super();
	}

	FilterExistsGraphPattern exists(boolean exists) {
		this.exists = exists;

		return this;
	}

	@Override
	public String getQueryString() {
		StringBuilder filterExists = new StringBuilder();

		filterExists.append(FILTER).append(" ");
		if (!exists) {
			filterExists.append(NOT).append(" ");
		}
		filterExists.append(EXISTS).append(" ");

		String innerPattern = super.getQueryString();
		String trimmedPattern = innerPattern.trim();

		if (!trimmedPattern.startsWith("{")) {
			innerPattern = SparqlBuilderUtils.getBracedString(innerPattern);
		}

		return filterExists.append(innerPattern).toString();
	}
}
