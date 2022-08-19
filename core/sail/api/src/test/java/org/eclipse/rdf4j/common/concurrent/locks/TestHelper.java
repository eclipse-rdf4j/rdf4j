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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.CountDownLatch;

import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.ConcurrentCleaner;

public class TestHelper {

	private final static ConcurrentCleaner cleaner = new ConcurrentCleaner();

	public static void callGC(AbstractReadWriteLockManager lockManager) throws InterruptedException {
		while (lockManager.isReaderActive() || lockManager.isWriterActive()) {
			System.gc();
			Thread.sleep(1);
		}
	}

	public static void callGC(ExclusiveLockManager lockManager) throws InterruptedException {
		while (lockManager.isActiveLock()) {
			System.gc();
			Thread.sleep(1);
		}
	}

	public static void callGC(LockManager lockManager) throws InterruptedException {
		while (lockManager.isActiveLock()) {
			System.gc();
			Thread.sleep(1);
		}
	}

	public static void interruptAndJoin(Thread thread) throws InterruptedException {
		assertNotNull(thread);
		thread.interrupt();
		join(thread);
	}

	public static void join(Thread thread) throws InterruptedException {
		assertNotNull(thread);
		thread.join(2000);
		assertFalse(thread.isAlive());

	}

	public static Thread getStartedDaemonThread(Lock.Supplier supplier) throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try {
				countDownLatch.countDown();
				supplier.getLock().release();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		thread.setDaemon(true);
		thread.start();
		countDownLatch.await();
		return thread;
	}

	public static Thread getStartedDaemonThread(Lock.Supplier supplier1, Lock.Supplier supplier2)
			throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);

		Thread thread = new Thread(() -> {
			Lock lock1 = null;
			Lock lock2 = null;
			try {
				countDownLatch.countDown();
				lock1 = supplier1.getLock();
				lock2 = supplier2.getLock();

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
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
		countDownLatch.await();

		return thread;
	}

	public static Thread getStartedDaemonThread(InterruptibleRunnable runnable) throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try {
				countDownLatch.countDown();
				runnable.run();
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		});

		thread.setDaemon(true);
		thread.start();
		countDownLatch.await();
		return thread;
	}

	interface InterruptibleRunnable {

		void run() throws InterruptedException;
	}

}
