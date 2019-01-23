/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.builder;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedDescribeQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;

/**
 * <p>
 * Factory class for obtaining instances of {@link QueryBuilder} objects for the various types of queries.
 * </p>
 * 
 * @author Michael Grove
 * @deprecated use {@link org.eclipse.rdf4j.sparqlbuilder} instead.
 */
@Deprecated
public class QueryBuilderFactory {

	/**
	 * Create a QueryBuilder for creating a select query
	 * 
	 * @return a select QueryBuilder
	 */
	public static QueryBuilder<ParsedBooleanQuery> ask() {
		return new AbstractQueryBuilder<>(new ParsedBooleanQuery());
	}

	/**
	 * Create a QueryBuilder for creating a select query
	 * 
	 * @return a select QueryBuilder
	 */
	public static QueryBuilder<ParsedTupleQuery> select() {
		return new AbstractQueryBuilder<>(new ParsedTupleQuery());
	}

	/**
	 * Create a QueryBuilder for creating a select query
	 * 
	 * @param theProjectionVars
	 *        the list of elements in the projection of the query
	 * @return a select query builder
	 */
	public static QueryBuilder<ParsedTupleQuery> select(String... theProjectionVars) {
		QueryBuilder<ParsedTupleQuery> aBuilder = new AbstractQueryBuilder<>(
				new ParsedTupleQuery());
		aBuilder.addProjectionVar(theProjectionVars);

		return aBuilder;
	}

	/**
	 * Create a QueryBuilder for building a construct query
	 * 
	 * @return a construct QueryBuilder
	 */
	public static QueryBuilder<ParsedGraphQuery> construct() {
		return new AbstractQueryBuilder<>(new ParsedGraphQuery());
	}

	/**
	 * Create a QueryBuilder for creating a describe query
	 * 
	 * @param theValues
	 *        the specific bound URI values to be described
	 * @return a describe query builder
	 */
	public static QueryBuilder<ParsedGraphQuery> describe(Resource... theValues) {
		return describe(null, theValues);
	}

	/**
	 * Create a QueryBuilder for creating a describe query
	 * 
	 * @param theVars
	 *        the variables to be described
	 * @param theValues
	 *        the specific bound URI values to be described
	 * @return a describe query builder
	 */
	public static QueryBuilder<ParsedGraphQuery> describe(String[] theVars, Resource... theValues) {
		QueryBuilder<ParsedGraphQuery> aBuilder = new AbstractQueryBuilder<>(
				new ParsedDescribeQuery());

		aBuilder.reduced();
		aBuilder.addProjectionVar("descr_subj", "descr_pred", "descr_obj");
		GroupBuilder<?, ?> aGroup = aBuilder.group();

		if (theVars != null) {
			for (String aVar : theVars) {
				Var aVarObj = new Var(aVar);
				aVarObj.setAnonymous(true);

				aGroup.filter().or(new SameTerm(aVarObj, new Var("descr_subj")),
						new SameTerm(aVarObj, new Var("descr_obj")));
			}
		}

		if (theValues != null) {
			for (Resource aVar : theValues) {
				Var aSubjVar = new Var("descr_subj");
				aSubjVar.setAnonymous(true);

				Var aObjVar = new Var("descr_obj");
				aObjVar.setAnonymous(true);

				aGroup.filter().or(new SameTerm(new ValueConstant(aVar), aSubjVar),
						new SameTerm(new ValueConstant(aVar), aObjVar));
			}
		}

		aGroup.atom("descr_subj", "descr_pred", "descr_obj");

		return aBuilder;
	}
}
