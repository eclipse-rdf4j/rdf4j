/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An Iteration that contains exactly one element.
 */
public class SingletonIteration<E, X extends Exception> extends AbstractCloseableIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final AtomicReference<E> value;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new EmptyIteration.
	 */
	public SingletonIteration(E value) {
		this.value = new AtomicReference<>(value);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean hasNext() {
		return value.get() != null;
	}

	@Override
	public E next()
		throws X
	{
		E result = value.getAndSet(null);
		if (result == null) {
			close();
			throw new NoSuchElementException();
		}
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void handleClose()
		throws X
	{
		try {
			super.handleClose();
		}
		finally {
			value.set(null);
		}
	}
}
