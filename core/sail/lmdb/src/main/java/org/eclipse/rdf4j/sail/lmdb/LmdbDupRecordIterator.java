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
import static org.lwjgl.util.lmdb.LMDB.MDB_GET_CURRENT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT_DUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_KEY;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.DupIndex;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A dupsort/dupfixed-optimized record iterator using MDB_GET_MULTIPLE/NEXT_MULTIPLE to reduce JNI calls.
 *
 * This iterator is hard coded to only work with the SP index and assumes that the object and context keys are free (-1),
 */
class LmdbDupRecordIterator implements RecordIterator {

	@FunctionalInterface
	interface FallbackSupplier {
		RecordIterator get() throws IOException;
	}

	private final Pool pool;
	private final DupIndex index;
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

	private final boolean matchValues;

	private ByteBuffer prefixKeyBuf;
	private long[] prefixValues;

	private boolean hasCurrentDuplicate = false;
	private long currentObj;
	private long currentContext;

	private int lastResult;
	private boolean closed = false;

	private final RecordIterator fallback;
	private final FallbackSupplier fallbackSupplier;

	LmdbDupRecordIterator(DupIndex index, long subj, long pred,
			boolean explicit, Txn txnRef, FallbackSupplier fallbackSupplier) throws IOException {
		this.index = index;

		this.quad = new long[] { subj, pred, -1, -1 };
		this.originalQuad = new long[] { subj, pred, -1, -1 };
		this.matchValues = subj > 0 || pred > 0;
		this.fallbackSupplier = fallbackSupplier;

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
			prefixValues = new long[] { subj, pred };
			prefixKeyBuf.clear();
			Varint.writeUnsigned(prefixKeyBuf, subj);
			Varint.writeUnsigned(prefixKeyBuf, pred);

//			index.toDupKeyPrefix(prefixKeyBuf, subj, pred, 0, 0);
			prefixKeyBuf.flip();

			boolean positioned = positionOnPrefix();
			if (positioned) {
				positioned = loadDuplicate(MDB_GET_CURRENT);
			}
			if (!positioned) {
				closeInternal(false);
				fallbackIterator = createFallback();
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
				if (!positionOnPrefix() || !loadDuplicate(MDB_GET_CURRENT)) {
					closeInternal(false);
					return null;
				}
			}

			while (true) {
				if (!hasCurrentDuplicate) {
					closeInternal(false);
					return null;
				}

				long v3 = currentObj;
				long v4 = currentContext;
				if (!loadDuplicate(MDB_NEXT_DUP)) {
					hasCurrentDuplicate = false;
				}
				fillQuadFromPrefixAndValue(v3, v4);
				return quad;
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

	private boolean loadDuplicate(int op) throws IOException {
		lastResult = mdb_cursor_get(cursor, keyData, valueData, op);
		if (lastResult == MDB_SUCCESS) {
			readCurrentDuplicate(valueData.mv_data());
			hasCurrentDuplicate = true;
			return true;
		} else if (lastResult == MDB_NOTFOUND) {
			hasCurrentDuplicate = false;
			return false;
		} else {
			throw new IOException("Failed to load duplicate, lmdb error code: " + lastResult);
		}
	}

	private void readCurrentDuplicate(ByteBuffer buffer) {
		ByteBuffer duplicate = buffer.duplicate();
		int offset = duplicate.position();
		currentObj = readLittleEndianLong(duplicate, offset);
		currentContext = readLittleEndianLong(duplicate, offset + Long.BYTES);
	}

	private void fillQuadFromPrefixAndValue(long v3, long v4) {
		quad[0] = prefixValues[0];
		quad[1] = prefixValues[1];
		quad[2] = v3;
		quad[3] = v4;
	}

	private boolean currentKeyHasPrefix() {
		return comparePrefix() == 0;
	}

	private boolean matchesQuad() {
		return (originalQuad[0] < 0 || quad[0] == originalQuad[0])
				&& (originalQuad[1] < 0 || quad[1] == originalQuad[1]);
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

	private RecordIterator createFallback() throws IOException {
		if (fallbackSupplier == null) {
			return null;
		}
		return fallbackSupplier.get();
	}

	private long readLittleEndianLong(ByteBuffer buffer, int offset) {
		long value = 0L;
		for (int i = 0; i < Long.BYTES; i++) {
			value |= (buffer.get(offset + i) & 0xFFL) << (i * 8);
		}
		return value;
	}
}
