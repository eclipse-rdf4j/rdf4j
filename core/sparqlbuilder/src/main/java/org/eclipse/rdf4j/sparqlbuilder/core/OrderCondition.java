/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * An ascending or descending order condition
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy"> SPARQL Order By Clause</a>
 */
public class OrderCondition implements Orderable {
	private static final String ASC = "ASC";
	private static final String DESC = "DESC";
	private final Orderable orderOn;
	private boolean isAscending;

	OrderCondition(Orderable orderOn) {
		this(orderOn, true);
	}

	OrderCondition(Orderable orderOn, boolean ascending) {
		this.orderOn = orderOn;
		if (ascending) {
			asc();
		} else {
			desc();
		}
	}

	/**
	 * Set this order condition to be ascending
	 *
	 * @return this
	 */
	@Override
	public OrderCondition asc() {
		this.isAscending = true;

		return this;
	}

	/**
	 * Set this order condition to be descending
	 *
	 * @return this
	 */
	@Override
	public OrderCondition desc() {
		this.isAscending = false;

		return this;
	}

	@Override
	public String getQueryString() {
		StringBuilder condition = new StringBuilder();

		if (orderOn != null) {
			if (isAscending) {
				condition.append(ASC);
			} else {
				condition.append(DESC);
			}

			condition.append(SparqlBuilderUtils.getParenthesizedString(orderOn.getQueryString()));
		}

		return condition.toString();
	}
}
