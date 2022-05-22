/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks.diagnostics;

import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.Properties;
import org.slf4j.Logger;

/**
 * Full tracking of locks with simple deadlock detection and logging as well as automatic release of abandoned locks
 * (same as LockCleaner).
 *
 * @author Håvard M. Ottestad
 */
@InternalUseOnly
public class LockTracking<T extends Lock> implements LockMonitoring<T> {

	public static final int LOGGED_STALLED_LOCKS_MINIMUM_WAIT_TO_COLLECT = 1000;

	private final Logger logger;

	private final static ConcurrentCleaner cleaner = new ConcurrentCleaner();
	private final static AtomicLong seq = new AtomicLong();

	private final ReentrantLock staleLoggingLock = new ReentrantLock();

	// locks that have not been GCed yet
	private final Map<SimpleLock<T>, WeakReference<SimpleLock<T>>> locks = Collections
			.synchronizedMap(new WeakHashMap<>());

	private final Lock.ExtendedSupplier<T> supplier;
	private final boolean stacktrace;
	private final int waitToCollect;
	private final String alias;
	private int currentWaitToCollect;
	private long previousCleanup = 0;
	private long previousActiveLocksSignature = 0;

	public LockTracking(boolean stacktrace, String alias, Logger logger, int waitToCollect,
			Lock.ExtendedSupplier<T> supplier) {
		this.stacktrace = stacktrace;
		this.supplier = supplier;
		this.waitToCollect = waitToCollect;
		this.currentWaitToCollect = waitToCollect;
		this.logger = logger;
		this.alias = alias;
	}

	private long getActiveLocksSignature() {

		synchronized (locks) {
			return locks.keySet()
					.stream()
					.filter(Objects::nonNull)
					.filter(SimpleLock::isActive)
					.mapToLong(s -> s.state.acquiredId)
					.sum();
		}

	}

	private void logStalledLocks() {
		Thread currentThread = Thread.currentThread();
		synchronized (locks) {
			locks.keySet().stream().filter(Objects::nonNull).filter(SimpleLock::isActive).forEach(simpleLock -> {
				if (simpleLock.state.thread == currentThread) {
					logger.warn("{} is possibly deadlocked waiting on \"{}\" with id {} acquired in the same thread",
							currentThread.getName(), simpleLock.state.alias, simpleLock.state.acquiredId,
							simpleLock.state.stack);
				} else {
					logger.info(
							"Current thread ({}) is waiting on a possibly stalled lock \"{}\" with id {} acquired in {}",
							currentThread.getName(), simpleLock.state.alias, simpleLock.state.acquiredId,
							simpleLock.state.thread.getName(), simpleLock.state.stack);
				}
			});
		}

	}

	public void runCleanup() {
		if (previousCleanup == 0) {
			previousCleanup = System.currentTimeMillis();
		} else {
			if (System.currentTimeMillis() - previousCleanup > currentWaitToCollect) {

				boolean locked = false;

				try {
					locked = staleLoggingLock.tryLock();

					if (locked) {
						if (System.currentTimeMillis() - previousCleanup > currentWaitToCollect) {
							// if something should fail we don't want to perform a new cleanup immediately
							previousCleanup = 0;

							System.gc();
							long activeLocksSignature = getActiveLocksSignature();
							if (previousActiveLocksSignature == activeLocksSignature) {
								logStalledLocks();
								// avoid logging the same stalled locks over and over when waitToCollect is very small
								currentWaitToCollect = Math.max(currentWaitToCollect,
										LOGGED_STALLED_LOCKS_MINIMUM_WAIT_TO_COLLECT);
							} else {
								currentWaitToCollect = waitToCollect;
							}
							previousActiveLocksSignature = activeLocksSignature;
							previousCleanup = System.currentTimeMillis();
						}
					}
				} finally {
					if (locked) {
						staleLoggingLock.unlock();
					}
				}

			}
		}
	}

	public Lock getLock() throws InterruptedException {
		return getLock(alias);
	}

	public Lock getLock(String alias) throws InterruptedException {
		return getLockInner(supplier.getLock(), alias);
	}

	@Override
	public T unsafeInnerLock(Lock lock) {
		if (lock instanceof SimpleLock) {
			return ((SimpleLock<T>) lock).state.lock;
		} else {
			throw new IllegalArgumentException("Supplied lock is not instanceof SimpleLock");
		}
	}

	@Override
	public Lock tryLock() {

		T lock = supplier.tryLock();
		if (lock != null) {
			return getLockInner(lock, alias);
		}

		return null;
	}

	private SimpleLock<T> getLockInner(T lock, String alias) {
		Thread thread = Thread.currentThread();

		long sequenceNumber = seq.incrementAndGet();

		Throwable stack;
		if (stacktrace) {
			stack = new Throwable(alias + " lock " + sequenceNumber + " acquired in " + thread.getName());
		} else {
			stack = null;
		}

		SimpleLock<T> simpleLock = new SimpleLock<>(lock, alias, sequenceNumber, stack, thread, logger);

		locks.put(simpleLock, new WeakReference<>(simpleLock));

		return simpleLock;
	}

	@Override
	public boolean requiresManualCleanup() {
		return true;
	}

	@Override
	public Lock register(T lock) {
		return getLockInner(lock, alias);
	}

	@Override
	public void unregister(Lock lock) {
		assert !lock.isActive();
		if (lock instanceof SimpleLock) {
			((SimpleLock<?>) lock).cleanable.clean();
		} else {
			throw new IllegalArgumentException("Supplied lock is not instanceof SimpleLock");
		}
	}

	public static class SimpleLock<T extends Lock> implements Lock {

		private final State<T> state;
		private final Cleaner.Cleanable cleanable;

		public SimpleLock(T lock, String alias, long acquiredId, Throwable stack, Thread thread, Logger logger) {
			this.state = new State<>(lock, alias, acquiredId, stack, thread, logger);
			this.cleanable = cleaner.register(this, state);
		}

		@Override
		public boolean isActive() {
			return state.lock.isActive();
		}

		@Override
		public void release() {
			state.lock.release();
			cleanable.clean();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof SimpleLock)) {
				return false;
			}
			SimpleLock that = (SimpleLock) o;
			return state.acquiredId == that.state.acquiredId;
		}

		@Override
		public int hashCode() {
			return Long.hashCode(state.acquiredId);
		}

		static class State<T extends Lock> implements Runnable {

			private final T lock;
			private final String alias;
			private final long acquiredId;
			private final Throwable stack;
			private final Thread thread;
			private final Logger logger;

			public State(T lock, String alias, long acquiredId, Throwable stack, Thread thread, Logger logger) {
				this.lock = lock;
				this.alias = alias;
				this.acquiredId = acquiredId;
				this.stack = stack;
				this.logger = logger;
				this.thread = thread;
			}

			public void run() {
				// cleanup action accessing State, executed at most once
				if (lock.isActive()) {
					lock.release();
					logAbandoned(logger);
				}
			}

			void logAbandoned(Logger logger) {
				if (stack == null) {
					logger.warn(
							"\"{}\" lock abandoned; lock was acquired in {}; consider setting the {} system property",
							alias, thread.getName(), Properties.TRACK_LOCKS);
				} else {
					logger.warn("\"{}\" lock abandoned; lock was acquired in {}", alias, thread.getName(), stack);
				}
			}

		}

	}
}
