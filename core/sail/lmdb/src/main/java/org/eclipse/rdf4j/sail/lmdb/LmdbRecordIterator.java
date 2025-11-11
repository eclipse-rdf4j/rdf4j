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
import static org.lwjgl.util.lmdb.LMDB.MDB_GET_BOTH_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cmp;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;
import static org.lwjgl.util.lmdb.LMDB.mdb_dcmp;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.TripleIndex;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.util.GroupMatcher;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A record iterator that wraps a native LMDB iterator.
 */
class LmdbRecordIterator implements RecordIterator {
	private static final Logger log = LoggerFactory.getLogger(LmdbRecordIterator.class);
	private final Pool pool;

	private final TripleIndex index;

	private final long subj;
	private final long pred;
	private final long obj;
	private final long context;

	private final long cursor;

	private final MDBVal maxKey;
	private final MDBVal maxValue;

	private final boolean matchValues;
	private GroupMatcher groupMatcher;

	private final Txn txnRef;

	private long txnRefVersion;

	private final long txn;

	private final int dbi;

	private volatile boolean closed = false;

	private final MDBVal keyData;

	private final MDBVal valueData;

	private ByteBuffer minKeyBuf;

	private ByteBuffer minValueBuf;

	private ByteBuffer maxKeyBuf;

	private ByteBuffer maxValueBuf;

	private int lastResult;

	private final long[] quad;
	private final long[] patternQuad;

	private boolean fetchNext = false;

	private final StampedLongAdderLockManager txnLockManager;

	private final Thread ownerThread = Thread.currentThread();

	private final int indexScore;

	LmdbRecordIterator(TripleIndex index, int indexScore, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef) throws IOException {
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.context = context;
		this.patternQuad = new long[] { subj, pred, obj, context };
		this.quad = new long[] { subj, pred, obj, context };
		this.pool = Pool.get();
		this.keyData = pool.getVal();
		this.valueData = pool.getVal();
		this.index = index;
		this.indexScore = indexScore;
		// prepare min and max keys if index can be used
		// otherwise, leave as null to indicate full scan
		if (indexScore > 0) {
			minKeyBuf = pool.getKeyBuffer();
			minValueBuf = pool.getKeyBuffer();
			index.getMinEntry(minKeyBuf, minValueBuf, subj, pred, obj, context);
			minKeyBuf.flip();
			minValueBuf.flip();

			this.maxKey = pool.getVal();
			this.maxValue = pool.getVal();
			this.maxKeyBuf = pool.getKeyBuffer();
			this.maxValueBuf = pool.getKeyBuffer();
			index.getMaxEntry(maxKeyBuf, maxValueBuf, subj, pred, obj, context);
			maxKeyBuf.flip();
			maxValueBuf.flip();
			this.maxKey.mv_data(maxKeyBuf);
			this.maxValue.mv_data(maxValueBuf);
		} else {
			minKeyBuf = null;
			this.maxKey = null;
			this.maxValue = null;
		}

		this.matchValues = subj > 0 || pred > 0 || obj > 0 || context >= 0;

		this.dbi = index.getDB(explicit);
		this.txnRef = txnRef;
		this.txnLockManager = txnRef.lockManager();

		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			this.txnRefVersion = txnRef.version();
			this.txn = txnRef.get();

			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer pp = stack.mallocPointer(1);
				E(mdb_cursor_open(txn, dbi, pp));
				cursor = pp.get(0);
			}
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	@Override
	public long[] next() {
		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			if (closed) {
				log.debug("Calling next() on an LmdbRecordIterator that is already closed, returning null");
				return null;
			}

			if (txnRefVersion != txnRef.version()) {
				// TODO: None of the tests in the LMDB Store cover this case!
				// cursor must be renewed
				mdb_cursor_renew(txn, cursor);
				if (fetchNext) {
					// cursor must be positioned on last item, reuse minKeyBuf if available
					if (minKeyBuf == null) {
						minKeyBuf = pool.getKeyBuffer();
					}
					minKeyBuf.clear();
					index.toEntry(minKeyBuf, minValueBuf, quad[0], quad[1], quad[2], quad[3]);
					minKeyBuf.flip();
					minValueBuf.flip();
					keyData.mv_data(minKeyBuf);
					// use set range if entry was deleted
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
					if (lastResult == MDB_SUCCESS) {
						valueData.mv_data(minValueBuf);
						lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_GET_BOTH_RANGE);
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
					lastResult = indexScore >= 2 ? MDB_SUCCESS
							: mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
					if (lastResult == MDB_SUCCESS) {
						valueData.mv_data(minValueBuf);
						lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_GET_BOTH_RANGE);
					}
				} else {
					// set cursor to first item
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				}
			}

			while (lastResult == MDB_SUCCESS) {
				// if (maxKey != null && TripleStore.COMPARATOR.compare(keyData.mv_data(), maxKey.mv_data()) > 0) {
				int keyDiff;
				if (maxKey != null &&
						(keyDiff = mdb_cmp(txn, dbi, keyData, maxKey)) >= 0
						&& (keyDiff > 0 || mdb_dcmp(txn, dbi, valueData, maxValue) > 0)) {
					lastResult = MDB_NOTFOUND;
				} else if (notMatches()) {
					// value doesn't match search key/mask, fetch next value
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				} else {
					// Matching value found
					index.entryToQuad(keyData.mv_data(), valueData.mv_data(), patternQuad, quad);
					// fetch next value
					fetchNext = true;
					return quad;
				}
			}
			closeInternal(false);
			return null;
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	private boolean notMatches() {
		if (groupMatcher != null) {
			return !this.groupMatcher.matches(keyData.mv_data(), valueData.mv_data());
		} else if (matchValues) {
			// lazy init of group matcher
			this.groupMatcher = index.createMatcher(subj, pred, obj, context);
			return !this.groupMatcher.matches(keyData.mv_data(), valueData.mv_data());
		} else {
			return false;
		}
	}

	private void closeInternal(boolean maybeCalledAsync) {
		if (!closed) {
			long writeStamp = 0L;
			boolean writeLocked = false;
			if (maybeCalledAsync && ownerThread != Thread.currentThread()) {
				try {
					writeStamp = txnLockManager.writeLock();
					writeLocked = true;
				} catch (InterruptedException e) {
					throw new SailException(e);
				}
			}
			try {
				if (!closed) {
					mdb_cursor_close(cursor);
					pool.free(keyData);
					pool.free(valueData);
					if (minKeyBuf != null) {
						pool.free(minKeyBuf);
						pool.free(minValueBuf);
					}
					if (maxKey != null) {
						pool.free(maxKeyBuf);
						pool.free(maxKey);
						pool.free(maxValueBuf);
						pool.free(maxValue);
					}
				}
			} finally {
				closed = true;
				if (writeLocked) {
					txnLockManager.unlockWrite(writeStamp);
				}
			}
		}
	}

	@Override
	public void close() {
		closeInternal(true);
	}
}
