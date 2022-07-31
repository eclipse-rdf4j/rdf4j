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
 * The SPARQL binary operators
 */
enum BinaryOperator implements SparqlOperator {
	EQUALS("="),
	GREATER_THAN(">"),
	GREATER_THAN_EQUALS(">="),
	LESS_THAN("<"),
	LESS_THAN_EQUALS("<="),
	NOT_EQUALS("!=");

	private final String operator;

	BinaryOperator(String operator) {
		this.operator = operator;
	}

	@Override
	public String getQueryString() {
		return operator;
	}
}
