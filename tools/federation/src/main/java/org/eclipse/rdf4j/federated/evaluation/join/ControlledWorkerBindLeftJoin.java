/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.EmptyStatementPattern;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTask;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Bind join implementation for left joins (i.e., OPTIOAL clauses)
 *
 * @author Andreas Schwarte
 */
public class ControlledWorkerBindLeftJoin extends ControlledWorkerBindJoinBase {

	public ControlledWorkerBindLeftJoin(ControlledWorkerScheduler<BindingSet> scheduler,
			FederationEvalStrategy strategy, CloseableIteration<BindingSet> leftIter, TupleExpr rightArg,
			BindingSet bindings, QueryInfo queryInfo) throws QueryEvaluationException {
		super(scheduler, strategy, leftIter, rightArg, bindings, queryInfo);
	}

	@Override
	protected TaskCreator determineTaskCreator(TupleExpr expr, BindingSet bs) {
		final TaskCreator taskCreator;
		if (expr instanceof StatementTupleExpr) {
			StatementTupleExpr stmt = (StatementTupleExpr) expr;
			taskCreator = new LeftBoundJoinTaskCreator(strategy, stmt);
		} else if (expr instanceof EmptyStatementPattern) {
			EmptyStatementPattern stmt = (EmptyStatementPattern) expr;
			taskCreator = new EmptyLeftBoundJoinTaskCreator(strategy, stmt);
		} else {
			throw new RuntimeException("Expr is of unexpected type: " + expr.getClass().getCanonicalName()
					+ ". Please report this problem.");
		}
		return taskCreator;
	}

	static protected class LeftBoundJoinTaskCreator implements TaskCreator {
		protected final FederationEvalStrategy _strategy;
		protected final StatementTupleExpr _expr;

		public LeftBoundJoinTaskCreator(
				FederationEvalStrategy strategy, StatementTupleExpr expr) {
			super();
			_strategy = strategy;
			_expr = expr;
		}

		@Override
		public ParallelTask<BindingSet> getTask(ParallelExecutor<BindingSet> control, List<BindingSet> bindings) {
			return new ParallelBindLeftJoinTask(control, _strategy, _expr, bindings);
		}
	}

	static protected class EmptyLeftBoundJoinTaskCreator implements TaskCreator {
		protected final FederationEvalStrategy _strategy;
		protected final EmptyStatementPattern _expr;

		public EmptyLeftBoundJoinTaskCreator(
				FederationEvalStrategy strategy, EmptyStatementPattern expr) {
			super();
			_strategy = strategy;
			_expr = expr;
		}

		@Override
		public ParallelTask<BindingSet> getTask(ParallelExecutor<BindingSet> control, List<BindingSet> bindings) {
			return new ParallelEmptyBindLeftJoinTask(control, _strategy, _expr, bindings);
		}
	}
}
