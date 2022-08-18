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

import java.util.NoSuchElementException;

/**
 * Provides a bag union of the two provided iterations.
 */
@Deprecated(since = "4.1.0")
public class DualUnionIteration<E, X extends Exception> implements CloseableIteration<E, X> {

	private CloseableIteration<? extends E, X> iteration1;
	private CloseableIteration<? extends E, X> iteration2;
	private E nextElement;
	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private boolean closed = false;

	private DualUnionIteration(CloseableIteration<? extends E, X> iteration1,
			CloseableIteration<? extends E, X> iteration2) {
		this.iteration1 = iteration1;
		this.iteration2 = iteration2;
	}

	public static <E, X extends Exception> CloseableIteration<? extends E, X> getWildcardInstance(
			CloseableIteration<? extends E, X> leftIteration, CloseableIteration<? extends E, X> rightIteration) {

		if (rightIteration instanceof EmptyIteration) {
			return leftIteration;
		} else if (leftIteration instanceof EmptyIteration) {
			return rightIteration;
		} else {
			return new DualUnionIteration<>(leftIteration, rightIteration);
		}
	}

	public static <E, X extends Exception> CloseableIteration<E, X> getInstance(CloseableIteration<E, X> leftIteration,
			CloseableIteration<E, X> rightIteration) {

		if (rightIteration instanceof EmptyIteration) {
			return leftIteration;
		} else if (leftIteration instanceof EmptyIteration) {
			return rightIteration;
		} else {
			return new DualUnionIteration<>(leftIteration, rightIteration);
		}
	}

	public E getNextElement() throws X {
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
	public final boolean hasNext() throws X {
		if (closed) {
			return false;
		}

		return lookAhead() != null;
	}

	@Override
	public final E next() throws X {
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
	 * @throws X If there is an issue getting the next element or closing the iteration.
	 */
	private E lookAhead() throws X {
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
	public final void close() throws X {
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
