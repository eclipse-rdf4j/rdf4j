/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * A {@link Spliterator} implementation that wraps an {@link Iteration}. It
 * handles occurrence of checked exceptions by wrapping them in
 * RuntimeExceptions, and in addition ensures that the wrapped Iteration is
 * closed when exhausted (if it's a {@link CloseableIteration}).
 *
 * @author Jeen Broekstra
 * @since 4.0
 */
public class IterationSpliterator<T> extends Spliterators.AbstractSpliterator<T> {

	private final Iteration<T, ? extends Exception> iteration;

	/**
	 * Creates a {@link Spliterator} implementation that wraps the supplied
	 * {@link Iteration}. It handles occurrence of checked exceptions by wrapping
	 * them in RuntimeExceptions, and in addition ensures that the wrapped
	 * Iteration is closed when exhausted (if it's a {@link CloseableIteration}).
	 * 
	 * @param iteration
	 *        the iteration to wrap
	 */
	public IterationSpliterator(final Iteration<T, ? extends Exception> iteration) {
		super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL);
		this.iteration = iteration;
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		Objects.requireNonNull(action, "action may not be null");
		try {
			if (iteration.hasNext()) {
				action.accept(iteration.next());
				return true;
			}
			Iterations.closeCloseable(iteration);
			return false;
		}
		catch (Exception e) {
			try {
				Iterations.closeCloseable(iteration);
			}
			catch (Exception e1) {
				// fall through
			}

			if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public void forEachRemaining(final Consumer<? super T> action) {
		Objects.requireNonNull(action, "action may not be null");
		try {
			while (iteration.hasNext()) {
				action.accept(iteration.next());
			}
			Iterations.closeCloseable(iteration);
		}
		catch (Exception e) {
			try {
				Iterations.closeCloseable(iteration);
			}
			catch (Exception e1) {
				// fall through
			}

			if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			}
			throw new RuntimeException(e);
		}
	}
}
