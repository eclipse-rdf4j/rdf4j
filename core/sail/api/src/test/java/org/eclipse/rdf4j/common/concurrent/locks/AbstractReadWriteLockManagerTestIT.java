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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Tag("slow")
abstract class AbstractReadWriteLockManagerTestIT {

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
	@Timeout(30)
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
		assertFalse(lockManager.isReaderActive());
		assertFalse(lockManager.isWriterActive());

	}

	private void runAsThreads(Stream<Runnable> runnableStream) {
		runnableStream.parallel().forEach(Runnable::run);
	}

	@Test
	@Timeout(30)
	void writeLockShouldSucceed() throws InterruptedException {

		Runnable runnable = () -> {
			while (true) {
				Lock readLock = null;
				try {
					readLock = lockManager.getReadLock();
					Thread.yield();
					if (Thread.interrupted()) {
						break;
					}
				} catch (InterruptedException e) {
					break;
				} finally {
					if (readLock != null) {
						readLock.release();
					}
				}

			}
		};

		List<Thread> threads = Arrays.asList(
				new Thread(runnable),
				new Thread(runnable),
				new Thread(runnable),
				new Thread(runnable)
		);

		try {
			threads.forEach(thread -> {
				thread.setDaemon(true);
				thread.start();
			});

			do {
				Thread.sleep(10);
			} while (!lockManager.isReaderActive());

			Lock writeLock = lockManager.getWriteLock();
			writeLock.release();

		} finally {
			for (Thread thread : threads) {
				TestHelper.interruptAndJoin(thread);
			}
		}
	}

}
