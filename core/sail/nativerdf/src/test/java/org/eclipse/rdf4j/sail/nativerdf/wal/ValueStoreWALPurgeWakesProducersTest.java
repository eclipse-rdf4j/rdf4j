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
package org.eclipse.rdf4j.sail.nativerdf.wal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that purging the WAL wakes any producers blocked on a full queue.
 *
 * <p>
 * Reproduces a deadlock that occurs when {@link java.util.concurrent.ArrayBlockingQueue#clear()} is used during purge:
 * it removes elements without signalling {@code notFull}, leaving producers blocked in {@code put()} even though the
 * queue is now empty.
 */
class ValueStoreWALPurgeWakesProducersTest {

	@TempDir
	Path tempDir;

	@Test
	void purgeWakesBlockedProducer() throws Exception {
		Path walDir = tempDir.resolve("wal-purge-wakeup");
		Files.createDirectories(walDir);

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.queueCapacity(1) // make saturation easy and deterministic
				.build();

		try (ValueStoreWAL wal = ValueStoreWAL.open(cfg)) {
			// Stop the writer thread to avoid it draining the queue during this focused concurrency check
			Object logWriter = getField(wal, "logWriter");
			Method shutdown = logWriter.getClass().getDeclaredMethod("shutdown");
			shutdown.setAccessible(true);
			shutdown.invoke(logWriter);

			Thread writerThread = (Thread) getField(wal, "writerThread");
			writerThread.join(TimeUnit.SECONDS.toMillis(5));
			assertThat(!writerThread.isAlive()).as("writer thread should be stopped for this test").isTrue();

			// Swap in a test queue with explicit "clear does not signal notFull" semantics to make the behavior
			// deterministic across JDK versions.
			TestBlockingQueue testQueue = new TestBlockingQueue(1);
			setField(wal, "queue", testQueue);

			// Fill the test queue to capacity so the next mint attempt will block in put()
			boolean offered = testQueue.offer(new ValueStoreWalRecord(1L, 1, ValueStoreWalValueKind.LITERAL,
					"pre-fill", "dt", "", 0));
			assertThat(offered).isTrue();

			AtomicBoolean producerFinished = new AtomicBoolean(false);

			Thread producer = new Thread(() -> {
				try {
					wal.logMint(2, ValueStoreWalValueKind.LITERAL, "after-purge", "dt", "", 0);
					producerFinished.set(true);
				} catch (Exception e) {
					// mark as finished to avoid hanging the test in case of interruption on put()
					producerFinished.set(true);
				}
			}, "blocked-producer");

			producer.start();

			// Small delay to ensure the producer is actually blocked on put()
			Thread.sleep(50);
			assertThat(producer.isAlive()).as("producer should be blocked on a full queue").isTrue();

			// Perform the purge using the internal method to model the writer's purge path without races
			Method performPurge = logWriter.getClass().getDeclaredMethod("performPurgeInternal");
			performPurge.setAccessible(true);
			performPurge.invoke(logWriter);

			// Expectation: purge must wake the blocked producer promptly
			producer.join(TimeUnit.SECONDS.toMillis(1));
			boolean finishedNaturally = !producer.isAlive();
			try {
				assertThat(finishedNaturally)
						.as("producer should have completed without external interruption after purge")
						.isTrue();
				assertThat(producerFinished.get())
						.as("purge must wake producers blocked in queue.put()")
						.isTrue();
			} finally {
				if (!finishedNaturally) {
					// ensure no stray thread if assertion failed
					producer.interrupt();
				}
			}
		}
	}

	private static Object getField(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	/**
	 * Minimal blocking queue with a fixed capacity whose clear() deliberately does not signal notFull, to reproduce the
	 * deadlock scenario independent of the JDK's ArrayBlockingQueue implementation.
	 */
	private static final class TestBlockingQueue implements BlockingQueue<ValueStoreWalRecord> {
		private final java.util.ArrayDeque<ValueStoreWalRecord> deque = new java.util.ArrayDeque<>();
		private final int capacity;
		private final java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();
		private final java.util.concurrent.locks.Condition notEmpty = lock.newCondition();
		private final java.util.concurrent.locks.Condition notFull = lock.newCondition();

		TestBlockingQueue(int capacity) {
			this.capacity = capacity;
		}

		@Override
		public boolean offer(ValueStoreWalRecord e) {
			lock.lock();
			try {
				if (deque.size() >= capacity)
					return false;
				deque.addLast(e);
				notEmpty.signal();
				return true;
			} finally {
				lock.unlock();
			}
		}

		@Override
		public void put(ValueStoreWalRecord e) throws InterruptedException {
			lock.lock();
			try {
				while (deque.size() >= capacity) {
					notFull.await();
				}
				deque.addLast(e);
				notEmpty.signal();
			} finally {
				lock.unlock();
			}
		}

		@Override
		public ValueStoreWalRecord poll(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
			long nanos = unit.toNanos(timeout);
			lock.lockInterruptibly();
			try {
				while (deque.isEmpty()) {
					if (nanos <= 0L)
						return null;
					nanos = notEmpty.awaitNanos(nanos);
				}
				ValueStoreWalRecord v = deque.removeFirst();
				notFull.signal();
				return v;
			} finally {
				lock.unlock();
			}
		}

		@Override
		public void clear() {
			lock.lock();
			try {
				deque.clear();
				// intentionally do NOT signal notFull here
			} finally {
				lock.unlock();
			}
		}

		@Override
		public boolean isEmpty() {
			lock.lock();
			try {
				return deque.isEmpty();
			} finally {
				lock.unlock();
			}
		}

		@Override
		public int remainingCapacity() {
			lock.lock();
			try {
				return capacity - deque.size();
			} finally {
				lock.unlock();
			}
		}

		// --- Methods below are unused in this test and implemented minimally or throw UnsupportedOperationException
		// ---

		@Override
		public ValueStoreWalRecord take() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ValueStoreWalRecord poll() {
			lock.lock();
			try {
				if (deque.isEmpty()) {
					return null;
				}
				ValueStoreWalRecord v = deque.removeFirst();
				notFull.signal();
				return v;
			} finally {
				lock.unlock();
			}
		}

		@Override
		public ValueStoreWalRecord remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ValueStoreWalRecord element() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ValueStoreWalRecord peek() {
			return null;
		}

		@Override
		public boolean add(ValueStoreWalRecord e) {
			return offer(e);
		}

		@Override
		public boolean offer(ValueStoreWalRecord e, long timeout, java.util.concurrent.TimeUnit unit) {
			return offer(e);
		}

		@Override
		public int drainTo(java.util.Collection<? super ValueStoreWalRecord> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int drainTo(java.util.Collection<? super ValueStoreWalRecord> c, int maxElements) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean contains(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.util.Iterator<ValueStoreWalRecord> iterator() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(java.util.Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(java.util.Collection<? extends ValueStoreWalRecord> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(java.util.Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(java.util.Collection<?> c) {
			throw new UnsupportedOperationException();
		}
	}
}
