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
package org.eclipse.rdf4j.query;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.explanation.Explanation;

/**
 * A query on a repository that can be formulated in one of the supported query languages (for example SPARQL). It
 * allows one to predefine bindings in the query to be able to reuse the same query with different bindings.
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
	 * @deprecated Use {@link Operation#setMaxExecutionTime(int)} instead.
	 */
	@Deprecated(since = "2.0")
	void setMaxQueryTime(int maxQueryTime);

	/**
	 * Returns the maximum query evaluation time.
	 *
	 * @return The maximum query evaluation time, measured in seconds.
	 * @see #setMaxQueryTime(int)
	 * @deprecated Use {@link Operation#getMaxExecutionTime()} instead.
	 */
	@Deprecated(since = "2.0")
	int getMaxQueryTime();

	/**
	 * <p>
	 * Explain how the query will be (or has been) executed/evaluated by returning an explanation of the query plan.
	 * </p>
	 *
	 * <p>
	 * This method is useful for understanding why a particular query is slow. The most useful level is Executed, but
	 * this takes as long as it takes to execute/evaluate the query.
	 * </p>
	 *
	 * <p>
	 * When timing a query you should keep in mind that the query performance will vary based on how much the JIT
	 * compiler has compiled the code (C1 vs C2) and based on what is or isn't cached in memory. If Timed explanations
	 * are considerably slower than Executed explanations the overhead with timing the query may be large on your system
	 * and should not be trusted.
	 * </p>
	 *
	 * <p>
	 * WARNING: This method is experimental and is subject to change or removal without warning. Same goes for the
	 * returned explanation. There is currently only partial support for this method in RDF4J and and
	 * UnsupportedOperationException where support is lacking.
	 * </p>
	 *
	 * @param level The explanation level that should be used to create the explanation. Choose between: Unoptimized (as
	 *              parsed without optimizations) , Optimized (as is actually going to be used), Executed (as was
	 *              executed/evaluated with actual result sizes), Telemetry (as was executed/evaluated, including
	 *              runtime telemetry metrics), Timed (as was executed/evaluated including timing for each plan node).
	 *              Executed, Telemetry and Timed levels can potentially be slow.
	 * @return The explanation that we generated, which can be viewed in a human readable format with toString(), as
	 *         JSON or as a simplified query plan object structure.
	 */
	@Experimental
	default Explanation explain(Explanation.Level level) {
		// with default implementation for backwards compatibility
		throw new UnsupportedOperationException();
	}

}
