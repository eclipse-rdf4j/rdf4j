/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

/**
 * Base class for {@link CloseableIteration}s offering common functionality. This class keeps track of whether the
 * iteration has been closed and handles multiple calls to {@link #close()} by ignoring all but the first call.
 */
public abstract class AbstractCloseableIteration<E, X extends Exception> implements CloseableIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private volatile boolean closed = false;
	private final Object MONITOR_FOR_CLOSED = new Object();

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Checks whether this CloseableIteration has been closed.
	 *
	 * @return <tt>true</tt> if the CloseableIteration has been closed, <tt>false</tt> otherwise.
	 */
	public final boolean isClosed() {
		return closed;
	}

	/**
	 * Calls {@link #handleClose()} upon first call and makes sure the resource closures are only executed once.
	 */
	@Override
	public final void close() throws X {
		// this code is used because AtomicBoolean is slow for our usecase
		if (!closed) {

			// closedInThisCall will be true if we actually end up setting closed = true within this method call
			boolean closedInThisCall = false;

			// We synchronize here on _MONITOR_FOR_CLOSED_ so that we eliminate any race conditions then we read the
			// variable again, since it could have been modified since last we checked it. Do not synchronize on _this_
			// since it causes contention with subclasses that might also use synchronization. See issue
			// https://github.com/eclipse/rdf4j/issues/2774.
			synchronized (MONITOR_FOR_CLOSED) {
				if (!closed) {
					closed = true;
					closedInThisCall = true;
				}
			}

			if (closedInThisCall) {
				handleClose();
			}
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
