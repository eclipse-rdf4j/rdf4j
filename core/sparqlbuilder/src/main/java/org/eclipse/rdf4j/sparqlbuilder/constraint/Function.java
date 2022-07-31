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
 * A SPARQL Function. Consists of a function name and a parenthesized, comma-separated list of arguments.
 *
 * @see <a href="http://www.w3.org/TR/rdf-sparql-query/#termConstraint">SPARQL Filters</a>
 * @see <a href= "http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#SparqlOps"> SPARQL Function Definitions</a>
 */
class Function extends Expression<Function> {
	Function(SparqlFunction function) {
		super(function, ", ");
		parenthesize();
		printBodyIfEmpty(true);
		setOperatorName(operator.getQueryString(), false);
	}
}
