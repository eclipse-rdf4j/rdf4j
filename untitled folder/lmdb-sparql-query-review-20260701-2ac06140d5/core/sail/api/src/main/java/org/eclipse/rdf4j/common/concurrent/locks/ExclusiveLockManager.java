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
package org.eclipse.rdf4j.common.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockCleaner;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockMonitoring;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockTracking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple non-reentrant lock.
 *
 * @author Håvard M. Ottestad
 */
public class ExclusiveLockManager {

	private final static Logger logger = LoggerFactory.getLogger(ExclusiveLockManager.class);

	// the underlying lock object
	final StampedLock lock = new StampedLock();

	private final int waitToCollect;

	LockMonitoring lockMonitoring;

	public ExclusiveLockManager() {
		this(false);
	}

	public ExclusiveLockManager(boolean trackLocks) {
		this(trackLocks, LockMonitoring.INITIAL_WAIT_TO_COLLECT);
	}

	public ExclusiveLockManager(boolean trackLocks, int collectionFrequency) {

		this.waitToCollect = collectionFrequency;

		if (trackLocks || Properties.lockTrackingEnabled()) {

			lockMonitoring = new LockTracking(
					true,
					"ExclusiveLockManager",
					LoggerFactory.getLogger(this.getClass()),
					waitToCollect,
					Lock.ExtendedSupplier.wrap(this::getExclusiveLockInner, this::tryExclusiveLockInner)
			);

		} else {
			lockMonitoring = new LockCleaner(
					false,
					"ExclusiveLockManager",
					LoggerFactory.getLogger(this.getClass()),
					Lock.ExtendedSupplier.wrap(this::getExclusiveLockInner, this::tryExclusiveLockInner)
			);
		}

	}

	private Lock tryExclusiveLockInner() {

		long writeLock = lock.tryWriteLock();
		if (writeLock != 0) {
			return new ExclusiveLock(writeLock, lock);
		} else {
			lockMonitoring.runCleanup();
			return null;
		}

	}

	private Lock getExclusiveLockInner() throws InterruptedException {

		long writeLock;

		if (lockMonitoring.requiresManualCleanup()) {
			do {
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
				writeLock = lock.tryWriteLock(waitToCollect, TimeUnit.MILLISECONDS);
				if (writeLock == 0) {
					lockMonitoring.runCleanup();
				}
			} while (writeLock == 0);
		} else {
			writeLock = lock.writeLockInterruptibly();
		}

		return new ExclusiveLock(writeLock, lock);
	}

	public Lock tryExclusiveLock() {
		return lockMonitoring.tryLock();
	}

	public Lock getExclusiveLock() throws InterruptedException {
		return lockMonitoring.getLock();
	}

	public boolean isActiveLock() {
		return lock.isWriteLocked();
	}

	static class ExclusiveLock implements Lock {

		private final StampedLock lock;
		private long stamp;

		public ExclusiveLock(long stamp, StampedLock lock) {
			assert stamp != 0;
			this.stamp = stamp;
			this.lock = lock;
		}

		@Override
		public boolean isActive() {
			return stamp != 0;
		}

		@Override
		public void release() {
			long temp = stamp;
			stamp = 0;

			if (temp == 0) {
				throw new IllegalMonitorStateException("Trying to release a lock that is not locked");
			}

			lock.unlockWrite(temp);
		}
	}

}
