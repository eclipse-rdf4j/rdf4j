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

	public JoinIterator(QueryEvaluationStep leftPrepared,
			QueryEvaluationStep preparedRight, BindingSet bindings) throws QueryEvaluationException {
		leftIter = leftPrepared.evaluate(bindings);
		this.preparedRight = preparedRight;
	}

	private JoinIterator(CloseableIteration<BindingSet> leftIter, QueryEvaluationStep preparedRight)
			throws QueryEvaluationException {
		this.leftIter = leftIter;
		this.preparedRight = preparedRight;
	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep leftPrepared,
			QueryEvaluationStep preparedRight, BindingSet bindings) {
		CloseableIteration<BindingSet> leftIter = leftPrepared.evaluate(bindings);
		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		}

		return new JoinIterator(leftIter, preparedRight);
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

		while (leftIter.hasNext()) {
			rightIter = preparedRight.evaluate(leftIter.next());
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
			if (rightIter != null) {
				rightIter.close();
			}
		}
	}
}
