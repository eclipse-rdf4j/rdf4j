/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.concurrent.locks.diagnostics;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;

/**
 * Interface to support monitoring and cleaning of locks.
 *
 * @author Håvard M. Ottestad
 */
@InternalUseOnly
public interface LockMonitoring<T extends Lock> {
	int INITIAL_WAIT_TO_COLLECT = 10000;

	Lock getLock() throws InterruptedException;

	Lock tryLock();

	default Lock getLock(String alias) throws InterruptedException {
		return getLock();
	}

	T unsafeInnerLock(Lock lock);

	default void runCleanup() {
		// no-op
	}

	default boolean requiresManualCleanup() {
		return false;
	}

	static <T extends Lock> LockMonitoring<T> wrap(Lock.ExtendedSupplier<T> supplier) {
		return new Wrapper<>(supplier);
	}

	Lock register(T lock);

	void unregister(Lock lock);

	class Wrapper<T extends Lock> implements LockMonitoring<T> {
		private final Lock.ExtendedSupplier<T> supplier;

		public Wrapper(Lock.ExtendedSupplier<T> supplier) {
			this.supplier = supplier;
		}

		@Override
		public Lock getLock() throws InterruptedException {
			return supplier.getLock();
		}

		@Override
		public Lock tryLock() {
			return supplier.tryLock();
		}

		@Override
		public T unsafeInnerLock(Lock lock) {
			return (T) lock;
		}

		@Override
		public void unregister(Lock lock) {
			// no-op
		}

		@Override
		public Lock register(Lock lock) {
			// no-op;
			return lock;
		}
	}
}
