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

import java.util.ArrayList;

/**
 * A SPARQL Projection
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#selectproject"> SPARQL Projections</a>
 */
public class Projection extends QueryElementCollection<Projectable> {
	private static final String SELECT = "SELECT";
	private static final String DISTINCT = "DISTINCT";
	private static final String DELIMETER = " ";

	private boolean isDistinct, selectAll;

	Projection() {
		super(DELIMETER, new ArrayList<>());
		all(false);
		distinct(false);
	}

	/**
	 * Specify this projection to be distinct
	 *
	 * @return this
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modDistinct"> SPARQL Distinct modifier</a>
	 */
	public Projection distinct() {
		return distinct(true);
	}

	/**
	 * Specify if this projection should be distinct or not
	 *
	 * @param isDistinct if this projection should be distinct
	 * @return this
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modDistinct"> SPARQL Distinct modifier</a>
	 */
	public Projection distinct(boolean isDistinct) {
		this.isDistinct = isDistinct;

		return this;
	}

	/**
	 * Specify that this projection should select all in-scope expressions
	 *
	 * @return this
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select"> SPARQL Select</a>
	 */
	public Projection all() {
		return all(true);
	}

	/**
	 * Specify if this projection should select all in-scope expressions or not
	 *
	 * @param selectAll if this projection should select all expressions
	 * @return this
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select"> SPARQL Select</a>
	 */
	public Projection all(boolean selectAll) {
		this.selectAll = selectAll;

		return this;
	}

	/**
	 * Add expressions for this projection to select
	 *
	 * @param projectables the projectable expressions to add
	 * @return this
	 */
	public Projection select(Projectable... projectables) {
		addElements(projectables);

		return this;
	}

	@Override
	public String getQueryString() {
		StringBuilder selectStatement = new StringBuilder();
		selectStatement.append(SELECT).append(" ");

		if (selectAll || isEmpty()) {
			selectStatement.append("*").append(" ");
		} else {
			if (isDistinct) {
				selectStatement.append(DISTINCT).append(" ");
			}

			selectStatement.append(super.getQueryString());
		}

		return selectStatement.toString();
	}
}
