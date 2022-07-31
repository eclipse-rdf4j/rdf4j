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

/**
 * The built-in SPARQL aggregates. Keeping this public until {@link Expressions} is completed.
 *
 * @see <a href= "http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#SparqlOps"> SPARQL Function Definitions</a>
 */
@SuppressWarnings("javadoc") // acceptable, as this won't be public for long
public enum SparqlAggregate implements SparqlOperator {
	AVG("AVG"),
	COUNT("COUNT"),
	GROUP_CONCAT("GROUP_CONCAT"),
	MAX("MAX"),
	MIN("MIN"),
	SAMPLE("SAMPLE"),
	SUM("SUM");

	private final String function;

	SparqlAggregate(String function) {
		this.function = function;
	}

	@Override
	public String getQueryString() {
		return function;
	}
}
