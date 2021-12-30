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

		Stream<Runnable> runnableStream = Stream.of(
				() -> {
					try {
						Lock readLock = readPrefReadWriteLockManager.getReadLock();
						acquireReadLockFirst.countDown();

						// try to force the other thread to begin acquiring a write lock before we continue
						Thread.yield();
						Thread.sleep(100);

						Lock readLock2 = readPrefReadWriteLockManager.getReadLock();

						readLock.release();
						readLock2.release();

					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				},
				() -> {

					try {
						acquireReadLockFirst.await();
						readPrefReadWriteLockManager.getWriteLock();
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}

		);

		runAsThreads(runnableStream);

	}

	private void runAsThreads(Stream<Runnable> runnableStream) {
		runnableStream.parallel().forEach(Runnable::run);
	}

}
