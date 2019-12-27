/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.evaluation;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.impl.QueueCursor;

import java.util.Set;

/**
 * Transform the condition into a filter and the right side into an {@link AlternativeCursor}, then evaluate as a
 * {@link ParallelJoinCursor}.
 *
 * @author James Leigh
 */
public class ParallelLeftJoinCursor extends LookAheadIteration<BindingSet, QueryEvaluationException>
		implements Runnable {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String LF_TAB = "\n\t";

	private final EvaluationStrategy strategy;

	private final LeftJoin join;

	/**
	 * The set of binding names that are "in scope" for the filter. The filter must not include bindings that are (only)
	 * included because of the depth-first evaluation strategy in the evaluation of the constraint.
	 */
	private final Set<String> scopeBindingNames;

	/*-----------*
	 * Variables *
	 *-----------*/

	private volatile Thread evaluationThread;

	private final CloseableIteration<BindingSet, QueryEvaluationException> leftIter;

	private volatile CloseableIteration<BindingSet, QueryEvaluationException> rightIter;

	/**
	 * @deprecated Use {@link AbstractCloseableIteration#isClosed()} instead.
	 */
	private volatile boolean closed;

	private final QueueCursor<CloseableIteration<BindingSet, QueryEvaluationException>> rightQueue = new QueueCursor<>(
			1024);

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ParallelLeftJoinCursor(EvaluationStrategy strategy, LeftJoin join, BindingSet bindings)
			throws QueryEvaluationException {
		super();
		this.strategy = strategy;
		this.join = join;
		this.scopeBindingNames = join.getBindingNames();
		this.leftIter = strategy.evaluate(join.getLeftArg(), bindings);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void run() {
		evaluationThread = Thread.currentThread();
		try {
			ValueExpr condition = join.getCondition();
			while (true) {
				synchronized (this) {
					if (!closed && !isClosed() && leftIter.hasNext()) {
						BindingSet leftBindings = leftIter.next();
						addToRightQueue(condition, leftBindings);
					} else {
						break;
					}
				}
			}
		} catch (RuntimeException e) {
			rightQueue.toss(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			evaluationThread = null; // NOPMD
			rightQueue.done();
		}
	}

	private void addToRightQueue(ValueExpr condition, BindingSet leftBindings)
			throws QueryEvaluationException, InterruptedException {
		CloseableIteration<BindingSet, QueryEvaluationException> result = strategy.evaluate(join.getRightArg(),
				leftBindings);
		if (condition != null) {
			result = new FilterCursor(result, condition, scopeBindingNames, strategy);
		}
		CloseableIteration<BindingSet, QueryEvaluationException> alt = new SingletonIteration<>(leftBindings);
		rightQueue.put(new AlternativeCursor<>(result, alt));
	}

	@Override
	public BindingSet getNextElement() throws QueryEvaluationException {
		BindingSet result = null;
		CloseableIteration<BindingSet, QueryEvaluationException> nextRightIter = rightIter;
		while (!isClosed() && (nextRightIter != null || rightQueue.hasNext())) {
			if (nextRightIter == null) {
				nextRightIter = rightIter = rightQueue.next();
			}
			if (nextRightIter != null) {
				if (nextRightIter.hasNext()) {
					result = nextRightIter.next();
					break;
				} else {
					nextRightIter.close();
					nextRightIter = rightIter = null; // NOPMD
				}
			}
		}

		return result;
	}

	@Override
	public synchronized void handleClose() throws QueryEvaluationException {
		closed = true;
		try {
			super.handleClose();
		} finally {
			try {
				Thread toCloseEvaluationThread = evaluationThread;
				if (toCloseEvaluationThread != null) {
					toCloseEvaluationThread.interrupt();
				}
			} finally {
				try {
					CloseableIteration<BindingSet, QueryEvaluationException> toCloseRightIter = rightIter;
					rightIter = null; // NOPMD
					if (toCloseRightIter != null) {
						toCloseRightIter.close();
					}
				} finally {
					leftIter.close();
				}
			}
		}
	}

	@Override
	public String toString() {
		String left = leftIter.toString().replace("\n", LF_TAB);
		CloseableIteration<BindingSet, QueryEvaluationException> nextRightIter = rightIter;
		String right = (null == nextRightIter) ? join.getRightArg().toString() : nextRightIter.toString();
		ValueExpr condition = join.getCondition();
		String filter = (null == condition) ? "" : condition.toString().trim().replace("\n", LF_TAB);
		return "ParallelLeftJoin " + filter + LF_TAB + left + LF_TAB + right.replace("\n", LF_TAB);
	}
}
