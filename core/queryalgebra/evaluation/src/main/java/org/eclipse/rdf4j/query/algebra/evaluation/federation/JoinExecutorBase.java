/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.http.client.QueueCursor;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;


/**
 * Base class for any join parallel join executor. Note that this class extends
 * {@link LookAheadIteration} and thus any implementation of this class is
 * applicable for pipelining when used in a different thread (access to shared
 * variables is synchronized).
 * 
 * @author Andreas Schwarte
 */
public abstract class JoinExecutorBase<T> extends LookAheadIteration<T, QueryEvaluationException> {

	protected static int NEXT_JOIN_ID = 1;

	/* Constants */
	protected final TupleExpr rightArg; // the right argument for the join

	protected final BindingSet bindings; // the bindings

	protected CloseableIteration<T, QueryEvaluationException> leftIter;

	protected CloseableIteration<T, QueryEvaluationException> rightIter;

	protected volatile boolean closed;

	protected boolean finished = false;

	protected final QueueCursor<CloseableIteration<T, QueryEvaluationException>> rightQueue = new QueueCursor<CloseableIteration<T, QueryEvaluationException>>(
			1024);

	public JoinExecutorBase(CloseableIteration<T, QueryEvaluationException> leftIter, TupleExpr rightArg,
			BindingSet bindings)
		throws QueryEvaluationException
	{
		this.leftIter = leftIter;
		this.rightArg = rightArg;
		this.bindings = bindings;
	}

	public final void run() {

		try {
			handleBindings();
		}
		catch (Exception e) {
			toss(e);
		}
		finally {
			finished = true;
			rightQueue.done();
		}

	}

	/**
	 * Implementations must implement this method to handle bindings. Use the
	 * following as a template <code>
	 * while (!closed && leftIter.hasNext()) {
	 * 		// your code
	 * }
	 * </code> and add results to rightQueue. Note that addResult() is
	 * implemented synchronized and thus thread safe. In case you can guarantee
	 * sequential access, it is also possible to directly access rightQueue
	 */
	protected abstract void handleBindings()
		throws Exception;

	public void addResult(CloseableIteration<T, QueryEvaluationException> res) {
		/* optimization: avoid adding empty results */
		if (res instanceof EmptyIteration<?, ?>)
			return;

		try {
			rightQueue.put(res);
		}
		catch (InterruptedException e) {
			throw new RuntimeException("Error adding element to right queue", e);
		}
	}

	public void done() {
		; // no-op
	}

	public void toss(Exception e) {
		rightQueue.toss(e);
	}

	@Override
	public T getNextElement()
		throws QueryEvaluationException
	{
		// TODO check if we need to protect rightQueue from synchronized access
		// wasn't done in the original implementation either
		// if we see any weird behavior check here !!

		while (rightIter != null || rightQueue.hasNext()) {
			if (rightIter == null) {
				rightIter = rightQueue.next();
			}
			if (rightIter.hasNext()) {
				return rightIter.next();
			}
			else {
				rightIter.close();
				rightIter = null;
			}
		}

		return null;
	}

	@Override
	public void handleClose()
		throws QueryEvaluationException
	{
		closed = true;
		rightQueue.close();

		if (rightIter != null) {
			rightIter.close();
			rightIter = null;
		}

		if (leftIter != null)
			leftIter.close();
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
