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

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.join.JoinExecutorBase;
import org.eclipse.rdf4j.federated.evaluation.union.UnionExecutorBase;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for common parallel executors such as {@link JoinExecutorBase} and {@link UnionExecutorBase}.
 *
 * @param <T>
 * @author Andreas Schwarte
 * @see JoinExecutorBase
 * @see UnionExecutorBase
 */
public abstract class ParallelExecutorBase<T> extends LookAheadIteration<T, QueryEvaluationException>
		implements ParallelExecutor<T> {

	protected static final Logger log = LoggerFactory.getLogger(ParallelExecutorBase.class);

	protected static final AtomicLong NEXT_EXECUTOR_ID = new AtomicLong(0L);

	/* Constants */
	protected final FederationEvalStrategy strategy; // the evaluation strategy
	protected final long executorId; // the executor id
	protected final QueryInfo queryInfo;

	/* Variables */
	protected volatile Thread evaluationThread;
	protected FedXQueueCursor<T> rightQueue = FedXQueueCursor.create(1024);
	protected volatile CloseableIteration<T, QueryEvaluationException> rightIter;
	protected volatile boolean finished = false;

	public ParallelExecutorBase(QueryInfo queryInfo) throws QueryEvaluationException {
		this.strategy = queryInfo.getStrategy();
		this.executorId = NEXT_EXECUTOR_ID.incrementAndGet();
		this.queryInfo = queryInfo;
	}

	@Override
	public final void run() {

		if (isClosed()) {
			return;
		}

		evaluationThread = Thread.currentThread();
		if (evaluationThread.isInterrupted()) {
			return;
		}

		if (log.isTraceEnabled()) {
			log.trace("Performing execution of " + getDisplayId() + ", thread: " + evaluationThread.getName());
		}
		try {
			performExecution();
			checkTimeout();

			if (log.isTraceEnabled()) {
				log.trace(getDisplayId() + " is finished.");
			}
			done();

		} catch (InterruptedException e) {
			toss(ExceptionUtil.toException(e));
			evaluationThread.interrupt();
		} catch (Throwable t) {
			toss(ExceptionUtil.toException(t));
		} finally {
			// signal DONE to queue: unblock polling
			rightQueue.done();
			finished = true;
			evaluationThread = null;
		}
	}

	/**
	 * Perform the parallel execution.
	 * <p>
	 * Note that this method must block until the execution is completed.
	 *
	 * @throws Exception
	 */
	protected abstract void performExecution() throws Exception;

	@Override
	public void addResult(CloseableIteration<T, QueryEvaluationException> res) {
		/* optimization: avoid adding empty results */
		if (res instanceof EmptyIteration<?, ?>) {
			return;
		}
		if (isClosed() || rightQueue.isClosed()) {
			res.close();
			return;
		}

		try {
			rightQueue.put(res);

			if (isClosed() || rightQueue.isClosed()) {
				res.close();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			res.close();
			throw new RuntimeException("Error adding element to right queue", e);
		}
	}

	@Override
	public void done() {
		// no-op
	}

	@Override
	public void toss(Exception e) {
		rightQueue.toss(e);
		if (log.isTraceEnabled()) {
			log.trace("Tossing exception of " + getDisplayId() + ": " + e.getMessage());
		}
	}

	@Override
	public T getNextElement() throws QueryEvaluationException {
		// TODO check if we need to protect rightQueue from synchronized access
		// wasn't done in the original implementation either
		// if we see any weird behavior check here !!

		while (rightIter != null || rightQueue.hasNext()) {
			if (rightIter == null) {
				rightIter = rightQueue.next();
			}
			if (rightIter.hasNext()) {
				return rightIter.next();
			} else {
				rightIter.close();
				rightIter = null;
			}
		}

		rightQueue.checkException();
		return null;
	}

	/**
	 * Checks whether the query execution has run into a timeout. If so, a {@link QueryInterruptedException} is thrown.
	 *
	 * @throws QueryInterruptedException
	 */
	protected void checkTimeout() throws QueryInterruptedException {
		long maxTimeLeft = queryInfo.getMaxRemainingTimeMS();
		if (maxTimeLeft <= 0) {
			throw new QueryInterruptedException("Query evaluation has run into a timeout");
		}
	}

	@Override
	public void handleClose() throws QueryEvaluationException {
		try {
			try {
				rightQueue.close();
			} finally {
				if (rightIter != null) {
					try {
						rightIter.close();
						rightIter = null;
					} catch (Throwable ignore) {
						if (ignore instanceof InterruptedException) {
							Thread.currentThread().interrupt();
						}
						log.trace("Failed to send interrupt signal:", ignore);
					}
				}
			}
		} finally {
			super.handleClose();
		}

	}

	/**
	 * Return true if this executor is finished or aborted
	 *
	 * @return whether the executor is finished
	 */
	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public QueryInfo getQueryInfo() {
		return queryInfo;
	}

	/**
	 * @return a unique identifier of this execution
	 */
	protected String getId() {
		return "#" + executorId + " (Query: " + queryInfo.getQueryID() + ")";
	}

	public String getDisplayId() {
		return getExecutorType() + " " + getId();
	}

	/**
	 * @return the executor type, e.g. "Join". Default "Executor"
	 */
	protected String getExecutorType() {
		return "Executor";
	}

	@Override
	public String toString() {
		return getExecutorType() + " " + getClass().getSimpleName() + " {id: " + getId() + "}";
	}
}
