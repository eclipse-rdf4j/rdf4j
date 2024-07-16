/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

class ExclusiveReentrantLockManagerTest {

	private ExclusiveReentrantLockManager lockManager;
	private ExclusiveReentrantLockManager lockManagerTracking;
	private MemoryAppender memoryAppender;

	@BeforeEach
	void beforeEach() {
		Properties.setLockTrackingEnabled(false);
		lockManager = new ExclusiveReentrantLockManager(false, 1);
		lockManagerTracking = new ExclusiveReentrantLockManager(true, 1);

		Logger logger = (Logger) LoggerFactory.getLogger(ExclusiveReentrantLockManager.class.getName());
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

		memoryAppender.waitForEvents(2);

		assertThat(memoryAppender.countEventsForLogger(ExclusiveReentrantLockManager.class.getName())).isEqualTo(2);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.ExclusiveReentrantLockManagerTest.lambda$lock$2",
				Level.WARN);

	}

	@Test
	@Timeout(2)
	void stalledTest() throws InterruptedException {

		AtomicReference<Lock> exclusiveLock1 = new AtomicReference<>();
		Thread thread = new Thread(() -> {
			try {
				exclusiveLock1.set(lockManagerTracking.getExclusiveLock());
			} catch (InterruptedException ignored) {
			}
		});
		thread.start();
		thread.join();

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
		assertTrue(exclusiveLock1.get().isActive());
		exclusiveLock1.get().release();
		assertFalse(exclusiveLock1.get().isActive());

		memoryAppender.waitForEvents(2);

		assertThat(memoryAppender.countEventsForLogger(ExclusiveReentrantLockManager.class.getName()))
				.isGreaterThanOrEqualTo(1);
		memoryAppender.assertContains("is waiting on a possibly stalled lock \"ExclusiveReentrantLockManager\" with id",
				Level.INFO);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.ExclusiveReentrantLockManagerTest.lambda$stalledTest$0(ExclusiveReentrantLockManagerTest.java:",
				Level.INFO);

	}

	private void lock(ExclusiveReentrantLockManager lockManager) throws InterruptedException {
		Thread thread = new Thread(() -> {
			try {
				lockManager.getExclusiveLock();
			} catch (InterruptedException ignored) {
			}
		});
		thread.start();
		thread.join(2000);
		assertThat(thread.isAlive()).isFalse();
	}
}
