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
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.JoinReadAheadBatchPool;

public class LeftJoinIterator extends LookAheadIteration<BindingSet> {
	/*-----------*
	 * Variables *
	 *-----------*/

	private final CloseableIteration<BindingSet> leftIter;
	private final QueryEvaluationStep rightEvaluationStep;
	private final JoinReadAheadBatchPool joinReadAheadBatchPool;

	private CloseableIteration<BindingSet> rightIter;
	private BindingSet[] leftBatch;
	private int leftBatchIndex;
	private int leftBatchSize;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LeftJoinIterator(
			EvaluationStrategy strategy,
			LeftJoin join,
			BindingSet bindings,
			QueryEvaluationStep rightEvaluationStep) throws QueryEvaluationException {
		this(strategy, join, bindings, rightEvaluationStep, 0, null);
	}

	public LeftJoinIterator(
			EvaluationStrategy strategy,
			LeftJoin join,
			BindingSet bindings,
			QueryEvaluationStep rightEvaluationStep,
			int joinReadAheadDepth,
			JoinReadAheadBatchPool joinReadAheadBatchPool) throws QueryEvaluationException {
		this(strategy.evaluate(join.getLeftArg(), bindings), rightEvaluationStep, joinReadAheadDepth,
				joinReadAheadBatchPool);
		join.setAlgorithm(this);
	}

	public LeftJoinIterator(
			QueryEvaluationStep left,
			BindingSet bindings,
			QueryEvaluationStep rightEvaluationStep) throws QueryEvaluationException {
		this(left, bindings, rightEvaluationStep, 0, null);
	}

	public LeftJoinIterator(
			QueryEvaluationStep left,
			BindingSet bindings,
			QueryEvaluationStep rightEvaluationStep,
			int joinReadAheadDepth,
			JoinReadAheadBatchPool joinReadAheadBatchPool) throws QueryEvaluationException {
		this(left.evaluate(bindings), rightEvaluationStep, joinReadAheadDepth, joinReadAheadBatchPool);
	}

	public LeftJoinIterator(CloseableIteration<BindingSet> leftIter, QueryEvaluationStep rightEvaluationStep) {
		this(leftIter, rightEvaluationStep, 0, null);
	}

	public LeftJoinIterator(CloseableIteration<BindingSet> leftIter, QueryEvaluationStep rightEvaluationStep,
			int joinReadAheadDepth, JoinReadAheadBatchPool joinReadAheadBatchPool) {
		this.leftIter = leftIter;
		this.rightIter = null;
		this.rightEvaluationStep = rightEvaluationStep;
		this.joinReadAheadBatchPool = joinReadAheadBatchPool;
		if (joinReadAheadDepth > 0) {
			leftBatch = joinReadAheadBatchPool != null ? joinReadAheadBatchPool.borrowBatch()
					: new BindingSet[joinReadAheadDepth];
		}
	}

	public static CloseableIteration<BindingSet> getInstance(
			QueryEvaluationStep left,
			BindingSet bindings,
			QueryEvaluationStep rightEvaluationStep) {
		return getInstance(left, bindings, rightEvaluationStep, 0, null);
	}

	public static CloseableIteration<BindingSet> getInstance(
			QueryEvaluationStep left,
			BindingSet bindings,
			QueryEvaluationStep rightEvaluationStep,
			int joinReadAheadDepth,
			JoinReadAheadBatchPool joinReadAheadBatchPool) {

		CloseableIteration<BindingSet> leftIter = left.evaluate(bindings);

		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		} else {
			return new LeftJoinIterator(leftIter, rightEvaluationStep, joinReadAheadDepth, joinReadAheadBatchPool);
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {

		try {
			CloseableIteration<BindingSet> nextRightIter = rightIter;
			while (true) {
				if (nextRightIter != null) {
					if (nextRightIter.hasNext()) {
						return nextRightIter.next();
					}
					nextRightIter.close();
					rightIter = null;
					nextRightIter = null;
				}

				// Use left arg's bindings in case join fails
				BindingSet leftBindings = nextLeftBinding();
				if (leftBindings == null) {
					return null;
				}

				nextRightIter = rightIter = rightEvaluationStep.evaluate(leftBindings);
				if (nextRightIter == QueryEvaluationStep.EMPTY_ITERATION) {
					rightIter = null;
					return leftBindings;
				}

				if (nextRightIter.hasNext()) {
					return nextRightIter.next();
				}

				if (leftBindings != null) {
					rightIter = null;
					// Join failed, return left arg's bindings
					return leftBindings;
				}
			}
		} catch (NoSuchElementException ignore) {
			// probably, one of the iterations has been closed concurrently in
			// handleClose()
		}

		return null;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			leftIter.close();
		} finally {
			try {
				if (rightIter != null) {
					rightIter.close();
				}
			} finally {
				releaseLeftBatch();
			}
		}
	}

	protected final BindingSet nextLeftBinding() {
		if (leftBatch == null) {
			if (leftIter.hasNext()) {
				return leftIter.next();
			}
			return null;
		}

		if (leftBatchIndex >= leftBatchSize) {
			leftBatchIndex = 0;
			leftBatchSize = 0;
			while (leftBatchSize < leftBatch.length && leftIter.hasNext()) {
				leftBatch[leftBatchSize] = leftIter.next();
				leftBatchSize++;
			}
			if (leftBatchSize == 0) {
				return null;
			}
		}

		return leftBatch[leftBatchIndex++];
	}

	private void releaseLeftBatch() {
		if (leftBatch == null) {
			return;
		}

		if (joinReadAheadBatchPool != null) {
			joinReadAheadBatchPool.releaseBatch(leftBatch);
		}

		leftBatch = null;
		leftBatchIndex = 0;
		leftBatchSize = 0;
	}
}
