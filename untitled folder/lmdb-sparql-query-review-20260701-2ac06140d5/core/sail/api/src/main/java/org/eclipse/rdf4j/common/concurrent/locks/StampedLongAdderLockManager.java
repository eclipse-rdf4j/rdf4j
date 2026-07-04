/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.concurrent.locks;

import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.StampedLock;

/**
 * A lightweight read/write lock manager that avoids allocating per-lock objects. Readers are tracked using two
 * {@link LongAdder LongAdders} while writers rely on a {@link StampedLock}. The read-lock method returns a constant
 * stamp ({@link #READ_LOCK_STAMP}) and writers receive the stamp produced by the underlying {@link StampedLock}.
 */
public class StampedLongAdderLockManager {

	/**
	 * Stamp returned to callers holding a read lock. Passing any other value to {@link #unlockRead(long)} is considered
	 * an illegal monitor state.
	 */
	public static final long READ_LOCK_STAMP = Long.MIN_VALUE;

	private final StampedLock stampedLock = new StampedLock();
	private final LongAdder readersLocked = new LongAdder();
	private final LongAdder readersUnlocked = new LongAdder();

	// milliseconds to wait when trying to acquire the write lock interruptibly
	private final int tryWriteLockMillis;

	// Number of spin attempts before temporarily releasing the write lock when readers are active.
	private final int writePreference;

	public StampedLongAdderLockManager() {
		this(1, 100);
	}

	public StampedLongAdderLockManager(int writePreference, int tryWriteLockMillis) {
		this.writePreference = Math.max(1, writePreference);
		this.tryWriteLockMillis = Math.max(1, tryWriteLockMillis);
	}

	public boolean isWriterActive() {
		return stampedLock.isWriteLocked();
	}

	public boolean isReaderActive() {
		return readersUnlocked.sum() != readersLocked.sum();
	}

	public void waitForActiveWriter() throws InterruptedException {
		while (stampedLock.isWriteLocked() && !isReaderActive()) {
			spinWait();
		}
	}

	public void waitForActiveReaders() throws InterruptedException {
		while (isReaderActive()) {
			spinWait();
		}
	}

	public long readLock() throws InterruptedException {
		readersLocked.increment();
		while (stampedLock.isWriteLocked()) {
			try {
				spinWaitAtReadLock();
			} catch (InterruptedException e) {
				readersUnlocked.increment();
				throw e;
			}
		}
		return READ_LOCK_STAMP;
	}

	public long tryReadLock() {
		readersLocked.increment();
		if (!stampedLock.isWriteLocked()) {
			return READ_LOCK_STAMP;
		}
		readersUnlocked.increment();
		return 0L;
	}

	public void unlockRead(long stamp) {
		if (stamp != READ_LOCK_STAMP) {
			throw new IllegalMonitorStateException("Trying to release a stamp that is not a read lock");
		}

		VarHandle.acquireFence();
		readersUnlocked.increment();
	}

	public long writeLock() throws InterruptedException {
		long writeStamp = writeLockInterruptibly();
		boolean lockAcquired = false;

		try {
			int attempts = 0;
			do {
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				if (!hasActiveReaders()) {
					lockAcquired = true;
					break;
				}

				if (attempts++ > writePreference) {
					attempts = 0;

					stampedLock.unlockWrite(writeStamp);
					writeStamp = 0;

					yieldWait();

					writeStamp = writeLockInterruptibly();
				} else {
					spinWait();
				}

			} while (!lockAcquired);
		} finally {
			if (!lockAcquired && writeStamp != 0) {
				stampedLock.unlockWrite(writeStamp);
			}
		}

		VarHandle.releaseFence();
		return writeStamp;
	}

	public long tryWriteLock() {
		long writeStamp = stampedLock.tryWriteLock();
		if (writeStamp == 0) {
			return 0L;
		}

		if (!hasActiveReaders()) {
			VarHandle.releaseFence();
			return writeStamp;
		}

		stampedLock.unlockWrite(writeStamp);
		return 0L;
	}

	public void unlockWrite(long stamp) {
		if (stamp == 0) {
			throw new IllegalMonitorStateException("Trying to release a write lock that is not locked");
		}
		stampedLock.unlockWrite(stamp);
	}

	private boolean hasActiveReaders() {
		return readersUnlocked.sum() != readersLocked.sum();
	}

	private long writeLockInterruptibly() throws InterruptedException {
		long writeStamp;
		do {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			writeStamp = stampedLock.tryWriteLock(tryWriteLockMillis, TimeUnit.MILLISECONDS);
		} while (writeStamp == 0);
		return writeStamp;
	}

	private void spinWait() throws InterruptedException {
		Thread.onSpinWait();
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
	}

	private void spinWaitAtReadLock() throws InterruptedException {
		Thread.onSpinWait();
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
	}

	private void yieldWait() throws InterruptedException {
		Thread.yield();
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
	}
}
