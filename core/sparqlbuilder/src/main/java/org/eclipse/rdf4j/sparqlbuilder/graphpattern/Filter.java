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

import java.util.Optional;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * A SPARQL Filter Clause
 *
 * @see <a href="http://www.w3.org/TR/sparql11-query/#termConstraint"> SPARQL Filter</a>
 */
class Filter implements QueryElement {
	private static final String FILTER = "FILTER";
	private Optional<Expression<?>> constraint = Optional.empty();

	Filter() {
		this(null);
	}

	Filter(Expression<?> expression) {
		filter(expression);
	}

	/**
	 * Set the constraint for this Filter clause
	 *
	 * @param expression the constraint to set
	 * @return this
	 */
	public Filter filter(Expression<?> expression) {
		constraint = Optional.ofNullable(expression);

		return this;
	}

	@Override
	public String getQueryString() {
		StringBuilder filter = new StringBuilder();

		filter.append(FILTER).append(" ");
		String exp = constraint.map(QueryElement::getQueryString).orElse("");
		filter.append(SparqlBuilderUtils.getParenthesizedString(exp));

		return filter.toString();
	}
}
