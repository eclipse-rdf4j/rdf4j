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
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_RDONLY;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_set_compare;
import static org.lwjgl.util.lmdb.LMDB.mdb_strerror;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_commit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.util.lmdb.MDBCmpFuncI;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * Utility class for working with LMDB.
 */
final class LmdbUtil {

	/**
	 * Minimum free space in an LMDB db before automatically resizing the map.
	 */
	static final long MIN_FREE_SPACE = 524_288; // 512 KiB

	/**
	 * Minimum size an LMDB db is automatically grown.
	 */
	static final long MIN_AUTOGROW_SIZE = 1_048_576; // 1024 KiB

	private LmdbUtil() {
	}

	static void E(int rc) throws IOException {
		if (rc != MDB_SUCCESS && rc != MDB_NOTFOUND) {
			throw new IOException(mdb_strerror(rc));
		}
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

	static int openDatabase(long env, String name, int flags, Comparator<ByteBuffer> comparator) throws IOException {
		return transaction(env, (stack, txn) -> {
			IntBuffer ip = stack.mallocInt(1);

			E(mdb_dbi_open(txn, name, flags, ip));
			int dbi = ip.get(0);
			if (comparator != null) {
				MDBCmpFuncI cmp = (a, b) -> {
					MDBVal aVal = MDBVal.create(a);
					MDBVal bVal = MDBVal.create(b);
					return comparator.compare(aVal.mv_data(), bVal.mv_data());
				};
				mdb_set_compare(txn, dbi, cmp);
			}
			return dbi;
		});
	}

	/**
	 * Returns the next unallocated page for a given transaction handle.
	 *
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
		return MemoryUtil.memGetAddress(txn + 2 * Pointer.POINTER_SIZE);
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
		return mapSize - nextPageNo * pageSize < Math.max(requiredSize, LmdbUtil.MIN_FREE_SPACE);
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
		mapSize = Math.max(mapSize * 2, Math.max(requiredSize, MIN_AUTOGROW_SIZE));
		// align map size to page size
		return mapSize % pageSize == 0 ? mapSize : mapSize + (mapSize / pageSize + 1) * pageSize;
	}

	@FunctionalInterface
	interface Transaction<T> {

		T exec(MemoryStack stack, long txn) throws IOException;
	}

}
