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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

class ExclusiveLockManagerTest {

	private ExclusiveLockManager lockManager;
	private ExclusiveLockManager lockManagerTracking;
	private MemoryAppender memoryAppender;

	@BeforeEach
	void beforeEach() {
		Properties.setLockTrackingEnabled(false);
		lockManager = new ExclusiveLockManager(false, 1);
		lockManagerTracking = new ExclusiveLockManager(true, 1);

		Logger logger = (Logger) LoggerFactory.getLogger(LockManager.class.getName());
		memoryAppender = new MemoryAppender();
		memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		logger.setLevel(Level.INFO);
		logger.addAppender(memoryAppender);
		memoryAppender.start();

	}

	@Test
	void createLock() throws InterruptedException {
		Lock lock = lockManager.getExclusiveLock();
		assertTrue(lock.isActive());
		lock.release();
		assertFalse(lock.isActive());
	}

	@Test
	@Timeout(10)
	void cleanupUnreleasedLocks() throws InterruptedException {

		lock(lockManager);

		System.gc();
		Thread.sleep(100);

		Lock exclusiveLock = lockManager.getExclusiveLock();
		exclusiveLock.release();

	}

	@Test
	@Timeout(10)
	void cleanupUnreleasedLocksWithTracking() throws InterruptedException {

		lock(lockManagerTracking);

		System.gc();
		Thread.sleep(100);

		Lock exclusiveLock = lockManagerTracking.getExclusiveLock();
		exclusiveLock.release();

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		assertThat(memoryAppender.contains(
				"at org.eclipse.rdf4j.common.concurrent.locks.ExclusiveLockManagerTest.cleanupUnreleasedLocksWithTracking",
				Level.WARN)).isTrue();

	}

	@Test
	@Timeout(10)
	void deadlockTest() throws InterruptedException {

		Lock exclusiveLock1 = lockManagerTracking.getExclusiveLock();
		Thread thread = null;
		try {
			thread = new Thread(() -> {
				try {
					Lock exclusiveLock2 = lockManagerTracking.getExclusiveLock();
					exclusiveLock2.release();
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

		assertNull(lockManagerTracking.tryExclusiveLock());
		assertTrue(exclusiveLock1.isActive());
		exclusiveLock1.release();
		assertFalse(exclusiveLock1.isActive());

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		assertThat(memoryAppender.contains("is waiting on an active Exclusive lock acquired in main", Level.INFO))
				.isTrue();
		assertThat(memoryAppender.contains(
				"org.eclipse.rdf4j.common.concurrent.locks.ExclusiveLockManagerTest.lambda$deadlockTest", Level.INFO))
						.isTrue();

	}

	private void lock(ExclusiveLockManager lockManager) throws InterruptedException {
		Lock lock = lockManager.getExclusiveLock();
		assertTrue(lock.isActive());
	}
}
