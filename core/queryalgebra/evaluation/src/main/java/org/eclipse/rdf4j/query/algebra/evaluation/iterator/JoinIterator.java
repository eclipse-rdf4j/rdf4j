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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.JoinReadAheadBatchPool;

/**
 * Interleaved join iterator.
 * <p>
 * This join iterator produces results by interleaving results from its left argument into its right argument to speed
 * up bindings and produce fail-fast results. Note that this join strategy is only valid in cases where all bindings
 * from the left argument can be considered in scope for the right argument.
 *
 * @author Jeen Broekstra
 */
public class JoinIterator extends LookAheadIteration<BindingSet> {

	private final CloseableIteration<BindingSet> leftIter;

	private CloseableIteration<BindingSet> rightIter;

	private final QueryEvaluationStep preparedRight;
	private final JoinReadAheadBatchPool joinReadAheadBatchPool;
	private BindingSet[] leftBatch;
	private int leftBatchIndex;
	private int leftBatchSize;

	public JoinIterator(QueryEvaluationStep leftPrepared,
			QueryEvaluationStep preparedRight, BindingSet bindings) throws QueryEvaluationException {
		this(leftPrepared, preparedRight, bindings, 0, null);
	}

	private JoinIterator(CloseableIteration<BindingSet> leftIter, QueryEvaluationStep preparedRight)
			throws QueryEvaluationException {
		this(leftIter, preparedRight, 0, null);
	}

	private JoinIterator(CloseableIteration<BindingSet> leftIter, QueryEvaluationStep preparedRight,
			int joinReadAheadDepth,
			JoinReadAheadBatchPool joinReadAheadBatchPool)
			throws QueryEvaluationException {
		this.leftIter = leftIter;
		this.preparedRight = preparedRight;
		this.joinReadAheadBatchPool = joinReadAheadBatchPool;
		if (joinReadAheadDepth > 0) {
			leftBatch = joinReadAheadBatchPool != null ? joinReadAheadBatchPool.borrowBatch()
					: new BindingSet[joinReadAheadDepth];
		}
	}

	public JoinIterator(QueryEvaluationStep leftPrepared,
			QueryEvaluationStep preparedRight, BindingSet bindings, int joinReadAheadDepth,
			JoinReadAheadBatchPool joinReadAheadBatchPool) throws QueryEvaluationException {
		this(leftPrepared.evaluate(bindings), preparedRight, joinReadAheadDepth, joinReadAheadBatchPool);
	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep leftPrepared,
			QueryEvaluationStep preparedRight, BindingSet bindings) {
		return getInstance(leftPrepared, preparedRight, bindings, 0, null);
	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep leftPrepared,
			QueryEvaluationStep preparedRight, BindingSet bindings, int joinReadAheadDepth,
			JoinReadAheadBatchPool joinReadAheadBatchPool) {
		CloseableIteration<BindingSet> leftIter = leftPrepared.evaluate(bindings);
		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		}

		return new JoinIterator(leftIter, preparedRight, joinReadAheadDepth, joinReadAheadBatchPool);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		if (rightIter != null) {
			if (rightIter.hasNext()) {
				return rightIter.next();
			} else {
				rightIter.close();
			}
		}

		BindingSet leftBindings;
		while ((leftBindings = nextLeftBinding()) != null) {
			rightIter = preparedRight.evaluate(leftBindings);
			if (rightIter.hasNext()) {
				return rightIter.next();
			} else {
				rightIter.close();
			}
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

	private BindingSet nextLeftBinding() {
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
