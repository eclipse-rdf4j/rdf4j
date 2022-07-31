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

package org.eclipse.rdf4j.sparqlbuilder.constraint;

import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * A SPARQL aggregate expression.
 *
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#aggregates"> SPARQL Aggregates</a>
 */
public class Aggregate extends Expression<Aggregate> {
	private static final String DISTINCT = "DISTINCT";
	private static final Object SEPARATOR = "SEPARATOR";

	private String separator;
	private boolean isDistinct = false;
	private boolean countAll = false;

	Aggregate(SparqlAggregate aggregate) {
		super(aggregate);
	}

	/**
	 * Specify this aggregate expression to be distinct
	 *
	 * @return this aggregate instance
	 */
	public Aggregate distinct() {
		return distinct(true);
	}

	/**
	 * Specify if this aggregate expression should be distinct or not
	 *
	 * @param isDistinct if this aggregate should be distinct
	 *
	 * @return this aggregate instance
	 */
	public Aggregate distinct(boolean isDistinct) {
		this.isDistinct = isDistinct;

		return this;
	}

	/**
	 * If this is a {@code count} aggregate expressions, specify that it should count all
	 *
	 * @return this aggregate instance
	 */
	public Aggregate countAll() {
		return countAll(true);
	}

	/**
	 * If this is a {@code count} aggregate expressions, specify if it should count all
	 *
	 * @param countAll if this should count all arguments or not
	 *
	 * @return this aggregate instance
	 */
	public Aggregate countAll(boolean countAll) {
		this.countAll = countAll;

		return this;
	}

	/**
	 * If this is a {@code group_concat} aggregate expression, specify the separator to use
	 *
	 * @param separator the separator to use
	 *
	 * @return this aggregate instance
	 *
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#defn_aggGroupConcat"> group_concat()</a>
	 */
	public Aggregate separator(String separator) {
		this.separator = separator;

		return this;
	}

	@Override
	public String getQueryString() {
		StringBuilder aggregate = new StringBuilder();
		StringBuilder params = new StringBuilder();

		aggregate.append(operator.getQueryString());

		if (isDistinct) {
			params.append(DISTINCT).append(" ");
		}

		// Yeah. I know...
		if (operator == SparqlAggregate.COUNT && countAll) {
			params.append("*");
		} else {
			params.append(super.getQueryString());
		}

		// Yep, I still know...
		if (operator == SparqlAggregate.GROUP_CONCAT && separator != null) {
			params.append(" ")
					.append(";")
					.append(" ")
					.append(SEPARATOR)
					.append(" ")
					.append("=")
					.append(" ")
					.append(separator);
		}

		return aggregate.append(SparqlBuilderUtils.getParenthesizedString(params.toString())).toString();
	}
}
