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
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cmp;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.KeyBuilder;
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
	private final Pool pool = Pool.get();

	private TripleIndex index;

	private long subj;
	private long pred;
	private long obj;
	private long context;

	private long cursor;

	private MDBVal maxKey;

	private boolean matchValues;
	private GroupMatcher groupMatcher;
	private GroupMatcher matcherForEvaluation;

	/**
	 * True when late-bound variables exist beyond the contiguous prefix of the chosen index order, requiring
	 * value-level filtering. When false, range bounds already guarantee that every visited key matches and the
	 * GroupMatcher is redundant.
	 */
	private boolean needMatcher;

	private Txn txnRef;

	private long txnRefVersion;

	private long txn;

	private int dbi;

	private volatile boolean closed = false;

	private MDBVal keyData;

	private MDBVal valueData;

	private ByteBuffer minKeyBuf;

	private ByteBuffer maxKeyBuf;
	private boolean externalMinKeyBuf;
	private boolean externalMaxKeyBuf;

	private int lastResult;

	private long[] quad;

	private boolean fetchNext = false;

	private StampedLongAdderLockManager txnLockManager;

	private final Thread ownerThread = Thread.currentThread();

	private boolean initialized = false;

	LmdbRecordIterator(TripleIndex index, boolean rangeSearch, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef) throws IOException {
		this(index, null, rangeSearch, subj, pred, obj, context, explicit, txnRef, null, null, null);
	}

	LmdbRecordIterator(TripleIndex index, boolean rangeSearch, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef, long[] quadReuse) throws IOException {
		this(index, null, rangeSearch, subj, pred, obj, context, explicit, txnRef, quadReuse, null, null);
	}

	LmdbRecordIterator(TripleIndex index, boolean rangeSearch, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef, long[] quadReuse, ByteBuffer minKeyBufParam,
			ByteBuffer maxKeyBufParam) throws IOException {
		this(index, null, rangeSearch, subj, pred, obj, context, explicit, txnRef, quadReuse, minKeyBufParam,
				maxKeyBufParam);
	}

	LmdbRecordIterator(TripleIndex index, KeyBuilder keyBuilder, boolean rangeSearch, long subj,
			long pred, long obj, long context, boolean explicit, Txn txnRef) throws IOException {
		this(index, keyBuilder, rangeSearch, subj, pred, obj, context, explicit, txnRef, null, null, null);
	}

	LmdbRecordIterator(TripleIndex index, KeyBuilder keyBuilder, boolean rangeSearch, long subj,
			long pred, long obj, long context, boolean explicit, Txn txnRef, long[] quadReuse,
			ByteBuffer minKeyBufParam, ByteBuffer maxKeyBufParam) throws IOException {
		initializeInternal(index, keyBuilder, rangeSearch, subj, pred, obj, context, explicit, txnRef, quadReuse,
				minKeyBufParam, maxKeyBufParam);
		initialized = true;
	}

	void initialize(TripleIndex index, KeyBuilder keyBuilder, boolean rangeSearch, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef, long[] quadReuse, ByteBuffer minKeyBufParam,
			ByteBuffer maxKeyBufParam) throws IOException {
		if (initialized && !closed) {
			// prepareForReuse();
		}
		initializeInternal(index, keyBuilder, rangeSearch, subj, pred, obj, context, explicit, txnRef, quadReuse,
				minKeyBufParam, maxKeyBufParam);
		initialized = true;
	}

	private void initializeInternal(TripleIndex index, KeyBuilder keyBuilder, boolean rangeSearch, long subj,
			long pred, long obj, long context, boolean explicit, Txn txnRef, long[] quadReuse,
			ByteBuffer minKeyBufParam, ByteBuffer maxKeyBufParam) throws IOException {

//		if (!initialized) {
//			System.out.println();
//		} else {
//			System.out.println();
//		}

		this.index = index;
		long prevSubj = minKeyBuf != null ? this.subj : TripleStore.NO_PREVIOUS_ID;
		long prevPred = minKeyBuf != null ? this.pred : TripleStore.NO_PREVIOUS_ID;
		long prevObj = minKeyBuf != null ? this.obj : TripleStore.NO_PREVIOUS_ID;
		long prevContext = minKeyBuf != null ? this.context : TripleStore.NO_PREVIOUS_ID;

		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.context = context;

		boolean prevExternalMinKeyBuf = this.externalMinKeyBuf;
		boolean prevExternalMaxKeyBuf = this.externalMaxKeyBuf;

		if (quadReuse != null && quadReuse.length >= 4) {
			this.quad = quadReuse;
		} else if (this.quad == null || this.quad.length < 4) {
			this.quad = new long[] { subj, pred, obj, context };
		}
		this.quad[0] = subj;
		this.quad[1] = pred;
		this.quad[2] = obj;
		this.quad[3] = context;

		if (this.keyData == null) {
			this.keyData = pool.getVal();
		}
		if (this.valueData == null) {
			this.valueData = pool.getVal();
		}

		if (rangeSearch) {
			this.externalMinKeyBuf = minKeyBufParam != null;
			if (externalMinKeyBuf) {
				this.minKeyBuf = minKeyBufParam;
			} else {
				if (this.minKeyBuf == null || prevExternalMinKeyBuf) {
					this.minKeyBuf = pool.getKeyBuffer();
				} else {
					this.minKeyBuf.clear();
				}
			}
			minKeyBuf.clear();
			if (keyBuilder != null) {
				keyBuilder.writeMin(minKeyBuf);
			} else {
				index.getMinKey(minKeyBuf, subj, pred, obj, context, prevSubj, prevPred, prevObj, prevContext);
			}
			minKeyBuf.flip();

			if (this.maxKey == null) {
				this.maxKey = pool.getVal();
			}
			this.externalMaxKeyBuf = maxKeyBufParam != null;
			if (externalMaxKeyBuf) {
				this.maxKeyBuf = maxKeyBufParam;
			} else {
				if (this.maxKeyBuf == null || prevExternalMaxKeyBuf) {
					this.maxKeyBuf = pool.getKeyBuffer();
				} else {
					this.maxKeyBuf.clear();
				}
			}
			maxKeyBuf.clear();
			if (keyBuilder != null) {
				keyBuilder.writeMax(maxKeyBuf);
			} else {
				index.getMaxKey(maxKeyBuf, subj, pred, obj, context);
			}
			maxKeyBuf.flip();
			this.maxKey.mv_data(maxKeyBuf);
		} else {
			if (this.maxKey != null) {
				pool.free(maxKey);
				this.maxKey = null;
			}
			if (this.maxKeyBuf != null && !prevExternalMaxKeyBuf) {
				pool.free(maxKeyBuf);
				this.maxKeyBuf = null;
			}
			this.externalMaxKeyBuf = maxKeyBufParam != null;
			this.maxKeyBuf = externalMaxKeyBuf ? maxKeyBufParam : null;

			if (subj > 0 || pred > 0 || obj > 0 || context >= 0) {
				this.externalMinKeyBuf = minKeyBufParam != null;
				if (externalMinKeyBuf) {
					this.minKeyBuf = minKeyBufParam;
				} else {
					if (this.minKeyBuf == null || prevExternalMinKeyBuf) {
						this.minKeyBuf = pool.getKeyBuffer();
					} else {
						this.minKeyBuf.clear();
					}
				}
				minKeyBuf.clear();
				index.getMinKey(minKeyBuf, subj, pred, obj, context, prevSubj, prevPred, prevObj, prevContext);
				minKeyBuf.flip();
			} else {
				if (this.minKeyBuf != null && !prevExternalMinKeyBuf) {
					pool.free(minKeyBuf);
					this.minKeyBuf = null;
				}
				this.externalMinKeyBuf = minKeyBufParam != null;
				if (externalMinKeyBuf) {
					this.minKeyBuf = minKeyBufParam;
				} else {
					this.minKeyBuf = null;
				}
			}
		}

		this.matchValues = subj > 0 || pred > 0 || obj > 0 || context >= 0;
		int prefixLen = index.getPatternScore(subj, pred, obj, context);
		int boundCount = (subj > 0 ? 1 : 0) + (pred > 0 ? 1 : 0) + (obj > 0 ? 1 : 0) + (context >= 0 ? 1 : 0);
		this.needMatcher = boundCount > prefixLen;
		this.groupMatcher = null;
		this.matcherForEvaluation = null;
		this.fetchNext = false;
		this.lastResult = MDB_SUCCESS;
		this.closed = false;

		if (!initialized) {
			this.dbi = index.getDB(explicit);
			this.txnRef = txnRef;
			this.txnLockManager = txnRef.lockManager();
		}

		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			this.txnRefVersion = txnRef.version();
			this.txn = txnRef.get();

			// Try to reuse a pooled cursor only for read-only transactions; otherwise open a new one
			if (txnRef.isReadOnly()) {
				if (cursor == 0L) {
					cursor = pool.getCursor(dbi, index);
				}

				if (cursor != 0L) {
					long c = cursor;
					try {
						E(mdb_cursor_renew(txn, c));
					} catch (IOException renewEx) {
						// Renewal failed (e.g., incompatible txn). Close pooled cursor and open a fresh one.
						mdb_cursor_close(c);
						try (MemoryStack stack = MemoryStack.stackPush()) {
							PointerBuffer pp = stack.mallocPointer(1);
							E(mdb_cursor_open(txn, dbi, pp));
							c = pp.get(0);
						}
					}
					cursor = c;
				} else {
					try (MemoryStack stack = MemoryStack.stackPush()) {
						PointerBuffer pp = stack.mallocPointer(1);
						E(mdb_cursor_open(txn, dbi, pp));
						cursor = pp.get(0);
					}
				}
			} else {
				if (cursor != 0L) {
					pool.freeCursor(dbi, index, cursor);
					cursor = 0L;
				}

				try (MemoryStack stack = MemoryStack.stackPush()) {
					PointerBuffer pp = stack.mallocPointer(1);
					E(mdb_cursor_open(txn, dbi, pp));
					cursor = pp.get(0);
				}
			}
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	@Override
	public long[] next() {
		if (closed) {
			return null;
		}
		StampedLongAdderLockManager manager = txnLockManager;
		if (manager == null) {
			throw new SailException("Iterator not initialized");
		}
		long readStamp;
		try {
			readStamp = manager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
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
					index.toKey(minKeyBuf, quad[0], quad[1], quad[2], quad[3]);
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
				// if (maxKey != null && TripleStore.COMPARATOR.compare(keyData.mv_data(), maxKey.mv_data()) > 0) {
				if (maxKey != null && mdb_cmp(txn, dbi, keyData, maxKey) > 0) {
					lastResult = MDB_NOTFOUND;
				} else if (matches()) {
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				} else {
					// Matching value found
					index.keyToQuad(keyData.mv_data(), subj, pred, obj, context, quad);
					// fetch next value
					fetchNext = true;
					return quad;
				}
			}
//			closeInternal(false);
			return null;
		} finally {
			manager.unlockRead(readStamp);
		}
	}

	private boolean matches() {
		// When there are no late-bound variables beyond the contiguous prefix, range bounds fully determine matches.
		if (!needMatcher) {
			return false;
		}

		if (matcherForEvaluation != null) {
			return !matcherForEvaluation.matches(keyData.mv_data());
		} else if (matchValues) {
			matcherForEvaluation = index.createMatcher(subj, pred, obj, context);
			groupMatcher = matcherForEvaluation;
			return !matcherForEvaluation.matches(keyData.mv_data());
		} else {
			return false;
		}
	}

	private void prepareForReuse() {
		if (cursor != 0L && txnRef != null) {
			if (txnRef.isReadOnly()) {
				pool.freeCursor(dbi, index, cursor);
			} else {
				mdb_cursor_close(cursor);
			}
		}
		cursor = 0L;
		groupMatcher = null;
		matcherForEvaluation = null;
		fetchNext = false;
		lastResult = MDB_SUCCESS;
		matchValues = false;
		needMatcher = false;
		txnRef = null;
		txn = 0L;
		txnRefVersion = 0L;
		txnLockManager = null;
		closed = true;
	}

	private void closeInternal(boolean maybeCalledAsync) {
		StampedLongAdderLockManager manager = this.txnLockManager;
		if (closed) {
			return;
		}
		long writeStamp = 0L;
		boolean writeLocked = false;
		if (maybeCalledAsync && ownerThread != Thread.currentThread() && manager != null) {
			try {
				writeStamp = manager.writeLock();
				writeLocked = true;
			} catch (InterruptedException e) {
				throw new SailException(e);
			}
		}
		try {
			if (cursor != 0L && txnRef != null) {
				if (txnRef.isReadOnly()) {
					pool.freeCursor(dbi, index, cursor);
				} else {
					mdb_cursor_close(cursor);
				}
				cursor = 0L;
			}
			if (keyData != null) {
				pool.free(keyData);
				keyData = null;
			}
			if (valueData != null) {
				pool.free(valueData);
				valueData = null;
			}
			if (minKeyBuf != null && !externalMinKeyBuf) {
				pool.free(minKeyBuf);
			}
			if (maxKeyBuf != null && !externalMaxKeyBuf) {
				pool.free(maxKeyBuf);
			}
			if (maxKey != null) {
				pool.free(maxKey);
				maxKey = null;
			}
			minKeyBuf = null;
			maxKeyBuf = null;
			externalMinKeyBuf = false;
			externalMaxKeyBuf = false;
			if (maybeCalledAsync) {
				groupMatcher = null;
			}
			matcherForEvaluation = null;
			fetchNext = false;
			lastResult = 0;
			matchValues = false;
			needMatcher = false;
			txnRef = null;
			txn = 0L;
			dbi = 0;
			index = null;
		} finally {
			closed = true;
			if (writeLocked) {
				manager.unlockWrite(writeStamp);
			}
			txnLockManager = null;
		}
	}

	@Override
	public void close() {
		closeInternal(true);
	}
}
