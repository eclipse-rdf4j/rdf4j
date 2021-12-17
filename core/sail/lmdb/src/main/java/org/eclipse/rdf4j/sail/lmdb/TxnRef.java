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
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.LMDB;

/**
 * Reference for a LMDB transaction.
 */
class TxnRef {

	private final Mode mode;
	private final Map<Thread, TxnState> state = new HashMap<>();
	private final Txn[] pool;
	private long env;
	private volatile int poolIndex = -1;

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

	private Txn createTxnInternal() {
		long readTxn;
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_txn_begin(env, NULL, MDB_RDONLY, pp));
			readTxn = pp.get(0);
		}
		return new Txn(readTxn);
	}

	long create() {
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
				if (mode == Mode.RESET) {
					synchronized (state) {
						if (poolIndex >= 0) {
							currentTxn = pool[poolIndex--];
						}
					}
					if (currentTxn == null) {
						currentTxn = createTxnInternal();
					} else {
						LMDB.mdb_txn_renew(currentTxn.txn);
						currentTxn.refCount = 0;
					}
				} else {
					currentTxn = createTxnInternal();
				}
				s.currentTxn = currentTxn;
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
						LMDB.mdb_txn_reset(txn);
						pool[++poolIndex] = t;
					} else {
						LMDB.mdb_txn_abort(txn);
					}
					break;
				case ABORT:
					LMDB.mdb_txn_abort(txn);
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

	void invalidate() {
		synchronized (state) {
			state.values().forEach(s -> s.invalidate());
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

		void invalidate() {
			if (currentTxn != null) {
				if (staleTxns == null) {
					staleTxns = new ArrayList<>(5);
				}
				staleTxns.add(currentTxn);
				currentTxn = null;
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
