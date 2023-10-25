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
 * @author Håvard M. Ottestad
 */
@Experimental
public class InnerMergeJoinIterator extends LookAheadIteration<BindingSet> {

	private final PeekMarkIterator<BindingSet> leftIterator;
	private final PeekMarkIterator<BindingSet> rightIterator;
	private final Comparator<Value> cmp;
	private final Function<BindingSet, Value> value;
	private final QueryEvaluationContext context;

	private InnerMergeJoinIterator(CloseableIteration<BindingSet> leftIterator,
			CloseableIteration<BindingSet> rightIterator,
			Comparator<Value> cmp, Function<BindingSet, Value> value, QueryEvaluationContext context)
			throws QueryEvaluationException {
		this.leftIterator = new PeekMarkIterator<>(leftIterator);
		this.rightIterator = new PeekMarkIterator<>(rightIterator);
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
	BindingSet currentLeft;

	BindingSet join(BindingSet left, BindingSet right, boolean createNewBindingSet) {
		MutableBindingSet joined;
		if (!createNewBindingSet && left instanceof MutableBindingSet) {
			joined = (MutableBindingSet) left;
		} else {
			joined = context.createBindingSet(left);
		}
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

		if (currentLeft == null && leftIterator.hasNext()) {
			currentLeft = leftIterator.next();
		}

		if (currentLeft == null) {
			return;
		}

		while (next == null) {
			if (rightIterator.hasNext()) {
				BindingSet peekRight = rightIterator.peek();
				Value left = value.apply(currentLeft);
				Value right = value.apply(peekRight);

				int compareTo = cmp.compare(left, right);
				if (compareTo == 0) {
					if (rightIterator.isResettable()) {
						next = join(currentLeft, rightIterator.next(), true);
						return;
					} else {
						BindingSet leftPeek = leftIterator.peek();
						if (leftPeek != null && left.equals(value.apply(leftPeek))) {
							rightIterator.mark();
							next = join(currentLeft, rightIterator.next(), true);
						} else {
							BindingSet nextRight = rightIterator.next();
							BindingSet rightPeek = rightIterator.peek();
							if (rightPeek != null && right.equals(value.apply(rightPeek))) {
								next = join(currentLeft, nextRight, true);
							} else {
								next = join(currentLeft, nextRight, false);
							}
							return;
						}
						return;
					}
				} else {
					if (compareTo < 0) {
						// leftIterator is behind, or in other words, rightIterator is ahead
						if (leftIterator.hasNext()) {
							BindingSet prevLeft = currentLeft;
							currentLeft = leftIterator.next();
							if (cmp.compare(value.apply(prevLeft), value.apply(currentLeft)) == 0) {
								// we have duplicate keys on the leftIterator and need to reset the rightIterator
								rightIterator.reset();
							}
						} else {
							currentLeft = null;
							break;
						}
					} else {
						// rightIterator is behind, skip forward
						rightIterator.next();

					}

				}

			} else if (rightIterator.isResettable() && leftIterator.hasNext()) {
				rightIterator.reset();
				currentLeft = leftIterator.next();
			} else {
				currentLeft = null;
				break;
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
