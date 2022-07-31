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

import java.util.Iterator;

/**
 * An Iteration that can convert an {@link Iterator} to a {@link Iteration}.
 */
@Deprecated(since = "4.1.0")
public class IteratorIteration<E, X extends Exception> implements Iteration<E, X> {

	private final Iterator<? extends E> iter;

	public IteratorIteration(Iterator<? extends E> iter) {
		assert iter != null;
		this.iter = iter;
	}

	@Override
	public boolean hasNext() {
		return iter.hasNext();
	}

	@Override
	public E next() {
		return iter.next();
	}

	@Override
	public void remove() {
		iter.remove();
	}
}
