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

public class LeftJoinIterator extends LookAheadIteration<BindingSet> {
	/*-----------*
	 * Variables *
	 *-----------*/

	private final CloseableIteration<BindingSet> leftIter;
	private final QueryEvaluationStep rightEvaluationStep;

	private CloseableIteration<BindingSet> rightIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LeftJoinIterator(
			EvaluationStrategy strategy,
			LeftJoin join,
			BindingSet bindings,
			QueryEvaluationStep rightEvaluationStep) throws QueryEvaluationException {
		leftIter = strategy.evaluate(join.getLeftArg(), bindings);

		rightIter = null;

		join.setAlgorithm(this);

		this.rightEvaluationStep = rightEvaluationStep;
	}

	public LeftJoinIterator(
			QueryEvaluationStep left,
			BindingSet bindings,
			QueryEvaluationStep rightEvaluationStep) throws QueryEvaluationException {
		this(left.evaluate(bindings), rightEvaluationStep);
	}

	public LeftJoinIterator(CloseableIteration<BindingSet> leftIter, QueryEvaluationStep rightEvaluationStep) {
		this.leftIter = leftIter;
		this.rightIter = null;
		this.rightEvaluationStep = rightEvaluationStep;
	}

	public static CloseableIteration<BindingSet> getInstance(
			QueryEvaluationStep left,
			BindingSet bindings,
			QueryEvaluationStep rightEvaluationStep) {

		CloseableIteration<BindingSet> leftIter = left.evaluate(bindings);

		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		} else {
			return new LeftJoinIterator(leftIter, rightEvaluationStep);
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {

		try {
			CloseableIteration<BindingSet> nextRightIter = rightIter;
			while (nextRightIter == null || nextRightIter.hasNext() || leftIter.hasNext()) {
				BindingSet leftBindings = null;

				if (nextRightIter == null) {
					if (leftIter.hasNext()) {
						// Use left arg's bindings in case join fails
						leftBindings = leftIter.next();
						nextRightIter = rightIter = rightEvaluationStep.evaluate(leftBindings);
					} else {
						return null;
					}
				} else if (!nextRightIter.hasNext()) {
					// Use left arg's bindings in case join fails
					leftBindings = leftIter.next();

					nextRightIter.close();
					nextRightIter = rightIter = rightEvaluationStep.evaluate(leftBindings);
				}

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
			if (rightIter != null) {
				rightIter.close();
			}
		}
	}
}
