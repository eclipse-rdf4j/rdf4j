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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

		Logger logger = (Logger) LoggerFactory.getLogger(ExclusiveLockManager.class.getName());
		memoryAppender = new MemoryAppender();
		memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		logger.detachAndStopAllAppenders();
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
	@Timeout(2)
	void cleanupUnreleasedLocks() throws InterruptedException {

		lock(lockManager);

		TestHelper.callGC(lockManager);

		Lock exclusiveLock = lockManager.getExclusiveLock();
		exclusiveLock.release();

	}

	@Test
	@Timeout(2)
	void cleanupUnreleasedLocksWithTracking() throws InterruptedException {

		lock(lockManagerTracking);

		Lock exclusiveLock = lockManagerTracking.getExclusiveLock();
		exclusiveLock.release();

		memoryAppender.waitForEvents();

		assertThat(memoryAppender.countEventsForLogger(ExclusiveLockManager.class.getName())).isEqualTo(1);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.ExclusiveLockManagerTest.cleanupUnreleasedLocksWithTracking",
				Level.WARN);

	}

	@Test
	@Timeout(2)
	void deadlockTest() throws InterruptedException {

		Thread thread = null;
		try {
			thread = new Thread(() -> {
				Lock lock1 = null;
				Lock lock2 = null;
				try {
					lock1 = lockManagerTracking.getExclusiveLock();
					lock2 = lockManagerTracking.getExclusiveLock();
				} catch (InterruptedException ignored) {

				} finally {
					if (lock1 != null) {
						lock1.release();
					}
					if (lock2 != null) {
						lock2.release();
					}
				}
			});

			thread.setDaemon(true);
			thread.start();

			memoryAppender.waitForEvents();

		} finally {
			TestHelper.interruptAndJoin(thread);
		}

		Lock lock = lockManagerTracking.getExclusiveLock();
		assertTrue(lock.isActive());
		lock.release();

		assertThat(memoryAppender.countEventsForLogger(ExclusiveLockManager.class.getName())).isEqualTo(1);
		memoryAppender.assertContains("is possibly deadlocked waiting on \"ExclusiveLockManager\" with id", Level.WARN);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.ExclusiveLockManagerTest.lambda$deadlockTest$0(ExclusiveLockManagerTest.",
				Level.WARN);
	}

	@Test
	@Timeout(2)
	void stalledTest() throws InterruptedException {

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

			memoryAppender.waitForEvents();

		} finally {
			TestHelper.interruptAndJoin(thread);
		}

		assertNull(lockManagerTracking.tryExclusiveLock());
		assertTrue(exclusiveLock1.isActive());
		exclusiveLock1.release();
		assertFalse(exclusiveLock1.isActive());

		assertThat(memoryAppender.countEventsForLogger(ExclusiveLockManager.class.getName())).isGreaterThanOrEqualTo(1);
		memoryAppender.assertContains("is waiting on a possibly stalled lock \"ExclusiveLockManager\" with id",
				Level.INFO);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.ExclusiveLockManagerTest.stalledTest(ExclusiveLockManagerTest.java:",
				Level.INFO);

	}

	private void lock(ExclusiveLockManager lockManager) throws InterruptedException {
		Lock lock = lockManager.getExclusiveLock();
		assertTrue(lock.isActive());
	}
}
