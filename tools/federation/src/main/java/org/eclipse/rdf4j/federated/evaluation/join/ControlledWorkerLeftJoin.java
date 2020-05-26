/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute the nested loop join in an asynchronous fashion, i.e. one binding after the other (but concurrently)
 *
 * The number of concurrent threads is controlled by a {@link ControlledWorkerScheduler} which works according to the
 * FIFO principle.
 *
 * This join cursor blocks until all scheduled tasks are finished, however the result iteration can be accessed from
 * different threads to allow for pipelining.
 *
 * @author Andreas Schwarte
 */
public class ControlledWorkerLeftJoin extends JoinExecutorBase<BindingSet> {

	private static final Logger log = LoggerFactory.getLogger(ControlledWorkerLeftJoin.class);

	protected final ControlledWorkerScheduler<BindingSet> scheduler;

	protected final Phaser phaser = new Phaser(1);

	protected final LeftJoin join;

	public ControlledWorkerLeftJoin(ControlledWorkerScheduler<BindingSet> scheduler, FederationEvalStrategy strategy,
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter,
			LeftJoin join, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {
		super(strategy, leftIter, join.getRightArg(), bindings, queryInfo);
		this.scheduler = scheduler;
		this.join = join;
	}

	@Override
	protected void handleBindings() throws Exception {

		int totalBindings = 0; // the total number of bindings

		while (!closed && leftIter.hasNext()) {
			ParallelLeftJoinTask task = new ParallelLeftJoinTask(this, strategy, join, leftIter.next());
			totalBindings++;
			phaser.register();
			scheduler.schedule(task);
		}

		scheduler.informFinish(this);

		if (log.isDebugEnabled()) {
			log.debug("JoinStats: left iter of " + getDisplayId() + " had " + totalBindings + " results.");
		}

		// wait until all tasks are executed
		phaser.awaitAdvanceInterruptibly(phaser.arrive(), queryInfo.getMaxRemainingTimeMS(), TimeUnit.MILLISECONDS);

	}

	@Override
	public void done() {
		phaser.arriveAndDeregister();
		super.done();
	}

	@Override
	public void toss(Exception e) {
		phaser.arriveAndDeregister();
		super.toss(e);
	}
}
