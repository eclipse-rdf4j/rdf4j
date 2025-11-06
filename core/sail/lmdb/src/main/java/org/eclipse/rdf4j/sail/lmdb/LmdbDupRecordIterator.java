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
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.DupIndex;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A dupsort/dupfixed-optimized record iterator using MDB_GET_MULTIPLE/NEXT_MULTIPLE to reduce JNI calls.
 */
class LmdbDupRecordIterator implements RecordIterator {

	private boolean explicit;
	private boolean exhausted;
	private boolean wasEmpty;

	@FunctionalInterface
	interface FallbackSupplier {
		RecordIterator get(long[] quadReuse, ByteBuffer minKeyBuf, ByteBuffer maxKeyBuf,
				LmdbRecordIterator iteratorReuse)
				throws IOException;
	}

	/** Toggle copying of duplicate blocks for extra safety (defaults to copying). */
	private static final boolean COPY_DUP_BLOCKS = Boolean.parseBoolean(
			System.getProperty("rdf4j.lmdb.copyDupBlocks", "true"));

	/** Size in bytes of one (v3,v4) tuple. */
	private static final int DUP_PAIR_BYTES = Long.BYTES * 2;

	private final Pool pool = Pool.get();
	private DupIndex index;
	private int dupDbi;
	private long cursor;

	private Txn txnRef;
	private long txn; // refreshed on txn version changes
	private long txnRefVersion;
	private StampedLongAdderLockManager txnLockManager;
	private final Thread ownerThread = Thread.currentThread();

	private MDBVal keyData;
	private MDBVal valueData;

	/** Reused output buffer required by the RecordIterator API. */
	private long[] quad;

	/** Scalars defining the prefix to scan (subject, predicate). */
	private long prefixSubj;
	private long prefixPred;

	private ByteBuffer prefixKeyBuf;

	/** Current duplicate block view and read indices. */
	private ByteBuffer dupBuf;
	private int dupPos;
	private int dupLimit;

	private int lastResult;
	private boolean closed = true;

	LmdbDupRecordIterator(DupIndex index, long subj, long pred,
			boolean explicit, Txn txnRef) throws IOException {
		initialize(index, subj, pred, explicit, txnRef, null);
	}

	LmdbDupRecordIterator(DupIndex index, long subj, long pred,
			boolean explicit, Txn txnRef, long[] quadReuse) throws IOException {
		initialize(index, subj, pred, explicit, txnRef, quadReuse);
	}

	void initialize(DupIndex index, long subj, long pred, boolean explicit, Txn txnRef, long[] quadReuse)
			throws IOException {

		this.exhausted = false;

		try {

			// TODO: Find out why this is slower than if we called close()
//				close();
			if (closed) {
				this.index = index;
				this.explicit = explicit;
				this.dupDbi = index.getDupDB(explicit);
				this.txnRef = txnRef;
				assert this.txnRef != null;
				this.txnLockManager = txnRef.lockManager();
				this.prefixSubj = subj;
				this.prefixPred = pred;
				assert this.keyData == null;
				assert this.valueData == null;
				assert this.prefixKeyBuf == null;
				this.keyData = pool.getVal();
				this.valueData = pool.getVal();
				this.prefixKeyBuf = pool.getKeyBuffer();

				prefixKeyBuf.clear();
				Varint.writeUnsigned(prefixKeyBuf, prefixSubj);
				Varint.writeUnsigned(prefixKeyBuf, prefixPred);
				prefixKeyBuf.flip();

				if (quadReuse != null && quadReuse.length >= 4) {
					this.quad = quadReuse;
				} else if (this.quad == null || this.quad.length < 4) {
					this.quad = new long[4];
				}

				this.quad[0] = subj;
				this.quad[1] = pred;
				this.quad[2] = -1L;
				this.quad[3] = -1L;

			} else {
				// TODO
				assert this.index == index;
				assert this.explicit == explicit;
				assert this.txnRef == txnRef;
				assert this.txnRef != null;
				assert this.txnLockManager != null;
				assert this.keyData != null;
				assert this.valueData != null;
				assert this.prefixKeyBuf != null;

				if (this.prefixSubj == subj && this.prefixPred == pred) {
					assert this.quad[0] == this.prefixSubj;
					assert this.quad[1] == this.prefixPred;

					if (wasEmpty) {
						exhausted = true;
						return;
					}

					// We can do a lot more reuse here!
					this.quad[2] = -1L;
					this.quad[3] = -1L;
				} else {
					this.prefixSubj = subj;
					this.prefixPred = pred;
					this.quad[0] = subj;
					this.quad[1] = pred;
					this.quad[2] = -1L;
					this.quad[3] = -1L;

					prefixKeyBuf.clear();
					Varint.writeUnsigned(prefixKeyBuf, prefixSubj);
					Varint.writeUnsigned(prefixKeyBuf, prefixPred);
					prefixKeyBuf.flip();
				}

			}

			this.dupBuf = null;
			this.dupPos = 0;
			this.dupLimit = 0;
			this.lastResult = MDB_SUCCESS;
			this.wasEmpty = false;

			if (closed) {
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

					boolean positioned = positionOnPrefix();
					if (positioned) {
						positioned = primeDuplicateBlock();
					}
					if (!positioned) {
						this.exhausted = true;
						this.wasEmpty = true;
						// closeInternal(false);
						// TODO: We must be empty????
					}
				} finally {
					txnLockManager.unlockRead(readStamp);
				}
			} else {
				long readStamp;
				try {
					readStamp = txnLockManager.readLock();
				} catch (InterruptedException e) {
					throw new SailException(e);
				}
				try {

					if (txnRefVersion != txnRef.version()) {
						this.txn = txnRef.get();
						E(mdb_cursor_renew(txn, cursor));
						txnRefVersion = txnRef.version();
					} else {
						E(mdb_cursor_renew(txn, cursor));
					}

					boolean positioned = positionOnPrefix();
					if (positioned) {
						positioned = primeDuplicateBlock();
					}
					if (!positioned) {
						this.exhausted = true;
						this.wasEmpty = true;
						// closeInternal(false);
						// TODO: We must be empty????
					}
				} finally {
					txnLockManager.unlockRead(readStamp);
				}
			}

		} finally {
			assert this.txnRef != null;
			this.closed = false;

		}

	}

	@Override
	public long[] next() {
		if (exhausted)
			return null;
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

			// Txn renewal if the TxnManager rotated its underlying LMDB txn
			if (txnRefVersion != txnRef.version()) {
				this.txn = txnRef.get();
				E(mdb_cursor_renew(txn, cursor));
				txnRefVersion = txnRef.version();
				if (!positionOnPrefix() || !primeDuplicateBlock()) {
//					closeInternal(false);
					exhausted = true;
					return null;
				}
			}

			while (true) {
				// Fast-path: emit from current duplicate block if at least one pair remains
				if (dupBuf != null && dupLimit - dupPos >= DUP_PAIR_BYTES) {
					long v3 = dupBuf.getLong(dupPos);
					long v4 = dupBuf.getLong(dupPos + Long.BYTES);
					dupPos += DUP_PAIR_BYTES;

					// s,p are constant; update only the tail
					quad[2] = v3;
					quad[3] = v4;
					return quad;
				}

				// Ask LMDB for the next duplicate block under the same key
				lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT_MULTIPLE);
				if (lastResult == MDB_SUCCESS) {
					resetDuplicateBuffer(valueData.mv_data());
					continue;
				}
				if (lastResult != MDB_NOTFOUND) {
					E(lastResult);
				}

				// No more duplicate blocks for this key; advance to next key in range
				do {
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
					if (lastResult != MDB_SUCCESS) {
//						closeInternal(false);
						exhausted = true;
						return null;
					}
					// Ensure we're still within the requested (subj,pred) prefix
					if (!currentKeyHasPrefix() && !adjustCursorToPrefix()) {
//						closeInternal(false);
						exhausted = true;
						return null;
					}
				} while (!primeDuplicateBlock()); // skip any keys without a duplicate block (defensive)
			}
		} catch (IOException e) {
			throw new SailException(e);
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	/* ---------- Positioning & Prefix handling ---------- */

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

	/**
	 * Compare current cursor key with (prefixSubj, prefixPred) without allocating a duplicate buffer.
	 */
	private int comparePrefix() {
		ByteBuffer key = keyData.mv_data();
		final int pos = key.position();
		try {
			long a0 = Varint.readUnsigned(key);
			int c0 = Long.compare(a0, prefixSubj);
			if (c0 != 0) {
				return c0;
			}
			long a1 = Varint.readUnsigned(key);
			return Long.compare(a1, prefixPred);
		} finally {
			key.position(pos); // restore buffer position
		}
	}

	private boolean currentKeyHasPrefix() {
		return comparePrefix() == 0;
	}

	/* ---------- Duplicate block priming ---------- */

	/**
	 * Prime the duplicate buffer for the current key using MDB_GET_MULTIPLE.
	 */
	private boolean primeDuplicateBlock() throws IOException {
		lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_GET_MULTIPLE);
		if (lastResult == MDB_SUCCESS) {
			resetDuplicateBuffer(valueData.mv_data());
			return dupBuf != null && dupLimit - dupPos >= DUP_PAIR_BYTES;
		}
		if (lastResult == MDB_NOTFOUND) {
			resetDuplicateBuffer(null);
			return false;
		}
		E(lastResult);
		return false; // unreachable
	}

	/**
	 * Prepare a readable view over the LMDB-provided value buffer. Default: zero-copy {@code slice()}. If
	 * COPY_DUP_BLOCKS is true, heap-copy for extra safety.
	 */
	private void resetDuplicateBuffer(ByteBuffer buffer) {
		if (buffer == null) {
			dupBuf = null;
			dupPos = dupLimit = 0;
			return;
		}

		if (!COPY_DUP_BLOCKS) {
			// Zero-copy: view over [position, limit) of the native buffer
			ByteBuffer view = buffer.slice();
			view.order(ByteOrder.BIG_ENDIAN);
			dupBuf = view;
			dupPos = view.position(); // 0
			dupLimit = view.limit();
		} else {
			// Conservative path: copy to Java heap to decouple lifetime from cursor operations
			ByteBuffer src = buffer.duplicate();
			src.position(buffer.position());
			src.limit(buffer.limit());
			ByteBuffer copy = ByteBuffer.allocate(src.remaining());
			copy.put(src);
			copy.flip();
			copy.order(ByteOrder.BIG_ENDIAN);
			dupBuf = copy;
			dupPos = dupBuf.position();
			dupLimit = dupBuf.limit();
		}
	}

	/* ---------- Lifecycle ---------- */

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
					if (cursor != 0L && txnRef != null) {
						if (txnRef.isReadOnly()) {
							pool.freeCursor(dupDbi, index, cursor);
						} else {
							mdb_cursor_close(cursor);
						}
					}
					cursor = 0L;
					if (keyData != null) {
						pool.free(keyData);
						keyData = null;
					}
					if (valueData != null) {
						pool.free(valueData);
						valueData = null;
					}
					if (prefixKeyBuf != null) {
						pool.free(prefixKeyBuf);
						prefixKeyBuf = null;
					}
				}
			} finally {
				closed = true;
				if (writeLocked) {
					txnLockManager.unlockWrite(writeStamp);
				}
				txnRef = null;
				dupBuf = null;
				dupPos = dupLimit = 0;
			}
		}
	}

	@Override
	public void close() {
		closeInternal(true);
		txnLockManager = null;
		txnRef = null;
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
