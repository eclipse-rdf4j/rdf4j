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

package org.eclipse.rdf4j.sparqlbuilder.core.query;

import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projection;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;

/**
 * A SPARQL Select query
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select"> SPARQL Select Query</a>
 */
public class SelectQuery extends OuterQuery<SelectQuery> {
	private Projection select = SparqlBuilder.select();

	// package-protect instantiation of this class
	SelectQuery() {
	}

	/**
	 * Specify the query's projection to be distinct
	 *
	 * @return this
	 *
	 * @see Projection#distinct()
	 */
	public SelectQuery distinct() {
		return distinct(true);
	}

	/**
	 * Specify if the query's projection should be distinct or not
	 *
	 * @param isDistinct if this query's projection should be distinct
	 * @return this
	 *
	 * @see Projection#distinct(boolean)
	 */
	public SelectQuery distinct(boolean isDistinct) {
		select.distinct(isDistinct);

		return this;
	}

	/**
	 * Specify that this query's projection should select all in-scope expressions
	 * <p>
	 * NOTE: setting this takes precedence over any expressions added to the projection via
	 * {@link #select(Projectable...)} or {@link #select(Projection)} when printing
	 *
	 * @return this
	 *
	 * @see Projection#all()
	 */
	public SelectQuery all() {
		return all(true);
	}

	/**
	 * Specify if this query's projection should select all in-scope expressions or not.
	 * <p>
	 * NOTE: if called with <code>true</code>, this setting will take precedence over any expressions added to the
	 * projection via {@link #select(Projectable...)} or {@link #select(Projection)} when converting to string via
	 * {@link #getQueryString()}
	 *
	 * @param selectAll if all in-scope expressions should be selected
	 * @return this
	 *
	 * @see Projection#all(boolean)
	 */
	public SelectQuery all(boolean selectAll) {
		select.all(selectAll);

		return this;
	}

	/**
	 * Add expressions to the query's projection
	 * <p>
	 * NOTE: if SELECT * has been specified (by {@link #all()} or calling {@link #all(boolean)} with <code>true</code>),
	 * that will take precedence over specified expressions when converting to string via {@link #getQueryString()}
	 *
	 * @param projectables expressions to add
	 * @return this
	 *
	 * @see Projection#select(Projectable...)
	 */
	public SelectQuery select(Projectable... projectables) {
		select.select(projectables);

		return this;
	}

	/**
	 * Set this query's projection
	 * <p>
	 * NOTE: if SELECT * has been specified (by {@link #all()} or calling {@link #all(boolean)} with <code>true</code>),
	 * that will take precedence over specified expressions when converting to string via {@link #getQueryString()}
	 *
	 * @param select the {@link Projection} to set
	 * @return this
	 */
	public SelectQuery select(Projection select) {
		this.select = select;

		return this;
	}

	@Override
	protected String getQueryActionString() {
		return select.getQueryString();
	}
}
