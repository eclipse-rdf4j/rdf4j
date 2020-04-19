/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query;

import org.eclipse.rdf4j.model.Statement;

/**
 * A query on a repository that can be formulated in one of the supported query languages (for example SeRQL or SPARQL).
 * It allows one to predefine bindings in the query to be able to reuse the same query with different bindings.
 *
 * @author Arjohn Kampman
 * @author jeen
 */
public interface Query extends Operation {

	/**
	 * The different types of queries that RDF4J recognizes: boolean queries, graph queries, and tuple queries.
	 *
	 * @since 3.2.0
	 */
	enum QueryType {
		/**
		 * Boolean queries (such as the SPARQL ASK query form) return either {@code true} or {@code false} as the
		 * result.
		 */
		BOOLEAN,
		/**
		 * Graph queries (such as the SPARQL CONSTRUCT and DESCRIBE query form) return a sequence of RDF
		 * {@link Statement statements} as the result.
		 */
		GRAPH,
		/**
		 * Tuple queries (such as the SPARQL SELECT query form) return a sequence of {@link BindingSet sets of variable
		 * bindings} as the result.
		 */
		TUPLE
	}

	/**
	 * Specifies the maximum time that a query is allowed to run. The query will be interrupted when it exceeds the time
	 * limit. Any consecutive requests to fetch query results will result in {@link QueryInterruptedException}s.
	 *
	 * @param maxQueryTime The maximum query time, measured in seconds. A negative or zero value indicates an unlimited
	 *                     query time (which is the default).
	 * @deprecated since 2.0. Use {@link Operation#setMaxExecutionTime(int)} instead.
	 */
	@Deprecated
	void setMaxQueryTime(int maxQueryTime);

	/**
	 * Returns the maximum query evaluation time.
	 *
	 * @return The maximum query evaluation time, measured in seconds.
	 * @see #setMaxQueryTime(int)
	 * @deprecated since 2.0. Use {@link Operation#getMaxExecutionTime()} instead.
	 */
	@Deprecated
	int getMaxQueryTime();

	// TODO - make default with no-op before merging to develop for backwards compatibility
	// Also wondering if it makes sense to move this up a level to the Operation interface? Since we will be using a
	// default no-op implementation we could move it without affecting backwards compatibility.
	QueryExplainWrapper explain(QueryExplainLevel queryExplainLevel);

	/**
	 * The different types of queries that RDF4J recognizes: boolean queries, graph queries, and tuple queries.
	 *
	 * @since 3.2.0
	 */
	enum QueryExplainLevel {
		Unoptimized,
		Optimized,
		Executed
	}

	interface QueryExplainWrapper {

		String toString();
		// TupleExpr asTupleExpr(); location in maven hierarchy prevents us from using TupleExpr here
	}

}
