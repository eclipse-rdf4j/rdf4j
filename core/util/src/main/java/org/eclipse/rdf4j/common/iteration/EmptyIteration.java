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
public class EmptyIteration<E, X extends Exception> extends AbstractCloseableIteration<E, X> {

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

	public boolean hasNext() {
		return false;
	}

	public E next() {
		throw new NoSuchElementException();
	}

	public void remove() {
		throw new IllegalStateException("Empty iterator does not contain any elements");
	}
}
