/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin;

import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;

public class QueryContext {

	private static final ThreadLocal<QueryPreparer> queryPreparer = new ThreadLocal<QueryPreparer>();

	public static QueryPreparer getQueryPreparer() {
		return queryPreparer.get();
	}

	public static QueryContext begin(QueryPreparer qp) {
		return new QueryContext(qp);
	}

	private final QueryPreparer previous;

	private QueryContext(QueryPreparer qp) {
		this.previous = queryPreparer.get();
		queryPreparer.set(qp);
	}

	public void end() {
		queryPreparer.remove();
		if (previous != null) {
			queryPreparer.set(previous);
		}
	}
}
