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

import java.util.Collections;
import java.util.Map;

import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A query forumalated in the OpenRDF query algebra that produces an RDF graph (a set of statements) as its result.
 *
 * @author Arjohn Kampman
 */
public class ParsedGraphQuery extends ParsedQuery {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Map<String, String> queryNamespaces;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new graph query. To complete this query, a tuple expression needs to be supplied to it using
	 * {@link #setTupleExpr(TupleExpr)}.
	 */
	public ParsedGraphQuery() {
		super();
	}

	/**
	 * Creates a new graph query. To complete this query, a tuple expression needs to be supplied to it using
	 * {@link #setTupleExpr(TupleExpr)}.
	 *
	 * @param namespaces A mapping of namespace prefixes to namespace names representing the namespaces that are used in
	 *                   the query.
	 */
	public ParsedGraphQuery(Map<String, String> namespaces) {
		super();
		queryNamespaces = namespaces;
	}

	/**
	 * Creates a new graph query for the supplied tuple expression.
	 *
	 * @param tupleExpr A tuple expression representing the query, formulated in Sail Query Model objects.
	 */
	public ParsedGraphQuery(TupleExpr tupleExpr) {
		super(tupleExpr);
	}

	/**
	 * Creates a new graph query for the supplied tuple expression.
	 *
	 * @param tupleExpr A tuple expression representing the query, formulated in Sail Query Model objects.
	 */
	public ParsedGraphQuery(String sourceString, TupleExpr tupleExpr) {
		super(sourceString, tupleExpr);
	}

	/**
	 * Creates a new graph query.
	 *
	 * @param tupleExpr  A tuple expression representing the query, formulated in Sail Query Model objects.
	 * @param namespaces A mapping of namespace prefixes to namespace names representing the namespaces that are used in
	 *                   the query.
	 */
	public ParsedGraphQuery(TupleExpr tupleExpr, Map<String, String> namespaces) {
		this(tupleExpr);
		queryNamespaces = namespaces;
	}

	/**
	 * Creates a new graph query.
	 *
	 * @param tupleExpr  A tuple expression representing the query, formulated in Sail Query Model objects.
	 * @param namespaces A mapping of namespace prefixes to namespace names representing the namespaces that are used in
	 *                   the query.
	 */
	public ParsedGraphQuery(String sourceString, TupleExpr tupleExpr, Map<String, String> namespaces) {
		this(sourceString, tupleExpr);
		queryNamespaces = namespaces;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Map<String, String> getQueryNamespaces() {
		if (queryNamespaces != null) {
			return queryNamespaces;
		} else {
			return Collections.emptyMap();
		}
	}
}
