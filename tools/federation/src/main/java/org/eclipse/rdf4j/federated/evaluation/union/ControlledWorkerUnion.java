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
package org.eclipse.rdf4j.federated.evaluation.union;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Execution of union tasks with {@link ControlledWorkerScheduler}. Tasks can be added using the provided functions.
 * Note that the union operation is to be executed with the {@link #run()} method (also threaded execution is possible).
 * Results are then contained in this iteration.
 *
 * @author Andreas Schwarte
 */
public class ControlledWorkerUnion<T> extends WorkerUnionBase<T> {

	public static int waitingCount = 0;
	public static int awakeCount = 0;

	protected final ControlledWorkerScheduler<T> scheduler;

	protected final Phaser phaser = new Phaser(1);

	public ControlledWorkerUnion(ControlledWorkerScheduler<T> scheduler,
			QueryInfo queryInfo) {
		super(queryInfo);
		this.scheduler = scheduler;
	}

	@Override
	protected void union() throws Exception {

		// schedule all tasks and inform about finish
		phaser.bulkRegister(tasks.size());
		scheduler.scheduleAll(tasks, this);

		// wait until all tasks are executed
		phaser.awaitAdvanceInterruptibly(phaser.arrive(), queryInfo.getMaxRemainingTimeMS(), TimeUnit.MILLISECONDS);
	}

	@Override
	public void done() {
		super.done();
		phaser.arriveAndDeregister();
	}

	@Override
	public void toss(Exception e) {
		super.toss(e);
		phaser.arriveAndDeregister();
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

}
