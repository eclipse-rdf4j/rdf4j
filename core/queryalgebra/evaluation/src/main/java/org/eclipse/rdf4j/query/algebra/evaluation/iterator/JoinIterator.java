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
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

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

	/*-----------*
	 * Variables *
	 *-----------*/

	private final CloseableIteration<BindingSet> leftIter;

	private CloseableIteration<BindingSet> rightIter;

	private final QueryEvaluationStep preparedRight;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public JoinIterator(EvaluationStrategy strategy, QueryEvaluationStep leftPrepared,
			QueryEvaluationStep rightPrepared, Join join, BindingSet bindings) {
		leftIter = leftPrepared.evaluate(bindings);

		// Initialize with empty iteration so that var is never null
		rightIter = new EmptyIteration<>();
		this.preparedRight = rightPrepared;
	}

	public JoinIterator(EvaluationStrategy strategy, Join join, BindingSet bindings, QueryEvaluationContext context) {
		leftIter = strategy.evaluate(join.getLeftArg(), bindings);

		// Initialize with empty iteration so that var is never null
		rightIter = new EmptyIteration<>();
		preparedRight = strategy.precompile(join.getRightArg(), context);
		join.setAlgorithm(this);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() {

		try {
			while (rightIter.hasNext() || leftIter.hasNext()) {
				if (rightIter.hasNext()) {
					return rightIter.next();
				}

				// Right iteration exhausted
				rightIter.close();

				if (leftIter.hasNext()) {
					rightIter = preparedRight.evaluate(leftIter.next());
				}
			}
		} catch (NoSuchElementException ignore) {
			// probably, one of the iterations has been closed concurrently in
			// handleClose()
		}

		return null;
	}

	@Override
	protected void handleClose() {
		try {
			super.handleClose();
		} finally {
			try {
				leftIter.close();
			} finally {
				rightIter.close();
			}
		}
	}
}
