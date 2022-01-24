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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

class LockManagerTest {

	private LockManager lockManager;
	private LockManager lockManagerTracking;
	private MemoryAppender memoryAppender;

	@BeforeEach
	void beforeEach() {
		Properties.setLockTrackingEnabled(false);
		lockManager = new LockManager(false, 1);
		lockManagerTracking = new LockManager(true, 1);

		Logger logger = (Logger) LoggerFactory.getLogger(LockManager.class.getName());
		memoryAppender = new MemoryAppender();
		memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		logger.setLevel(Level.WARN);
		logger.addAppender(memoryAppender);
		memoryAppender.start();

	}

	@Test
	void createLock() {
		Lock lock = lockManager.createLock("alias1");
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

		lockManager.waitForActiveLocks();

		assertFalse(lockManager.isActiveLock());
	}

	@Test
	@Timeout(10)
	void cleanupUnreleasedLocksWithTracking() throws InterruptedException {

		lock(lockManagerTracking);

		System.gc();
		Thread.sleep(100);

		lockManagerTracking.waitForActiveLocks();

		assertFalse(lockManagerTracking.isActiveLock());

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		assertThat(memoryAppender.contains("alias1 lock abandoned; lock was acquired in main", Level.WARN)).isTrue();
		assertThat(memoryAppender.contains(
				"at org.eclipse.rdf4j.common.concurrent.locks.LockManagerTest.cleanupUnreleasedLocksWithTracking",
				Level.WARN)).isTrue();

	}

	private void lock(LockManager lockManager) {
		Lock lock = lockManager.createLock("alias1");
		assertTrue(lock.isActive());
	}
}
