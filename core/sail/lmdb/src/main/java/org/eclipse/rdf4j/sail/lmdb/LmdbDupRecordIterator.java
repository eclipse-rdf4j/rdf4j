/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
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
import static org.lwjgl.util.lmdb.LMDB.MDB_GET_MULTIPLE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT_MULTIPLE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_PREV;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_KEY;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.TripleIndex;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.util.GroupMatcher;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A dupsort/dupfixed-optimized record iterator using MDB_GET_MULTIPLE/NEXT_MULTIPLE to reduce JNI calls.
 */
class LmdbDupRecordIterator implements RecordIterator {

	private final Pool pool;
	private final TripleIndex index;
	private final int dupDbi;
	private final long cursor;

	private final Txn txnRef;
	private final long txn;
	private long txnRefVersion;
	private final StampedLongAdderLockManager txnLockManager;
	private final Thread ownerThread = Thread.currentThread();

	private final MDBVal keyData;
	private final MDBVal valueData;

	private final long[] quad;
	private final long[] originalQuad;
	private final char[] fieldSeq;

	private final boolean matchValues;
	private GroupMatcher groupMatcher;

	private ByteBuffer prefixKeyBuf;
	private long[] prefixValues;

	private ByteBuffer dupBuf;
	private int dupPos;
	private int dupLimit;

	private int lastResult;
	private boolean closed = false;

	private final RecordIterator fallback;

	LmdbDupRecordIterator(TripleIndex index, long subj, long pred, long obj, long context,
			boolean explicit, Txn txnRef) throws IOException {
		this.index = index;
		this.fieldSeq = index.getFieldSeq();
		this.quad = new long[] { subj, pred, obj, context };
		this.originalQuad = new long[] { subj, pred, obj, context };
		this.matchValues = subj > 0 || pred > 0 || obj > 0 || context >= 0;

		this.pool = Pool.get();
		this.keyData = pool.getVal();
		this.valueData = pool.getVal();
		this.dupDbi = index.getDupDB(explicit);
		this.txnRef = txnRef;
		this.txnLockManager = txnRef.lockManager();

		RecordIterator fallbackIterator = null;

		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			this.txnRefVersion = txnRef.version();
			this.txn = txnRef.get();

			cursor = openCursor(txn, dupDbi, txnRef.isReadOnly());

			prefixKeyBuf = pool.getKeyBuffer();
			prefixValues = new long[2];
			long adjSubj = subj < 0 ? 0 : subj;
			long adjPred = pred < 0 ? 0 : pred;
			long adjObj = obj < 0 ? 0 : obj;
			long adjContext = context < 0 ? 0 : context;
			{
				char f = fieldSeq[0];
				long v;
				switch (f) {
					case 's':
						v = adjSubj;
						break;
					case 'p':
						v = adjPred;
						break;
					case 'o':
						v = adjObj;
						break;
					default:
						v = adjContext;
						break;
				}
				prefixValues[0] = v;
			}
			{
				char f = fieldSeq[1];
				long v;
				switch (f) {
					case 's':
						v = adjSubj;
						break;
					case 'p':
						v = adjPred;
						break;
					case 'o':
						v = adjObj;
						break;
					default:
						v = adjContext;
						break;
				}
				prefixValues[1] = v;
			}
			prefixKeyBuf.clear();
			index.toDupKeyPrefix(prefixKeyBuf, adjSubj, adjPred, adjObj, adjContext);
			prefixKeyBuf.flip();

			boolean positioned = positionOnPrefix();
			if (positioned) {
				positioned = primeDuplicateBlock();
			}
			if (!positioned) {
				closeInternal(false);
				fallbackIterator = new LmdbRecordIterator(index,
						index.getPatternScore(subj, pred, obj, context) > 0, subj, pred, obj, context, explicit,
						txnRef);
			}
		} finally {
			txnLockManager.unlockRead(readStamp);
		}

		this.fallback = fallbackIterator;
	}

	@Override
	public long[] next() {
		if (fallback != null) {
			return fallback.next();
		}

		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			if (closed) {
				return null;
			}

			if (txnRefVersion != txnRef.version()) {
				E(mdb_cursor_renew(txn, cursor));
				txnRefVersion = txnRef.version();
				if (!positionOnPrefix() || !primeDuplicateBlock()) {
					closeInternal(false);
					return null;
				}
			}

			while (true) {
				if (dupBuf != null && dupPos + (Long.BYTES * 2) <= dupLimit) {
					long v3 = dupBuf.getLong(dupPos);
					long v4 = dupBuf.getLong(dupPos + Long.BYTES);
					dupPos += Long.BYTES * 2;
					fillQuadFromPrefixAndValue(v3, v4);
					if (!matchValues || matchesQuad()) {
						return quad;
					}
					continue;
				}

				lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT_MULTIPLE);
				if (lastResult == MDB_SUCCESS) {
					resetDuplicateBuffer(valueData.mv_data());
					continue;
				}

				lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				if (lastResult != MDB_SUCCESS) {
					closeInternal(false);
					return null;
				}
				if (!currentKeyHasPrefix()) {
					if (!adjustCursorToPrefix()) {
						closeInternal(false);
						return null;
					}
				}
				if (!primeDuplicateBlock()) {
					closeInternal(false);
					return null;
				}
			}
		} catch (IOException e) {
			throw new SailException(e);
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	private boolean positionOnPrefix() throws IOException {
		keyData.mv_data(prefixKeyBuf);
		lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_KEY);
		if (lastResult == MDB_NOTFOUND) {
			lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
		}
		if (lastResult != MDB_SUCCESS) {
			return false;
		}
		if (currentKeyHasPrefix()) {
			return true;
		}
		return adjustCursorToPrefix();
	}

	private boolean adjustCursorToPrefix() throws IOException {
		int cmp = comparePrefix();
		while (cmp < 0) {
			lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
			if (lastResult != MDB_SUCCESS) {
				return false;
			}
			cmp = comparePrefix();
		}
		return cmp == 0;
	}

	private int comparePrefix() {
		ByteBuffer key = keyData.mv_data().duplicate();
		{
			long actual = Varint.readUnsigned(key);
			long expected = prefixValues[0];
			if (actual < expected) {
				return -1;
			}
			if (actual > expected) {
				return 1;
			}
		}
		{
			long actual = Varint.readUnsigned(key);
			long expected = prefixValues[1];
			if (actual < expected) {
				return -1;
			}
			if (actual > expected) {
				return 1;
			}
		}
		return 0;
	}

	private boolean primeDuplicateBlock() throws IOException {
		lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_GET_MULTIPLE);
		if (lastResult == MDB_SUCCESS) {
			resetDuplicateBuffer(valueData.mv_data());
			return dupBuf != null && dupLimit - dupPos >= Long.BYTES * 2;
		} else if (lastResult == MDB_NOTFOUND) {
			return false;
		} else {
			resetDuplicateBuffer(valueData.mv_data());
			return dupBuf != null && dupLimit - dupPos >= Long.BYTES * 2;
		}
	}

	private void resetDuplicateBuffer(ByteBuffer buffer) {
		if (buffer == null) {
			dupBuf = null;
			dupPos = dupLimit = 0;
		} else {
			buffer.order(ByteOrder.nativeOrder());
			dupBuf = buffer;
			dupPos = dupBuf.position();
			dupLimit = dupBuf.limit();
		}
	}

	private void fillQuadFromPrefixAndValue(long v3, long v4) {
		int pi = 0;
		{
			char f = fieldSeq[0];
			long v = prefixValues[pi++];
			switch (f) {
				case 's':
					quad[0] = v;
					break;
				case 'p':
					quad[1] = v;
					break;
				case 'o':
					quad[2] = v;
					break;
				case 'c':
					quad[3] = v;
					break;
			}
		}
		{
			char f = fieldSeq[1];
			long v = prefixValues[pi++];
			switch (f) {
				case 's':
					quad[0] = v;
					break;
				case 'p':
					quad[1] = v;
					break;
				case 'o':
					quad[2] = v;
					break;
				case 'c':
					quad[3] = v;
					break;
			}
		}
		char f = fieldSeq[2];
		switch (f) {
		case 's':
			quad[0] = v3;
			break;
		case 'p':
			quad[1] = v3;
			break;
		case 'o':
			quad[2] = v3;
			break;
		case 'c':
			quad[3] = v3;
			break;
		}
		f = fieldSeq[3];
		switch (f) {
		case 's':
			quad[0] = v4;
			break;
		case 'p':
			quad[1] = v4;
			break;
		case 'o':
			quad[2] = v4;
			break;
		case 'c':
			quad[3] = v4;
			break;
		}
	}

	private boolean currentKeyHasPrefix() {
		return comparePrefix() == 0;
	}

	private boolean matchesQuad() {
		return (originalQuad[0] < 0 || quad[0] == originalQuad[0])
				&& (originalQuad[1] < 0 || quad[1] == originalQuad[1])
				&& (originalQuad[2] < 0 || quad[2] == originalQuad[2])
				&& (originalQuad[3] < 0 || quad[3] == originalQuad[3]);
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
					if (txnRef.isReadOnly()) {
						pool.freeCursor(dupDbi, index, cursor);
					} else {
						mdb_cursor_close(cursor);
					}
					pool.free(keyData);
					pool.free(valueData);
					if (prefixKeyBuf != null) {
						pool.free(prefixKeyBuf);
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
		if (fallback != null) {
			fallback.close();
		} else {
			closeInternal(true);
		}
	}

	private long openCursor(long txn, int dbi, boolean tryReuse) throws IOException {
		if (tryReuse) {
			long pooled = pool.getCursor(dbi, index);
			if (pooled != 0L) {
				try {
					E(mdb_cursor_renew(txn, pooled));
					return pooled;
				} catch (IOException renewEx) {
					mdb_cursor_close(pooled);
				}
			}
		}
		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_cursor_open(txn, dbi, pp));
			return pp.get(0);
		}
	}
}
