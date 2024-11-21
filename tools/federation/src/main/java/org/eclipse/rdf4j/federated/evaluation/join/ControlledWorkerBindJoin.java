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
package org.eclipse.rdf4j.federated.evaluation.join;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.CheckStatementPattern;
import org.eclipse.rdf4j.federated.algebra.FedXService;
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
 * Execution of a regular join as bind join.
 *
 * @author Andreas Schwarte
 * @see ControlledWorkerBindJoinBase
 */
public class ControlledWorkerBindJoin extends ControlledWorkerBindJoinBase {

	public ControlledWorkerBindJoin(ControlledWorkerScheduler<BindingSet> scheduler, FederationEvalStrategy strategy,
			CloseableIteration<BindingSet> leftIter,
			TupleExpr rightArg, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {
		super(scheduler, strategy, leftIter, rightArg, bindings, queryInfo);
	}

	@Override
	protected TaskCreator determineTaskCreator(TupleExpr expr, BindingSet bs) {
		final TaskCreator taskCreator;
		if (expr instanceof StatementTupleExpr) {
			StatementTupleExpr stmt = (StatementTupleExpr) expr;
			taskCreator = new BoundJoinTaskCreator(strategy, stmt);
		} else if (expr instanceof FedXService) {
			taskCreator = new FedXServiceJoinTaskCreator(strategy, (FedXService) expr);
		} else {
			throw new RuntimeException("Expr is of unexpected type: " + expr.getClass().getCanonicalName()
					+ ". Please report this problem.");
		}
		return taskCreator;
	}

	protected class BoundJoinTaskCreator implements TaskCreator {
		protected final FederationEvalStrategy _strategy;
		protected final StatementTupleExpr _expr;

		public BoundJoinTaskCreator(
				FederationEvalStrategy strategy, StatementTupleExpr expr) {
			super();
			_strategy = strategy;
			_expr = expr;
		}

		@Override
		public ParallelTask<BindingSet> getTask(ParallelExecutor<BindingSet> control, List<BindingSet> bindings) {
			return new ParallelBoundJoinTask(control, _strategy, _expr, bindings);
		}
	}

	@Deprecated(forRemoval = true)
	protected class CheckJoinTaskCreator implements TaskCreator {
		protected final FederationEvalStrategy _strategy;
		protected final CheckStatementPattern _expr;

		public CheckJoinTaskCreator(
				FederationEvalStrategy strategy, CheckStatementPattern expr) {
			super();
			_strategy = strategy;
			_expr = expr;
		}

		@Override
		public ParallelTask<BindingSet> getTask(ParallelExecutor<BindingSet> control, List<BindingSet> bindings) {
			return new ParallelCheckJoinTask(control, _strategy, _expr, bindings);
		}
	}

	protected class FedXServiceJoinTaskCreator implements TaskCreator {
		protected final FederationEvalStrategy _strategy;
		protected final FedXService _expr;

		public FedXServiceJoinTaskCreator(
				FederationEvalStrategy strategy, FedXService expr) {
			super();
			_strategy = strategy;
			_expr = expr;
		}

		@Override
		public ParallelTask<BindingSet> getTask(ParallelExecutor<BindingSet> control, List<BindingSet> bindings) {
			return new ParallelServiceJoinTask(control, _strategy, _expr, bindings);
		}
	}

}
