/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.algebra.BoundJoinTupleExpr;
import org.eclipse.rdf4j.federated.algebra.CheckStatementPattern;
import org.eclipse.rdf4j.federated.algebra.FedXService;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTask;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute the nested loop join in an asynchronous fashion, using grouped requests, i.e. group bindings into one SPARQL
 * request using the UNION operator.
 * 
 * The number of concurrent threads is controlled by a {@link ControlledWorkerScheduler} which works according to the
 * FIFO principle and uses worker threads.
 * 
 * This join cursor blocks until all scheduled tasks are finished, however the result iteration can be accessed from
 * different threads to allow for pipelining.
 * 
 * @author Andreas Schwarte
 * 
 */
public class ControlledWorkerBoundJoin extends ControlledWorkerJoin {

	private static final Logger log = LoggerFactory.getLogger(ControlledWorkerBoundJoin.class);

	public ControlledWorkerBoundJoin(ControlledWorkerScheduler<BindingSet> scheduler, FederationEvalStrategy strategy,
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter,
			TupleExpr rightArg, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {
		super(scheduler, strategy, leftIter, rightArg, bindings, queryInfo);
	}

	@Override
	protected void handleBindings() throws Exception {
		if (!(canApplyVectoredEvaluation(rightArg))) {
			log.debug(
					"Right argument is not an applicable BoundJoinTupleExpr. Fallback on ControlledWorkerJoin implementation: "
							+ rightArg.getClass().getCanonicalName());
			super.handleBindings(); // fallback
			return;
		}

		int nBindingsCfg = this.queryInfo.getFederationContext().getConfig().getBoundJoinBlockSize();
		int totalBindings = 0; // the total number of bindings
		TupleExpr expr = rightArg;

		TaskCreator taskCreator = null;

		// first item is always sent in a non-bound way
		if (!closed && leftIter.hasNext()) {
			BindingSet b = leftIter.next();
			totalBindings++;
			if (expr instanceof StatementTupleExpr) {
				StatementTupleExpr stmt = (StatementTupleExpr) expr;
				if (stmt.hasFreeVarsFor(b)) {
					taskCreator = new BoundJoinTaskCreator(this, strategy, stmt);
				} else {
					expr = new CheckStatementPattern(stmt, queryInfo);
					taskCreator = new CheckJoinTaskCreator(this, strategy, (CheckStatementPattern) expr);
				}
			} else if (expr instanceof FedXService) {
				taskCreator = new FedXServiceJoinTaskCreator(this, strategy, (FedXService) expr);
			} else {
				throw new RuntimeException("Expr is of unexpected type: " + expr.getClass().getCanonicalName()
						+ ". Please report this problem.");
			}
			phaser.register();
			scheduler.schedule(new ParallelJoinTask(this, strategy, expr, b));
		}

		int nBindings;
		List<BindingSet> bindings = null;
		while (!closed && leftIter.hasNext()) {

			/*
			 * XXX idea:
			 * 
			 * make nBindings dependent on the number of intermediate results of the left argument.
			 * 
			 * If many intermediate results, increase the number of bindings. This will result in less remote SPARQL
			 * requests.
			 * 
			 */

			if (totalBindings > 10)
				nBindings = nBindingsCfg;
			else
				nBindings = 3;

			bindings = new ArrayList<>(nBindings);

			int count = 0;
			while (count < nBindings && leftIter.hasNext()) {
				bindings.add(leftIter.next());
				count++;
			}

			totalBindings += count;

			phaser.register();
			scheduler.schedule(taskCreator.getTask(bindings));
		}

		scheduler.informFinish(this);

		if (log.isDebugEnabled()) {
			log.debug("JoinStats: left iter of " + getDisplayId() + " had " + totalBindings + " results.");
		}

		phaser.awaitAdvanceInterruptibly(phaser.arrive(), queryInfo.getMaxRemainingTimeMS(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Returns true if the vectored evaluation can be applied for the join argument, i.e. there is no fallback to
	 * {@link ControlledWorkerJoin#handleBindings()}. This is
	 * 
	 * a) if the expr is a {@link BoundJoinTupleExpr} (Mind the special handling for {@link FedXService} as defined in
	 * b) b) if the expr is a {@link FedXService} and {@link FedXConfig#getEnableServiceAsBoundJoin()}
	 * 
	 * @return
	 */
	private boolean canApplyVectoredEvaluation(TupleExpr expr) {
		if (expr instanceof BoundJoinTupleExpr) {
			if (expr instanceof FedXService)
				return this.queryInfo.getFederationContext().getConfig().getEnableServiceAsBoundJoin();
			return true;
		}
		return false;
	}

	protected interface TaskCreator {
		public ParallelTask<BindingSet> getTask(List<BindingSet> bindings);
	}

	protected class BoundJoinTaskCreator implements TaskCreator {
		protected final ControlledWorkerBoundJoin _control;
		protected final FederationEvalStrategy _strategy;
		protected final StatementTupleExpr _expr;

		public BoundJoinTaskCreator(ControlledWorkerBoundJoin control,
				FederationEvalStrategy strategy, StatementTupleExpr expr) {
			super();
			_control = control;
			_strategy = strategy;
			_expr = expr;
		}

		@Override
		public ParallelTask<BindingSet> getTask(List<BindingSet> bindings) {
			return new ParallelBoundJoinTask(_control, _strategy, _expr, bindings);
		}
	}

	protected class CheckJoinTaskCreator implements TaskCreator {
		protected final ControlledWorkerBoundJoin _control;
		protected final FederationEvalStrategy _strategy;
		protected final CheckStatementPattern _expr;

		public CheckJoinTaskCreator(ControlledWorkerBoundJoin control,
				FederationEvalStrategy strategy, CheckStatementPattern expr) {
			super();
			_control = control;
			_strategy = strategy;
			_expr = expr;
		}

		@Override
		public ParallelTask<BindingSet> getTask(List<BindingSet> bindings) {
			return new ParallelCheckJoinTask(_control, _strategy, _expr, bindings);
		}
	}

	protected class FedXServiceJoinTaskCreator implements TaskCreator {
		protected final ControlledWorkerBoundJoin _control;
		protected final FederationEvalStrategy _strategy;
		protected final FedXService _expr;

		public FedXServiceJoinTaskCreator(ControlledWorkerBoundJoin control,
				FederationEvalStrategy strategy, FedXService expr) {
			super();
			_control = control;
			_strategy = strategy;
			_expr = expr;
		}

		@Override
		public ParallelTask<BindingSet> getTask(List<BindingSet> bindings) {
			return new ParallelServiceJoinTask(_control, _strategy, _expr, bindings);
		}
	}

}
