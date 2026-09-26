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

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
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

	private volatile boolean closed = false;

	private final MDBVal keyData;

	private final MDBVal valueData;

	private ByteBuffer minKeyBuf;

	private final long[] record = new long[1];

	private boolean fetchNext = false;

	private final StampedLongAdderLockManager txnLockManager;

	private final Thread ownerThread = Thread.currentThread();

	LmdbContextIdIterator(int dbi, Txn txnRef) throws IOException {
		this.pool = txnRef.getValuePool();
		this.keyData = pool.getVal();
		this.valueData = pool.getVal();

		this.txnRef = txnRef;
		this.txnLockManager = txnRef.lockManager();

		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			freeBuffers();
			throw new SailException("Interrupted while opening a context cursor", e);
		}
		long openedCursor = 0;
		try {
			synchronized (txnRef) {
				txnRef.ensureSnapshotValid();
				this.txnRefVersion = txnRef.version();
				this.txn = txnRef.get();

				try (MemoryStack stack = MemoryStack.stackPush()) {
					PointerBuffer pp = stack.mallocPointer(1);
					E(mdb_cursor_open(txn, dbi, pp));
					cursor = pp.get(0);
					openedCursor = cursor;
				}
			}
		} catch (IOException | RuntimeException | Error e) {
			if (openedCursor != 0) {
				mdb_cursor_close(openedCursor);
			}
			freeBuffers();
			throw e;
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	public long[] next() {
		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SailException("Interrupted while reading a context cursor", e);
		}
		try {
			synchronized (txnRef) {
				if (closed) {
					return null;
				}
				txnRef.ensureSnapshotValid();

				int lastResult;
				if (txnRefVersion != txnRef.version()) {
					// A pinned cursor must fail before renewal onto the transaction's newer map generation.
					txnRef.ensureSnapshotValid();
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
							closeInternalUnderCurrentReadLock();
							return null;
						}
					}
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
				closeInternalUnderCurrentReadLock();
				return null;
			}
		} catch (SailException e) {
			closeInternalUnderCurrentReadLock();
			throw e;
		} catch (IOException e) {
			closeInternalUnderCurrentReadLock();
			throw new SailException(e.getMessage(), e);
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	private void closeInternal(boolean maybeCalledAsync) {
		closeInternal(maybeCalledAsync, false);
	}

	private void closeInternalUnderCurrentReadLock() {
		closeInternal(false, true);
	}

	private void closeInternal(boolean maybeCalledAsync, boolean currentThreadHasReadLock) {
		if (!closed) {
			long lockStamp = 0L;
			boolean locked = false;
			boolean writeLocked = maybeCalledAsync && ownerThread != Thread.currentThread();
			if (!currentThreadHasReadLock) {
				try {
					lockStamp = writeLocked ? txnLockManager.writeLock() : txnLockManager.readLock();
					locked = true;
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new SailException(e);
				}
			}
			try {
				synchronized (txnRef) {
					if (!closed) {
						try {
							mdb_cursor_close(cursor);
						} finally {
							freeBuffers();
							closed = true;
						}
					}
				}
			} finally {
				if (locked) {
					if (writeLocked) {
						txnLockManager.unlockWrite(lockStamp);
					} else {
						txnLockManager.unlockRead(lockStamp);
					}
				}
			}
		}
	}

	private void freeBuffers() {
		pool.free(keyData);
		pool.free(valueData);
		if (minKeyBuf != null) {
			pool.free(minKeyBuf);
		}
	}

	@Override
	public void close() {
		closeInternal(true);
	}

}
