/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import org.junit.Test;

public class ReadPrefReadWriteLockManagerTest {

	@Test
	public void testMultipleReadLocksSameThread() {
		ReadPrefReadWriteLockManager readPrefReadWriteLockManager = new ReadPrefReadWriteLockManager();

		CountDownLatch acquireReadLockFirst = new CountDownLatch(1);
		CountDownLatch acquireReadLockWhileWriteLockWaiting = new CountDownLatch(1);

		Stream<Runnable> runnableStream = Stream.of(
				() -> {
					Lock readLock = readPrefReadWriteLockManager.getReadLock();
					acquireReadLockFirst.countDown();
					try {
						acquireReadLockWhileWriteLockWaiting.await();
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
					Lock readLock1 = readPrefReadWriteLockManager.getReadLock();

					readLock.release();
					readLock1.release();
				},
				() -> {

					try {
						acquireReadLockFirst.await();
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
					acquireReadLockWhileWriteLockWaiting.countDown();
					readPrefReadWriteLockManager.getWriteLock();

				},
				() -> {

				}

		);

		runnableStream.parallel().forEach(r -> {
			Thread thread = new Thread(r);
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		});

	}

}
