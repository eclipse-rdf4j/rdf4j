/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

abstract class AbstractReadWriteLockManagerTest {

	AbstractReadWriteLockManager lockManager;
	AbstractReadWriteLockManager lockManagerReleaseAbandoned;
	AbstractReadWriteLockManager lockManagerReleaseAbandonedStackTrace;
	AbstractReadWriteLockManager lockManagerTracking;

	private MemoryAppender memoryAppender;

	private String className;

	@BeforeEach
	void beforeEach() {
		if (memoryAppender != null) {
			memoryAppender.stop();
		}

		Stream.of(lockManager, lockManagerReleaseAbandoned, lockManagerReleaseAbandonedStackTrace, lockManagerTracking)
				.filter(Objects::nonNull)
				.forEach(l -> {
					assertFalse(l.isWriterActive());
					assertFalse(l.isReaderActive());
				});

		lockManager = null;
		lockManagerReleaseAbandoned = null;
		lockManagerReleaseAbandonedStackTrace = null;
		lockManagerTracking = null;

		setUpLockManagers();

		assertNotNull(lockManager);
		assertNotNull(lockManagerReleaseAbandoned);
		assertNotNull(lockManagerReleaseAbandonedStackTrace);
		assertNotNull(lockManagerTracking);

		assertEquals(lockManager.getClass(), lockManagerReleaseAbandoned.getClass());
		assertEquals(lockManagerReleaseAbandoned.getClass(), lockManagerReleaseAbandonedStackTrace.getClass());
		assertEquals(lockManagerReleaseAbandonedStackTrace.getClass(), lockManagerTracking.getClass());

		className = lockManager.getClass().getName();

		Logger logger = (Logger) LoggerFactory.getLogger(lockManager.getClass());
		memoryAppender = new MemoryAppender();
		memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		logger.detachAndStopAllAppenders();
		logger.setLevel(Level.INFO);
		logger.addAppender(memoryAppender);
		memoryAppender.start();
	}

	abstract void setUpLockManagers();

	@Test
	void createWriteLock() throws InterruptedException {
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
	void tryWriteLock() {
		Lock lock = lockManager.tryWriteLock();
		assertTrue(lock.isActive());
		lock.release();
		assertFalse(lock.isActive());
	}

	@Test
	void tryReadLock() {
		Lock lock = lockManager.tryReadLock();
		Lock lock2 = lockManager.tryReadLock();
		assertTrue(lock.isActive());
		lock.release();
		assertFalse(lock.isActive());
		assertTrue(lock2.isActive());
		lock2.release();
	}

	@Test
	@Timeout(2)
	void cleanupUnreleasedLocks() throws InterruptedException {

		writeLock(lockManagerReleaseAbandoned);

		TestHelper.callGC(lockManagerReleaseAbandoned);

		Lock writeLock = lockManagerReleaseAbandoned.getWriteLock();
		writeLock.release();

	}

	@Test
	@Timeout(2)
	void cleanupUnreleasedReadLocks() throws InterruptedException {

		readLock(lockManagerReleaseAbandoned);

		TestHelper.callGC(lockManagerReleaseAbandoned);

		Lock writeLock = lockManagerReleaseAbandoned.getWriteLock();
		writeLock.release();

	}

	@Test
	@Timeout(2)
	void cleanupUnreleasedWriteLocksWithStackTrace() throws InterruptedException {

		writeLock(lockManagerReleaseAbandonedStackTrace);

		TestHelper.callGC(lockManagerReleaseAbandonedStackTrace);

		Lock writeLock = lockManagerReleaseAbandonedStackTrace.getWriteLock();
		writeLock.release();
		memoryAppender.waitForEvents();
		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains("\"_WRITE\" lock abandoned; lock was acquired in main", Level.WARN);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.cleanupUnreleasedWriteLocksWithStackTrace",
				Level.WARN);

	}

	@Test
	@Timeout(2)
	void cleanupUnreleasedTryWriteLocksWithStackTrace() throws InterruptedException {

		writeLockTry(lockManagerReleaseAbandonedStackTrace);

		TestHelper.callGC(lockManagerReleaseAbandonedStackTrace);

		Lock writeLock = lockManagerReleaseAbandonedStackTrace.getWriteLock();
		writeLock.release();
		memoryAppender.waitForEvents();
		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains("\"_WRITE\" lock abandoned; lock was acquired in main", Level.WARN);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.cleanupUnreleasedTryWriteLocksWithStackTrace",
				Level.WARN);

	}

	@Test
	@Timeout(2)
	void cleanupUnreleasedWriteLocksWithoutStackTrace() throws InterruptedException {

		writeLock(lockManagerReleaseAbandoned);

		TestHelper.callGC(lockManagerReleaseAbandoned);

		Lock writeLock = lockManagerReleaseAbandoned.getWriteLock();
		writeLock.release();
		memoryAppender.waitForEvents();
		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains("\"_WRITE\" lock abandoned; consider setting the ", Level.WARN);
		memoryAppender.assertNotContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.cleanupUnreleasedWriteLocksWithStackTrace",
				Level.WARN);
	}

	@Test
	@Timeout(2)
	void cleanupUnreleasedWriteLocksWithTracking() throws InterruptedException {

		writeLock(lockManagerTracking);

		Lock writeLock = lockManagerTracking.getWriteLock();
		writeLock.release();

		memoryAppender.waitForEvents();
		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);

		memoryAppender.assertContains("\"_WRITE\" lock abandoned; lock was acquired in main", Level.WARN);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.cleanupUnreleasedWriteLocksWithTracking(AbstractReadWriteLockManagerTest.java",
				Level.WARN);

	}

	@Test
	@Timeout(2)
	void cleanupUnreleasedReadLocksWithTracking() throws InterruptedException {

		readLock(lockManagerTracking);

		Lock writeLock = lockManagerTracking.getWriteLock();
		writeLock.release();
		memoryAppender.waitForEvents();

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.cleanupUnreleasedReadLocksWithTracking",
				Level.WARN);

	}

	@Test
	@Timeout(2)
	void cleanupUnreleasedTryReadLocksWithTracking() throws InterruptedException {

		readLockTry(lockManagerTracking);

		Lock writeLock = lockManagerTracking.getWriteLock();
		writeLock.release();
		memoryAppender.waitForEvents();

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.cleanupUnreleasedTryReadLocksWithTracking",
				Level.WARN);

	}

	@Test
	@Timeout(2)
	void stalledTestReadWrite() throws InterruptedException {

		Lock readLock = lockManagerTracking.getReadLock();
		Thread thread = null;
		try {
			thread = TestHelper.getStartedDaemonThread(lockManagerTracking::getWriteLock);
			memoryAppender.waitForEvents();
		} finally {
			TestHelper.interruptAndJoin(thread);
		}

		readLock.release();

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains(" is waiting on a possibly stalled lock \"_READ\" with id ", Level.INFO);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.stalledTestReadWrite(AbstractReadWriteLockManagerTest.java:",
				Level.INFO);
	}

	@Test
	@Timeout(2)
	void stalledTestWriteRead() throws InterruptedException {

		Lock writeLock = lockManagerTracking.getWriteLock();
		Thread thread = null;
		try {
			thread = TestHelper.getStartedDaemonThread(lockManagerTracking::getReadLock);
			memoryAppender.waitForEvents();
		} finally {
			TestHelper.interruptAndJoin(thread);
		}

		writeLock.release();

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains(" is waiting on a possibly stalled lock \"_WRITE\" with id ", Level.INFO);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.stalledTestWriteRead(AbstractReadWriteLockManagerTest.java:",
				Level.INFO);

	}

	@Test
	@Timeout(2)
	void stalledTestWriteWrite() throws InterruptedException {
		Lock writeLock = lockManagerTracking.getWriteLock();
		Thread thread = null;
		try {
			thread = TestHelper.getStartedDaemonThread(lockManagerTracking::getWriteLock);
			memoryAppender.waitForEvents();
		} finally {
			TestHelper.interruptAndJoin(thread);
		}

		writeLock.release();

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains(" is waiting on a possibly stalled lock \"_WRITE\" with id ", Level.INFO);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.stalledTestWriteWrite(AbstractReadWriteLockManagerTest.java:",
				Level.INFO);
	}

	@Test
	@Timeout(2)
	void deadlockTestReadWrite() throws InterruptedException {

		Thread thread = null;
		try {
			thread = TestHelper.getStartedDaemonThread(lockManagerTracking::getReadLock,
					lockManagerTracking::getWriteLock);
			memoryAppender.waitForEvents();
		} finally {
			TestHelper.interruptAndJoin(thread);
		}

		Lock writeLock = lockManagerTracking.getWriteLock();
		assertTrue(writeLock.isActive());
		writeLock.release();

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains("is possibly deadlocked waiting on \"_READ\" with id ", Level.WARN);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.TestHelper.lambda$getStartedDaemonThread", Level.WARN);

	}

	@Test
	@Timeout(2)
	void deadlockTestWriteRead() throws InterruptedException {

		Thread thread = null;
		try {
			thread = TestHelper.getStartedDaemonThread(lockManagerTracking::getWriteLock,
					lockManagerTracking::getReadLock);
			memoryAppender.waitForEvents();
		} finally {
			TestHelper.interruptAndJoin(thread);
		}

		Lock writeLock = lockManagerTracking.getWriteLock();
		assertTrue(writeLock.isActive());
		writeLock.release();

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains("is possibly deadlocked waiting on \"_WRITE\" with id ", Level.WARN);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.TestHelper.lambda$getStartedDaemonThread", Level.WARN);

	}

	@Test
	@Timeout(2)
	void deadlockTestWriteWrite() throws InterruptedException {

		Thread thread = null;
		try {
			thread = TestHelper.getStartedDaemonThread(lockManagerTracking::getWriteLock,
					lockManagerTracking::getWriteLock);
			memoryAppender.waitForEvents();
		} finally {
			TestHelper.interruptAndJoin(thread);
		}

		Lock writeLock = lockManagerTracking.getWriteLock();
		assertTrue(writeLock.isActive());
		writeLock.release();

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains("is possibly deadlocked waiting on \"_WRITE\" with id ", Level.WARN);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.TestHelper.lambda$getStartedDaemonThread", Level.WARN);

	}

	@Test
	@Timeout(2)
	void interruptWaitForActiveWriter() throws InterruptedException {

		Lock lock = lockManagerTracking.getWriteLock();

		Thread thread = null;
		try {
			thread = TestHelper.getStartedDaemonThread(lockManagerTracking::waitForActiveWriter);
			memoryAppender.waitForEvents();
		} finally {
			TestHelper.interruptAndJoin(thread);
		}

		lock.release();

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains("is waiting on a possibly stalled lock \"_WRITE\" with id", Level.INFO);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.interruptWaitForActiveWriter(AbstractReadWriteLockManagerTest.java:",
				Level.INFO);

	}

	@Test
	@Timeout(2)
	void waitForActiveWriter() throws InterruptedException {

		Lock lock = lockManagerTracking.getWriteLock();

		Thread thread = null;
		try {
			thread = TestHelper.getStartedDaemonThread(lockManagerTracking::waitForActiveWriter);
			memoryAppender.waitForEvents();
			lock.release();
		} finally {
			TestHelper.join(thread);
		}

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains("is waiting on a possibly stalled lock \"_WRITE\" with id", Level.INFO);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.waitForActiveWriter(AbstractReadWriteLockManagerTest.java:",
				Level.INFO);
	}

	@Test
	@Timeout(2)
	void interruptWaitForActiveReader() throws InterruptedException {

		Lock lock = lockManagerTracking.getReadLock();

		Thread thread = null;
		try {
			thread = TestHelper.getStartedDaemonThread(lockManagerTracking::waitForActiveReaders);
			memoryAppender.waitForEvents();
		} finally {
			TestHelper.interruptAndJoin(thread);
		}

		lock.release();

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains("is waiting on a possibly stalled lock \"_READ\" with id", Level.INFO);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.interruptWaitForActiveReader(AbstractReadWriteLockManagerTest.java:",
				Level.INFO);

	}

	@Test
	@Timeout(2)
	void waitForActiveReader() throws InterruptedException {

		Lock writeLock = lockManagerTracking.getReadLock();

		Thread thread = null;
		try {
			thread = TestHelper.getStartedDaemonThread(lockManagerTracking::waitForActiveReaders);
			memoryAppender.waitForEvents();
			writeLock.release();
		} finally {
			TestHelper.join(thread);
		}

		assertThat(memoryAppender.countEventsForLogger(className)).isEqualTo(1);
		memoryAppender.assertContains("is waiting on a possibly stalled lock \"_READ\" with id", Level.INFO);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManagerTest.waitForActiveReader(AbstractReadWriteLockManagerTest.java:",
				Level.INFO);
	}

	@Test
	void interruptTestWriteRead() throws InterruptedException {

		Lock writeLock = lockManager.getWriteLock();

		Thread thread1 = TestHelper.getStartedDaemonThread(lockManager::getReadLock);
		Thread thread2 = TestHelper.getStartedDaemonThread(lockManager::getReadLock);
		Thread thread3 = TestHelper.getStartedDaemonThread(lockManager::getReadLock);
		Thread thread4 = TestHelper.getStartedDaemonThread(lockManager::getReadLock);

		thread2.interrupt();
		thread4.interrupt();

		TestHelper.join(thread2);
		TestHelper.join(thread4);

		assertTrue(writeLock.isActive());
		writeLock.release();
		assertFalse(writeLock.isActive());

		TestHelper.join(thread1);
		TestHelper.join(thread3);
	}

	@Test
	void interruptTestWriteWrite() throws InterruptedException {

		Lock writeLock = lockManager.getWriteLock();

		Thread thread = TestHelper.getStartedDaemonThread(lockManager::getWriteLock);

		TestHelper.interruptAndJoin(thread);

		assertTrue(writeLock.isActive());
		writeLock.release();
		assertFalse(writeLock.isActive());

	}

	@Test
	void counter() {

		long[] counter = { 0, 0, 0 };

		ExecutorService executorService = Executors.newFixedThreadPool(8);

		Random random = new Random(475824);

		IntStream.range(0, 100)
				.mapToObj(i -> {
					int randomInt = random.nextInt(3);

					return new Runnable() {
						@Override
						public void run() {
							try {
								for (int j = 0; j < 1000; j++) {

									if (randomInt == 0) {
										long counter2 = counter[2];
										long counter0 = counter[0];
										long counter1 = counter[1];
										if (counter2 < counter1) {
											throw new IllegalStateException();
										}
										if (counter1 < counter0) {
											throw new IllegalStateException();
										}

										Lock writeLock = lockManager.getWriteLock();
										counter[2]++;
										writeLock.release();

									} else if (randomInt == 1) {
										long counter1 = counter[1];
										long counter0 = counter[0];
										long counter2 = counter[2];
										if (counter2 < counter1) {
											throw new IllegalStateException();
										}
										if (counter1 < counter0) {
											throw new IllegalStateException();
										}

										Lock writeLock = lockManager.getWriteLock();
										counter[1] = Math.max(counter[2], counter[1]);
										writeLock.release();

									} else if (randomInt == 2) {
										long counter2 = counter[2];
										long counter1 = counter[1];
										long counter0 = counter[0];
										if (counter2 < counter1) {
											throw new IllegalStateException();
										}
										if (counter1 < counter0) {
											throw new IllegalStateException();
										}

										Lock writeLock = lockManager.getWriteLock();
										counter[0] = Math.max(counter[1], counter[0]);
										writeLock.release();

									}

								}
							} catch (InterruptedException ignored) {
							}
						}
					};
				})
				.collect(Collectors.toList())
				.stream()
				.map(executorService::submit)
				.forEach(f -> {
					try {
						f.get();
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				});

		executorService.shutdownNow();

		assertEquals(34000, counter[0]);
		assertEquals(34000, counter[1]);
		assertEquals(35000, counter[2]);
	}

	@Test
	void interruptTestReadWrite() throws InterruptedException {

		Lock readLock = lockManagerTracking.getReadLock();

		Thread thread = TestHelper.getStartedDaemonThread(lockManagerTracking::getWriteLock);

		TestHelper.interruptAndJoin(thread);

		assertTrue(readLock.isActive());
		readLock.release();
		assertFalse(readLock.isActive());

	}

	private void writeLock(AbstractReadWriteLockManager lockManager) throws InterruptedException {
		Lock lock = lockManager.getWriteLock();
		assertTrue(lock.isActive());
	}

	private void readLock(AbstractReadWriteLockManager lockManager) throws InterruptedException {
		Lock lock = lockManager.getReadLock();
		assertTrue(lock.isActive());
	}

	private void writeLockTry(AbstractReadWriteLockManager lockManager) throws InterruptedException {
		Lock lock = lockManager.tryWriteLock();
		assertNotNull(lock);
		assertTrue(lock.isActive());
	}

	private void readLockTry(AbstractReadWriteLockManager lockManager) throws InterruptedException {
		Lock lock = lockManager.tryReadLock();
		assertNotNull(lock);
		assertTrue(lock.isActive());
	}

}
