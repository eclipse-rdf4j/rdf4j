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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.BoundJoinTupleExpr;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTask;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for executing joins as bind joins (i.e., the bindings of a block are injected in the SPARQL query as
 * VALUES clause).
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
public abstract class ControlledWorkerBindJoinBase extends JoinExecutorBase<BindingSet> {

	private static final Logger log = LoggerFactory.getLogger(ControlledWorkerBindJoinBase.class);

	protected final ControlledWorkerScheduler<BindingSet> scheduler;

	protected final Phaser phaser = new Phaser(1);

	public ControlledWorkerBindJoinBase(ControlledWorkerScheduler<BindingSet> scheduler,
			FederationEvalStrategy strategy,
			CloseableIteration<BindingSet> leftIter,
			TupleExpr rightArg, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {
		super(strategy, leftIter, rightArg, bindings, queryInfo);
		this.scheduler = scheduler;
	}

	@Override
	protected void handleBindings() throws Exception {
		if (!(rightArg instanceof BoundJoinTupleExpr)) {
			String msg = "Right argument is not an applicable expression for bind joins. Was: "
					+ rightArg.getClass().getCanonicalName();
			log.debug(msg);
			throw new QueryEvaluationException(msg);
		}

		int nBindingsCfg = this.queryInfo.getFederationContext().getConfig().getBoundJoinBlockSize();
		int totalBindings = 0; // the total number of bindings
		TupleExpr expr = rightArg;

		TaskCreator taskCreator = null;
		Phaser currentPhaser = phaser;

		int nBindings;
		List<BindingSet> bindings;
		while (!isClosed() && leftIter.hasNext()) {

			// create a new phaser if there are more than 10000 parties
			// note: a phaser supports only up to 65535 registered parties
			if (currentPhaser.getRegisteredParties() >= 10000) {
				currentPhaser = new Phaser(currentPhaser);
			}

			// determine the bind join block size
			nBindings = getNextBindJoinSize(nBindingsCfg, totalBindings);

			bindings = new ArrayList<>(nBindings);

			int count = 0;
			while (!isClosed() && count < nBindings && leftIter.hasNext()) {
				var bs = leftIter.next();
				if (taskCreator == null) {
					taskCreator = determineTaskCreator(expr, bs);
				}
				bindings.add(bs);
				count++;
			}

			totalBindings += count;

			currentPhaser.register();
			scheduler.schedule(taskCreator.getTask(new PhaserHandlingParallelExecutor(this, currentPhaser), bindings));
		}

		leftIter.close();

		scheduler.informFinish(this);

		if (log.isDebugEnabled()) {
			log.debug("JoinStats: left iter of " + getDisplayId() + " had " + totalBindings + " results.");
		}

		phaser.awaitAdvanceInterruptibly(phaser.arrive(), queryInfo.getMaxRemainingTimeMS(), TimeUnit.MILLISECONDS);
	}

	@Override
	public void handleClose() throws QueryEvaluationException {
		try {
			super.handleClose();
		} finally {
			// signal the phaser to close (if currently being blocked)
			phaser.forceTermination();
		}
	}

	/**
	 * Return the {@link TaskCreator} for executing the bind join
	 *
	 * @param expr
	 * @param bs
	 * @return
	 */
	protected abstract TaskCreator determineTaskCreator(TupleExpr expr, BindingSet bs);

	/**
	 * Return the size of the next bind join block.
	 *
	 * @param configuredBindJoinSize the configured bind join size
	 * @param totalBindings          the current process bindings from the intermediate result set
	 * @return
	 */
	protected int getNextBindJoinSize(int configuredBindJoinSize, int totalBindings) {

		/*
		 * XXX idea:
		 *
		 * make nBindings dependent on the number of intermediate results of the left argument.
		 *
		 * If many intermediate results, increase the number of bindings. This will result in less remote SPARQL
		 * requests.
		 *
		 */

		return configuredBindJoinSize;
	}

	protected interface TaskCreator {
		ParallelTask<BindingSet> getTask(ParallelExecutor<BindingSet> control, List<BindingSet> bindings);
	}
}
