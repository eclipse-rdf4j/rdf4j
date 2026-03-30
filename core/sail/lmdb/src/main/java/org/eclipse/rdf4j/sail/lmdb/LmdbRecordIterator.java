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
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.TripleIndex;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.util.GroupMatcher;
import org.lwjgl.PointerBuffer;
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

	private final boolean matchValues;
	private GroupMatcher groupMatcher;
	private final TripleStore.KeyReader keyReader;

	private final Txn txnRef;

	private long txnRefVersion;

	private final long txn;

	private final int dbi;

	private volatile boolean closed = false;

	private final MDBVal keyData;

	private final MDBVal valueData;

	private ByteBuffer minKeyBuf;

	private ByteBuffer maxKeyBuf;

	private final long[] quad;
	private final long[] originalQuad;

	private boolean fetchNext = false;

	private final StampedLongAdderLockManager txnLockManager;

	private final Thread ownerThread = Thread.currentThread();

	// Lock batching optimization: hold lock for multiple rows to reduce contention
	// Larger batch sizes reduce lock acquisition overhead but may increase contention
	// Testing showed 512 provides good balance between throughput and latency
	private static final int LOCK_BATCH_SIZE = 512;
	private int batchCounter = 0;
	private long heldReadStamp = 0L;
	private boolean lockHeld = false;

	// Optimistic lock-free reads: when no writer is active, skip lock acquisition entirely.
	// The AtomicInteger signals to closeInternal that a reader is actively using the cursor.
	private final AtomicInteger optimisticReaderActive = new AtomicInteger(0);
	private boolean optimisticBatch = false;

	LmdbRecordIterator(TripleIndex index, boolean rangeSearch, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef) throws IOException {
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.context = context;
		this.pool = Pool.get();
		// Use pooled arrays to reduce allocation in hot path
		this.originalQuad = pool.getQuadArray();
		this.originalQuad[0] = subj;
		this.originalQuad[1] = pred;
		this.originalQuad[2] = obj;
		this.originalQuad[3] = context;
		this.quad = pool.getQuadArray();
		this.quad[0] = subj;
		this.quad[1] = pred;
		this.quad[2] = obj;
		this.quad[3] = context;
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

		this.matchValues = subj > 0 || pred > 0 || obj > 0 || context >= 0;
		// Pre-create GroupMatcher to avoid lazy initialization latency on first match
		if (this.matchValues) {
			this.groupMatcher = index.createMatcher(subj, pred, obj, context);
		}
		// Pre-create optimized KeyReader to eliminate array lookups in hot path
		this.keyReader = index.createKeyReader(this.originalQuad);

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

			// Use pooled PointerBuffer to avoid allocation
			PointerBuffer pp = pool.getPointerBuffer();
			E(mdb_cursor_open(txn, dbi, pp));
			cursor = pp.get(0);
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	@Override
	public long[] next() {
		// Acquire lock only when starting a new batch
		if (!lockHeld && !optimisticBatch) {
			if (!txnLockManager.isWriterActive()) {
				// Optimistic path: no writer active, skip lock acquisition
				optimisticReaderActive.incrementAndGet();
				optimisticBatch = true;
				batchCounter = 0;
				// Double-check: if closed or writer appeared after our signal, fall back to lock
				if (closed || txnLockManager.isWriterActive()) {
					optimisticReaderActive.decrementAndGet();
					optimisticBatch = false;
					// Fall through to locked path
					try {
						heldReadStamp = txnLockManager.readLock();
						lockHeld = true;
						batchCounter = 0;
					} catch (InterruptedException e) {
						throw new SailException(e);
					}
				}
			} else {
				try {
					heldReadStamp = txnLockManager.readLock();
					lockHeld = true;
					batchCounter = 0;
				} catch (InterruptedException e) {
					throw new SailException(e);
				}
			}
		}

		try {
			if (closed) {
				log.debug("Calling next() on an LmdbRecordIterator that is already closed, returning null");
				releaseAnyHeldLock();
				return null;
			}

			int lastResult;
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
					// value doesn't match search key/mask, fetch next value
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				} else {
					// Matching value found - use pre-computed KeyReader for optimal performance
					keyReader.read(keyData.mv_data(), quad);
					// fetch next value
					fetchNext = true;

					// Increment batch counter and release lock/signal if batch is complete
					batchCounter++;
					if (batchCounter >= LOCK_BATCH_SIZE) {
						releaseAnyHeldLock();
					}

					return quad;
				}
			}
			// Iterator exhausted, release lock
			closeInternal(false);
			return null;
		} catch (Exception e) {
			// On error, ensure lock is released
			releaseAnyHeldLock();
			throw e;
		}
	}

	/**
	 * Release the held read lock if currently held.
	 */
	private void releaseLockIfHeld() {
		if (lockHeld) {
			txnLockManager.unlockRead(heldReadStamp);
			lockHeld = false;
			batchCounter = 0;
		}
	}

	/**
	 * Release any held lock or optimistic batch signal.
	 */
	private void releaseAnyHeldLock() {
		if (lockHeld) {
			txnLockManager.unlockRead(heldReadStamp);
			lockHeld = false;
			batchCounter = 0;
		} else if (optimisticBatch) {
			optimisticReaderActive.decrementAndGet();
			optimisticBatch = false;
			batchCounter = 0;
		}
	}

	private boolean matches() {
		if (groupMatcher != null) {
			return !this.groupMatcher.matches(keyData.mv_data());
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
				// Wait for any optimistic reader to finish before closing the cursor
				while (optimisticReaderActive.get() > 0) {
					Thread.onSpinWait();
				}
			}
			try {
				if (!closed) {
					// Release any held lock or optimistic signal before closing
					releaseAnyHeldLock();

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
					// Return pooled quad arrays
					pool.free(quad);
					pool.free(originalQuad);
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
