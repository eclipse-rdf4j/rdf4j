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
		logger.detachAndStopAllAppenders();
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
	@Timeout(2)
	void cleanupUnreleasedLocks() throws InterruptedException {

		lock(lockManager);

		TestHelper.callGC(lockManager);

		lockManager.waitForActiveLocks();

		assertFalse(lockManager.isActiveLock());
	}

	@Test
	@Timeout(2)
	void cleanupUnreleasedLocksWithTracking() throws InterruptedException {

		lock(lockManagerTracking);

		TestHelper.callGC(lockManagerTracking);

		lockManagerTracking.waitForActiveLocks();

		assertFalse(lockManagerTracking.isActiveLock());

		memoryAppender.waitForEvents();

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		memoryAppender.assertContains("\"alias1\" lock abandoned; lock was acquired in main", Level.WARN);
		memoryAppender.assertContains(
				"at org.eclipse.rdf4j.common.concurrent.locks.LockManagerTest.cleanupUnreleasedLocksWithTracking",
				Level.WARN);

	}

	private void lock(LockManager lockManager) {
		Lock lock = lockManager.createLock("alias1");
		assertTrue(lock.isActive());
	}
}
