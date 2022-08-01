/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
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
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.mdb_cmp;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.StampedLock;

import org.eclipse.rdf4j.sail.lmdb.TripleStore.TripleIndex;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.Varint.GroupMatcher;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A record iterator that wraps a native LMDB iterator.
 *
 */
class LmdbRecordIterator implements RecordIterator {
	private final Pool pool;

	private final TripleIndex index;

	private final long cursor;

	private final MDBVal maxKey;

	private final GroupMatcher groupMatcher;

	private final Txn txnRef;

	private long txnRefVersion;

	private final long txn;

	private final int dbi;

	private boolean closed = false;

	private final MDBVal keyData;

	private final MDBVal valueData;

	private ByteBuffer minKeyBuf;

	private ByteBuffer maxKeyBuf;

	private int lastResult;

	private final long[] quad = new long[4];

	private boolean fetchNext = false;

	private final StampedLock txnLock;

	LmdbRecordIterator(Pool pool, TripleIndex index, boolean rangeSearch, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef) throws IOException {
		this.pool = pool;
		this.keyData = pool.getVal();
		this.valueData = pool.getVal();
		this.index = index;
		if (rangeSearch) {
			minKeyBuf = pool.getKeyBuffer();
			index.getMinKey(minKeyBuf, subj, pred, obj, context);
			minKeyBuf.flip();

			this.maxKey = pool.getVal();
			this.maxKeyBuf = pool.getKeyBuffer();
			index.getMaxKey(maxKeyBuf, subj, pred, obj, context);
			maxKeyBuf.flip();
			this.maxKey.mv_data(maxKeyBuf);
		} else {
			minKeyBuf = null;
			this.maxKey = null;
		}

		boolean matchValues = subj > 0 || pred > 0 || obj > 0 || context >= 0;
		if (matchValues) {
			this.groupMatcher = index.createMatcher(subj, pred, obj, context);
		} else {
			this.groupMatcher = null;
		}
		this.dbi = index.getDB(explicit);
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

	@Override
	public long[] next() throws IOException {
		long stamp = txnLock.readLock();
		try {
			if (txnRefVersion != txnRef.version()) {
				// cursor must be renewed
				mdb_cursor_renew(txn, cursor);
				if (fetchNext) {
					// cursor must be positioned on last item, reuse minKeyBuf if available
					if (minKeyBuf == null) {
						minKeyBuf = pool.getKeyBuffer();
					}
					minKeyBuf.clear();
					index.toKey(minKeyBuf, quad[0], quad[1], quad[2], quad[3]);
					minKeyBuf.flip();
					keyData.mv_data(minKeyBuf);
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET);
					if (lastResult != 0) {
						// use MDB_SET_RANGE if key was deleted
						lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
					}
					if (lastResult != 0) {
						close();
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

			while (lastResult == 0) {
				// if (maxKey != null && TripleStore.COMPARATOR.compare(keyData.mv_data(), maxKey.mv_data()) > 0) {
				if (maxKey != null && mdb_cmp(txn, dbi, keyData, maxKey) > 0) {
					lastResult = MDB_NOTFOUND;
				} else if (groupMatcher != null && !groupMatcher.matches(keyData.mv_data())) {
					// value doesn't match search key/mask, fetch next value
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				} else {
					// Matching value found
					index.keyToQuad(keyData.mv_data(), quad);
					// fetch next value
					fetchNext = true;
					return quad;
				}
			}
			close();
			return null;
		} finally {
			txnLock.unlockRead(stamp);
		}
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			try {
				mdb_cursor_close(cursor);
				pool.free(keyData);
				pool.free(valueData);
				if (minKeyBuf != null) {
					pool.free(minKeyBuf);
				}
				if (maxKey != null) {
					pool.free(maxKeyBuf);
					pool.free(maxKey);
				}
			} finally {
				closed = true;
			}
		}
	}
}
