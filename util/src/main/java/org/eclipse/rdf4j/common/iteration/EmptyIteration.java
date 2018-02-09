/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;

/**
 * An iterator that does not contain any elements.
 */
public final class EmptyIteration<E, X extends Exception> extends AbstractCloseableIteration<E, X> {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new EmptyIteration.
	 */
	public EmptyIteration() {
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public final boolean hasNext() {
		return false;
	}

	@Override
	public final E next() {
		throw new NoSuchElementException();
	}

	@Override
	public final void remove() {
		throw new IllegalStateException("Empty iterator does not contain any elements");
	}
}
