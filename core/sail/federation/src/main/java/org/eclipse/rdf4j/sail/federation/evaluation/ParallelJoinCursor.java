/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;

/**
 * Iterate the left side and evaluate the right side in separate thread, only
 * iterate the right side in the controlling thread.
 * 
 * @author James Leigh
 */
public class ParallelJoinCursor extends LookAheadIteration<BindingSet, QueryEvaluationException> implements
		Runnable
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

	private CloseableIteration<BindingSet, QueryEvaluationException> rightIter;

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

	public void run() {
		evaluationThread = Thread.currentThread();
		try {
			while (!closed && leftIter.hasNext()) {
				rightQueue.put(strategy.evaluate(rightArg, leftIter.next()));
			}
		}
		catch (RuntimeException e) {
			rightQueue.toss(e);
		}
		catch (InterruptedException e) {
			// stop
		}
		finally {
			evaluationThread = null; // NOPMD
			rightQueue.done();
		}
	}

	@Override
	public synchronized BindingSet getNextElement()
		throws QueryEvaluationException
	{
		BindingSet result = null;
		while (rightIter != null || rightQueue.hasNext()) {
			if (rightIter == null) {
				rightIter = rightQueue.next();
			}
			if (rightIter.hasNext()) {
				result = rightIter.next();
				break;
			}
			else {
				rightIter.close();
				rightIter = null; // NOPMD
			}
		}

		return result;
	}

	@Override
	public synchronized void handleClose()
		throws QueryEvaluationException
	{
		closed = true;
		if (evaluationThread != null) {
			evaluationThread.interrupt();
		}
		if (rightIter != null) {
			rightIter.close();
			rightIter = null; // NOPMD
		}

		leftIter.close();
	}

	@Override
	public String toString() {
		String left = leftIter.toString().replace("\n", "\n\t");
		String right = (null == rightIter) ? rightArg.toString() : rightIter.toString();
		return "ParallelJoin\n\t" + left + "\n\t" + right.replace("\n", "\n\t");
	}
}
