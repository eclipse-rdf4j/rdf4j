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

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;

/**
 * Wraps a {@link CloseableIteration} as an {@link Iterator}.
 *
 * @author Mark
 */
public class CloseableIterationIterator<E> implements Iterator<E>, Closeable {

	private final CloseableIteration<? extends E, ? extends RuntimeException> iteration;

	@Deprecated(since = "4.1.0", forRemoval = true)
	public CloseableIterationIterator(Iteration<? extends E, ? extends RuntimeException> iteration) {
		this.iteration = new CloseableIteration<>() {
			@Override
			public boolean hasNext() throws RuntimeException {
				return iteration.hasNext();
			}

			@Override
			public E next() throws RuntimeException {
				return iteration.next();
			}

			@Override
			public void remove() throws RuntimeException {
				iteration.remove();
			}

			@Override
			public void close() throws RuntimeException {
			}
		};
	}

	public CloseableIterationIterator(CloseableIteration<? extends E, ? extends RuntimeException> iteration) {
		this.iteration = iteration;
	}

	@Override
	public boolean hasNext() {
		boolean hasMore = iteration.hasNext();
		if (!hasMore) {
			try {
				close();
			} catch (IOException ioe) {
				// ignore
			}
		}
		return hasMore;
	}

	@Override
	public E next() {
		return iteration.next();
	}

	@Override
	public void remove() {
		iteration.remove();
	}

	@Override
	public void close() throws IOException {
		iteration.close();
	}
}
