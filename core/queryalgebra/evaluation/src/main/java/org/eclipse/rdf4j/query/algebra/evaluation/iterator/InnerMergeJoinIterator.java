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
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.ArrayBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

/**
 * @author Håvard M. Ottestad
 */
@Experimental
public class InnerMergeJoinIterator implements CloseableIteration<BindingSet> {

	private final PeekMarkIterator<BindingSet> leftIterator;
	private final PeekMarkIterator<BindingSet> rightIterator;
	private final Comparator<Value> cmp;
	private final Function<BindingSet, Value> valueFunction;
	private final QueryEvaluationContext context;
	private final boolean leftSeek;
	private final boolean rightSeek;

	private BindingSet next;
	private BindingSet currentLeft;
	private Value currentLeftValue;
	private Value leftPeekValue;

	// -1 for unset, 0 for equal, 1 for different
	int currentLeftValueAndPeekEquals = -1;

	private boolean closed = false;

	InnerMergeJoinIterator(CloseableIteration<BindingSet> leftIterator, CloseableIteration<BindingSet> rightIterator,
			Comparator<Value> cmp, Function<BindingSet, Value> valueFunction, QueryEvaluationContext context)
			throws QueryEvaluationException {

		this.leftIterator = new PeekMarkIterator<>(leftIterator);
		this.rightIterator = new PeekMarkIterator<>(rightIterator);
		this.cmp = cmp;
		this.valueFunction = valueFunction;
		this.context = context;
		this.leftSeek = this.leftIterator.supportsSeek();
		this.rightSeek = this.rightIterator.supportsSeek();
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

	private BindingSet join(BindingSet left, BindingSet right, boolean createNewBindingSet) {
		MutableBindingSet joined;
		if (!createNewBindingSet && left instanceof MutableBindingSet) {
			joined = (MutableBindingSet) left;
		} else {
			joined = context.createBindingSet(left);
		}
		if (joined instanceof ArrayBindingSet && right instanceof ArrayBindingSet) {
			((ArrayBindingSet) joined).addAll((ArrayBindingSet) right);
			return joined;
		}

		for (Binding binding : right) {
			if (!joined.hasBinding(binding.getName())) {
				joined.addBinding(binding);
			}
		}
		return joined;
	}

	private void calculateNext() {
		if (next != null) {
			return;
		}

		if (currentLeft == null && leftIterator.hasNext()) {
			currentLeft = leftIterator.next();
			currentLeftValue = null;
			leftPeekValue = null;
			currentLeftValueAndPeekEquals = -1;

		}

		if (currentLeft == null) {
			return;
		}

		loop();

	}

	private void loop() {
		while (next == null) {
			if (rightIterator.hasNext()) {
				BindingSet peekRight = rightIterator.peek();

				if (currentLeftValue == null) {
					currentLeftValue = valueFunction.apply(currentLeft);
					leftPeekValue = null;
					currentLeftValueAndPeekEquals = -1;
				}

				Value rightValue = valueFunction.apply(peekRight);
				int compare = compare(currentLeftValue, rightValue);

				if (compare == 0) {
					equal();
					return;
				} else if (compare < 0) {
					// leftIterator is behind, or in other words, rightIterator is ahead
					if (leftIterator.hasNext()) {
						lessThan(rightValue);
					} else {
						close();
						return;
					}
				} else {
					// rightIterator is behind, skip forward
					rightIterator.next();
					if (rightSeek) {
						// advisory hint: right rows ordered before the current left value can never join
						rightIterator.seek(currentLeftValue, true, null, true);
					}
				}

			} else if (rightIterator.isResettable() && leftIterator.hasNext()) {
				rightIterator.reset();
				currentLeft = leftIterator.next();
				currentLeftValue = null;
				leftPeekValue = null;
				currentLeftValueAndPeekEquals = -1;
			} else {
				close();
				return;
			}

		}
	}

	private int compare(Value left, Value right) {
		int compareTo;

		if (left == right) {
			compareTo = 0;
		} else {
			compareTo = cmp.compare(left, right);
		}
		return compareTo;
	}

	private void lessThan(Value rightValue) {
		Value oldLeftValue = currentLeftValue;
		currentLeft = leftIterator.next();
		if (leftPeekValue != null) {
			currentLeftValue = leftPeekValue;
		} else {
			currentLeftValue = valueFunction.apply(currentLeft);
		}

		leftPeekValue = null;
		currentLeftValueAndPeekEquals = -1;

		if (oldLeftValue.equals(currentLeftValue)) {
			// we have duplicate keys on the leftIterator and need to reset the rightIterator (if it
			// is resettable)
			if (rightIterator.isResettable()) {
				rightIterator.reset();
			}
		} else {
			rightIterator.unmark();
		}

		if (leftSeek && !rightIterator.isResettable()) {
			// advisory hint: with no replay obligations left on the right side, every unconsumed right row is
			// ordered at or after rightValue, so left rows ordered before it can never join
			leftIterator.seek(rightValue, true, null, true);
		}
	}

	private void equal() {
		if (rightIterator.isResettable()) {
			next = join(currentLeft, rightIterator.next(), true);
		} else {
			doLeftPeek();

			if (currentLeftValueAndPeekEquals == 0) {
				rightIterator.mark();
				next = join(currentLeft, rightIterator.next(), true);
			} else {
				next = join(rightIterator.next(), currentLeft, false);
			}
		}
	}

	private void doLeftPeek() {
		if (leftPeekValue == null) {
			BindingSet leftPeek = leftIterator.peek();
			leftPeekValue = leftPeek != null ? valueFunction.apply(leftPeek) : null;
			currentLeftValueAndPeekEquals = -1;
		}

		if (currentLeftValueAndPeekEquals == -1) {
			boolean equals = currentLeftValue.equals(leftPeekValue);
			if (equals) {
				currentLeftValue = leftPeekValue;
				currentLeftValueAndPeekEquals = 0;
			} else {
				currentLeftValueAndPeekEquals = 1;
			}
		}
	}

	@Override
	public final boolean hasNext() {
		if (isClosed()) {
			return false;
		}

		calculateNext();

		return next != null;
	}

	@Override
	public final BindingSet next() {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		calculateNext();

		if (next == null) {
			close();
		}

		BindingSet result = next;

		if (result != null) {
			next = null;
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Checks whether this CloseableIteration has been closed.
	 *
	 * @return <var>true</var> if the CloseableIteration has been closed, <var>false</var> otherwise.
	 */
	public final boolean isClosed() {
		return closed;
	}

	@Override
	public final void close() {
		if (!closed) {
			closed = true;
			try {
				leftIterator.close();
			} finally {
				rightIterator.close();
			}
		}
	}
}
