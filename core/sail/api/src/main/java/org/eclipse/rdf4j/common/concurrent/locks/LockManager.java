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
 * Manages a set of non-exclusive locks.
 *
 * @author Håvard M. Ottestad
 */
public class LockManager {

	private final static Logger logger = LoggerFactory.getLogger(LockManager.class);

	final StampedLock lock = new StampedLock();

	private final int waitToCollect;

	LockMonitoring lockMonitoring;

	public LockManager() {
		this(false);
	}

	public LockManager(boolean trackLocks) {
		this(trackLocks, LockMonitoring.INITIAL_WAIT_TO_COLLECT);
	}

	public LockManager(boolean trackLocks, int waitToCollect) {
		this.waitToCollect = waitToCollect;

		if (trackLocks || Properties.lockTrackingEnabled()) {

			lockMonitoring = new LockTracking(
					true,
					"LockManager",
					LoggerFactory.getLogger(this.getClass()),
					waitToCollect,
					Lock.ExtendedSupplier.wrap(() -> new ReadLock(lock), null)
			);

		} else {

			lockMonitoring = new LockCleaner(
					false,
					"LockManager",
					LoggerFactory.getLogger(this.getClass()),
					Lock.ExtendedSupplier.wrap(() -> new ReadLock(lock), null)
			);

		}
	}

	public boolean isActiveLock() {
		return lock.isReadLocked();
	}

	public void waitForActiveLocks() throws InterruptedException {
		while (isActiveLock()) {

			if (lockMonitoring.requiresManualCleanup()) {

				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				// if we get a non-zero write lock then we know that we don't have any active readers
				long writeLock = lock.tryWriteLock(waitToCollect, TimeUnit.MILLISECONDS);

				if (writeLock == 0) {
					lockMonitoring.runCleanup();
				} else {
					lock.unlockWrite(writeLock);
					return;
				}
			} else {
				long writeLock = lock.writeLockInterruptibly();
				lock.unlockWrite(writeLock);
			}

		}

	}

	public Lock createLock(String alias) {
		try {
			return lockMonitoring.getLock(alias);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	static class ReadLock implements Lock {

		private final StampedLock lock;
		private long stamp;

		public ReadLock(StampedLock lock) {
			this.stamp = lock.readLock();
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

			lock.unlockRead(temp);
		}

	}

}
