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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElementCollection;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * A SPARQL Group Graph Pattern
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GroupPatterns"> SPARQL Group Graph Patterns</a>
 */
class GroupGraphPattern extends QueryElementCollection<GraphPattern> implements GraphPattern {
	private static final String OPTIONAL = "OPTIONAL";
	private static final String GRAPH = "GRAPH ";

	private Optional<GraphName> from = Optional.empty();
	private List<Filter> filters = new ArrayList<>();
	protected boolean isOptional = false;

	GroupGraphPattern() {
		this(false);
	}

	GroupGraphPattern(boolean isOptional) {
		this.isOptional = isOptional;
	}

	GroupGraphPattern(GraphPattern original) {
		if (original instanceof GroupGraphPattern) {
			copy((GroupGraphPattern) original);
		} else if (original != null && !original.isEmpty()) {
			elements.add(original);
		}
	}

	protected void copy(GroupGraphPattern original) {
		this.elements = original.elements;
		this.isOptional = original.isOptional;
		this.from = original.from;
		this.filters = new ArrayList<>(original.filters);
	}

	@Override
	public GroupGraphPattern and(GraphPattern... patterns) {
		if (isEmpty() && patterns.length == 1 && (isGGP(patterns[0]))) {
			copy(GraphPatterns.extractOrConvertToGGP(patterns[0]));
		} else {
			addElements(patterns);
		}

		return this;
	}

	@Override
	public GroupGraphPattern optional(boolean isOptional) {
		this.isOptional = isOptional;

		return this;
	}

	@Override
	public GroupGraphPattern from(GraphName name) {
		from = Optional.of(name);

		return this;
	}

	@Override
	public GroupGraphPattern filter(Expression<?> constraint) {
		filters.add(new Filter(constraint));

		return this;
	}

	@Override
	public boolean isEmpty() {
		return super.isEmpty();
	}

	@Override
	public String getQueryString() {
		StringBuilder pattern = new StringBuilder();
		StringBuilder innerPattern = new StringBuilder();

		if (isOptional) {
			pattern.append(OPTIONAL).append(" ");
		}

		SparqlBuilderUtils.appendQueryElementIfPresent(from, pattern, GRAPH, " ");

		innerPattern.append(super.getQueryString());

		filters.forEach(filter -> SparqlBuilderUtils.appendQueryElementIfPresent(
				Optional.of(filter),
				innerPattern, "\n", null));

		if (bracketInner()) {
			pattern.append(SparqlBuilderUtils.getBracedString(innerPattern.toString()));
		} else {
			pattern.append(innerPattern.toString());
		}

		return pattern.toString();
	}

	private static boolean isGGP(GraphPattern pattern) {
		if (pattern instanceof GroupGraphPattern) {
			return true;
		}
		if (pattern instanceof GraphPatternNotTriples) {
			return ((GraphPatternNotTriples) pattern).gp instanceof GroupGraphPattern;
		}

		return false;
	}

	// Prevent extra brackets being added in the case of this graph pattern
	// containing only one group graph pattern. Resulting syntax is
	// logically equivalent and easier to read (and hopefully parse by query
	// parsers) or make sure to add them if "modifiers" exist
	private boolean bracketInner() {
		return !(elements.size() == 1 && elements.iterator().next() instanceof GroupGraphPattern);
	}
}
