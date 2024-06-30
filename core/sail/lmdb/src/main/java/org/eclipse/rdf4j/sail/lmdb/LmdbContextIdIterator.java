/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
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
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.StampedLock;

import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * An iterator for context IDs of the LMDB triple store.
 */
class LmdbContextIdIterator implements Closeable {
	private final Pool pool;

	private final long cursor;

	private final Txn txnRef;

	private long txnRefVersion;

	private final long txn;

	private final int dbi;

	private volatile boolean closed = false;

	private final MDBVal keyData;

	private final MDBVal valueData;

	private ByteBuffer minKeyBuf;

	private int lastResult;

	private final long[] record = new long[1];

	private boolean fetchNext = false;

	private final StampedLock txnLock;

	private final Thread ownerThread = Thread.currentThread();

	LmdbContextIdIterator(Pool pool, int dbi, Txn txnRef) throws IOException {
		this.pool = pool;
		this.keyData = pool.getVal();
		this.valueData = pool.getVal();

		this.dbi = dbi;
		this.txnRef = txnRef;
		this.txnLock = txnRef.lock();

		long stamp = txnLock.readLock();
		try {
			this.txnRefVersion = txnRef.version();
			this.txn = txnRef.get();

			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer pp = stack.mallocPointer(1);
				E(mdb_cursor_open(txn, dbi, pp));
				cursor = pp.get(0);
			}
		} finally {
			txnLock.unlockRead(stamp);
		}
	}

	public long[] next() {
		long stamp = txnLock.readLock();
		try {
			if (txnRefVersion != txnRef.version()) {
				// cursor must be renewed
				E(mdb_cursor_renew(txn, cursor));
				if (fetchNext) {
					// cursor must be positioned on last item, reuse minKeyBuf if available
					if (minKeyBuf == null) {
						minKeyBuf = pool.getKeyBuffer();
					}
					minKeyBuf.clear();
					Varint.writeUnsigned(minKeyBuf, record[0]);
					minKeyBuf.flip();
					keyData.mv_data(minKeyBuf);
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET);
					if (lastResult != MDB_SUCCESS) {
						// use MDB_SET_RANGE if key was deleted
						lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
					}
					if (lastResult != MDB_SUCCESS) {
						closeInternal(false);
						return null;
					}
				}
				// update version of txn ref
				this.txnRefVersion = txnRef.version();
			}

			if (fetchNext) {
				lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				fetchNext = false;
			} else {
				if (minKeyBuf != null) {
					// set cursor to min key
					keyData.mv_data(minKeyBuf);
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
				} else {
					// set cursor to first item
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				}
			}

			while (lastResult == MDB_SUCCESS) {
				record[0] = Varint.readUnsigned(keyData.mv_data());
				// fetch next value
				fetchNext = true;
				return record;
			}
			closeInternal(false);
			return null;
		} catch (IOException e) {
			throw new SailException(e);
		} finally {
			txnLock.unlockRead(stamp);
		}
	}

	private void closeInternal(boolean maybeCalledAsync) {
		if (!closed) {
			long stamp;
			if (maybeCalledAsync && ownerThread != Thread.currentThread()) {
				stamp = txnLock.writeLock();
			} else {
				stamp = 0;
			}
			try {
				if (!closed) {
					mdb_cursor_close(cursor);
					pool.free(keyData);
					pool.free(valueData);
					if (minKeyBuf != null) {
						pool.free(minKeyBuf);
					}
				}
			} finally {
				closed = true;
				if (stamp != 0) {
					txnLock.unlockWrite(stamp);
				}
			}
		}
	}

	@Override
	public void close() {
		closeInternal(true);
	}
}
