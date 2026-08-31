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
/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.eclipse.rdf4j.sail.lmdb;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_APPENDDUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_GET_BOTH_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_KEYEXIST;
import static org.lwjgl.util.lmdb.LMDB.MDB_LAST_DUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOOVERWRITE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_RDONLY;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_del;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_put;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_strerror;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_commit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.util.lmdb.MDBVal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for working with LMDB.
 */
final class LmdbUtil {

	private static final Logger logger = LoggerFactory.getLogger(LmdbUtil.class);

	/**
	 * Minimum free space in an LMDB db before automatically resizing the map.
	 */
	static final long MIN_FREE_SPACE = 524_288; // 512 KiB

	/**
	 * Percentage free space in an LMDB db before automatically resizing the map. Default is 80%.
	 */
	@SuppressWarnings("StaticNonFinalField")
	public static int PERCENTAGE_FULL_TRIGGERS_RESIZE = 80;

	private LmdbUtil() {
	}

	static int E(int rc) throws IOException {
		if (rc != MDB_SUCCESS && rc != MDB_NOTFOUND && rc != MDB_KEYEXIST) {
			IOException ioException = new IOException(mdb_strerror(rc));
			logger.info("Possible LMDB error: {}", mdb_strerror(rc), ioException);
			throw ioException;
		}
		return rc;
	}

	static <T> T readTransaction(long env, Transaction<T> transaction) throws IOException {
		return readTransaction(env, 0L, transaction);
	}

	static <T> T readTransaction(long env, long writeTxn, Transaction<T> transaction) throws IOException {
		T ret;
		try (MemoryStack stack = stackPush()) {
			long txn;
			if (writeTxn == 0) {
				PointerBuffer pp = stack.mallocPointer(1);
				E(mdb_txn_begin(env, NULL, MDB_RDONLY, pp));
				txn = pp.get(0);
			} else {
				txn = writeTxn;
			}

			try {
				ret = transaction.exec(stack, txn);
			} finally {
				if (writeTxn == 0) {
					mdb_txn_abort(txn);
				}
			}
		}

		return ret;
	}

	static <T> T transaction(long env, Transaction<T> transaction) throws IOException {
		T ret;
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);

			E(mdb_txn_begin(env, NULL, 0, pp));
			long txn = pp.get(0);

			int err;
			try {
				ret = transaction.exec(stack, txn);
				err = mdb_txn_commit(txn);
			} catch (Throwable t) {
				mdb_txn_abort(txn);
				throw t;
			}
			E(err);
		}

		return ret;
	}

	static int openDatabase(long env, String name, int flags) throws IOException {
		return transaction(env, (stack, txn) -> openDatabaseWithTxn(txn, name, flags));
	}

	static int openDatabaseWithTxn(long txn, String name, int flags) throws IOException {
		try (MemoryStack stack = stackPush()) {
			IntBuffer ip = stack.mallocInt(1);
			E(mdb_dbi_open(txn, name, flags, ip));
			return ip.get(0);
		}
	}

	/**
	 * Returns the next unallocated page for a given transaction handle.
	 * <p>
	 * The function expects the following layout of the transaction struct:
	 *
	 * <pre>
	 * <code>
	 * struct MDB_txn {
	 *     size_t mt_parent;
	 *     size_t mt_child;
	 *     size_t mt_next_pgno;
	 *     ...
	 * }</code>
	 * </pre>
	 */
	private static long mdbTxnMtNextPgno(long txn) {
		return MemoryUtil.memGetAddress(txn + 2L * Pointer.POINTER_SIZE);
	}

	/**
	 * Determines if a resize of the current map size for an LMDB db is required.
	 *
	 * @param mapSize      the current map size
	 * @param pageSize     the page size
	 * @param txn          a transaction handle
	 * @param requiredSize the minimum required size
	 * @return <code>true</code> if map should be resized, else <code>false</code>
	 */
	static boolean requiresResize(long mapSize, long pageSize, long txn, long requiredSize) {
		long nextPageNo = mdbTxnMtNextPgno(txn);
		double percentageUsed = (100.0 / mapSize) * (nextPageNo * pageSize);
		if (percentageUsed > PERCENTAGE_FULL_TRIGGERS_RESIZE) {
			return true;
		}
		return mapSize - nextPageNo * pageSize < Math.max(requiredSize, MIN_FREE_SPACE);
	}

	/**
	 * Computes a new map size for auto-growing an existing LMDB db.
	 *
	 * @param mapSize      the current map size
	 * @param pageSize     the page size
	 * @param requiredSize the minimum required size
	 * @return the new map size
	 */
	static long autoGrowMapSize(long mapSize, long pageSize, long requiredSize) {
		mapSize = Math.max(mapSize * 2, Math.max(requiredSize, MIN_FREE_SPACE));
		// align map size to page size
		return mapSize % pageSize == 0 ? mapSize : mapSize + (mapSize / pageSize + 1) * pageSize;
	}

	public static long getNewSize(int pageSize, long txn, long requiredSize) {
		long nextPgno = mdbTxnMtNextPgno(txn);
		return (nextPgno * pageSize) + requiredSize;
	}

	static int merge(long cursor, int elements, MDBVal keyVal, MDBVal dataVal,
			ByteBuffer newValueBuf, ByteBuffer target) throws IOException {
		dataVal.mv_data(newValueBuf);
		int rc = E(mdb_cursor_put(cursor, keyVal, dataVal, MDB_NOOVERWRITE));
		if (rc == MDB_SUCCESS) {
			return MDB_SUCCESS;
		}
		rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_GET_BOTH_RANGE));
		int newValuesSize = 0;
		if (rc == MDB_SUCCESS) {
			ByteBuffer existing = dataVal.mv_data();

			newValuesSize = newValueBuf.remaining();
			int existingValuesSize = existing.remaining();
			int diff = -1;
			while (existing.hasRemaining() &&
					(diff = compareRegion(newValueBuf, 0, existing, existing.position(),
							Math.min(newValuesSize, existing.remaining()))) > 0) {
				for (int i = 0; i < elements; i++) {
					skipVarint(existing);
				}
			}
			if (diff == 0) {
				return MDB_KEYEXIST;
			}
			int insertPos = existing.position();
			if (insertPos > 0) {
				target.clear();
				target.put(existing.duplicate().flip());
				target.put(newValueBuf);
				if (existing.hasRemaining()) {
					target.put(existing);
				}
				E(mdb_cursor_del(cursor, 0));
				int totalSize = target.position();
				target.flip();
				if (insertPos + newValuesSize > 500) {
					target.limit(insertPos);
					dataVal.mv_data(target);
					E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
					target.position(insertPos);
					target.limit(totalSize);
				}
				if (target.remaining() > 500) {
					target.limit(insertPos + newValuesSize);
					dataVal.mv_data(target);
					E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
					target.position(insertPos + newValuesSize);
					target.limit(totalSize);
				}
				if (target.hasRemaining()) {
					dataVal.mv_data(target);
					E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
				}
			} else {
				if (existingValuesSize + newValuesSize <= 500) {
					target.clear();
					target.put(newValueBuf);
					target.put(existing);
					target.flip();
					dataVal.mv_data(target);
					E(mdb_cursor_del(cursor, 0));
					E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
				} else {
					dataVal.mv_data(newValueBuf);
					E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
				}
			}
			return MDB_SUCCESS;
		} else {
			if (E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET)) == MDB_SUCCESS) {
				if (E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_LAST_DUP)) == MDB_SUCCESS) {
					ByteBuffer existing = dataVal.mv_data();
					if (existing.remaining() + newValuesSize <= 500) {
						target.clear();
						target.put(existing);
						target.put(newValueBuf);
						target.flip();
						E(mdb_cursor_del(cursor, 0));
						dataVal.mv_data(target);
						E(mdb_cursor_put(cursor, keyVal, dataVal, MDB_APPENDDUP));
						return MDB_SUCCESS;
					}
				}
			}
		}
		dataVal.mv_data(newValueBuf);
		E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
		return MDB_SUCCESS;
	}

	static boolean deleteFromMergedValue(long cursor, int elements, MDBVal keyVal, MDBVal dataVal,
			ByteBuffer valueToDelete, ByteBuffer target) throws IOException {
		int rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_GET_BOTH_RANGE));
		if (rc != MDB_SUCCESS) {
			return false;
		}

		ByteBuffer existing = dataVal.mv_data();
		int valueToDeleteSize = valueToDelete.remaining();
		if (valueToDeleteSize == 0 || existing.remaining() < valueToDeleteSize) {
			return false;
		}

		while (existing.hasRemaining()) {
			int entryStart = existing.position();
			int compareLength = Math.min(valueToDeleteSize, existing.remaining());
			int diff = compareRegion(valueToDelete, 0, existing, entryStart, compareLength);
			if (diff < 0) {
				return false;
			}
			if (diff == 0 && existing.remaining() >= valueToDeleteSize) {
				int originalLimit = existing.limit();
				int entryEnd = entryStart + valueToDeleteSize;
				target.clear();
				if (entryStart > 0) {
					existing.limit(entryStart);
					target.put(existing.duplicate().flip());
				}
				if (entryEnd < originalLimit) {
					existing.limit(originalLimit);
					existing.position(entryEnd);
					target.put(existing);
				}
				E(mdb_cursor_del(cursor, 0));
				if (target.position() > 0) {
					target.flip();
					dataVal.mv_data(target);
					E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
				}
				return true;
			}
			existing.position(entryStart);
			for (int i = 0; i < elements; i++) {
				skipVarint(existing);
			}
		}
		return false;
	}

	static int compareRegion(ByteBuffer bb1, int startIdx1, ByteBuffer bb2, int startIdx2, int length) {
		int result = 0;
		for (int i = 0; result == 0 && i < length; i++) {
			result = (bb1.get(startIdx1 + i) & 0xff) - (bb2.get(startIdx2 + i) & 0xff);
		}
		return result;
	}

	static void skipVarint(ByteBuffer other) {
		int i = Varint.firstToLength(other.get()) - 1;
		assert i >= 0;
		if (i > 0) {
			other.position(i + other.position());
		}
	}

	@FunctionalInterface
	interface Transaction<T> {

		T exec(MemoryStack stack, long txn) throws IOException;
	}

}
