/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

/**
 * A lock on a specific monitor that can be used for synchronization purposes.
 */
public interface Lock {

	/**
	 * Checks whether the lock is still active.
	 */
	boolean isActive();

	/**
	 * Release the lock, making it inactive.
	 */
	void release();

	/**
	 * Functional interface for supplying a lock with support for InterruptedException.
	 */
	interface Supplier {
		Lock getLock() throws InterruptedException;
	}

	/**
	 * Extension of the Lock.Supplier interface to support tryLock().
	 */
	interface ExtendedSupplier extends Supplier {
		Lock tryLock();

		static Wrapper wrap(Supplier getLockSupplier, Supplier tryLockSupplier) {
			return new Wrapper(getLockSupplier, tryLockSupplier);
		}

		class Wrapper implements ExtendedSupplier {
			Supplier getLockSupplier;
			Supplier tryLockSupplier;

			private Wrapper(Supplier getLockSupplier, Supplier tryLockSupplier) {
				this.getLockSupplier = getLockSupplier;
				this.tryLockSupplier = tryLockSupplier;
			}

			@Override
			public Lock tryLock() {
				try {
					return tryLockSupplier.getLock();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(e);
				}
			}

			@Override
			public Lock getLock() throws InterruptedException {
				return getLockSupplier.getLock();
			}
		}

	}

}
