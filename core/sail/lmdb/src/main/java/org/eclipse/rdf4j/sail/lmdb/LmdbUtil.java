/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.eclipse.rdf4j.sail.lmdb;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
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
import org.lwjgl.util.lmdb.MDBCmpFuncI;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * Utility class for working with LMDB.
 */
final class LmdbUtil {

	private LmdbUtil() {
	}

	static void E(int rc) {
		if (rc != MDB_SUCCESS) {
			throw new IllegalStateException(mdb_strerror(rc));
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

	@FunctionalInterface
	interface Transaction<T> {

		T exec(MemoryStack stack, long txn) throws IOException;
	}

}