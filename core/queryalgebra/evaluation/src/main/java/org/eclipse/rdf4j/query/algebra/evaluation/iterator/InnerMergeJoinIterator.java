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
import java.util.function.Function;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

/**
 *
 *
 * @author Håvard M. Ottestad
 */
@Experimental
public class InnerMergeJoinIterator extends LookAheadIteration<BindingSet> {

	private final CloseableIteration<BindingSet> leftIterator;
	private final CloseableIteration<BindingSet> rightIterator;
	private final Comparator<Value> cmp;
	private final Function<BindingSet, Value> value;
	private final QueryEvaluationContext context;

	private InnerMergeJoinIterator(CloseableIteration<BindingSet> leftIterator,
			CloseableIteration<BindingSet> rightIterator,
			Comparator<Value> cmp, Function<BindingSet, Value> value, QueryEvaluationContext context)
			throws QueryEvaluationException {
		this.leftIterator = leftIterator;
		this.rightIterator = rightIterator;
		this.cmp = cmp;
		this.value = value;
		this.context = context;
	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep leftPrepared,
			QueryEvaluationStep preparedRight, BindingSet bindings, Comparator<Value> cmp,
			Function<BindingSet, Value> value, QueryEvaluationContext context) {
		CloseableIteration<BindingSet> leftIter = leftPrepared.evaluate(bindings);
		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		}

		CloseableIteration<BindingSet> rightIter = preparedRight.evaluate(bindings);
		if (rightIter == QueryEvaluationStep.EMPTY_ITERATION) {
			leftIter.close();
			return rightIter;
		}

		return new InnerMergeJoinIterator(leftIter, rightIter, cmp, value, context);
	}

	BindingSet next;
	BindingSet nextLeft;
	BindingSet nextRight;
	BindingSet joinedLeft;

	BindingSet join(BindingSet left, BindingSet right) {
		MutableBindingSet joined = context.createBindingSet(left);
		for (Binding binding : right) {
			if (!joined.hasBinding(binding.getName())) {
				joined.addBinding(binding);
			}
		}
		return joined;
	}

	void calculateNext() {
		if (next != null) {
			return;
		}

		BindingSet prevLeft = nextLeft;
		if (nextLeft == null && leftIterator.hasNext()) {
			nextLeft = leftIterator.next();
		}

		if (nextRight == null && rightIterator.hasNext()) {
			nextRight = rightIterator.next();
		}

		if (nextRight == null && prevLeft == null && nextLeft != null) {

			nextLeft = null;

			return;
		}

		if (nextLeft == null && nextRight != null) {
			nextRight = null;

			return;
		}

		while (next == null) {
			if (nextRight != null) {
				Value left = value.apply(nextLeft);
				Value right = value.apply(nextRight);
				int compareTo = cmp.compare(left, right);

				if (compareTo == 0) {
					next = join(nextLeft, nextRight);
					joinedLeft = nextLeft;
					nextRight = null;
				} else {
					if (compareTo < 0) {
						if (leftIterator.hasNext()) {
							nextLeft = leftIterator.next();
						} else {
							nextLeft = null;
							break;
						}
					} else {
						if (rightIterator.hasNext()) {
							nextRight = rightIterator.next();
						} else {
							nextRight = null;
							break;
						}
					}

				}
			} else {
				assert !rightIterator.hasNext() : rightIterator.toString();

				return;
			}
		}

	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		calculateNext();
		BindingSet temp = next;
		next = null;
		return temp;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			leftIterator.close();
		} finally {
			if (rightIterator != null) {
				rightIterator.close();
			}
		}
	}

}
