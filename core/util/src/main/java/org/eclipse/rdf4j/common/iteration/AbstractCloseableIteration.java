/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for {@link CloseableIteration}s offering common functionality.
 * This class keeps track of whether the iteration has been closed and handles
 * multiple calls to {@link #close()} by ignoring all but the first call.
 */
public abstract class AbstractCloseableIteration<E, X extends Exception> implements CloseableIteration<E, X> {

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
	 * Checks whether this CloseableIteration has been closed.
	 * 
	 * @return <tt>true</tt> if the CloseableIteration has been closed,
	 *         <tt>false</tt> otherwise.
	 */
	public final boolean isClosed() {
		return closed.get();
	}

	/**
	 * Calls {@link #handleClose()} upon first call and makes sure this method
	 * gets called only once.
	 */
	public final void close()
		throws X
	{
		if (closed.compareAndSet(false, true)) {
			handleClose();
		}
	}

	/**
	 * Called by {@link #close} when it is called for the first time. This method
	 * is only called once on each iteration. By default, this method does
	 * nothing.
	 * 
	 * @throws X
	 */
	protected void handleClose()
		throws X
	{
	}
}
