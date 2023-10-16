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

package org.eclipse.rdf4j.common.iteration;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * An Iteration that returns the bag union of the results of a number of Iterations. 'Bag union' means that the
 * UnionIteration does not filter duplicate objects.
 */
public class UnionIteration<E> extends LookAheadIteration<E> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Iterator<? extends CloseableIteration<? extends E>> argIter;

	private CloseableIteration<? extends E> currentIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new UnionIteration that returns the bag union of the results of a number of Iterations.
	 *
	 * @param args The Iterations containing the elements to iterate over.
	 */
	@SafeVarargs
	public UnionIteration(CloseableIteration<? extends E>... args) {
		this(Arrays.asList(args));
	}

	/**
	 * Creates a new UnionIteration that returns the bag union of the results of a number of Iterations.
	 *
	 * @param args The Iterations containing the elements to iterate over.
	 */
	public UnionIteration(Iterable<? extends CloseableIteration<? extends E>> args) {
		argIter = args.iterator();

		// Initialize with empty iteration
		currentIter = new EmptyIteration<>();
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	@Override
	protected E getNextElement() {
		if (isClosed()) {
			return null;
		}

		while (true) {

			CloseableIteration<? extends E> nextCurrentIter = currentIter;
			if (nextCurrentIter != null && nextCurrentIter.hasNext()) {
				return nextCurrentIter.next();
			}

			// Current Iteration exhausted, continue with the next one
			if (nextCurrentIter != null) {
				nextCurrentIter.close();
			}

			if (argIter.hasNext()) {
				currentIter = argIter.next();
			} else {
				// All elements have been returned
				return null;
			}
		}
	}

	@Override
	protected void handleClose() {
		try {
			List<Throwable> collectedExceptions = new ArrayList<>();
			while (argIter.hasNext()) {
				try {
					CloseableIteration<? extends E> next = argIter.next();
					if (next != null) {
						next.close();
					}
				} catch (Throwable e) {
					collectedExceptions.add(e);
				}
			}
			if (!collectedExceptions.isEmpty()) {
				throw new UndeclaredThrowableException(collectedExceptions.get(0));
			}
		} finally {
			if (currentIter != null) {
				currentIter.close();
			}
		}

	}
}
