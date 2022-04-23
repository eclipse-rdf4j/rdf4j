/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.constraint;

/**
 * The SPARQL unary operators
 */
enum UnaryOperator implements SparqlOperator {
	NOT("!"),
	UNARY_PLUS("+"),
	UNARY_MINUS("-");

	private final String operator;

	UnaryOperator(String operator) {
		this.operator = operator;
	}

	@Override
	public String getQueryString() {
		return operator;
	}
}
