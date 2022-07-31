/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;

public class GraphPatternNotTriples implements GraphPattern {
	GraphPattern gp;

	GraphPatternNotTriples() {
		this(new GroupGraphPattern());
	}

	GraphPatternNotTriples(GraphPattern from) {
		this.gp = from;
	}

	/**
	 * Like {@link GraphPattern#and(GraphPattern...)}, but mutates and returns this instance
	 *
	 * @param patterns the patterns to add
	 * @return this
	 */
	@Override
	public GraphPatternNotTriples and(GraphPattern... patterns) {
		gp = gp.and(patterns);

		return this;
	}

	/**
	 * Like {@link GraphPattern#union(GraphPattern...)}, but mutates and returns this instance
	 *
	 * @param patterns the patterns to add
	 * @return this
	 */
	@Override
	public GraphPatternNotTriples union(GraphPattern... patterns) {
		gp = gp.union(patterns);

		return this;
	}

	/**
	 * Like {@link GraphPattern#optional()}, but mutates and returns this instance
	 *
	 * @return this
	 */
	@Override
	public GraphPatternNotTriples optional() {
		gp = gp.optional();

		return this;
	}

	/**
	 * Like {@link GraphPattern#optional(boolean)}, but mutates and returns this instance
	 *
	 * @param isOptional if this graph pattern should be optional or not
	 * @return this
	 */
	@Override
	public GraphPatternNotTriples optional(boolean isOptional) {
		gp = gp.optional(isOptional);

		return this;
	}

	/**
	 * Like {@link GraphPattern#filter(Expression)}, but mutates and returns this instance
	 *
	 * @param constraint the filter constraint
	 * @return this
	 */
	@Override
	public GraphPatternNotTriples filter(Expression<?> constraint) {
		gp = gp.filter(constraint);

		return this;
	}

	/**
	 * Like {@link GraphPattern#minus(GraphPattern...)}, but mutates and returns this instance
	 *
	 * @param patterns the patterns to construct the <code>MINUS</code> graph pattern with
	 * @return this
	 */
	@Override
	public GraphPatternNotTriples minus(GraphPattern... patterns) {
		gp = gp.minus(patterns);

		return this;
	}

	/**
	 * Like {@link GraphPattern#from(GraphName)}, but mutates and returns this instance
	 *
	 * @param name the name to specify
	 * @return this
	 */
	@Override
	public GraphPatternNotTriples from(GraphName name) {
		gp = gp.from(name);

		return this;
	}

	@Override
	public boolean isEmpty() {
		return gp.isEmpty();
	}

	@Override
	public String getQueryString() {
		return gp.getQueryString();
	}
}
