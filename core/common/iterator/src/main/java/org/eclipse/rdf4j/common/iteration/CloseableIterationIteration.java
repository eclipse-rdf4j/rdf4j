/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.security.spec.ECField;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Value;

/**
 * An Iteration that can convert an {@link Iterator} to a {@link CloseableIteration}.
 */
@InternalUseOnly
public class CloseableIterationIteration<K extends Iteration<E, X>, E, X extends Exception>
		extends AbstractCloseableIteration<E, X> {

	private final K iter;

	public CloseableIterationIteration(K iter) {
		this.iter = iter;
	}

	public static <K extends Iteration<E, X>, E, X extends Exception> CloseableIterationIteration<K, E, X> of(
			K iteration) {
		return new CloseableIterationIteration<>(iteration);
	}

	@Override
	public boolean hasNext() throws X {
		if (isClosed()) {
			return false;
		}

		boolean result = iter.hasNext();
		if (!result) {
			close();
		}
		return result;
	}

	@Override
	public E next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("Iteration has been closed");
		}

		return iter.next();
	}

	@Override
	public void remove() throws X {
		if (isClosed()) {
			throw new IllegalStateException("Iteration has been closed");
		}

		iter.remove();
	}

	@Override
	protected final void handleClose() throws X {
		// no-op
	}
}
