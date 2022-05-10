/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIterationWrapper;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;

/**
 * An Iteration that holds on to a lock until the Iteration is closed. Upon closing, the underlying Iteration is closed
 * before the lock is released. This iterator closes itself as soon as all elements have been read.
 */
@InternalUseOnly
public class LockedIteration<K extends CloseableIteration<? extends E, X>, E, X extends Exception>
		extends CloseableIterationWrapper<K, E, X> {

	/**
	 * The lock to release when the Iteration is closed.
	 */
	private final Lock lock;

	/**
	 * Creates a new LockingIteration.
	 *
	 * @param iteration The underlying Iteration, must not be <var>null</var>.
	 * @param lock      The lock to release when the iterator is closed, must not be <var>null</var>.
	 */
	public LockedIteration(K iteration, Lock lock) {
		super(iteration);
		this.lock = lock;
	}

	public static <K extends CloseableIteration<E, X>, E, X extends Exception> CloseableIteration<E, X> getInstance(
			K iteration,
			Lock lock) {
		if (iteration instanceof EmptyIteration) {
			lock.release();
			return iteration;
		} else {
			return new LockedIteration<>(iteration, lock);
		}
	}

	@Override
	final protected void preHasNext() {

	}

	@Override
	final protected void preNext() {

	}

	@Override
	protected final void onClose() {
		lock.release();
	}
}
