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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author MJAHale
 */
public abstract class AbstractCloseableIterator<E> implements Iterator<E>, Closeable {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Checks whether this Iterator has been closed.
	 *
	 * @return <var>true</var> if the Iterator has been closed, <var>false</var> otherwise.
	 */
	public final boolean isClosed() {
		return closed.get();
	}

	/**
	 * Calls {@link #handleClose()} upon first call and makes sure this method gets called only once.
	 */
	@Override
	public final void close() throws IOException {
		if (closed.compareAndSet(false, true)) {
			handleClose();
		} else {
			handleAlreadyClosed();
		}
	}

	/**
	 * Called by {@link #close} when it is called for the first time. This method is only called once on each iteration.
	 * By default, this method does nothing.
	 *
	 * @throws X
	 */
	protected void handleClose() throws IOException {
	}

	protected void handleAlreadyClosed() throws IOException {
	}
}
