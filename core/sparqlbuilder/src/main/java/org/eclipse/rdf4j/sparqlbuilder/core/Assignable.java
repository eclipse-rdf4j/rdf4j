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

/**
 * A marker interface to denote objects which are bind-able in a SPARQL assignment expression.
 *
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#assignment"> SPARQL Assignments </a>
 *
 */
public interface Assignable extends QueryElement {
	/**
	 * Create a SPARQL assignment from this object
	 *
	 * @param var the variable to bind the expression value to
	 * @return an Assignment object
	 */
	default Assignment as(Variable var) {
		return SparqlBuilder.as(this, var);
	}
}
