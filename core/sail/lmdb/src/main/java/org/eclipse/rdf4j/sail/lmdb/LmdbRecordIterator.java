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

import static org.lwjgl.util.lmdb.LMDB.MDB_FIRST;
import static org.lwjgl.util.lmdb.LMDB.MDB_FIRST_DUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_GET_BOTH_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT_DUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT_NODUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cmp;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;
import static org.lwjgl.util.lmdb.LMDB.mdb_dcmp;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.TripleIndex;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.util.EntryMatcher;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A record iterator that wraps a native LMDB iterator.
 */
class LmdbRecordIterator implements RecordIterator {

	private static final Logger log = LoggerFactory.getLogger(LmdbRecordIterator.class);

	static class State {

		private TripleIndex index;

		private long cursor;

		private final MDBVal maxKey = MDBVal.malloc();
		private final MDBVal maxValue = MDBVal.malloc();

		private boolean matchValues;
		private EntryMatcher matcher;

		private Txn txnRef;

		private long txnRefVersion;

		private long txn;

		private int dbi;

		private final MDBVal keyData = MDBVal.malloc();

		private final MDBVal valueData = MDBVal.malloc();

		private ByteBuffer valueBuf;

		private final ByteBuffer minKeyBuf = MemoryUtil.memAlloc((Long.BYTES + 1) * 4);

		private final ByteBuffer minValueBuf = MemoryUtil.memAlloc((Long.BYTES + 1) * 4);

		private final ByteBuffer maxKeyBuf = MemoryUtil.memAlloc((Long.BYTES + 1) * 4);

		private final ByteBuffer maxValueBuf = MemoryUtil.memAlloc((Long.BYTES + 1) * 4);

		private long[] quad;
		private long[] patternQuad;

		private StampedLongAdderLockManager txnLockManager;

		private int indexScore;

		void close() {
			if (cursor != 0) {
				mdb_cursor_close(cursor);
				cursor = 0;
			}
			keyData.close();
			valueData.close();
			MemoryUtil.memFree(minKeyBuf);
			MemoryUtil.memFree(minValueBuf);
			MemoryUtil.memFree(maxKeyBuf);
			maxKey.close();
			MemoryUtil.memFree(maxValueBuf);
			maxValue.close();
		}
	}

	private final Thread ownerThread = Thread.currentThread();
	private final State state;
	private final boolean keyELementsFixed;
	private volatile boolean closed = false;
	private boolean fetchNext = false;

	LmdbRecordIterator(TripleIndex index, int indexScore, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef) throws IOException {
		this.state = Pool.get().getState();
		this.state.patternQuad = new long[] { subj, pred, obj, context };
		this.state.quad = new long[] { subj, pred, obj, context };
		this.state.index = index;
		this.state.indexScore = indexScore;
		this.keyELementsFixed = indexScore >= index.getIndexSplitPosition();

		// prepare min and max keys if index can be used
		// otherwise, leave as null to indicate full scan
		if (indexScore > 0) {
			state.minKeyBuf.clear();
			state.minValueBuf.clear();
			index.getMinEntry(state.minKeyBuf, state.minValueBuf, subj, pred, obj, context);
			state.minKeyBuf.flip();
			state.minValueBuf.flip();

			state.maxKeyBuf.clear();
			state.maxValueBuf.clear();
			index.getMaxEntry(state.maxKeyBuf, state.maxValueBuf, subj, pred, obj, context);
			state.maxKeyBuf.flip();
			state.maxValueBuf.flip();
			state.maxKey.mv_data(state.maxKeyBuf);
			state.maxValue.mv_data(state.maxValueBuf);
		}

		state.matchValues = subj > 0 || pred > 0 || obj > 0 || context >= 0;
		state.matcher = null;

		var dbi = index.getDB(explicit);

		long readStamp;
		try {
			readStamp = txnRef.lockManager().readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			state.dbi = dbi;
			state.txnRef = txnRef;
			state.txnLockManager = txnRef.lockManager();
			state.txnRefVersion = txnRef.version();
			state.txn = txnRef.get();
			state.cursor = txnRef.getCursor(dbi);
		} finally {
			txnRef.lockManager().unlockRead(readStamp);
		}
	}

	@Override
	public long[] next() {
		long readStamp;
		try {
			readStamp = state.txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		int lastResult;
		try {
			if (closed) {
				log.debug("Calling next() on an LmdbRecordIterator that is already closed, returning null");
				return null;
			}

			if (state.txnRefVersion != state.txnRef.version()) {
				// TODO: None of the tests in the LMDB Store cover this case!
				// cursor must be renewed
				mdb_cursor_renew(state.txn, state.cursor);
				if (fetchNext) {
					// cursor must be positioned on last item, reuse minKeyBuf if available
					state.minKeyBuf.clear();
					state.minValueBuf.clear();
					state.index.toEntry(state.minKeyBuf, state.minValueBuf, state.quad[0], state.quad[1], state.quad[2],
							state.quad[3]);
					state.minKeyBuf.flip();
					state.minValueBuf.flip();
					state.keyData.mv_data(state.minKeyBuf);
					// use set range if entry was deleted
					lastResult = mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_SET_RANGE);
					if (lastResult == MDB_SUCCESS) {
						state.valueData.mv_data(state.minValueBuf);
						lastResult = mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_GET_BOTH_RANGE);
						if (lastResult != MDB_SUCCESS) {
							lastResult = mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_FIRST_DUP);
						}
					}
					if (lastResult != MDB_SUCCESS) {
						closeInternal(false);
						return null;
					}
				}
				// update version of txn ref
				state.txnRefVersion = state.txnRef.version();
			}

			boolean isDupValue = false;
			if (fetchNext) {
				if (!state.valueBuf.hasRemaining()) {
					lastResult = mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_NEXT_DUP);
					if (lastResult != MDB_SUCCESS) {
						// no more duplicates, move to next key
						lastResult = mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_NEXT_NODUP);
					} else {
						isDupValue = true;
					}
					if (lastResult == MDB_SUCCESS) {
						state.valueBuf = state.valueData.mv_data();
					}
				} else {
					lastResult = MDB_SUCCESS;
				}
				fetchNext = false;
			} else {
				if (state.indexScore > 0) {
					// set cursor to min key
					state.keyData.mv_data(state.minKeyBuf);
					// set range on key is only required if less than the first two key elements are fixed
					lastResult = keyELementsFixed ? MDB_SUCCESS
							: mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_SET_RANGE);
					if (lastResult == MDB_SUCCESS) {
						state.valueData.mv_data(state.minValueBuf);
						lastResult = mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_GET_BOTH_RANGE);
						if (lastResult != MDB_SUCCESS) {
							lastResult = mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_FIRST_DUP);
						} else {
							isDupValue = keyELementsFixed;
						}
					}
				} else {
					// set cursor to first item
					lastResult = mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_FIRST);
				}
				if (lastResult == MDB_SUCCESS) {
					state.valueBuf = state.valueData.mv_data();
				}
			}

			while (lastResult == MDB_SUCCESS) {
				if (state.indexScore > 0) {
					int keyDiff = isDupValue ? 0 : mdb_cmp(state.txn, state.dbi, state.keyData, state.maxKey);
					if (keyDiff > 0) {
						break;
					}
					int valueDiff = TripleStore.compareRegion(state.valueBuf, state.valueBuf.position(),
							state.maxValueBuf, 0,
							Math.min(state.valueBuf.remaining(), state.maxValueBuf.remaining()));
					if (valueDiff > 0) {
						break;
					}
				}

				// value doesn't match search key/mask, fetch next value
				if (!state.valueBuf.hasRemaining()) {
					lastResult = mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_NEXT_DUP);
					if (lastResult != MDB_SUCCESS) {
						// no more duplicates, move to next key
						lastResult = mdb_cursor_get(state.cursor, state.keyData, state.valueData, MDB_NEXT_NODUP);
						isDupValue = false;
					}
					if (lastResult == MDB_SUCCESS) {
						state.valueBuf = state.valueData.mv_data();
					} else {
						break;
					}
				}

				int valueBufPos = state.valueBuf.position();
				if (notMatches(isDupValue, isDupValue ? null : state.keyData.mv_data(), state.valueBuf)) {
					state.valueBuf.position(valueBufPos);
					int skip = 4 - state.index.getIndexSplitPosition();
					for (int i = 0; i < skip; i++) {
						TripleStore.skipVarint(state.valueBuf);
					}
					continue;
				}
				state.valueBuf.position(valueBufPos);

				// Matching value found
				state.index.entryToQuad(state.keyData.mv_data(), state.valueBuf, state.patternQuad, state.quad);
				// fetch next value
				fetchNext = true;
				return state.quad;
			}
			closeInternal(false);
			return null;
		} finally {
			state.txnLockManager.unlockRead(readStamp);
		}
	}

	private boolean notMatches(boolean testValueOnly, ByteBuffer keyBuf, ByteBuffer valueBuf) {
		if (state.matcher != null) {
			return testValueOnly ? !state.matcher.matchesValue(valueBuf) : !state.matcher.matches(keyBuf, valueBuf);
		} else if (state.matchValues) {
			// lazy init of group matcher
			state.matcher = state.index.createMatcher(state.patternQuad[0], state.patternQuad[1], state.patternQuad[2],
					state.patternQuad[3]);
			return testValueOnly ? !state.matcher.matchesValue(valueBuf) : !state.matcher.matches(keyBuf, valueBuf);
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
					writeStamp = state.txnLockManager.writeLock();
					writeLocked = true;
				} catch (InterruptedException e) {
					throw new SailException(e);
				}
			}
			try {
				if (!closed) {
					state.txnRef.returnCursor(state.dbi, state.cursor);
					state.cursor = 0;
					Pool.get().free(state);
				}
			} finally {
				closed = true;
				if (writeLocked) {
					state.txnLockManager.unlockWrite(writeStamp);
				}
			}
		}
	}

	@Override
	public void close() {
		closeInternal(true);
	}
}
