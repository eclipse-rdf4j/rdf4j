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
import java.util.Comparator;

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

	private final Comparator<ByteBuffer> cmp;

	private final byte[] searchKey;

	private final byte[] searchMask;

	private final TxnRef txnRef;

	private boolean closed = false;

	private MDBVal keyData = MDBVal.callocStack(stack), valueData = MDBVal.callocStack(stack);

	private int lastResult;

	public LmdbRecordIterator(int dbi, TxnRef txnRef) {
		this(null, null, null, null, null, dbi, txnRef);
	}

	public LmdbRecordIterator(byte[] minKey, byte[] maxKey, Comparator<ByteBuffer> cmp,
			byte[] searchKey, byte[] searchMask, int dbi, TxnRef txnRef) {
		this.maxKey = maxKey == null ? null : ByteBuffer.wrap(maxKey);
		this.cmp = cmp;
		this.searchKey = searchKey;
		this.searchMask = searchMask;
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
	public byte[] next() throws IOException {
		while (lastResult == 0) {
			if (maxKey != null && cmp.compare(keyData.mv_data(), maxKey) > 0) {
				lastResult = MDB_NOTFOUND;
			} else if (searchKey != null && !matchesPattern(keyData.mv_data(), searchMask, searchKey)) {
				// value doesn't match search key/mask, fetch next value
				lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
			} else {
				// Matching value found
				byte[] bytes = new byte[keyData.mv_data().remaining()];
				keyData.mv_data().get(bytes);
				// fetch next value
				lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				return bytes;
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

	boolean matchesPattern(ByteBuffer value, byte[] mask, byte[] pattern) {
		for (int i = 0; i < value.limit(); i++) {
			if (((value.get(i) ^ pattern[i]) & mask[i]) != 0) {
				return false;
			}
		}

		return true;
	}
}
