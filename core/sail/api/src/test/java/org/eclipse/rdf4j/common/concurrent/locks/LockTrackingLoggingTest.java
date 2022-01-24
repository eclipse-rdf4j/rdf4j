/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

		Logger logger = (Logger) LoggerFactory.getLogger(LockManager.class.getName());
		memoryAppender = new MemoryAppender();
		memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		logger.setLevel(Level.INFO);
		logger.addAppender(memoryAppender);
		memoryAppender.start();

	}

	@Test
	@Timeout(10)
	void exclusiveLockManager() throws InterruptedException {

		ExclusiveLockManager exclusiveLockManager = new ExclusiveLockManager(false, 1);

		createLock(exclusiveLockManager);

		System.gc();
		Thread.sleep(1);

		Lock exclusiveLock = exclusiveLockManager.getExclusiveLock();
		exclusiveLock.release();

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		assertThat(memoryAppender.contains("at org.eclipse.rdf4j.common.concurrent.locks.LockTrackingLoggingTest.",
				Level.WARN)).isTrue();
	}

	@Test
	@Timeout(10)
	void readPrefReadWriteLockManager() throws InterruptedException {

		ReadPrefReadWriteLockManager lockManager = new ReadPrefReadWriteLockManager(false, 1);

		createLock(lockManager);

		System.gc();
		Thread.sleep(1);

		Lock writeLock = lockManager.getWriteLock();
		writeLock.release();

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		assertThat(memoryAppender.contains("at org.eclipse.rdf4j.common.concurrent.locks.LockTrackingLoggingTest.",
				Level.WARN)).isTrue();
	}

	@Test
	@Timeout(10)
	void writePrefReadWriteLockManager() throws InterruptedException {

		WritePrefReadWriteLockManager lockManager = new WritePrefReadWriteLockManager(false, 1);

		createLock(lockManager);

		System.gc();
		Thread.sleep(1);

		Lock writeLock = lockManager.getWriteLock();
		writeLock.release();

		assertThat(memoryAppender.countEventsForLogger(LockManager.class.getName())).isEqualTo(1);
		assertThat(memoryAppender.contains("at org.eclipse.rdf4j.common.concurrent.locks.LockTrackingLoggingTest.",
				Level.WARN)).isTrue();
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
