/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.federation;

import java.lang.ref.WeakReference;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.QueueCursor;

/**
 * Base class for any join parallel join executor. Note that this class extends {@link LookAheadIteration} and thus any
 * implementation of this class is applicable for pipelining when used in a different thread (access to shared variables
 * is synchronized).
 *
 * @author Andreas Schwarte
 */
public abstract class JoinExecutorBase<T> extends LookAheadIteration<T, QueryEvaluationException> {

	/**
	 * @deprecated No replacement, don't use static shared int variables.
	 */
	protected static int NEXT_JOIN_ID = 1;

	/* Constants */
	protected final TupleExpr rightArg; // the right argument for the join

	protected final BindingSet bindings; // the bindings

	protected final CloseableIteration<T, QueryEvaluationException> leftIter;

	protected volatile CloseableIteration<T, QueryEvaluationException> rightIter;

	/**
	 * @deprecated Use {@link AbstractCloseableIteration#isClosed()} instead.
	 */
	protected volatile boolean closed = false;

	/**
	 * @deprecated Use {@link #isFinished()} instead.
	 */
	protected volatile boolean finished = false;

	protected final QueueCursor<CloseableIteration<T, QueryEvaluationException>> rightQueue = new QueueCursor<>(1024,
			new WeakReference<>(this));

	protected JoinExecutorBase(CloseableIteration<T, QueryEvaluationException> leftIter, TupleExpr rightArg,
			BindingSet bindings) throws QueryEvaluationException {
		this.leftIter = leftIter;
		this.rightArg = rightArg;
		this.bindings = bindings;
	}

	public final void run() {

		try {
			handleBindings();
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			toss(e);
		} finally {
			finished = true;
			rightQueue.done();
		}

	}

	/**
	 * Implementations must implement this method to handle bindings. Use the following as a template <code>
	 * while (!closed && leftIter.hasNext()) {
	 * 		// your code
	 * }
	 * </code> and add results to rightQueue. Note that addResult() is implemented synchronized and thus thread safe. In
	 * case you can guarantee sequential access, it is also possible to directly access rightQueue
	 */
	protected abstract void handleBindings() throws Exception;

	public void addResult(CloseableIteration<T, QueryEvaluationException> res) {
		/* optimization: avoid adding empty results */
		if (res instanceof EmptyIteration<?, ?>) {
			return;
		}

		try {
			rightQueue.put(res);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Error adding element to right queue", e);
		}
	}

	public void done() {
		// no-op
	}

	public void toss(Exception e) {
		rightQueue.toss(e);
	}

	@Override
	public T getNextElement() throws QueryEvaluationException {
		// TODO check if we need to protect rightQueue from synchronized access
		// wasn't done in the original implementation either
		// if we see any weird behavior check here !!

		while (rightIter != null || rightQueue.hasNext()) {
			CloseableIteration<T, QueryEvaluationException> nextRightIter = rightIter;
			if (nextRightIter == null) {
				nextRightIter = rightIter = rightQueue.next();
			}
			if (nextRightIter != null) {
				if (nextRightIter.hasNext()) {
					return nextRightIter.next();
				} else {
					rightIter = null;
					nextRightIter.close();
				}
			}
		}

		return null;
	}

	@Override
	public void handleClose() throws QueryEvaluationException {
		closed = true;
		try {
			super.handleClose();
		} finally {
			try {
				rightQueue.close();
			} finally {
				try {
					CloseableIteration<T, QueryEvaluationException> toCloseRightIter = rightIter;
					rightIter = null;
					if (toCloseRightIter != null) {
						toCloseRightIter.close();
					}
				} finally {
					CloseableIteration<T, QueryEvaluationException> toCloseLeftIter = leftIter;
					if (toCloseLeftIter != null) {
						toCloseLeftIter.close();
					}
				}
			}
		}
	}

	/**
	 * Gets whether this executor is finished or aborted.
	 *
	 * @return true if this executor is finished or aborted
	 */
	public boolean isFinished() {
		return finished;
	}

}
