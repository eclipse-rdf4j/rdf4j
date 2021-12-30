/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

import java.util.Objects;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;

/**
 * An Iteration that holds on to a lock until the Iteration is closed. Upon closing, the underlying Iteration is closed
 * before the lock is released. This iterator closes itself as soon as all elements have been read.
 */
public class LockingIteration<E, X extends Exception> extends IterationWrapper<E, X> {

	/**
	 * The lock to release when the Iteration is closed.
	 */
	private final Lock lock;

	/**
	 * Creates a new LockingIteration.
	 *
	 * @param lock The lock to release when the itererator is closed, must not be <var>null</var>.
	 * @param iter The underlying Iteration, must not be <var>null</var>.
	 */
	public LockingIteration(Lock lock, Iteration<? extends E, X> iter) {
		super(iter);
		this.lock = Objects.requireNonNull(lock);
	}

	@Override
	protected void handleClose() throws X {
		try {
			super.handleClose();
		} finally {
			lock.release();
		}
	}
}
