/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;

/**
 * Iterate the left side and evaluate the right side in separate thread, only iterate the right side in the
 * controlling thread.
 * 
 * @author James Leigh
 */
public class ParallelJoinCursor extends LookAheadIteration<BindingSet, QueryEvaluationException>
		implements Runnable
{

	/*-----------*
	 * Constants *
	 *-----------*/

	private final EvaluationStrategy strategy;

	private final TupleExpr rightArg;

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

	private final QueueCursor<CloseableIteration<BindingSet, QueryEvaluationException>> rightQueue = new QueueCursor<CloseableIteration<BindingSet, QueryEvaluationException>>(
			1024);

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ParallelJoinCursor(EvaluationStrategy strategy,
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter, TupleExpr rightArg)
		throws QueryEvaluationException
	{
		super();
		this.strategy = strategy;
		this.leftIter = leftIter;
		this.rightArg = rightArg;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void run() {
		evaluationThread = Thread.currentThread();
		try {
			while (!isClosed() && leftIter.hasNext()) {
				CloseableIteration<BindingSet, QueryEvaluationException> evaluate = strategy.evaluate(
						rightArg, leftIter.next());
				try {
					rightQueue.put(evaluate);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					try {
						// Close the iteration we were attempting to put onto the queue
						// so we don't rely on the automatic closure method
						evaluate.close();
						break;
					}
					finally {
						close();
					}
				}
			}
		}
		catch (RuntimeException e) {
			rightQueue.toss(e);
		}
		finally {
			evaluationThread = null;
			rightQueue.done();
		}
	}

	@Override
	public BindingSet getNextElement()
		throws QueryEvaluationException
	{
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
				}
				else {
					nextRightIter.close();
					nextRightIter = rightIter = null; // NOPMD
				}
			}
		}

		return result;
	}

	@Override
	public void handleClose()
		throws QueryEvaluationException
	{
		closed = true;
		try {
			super.handleClose();
		}
		finally {
			try {
				Thread toCloseEvaluationThread = evaluationThread;
				if (toCloseEvaluationThread != null) {
					toCloseEvaluationThread.interrupt();
				}
			}
			finally {
				try {
					CloseableIteration<BindingSet, QueryEvaluationException> toCloseRightIter = rightIter;
					rightIter = null; // NOPMD
					if (toCloseRightIter != null) {
						toCloseRightIter.close();
					}
				}
				finally {
					try {
						leftIter.close();
					}
					finally {
						rightQueue.close();
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		String left = leftIter.toString().replace("\n", "\n\t");
		CloseableIteration<BindingSet, QueryEvaluationException> nextRightIter = rightIter;
		String right = (null == nextRightIter) ? rightArg.toString() : nextRightIter.toString();
		return "ParallelJoin\n\t" + left + "\n\t" + right.replace("\n", "\n\t");
	}
}
