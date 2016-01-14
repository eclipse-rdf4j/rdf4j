/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.evaluation;

import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;

/**
 * Transform the condition into a filter and the right side into an
 * {@link AlternativeCursor}, then evaluate as a {@link ParallelJoinCursor}.
 * 
 * @author James Leigh
 */
public class ParallelLeftJoinCursor extends LookAheadIteration<BindingSet, QueryEvaluationException>
		implements Runnable
{

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String LF_TAB = "\n\t";

	private final EvaluationStrategy strategy;

	private final LeftJoin join;

	/**
	 * The set of binding names that are "in scope" for the filter. The filter
	 * must not include bindings that are (only) included because of the
	 * depth-first evaluation strategy in the evaluation of the constraint.
	 */
	private final Set<String> scopeBindingNames;

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

	public ParallelLeftJoinCursor(EvaluationStrategy strategy, LeftJoin join, BindingSet bindings)
		throws QueryEvaluationException
	{
		super();
		this.strategy = strategy;
		this.join = join;
		this.scopeBindingNames = join.getBindingNames();
		this.leftIter = strategy.evaluate(join.getLeftArg(), bindings);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public void run() {
		evaluationThread = Thread.currentThread();
		try {
			ValueExpr condition = join.getCondition();
			while (!closed && leftIter.hasNext()) {
				BindingSet leftBindings = leftIter.next();
				addToRightQueue(condition, leftBindings);
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

	private void addToRightQueue(ValueExpr condition, BindingSet leftBindings)
		throws QueryEvaluationException, InterruptedException
	{
		CloseableIteration<BindingSet, QueryEvaluationException> result = strategy.evaluate(join.getRightArg(),
				leftBindings);
		if (condition != null) {
			result = new FilterCursor(result, condition, scopeBindingNames, strategy);
		}
		CloseableIteration<BindingSet, QueryEvaluationException> alt = new SingletonIteration<BindingSet, QueryEvaluationException>(
				leftBindings);
		rightQueue.put(new AlternativeCursor<BindingSet>(result, alt));
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
		String left = leftIter.toString().replace("\n", LF_TAB);
		String right = (null == rightIter) ? join.getRightArg().toString() : rightIter.toString();
		ValueExpr condition = join.getCondition();
		String filter = (null == condition) ? "" : condition.toString().trim().replace("\n", LF_TAB);
		return "ParallelLeftJoin " + filter + LF_TAB + left + LF_TAB + right.replace("\n", LF_TAB);
	}
}
