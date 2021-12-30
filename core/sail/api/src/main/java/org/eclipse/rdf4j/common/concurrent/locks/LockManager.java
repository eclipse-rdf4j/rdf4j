/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.concurrent.locks;

import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a set of active locks. If any active lock is garbage collected it is automatically removed from the set and
 * logged.
 *
 * @author James Leigh
 */
public class LockManager {

	/**
	 * Number of milliseconds to block thread before the garbage collection should search and collect abandoned active
	 * locks
	 */
	private static final int INITIAL_WAIT_TO_COLLECT = 10000;

	private static final int MAX_WAIT_TO_COLLECT = 90 * 60 * 1000;

	private static final AtomicLong seq = new AtomicLong();

	// the underlying lock object
	final StampedLock lock = new StampedLock();

	LockMonitoring lockMonitoring;

	private final Logger logger = LoggerFactory.getLogger(LockManager.class);

	/**
	 * Controls whether the lock manager will keep a stack trace of where each lock was created. Mainly useful for
	 * debugging.
	 */
	private final boolean trackLocks;

	/**
	 * Number of milliseconds to block thread before the garbage collection should search and collect abandoned active
	 * locks
	 */
	private int waitToCollect;

	/**
	 * Create a new set of locks.
	 */
	public LockManager() {
		this(false);
	}

	/**
	 * Creates a new set of locks, optionally with lock tracking enabled.
	 *
	 * @param trackLocks Controls whether to keep a stack trace of active locks. Enabling lock tracking will add some
	 *                   overhead, but can be very useful for debugging.
	 */
	public LockManager(boolean trackLocks) {
		this(trackLocks, INITIAL_WAIT_TO_COLLECT);
	}

	/**
	 * Creates a new set of locks, optionally with lock tracking enabled.
	 *
	 * @param trackLocks          Controls whether to keep a stack trace of active locks. Enabling lock tracking will
	 *                            add some overhead, but can be very useful for debugging.
	 * @param collectionFrequency Number of milliseconds to block the first thread, waiting for active locks to finish,
	 *                            before running the memory garbage collection, to free abandoned active locks.
	 */
	public LockManager(boolean trackLocks, int collectionFrequency) {
		this.trackLocks = trackLocks || Properties.lockTrackingEnabled();
		this.waitToCollect = collectionFrequency;

		lockMonitoring = new LockMonitoring(this.trackLocks, () -> new Lock() {

			transient long stamp = lock.readLock();

			@Override
			public boolean isActive() {
				return stamp != 0;
			}

			@Override
			public void release() {
				if (stamp == 0)
					return;
				lock.unlockRead(stamp);
				stamp = 0;
			}

		});
	}

	/**
	 * If any locks in this collection that are still active.
	 *
	 * @return <code>true</code> of one or more locks that have not be released.
	 */
	public boolean isActiveLock() {
		return lock.isReadLocked();
	}

	/**
	 * Blocks current thread until the number of active locks has reached zero.
	 *
	 * @throws InterruptedException if any thread interrupted the current thread before or while the current thread was
	 *                              waiting for a notification. The interrupted status of the current thread is cleared
	 *                              when this exception is thrown.
	 */
	public void waitForActiveLocks() throws InterruptedException {
		int loopCounter = 0;
		long activeLocksSignature = 0;

		while (isActiveLock()) {
			loopCounter++;

			// if we get a non-zero write lock then we know that we don't have any active readers
			long writeLock = lock.tryWriteLock(waitToCollect, TimeUnit.MILLISECONDS);

			if (writeLock == 0) {
				if (lockMonitoring.hasAbandonedLocks()) {
					lockMonitoring.forceReleaseAbandonedLocks();
				} else if (loopCounter > 10) {
					System.gc();
					long previousActiveLocksSignature = activeLocksSignature;
					activeLocksSignature = lockMonitoring.getActiveLocksSignature();
					if (previousActiveLocksSignature != 0 && previousActiveLocksSignature == activeLocksSignature) {
						lockMonitoring.logStalledLocks();
					}
				}

			} else {
				lock.unlockWrite(writeLock);
				return;
			}

		}

	}

	/**
	 * Creates a new active lock. This increases the number of active locks until its {@link Lock#release()} method is
	 * called, which decreases the number of active locks by the same amount.
	 *
	 * @param alias a short string used to log abandon locks
	 * @return an active lock
	 */
	public Lock createLock(String alias) {

		return lockMonitoring.getLock(alias);

	}

}
