/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

import org.eclipse.rdf4j.sail.lmdb.LmdbUtil.Transaction;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

/**
 * Reference for a LMDB transaction.
 */
class TxnRef {

	private final Mode mode;
	private final Map<Thread, TxnState> state = new HashMap<>();
	private final Txn[] pool;
	private long env;
	private volatile int poolIndex = -1;

	private volatile long version = 1;
	private final StampedLock lock = new StampedLock();

	TxnRef(long txn) {
		synchronized (state) {
			state.put(Thread.currentThread(), new TxnState(new Txn(txn)));
			mode = Mode.NONE;
			pool = null;
		}
	}

	TxnRef(long env, Mode mode) {
		this.env = env;
		this.mode = mode;
		this.pool = mode == Mode.RESET ? new Txn[128] : null;
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

	private Txn createTxnInternal() throws IOException {
		Txn newTxn = null;
		if (mode == Mode.RESET) {
			synchronized (state) {
				if (poolIndex >= 0) {
					newTxn = pool[poolIndex--];
				}
			}
			if (newTxn == null) {
				newTxn = new Txn(startReadTxn());
			} else {
				mdb_txn_renew(newTxn.txn);
				newTxn.refCount = 0;
			}
		} else {
			newTxn = new Txn(startReadTxn());
		}
		return newTxn;
	}

	long create() throws IOException {
		TxnState s;
		synchronized (state) {
			s = state.get(Thread.currentThread());
		}
		Txn currentTxn;
		if (s == null) {
			s = new TxnState(createTxnInternal());
			currentTxn = s.currentTxn;
			synchronized (state) {
				state.put(Thread.currentThread(), s);
			}
		} else {
			currentTxn = s.currentTxn;
			if (currentTxn == null) {
				s.currentTxn = currentTxn = createTxnInternal();
			}
		}
		currentTxn.refCount++;
		return currentTxn.txn;
	}

	void free(long txn) {
		synchronized (state) {
			TxnState s = state.get(Thread.currentThread());
			Txn t = s.currentTxn;
			if (t == null || t.txn != txn) {
				// check if a stale transaction has to be closed
				t = s.staleTxns.stream().filter(oldTxn -> oldTxn.txn == txn).findFirst().get();
			}
			if (--t.refCount <= 0) {
				switch (mode) {
				case RESET:
					if (poolIndex < pool.length - 1) {
						mdb_txn_reset(txn);
						pool[++poolIndex] = t;
					} else {
						mdb_txn_abort(txn);
					}
					break;
				case ABORT:
					mdb_txn_abort(txn);
					break;
				case NONE:
					break;
				}

				if (t == s.currentTxn) {
					if (s.staleTxns == null || s.staleTxns.isEmpty()) {
						// remove complete state object
						state.remove(Thread.currentThread());
					} else {
						// reset only currentTxn
						s.currentTxn = null;
					}
				} else {
					if (s.staleTxns.size() == 1) {
						// remove complete state object
						state.remove(Thread.currentThread());
					} else {
						// remove only the stale txn
						s.staleTxns.remove(t);
					}
				}
			}
		}
	}

	<T> T doWith(Transaction<T> transaction) throws IOException {
		long stamp = lock.readLock();
		T ret;
		try (MemoryStack stack = stackPush()) {
			long txn = create();
			try {
				ret = transaction.exec(stack, txn);
			} finally {
				free(txn);
			}
		} finally {
			lock.unlockRead(stamp);
		}
		return ret;
	}

	StampedLock lock() {
		return lock;
	}

	long version() {
		return version;
	}

	void activate() {
		synchronized (state) {
			state.values().forEach(s -> s.setActive(true));
			version++;
		}
	}

	void deactivate() {
		synchronized (state) {
			state.values().forEach(s -> s.setActive(false));
		}
	}

	/**
	 * Resets the transactions for all registered threads to ensure they are renewed next time a new transaction is
	 * requested.
	 */
	void reset() {
		synchronized (state) {
			state.values().forEach(s -> s.reset());
		}
	}

	enum Mode {
		RESET,
		ABORT,
		NONE
	}

	static class TxnState {

		Txn currentTxn;
		List<Txn> staleTxns;

		TxnState(Txn txn) {
			this.currentTxn = txn;
		}

		/**
		 * Marks current transaction as stale as it points to "old" data.
		 */
		void reset() {
			if (currentTxn != null) {
				if (staleTxns == null) {
					staleTxns = new ArrayList<>(5);
				}
				staleTxns.add(currentTxn);
				currentTxn = null;
			}
		}

		/**
		 * Triggers active state of current and stale transactions.
		 */
		void setActive(boolean active) {
			if (currentTxn != null) {
				if (active) {
					mdb_txn_renew(currentTxn.txn);
				} else {
					mdb_txn_reset(currentTxn.txn);
				}
			}
			if (staleTxns != null) {
				for (Txn staleTxn : staleTxns) {
					if (active) {
						mdb_txn_renew(staleTxn.txn);
					} else {
						mdb_txn_reset(staleTxn.txn);
					}
				}
			}
		}
	}

	static class Txn {

		long txn;
		long refCount;

		Txn(long txn) {
			this.txn = txn;
		}
	}
}