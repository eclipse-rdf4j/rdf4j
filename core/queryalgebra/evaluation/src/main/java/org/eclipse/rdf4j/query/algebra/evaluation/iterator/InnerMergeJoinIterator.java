/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Comparator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;

public class InnerMergeJoinIterator extends LookAheadIteration<BindingSet> {

	private final CloseableIteration<BindingSet> leftIter;
	private final Comparator<? extends Value> cmp;

	private CloseableIteration<BindingSet> rightIter;

	private InnerMergeJoinIterator(CloseableIteration<BindingSet> leftIter, CloseableIteration<BindingSet> rightIter,
			Comparator<? extends Value> cmp)
			throws QueryEvaluationException {
		this.leftIter = leftIter;
		this.rightIter = rightIter;
		this.cmp = cmp;
	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep leftPrepared,
			QueryEvaluationStep preparedRight, BindingSet bindings, Comparator<? extends Value> cmp) {
		CloseableIteration<BindingSet> leftIter = leftPrepared.evaluate(bindings);
		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		}

		CloseableIteration<BindingSet> rightIter = preparedRight.evaluate(bindings);
		if (rightIter == QueryEvaluationStep.EMPTY_ITERATION) {
			leftIter.close();
			return rightIter;
		}

		return new InnerMergeJoinIterator(leftIter, rightIter, cmp);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {

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
