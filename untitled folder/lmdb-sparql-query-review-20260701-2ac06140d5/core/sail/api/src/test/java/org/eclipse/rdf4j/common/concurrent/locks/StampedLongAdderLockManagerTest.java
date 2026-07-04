/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class StampedLongAdderLockManagerTest {

	@Test
	void writeLockWaitsForReaders() throws Exception {
		StampedLongAdderLockManager manager = new StampedLongAdderLockManager();
		long readStamp = manager.readLock();
		assertTrue(manager.isReaderActive());

		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			CountDownLatch attemptingWrite = new CountDownLatch(1);
			AtomicBoolean acquiredWrite = new AtomicBoolean(false);

			Future<Long> writeFuture = executor.submit(() -> {
				attemptingWrite.countDown();
				long stamp = manager.writeLock();
				acquiredWrite.set(true);
				return stamp;
			});

			assertTrue(attemptingWrite.await(500, TimeUnit.MILLISECONDS), "write attempt did not start in time");
			TimeUnit.MILLISECONDS.sleep(100);
			assertFalse(acquiredWrite.get(), "write lock acquired while read lock active");

			manager.unlockRead(readStamp);
			long writeStamp = writeFuture.get(2, TimeUnit.SECONDS);
			assertTrue(acquiredWrite.get());
			assertTrue(manager.isWriterActive());
			manager.unlockWrite(writeStamp);
			assertFalse(manager.isWriterActive());
		} finally {
			executor.shutdownNow();
			executor.awaitTermination(1, TimeUnit.SECONDS);
		}
	}

	@Test
	void readLockWaitsForWriters() throws Exception {
		StampedLongAdderLockManager manager = new StampedLongAdderLockManager();
		long writeStamp = manager.writeLock();
		assertTrue(manager.isWriterActive());

		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			CountDownLatch attemptingRead = new CountDownLatch(1);
			AtomicBoolean acquiredRead = new AtomicBoolean(false);

			Future<Long> readFuture = executor.submit(() -> {
				attemptingRead.countDown();
				long stamp = manager.readLock();
				acquiredRead.set(true);
				return stamp;
			});

			assertTrue(attemptingRead.await(500, TimeUnit.MILLISECONDS), "read attempt did not start in time");
			TimeUnit.MILLISECONDS.sleep(100);
			assertFalse(acquiredRead.get(), "read lock acquired while write lock active");

			manager.unlockWrite(writeStamp);
			long readStamp = readFuture.get(2, TimeUnit.SECONDS);
			assertTrue(acquiredRead.get());
			assertEquals(StampedLongAdderLockManager.READ_LOCK_STAMP, readStamp);
			assertTrue(manager.isReaderActive());
			manager.unlockRead(readStamp);
			assertFalse(manager.isReaderActive());
		} finally {
			executor.shutdownNow();
			executor.awaitTermination(1, TimeUnit.SECONDS);
		}
	}
}
