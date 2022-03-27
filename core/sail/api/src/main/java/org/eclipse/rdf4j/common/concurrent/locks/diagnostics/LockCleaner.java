/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks.diagnostics;

import java.lang.ref.Cleaner;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.Properties;
import org.slf4j.Logger;

/**
 * Automatically log and release locks that are no longer referenced and will be garbage collected.
 *
 * @author Håvard M. Ottestad
 */
@InternalUseOnly
public class LockCleaner<T extends Lock> implements LockMonitoring<T> {

	private final static ConcurrentCleaner cleaner = new ConcurrentCleaner();
	private final Logger logger;
	private final Lock.ExtendedSupplier<T> supplier;
	private final String alias;
	private final boolean stacktrace;

	public LockCleaner(boolean stacktrace, String alias, Logger logger, Lock.ExtendedSupplier<T> supplier) {
		this.stacktrace = stacktrace;
		this.supplier = supplier;
		this.logger = logger;
		this.alias = alias;
	}

	public Lock getLock() throws InterruptedException {
		return getLockInner(supplier.getLock());
	}

	@Override
	public Lock tryLock() {

		T lock = supplier.tryLock();
		if (lock != null) {
			return getLockInner(lock);
		}

		return null;
	}

	@Override
	public T unsafeInnerLock(Lock lock) {
		if (lock instanceof CleanableLock) {
			return ((CleanableLock<T>) lock).state.lock;
		} else {
			throw new IllegalArgumentException("Supplied lock is not instanceof CleanableLock");
		}
	}

	@Override
	public Lock register(T lock) {
		return getLockInner(lock);
	}

	@Override
	public void unregister(Lock lock) {
		assert !lock.isActive();
		if (lock instanceof CleanableLock) {
			((CleanableLock<T>) lock).cleanable.clean();
		} else {
			throw new IllegalArgumentException("Supplied lock is not instanceof CleanableLock");
		}

	}

	private CleanableLock<T> getLockInner(T lock) {
		if (stacktrace) {
			return new CleanableLock<>(cleaner, lock, alias, logger, Thread.currentThread(),
					new Throwable("\"" + alias + "\" lock acquired in " + Thread.currentThread().getName()));
		} else {
			return new CleanableLock<>(cleaner, lock, alias, logger, null, null);
		}
	}

	public static class CleanableLock<T extends Lock> implements Lock {

		private final Cleaner.Cleanable cleanable;
		private final State<T> state;

		public CleanableLock(ConcurrentCleaner cleaner, T lock, String alias, Logger logger, Thread thread,
				Throwable throwable) {
			this.state = new State<>(lock, alias, logger, thread, throwable);
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

		static class State<T extends Lock> implements Runnable {

			private final T lock;
			private final String alias;
			private final Logger logger;
			private final Thread thread;
			private final Throwable stack;

			public State(T lock, String alias, Logger logger, Thread thread, Throwable stack) {
				this.lock = lock;
				this.alias = alias;
				this.logger = logger;
				this.thread = thread;
				this.stack = stack;
			}

			public void run() {
				// cleanup action accessing State, executed at most once
				if (lock.isActive()) {

					if (stack == null) {
						logger.warn("\"{}\" lock abandoned; consider setting the {} system property", alias,
								Properties.TRACK_LOCKS);
					} else {
						logger.warn("\"{}\" lock abandoned; lock was acquired in {}", alias, thread.getName(), stack);
					}

					lock.release();

				}
			}

		}

	}
}
