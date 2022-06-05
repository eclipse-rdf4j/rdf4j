/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * A {@link Spliterator} implementation that wraps a {@link CloseableIteration}.
 */
class CloseableIterationSpliterator<T, E extends Exception> extends Spliterators.AbstractSpliterator<T> {

	private final CloseableIteration<T, E> iteration;

	/**
	 * Creates a {@link Spliterator} implementation that wraps the supplied {@link CloseableIteration}. It handles
	 * occurrence of checked exceptions by wrapping them in RuntimeException, and in addition ensures that the wrapped
	 * iteration is closed when exhausted.
	 *
	 * @param iteration the iteration to wrap
	 */
	public CloseableIterationSpliterator(CloseableIteration<T, E> iteration) {
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
			} else {
				// iteration is empty, so we can close it
				iteration.close();
				return false;
			}
		} catch (Throwable e) {
			try {
				iteration.close();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			if (e instanceof Error) {
				throw (Error) e;
			}
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public void forEachRemaining(Consumer<? super T> action) {
		Objects.requireNonNull(action, "action may not be null");
		try (iteration) {
			while (iteration.hasNext()) {
				action.accept(iteration.next());
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}
	}
}
