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

import java.util.NoSuchElementException;

/**
 * An Iteration that contains exactly one element.
 */
@Deprecated(since = "4.1.0")
public class SingletonIteration<E, X extends Exception> extends AbstractCloseableIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private E value;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new EmptyIteration.
	 */
	public SingletonIteration(E value) {
		this.value = value;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean hasNext() {
		return value != null;
	}

	@Override
	public E next() throws X {
		E result = value;
		value = null;
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
	protected void handleClose() throws X {
		value = null;
	}
}
