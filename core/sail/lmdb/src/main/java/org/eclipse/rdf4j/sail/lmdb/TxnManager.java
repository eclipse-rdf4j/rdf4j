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
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_renew;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_reset;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.locks.StampedLock;

import org.eclipse.rdf4j.sail.lmdb.LmdbUtil.Transaction;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

/**
 * Manager for LMDB transactions.
 */
class TxnManager {

	private final Mode mode;
	private final IdentityHashMap<Txn, Boolean> active = new IdentityHashMap<>();
	private final long[] pool;
	private final StampedLock lock = new StampedLock();
	private final long env;
	private volatile int poolIndex = -1;

	TxnManager(long env, Mode mode) {
		this.env = env;
		this.mode = mode;
		this.pool = mode == Mode.RESET ? new long[128] : null;
	}

	private long startReadTxn() throws IOException {
		long readTxn;
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_txn_begin(env, NULL, MDB_RDONLY, pp));
			readTxn = pp.get(0);
		}
		return readTxn;
	}

	/**
	 * Wraps an existing transaction into a txn reference object.
	 *
	 * @param txn the existing read or write transactions
	 * @return the txn reference object
	 */
	Txn createTxn(long txn) {
		return new Txn(txn) {
			@Override
			public void close() {
				// do nothing
			}
		};
	}

	/**
	 * Creates a new read-only transaction reference.
	 *
	 * @return the new transaction reference
	 * @throws IOException if the transaction cannot be started for some reason
	 */
	Txn createReadTxn() throws IOException {
		Txn txnRef = new Txn(createReadTxnInternal());
		synchronized (active) {
			active.put(txnRef, Boolean.TRUE);
		}
		return txnRef;
	}

	long createReadTxnInternal() throws IOException {
		long txn = 0;
		if (mode == Mode.RESET) {
			synchronized (pool) {
				if (poolIndex >= 0) {
					txn = pool[poolIndex--];
				}
			}
			if (txn == 0) {
				txn = startReadTxn();
			} else {
				mdb_txn_renew(txn);
			}
		} else {
			txn = startReadTxn();
		}
		return txn;
	}

	<T> T doWith(Transaction<T> transaction) throws IOException {
		long stamp = lock.readLock();
		T ret;
		try (MemoryStack stack = stackPush()) {
			try (Txn txn = createReadTxn()) {
				ret = transaction.exec(stack, txn.get());
			}
		} finally {
			lock.unlockRead(stamp);
		}
		return ret;
	}

	/**
	 * This lock is used to globally block all transactions by using a {@link StampedLock#writeLock()}. This is required
	 * to block transactions while automatic resizing the memory map.
	 *
	 * @return lock for managed transactions
	 */
	StampedLock lock() {
		return lock;
	}

	void activate() throws IOException {
		synchronized (active) {
			for (Txn txn : active.keySet()) {
				txn.setActive(true);
			}
		}
	}

	void deactivate() throws IOException {
		synchronized (active) {
			for (Txn txn : active.keySet()) {
				txn.setActive(false);
			}
		}
	}

	void reset() throws IOException {
		synchronized (active) {
			for (Txn txn : active.keySet()) {
				txn.reset();
			}
		}
	}

	enum Mode {
		RESET,
		ABORT,
		NONE
	}

	class Txn implements Closeable, AutoCloseable {

		private long txn;
		private List<Long> staleTxns;
		private long version;

		Txn(long txn) {
			this.txn = txn;
		}

		long get() {
			return txn;
		}

		/**
		 * A {@link StampedLock#readLock()} should be acquired while working with the transaction. This is required to
		 * block transactions while automatic resizing the memory map.
		 *
		 * @return lock for managed transactions
		 */
		StampedLock lock() {
			return lock;
		}

		private void free(long txn) {
			switch (mode) {
			case RESET:
				synchronized (pool) {
					if (poolIndex < pool.length - 1) {
						mdb_txn_reset(txn);
						pool[++poolIndex] = txn;
					} else {
						mdb_txn_abort(txn);
					}
				}
				break;
			case ABORT:
				mdb_txn_abort(txn);
				break;
			case NONE:
				break;
			}
		}

		@Override
		public void close() {
			synchronized (active) {
				active.remove(this);
			}
			free(txn);
			if (staleTxns != null) {
				for (long staleTxn : staleTxns) {
					free(staleTxn);
				}
			}
		}

		/**
		 * Marks current transaction as stale as it points to "old" data.
		 */
		void reset() throws IOException {
			if (staleTxns == null) {
				staleTxns = new ArrayList<>(5);
			}
			staleTxns.add(txn);
			txn = createReadTxnInternal();
		}

		/**
		 * Triggers active state of current and stale transactions.
		 */
		void setActive(boolean active) throws IOException {
			if (active) {
				E(mdb_txn_renew(txn));
				version++;
			} else {
				mdb_txn_reset(txn);
			}
			if (staleTxns != null) {
				for (Long staleTxn : staleTxns) {
					if (active) {
						E(mdb_txn_renew(staleTxn));
					} else {
						mdb_txn_reset(staleTxn);
					}
				}
			}
		}

		long version() {
			return version;
		}
	}
}
