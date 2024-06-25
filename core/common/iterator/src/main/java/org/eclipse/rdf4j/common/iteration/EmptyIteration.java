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
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * An iterator that does not contain any elements.
 *
 * @implNote In the future this class will stop extending AbstractCloseableIteration and instead implement
 *           CloseableIteration directly.
 */
public final class EmptyIteration<E> implements CloseableIteration<E> {

	public EmptyIteration() {
	}

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public E next() {
		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new IllegalStateException("Empty iterator does not contain any elements");
	}

	@Override
	public Stream<E> stream() {
		return Stream.empty();
	}

	@Override
	public void close() {

	}

	@Override
	public void forEachRemaining(Consumer<? super E> action) {
	}

}
