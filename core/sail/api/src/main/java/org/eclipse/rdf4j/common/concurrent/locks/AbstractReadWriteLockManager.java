/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.StampedLock;

/**
 * An abstract base implementation of a read/write lock manager.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 */
public abstract class AbstractReadWriteLockManager implements ReadWriteLockManager {

	/*
	 * ----------- Variables -----------
	 */

	/**
	 * Flag indicating whether a writer is active.
	 */
	private final StampedLock lock = new StampedLock();
	private final LongAdder readersLocked = new LongAdder();
	private final LongAdder readersUnlocked = new LongAdder();

	/**
	 * When acquiring a write-lock, the thread will acquire the write-lock and then spin & yield while waiting for
	 * readers to unlock their locks. A deadlock is possible if someone already holding a read-lock acquires another
	 * read-lock at the same time that another thread is waiting for a write-lock. To stop this from happening we can
	 * set READ_PREFERENCE to a number higher than zero. READ_PREFERENCE of 1 means that the thread acquiring a
	 * write-lock will release the write-lock if there are any readers. A READ_PREFERENCE of 100 means that the thread
	 * acquiring a write-lock will spin & yield 100 times before it attempts to release the write-lock.
	 */
	int READ_PREFERENCE = 0;

	/*
	 * -------------- Constructors --------------
	 */

	/**
	 * Creates a MultiReadSingleWriteLockManager.
	 */
	public AbstractReadWriteLockManager() {
		this(false);
	}

	/**
	 * Creates a new MultiReadSingleWriteLockManager, optionally with lock tracking enabled.
	 *
	 * @param trackLocks Controls whether the lock manager will keep track of active locks. Enabling lock tracking will
	 *                   add some overhead, but can be very useful for debugging.
	 */
	public AbstractReadWriteLockManager(boolean trackLocks) {
		boolean trace = trackLocks || Properties.lockTrackingEnabled();
	}

	/*
	 * --------- Methods ---------
	 */

	/**
	 * If a writer is active
	 */
	protected boolean isWriterActive() {
		return lock.isWriteLocked();
	}

	/**
	 * If one or more readers are active
	 */
	protected boolean isReaderActive() {
		long unlockedSum = readersUnlocked.sum();
		long lockedSum = readersLocked.sum();
		return unlockedSum != lockedSum;
	}

	/**
	 * Blocks current thread until after the writer lock is released (if active).
	 *
	 * @throws InterruptedException
	 */
	protected void waitForActiveWriter() throws InterruptedException {
		while (lock.isWriteLocked()) {
			Thread.yield();
		}
	}

	/**
	 * Blocks current thread until there are no reader locks active.
	 *
	 * @throws InterruptedException
	 */
	protected void waitForActiveReaders() throws InterruptedException {
		while (isReaderActive()) {
			Thread.yield();
		}
	}

	/**
	 * Creates a new Lock for reading and increments counter for active readers. The lock is tracked if lock tracking is
	 * enabled. This method is not thread safe itself, the calling method is expected to handle synchronization issues.
	 *
	 * @return a read lock.
	 */
	protected Lock createReadLock() {
		while (true) {
			readersLocked.increment();
			if (!lock.isWriteLocked()) {
				// Acquired lock in read-only mode
				break;
			} else {
				// Rollback logical counter to avoid blocking a Writer
				readersUnlocked.increment();
				// If there is a Writer, wait until it is gone
				while (lock.isWriteLocked()) {
					Thread.yield();
				}
			}
		}

		return new Lock() {

			boolean locked = true;

			@Override
			public boolean isActive() {
				return locked;
			}

			@Override
			public void release() {
				if (isActive()) {
					readersUnlocked.increment();
					locked = false;
				}
			}
		};
	}

	/**
	 * Creates a new Lock for writing. The lock is tracked if lock tracking is enabled. This method is not thread safe
	 * itself for performance reasons, the calling method is expected to handle synchronization issues.
	 *
	 * @return a write lock.
	 */
	protected Lock createWriteLock() {

		long writeStamp = lock.writeLock();
		int counter = 0;
		while (true) {
			long unlockedSum = readersUnlocked.sum();
			long lockedSum = readersLocked.sum();
			if (unlockedSum == lockedSum) {
				break;
			}
			if (READ_PREFERENCE > 0 && ++counter % READ_PREFERENCE == 0) {
				lock.unlockWrite(writeStamp);
				Thread.yield();
				writeStamp = lock.writeLock();
			} else {
				Thread.yield();
			}
		}

		long finalWriteStamp = writeStamp;

		return new Lock() {

			long stamp = finalWriteStamp;

			@Override
			public boolean isActive() {
				return stamp != 0;
			}

			@Override
			public void release() {
				if (isActive()) {
					lock.unlockWrite(stamp);
					stamp = 0;
				}
			}
		};
	}
}
