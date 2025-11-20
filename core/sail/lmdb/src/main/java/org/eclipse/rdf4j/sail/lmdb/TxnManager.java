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
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_renew;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_reset;

import java.io.Closeable;
import java.io.IOException;
import java.util.IdentityHashMap;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
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
	private final StampedLongAdderLockManager lockManager = new StampedLongAdderLockManager();
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
		long readStamp;
		try {
			readStamp = lockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		T ret;
		try (MemoryStack stack = stackPush()) {
			try (Txn txn = createReadTxn()) {
				ret = transaction.exec(stack, txn.get());
			}
		} finally {
			lockManager.unlockRead(readStamp);
		}
		return ret;
	}

	/**
	 * Exposes the shared lock manager used to coordinate read and write transactions.
	 */
	StampedLongAdderLockManager lockManager() {
		return lockManager;
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
		private static final int MAX_DBI = 16;

		private final long txn;
		private long version;
		private final long[][] cursorPool = new long[MAX_DBI][64];
		private final int[] cursorPoolIndex = new int[MAX_DBI];

		Txn(long txn) {
			this.txn = txn;
		}

		long get() {
			return txn;
		}

		StampedLongAdderLockManager lockManager() {
			return lockManager;
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

		long getCursor(int dbi) throws IOException {
			synchronized (cursorPool[dbi]) {
				if (cursorPoolIndex[dbi] > 0) {
					return cursorPool[dbi][--cursorPoolIndex[dbi]];
				}
			}
			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer pp = stack.mallocPointer(1);
				E(mdb_cursor_open(txn, dbi, pp));
				return pp.get(0);
			}
		}

		void returnCursor(int dbi, long cursor) {
			synchronized (cursorPool[dbi]) {
				if (cursorPoolIndex[dbi] < cursorPool[dbi].length) {
					cursorPool[dbi][cursorPoolIndex[dbi]++] = cursor;
				} else {
					mdb_cursor_close(cursor);
				}
			}
		}

		void closeCursors() {
			for (int i = 0; i < cursorPool.length; i++) {
				synchronized (cursorPool[i]) {
					while (cursorPoolIndex[i] > 0) {
						mdb_cursor_close(cursorPool[i][--cursorPoolIndex[i]]);
					}
				}
			}
		}

		@Override
		public void close() {
			closeCursors();
			synchronized (active) {
				active.remove(this);
			}
			free(txn);
		}

		/**
		 * Resets current transaction as it points to "old" data.
		 */
		void reset() throws IOException {
			mdb_txn_reset(txn);
			E(mdb_txn_renew(txn));
			version++;
			closeCursors();
		}

		/**
		 * Triggers active state of current transaction.
		 */
		void setActive(boolean active) throws IOException {
			if (active) {
				E(mdb_txn_renew(txn));
				version++;
			} else {
				mdb_txn_reset(txn);
			}
		}

		long version() {
			return version;
		}
	}
}
