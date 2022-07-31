/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.support.query;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * @param <T>
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class DelegatingIterator<T> implements Iterator<T> {
	private final Iterator<T> delegate;

	public DelegatingIterator(Iterator<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public T next() {
		return delegate.next();
	}

	@Override
	public void remove() {
		delegate.remove();
	}

	@Override
	public void forEachRemaining(Consumer<? super T> action) {
		delegate.forEachRemaining(action);
	}
}
