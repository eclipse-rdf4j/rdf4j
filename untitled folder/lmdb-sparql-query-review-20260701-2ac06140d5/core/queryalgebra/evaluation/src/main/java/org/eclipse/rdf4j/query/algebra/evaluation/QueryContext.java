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
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link QueryContext} provides a means to pass arbitrary local state to a
 * {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function} or
 * {@link org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction}. The implementation of
 * {@link org.eclipse.rdf4j.sail.SailConnection#evaluate()} is responsible for initialising a QueryContext and making it
 * available during evaluation via {@link org.eclipse.rdf4j.query.algebra.evaluation.iterator.QueryContextIteration}. A
 * QueryContext is commonly used to provide a {@link QueryPreparer} for the current SailConnection. This allows, for
 * example, Functions to be written that conveniently express more complex queries.
 */
public class QueryContext {

	private static final String QUERY_PREPARER_ATTRIBUTE = QueryPreparer.class.getName();

	private static final ThreadLocal<QueryContext> queryContext = new ThreadLocal<>();

	public static QueryContext getQueryContext() {
		return queryContext.get();
	}

	private final Map<String, Object> attributes = new HashMap<>();

	private QueryContext previous;

	public QueryContext() {
	}

	public QueryContext(QueryPreparer qp) {
		setAttribute(QUERY_PREPARER_ATTRIBUTE, qp);
	}

	public void begin() {
		this.previous = queryContext.get();
		queryContext.set(this);
	}

	public QueryPreparer getQueryPreparer() {
		return getAttribute(QUERY_PREPARER_ATTRIBUTE);
	}

	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	public <T> T getAttribute(String name) {
		return (T) attributes.get(name);
	}

	public void end() {
		queryContext.remove();
		if (previous != null) {
			queryContext.set(previous);
		}
	}
}
