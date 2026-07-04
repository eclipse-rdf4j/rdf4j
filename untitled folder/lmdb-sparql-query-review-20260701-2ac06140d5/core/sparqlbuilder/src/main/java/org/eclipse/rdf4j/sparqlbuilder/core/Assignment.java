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

import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * A SPARQL expression-to-variable assignment
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#assignment"> SPARQL Assignments</a>
 */
public class Assignment implements Projectable, Groupable {
	private static final String AS = " AS ";
	private final Assignable expression;
	private final Variable var;

	Assignment(Assignable exp, Variable var) {
		this.expression = exp;
		this.var = var;
	}

	@Override
	public String getQueryString() {
		return SparqlBuilderUtils.getParenthesizedString(expression.getQueryString() + AS + var.getQueryString());
	}
}
