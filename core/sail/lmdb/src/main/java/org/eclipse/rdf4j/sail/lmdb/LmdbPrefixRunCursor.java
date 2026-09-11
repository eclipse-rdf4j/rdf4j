/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * Cursor that emits one representative statement for each run of index keys sharing the same prefix.
 */
interface LmdbPrefixRunCursor extends AutoCloseable {

	LmdbPrefixRunCursor EMPTY = new LmdbPrefixRunCursor() {
		@Override
		public boolean next() {
			return false;
		}

		@Override
		public long[] quad() {
			return null;
		}

		@Override
		public long runRowCount() {
			return 0L;
		}

		@Override
		public long getSourceRowsScannedActual() {
			return 0L;
		}

		@Override
		public long getSourceRowsMatchedActual() {
			return 0L;
		}

		@Override
		public long getSourceRowsFilteredActual() {
			return 0L;
		}

		@Override
		public String getIndexName() {
			return "empty";
		}

		@Override
		public void close() {
		}
	};

	/**
	 * Advances to the next prefix run.
	 *
	 * @return {@code true} if a run was found, in which case {@link #quad()} holds its first matching statement
	 */
	boolean next() throws IOException;

	/**
	 * The representative statement of the current run as {@code [subj, pred, obj, context]} ids. Only the prefix fields
	 * are guaranteed to be representative of the run; the remaining fields belong to the first matching statement of
	 * the run. The array is reused between calls.
	 */
	long[] quad();

	/** Number of matching statements in the current run, or 1 when run rows are not counted. */
	long runRowCount();

	long getSourceRowsScannedActual();

	long getSourceRowsMatchedActual();

	long getSourceRowsFilteredActual();

	String getIndexName();

	@Override
	void close();
}

/**
 * Prefix-run scan over a single LMDB statement index. The cursor positions itself with {@code MDB_SET_RANGE} on the
 * smallest key that can match the pattern, emits the first matching statement of the run found there and then leaves
 * the run: short runs are stepped over with {@code MDB_NEXT}, while after {@link #SKIP_MIN_RUN} same-prefix rows the
 * cursor constructs the successor key of the run (the deepest unbound prefix field incremented, deeper fields reset to
 * their bound value or zero) and seeks straight to it. Long, dense runs therefore cost roughly one B-tree seek per
 * distinct prefix instead of a visit to every statement, while short runs never pay for a seek that would be slower
 * than simply stepping past them.
 * <p>
 * In counting mode ({@code countRunRows}) every row of a run is visited so that {@link #runRowCount()} reports the
 * number of matching statements per prefix.
 */
final class LmdbPrefixRunIterator implements LmdbPrefixRunCursor {

	/** In-run rows stepped over with {@code MDB_NEXT} before a successor seek engages. */
	static final int SKIP_MIN_RUN = 16;

	/** The largest id the varint key encoding can carry (see {@link Varint#writeUnsigned}). */
	private static final long MAX_FIELD_VALUE = Long.MAX_VALUE;

	private final LmdbPrefixRunPlan plan;
	private final TripleIndex index;
	private final Txn txnRef;
	private final StampedLongAdderLockManager txnLockManager;
	private final long txn;
	private final int dbi;
	private final boolean countRunRows;
	private final Pool pool;
	private final MDBVal keyData;
	private final MDBVal valueData;
	private final ByteBuffer seekKeyBuf;
	private final long[] quad = new long[4];
	private final long[] scratchQuad = new long[4];
	private final long[] targetQuad = new long[4];
	private final long[] boundValues = new long[4];
	private final Thread ownerThread = Thread.currentThread();

	private long cursor;
	private long txnRefVersion;
	private volatile boolean closed;
	/** Whether {@link #targetQuad} holds a key to seek to; false once no later run can exist. */
	private boolean hasSeekTarget;
	/** Whether the LMDB cursor rests on a fetched, not yet classified key (the first row after a stepped-over run). */
	private boolean positioned;
	private long runRowCount;
	private long sourceRowsScannedActual;
	private long sourceRowsMatchedActual;
	private long sourceRowsFilteredActual;
	private long emittedPrefixes;
	private long countedRunRows;

	LmdbPrefixRunIterator(LmdbPrefixRunPlan plan, Txn txnRef, long subj, long pred, long obj, long context,
			boolean explicit, boolean countRunRows) throws IOException {
		this.plan = plan;
		this.index = plan.index();
		this.txnRef = txnRef;
		this.txnLockManager = txnRef.lockManager();
		this.boundValues[TripleIndex.SUBJ_IDX] = subj > 0 ? subj : -1;
		this.boundValues[TripleIndex.PRED_IDX] = pred > 0 ? pred : -1;
		this.boundValues[TripleIndex.OBJ_IDX] = obj > 0 ? obj : -1;
		this.boundValues[TripleIndex.CONTEXT_IDX] = context >= 0 ? context : -1;
		this.countRunRows = countRunRows;
		this.pool = Pool.get();
		this.keyData = pool.getVal();
		this.valueData = pool.getVal();
		this.seekKeyBuf = pool.getKeyBuffer();
		this.dbi = index.getDB(explicit);
		prepareInitialSeekTarget();

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
			LmdbPrefixRunPlan.OPENED.incrementAndGet();
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	@Override
	public boolean next() throws IOException {
		if (closed) {
			return false;
		}

		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			if (closed) {
				return false;
			}
			renewCursorIfNeeded();
			int rc;
			if (positioned) {
				positioned = false;
				rc = MDB_SUCCESS;
			} else {
				if (!hasSeekTarget) {
					closeInternal(false);
					return false;
				}
				rc = seek();
			}
			while (rc == MDB_SUCCESS) {
				int matchStatus = classify(quad);
				if (matchStatus == TripleIndex.KEY_MATCH) {
					sourceRowsMatchedActual++;
					runRowCount = 1;
					prepareNextSeekTarget();
					if (countRunRows) {
						countCurrentRun();
					} else {
						advancePastCurrentRun();
					}
					emittedPrefixes++;
					if (countRunRows) {
						countedRunRows += runRowCount;
					}
					return true;
				}
				sourceRowsFilteredActual++;
				if (matchStatus == TripleIndex.KEY_OUT_OF_RANGE) {
					closeInternal(false);
					return false;
				}
				rc = step();
			}
			if (rc != MDB_NOTFOUND) {
				E(rc);
			}
			closeInternal(false);
			return false;
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	@Override
	public long[] quad() {
		return quad;
	}

	@Override
	public long runRowCount() {
		return runRowCount;
	}

	@Override
	public long getSourceRowsScannedActual() {
		return sourceRowsScannedActual;
	}

	@Override
	public long getSourceRowsMatchedActual() {
		return sourceRowsMatchedActual;
	}

	@Override
	public long getSourceRowsFilteredActual() {
		return sourceRowsFilteredActual;
	}

	@Override
	public String getIndexName() {
		return index.toString();
	}

	/** Positions the cursor on the first key at or after {@link #targetQuad}. */
	private int seek() {
		seekKeyBuf.clear();
		index.toKey(seekKeyBuf, targetQuad[TripleIndex.SUBJ_IDX], targetQuad[TripleIndex.PRED_IDX],
				targetQuad[TripleIndex.OBJ_IDX], targetQuad[TripleIndex.CONTEXT_IDX]);
		seekKeyBuf.flip();
		keyData.mv_data(seekKeyBuf);
		int rc = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
		if (rc == MDB_SUCCESS) {
			sourceRowsScannedActual++;
		}
		return rc;
	}

	/** Moves the cursor to the next key. */
	private int step() {
		int rc = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
		if (rc == MDB_SUCCESS) {
			sourceRowsScannedActual++;
		}
		return rc;
	}

	private int classify(long[] target) {
		return index.keyToQuadMatchStatus(keyData.mv_data(), plan.prefixLength(), boundValues[TripleIndex.SUBJ_IDX],
				boundValues[TripleIndex.PRED_IDX], boundValues[TripleIndex.OBJ_IDX],
				boundValues[TripleIndex.CONTEXT_IDX], target);
	}

	/**
	 * Steps over the current run. Leaves the cursor {@link #positioned} on the first key of a different prefix, or
	 * marks the scan exhausted at the end of the index. When the run is still going after {@link #SKIP_MIN_RUN} rows
	 * the method returns without positioning, so that the next call seeks to the run's successor key instead.
	 */
	private void advancePastCurrentRun() throws IOException {
		for (int stepped = 0; stepped < SKIP_MIN_RUN; stepped++) {
			int rc = step();
			if (rc == MDB_NOTFOUND) {
				hasSeekTarget = false;
				return;
			}
			if (rc != MDB_SUCCESS) {
				E(rc);
			}
			classify(scratchQuad);
			if (!samePrefix(scratchQuad, quad)) {
				positioned = true;
				return;
			}
		}
	}

	/**
	 * Walks the current run row by row, counting matching statements. Leaves the cursor {@link #positioned} on the
	 * first key of a different prefix, or marks the scan exhausted at the end of the index.
	 */
	private void countCurrentRun() throws IOException {
		while (true) {
			int rc = step();
			if (rc == MDB_NOTFOUND) {
				hasSeekTarget = false;
				return;
			}
			if (rc != MDB_SUCCESS) {
				E(rc);
			}
			int matchStatus = classify(scratchQuad);
			if (!samePrefix(scratchQuad, quad)) {
				positioned = true;
				return;
			}
			if (matchStatus != TripleIndex.KEY_MATCH) {
				sourceRowsFilteredActual++;
				continue;
			}
			sourceRowsMatchedActual++;
			runRowCount++;
		}
	}

	private boolean samePrefix(long[] left, long[] right) {
		char[] fieldSeq = index.getFieldSeq();
		for (int i = 0; i < plan.prefixLength(); i++) {
			int field = fieldIndex(fieldSeq[i]);
			if (left[field] != right[field]) {
				return false;
			}
		}
		return true;
	}

	private void prepareInitialSeekTarget() {
		for (int field = 0; field < 4; field++) {
			targetQuad[field] = boundValues[field] == -1 ? 0 : boundValues[field];
		}
		hasSeekTarget = true;
	}

	private void prepareNextSeekTarget() {
		hasSeekTarget = successorKey(index.getFieldSeq(), plan.prefixLength(), quad, boundValues, targetQuad);
	}

	/**
	 * Builds the smallest key strictly after every key sharing the run prefix of {@code quad}: the deepest unbound
	 * prefix field that is not yet at the maximum id is incremented, all deeper fields are reset to their bound value
	 * or zero, and shallower fields are kept.
	 *
	 * @param fieldSeq     the index key order
	 * @param prefixLength number of leading key fields forming the run prefix
	 * @param quad         the current run's representative statement (statement field order)
	 * @param boundValues  the bound id per statement field, or -1 when unbound
	 * @param target       receives the successor key (statement field order)
	 * @return {@code false} when every unbound prefix field is at its maximum, i.e. no later run can exist
	 */
	static boolean successorKey(char[] fieldSeq, int prefixLength, long[] quad, long[] boundValues, long[] target) {
		int carry = -1;
		for (int i = prefixLength - 1; i >= 0; i--) {
			int field = fieldIndex(fieldSeq[i]);
			if (boundValues[field] == -1 && quad[field] != MAX_FIELD_VALUE) {
				carry = i;
				break;
			}
		}
		if (carry < 0) {
			return false;
		}
		for (int i = 0; i < fieldSeq.length; i++) {
			int field = fieldIndex(fieldSeq[i]);
			long value;
			if (i < carry) {
				value = quad[field];
			} else if (i == carry) {
				value = quad[field] + 1;
			} else {
				long bound = boundValues[field];
				value = bound == -1 ? 0 : bound;
			}
			target[field] = value;
		}
		return true;
	}

	static int fieldIndex(char field) {
		return switch (field) {
		case 's' -> TripleIndex.SUBJ_IDX;
		case 'p' -> TripleIndex.PRED_IDX;
		case 'o' -> TripleIndex.OBJ_IDX;
		case 'c' -> TripleIndex.CONTEXT_IDX;
		default -> throw new IllegalArgumentException("Invalid statement field: " + field);
		};
	}

	private void renewCursorIfNeeded() throws IOException {
		if (txnRefVersion != txnRef.version()) {
			// the read transaction was renewed: the cursor loses its position and must seek again
			E(mdb_cursor_renew(txn, cursor));
			txnRefVersion = txnRef.version();
			positioned = false;
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
					if (cursor != 0) {
						mdb_cursor_close(cursor);
						cursor = 0;
					}
					pool.free(keyData);
					pool.free(valueData);
					pool.free(seekKeyBuf);
					LmdbPrefixRunPlan.ROWS_SCANNED.addAndGet(sourceRowsScannedActual);
					LmdbPrefixRunPlan.PREFIXES_EMITTED.addAndGet(emittedPrefixes);
					LmdbPrefixRunPlan.RUN_ROWS_COUNTED.addAndGet(countedRunRows);
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
