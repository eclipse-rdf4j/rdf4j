/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.Comparator;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.Value;

/**
 * Provides a bag union of the two provided iterations.
 */
public class DualUnionIteration<E> implements CloseableIteration<E> {

	private final StatementOrder statementOrder;
	private final Comparator<Value> cmp;
	private CloseableIteration<? extends E> iteration1;
	private CloseableIteration<? extends E> iteration2;
	private E nextElement;
	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private boolean closed = false;

	private DualUnionIteration(CloseableIteration<? extends E> iteration1,
			CloseableIteration<? extends E> iteration2) {
		this.iteration1 = iteration1;
		this.iteration2 = iteration2;
		this.statementOrder = null;
		this.cmp = null;
	}

	@Experimental
	public DualUnionIteration(StatementOrder statementOrder, Comparator<Value> cmp,
			CloseableIteration<? extends E> iteration1, CloseableIteration<? extends E> iteration2) {
		this.iteration1 = iteration1;
		this.iteration2 = iteration2;
		this.statementOrder = statementOrder;
		this.cmp = cmp;

		// TODO
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public static <E> CloseableIteration<? extends E> getWildcardInstance(
			CloseableIteration<? extends E> leftIteration, CloseableIteration<? extends E> rightIteration) {

		if (rightIteration instanceof EmptyIteration) {
			return leftIteration;
		} else if (leftIteration instanceof EmptyIteration) {
			return rightIteration;
		} else {
			return new DualUnionIteration<>(leftIteration, rightIteration);
		}
	}

	@Experimental
	public static <E> CloseableIteration<? extends E> getWildcardInstance(StatementOrder order,
			Comparator<Value> cmp,
			CloseableIteration<? extends E> leftIteration, CloseableIteration<? extends E> rightIteration) {

		if (rightIteration instanceof EmptyIteration) {
			return leftIteration;
		} else if (leftIteration instanceof EmptyIteration) {
			return rightIteration;
		} else {
			if (!rightIteration.hasNext()) {
				rightIteration.close();
				return leftIteration;
			}
			if (!leftIteration.hasNext()) {
				leftIteration.close();
				return rightIteration;
			}
			return new DualUnionIteration<>(order, cmp, leftIteration, rightIteration);
		}
	}

	public static <E> CloseableIteration<E> getInstance(CloseableIteration<E> leftIteration,
			CloseableIteration<E> rightIteration) {

		if (rightIteration instanceof EmptyIteration) {
			return leftIteration;
		} else if (leftIteration instanceof EmptyIteration) {
			return rightIteration;
		} else {
			return new DualUnionIteration<>(leftIteration, rightIteration);
		}
	}

	public E getNextElement() {
		if (iteration1 == null && iteration2 != null) {
			if (iteration2.hasNext()) {
				return iteration2.next();
			} else {
				iteration2.close();
				iteration2 = null;
			}
		} else if (iteration1 != null) {
			if (iteration1.hasNext()) {
				return iteration1.next();
			} else if (iteration2.hasNext()) {
				iteration1.close();
				iteration1 = null;
				return iteration2.next();
			} else {
				iteration1.close();
				iteration1 = null;
				iteration2.close();
				iteration2 = null;
			}
		}

		return null;
	}

	@Override
	public final boolean hasNext() {
		if (closed) {
			return false;
		}

		return lookAhead() != null;
	}

	@Override
	public final E next() {
		if (closed) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		E result = lookAhead();

		if (result != null) {
			nextElement = null;
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Fetches the next element if it hasn't been fetched yet and stores it in {@link #nextElement}.
	 *
	 * @return The next element, or null if there are no more results.
	 */
	private E lookAhead() {
		if (nextElement == null) {
			nextElement = getNextElement();

			if (nextElement == null) {
				close();
			}
		}
		return nextElement;
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void close() {
		if (!closed) {
			closed = true;
			nextElement = null;
			try {
				if (iteration1 != null) {
					iteration1.close();
				}
			} finally {
				if (iteration2 != null) {
					iteration2.close();
				}
			}
		}
	}
}
