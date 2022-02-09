/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks.diagnostics;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;

/**
 * Interface to support monitoring and cleaning of locks.
 *
 * @author Håvard M. Ottestad
 */
@InternalUseOnly
public interface LockMonitoring {
	int INITIAL_WAIT_TO_COLLECT = 10000;

	Lock getLock() throws InterruptedException;

	Lock tryLock();

	default Lock getLock(String alias) throws InterruptedException {
		return getLock();
	}

	default void runCleanup() {
		// no-op
	}

	default boolean requiresManualCleanup() {
		return false;
	}

	static LockMonitoring wrap(Lock.ExtendedSupplier supplier) {
		return new Wrapper(supplier);
	}

	class Wrapper implements LockMonitoring {
		private final Lock.ExtendedSupplier supplier;

		public Wrapper(Lock.ExtendedSupplier supplier) {
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
	}
}
