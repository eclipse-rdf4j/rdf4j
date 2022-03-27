/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.concurrent;

import java.util.concurrent.Future;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParallelTaskBase<T> implements ParallelTask<T> {

	private static final Logger _log = LoggerFactory.getLogger(ParallelExecutorBase.class);

	protected Future<?> scheduledFuture;
	private CloseableIteration<T, QueryEvaluationException> closableIter;
	private volatile boolean cancelled = false;

	@Override
	public void cancel() {
		cancelled = true;

		if (scheduledFuture != null) {
			if (scheduledFuture.isDone()) {
				_log.trace("Task is already done: " + toString());
			} else {
				_log.debug("Attempting to cancel task " + toString());
				boolean successfullyCanceled = scheduledFuture.cancel(true);
				if (!successfullyCanceled) {
					_log.debug("Task " + toString() + " could not be cancelled properly.");
				}
			}
		}
		if (closableIter != null) {
			closableIter.close();
		}
	}

	@Override
	public CloseableIteration<T, QueryEvaluationException> performTask() throws Exception {

		if (cancelled) {
			throw new QueryEvaluationException("Evaluation has been cancelled");
		}
		assert closableIter == null;
		closableIter = performTaskInternal();

		if (cancelled) {
			// proactively close when this task has been cancelled in the meantime
			closableIter.close();
		}

		return closableIter;
	}

	protected abstract CloseableIteration<T, QueryEvaluationException> performTaskInternal() throws Exception;

	public void setScheduledFuture(Future<?> future) {
		this.scheduledFuture = future;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (Query: " + getQueryInfo().getQueryID() + ")";
	}
}
