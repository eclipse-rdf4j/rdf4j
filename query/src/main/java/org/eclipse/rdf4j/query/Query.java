/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query;

/**
 * A query on a repository that can be formulated in one of the supported query languages (for example SeRQL or SPARQL).
 * It allows one to predefine bindings in the query to be able to reuse the same query with different bindings.
 * 
 * @author Arjohn Kampman
 * @author jeen
 */
public interface Query extends Operation {

	/**
	 * Specifies the maximum time that a query is allowed to run. The query will be interrupted when it exceeds the time
	 * limit. Any consecutive requests to fetch query results will result in {@link QueryInterruptedException}s.
	 * 
	 * @param maxQueryTime The maximum query time, measured in seconds. A negative or zero value indicates an unlimited
	 *                     query time (which is the default).
	 * @deprecated since 2.8.0. Use {@link Operation#setMaxExecutionTime(int)} instead.
	 */
	@Deprecated
	public void setMaxQueryTime(int maxQueryTime);

	/**
	 * Returns the maximum query evaluation time.
	 * 
	 * @return The maximum query evaluation time, measured in seconds.
	 * @see #setMaxQueryTime(int)
	 * @deprecated since 2.8.0. Use {@link Operation#getMaxExecutionTime()} instead.
	 */
	@Deprecated
	public int getMaxQueryTime();
}
