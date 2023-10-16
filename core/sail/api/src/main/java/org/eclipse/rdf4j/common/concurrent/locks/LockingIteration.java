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

package org.eclipse.rdf4j.common.concurrent.locks;

import java.util.Objects;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;

/**
 * An Iteration that holds on to a lock until the Iteration is closed. Upon closing, the underlying Iteration is closed
 * before the lock is released. This iterator closes itself as soon as all elements have been read.
 */
public class LockingIteration<E> extends IterationWrapper<E> {

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
	private LockingIteration(Lock lock, CloseableIteration<? extends E> iter) {
		super(iter);
		if (iter instanceof EmptyIteration) {
			lock.release();
			this.lock = null;
		} else {
			this.lock = Objects.requireNonNull(lock);
		}
	}

	public static <T, R extends Exception> CloseableIteration<T> getInstance(Lock lock,
			CloseableIteration<T> iter) {
		if (iter instanceof EmptyIteration) {
			lock.release();
			return iter;
		} else {
			return new LockingIteration<>(lock, iter);
		}
	}

	@Override
	protected void handleClose() {
		try {
			super.handleClose();
		} finally {
			if (lock != null) {
				lock.release();
			}
		}
	}
}
