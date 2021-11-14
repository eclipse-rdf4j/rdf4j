package org.eclipse.rdf4j.common.concurrent.locks;

import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockMonitoring {

	private final Logger logger = LoggerFactory.getLogger(LockMonitoring.class);

	private final static Cleaner cleaner = Cleaner.create();

	// locks that have not been GCed yet
	private final ConcurrentLinkedQueue<WeakReference<SimpleLock>> locks = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<SimpleLock> abandoned = new ConcurrentLinkedQueue<>();

	private final Supplier<Lock> supplier;
	private final boolean trackLocks;

	public LockMonitoring(boolean trackLocks, Supplier<Lock> supplier) {
		this.trackLocks = trackLocks;
		this.supplier = supplier;
	}

	public boolean hasAbandonedLocks() {
		return !abandoned.isEmpty();
	}

	public void forceReleaseAbandonedLocks() {
		SimpleLock poll = abandoned.poll();
		while (poll != null) {

			logAbandonedLock(poll);
			poll.release();
			poll = abandoned.poll();
		}
	}

	private void logAbandonedLock(SimpleLock lock) {
		if (logger.isWarnEnabled()) {
			lock.logAbandoned(logger);
		}

	}

	public long getActiveLocksSignature() {
		WeakReference<SimpleLock> lock = locks.poll();

		long signature = 0;
		WeakReference<SimpleLock> first = null;

		while (lock != null) {
			SimpleLock simpleLock = lock.get();
			if (simpleLock != null) {
				if (lock == first) {
					break;
				}
				if (simpleLock.isActive()) {
					signature += System.identityHashCode(simpleLock);
					// add to other end of queue
					locks.add(lock);
					if (first == null) {
						first = lock;
					}
				}
			}

			lock = locks.poll();
		}

		return signature;

	}

	public void logStalledLocks() {
//		Thread current = Thread.currentThread();
//		if (activeLocks.size() == 1) {
//			WeakLockReference lock = activeLocks.iterator().next();
//			if (logger.isWarnEnabled()) {
//				String msg = "Thread " + current.getName() + " is waiting on an active " + lock.alias
//					+ " lock acquired in " + lock.acquiredName;
//				if (lock.acquiredId == current.getId()) {
//					if (lock.stack == null) {
//						logger.warn(msg, new Throwable());
//					} else {
//						logger.warn(msg, new Throwable(lock.stack));
//					}
//				} else {
//					if (lock.stack == null) {
//						logger.info(msg);
//					} else {
//						logger.info(msg, new Throwable(lock.stack));
//					}
//				}
//			}
//		} else {
//			String alias = null;
//			boolean warn = false;
//			for (WeakLockReference lock : activeLocks) {
//				warn |= lock.acquiredId == current.getId();
//				if (alias == null) {
//					alias = lock.alias;
//				} else if (!alias.contains(lock.alias)) {
//					alias = alias + ", " + lock.alias;
//				}
//			}
//			String msg = "Thread " + current.getName() + " is waiting on " + activeLocks.size() + " active " + alias
//				+ " locks";
//			if (warn) {
//				logger.warn(msg);
//			} else {
//				logger.info(msg);
//			}
//		}
	}

	public static class SimpleLock implements Lock {

		static class State implements Runnable {

			private final Lock lock;
			private final String alias;
			private final String acquiredName;
			private final long acquiredId;
			private final Throwable stack;
			private final ConcurrentLinkedQueue<SimpleLock> abandoned;

			public State(Lock lock, String alias, String acquiredName, long acquiredId, Throwable stack,
					ConcurrentLinkedQueue<SimpleLock> abandoned) {
				this.lock = lock;
				this.alias = alias;
				this.acquiredName = acquiredName;
				this.acquiredId = acquiredId;
				this.stack = stack;
				this.abandoned = abandoned;
			}

			public void run() {
				// cleanup action accessing State, executed at most once
				if (lock.isActive()) {
					abandoned.add(new SimpleLock(lock, alias, acquiredName, acquiredId, stack));
				}
			}

		}

		private final State state;

		public SimpleLock(Lock lock, String alias, String acquiredName, long acquiredId, Throwable stack,
				ConcurrentLinkedQueue<SimpleLock> abandoned) {
			this.state = new State(lock, alias, acquiredName, acquiredId, stack, abandoned);
			cleaner.register(this, state);
		}

		SimpleLock(Lock lock, String alias, String acquiredName, long acquiredId, Throwable stack) {
			this.state = new State(lock, alias, acquiredName, acquiredId, stack, null);
		}

		@Override
		public boolean isActive() {
			return state.lock.isActive();
		}

		@Override
		public void release() {
			state.lock.release();
		}

		void logAbandoned(Logger logger) {
			if (state.stack == null) {
				String msg = "{} lock abandoned; lock was acquired in {}; consider setting the {} system property";
				logger.warn(msg, state.alias, state.acquiredName, Properties.TRACK_LOCKS);
			} else {
				String msg = state.alias + " lock abandoned; lock was acquired in " + state.acquiredName;
				logger.warn(msg, state.stack);
			}
		}
	}

	private final static AtomicLong seq = new AtomicLong();

	Lock getLock(String alias) {

		Thread thread = Thread.currentThread();

		Throwable stack;
		if (trackLocks) {
			stack = new Throwable(alias + " lock " + seq.incrementAndGet() + " acquired in " + thread.getName());
		} else {
			stack = null;
		}

		SimpleLock lock = new SimpleLock(supplier.get(), alias, thread.getName(), thread.getId(), stack);

		locks.add(new WeakReference<>(lock));

		return lock;
	}

}
