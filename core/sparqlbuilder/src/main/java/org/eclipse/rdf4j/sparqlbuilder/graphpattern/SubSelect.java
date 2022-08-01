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

import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projection;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Query;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * A SPARQL subquery
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#subqueries"> SPARQL Subquery</a>
 */
public class SubSelect extends Query<SubSelect> implements GraphPattern {
	private Projection select = SparqlBuilder.select();

	/*
	 * If someone has any ideas how I can eliminate the need for repeating the following methods from SelectQuery
	 * without inheriting the methods that don't apply to SubSelect (prologue and dataset stuff), that would be awesome.
	 */

	/**
	 * Specify the query's projection to be distinct
	 *
	 * @return this
	 *
	 * @see Projection#distinct()
	 */
	public SubSelect distinct() {
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
	public SubSelect distinct(boolean isDistinct) {
		select.distinct(isDistinct);

		return this;
	}

	/**
	 * Specify that this query's projection should select all in-scope expressions
	 * <p>
	 * NOTE: setting this takes precedence over any expressions added to the projection via
	 * {@link #select(Projectable...)} when printing
	 *
	 * @return this
	 *
	 * @see Projection#all()
	 */
	public SubSelect all() {
		return all(true);
	}

	/**
	 * Specify if this query's projection should select all in-scope expressions or not.
	 * <p>
	 * NOTE: if called with <code>true</code>, this setting will take precedence over any expressions added to the
	 * projection via {@link #select(Projectable...)} when printing
	 *
	 * @param selectAll if all in-scope expressions should be selected
	 * @return this
	 *
	 * @see Projection#all(boolean)
	 */
	public SubSelect all(boolean selectAll) {
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
	public SubSelect select(Projectable... projectables) {
		select.select(projectables);

		return this;
	}

	/**
	 * Set this query's projection
	 *
	 * @param select the {@link Projection} to set
	 * @return this
	 */
	public SubSelect select(Projection select) {
		this.select = select;

		return this;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	// TODO: Values

	@Override
	protected String getQueryActionString() {
		return select.getQueryString();
	}

	@Override
	public String getQueryString() {
		StringBuilder subSelect = new StringBuilder();

		subSelect.append(super.getQueryString());

		// TODO: VALUES
		// subselect.append(values.getQueryString());

		return SparqlBuilderUtils.getBracedString(subSelect.toString());
	}
}
