/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.ExclusiveTupleExpr;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.structures.QueryType;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Interface for implementations of triple sources. Particular implementations define how to evaluate the expression on
 * the endpoint. Different implementations might be necessary depending on the underlying repository.
 *
 * @author Andreas Schwarte
 *
 * @see SparqlTripleSource
 * @see SailTripleSource
 */
public interface TripleSource {

	/**
	 * Evaluate the prepared query in its internal representation on the provided endpoint.
	 *
	 * @param preparedQuery a prepared query to evaluate
	 * @param bindings      the bindings to use
	 * @param filterExpr    the filter expression to apply or null if there is no filter or if it is evaluated already
	 *
	 * @return the resulting iteration
	 *
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	CloseableIteration<BindingSet, QueryEvaluationException> getStatements(TupleExpr preparedQuery,
			final BindingSet bindings, FilterValueExpr filterExpr, QueryInfo queryInfo)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException;

	/**
	 * Evaluate the prepared query (SPARQL query as String) on the provided endpoint.
	 *
	 * @param preparedQuery a prepared query to evaluate (SPARQL query as String)
	 * @param bindings      the bindings to use
	 * @param filterExpr    the filter expression to apply or null if there is no filter or if it is evaluated already
	 *
	 * @return the resulting iteration
	 *
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	CloseableIteration<BindingSet, QueryEvaluationException> getStatements(String preparedQuery,
			final BindingSet bindings, FilterValueExpr filterExpr, QueryInfo queryInfo)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException;

	/**
	 * Evaluate a given SPARQL query of the provided query type at the given source.
	 *
	 * @param preparedQuery
	 * @param queryType
	 * @param queryInfo
	 * @return the statements
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 * @Deprecated will be removed in 4.0. Replaced with
	 *             {@link #getStatements(String, BindingSet, QueryType, QueryInfo)}
	 */
	@Deprecated
	default CloseableIteration<BindingSet, QueryEvaluationException> getStatements(String preparedQuery,
			QueryType queryType, QueryInfo queryInfo)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		return getStatements(preparedQuery, EmptyBindingSet.getInstance(), queryType, queryInfo);
	}

	/**
	 * Evaluate a given SPARQL query of the provided query type at the given source.
	 *
	 * @param preparedQuery
	 * @param queryBindings optional query bindings, use {@link EmptyBindingSet} if there are none
	 * @param queryType
	 * @param queryInfo
	 * @return the statements
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	CloseableIteration<BindingSet, QueryEvaluationException> getStatements(String preparedQuery,
			BindingSet queryBindings,
			QueryType queryType, QueryInfo queryInfo)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException;

	/**
	 * Evaluate the query expression on the provided endpoint.
	 *
	 * @param stmt       the stmt expression to evaluate
	 * @param bindings   the bindings to use
	 * @param filterExpr the filter expression to apply or null if there is no filter or if it is evaluated already
	 *
	 * @return the resulting iteration
	 *
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	CloseableIteration<BindingSet, QueryEvaluationException> getStatements(StatementPattern stmt,
			final BindingSet bindings, FilterValueExpr filterExpr, QueryInfo queryInfo)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException;

	/**
	 * Return the statements matching the given pattern as a {@link Statement} iteration.
	 *
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param contexts
	 *
	 * @return the resulting iteration
	 *
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	CloseableIteration<Statement, QueryEvaluationException> getStatements(
			Resource subj, IRI pred, Value obj, QueryInfo queryInfo, Resource... contexts)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException;

	/**
	 * Check if the provided statement can return results.
	 *
	 * @param stmt
	 * @param bindings  a binding set. in case no bindings are present, an {@link EmptyBindingSet} can be used (i.e.
	 *                  never null)
	 * @param queryInfo
	 * @param dataset
	 *
	 * @return whether the source can return results
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	boolean hasStatements(StatementPattern stmt, BindingSet bindings, QueryInfo queryInfo, Dataset dataset)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException;

	/**
	 * Check if the repository can return results for the given triple pattern represented by subj, pred and obj
	 *
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param queryInfo
	 * @param contexts
	 * @return whether the source can provide results
	 * @throws RepositoryException
	 */
	boolean hasStatements(Resource subj, IRI pred, Value obj, QueryInfo queryInfo, Resource... contexts)
			throws RepositoryException;

	/**
	 * Check if the repository can return results for the given {@link ExclusiveTupleExpr}, e.g. for an
	 * {@link ExclusiveGroup} with a list of Statements.
	 *
	 * @param group
	 * @param bindings
	 * @return whether the repository can return results
	 * @throws RepositoryException
	 */
	boolean hasStatements(ExclusiveTupleExpr expr, BindingSet bindings)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException;

	/**
	 *
	 * @return true if a prepared query is to be used preferably, false otherwise
	 * @deprecated replaced with {@link #usePreparedQuery(StatementPattern, QueryInfo)}, to be removed in 4.0
	 */
	@Deprecated
	default boolean usePreparedQuery() {
		return true;
	}

	/**
	 *
	 * @param stmt
	 * @param queryInfo
	 * @return true if a prepared query is to be used preferably, false otherwise
	 */
	boolean usePreparedQuery(StatementPattern stmt, QueryInfo queryInfo);

}
