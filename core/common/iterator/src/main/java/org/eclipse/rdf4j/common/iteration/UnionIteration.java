/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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
public class UnionIteration<E, X extends Exception> extends LookAheadIteration<E, X> {

	private final CloseableIteration<E, X> delegate;

	private UnionIteration() {
		delegate = null;
	}

	/**
	 * Creates a new UnionIteration that returns the bag union of the results of a number of Iterations.
	 *
	 * @param args The Iterations containing the elements to iterate over.
	 * @deprecated Use {@link UnionIteration#getInstance(Iteration[])} instead;
	 */
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SafeVarargs
	public UnionIteration(Iteration<? extends E, X>... args) {
		this.delegate = getInstance(args);
	}

	/**
	 * Creates a new UnionIteration that returns the bag union of the results of a number of Iterations.
	 *
	 * @param args The Iterations containing the elements to iterate over.
	 * @deprecated Use {@link UnionIteration#getInstance(List)} instead;
	 */
	@Deprecated(since = "4.0.0", forRemoval = true)
	public UnionIteration(Iterable<? extends Iteration<? extends E, X>> args) {
		this.delegate = new ArbitrarySizeUnionIteration<>(args);
	}

	@SafeVarargs
	public static <E, X extends Exception> CloseableIteration<E, X> getInstance(Iteration<? extends E, X>... args) {
		return new ArbitrarySizeUnionIteration<>(Arrays.asList(args));
	}

	public static <E, X extends Exception> CloseableIteration<E, X> getInstance(
			List<? extends Iteration<? extends E, X>> args) {
		return new ArbitrarySizeUnionIteration<>(args);
	}

	public static <E, X extends Exception> CloseableIteration<E, X> getInstance(CloseableIteration<E, X> iteration1,
			CloseableIteration<E, X> iteration2) {
		if (iteration2 instanceof EmptyIteration) {
			return iteration1;
		} else if (iteration1 instanceof EmptyIteration) {
			return iteration2;
		} else {
			return new DualUnionIteration<>(iteration1, iteration2);
		}
	}

	@Override
	public E getNextElement() throws X {
		if (delegate.hasNext()) {
			return delegate.next();
		}
		return null;
	}

	@Override
	public void handleClose() throws X {
		if (delegate != null) {
			delegate.close();
		}
	}

	private static class ArbitrarySizeUnionIteration<E, X extends Exception> extends UnionIteration<E, X> {

		private final Iterator<? extends Iteration<? extends E, X>> argIter;

		private Iteration<? extends E, X> currentIter;

		private ArbitrarySizeUnionIteration(List<? extends Iteration<? extends E, X>> args) {
			super();
			argIter = args.iterator();
		}

		private ArbitrarySizeUnionIteration(Iterable<? extends Iteration<? extends E, X>> args) {
			super();
			argIter = args.iterator();
		}

		@Override
		public E getNextElement() throws X {
			if (isClosed()) {
				return null;
			}

			while (true) {

				if (currentIter != null) {
					if (currentIter.hasNext()) {
						return currentIter.next();
					} else {
						// Current Iteration exhausted, continue with the next one
						Iterations.closeCloseable(currentIter);
					}
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
		public void handleClose() throws X {
			try {
				// Close this iteration, this will prevent lookAhead() from calling
				// getNextElement() again
				super.handleClose();
			} finally {
				try {
					List<Throwable> collectedExceptions = null;
					while (argIter.hasNext()) {
						try {
							Iterations.closeCloseable(argIter.next());
						} catch (Throwable e) {
							if (collectedExceptions == null) {
								collectedExceptions = new ArrayList<>();
							}
							collectedExceptions.add(e);
						}
					}
					if (collectedExceptions != null && !collectedExceptions.isEmpty()) {
						throw new UndeclaredThrowableException(collectedExceptions.get(0));
					}
				} finally {
					Iterations.closeCloseable(currentIter);
				}
			}
		}

	}

	private static class DualUnionIteration<E, X extends Exception> extends UnionIteration<E, X> {

		private final Iteration<? extends E, X> iteration1;
		private final Iteration<? extends E, X> iteration2;

		public DualUnionIteration(Iteration<? extends E, X> iteration1, Iteration<? extends E, X> iteration2) {
			super();
			this.iteration1 = iteration1;
			this.iteration2 = iteration2;
		}

		@Override
		public E getNextElement() throws X {
			if (iteration1.hasNext()) {
				return iteration1.next();
			} else if (iteration2.hasNext()) {
				return iteration2.next();
			}

			return null;
		}

		@Override
		public void handleClose() throws X {
			try {
				Iterations.closeCloseable(iteration1);
			} finally {
				Iterations.closeCloseable(iteration2);
			}
		}
	}

}
