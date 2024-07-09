/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.common.concurrent.locks;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockCleaner;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockMonitoring;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockTracking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple reentrant lock that allows other threads to unlock the lock.
 *
 * @author Håvard M. Ottestad
 */
public class ExclusiveReentrantLockManager {

	private final static Logger logger = LoggerFactory.getLogger(ExclusiveReentrantLockManager.class);

	// the underlying lock object
	final AtomicLong activeLocks = new AtomicLong();
	final AtomicReference<Thread> owner = new AtomicReference<>();

	private final int waitToCollect;

	LockMonitoring<ExclusiveReentrantLock> lockMonitoring;

	public ExclusiveReentrantLockManager() {
		this(false);
	}

	public ExclusiveReentrantLockManager(boolean trackLocks) {
		this(trackLocks, LockMonitoring.INITIAL_WAIT_TO_COLLECT);
	}

	public ExclusiveReentrantLockManager(boolean trackLocks, int collectionFrequency) {

		this.waitToCollect = collectionFrequency;

		if (trackLocks || Properties.lockTrackingEnabled()) {

			lockMonitoring = new LockTracking(
					true,
					"ExclusiveReentrantLockManager",
					LoggerFactory.getLogger(this.getClass()),
					waitToCollect,
					Lock.ExtendedSupplier.wrap(this::getExclusiveLockInner, this::tryExclusiveLockInner)
			);

		} else {
			lockMonitoring = new LockCleaner(
					false,
					"ExclusiveReentrantLockManager",
					LoggerFactory.getLogger(this.getClass()),
					Lock.ExtendedSupplier.wrap(this::getExclusiveLockInner, this::tryExclusiveLockInner)
			);
		}

	}

	private Lock tryExclusiveLockInner() {

		synchronized (owner) {
			if (owner.get() == Thread.currentThread()) {
				activeLocks.incrementAndGet();
				return new ExclusiveReentrantLock(owner, activeLocks);
			}

			if (owner.compareAndSet(null, Thread.currentThread())) {
				activeLocks.incrementAndGet();
				return new ExclusiveReentrantLock(owner, activeLocks);
			}
		}

		return null;

	}

	private Lock getExclusiveLockInner() throws InterruptedException {

		synchronized (owner) {

			if (lockMonitoring.requiresManualCleanup()) {
				do {
					if (Thread.interrupted()) {
						throw new InterruptedException();
					}
					Lock lock = tryExclusiveLockInner();
					if (lock != null) {
						return lock;
					} else {
						lockMonitoring.runCleanup();
						owner.wait(waitToCollect);
					}
				} while (true);
			} else {
				while (true) {
					if (Thread.interrupted()) {
						throw new InterruptedException();
					}
					Lock lock = tryExclusiveLockInner();
					if (lock != null) {
						return lock;
					} else {
						owner.wait(waitToCollect);
					}
				}
			}
		}
	}

	public Lock tryExclusiveLock() {
		return lockMonitoring.tryLock();
	}

	public Lock getExclusiveLock() throws InterruptedException {
		return lockMonitoring.getLock();
	}

	public boolean isActiveLock() {
		return owner.get() != null;
	}

	static class ExclusiveReentrantLock implements Lock {

		final AtomicLong activeLocks;
		final AtomicReference<Thread> owner;
		private boolean released = false;

		public ExclusiveReentrantLock(AtomicReference<Thread> owner, AtomicLong activeLocks) {
			this.owner = owner;
			this.activeLocks = activeLocks;
		}

		@Override
		public boolean isActive() {
			return !released;
		}

		@Override
		public void release() {
			if (released) {
				throw new IllegalStateException("Lock already released");
			}

			synchronized (owner) {
				if (owner.get() != Thread.currentThread()) {
					logger.warn("Releasing lock from different thread, owner: " + owner.get() + ", current: "
							+ Thread.currentThread());
				}

				if (activeLocks.decrementAndGet() == 0) {
					owner.set(null);
					owner.notifyAll();
				}
			}

			released = true;

		}
	}

}
