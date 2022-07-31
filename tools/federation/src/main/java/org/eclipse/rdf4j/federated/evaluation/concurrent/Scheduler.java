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
package org.eclipse.rdf4j.federated.evaluation.concurrent;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Interface for any scheduler.
 *
 * @author Andreas Schwarte
 *
 * @see ControlledWorkerScheduler
 */
public interface Scheduler<T> {

	/**
	 * Schedule the provided task.
	 *
	 * @param task
	 */
	void schedule(ParallelTask<T> task);

	/**
	 * Callback to handle the result.
	 *
	 * @param res
	 */
	void handleResult(CloseableIteration<T, QueryEvaluationException> res);

	/**
	 * Inform the scheduler that a certain task is done.
	 *
	 */
	void done();

	/**
	 * Toss an exception to the scheduler.
	 *
	 * @param e
	 */
	void toss(Exception e);

	/**
	 * Abort the execution of running and queued tasks.
	 *
	 */
	void abort();

	void shutdown();

	/**
	 * Inform the scheduler that no more tasks will be scheduled.
	 */
	void informFinish();

	/**
	 * Determine if the scheduler has unfinished tasks.
	 *
	 * @return whether the scheduler is running
	 */
	boolean isRunning();

}
