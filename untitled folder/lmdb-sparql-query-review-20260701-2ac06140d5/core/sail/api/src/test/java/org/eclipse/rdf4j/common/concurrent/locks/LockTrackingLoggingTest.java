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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

class LockTrackingLoggingTest {

	private MemoryAppender memoryAppender;

	@BeforeEach
	void beforeEach() {
		Properties.setLockTrackingEnabled(true);

		memoryAppender = new MemoryAppender();
		memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());

		Stream.of(LockManager.class, ExclusiveLockManager.class, ReadPrefReadWriteLockManager.class,
				WritePrefReadWriteLockManager.class).forEach(clazz -> {
					Logger logger = (Logger) LoggerFactory.getLogger(clazz);
					logger.detachAndStopAllAppenders();
					logger.setLevel(Level.INFO);
					logger.addAppender(memoryAppender);
				});

		memoryAppender.start();

	}

	@AfterEach
	void afterEach() {
		Properties.setLockTrackingEnabled(false);

	}

	@Test
	@Timeout(2)
	void exclusiveLockManager() throws InterruptedException {

		ExclusiveLockManager exclusiveLockManager = new ExclusiveLockManager(false, 1);

		createLock(exclusiveLockManager);

		TestHelper.callGC(exclusiveLockManager);

		Lock exclusiveLock = exclusiveLockManager.getExclusiveLock();
		exclusiveLock.release();
		memoryAppender.waitForEvents();

		assertThat(memoryAppender.countEventsForLogger(ExclusiveLockManager.class.getName())).isEqualTo(1);
		memoryAppender.assertContains("at org.eclipse.rdf4j.common.concurrent.locks.LockTrackingLoggingTest.",
				Level.WARN);
	}

	@Test
	@Timeout(2)
	void readPrefReadWriteLockManager() throws InterruptedException {

		ReadPrefReadWriteLockManager lockManager = new ReadPrefReadWriteLockManager(false);

		createLock(lockManager);

		TestHelper.callGC(lockManager);

		Lock writeLock = lockManager.getWriteLock();
		writeLock.release();
		memoryAppender.waitForEvents();

		assertThat(memoryAppender.countEventsForLogger(ReadPrefReadWriteLockManager.class.getName())).isEqualTo(1);
		memoryAppender.assertContains("at org.eclipse.rdf4j.common.concurrent.locks.LockTrackingLoggingTest.",
				Level.WARN);
	}

	@Test
	@Timeout(2)
	void writePrefReadWriteLockManager() throws InterruptedException {

		WritePrefReadWriteLockManager lockManager = new WritePrefReadWriteLockManager(false);

		createLock(lockManager);

		TestHelper.callGC(lockManager);

		Lock writeLock = lockManager.getWriteLock();
		writeLock.release();

		memoryAppender.waitForEvents();

		assertThat(memoryAppender.countEventsForLogger(WritePrefReadWriteLockManager.class.getName())).isEqualTo(1);
		memoryAppender.assertContains("at org.eclipse.rdf4j.common.concurrent.locks.LockTrackingLoggingTest.",
				Level.WARN);
	}

	private void createLock(ExclusiveLockManager lockManager) throws InterruptedException {
		Lock exclusiveLock = lockManager.getExclusiveLock();
		assertTrue(exclusiveLock.isActive());
	}

	private void createLock(ReadPrefReadWriteLockManager lockManager) throws InterruptedException {
		Lock exclusiveLock = lockManager.getWriteLock();
		assertTrue(exclusiveLock.isActive());
	}

	private void createLock(WritePrefReadWriteLockManager lockManager) throws InterruptedException {
		Lock exclusiveLock = lockManager.getWriteLock();
		assertTrue(exclusiveLock.isActive());
	}

}
