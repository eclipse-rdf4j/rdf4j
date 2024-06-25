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

import java.util.concurrent.Future;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParallelTaskBase<T> implements ParallelTask<T> {

	private static final Logger logger = LoggerFactory.getLogger(ParallelExecutorBase.class);

	protected Future<?> scheduledFuture;
	private CloseableIteration<T> closableIter;
	private volatile boolean cancelled = false;
	private volatile boolean closed = false;

	@Override
	public void cancel() {
		cancelled = true;
		close();
	}

	@Override
	public CloseableIteration<T> performTask() throws Exception {
		if (closed) {
			return new EmptyIteration<>();
		}

		if (cancelled) {
			throw new QueryEvaluationException("Evaluation has been cancelled");
		}
		try {
			closableIter = performTaskInternal();
		} catch (Throwable t) {

			if (Thread.interrupted() || t instanceof InterruptedException) {
				Thread.currentThread().interrupt();
				if (closed) {
					logger.trace(
							"Exception was thrown while performing task, but it was ignored because the task was closed.",
							t);
					return new EmptyIteration<>();
				} else if (cancelled) {
					throw new QueryEvaluationException("Evaluation has been cancelled", t);
				} else {
					throw new QueryEvaluationException("Evaluation has been interrupted", t);
				}
			} else if (closed) {
				assert Thread.currentThread().isInterrupted()
						: "Exception was thrown and task was closed, but the current thread is not interrupted which means that the exception was either something bad or some code forgot to re-interrupt the current thread: "
								+ t;
				logger.trace(
						"Exception was thrown while performing task, but it was ignored because the task was closed.",
						t);
				return new EmptyIteration<>();
			}

			assert !cancelled && !closed;

			throw t;
		}

		if (cancelled || closed) {
			// proactively close when this task has been cancelled in the meantime
			closableIter.close();
		}

		return closableIter;
	}

	protected abstract CloseableIteration<T> performTaskInternal() throws Exception;

	public void setScheduledFuture(Future<?> future) {
		this.scheduledFuture = future;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (Query: " + getQueryInfo().getQueryID() + ")";
	}

	@Override
	public void close() {
		if (!closed) {
			closed = true;
			try {
				Future<?> scheduledFuture = this.scheduledFuture;
				this.scheduledFuture = null;

				if (scheduledFuture != null) {
					if (scheduledFuture.isDone()) {
						logger.trace("Task is already done: {}", this);
					} else {
						logger.debug("Attempting to cancel task {}", this);
						boolean successfullyCanceled = scheduledFuture.cancel(true);
						if (!successfullyCanceled) {
							logger.debug("Task {} could not be cancelled properly. Maybe it has already completed.",
									this);
						}

						int timeout = 100;
						for (int i = 0; i < timeout && !scheduledFuture.isDone(); i++) {
							try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								break;
							}
						}

						if (!scheduledFuture.isDone()) {
							logger.error("Timeout while waiting for task {} to terminate after it was cancelled.",
									this);
						}
					}
				}

			} finally {
				if (closableIter != null) {
					closableIter.close();
				}
			}

		}

	}
}
