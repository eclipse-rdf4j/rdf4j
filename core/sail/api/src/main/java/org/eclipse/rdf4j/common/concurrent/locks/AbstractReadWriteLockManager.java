/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.StampedLock;

import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockCleaner;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockDiagnostics;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockMonitoring;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockTracking;
import org.eclipse.rdf4j.query.algebra.Var;
import org.slf4j.LoggerFactory;

/**
 * An abstract base implementation of a read/write-lock manager.
 *
 * @author Håvard M. Ottestad
 */
public abstract class AbstractReadWriteLockManager implements ReadWriteLockManager {

	private final LockMonitoring<ReadLock> readLockMonitoring;
	private final LockMonitoring<WriteLock> writeLockMonitoring;

	// StampedLock for handling writers.
	final StampedLock stampedLock = new StampedLock();

	// LongAdder for handling readers. When the count is equal then there are no active readers.
	final LongAdder readersLocked = new LongAdder();
	final LongAdder readersUnlocked = new LongAdder();

	// milliseconds to wait when calling the try-lock method of the stamped lock
	private final int tryWriteLockMillis;

	/**
	 * When acquiring a write-lock, the thread will acquire the write-lock and then spin & yield while waiting for
	 * readers to unlock their locks. A deadlock is possible if someone already holding a read-lock acquires another
	 * read-lock at the same time that another thread is waiting for a write-lock. To stop this from happening we can
	 * set READ_PREFERENCE to a number higher than zero. READ_PREFERENCE of 1 means that the thread acquiring a
	 * write-lock will release the write-lock if there are any readers. A READ_PREFERENCE of 100 means that the thread
	 * acquiring a write-lock will spin & yield 100 times before it attempts to release the write-lock.
	 */
	final int writePreference;

	public AbstractReadWriteLockManager() {
		this(false);
	}

	public AbstractReadWriteLockManager(boolean trackLocks) {
		this(trackLocks, LockMonitoring.INITIAL_WAIT_TO_COLLECT);
	}

	public AbstractReadWriteLockManager(boolean trackLocks, int waitToCollect) {
		this("", waitToCollect, LockDiagnostics.fromLegacyTracking(trackLocks));
	}

	public AbstractReadWriteLockManager(String alias, LockDiagnostics... lockDiagnostics) {
		this(alias, LockMonitoring.INITIAL_WAIT_TO_COLLECT, lockDiagnostics);
	}

	public AbstractReadWriteLockManager(String alias, int waitToCollect, LockDiagnostics... lockDiagnostics) {

		this.tryWriteLockMillis = Math.min(1000, waitToCollect);

		// WRITE_PREFERENCE can not be negative or 0.
		this.writePreference = Math.max(1, getWriterPreference());

		boolean releaseAbandoned = false;
		boolean detectStalledOrDeadlock = false;
		boolean stackTrace = false;

		for (LockDiagnostics lockDiagnostic : lockDiagnostics) {
			switch (lockDiagnostic) {
			case releaseAbandoned:
				releaseAbandoned = true;
				break;
			case detectStalledOrDeadlock:
				detectStalledOrDeadlock = true;
				break;
			case stackTrace:
				stackTrace = true;
				break;
			}
		}

		if (lockDiagnostics.length == 0) {
			readLockMonitoring = LockMonitoring
					.wrap(Lock.ExtendedSupplier.wrap(this::createReadLockInner, this::tryReadLockInner));
			writeLockMonitoring = LockMonitoring
					.wrap(Lock.ExtendedSupplier.wrap(this::createWriteLockInner, this::tryWriteLockInner));

		} else if (releaseAbandoned && !detectStalledOrDeadlock) {

			readLockMonitoring = new LockCleaner<>(
					stackTrace,
					alias + "_READ",
					LoggerFactory.getLogger(this.getClass()),
					Lock.ExtendedSupplier.wrap(this::createReadLockInner, this::tryReadLockInner)
			);

			writeLockMonitoring = new LockCleaner<>(
					stackTrace,
					alias + "_WRITE",
					LoggerFactory.getLogger(this.getClass()),
					Lock.ExtendedSupplier.wrap(this::createWriteLockInner, this::tryWriteLockInner)
			);

		} else {

			readLockMonitoring = new LockTracking<>(
					stackTrace,
					alias + "_READ",
					LoggerFactory.getLogger(this.getClass()),
					waitToCollect,
					Lock.ExtendedSupplier.wrap(this::createReadLockInner, this::tryReadLockInner)
			);

			writeLockMonitoring = new LockTracking<>(
					stackTrace,
					alias + "_WRITE",
					LoggerFactory.getLogger(this.getClass()),
					waitToCollect,
					Lock.ExtendedSupplier.wrap(this::createWriteLockInner, this::tryWriteLockInner)
			);
		}
	}

	abstract int getWriterPreference();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isWriterActive() {
		return stampedLock.isWriteLocked();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isReaderActive() {
		long unlockedSum = readersUnlocked.sum();
		long lockedSum = readersLocked.sum();
		return unlockedSum != lockedSum;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void waitForActiveWriter() throws InterruptedException {
		while (stampedLock.isWriteLocked() && !isReaderActive()) {
			spinWait();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void waitForActiveReaders() throws InterruptedException {
		while (isReaderActive()) {
			spinWait();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Lock getReadLock() throws InterruptedException {
		return readLockMonitoring.getLock();
	}

	ReadLock createReadLockInner() throws InterruptedException {

		readersLocked.increment();
		while (stampedLock.isWriteLocked()) {
			try {
				spinWaitAtReadLock();
			} catch (InterruptedException e) {
				readersUnlocked.increment();
				throw e;
			}
		}

		return new ReadLock(readersUnlocked);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Lock getWriteLock() throws InterruptedException {
		return writeLockMonitoring.getLock();
	}

	private WriteLock createWriteLockInner() throws InterruptedException {

		// Acquire a write-lock.
		long writeStamp = writeLockInterruptibly();
		boolean lockAcquired = false;

		try {
			int attempts = 0;

			// Wait for active readers to finish.
			do {

				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				// The order is important here.
				long unlockedSum = readersUnlocked.sum();
				long lockedSum = readersLocked.sum();
				if (unlockedSum == lockedSum) {
					// No active readers.
					lockAcquired = true;
				} else {

					// If a thread is allowed to acquire more than one read-lock then we could deadlock if we keep
					// holding the write-lock while we wait for all readers to finish. This is because no read-locks can
					// be acquired while the write-lock is locked.
					if (attempts++ > writePreference) {
						attempts = 0;

						stampedLock.unlockWrite(writeStamp);
						writeStamp = 0;

						yieldWait();

						writeStamp = writeLockInterruptibly();
					} else {
						spinWait();
					}

				}

			} while (!lockAcquired);
		} finally {
			if (!lockAcquired && writeStamp != 0) {
				stampedLock.unlockWrite(writeStamp);
				writeStamp = 0;
			}
		}

		VarHandle.releaseFence();
		return new WriteLock(stampedLock, writeStamp);
	}

	private long writeLockInterruptibly() throws InterruptedException {

		if (writeLockMonitoring.requiresManualCleanup()) {
			long writeStamp;
			do {
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				writeStamp = stampedLock.tryWriteLock(tryWriteLockMillis, TimeUnit.MILLISECONDS);

				if (writeStamp == 0) {

					writeLockMonitoring.runCleanup();
					readLockMonitoring.runCleanup();
				}
			} while (writeStamp == 0);
			return writeStamp;
		} else {
			return stampedLock.writeLockInterruptibly();
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Lock tryReadLock() {
		return readLockMonitoring.tryLock();
	}

	private ReadLock tryReadLockInner() {
		readersLocked.increment();
		if (!stampedLock.isWriteLocked()) {
			// Everything is good! We have acquired a read-lock and there are no active writers.
			return new ReadLock(readersUnlocked);
		} else {
			// There are active writers release our read lock
			readersUnlocked.increment();

			readLockMonitoring.runCleanup();
			writeLockMonitoring.runCleanup();
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Lock tryWriteLock() {
		return writeLockMonitoring.tryLock();
	}

	private WriteLock tryWriteLockInner() {
		// Try to acquire a write-lock.
		long writeStamp = stampedLock.tryWriteLock();

		if (writeStamp != 0) {

			// The order is important here.
			long unlockedSum = readersUnlocked.sum();
			long lockedSum = readersLocked.sum();
			if (unlockedSum == lockedSum) {
				// No active readers.
				VarHandle.releaseFence();
				return new WriteLock(stampedLock, writeStamp);
			} else {
				stampedLock.unlockWrite(writeStamp);

				readLockMonitoring.runCleanup();
				writeLockMonitoring.runCleanup();
			}
		}

		return null;
	}

	void spinWait() throws InterruptedException {
		Thread.onSpinWait();

		writeLockMonitoring.runCleanup();
		readLockMonitoring.runCleanup();

		if (Thread.interrupted()) {
			throw new InterruptedException();
		}

	}

	void spinWaitAtReadLock() throws InterruptedException {
		Thread.onSpinWait();

		writeLockMonitoring.runCleanup();

		if (Thread.interrupted()) {
			throw new InterruptedException();
		}

	}

	private void yieldWait() throws InterruptedException {
		Thread.yield();

		writeLockMonitoring.runCleanup();
		readLockMonitoring.runCleanup();

		if (Thread.interrupted()) {
			throw new InterruptedException();
		}

	}

	static class WriteLock implements Lock {

		private final StampedLock lock;
		private long stamp;

		public WriteLock(StampedLock lock, long stamp) {
			assert stamp != 0;
			this.lock = lock;
			this.stamp = stamp;
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

	static class ReadLock implements Lock {

		private final LongAdder readersUnlocked;
		private boolean locked = true;

		public ReadLock(LongAdder readersUnlocked) {
			this.readersUnlocked = readersUnlocked;
		}

		@Override
		public boolean isActive() {
			return locked;
		}

		@Override
		public void release() {
			if (!locked) {
				throw new IllegalMonitorStateException("Trying to release a lock that is not locked");
			}

			VarHandle.acquireFence();
			locked = false;
			readersUnlocked.increment();

		}
	}

}
