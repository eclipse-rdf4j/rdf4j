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

import java.util.Map;

import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A ParsedGraphQuery to identify DESCRIBE queries.
 */
public class ParsedDescribeQuery extends ParsedGraphQuery {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new graph query. To complete this query, a tuple expression needs to be supplied to it using
	 * {@link #setTupleExpr(TupleExpr)}.
	 */
	public ParsedDescribeQuery() {
		super();
	}

	/**
	 * Creates a new graph query. To complete this query, a tuple expression needs to be supplied to it using
	 * {@link #setTupleExpr(TupleExpr)}.
	 *
	 * @param namespaces A mapping of namespace prefixes to namespace names representing the namespaces that are used in
	 *                   the query.
	 */
	public ParsedDescribeQuery(Map<String, String> namespaces) {
		super(namespaces);
	}

	/**
	 * Creates a new graph query for the supplied tuple expression.
	 *
	 * @param tupleExpr A tuple expression representing the query, formulated in Sail Query Model objects.
	 */
	public ParsedDescribeQuery(TupleExpr tupleExpr) {
		super(tupleExpr);
	}

	/**
	 * Creates a new graph query for the supplied tuple expression.
	 *
	 * @param tupleExpr A tuple expression representing the query, formulated in Sail Query Model objects.
	 */
	public ParsedDescribeQuery(String sourceString, TupleExpr tupleExpr) {
		super(sourceString, tupleExpr);
	}

	/**
	 * Creates a new graph query.
	 *
	 * @param tupleExpr  A tuple expression representing the query, formulated in Sail Query Model objects.
	 * @param namespaces A mapping of namespace prefixes to namespace names representing the namespaces that are used in
	 *                   the query.
	 */
	public ParsedDescribeQuery(TupleExpr tupleExpr, Map<String, String> namespaces) {
		super(tupleExpr, namespaces);
	}

	/**
	 * Creates a new graph query.
	 *
	 * @param tupleExpr  A tuple expression representing the query, formulated in Sail Query Model objects.
	 * @param namespaces A mapping of namespace prefixes to namespace names representing the namespaces that are used in
	 *                   the query.
	 */
	public ParsedDescribeQuery(String sourceString, TupleExpr tupleExpr, Map<String, String> namespaces) {
		super(sourceString, tupleExpr, namespaces);
	}
}
