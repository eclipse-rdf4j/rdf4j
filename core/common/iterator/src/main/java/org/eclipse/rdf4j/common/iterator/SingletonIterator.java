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

package org.eclipse.rdf4j.common.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An iterator that contains exactly one element.
 */
public class SingletonIterator<E> implements Iterator<E> {

	private final AtomicReference<E> value;

	/**
	 * Creates a new EmptyIterator.
	 */
	public SingletonIterator(E value) {
		this.value = new AtomicReference<>(value);
	}

	@Override
	public boolean hasNext() {
		return value.get() != null;
	}

	@Override
	public E next() {
		E result = value.getAndSet(null);
		if (result == null) {
			throw new NoSuchElementException();
		}
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
