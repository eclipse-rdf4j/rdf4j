/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.builder;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.parser.ParsedQuery;

/**
 * <p>
 * Interface for a QueryBuilder which provides a simple fluent API for
 * constructing Sesame query object programmatically.
 * </p>
 * 
 * @author Michael Grove
 * @since 2.7.0
 */
public interface QueryBuilder<T extends ParsedQuery> extends SupportsGroups {

	/**
	 * Return the query constructed by this query builder
	 * 
	 * @return the query
	 */
	public T query();

	/**
	 * Specify an offset for the query
	 * 
	 * @param theOffset
	 *        the new offset
	 * @return this query builder
	 */
	public QueryBuilder<T> offset(int theOffset);

	/**
	 * Specify a limit for the query
	 * 
	 * @param theLimit
	 *        the new limit for the query
	 * @return this query builder
	 */
	public QueryBuilder<T> limit(int theLimit);

	/**
	 * Create an option sub-group
	 * 
	 * @return the new group
	 */
	public GroupBuilder<T, QueryBuilder<T>> optional();

	/**
	 * Create a new sub-group of the query
	 * 
	 * @return the new group
	 */
	public GroupBuilder<T, QueryBuilder<T>> group();

	/**
	 * Reset the state of the query builder
	 */
	public void reset();

	/**
	 * Specify that this query should use the "distinct" keyword
	 * 
	 * @return this query builder
	 */
	public QueryBuilder<T> distinct();

	/**
	 * Specify that this query should use the "reduced" keyword
	 * 
	 * @return this query builder
	 */
	public QueryBuilder<T> reduced();

	/**
	 * Add projection variables to the query
	 * 
	 * @param theNames
	 *        the names of the variables to add to the projection
	 * @return this query builder
	 */
	public QueryBuilder<T> addProjectionVar(String... theNames);

	/**
	 * Add a from clause to this query
	 * 
	 * @param theURI
	 *        the from URI
	 * @return this query builder
	 */
	public QueryBuilder<T> from(IRI theURI);

	/**
	 * Add a 'from named' clause to this query
	 * 
	 * @param theURI
	 *        the graph URI
	 * @return this query builder
	 */
	public QueryBuilder<T> fromNamed(IRI theURI);

	/**
	 * Specify ORDER BY clause with ASC modifier by default
	 * @param theNames the names of the variables to apply the ordering
	 * @return this query builder
	 */
	public QueryBuilder<T> orderBy(String... theNames);

	/**
	 * Specify ORDER BY clause with ASC modifier
	 * @param theNames the names of the variables to apply the ordering
	 * @return this query builder
	 */
	public QueryBuilder<T> orderByAsc(String... theNames);

	/**
	 * Specify ORDER BY clause with DESC modifier
	 * @param theNames the names of the variables to apply the ordering
	 * @return this query builder
	 */
	public QueryBuilder<T> orderByDesc(String... theNames);

	public QueryBuilder<T> addProjectionStatement(String theSubj, String thePred, String theObj);

	public QueryBuilder<T> addProjectionStatement(String theSubj, String thePred, Value theObj);

	public QueryBuilder<T> addProjectionStatement(String theSubj, IRI thePred, Value theObj);

	public QueryBuilder<T> addProjectionStatement(IRI theSubj, String thePred, String theObj);

	public QueryBuilder<T> addProjectionStatement(IRI theSubj, IRI thePred, String theObj);

	public QueryBuilder<T> addProjectionStatement(String theSubj, IRI thePred, String theObj);
}
