/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser;

import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A query formulated in the OpenRDF query algebra that produces a boolean value as its result.
 *
 * @author Arjohn Kampman
 */
public class ParsedBooleanQuery extends ParsedQuery {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new boolean query. To complete this query, a tuple expression needs to be supplied to it using
	 * {@link #setTupleExpr(TupleExpr)}.
	 */
	public ParsedBooleanQuery() {
		super();
	}

	/**
	 * Creates a new boolean query for the supplied tuple expression.
	 *
	 * @param tupleExpr A tuple expression representing the query, formulated in OpenRDF Query Algebra objects.
	 */
	public ParsedBooleanQuery(TupleExpr tupleExpr) {
		super(tupleExpr);
	}

	/**
	 * Creates a new boolean query for the supplied tuple expression.
	 *
	 * @param tupleExpr A tuple expression representing the query, formulated in OpenRDF Query Algebra objects.
	 */
	public ParsedBooleanQuery(String sourceString, TupleExpr tupleExpr) {
		super(sourceString, tupleExpr);
	}
}
