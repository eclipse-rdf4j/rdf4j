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
package org.eclipse.rdf4j.sail.lmdb;

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_RDONLY;
import static org.lwjgl.util.lmdb.LMDB.MDB_READERS_FULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_reader_check;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_renew;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_reset;

import java.io.Closeable;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.LmdbUtil.Transaction;
import org.eclipse.rdf4j.sail.lmdb.util.MpmcRingBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

/**
 * Manager for LMDB read transactions.
 *
 * <h2>Reader admission</h2>
 * <p>
 * {@link #readerSlots} does <em>not</em> count live LMDB transactions; it counts the right to <em>hold</em> a read
 * transaction. A caller acquires exactly one permit before it is handed a transaction (either a recycled one from
 * {@link #txnPool} or a freshly started one) and returns that permit in {@link Txn#close()} - regardless of whether the
 * transaction was parked in the pool ({@link Mode#RESET}) or aborted ({@link Mode#ABORT}).
 * <p>
 * Consequences:
 * <ul>
 * <li>An idle, pooled transaction holds <strong>no</strong> permit. Therefore the semaphore is a fair FIFO waiting room
 * for both events "a pooled transaction became available" and "a reader slot was freed" - a thread simply sleeps in
 * {@code readerSlots.tryAcquire(timeout)} until either happens. No polling, no busy-wait.</li>
 * <li>Destroying a pooled transaction ({@link #closePooledReaders()}) must <strong>not</strong> release a permit, since
 * the permit was already returned when the transaction entered the pool.</li>
 * <li>{@link Mode#ABORT} needs no special casing: the permit is released after {@code mdb_txn_abort}, which unblocks
 * the next waiter.</li>
 * </ul>
 *
 * <h2>Invariants</h2>
 * <ul>
 * <li>{@code readerSlots.availablePermits() == POOL_SIZE - handedOutTransactions} (before {@link #close()}).</li>
 * <li>{@code pooledTransactions <= readerSlots.availablePermits()}, hence {@code handedOut + pooled <= POOL_SIZE}:
 * after acquiring a permit it is always safe to start a new transaction.</li>
 * <li>{@link #open} contains <em>every</em> live LMDB read transaction owned by this manager, including transactions
 * that are currently idle in {@link #txnPool}. This guarantees that {@link #reset()}, {@link #deactivate()} and
 * {@link #activate()} can never miss a reader.</li>
 * <li>{@link #txnPool} is a subset of {@link #open}: idle, reusable transactions.</li>
 * <li>No LMDB call is ever made while an intrinsic manager-wide lock is held.</li>
 * </ul>
 */
final class TxnManager {

	/** Overall budget for a single readers-full retry sequence. */
	private static final long READERS_FULL_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);
	/** Overall budget for waiting until a read transaction becomes available. */
	private static final long READER_ADMISSION_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);
	/** Initial / maximum backoff while waiting for a reader slot. */
	private static final long BACKOFF_MIN_MILLIS = 1L;
	private static final long BACKOFF_MAX_MILLIS = 32L;
	/** Minimum interval between two global mdb_reader_check() calls. */
	private static final long READER_CHECK_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(50);

	private static final int CACHED_POOLS = 1 << 4;
	/** Maximum number of read transactions this manager hands out concurrently. */
	static final int POOL_SIZE = 1 << 7;

	/** Round-robin distribution of value pools across transactions. */
	private static final AtomicInteger POOL_ROTATION = new AtomicInteger();

	private final long env;
	private final Mode mode;

	/** All live read transactions owned by this manager (identity semantics: Txn does not override equals). */
	private final Set<Txn> open = ConcurrentHashMap.newKeySet();
	/** Idle, reusable transactions; {@code null} unless {@link Mode#RESET}. */
	private final MpmcRingBuffer<Txn> txnPool;
	/** One permit per read transaction that may be handed out; see class javadoc. */
	private final Semaphore readerSlots = new Semaphore(POOL_SIZE, true);

	private final ReentrantLock readersFullLock = new ReentrantLock();
	private final Condition readerInactive = readersFullLock.newCondition();
	private final AtomicInteger readersFullWaiters = new AtomicInteger();
	private final AtomicLong lastReaderCheck = new AtomicLong(Long.MIN_VALUE);

	private final Pool[] pools = new Pool[CACHED_POOLS];
	private final StampedLongAdderLockManager lockManager = new StampedLongAdderLockManager();

	private volatile boolean managerClosed;

	TxnManager(long env, Mode mode) throws IOException {
		this.env = env;
		this.mode = mode;
		this.txnPool = mode == Mode.RESET ? new MpmcRingBuffer<>(POOL_SIZE) : null;
		for (int i = 0; i < pools.length; i++) {
			pools[i] = new Pool();
		}
	}

	// ---------------------------------------------------------------------------------------------
	// public-ish API
	// ---------------------------------------------------------------------------------------------

	/**
	 * Wraps an existing (foreign) transaction into a reference object. The returned reference does not own the
	 * transaction: closing it is a no-op, it is not tracked for reset/activate and it does not consume a reader permit.
	 */
	Txn createTxn(long txn) {
		return new Txn(txn, /* owned= */ false, /* resetOnWrite= */ false);
	}

	/**
	 * Creates a new tracked read-only transaction. Tracked transactions are reset on every write commit.
	 */
	Txn createReadTxn() throws IOException {
		return createReadTxnInternal(true);
	}

	/**
	 * Creates a read-only transaction that is <em>untracked</em> for reset semantics.
	 * <p>
	 * Untracked readers survive {@link #reset()} so that long-lived refresh readers are not invalidated on every write
	 * commit; they are only marked {@link Txn#stale} and renewed lazily. They still participate in
	 * {@link #deactivate()}/{@link #activate()} to remain safe during map resize.
	 */
	Txn createReadTxnUntracked() throws IOException {
		return createReadTxnInternal(false);
	}

	/**
	 * Hands out a read transaction, blocking (interruptibly, with timeout) until one becomes available.
	 * <p>
	 * Exactly one reader permit is consumed per returned transaction; it is returned by {@link Txn#close()}. Waiting
	 * for the permit covers both "a pooled transaction was released" and "a reader slot was freed by an abort", so the
	 * caller never has to poll the pool.
	 */
	Txn createReadTxnInternal(boolean resetOnWrite) throws IOException {
		checkNotClosed();
		acquireReaderPermit();

		boolean permitConsumed = false;
		try {
			// Fast path: recycle an idle reader. The permit guarantees that either the pool is non-empty or that
			// starting a new transaction stays within POOL_SIZE.
			Txn pooled = pollPooled();
			if (pooled != null) {
				try {
					pooled.reuse(resetOnWrite);
				} catch (IOException | RuntimeException e) {
					discardPooled(pooled);
					throw e;
				}
				permitConsumed = true;
				return pooled;
			}

			checkNotClosed();
			Txn txn = new Txn(startReadTxn(), /* owned= */ true, resetOnWrite);
			open.add(txn);
			if (managerClosed) {
				// lost the race against close(): do not leak the native transaction
				if (open.remove(txn)) {
					txn.abortAndMarkClosed();
				}
				throw new IOException("Transaction manager is closed");
			}
			permitConsumed = true;
			return txn;
		} finally {
			if (!permitConsumed) {
				releaseReaderPermit();
			}
		}
	}

	<T> T doWith(Transaction<T> transaction) throws IOException {
		long readStamp;
		try {
			readStamp = lockManager.readLock();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SailException("Interrupted while acquiring read lock", e);
		}
		try (Txn txn = createReadTxn(); MemoryStack stack = stackPush()) {
			return transaction.exec(stack, txn.get());
		} finally {
			lockManager.unlockRead(readStamp);
		}
	}

	/** Exposes the shared lock manager used to coordinate read and write transactions. */
	StampedLongAdderLockManager lockManager() {
		return lockManager;
	}

	/** Renews all tracked transactions, e.g. after a map resize. */
	void activate() throws IOException {
		forEachOpen(txn -> txn.setActive(true));
	}

	/** Resets all tracked transactions so that no reader blocks a map resize. */
	void deactivate() throws IOException {
		forEachOpen(txn -> txn.setActive(false));
	}

	/** Marks all tracked transactions as pointing to outdated data. */
	void reset() throws IOException {
		forEachOpen(Txn::reset);
	}

	void close() {
		managerClosed = true;

		// Drain the idle pool first; the transactions themselves are still tracked in `open` and are aborted below.
		if (txnPool != null) {
			// noinspection StatementWithEmptyBody
			while (txnPool.poll() != null) {
				// discard
			}
		}

		for (Txn txn : open) {
			if (open.remove(txn)) {
				txn.abortAndMarkClosed();
			}
		}

		// Poison the semaphore: wake every admission waiter, which then fails fast in acquireReaderPermit().
		// Permit accounting is irrelevant from here on, the manager is closed.
		readerSlots.release(POOL_SIZE);
		signalReaderInactive();

		for (Pool pool : pools) {
			pool.close();
		}
	}

	// ---------------------------------------------------------------------------------------------
	// reader admission
	// ---------------------------------------------------------------------------------------------

	private void acquireReaderPermit() throws IOException {
		try {
			if (!readerSlots.tryAcquire(READER_ADMISSION_TIMEOUT_NANOS, TimeUnit.NANOSECONDS)) {
				throw new IOException("Timed out after "
						+ TimeUnit.NANOSECONDS.toMillis(READER_ADMISSION_TIMEOUT_NANOS)
						+ " ms waiting for a free read transaction (limit " + POOL_SIZE + ")");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for a free read transaction", e);
		}
		if (managerClosed) {
			readerSlots.release();
			throw new IOException("Transaction manager is closed");
		}
	}

	/** Returns a reader permit and wakes both admission waiters and readers-full waiters. */
	private void releaseReaderPermit() {
		readerSlots.release();
		signalReaderInactive();
	}

	/** Destroys a transaction that was just polled from the pool (its permit is held by the caller). */
	private void discardPooled(Txn pooled) {
		if (open.remove(pooled)) {
			pooled.abortAndMarkClosed();
		}
	}

	// ---------------------------------------------------------------------------------------------
	// transaction start / renew, with a single unified readers-full retry
	// ---------------------------------------------------------------------------------------------

	private long startReadTxn() throws IOException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(withReadersFullRetry(null, () -> mdb_txn_begin(env, NULL, MDB_RDONLY, pp)));
			return pp.get(0);
		}
	}

	private void renewReadTxn(long txn, Txn excluded) throws IOException {
		int rc = withReadersFullRetry(excluded, () -> mdb_txn_renew(txn));
		if (rc != MDB_SUCCESS) {
			E(rc);
		}
	}

	@FunctionalInterface
	private interface NativeCall {
		int run();
	}

	/**
	 * Runs {@code call} and, on {@link org.lwjgl.util.lmdb.LMDB#MDB_READERS_FULL}, retries with exponential backoff
	 * until the deadline expires. Between attempts idle pooled readers are aborted and dead readers are reaped.
	 * <p>
	 * This deals with the <em>environment-wide</em> reader table (shared with other processes / managers), which is a
	 * different resource than {@link #readerSlots}.
	 */
	private int withReadersFullRetry(Txn excluded, NativeCall call) throws IOException {
		int rc = call.run();
		if (rc != MDB_READERS_FULL) {
			return rc;
		}

		readersFullWaiters.incrementAndGet();
		try (MemoryStack stack = stackPush()) {
			IntBuffer dead = stack.mallocInt(1);
			long deadline = System.nanoTime() + READERS_FULL_TIMEOUT_NANOS;
			long backoffMillis = BACKOFF_MIN_MILLIS;

			while (rc == MDB_READERS_FULL) {
				if (Thread.interrupted()) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted while waiting for a free LMDB reader slot");
				}
				closePooledReaders();
				checkForDeadReaders(dead);
				awaitReaderRelease(excluded, backoffMillis);
				backoffMillis = Math.min(backoffMillis << 1, BACKOFF_MAX_MILLIS);

				rc = call.run();
				if (rc == MDB_READERS_FULL && System.nanoTime() - deadline >= 0) {
					break;
				}
			}
		} finally {
			readersFullWaiters.decrementAndGet();
		}
		return rc;
	}

	/**
	 * Reaps stale reader slots of crashed processes. Throttled globally because {@code mdb_reader_check} scans the
	 * whole reader table under an environment-wide mutex.
	 */
	private void checkForDeadReaders(IntBuffer dead) throws IOException {
		long now = System.nanoTime();
		long last = lastReaderCheck.get();
		if ((last == Long.MIN_VALUE || now - last >= READER_CHECK_INTERVAL_NANOS)
				&& lastReaderCheck.compareAndSet(last, now)) {
			E(mdb_reader_check(env, dead));
		}
	}

	/**
	 * Aborts all idle pooled readers, releasing their LMDB reader-table slots.
	 * <p>
	 * Pooled readers hold no reader permit (see class javadoc), therefore <strong>no</strong> permit is released here -
	 * doing so would inflate {@link #readerSlots} and allow more than {@link #POOL_SIZE} concurrent readers.
	 */
	private void closePooledReaders() {
		if (txnPool == null) {
			return;
		}
		Txn txn;
		boolean aborted = false;
		while ((txn = txnPool.poll()) != null) {
			if (open.remove(txn)) {
				txn.abortAndMarkClosed();
				aborted = true;
			}
		}
		if (aborted) {
			signalReaderInactive();
		}
	}

	private void awaitReaderRelease(Txn excluded, long timeoutMillis) throws IOException {
		readersFullLock.lock();
		try {
			if (hasTrackedReaders(excluded)) {
				readerInactive.await(timeoutMillis, TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		} finally {
			readersFullLock.unlock();
		}
	}

	private boolean hasTrackedReaders(Txn excluded) {
		int size = open.size();
		return (excluded != null && open.contains(excluded)) ? size > 1 : size > 0;
	}

	/** Cheap no-op unless somebody is actually blocked on a readers-full condition. */
	private void signalReaderInactive() {
		if (readersFullWaiters.get() == 0) {
			return;
		}
		readersFullLock.lock();
		try {
			readerInactive.signalAll();
		} finally {
			readersFullLock.unlock();
		}
	}

	// ---------------------------------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------------------------------

	@FunctionalInterface
	private interface TxnAction {
		void apply(Txn txn) throws IOException;
	}

	/**
	 * Applies {@code action} to every tracked transaction without holding a manager-wide lock. Failures are collected
	 * so that a single failing reader cannot leave the remaining ones in an inconsistent state.
	 */
	private void forEachOpen(TxnAction action) throws IOException {
		IOException failure = null;
		for (Txn txn : open) {
			try {
				action.apply(txn);
			} catch (IOException e) {
				if (failure == null) {
					failure = e;
				} else {
					failure.addSuppressed(e);
				}
			}
		}
		if (failure != null) {
			throw failure;
		}
	}

	private Txn pollPooled() {
		return txnPool != null ? txnPool.poll() : null;
	}

	private boolean offerPooled(Txn txn) {
		return txnPool != null && !managerClosed && txnPool.offer(txn);
	}

	private void checkNotClosed() throws IOException {
		if (managerClosed) {
			throw new IOException("Transaction manager is closed");
		}
	}

	enum Mode {
		RESET,
		ABORT,
		NONE
	}

	// ---------------------------------------------------------------------------------------------
	// Txn
	// ---------------------------------------------------------------------------------------------

	final class Txn implements Closeable {

		private final long txn;
		/** {@code false} for foreign transactions wrapped via {@link TxnManager#createTxn(long)}. */
		private final boolean owned;
		private final Pool valuePool = pools[POOL_ROTATION.getAndIncrement() & (CACHED_POOLS - 1)];

		private volatile long version;
		private volatile boolean active = true;
		/** Permanently finished: aborted or handed back to LMDB. */
		private volatile boolean closed;
		/** Idle in {@link TxnManager#txnPool} and thus reusable, but still a live LMDB reader. */
		private volatile boolean idle;
		private volatile boolean resetOnWrite;
		private volatile boolean stale;

		private Txn(long txn, boolean owned, boolean resetOnWrite) {
			this.txn = txn;
			this.owned = owned;
			this.resetOnWrite = resetOnWrite;
		}

		long get() {
			return txn;
		}

		long version() {
			return version;
		}

		StampedLongAdderLockManager lockManager() {
			return lockManager;
		}

		Pool getValuePool() {
			return valuePool;
		}

		@Override
		public void close() {
			if (!owned) {
				return; // foreign transaction: not ours to abort, no permit involved
			}
			boolean releasePermit;
			synchronized (this) {
				if (closed || idle) {
					return;
				}
				releasePermit = release();
			}
			if (releasePermit) {
				releaseReaderPermit();
			} else {
				signalReaderInactive();
			}
		}

		/** Marks this transaction as pointing to outdated data. */
		synchronized void reset() throws IOException {
			if (closed) {
				return;
			}
			if (resetOnWrite || idle) {
				resetNative();
				version++;
				if (!idle) {
					activate();
				}
			} else {
				// untracked reader: keep the snapshot, renew lazily on next reuse
				stale = true;
			}
		}

		/** Toggles the active state, e.g. around a map resize. */
		synchronized void setActive(boolean active) throws IOException {
			if (closed) {
				return;
			}
			if (active) {
				if (!idle) {
					// idle readers stay reset; they are renewed lazily in reuse()
					activate();
				}
				version++;
			} else {
				deactivate();
			}
		}

		// -- internals, all called with the monitor held -------------------------------------------

		/** Prepares a pooled transaction for reuse. */
		private synchronized void reuse(boolean resetOnWrite) throws IOException {
			if (stale) {
				resetNative();
				stale = false;
			}
			this.resetOnWrite = resetOnWrite;
			this.idle = false;
			this.closed = false;
			activate();
		}

		private void activate() throws IOException {
			if (!active && !closed) {
				renewReadTxn(txn, this);
				active = true;
			}
		}

		private void deactivate() {
			resetNative();
		}

		/**
		 * Resets the underlying LMDB transaction if it is currently active. Deliberately does <em>not</em> renew: the
		 * renew happens lazily in {@link #activate()}. This avoids reset/renew churn on every write commit and removes
		 * the {@code open -> Txn} vs. {@code Txn -> open} lock-order inversion of the previous implementation.
		 */
		private void resetNative() {
			if (active) {
				mdb_txn_reset(txn);
				active = false;
				signalReaderInactive();
			}
		}

		/**
		 * Either parks this transaction in the reuse pool or aborts it, depending on {@link Mode}.
		 *
		 * @return {@code true} if the caller must return this transaction's reader permit. This is the case for every
		 *         normal completion - pooling <em>and</em> aborting - because the permit represents the right to hold a
		 *         transaction, not the existence of the native transaction. It is only {@code false} if somebody else
		 *         (i.e. {@link TxnManager#close()}) already took ownership of this transaction.
		 */
		private boolean release() {
			switch (mode) {
			case RESET:
				// keep the LMDB reader (and stay tracked in `open`) for reuse, but hand back the permit
				idle = true;
				if (offerPooled(this)) {
					// reset the snapshot lazily on next reuse, unless it was already marked stale by a write commit
					if (stale) {
						resetNative();
						stale = false;
					}
					return true;
				}
				idle = false;
				// fall through: pool full or manager closing -> abort
			case ABORT:
				if (open.remove(this)) {
					abortAndMarkClosed();
					return true;
				}
				// already reclaimed by close(); permit accounting is done there
				closed = true;
				return false;
			case NONE:
				// the caller takes over responsibility for the native transaction
				closed = true;
				open.remove(this);
				return true;
			default:
				throw new IllegalStateException("Unknown mode: " + mode);
			}
		}

		private void abortAndMarkClosed() {
			if (!closed) {
				mdb_txn_abort(txn);
			}
			active = false;
			idle = false;
			closed = true;
		}

		@Override
		public String toString() {
			return "Txn{txn=" + txn + ", version=" + version + ", active=" + active + ", idle=" + idle
					+ ", resetOnWrite=" + resetOnWrite + ", stale=" + stale + ", closed=" + closed + "}";
		}

	}
}
