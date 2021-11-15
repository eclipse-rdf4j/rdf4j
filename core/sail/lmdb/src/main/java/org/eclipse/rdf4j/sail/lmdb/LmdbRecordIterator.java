/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Comparator;

import org.eclipse.rdf4j.sail.lmdb.Varint.GroupMatcher;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A record iterator that wraps a native LMDB iterator.
 *
 * @author Ken Wenzel
 */
public class LmdbRecordIterator implements RecordIterator {

	private final MemoryStack stack = MemoryStack.create().push();

	private final long cursor;

	private final ByteBuffer maxKey;

	private final GroupMatcher groupMatcher;

	private final Comparator<ByteBuffer> cmp;

	private final TxnRef txnRef;

	private boolean closed = false;

	private MDBVal keyData = MDBVal.callocStack(stack), valueData = MDBVal.callocStack(stack);

	private int lastResult;

	private boolean fetchNext = false;

	public LmdbRecordIterator(boolean rangeSearch, long subj, long pred, long obj, long context,
			Comparator<ByteBuffer> cmp, int dbi, TxnRef txnRef) {
		byte[] minKey;
		if (rangeSearch) {
			minKey = TripleStore.getMinKey(subj, pred, obj, context);
			this.maxKey = ByteBuffer.wrap(TripleStore.getMaxKey(subj, pred, obj, context));
		} else {
			minKey = null;
			this.maxKey = null;
		}
		boolean matchValues = subj > 0 || pred > 0 || obj > 0 || context >= 0;
		if (matchValues) {
			ByteBuffer bb = ByteBuffer.allocate(TripleStore.MAX_KEY_LENGTH);
			TripleStore.toSearchKey(bb, subj, pred, obj, context);
			bb.flip();
			this.groupMatcher = new GroupMatcher(bb, subj > 0, pred > 0, obj > 0, context >= 0, false);
		} else {
			this.groupMatcher = null;
		}
		this.cmp = cmp;
		this.txnRef = txnRef;

		PointerBuffer pp = stack.mallocPointer(1);
		E(mdb_cursor_open(txnRef.get(), dbi, pp));
		cursor = pp.get(0);

		if (minKey != null) {
			// set cursor to min key
			keyData.mv_data(stack.bytes(minKey));
			lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
		} else {
			// set cursor to first item
			lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
		}
	}

	@Override
	public Record next() throws IOException {
		if (fetchNext) {
			lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
			fetchNext = false;
		}
		while (lastResult == 0) {
			if (maxKey != null && cmp.compare(keyData.mv_data(), maxKey) > 0) {
				lastResult = MDB_NOTFOUND;
			} else if (groupMatcher != null && !groupMatcher.matches(keyData.mv_data())) {
				// value doesn't match search key/mask, fetch next value
				lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
			} else {
				// Matching value found
				Record record = new Record(keyData.mv_data(), valueData.mv_data());
				record.key.order(ByteOrder.BIG_ENDIAN);
				record.val.order(ByteOrder.BIG_ENDIAN);

				// fetch next value
				fetchNext = true;
				return record;
			}
		}
		close();
		return null;
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			try {
				mdb_cursor_close(cursor);
				if (txnRef != null) {
					txnRef.end();
				}
				stack.close();
			} finally {
				closed = true;
			}
		}
	}
}
