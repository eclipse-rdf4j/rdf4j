/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockCleaner;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockDiagnostics;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockMonitoring;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockTracking;
import org.slf4j.LoggerFactory;

/**
 * A read/write-lock manager backed by a StampedLock.
 *
 * This class is in an experimental state: its existence, signature or behavior may change without warning from one
 * release to the next.
 *
 * @author Håvard M. Ottestad
 */
@Experimental
public class StampedLockManager implements ReadWriteLockManager {

	private final LockMonitoring<ReadLock> readLockMonitoring;
	private final LockMonitoring<WriteLock> writeLockMonitoring;

	final StampedLock stampedLock = new StampedLock();

	// milliseconds to wait when calling the try-lock method of the stamped lock
	private final int tryWriteLockMillis;

	public StampedLockManager() {
		this(false);
	}

	public StampedLockManager(boolean trackLocks) {
		this(trackLocks, LockMonitoring.INITIAL_WAIT_TO_COLLECT);
	}

	public StampedLockManager(boolean trackLocks, int waitToCollect) {
		this("", waitToCollect, LockDiagnostics.fromLegacyTracking(trackLocks));
	}

	public StampedLockManager(String alias, LockDiagnostics... lockDiagnostics) {
		this(alias, LockMonitoring.INITIAL_WAIT_TO_COLLECT, lockDiagnostics);
	}

	public StampedLockManager(String alias, int waitToCollect, LockDiagnostics... lockDiagnostics) {

		this.tryWriteLockMillis = Math.min(1000, waitToCollect);

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

			readLockMonitoring = new LockCleaner(
					stackTrace,
					alias + "_READ",
					LoggerFactory.getLogger(this.getClass()),
					Lock.ExtendedSupplier.wrap(this::createReadLockInner, this::tryReadLockInner)
			);

			writeLockMonitoring = new LockCleaner(
					stackTrace,
					alias + "_WRITE",
					LoggerFactory.getLogger(this.getClass()),
					Lock.ExtendedSupplier.wrap(this::createWriteLockInner, this::tryWriteLockInner)
			);

		} else {

			readLockMonitoring = new LockTracking(
					stackTrace,
					alias + "_READ",
					LoggerFactory.getLogger(this.getClass()),
					waitToCollect,
					Lock.ExtendedSupplier.wrap(this::createReadLockInner, this::tryReadLockInner)
			);

			writeLockMonitoring = new LockTracking(
					stackTrace,
					alias + "_WRITE",
					LoggerFactory.getLogger(this.getClass()),
					waitToCollect,
					Lock.ExtendedSupplier.wrap(this::createWriteLockInner, this::tryWriteLockInner)
			);
		}
	}

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
		return stampedLock.isReadLocked();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void waitForActiveWriter() throws InterruptedException {
		while (isWriterActive()) {
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

	private ReadLock createReadLockInner() throws InterruptedException {
		return new ReadLock(stampedLock, stampedLock.readLockInterruptibly());
	}

	/**
	 * Gets an optimistic read lock, if available. This method will return <var>null</var> if the optimistic read lock
	 * is not immediately available.
	 */
	public OptimisticReadLock getOptimisticReadLock() {
		long optimisticReadStamp = stampedLock.tryOptimisticRead();
		if (optimisticReadStamp != 0) {
			return new OptimisticReadLock(stampedLock, optimisticReadStamp);
		}
		return null;
	}

	/**
	 * Convert a write lock to a read lock.
	 */
	public Lock convertToReadLock(Lock writeLock) {

		WriteLock innerWriteLock = writeLockMonitoring.unsafeInnerLock(writeLock);

		long readLockStamp = stampedLock.tryConvertToReadLock(innerWriteLock.stamp);
		innerWriteLock.stamp = 0;
		if (readLockStamp == 0) {
			throw new IllegalMonitorStateException("Lock is not a locked write lock.");
		}
		ReadLock readLock = new ReadLock(stampedLock, readLockStamp);
		try {
			Lock registered = readLockMonitoring.register(readLock);
			writeLockMonitoring.unregister(writeLock);
			return registered;
		} catch (Throwable t) {
			readLock.release();
			throw t;
		}
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
		long stamp = stampedLock.tryReadLock();
		if (stamp != 0) {
			return new ReadLock(stampedLock, stamp);
		} else {
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
		long stamp = stampedLock.tryWriteLock();
		if (stamp != 0) {
			return new WriteLock(stampedLock, stamp);
		} else {
			return null;
		}

	}

	private void spinWait() throws InterruptedException {
		Thread.onSpinWait();

		writeLockMonitoring.runCleanup();
		readLockMonitoring.runCleanup();

		if (Thread.interrupted()) {
			throw new InterruptedException();
		}

	}

	private static class WriteLock implements Lock {

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

	private static class ReadLock implements Lock {

		private final StampedLock stampedLock;
		private final long stamp;
		private boolean locked = true;

		public ReadLock(StampedLock stampedLock, long stamp) {
			this.stampedLock = stampedLock;
			this.stamp = stamp;
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

			locked = false;
			stampedLock.unlockRead(stamp);
		}
	}

	public static class OptimisticReadLock implements Lock {

		private final StampedLock stampedLock;
		private final long optimisticReadStamp;

		public OptimisticReadLock(StampedLock stampedLock, long stamp) {
			this.stampedLock = stampedLock;
			this.optimisticReadStamp = stamp;
		}

		@Override
		public boolean isActive() {
			return stampedLock.validate(optimisticReadStamp);
		}

		@Override
		public void release() {
			// no-op
		}
	}

	public static class Cache<T> {
		private final Supplier<T> dataSupplier;
		private volatile T data;
		private final StampedLockManager stampedLockManager;

		public Cache(StampedLockManager stampedLockManager, Supplier<T> dataSupplier) {
			this.dataSupplier = dataSupplier;
			this.stampedLockManager = stampedLockManager;
		}

		public ReadableState getReadState() throws InterruptedException {
			Lock readLock = refreshCacheIfNeeded(stampedLockManager.getReadLock());
			return new ReadableState(data, readLock);
		}

		private Lock refreshCacheIfNeeded(Lock readLock) throws InterruptedException {
			if (data == null) {
				readLock.release();
				Lock writeLock = stampedLockManager.getWriteLock();
				try {
					if (data == null) {
						data = dataSupplier.get();
					}
					readLock = stampedLockManager.convertToReadLock(writeLock);
				} catch (Throwable t) {
					if (writeLock.isActive()) {
						writeLock.release();
					}
					throw t;
				}
			}
			return readLock;
		}

		private void refreshCacheIfNeeded() throws InterruptedException {
			if (data == null) {
				Lock writeLock = null;
				try {
					writeLock = stampedLockManager.getWriteLock();

					if (data == null) {
						data = dataSupplier.get();
					}

				} finally {
					if (writeLock != null) {
						writeLock.release();
					}
				}
			}
		}

		public WritableState getWriteState() throws InterruptedException {
			Lock writeLock = stampedLockManager.getWriteLock();
			return new WritableState(writeLock);
		}

		public OptimisticState getOptimisticState() throws InterruptedException {
			refreshCacheIfNeeded();

			OptimisticReadLock optimisticReadLock = stampedLockManager.getOptimisticReadLock();
			if (optimisticReadLock == null) {
				return new OptimisticState();
			}
			return new OptimisticState(this.data, stampedLockManager, optimisticReadLock);

		}

		public void warmUp() throws InterruptedException {
			assert this.data == null;

			try (WritableState writeState = getWriteState()) {
				T data = writeState.getData();
				assert this.data != null;
				assert this.data == data;
			}

		}

		public class OptimisticState {
			T data;
			StampedLockManager stampedLockManager;
			OptimisticReadLock lock;

			OptimisticState(T data, StampedLockManager stampedLockManager, OptimisticReadLock lock) {
				this.data = data;
				assert this.data != null;
				this.stampedLockManager = stampedLockManager;
				this.lock = lock;
			}

			public OptimisticState() {
				this.data = null;
				this.stampedLockManager = null;
				this.lock = null;
			}

			public boolean isValid() {
				return lock != null && lock.isActive();
			}

			public T getData() {
				assert isValid();
				return data;
			}

		}

		public class ReadableState implements AutoCloseable {
			T data;
			Lock readLock;

			ReadableState(T data, Lock readLock) {
				this.data = data;
				this.readLock = readLock;
			}

			@Override
			public void close() {
				readLock.release();
			}

			public T getData() {
				if (!readLock.isActive()) {
					throw new IllegalMonitorStateException("Read lock has been released");
				}
				return data;
			}

			public T getDataAndRelease() {
				if (!readLock.isActive()) {
					throw new IllegalMonitorStateException("Read lock has been released");
				}
				readLock.release();
				return data;
			}
		}

		public class WritableState implements AutoCloseable {
			Lock writeLock;
			private boolean purged;

			WritableState(Lock writeLock) {
				this.writeLock = writeLock;
			}

			public void purge() {
				if (!writeLock.isActive()) {
					throw new IllegalMonitorStateException("Write lock has been released");
				}
				purged = true;
				data = null;
			}

			T getData() {
				if (!writeLock.isActive()) {
					throw new IllegalMonitorStateException("Write lock has been released");
				}
				if (purged) {
					throw new IllegalMonitorStateException("Cache was previously purged by this object.");
				}
				if (data == null) {
					data = dataSupplier.get();
				}
				assert data != null;
				return data;
			}

			@Override
			public void close() {
				writeLock.release();
			}
		}

	}

}
