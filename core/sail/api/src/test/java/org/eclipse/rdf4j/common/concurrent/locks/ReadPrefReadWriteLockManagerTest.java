/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

class ReadPrefReadWriteLockManagerTest {

	private ReadPrefReadWriteLockManager lockManager;
	private ReadPrefReadWriteLockManager lockManagerTracking;
	private MemoryAppender memoryAppender;

	@BeforeEach
	void beforeEach() {
		Properties.setLockTrackingEnabled(false);
		lockManager = new ReadPrefReadWriteLockManager(false, 1);
		lockManagerTracking = new ReadPrefReadWriteLockManager(true, 1);

		Logger logger = (Logger) LoggerFactory.getLogger(LockManager.class.getName());
		memoryAppender = new MemoryAppender();
		memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		logger.setLevel(Level.INFO);
		logger.addAppender(memoryAppender);
		memoryAppender.start();
	}

	@Test
	void testMultipleReadLocksSameThread() {

		CountDownLatch acquireReadLockFirst = new CountDownLatch(1);

		Stream<Runnable> runnableStream = Stream.of(() -> {
			try {
				Lock readLock = lockManager.getReadLock();
				acquireReadLockFirst.countDown();

				// try to force the other thread to begin acquiring a write lock before we continue
				Thread.yield();
				Thread.sleep(100);

				Lock readLock2 = lockManager.getReadLock();

				readLock.release();
				readLock2.release();

			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}, () -> {

			try {
				acquireReadLockFirst.await();
				Lock writeLock = lockManager.getWriteLock();
				writeLock.release();
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}

		);

		runAsThreads(runnableStream);

	}

	private void runAsThreads(Stream<Runnable> runnableStream) {
		runnableStream.parallel().forEach(Runnable::run);
	}

	@Test
	void createLock() throws InterruptedException {
		Lock lock = lockManager.getWriteLock();
		assertTrue(lock.isActive());
		lock.release();
		assertFalse(lock.isActive());
	}

	@Test
	void createReadLock() throws InterruptedException {
		Lock lock = lockManager.getReadLock();
		Lock lock2 = lockManager.getReadLock();
		assertTrue(lock.isActive());
		lock.release();
		assertFalse(lock.isActive());
		assertTrue(lock2.isActive());
		lock2.release();
	}

	@Test
	@Timeout(10)
	void cleanupUnreleasedLocks() throws InterruptedException {

		writeLock(lockManager);

		System.gc();
		Thread.sleep(100);

		Lock writeLock = lockManager.getWriteLock();
		writeLock.release();

	}

	@Test
	@Timeout(10)
	void cleanupUnreleasedReadLocks() throws InterruptedException {

		readLock(lockManager);

		System.gc();
		Thread.sleep(100);

		Lock writeLock = lockManager.getWriteLock();
		writeLock.release();

	}

	@Test
	@Timeout(10)
	void cleanupUnreleasedLocksWithTracking() throws InterruptedException {

		writeLock(lockManagerTracking);

		System.gc();
		Thread.sleep(100);

		Lock writeLock = lockManagerTracking.getWriteLock();
		writeLock.release();

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		assertThat(memoryAppender.contains(
				"at org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManagerTest.cleanupUnreleasedLocksWithTracking",
				Level.WARN)).isTrue();

	}

	@Test
	@Timeout(10)
	void cleanupUnreleasedReadLocksWithTracking() throws InterruptedException {

		readLock(lockManagerTracking);

		System.gc();
		Thread.sleep(100);

		Lock writeLock = lockManagerTracking.getWriteLock();
		writeLock.release();

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		assertThat(memoryAppender.contains(
				"at org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManagerTest.cleanupUnreleasedReadLocksWithTracking",
				Level.WARN)).isTrue();

	}

	@Test
	@Timeout(10)
	void deadlockTest() throws InterruptedException {

		Lock readLock = lockManagerTracking.getReadLock();
		Thread thread = null;
		try {
			thread = new Thread(() -> {
				try {
					Lock writeLock = lockManagerTracking.getWriteLock();
					writeLock.release();
				} catch (InterruptedException ignored) {
				}
			});

			thread.setDaemon(true);
			thread.start();

			while (memoryAppender.getLoggedEvents().isEmpty()) {
				Thread.yield();
			}
		} finally {
			if (thread != null) {
				thread.interrupt();
			}
		}

		assertNull(lockManagerTracking.tryWriteLock());
		assertTrue(readLock.isActive());
		readLock.release();
		assertFalse(readLock.isActive());

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		assertThat(memoryAppender.contains("is waiting on an active Read lock acquired in main", Level.INFO)).isTrue();
		assertThat(memoryAppender.contains(
				"org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManagerTest.lambda$deadlockTest",
				Level.INFO)).isTrue();

	}

	@Test
	@Timeout(10)
	void deadlockTest2() throws InterruptedException {

		Lock writeLock = lockManagerTracking.getWriteLock();
		Thread thread = null;
		try {
			thread = new Thread(() -> {
				try {
					Lock readlock = lockManagerTracking.getReadLock();
					readlock.release();
				} catch (InterruptedException ignored) {
				}
			});

			thread.setDaemon(true);
			thread.start();

			while (memoryAppender.getLoggedEvents().isEmpty()) {
				Thread.yield();
			}
		} finally {
			if (thread != null) {
				thread.interrupt();
			}
		}

		assertNull(lockManagerTracking.tryWriteLock());
		assertTrue(writeLock.isActive());
		writeLock.release();
		assertFalse(writeLock.isActive());

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		assertThat(memoryAppender.contains("is waiting on an active Write lock acquired in main", Level.INFO)).isTrue();
		assertThat(memoryAppender.contains(
				"org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManagerTest.lambda$deadlockTest",
				Level.INFO)).isTrue();

	}

	private void writeLock(ReadPrefReadWriteLockManager lockManager) throws InterruptedException {
		Lock lock = lockManager.getWriteLock();
		assertTrue(lock.isActive());
	}

	private void readLock(ReadPrefReadWriteLockManager lockManager) throws InterruptedException {
		Lock lock = lockManager.getReadLock();
		assertTrue(lock.isActive());
	}
}
