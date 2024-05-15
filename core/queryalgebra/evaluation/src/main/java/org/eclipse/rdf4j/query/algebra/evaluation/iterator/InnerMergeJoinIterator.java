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
import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
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
	private final Function<BindingSet, Value> value;
	private final QueryEvaluationContext context;
	private BindingSet nextElement;
	private boolean closed;

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

	BindingSet prevLeft;

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
					equal(left, right);
					break;
				} else {
					if (compareTo < 0) {
						// leftIterator is behind, or in other words, rightIterator is ahead
						if (leftIterator.hasNext()) {
							lessThan();
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

	private void lessThan() {
		BindingSet prevLeft = currentLeft;
		currentLeft = leftIterator.next();
		if (cmp.compare(value.apply(prevLeft), value.apply(currentLeft)) == 0) {
			// we have duplicate keys on the leftIterator and need to reset the rightIterator (if it
			// is resettable)
			if (rightIterator.isResettable()) {
				rightIterator.reset();
			}
		} else {
			rightIterator.unmark();
		}
	}

	private void equal(Value left, Value right) {
		if (rightIterator.isResettable()) {
			next = join(currentLeft, rightIterator.next(), true);
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
			}
		}
	}

	@Override
	public final boolean hasNext() {
		if (isClosed()) {
			return false;
		}

		calculateNext();

		if (next == null) {
			close();
		}

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
