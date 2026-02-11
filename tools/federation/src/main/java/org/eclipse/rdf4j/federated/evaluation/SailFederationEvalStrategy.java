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
import org.eclipse.rdf4j.federated.evaluation.iterator.FilteringIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.GroupedCheckConversionIteration;
import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerJoin;
import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerLeftJoin;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Implementation of a federation evaluation strategy which provides some special optimizations for Native (local) RDF4J
 * repositories. The most important optimization is to use prepared Queries that are already created in the internal
 * representation used by RDF4J. This is necessary to avoid String parsing overhead.
 *
 * Joins are executed using {@link ControlledWorkerJoin}
 *
 * @author Andreas Schwarte
 *
 */
@Deprecated(forRemoval = true) // will be replaced with single unified federation eval strategy
public class SailFederationEvalStrategy extends FederationEvalStrategy {

	public SailFederationEvalStrategy(FederationContext federationContext) {
		super(federationContext);
	}

	@Override
	public CloseableIteration<BindingSet> evaluateBoundJoinStatementPattern(
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
		TupleExpr preparedQuery = QueryAlgebraUtil.selectQueryBoundUnion((StatementPattern) stmt, bindings, filterExpr,
				isEvaluated);

		CloseableIteration<BindingSet> result = evaluateAtStatementSources(preparedQuery,
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
	public CloseableIteration<BindingSet> evaluateGroupedCheck(
			CheckStatementPattern stmt, List<BindingSet> bindings)
			throws QueryEvaluationException {

		if (bindings.size() == 1) {
			return stmt.evaluate(bindings.get(0));
		}

		TupleExpr preparedQuery = QueryAlgebraUtil.selectQueryStringBoundCheck(stmt.getStatementPattern(), bindings);

		CloseableIteration<BindingSet> result = evaluateAtStatementSources(preparedQuery,
				stmt.getStatementSources(), stmt.getQueryInfo());

		return new GroupedCheckConversionIteration(result, bindings);
	}

	@Override
	public CloseableIteration<BindingSet> executeJoin(
			ControlledWorkerScheduler<BindingSet> joinScheduler,
			CloseableIteration<BindingSet> leftIter,
			TupleExpr rightArg, Set<String> joinVars, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {

		ControlledWorkerJoin join = new ControlledWorkerJoin(joinScheduler, this, leftIter, rightArg, bindings,
				queryInfo);
		join.setJoinVars(joinVars);
		executor.execute(join);
		return join;
	}

	@Override
	protected CloseableIteration<BindingSet> executeLeftJoin(ControlledWorkerScheduler<BindingSet> joinScheduler,
			CloseableIteration<BindingSet> leftIter, LeftJoin leftJoin, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {
		ControlledWorkerLeftJoin join = new ControlledWorkerLeftJoin(joinScheduler, this,
				leftIter, leftJoin, bindings, queryInfo);
		executor.execute(join);
		return join;
	}

	@Override
	public CloseableIteration<BindingSet> evaluateExclusiveGroup(
			ExclusiveGroup group, BindingSet bindings)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		// simple thing: use a prepared query
		TripleSource tripleSource = group.getOwnedEndpoint().getTripleSource();
		AtomicBoolean isEvaluated = new AtomicBoolean(false);
		TupleExpr preparedQuery = QueryAlgebraUtil.selectQuery(group, bindings, group.getFilterExpr(), isEvaluated);
		return tripleSource.getStatements(preparedQuery, bindings,
				(isEvaluated.get() ? null : group.getFilterExpr()), group.getQueryInfo());

		// other option (which might be faster for RDF4J native stores): join over the statements
		// TODO implement this and evaluate if it is faster ..

	}

}
