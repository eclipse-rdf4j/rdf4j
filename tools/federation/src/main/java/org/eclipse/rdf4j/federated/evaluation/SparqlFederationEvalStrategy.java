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

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.CheckStatementPattern;
import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.FilterTuple;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.evaluation.iterator.BoundJoinConversionIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.BoundJoinVALUESConversionIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.FilteringIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.GroupedCheckConversionIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.InsertBindingsIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.SingleBindingSetIteration;
import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerBoundJoin;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.exception.IllegalQueryException;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Implementation of a federation evaluation strategy which provides some special optimizations for SPARQL (remote)
 * endpoints. The most important optimization is to used prepared SPARQL Queries that are already created using Strings.
 * <p>
 * Joins are executed using {@link ControlledWorkerBoundJoin}.
 * </p>
 * <p>
 * This implementation uses the SPARQL 1.1 VALUES operator for the bound-join evaluation
 * </p>
 * s
 *
 * @author Andreas Schwarte
 *
 */
public class SparqlFederationEvalStrategy extends FederationEvalStrategy {

	public SparqlFederationEvalStrategy(FederationContext federationContext) {
		super(federationContext);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateBoundJoinStatementPattern(
			StatementTupleExpr stmt, List<BindingSet> bindings)
			throws QueryEvaluationException {

		// we can omit the bound join handling
		if (bindings.size() == 1) {
			return evaluate(stmt, bindings.get(0));
		}

		FilterValueExpr filterExpr = null;
		if (stmt instanceof FilterTuple) {
			filterExpr = ((FilterTuple) stmt).getFilterExpr();
		}

		AtomicBoolean isEvaluated = new AtomicBoolean(false);
		String preparedQuery = QueryStringUtil.selectQueryStringBoundJoinVALUES((StatementPattern) stmt, bindings,
				filterExpr, isEvaluated, stmt.getQueryInfo().getDataset());

		CloseableIteration<BindingSet, QueryEvaluationException> result = null;
		try {
			result = evaluateAtStatementSources(preparedQuery, stmt.getStatementSources(), stmt.getQueryInfo());

			// apply filter and/or convert to original bindings
			if (filterExpr != null && !isEvaluated.get()) {
				result = new BoundJoinVALUESConversionIteration(result, bindings); // apply conversion
				result = new FilteringIteration(filterExpr, result, this); // apply filter
				if (!result.hasNext()) {
					result.close();
					return new EmptyIteration<>();
				}
			} else {
				result = new BoundJoinVALUESConversionIteration(result, bindings);
			}

			return result;
		} catch (Throwable t) {
			if (result != null) {
				result.close();
			}
			if (t instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw ExceptionUtil.toQueryEvaluationException(t);
		}
	}

	/**
	 * Alternative evaluation implementation using UNION. Nowadays we use a VALUES clause based implementation
	 *
	 * @deprecated
	 */
	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateBoundJoinStatementPattern_UNION(
			StatementTupleExpr stmt, List<BindingSet> bindings)
			throws QueryEvaluationException {

		// we can omit the bound join handling
		if (bindings.size() == 1) {
			return evaluate(stmt, bindings.get(0));
		}

		FilterValueExpr filterExpr = null;
		if (stmt instanceof FilterTuple) {
			filterExpr = ((FilterTuple) stmt).getFilterExpr();
		}

		Boolean isEvaluated = false;
		String preparedQuery = QueryStringUtil.selectQueryStringBoundUnion((StatementPattern) stmt, bindings,
				filterExpr, isEvaluated, stmt.getQueryInfo().getDataset());

		CloseableIteration<BindingSet, QueryEvaluationException> result = evaluateAtStatementSources(preparedQuery,
				stmt.getStatementSources(), stmt.getQueryInfo());

		// apply filter and/or convert to original bindings
		if (filterExpr != null && !isEvaluated) {
			result = new BoundJoinConversionIteration(result, bindings); // apply conversion
			result = new FilteringIteration(filterExpr, result, this); // apply filter
			if (!result.hasNext()) {
				return new EmptyIteration<>();
			}
		} else {
			result = new BoundJoinConversionIteration(result, bindings);
		}

		return result;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateGroupedCheck(
			CheckStatementPattern stmt, List<BindingSet> bindings)
			throws QueryEvaluationException {

		if (bindings.size() == 1) {
			return stmt.evaluate(bindings.get(0));
		}

		String preparedQuery = QueryStringUtil.selectQueryStringBoundCheck(stmt.getStatementPattern(), bindings,
				stmt.getQueryInfo().getDataset());

		CloseableIteration<BindingSet, QueryEvaluationException> result = evaluateAtStatementSources(preparedQuery,
				stmt.getStatementSources(), stmt.getQueryInfo());

		return new GroupedCheckConversionIteration(result, bindings);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> executeJoin(
			ControlledWorkerScheduler<BindingSet> joinScheduler,
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter,
			TupleExpr rightArg, Set<String> joinVars, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {

		ControlledWorkerBoundJoin join = new ControlledWorkerBoundJoin(joinScheduler, this, leftIter, rightArg,
				bindings, queryInfo);
		join.setJoinVars(joinVars);
		executor.execute(join);
		return join;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateExclusiveGroup(
			ExclusiveGroup group, BindingSet bindings) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {

		TripleSource tripleSource = group.getOwnedEndpoint().getTripleSource();
		AtomicBoolean isEvaluated = new AtomicBoolean(false);

		try {
			String preparedQuery = QueryStringUtil.selectQueryString(group, bindings, group.getFilterExpr(),
					isEvaluated, group.getQueryInfo().getDataset());
			return tripleSource.getStatements(preparedQuery, bindings,
					(isEvaluated.get() ? null : group.getFilterExpr()), group.getQueryInfo());
		} catch (IllegalQueryException e) {
			/* no projection vars, e.g. local vars only, can occur in joins */
			if (tripleSource.hasStatements(group, bindings)) {
				CloseableIteration<BindingSet, QueryEvaluationException> res = new SingleBindingSetIteration(bindings);
				if (group.getBoundFilters() != null) {
					// make sure to insert any values from FILTER expressions that are directly
					// bound in this expression
					res = new InsertBindingsIteration(res, group.getBoundFilters());
				}
				return res;
			}
			return new EmptyIteration<>();
		}

	}
}
