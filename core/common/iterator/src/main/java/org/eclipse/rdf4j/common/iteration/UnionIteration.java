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
@Deprecated(since = "4.1.0")
public class UnionIteration<E, X extends Exception> extends LookAheadIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Iterator<? extends Iteration<? extends E, X>> argIter;

	private Iteration<? extends E, X> currentIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new UnionIteration that returns the bag union of the results of a number of Iterations.
	 *
	 * @param args The Iterations containing the elements to iterate over.
	 */
	@SafeVarargs
	public UnionIteration(Iteration<? extends E, X>... args) {
		this(Arrays.asList(args));
	}

	/**
	 * Creates a new UnionIteration that returns the bag union of the results of a number of Iterations.
	 *
	 * @param args The Iterations containing the elements to iterate over.
	 */
	public UnionIteration(Iterable<? extends Iteration<? extends E, X>> args) {
		argIter = args.iterator();

		// Initialize with empty iteration
		currentIter = new EmptyIteration<>();
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	@Override
	protected E getNextElement() throws X {
		if (isClosed()) {
			return null;
		}

		while (true) {

			Iteration<? extends E, X> nextCurrentIter = currentIter;
			if (nextCurrentIter != null && nextCurrentIter.hasNext()) {
				return nextCurrentIter.next();
			}

			// Current Iteration exhausted, continue with the next one
			Iterations.closeCloseable(nextCurrentIter);

			if (argIter.hasNext()) {
				currentIter = argIter.next();
			} else {
				// All elements have been returned
				return null;
			}
		}
	}

	@Override
	protected void handleClose() throws X {
		try {
			// Close this iteration, this will prevent lookAhead() from calling
			// getNextElement() again
			super.handleClose();
		} finally {
			try {
				List<Throwable> collectedExceptions = new ArrayList<>();
				while (argIter.hasNext()) {
					try {
						Iterations.closeCloseable(argIter.next());
					} catch (Throwable e) {
						if (e instanceof InterruptedException) {
							Thread.currentThread().interrupt();
						}
						collectedExceptions.add(e);
					}
				}
				if (!collectedExceptions.isEmpty()) {
					throw new UndeclaredThrowableException(collectedExceptions.get(0));
				}
			} finally {
				Iterations.closeCloseable(currentIter);
			}
		}
	}
}
