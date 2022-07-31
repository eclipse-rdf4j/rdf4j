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

/**
 * Base class for {@link CloseableIteration}s offering common functionality. This class keeps track of whether the
 * iteration has been closed and handles multiple calls to {@link #close()} by ignoring all but the first call.
 *
 * Instances of this class is not safe to be accessed from multiple threads at the same time.
 */
@Deprecated(since = "4.1.0")
public abstract class AbstractCloseableIteration<E, X extends Exception> implements CloseableIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private boolean closed = false;

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Checks whether this CloseableIteration has been closed.
	 *
	 * @return <var>true</var> if the CloseableIteration has been closed, <var>false</var> otherwise.
	 */
	public final boolean isClosed() {
		return closed;
	}

	/**
	 * Calls {@link #handleClose()} upon first call and makes sure the resource closures are only executed once.
	 */
	@Override
	public final void close() throws X {
		if (!closed) {
			closed = true;
			handleClose();
		}
	}

	/**
	 * Called by {@link #close} when it is called for the first time. This method is only called once on each iteration.
	 * By default, this method does nothing.
	 *
	 * @throws X
	 */
	protected void handleClose() throws X {
	}
}
