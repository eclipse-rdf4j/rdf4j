/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.openDatabase;
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.readTransaction;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOMETASYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOSYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.MDB_RDONLY;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_del;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_mapsize;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxdbs;
import static org.lwjgl.util.lmdb.LMDB.mdb_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_put;
import static org.lwjgl.util.lmdb.LMDB.mdb_stat;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_commit;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBStat;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A cache for quads with an associated value. This cache uses a temporary file to store the records. This file is
 * deleted upon calling {@link #close()}.
 */
final class TxnRecordCache {

	private final Path dbDir;
	private final long env;
	private final int dbiExplicit;
	private final int dbiInferred;
	private long writeTxn;
	private long mapSize = 1048576; // 1 MiB
	private long pageSize;

	public TxnRecordCache(File cacheDir) throws IOException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			env = pp.get(0);

			E(mdb_env_set_maxdbs(env, 2));
			E(mdb_env_set_mapsize(env, mapSize));

			int flags = MDB_NOTLS | MDB_NOSYNC | MDB_NOMETASYNC;

			dbDir = Files.createTempDirectory(cacheDir.toPath(), "txncache");
			E(mdb_env_open(env, dbDir.toAbsolutePath().toString(), flags, 0664));
			dbiExplicit = openDatabase(env, "quads", MDB_CREATE, null);
			dbiInferred = openDatabase(env, "quads-inf", MDB_CREATE, null);

			MDBStat stat = MDBStat.malloc(stack);
			readTransaction(env, (stack2, txn) -> {
				E(mdb_stat(txn, dbiExplicit, stat));
				pageSize = stat.ms_psize();
				return null;
			});

			// directly start a write transaction
			E(mdb_txn_begin(env, NULL, 0, pp));
			writeTxn = pp.get(0);
		}
	}

	public void close() throws IOException {
		mdb_env_close(env);
		FileUtils.deleteDirectory(dbDir.toFile());
	}

	protected void commit() throws IOException {
		if (writeTxn != 0) {
			E(mdb_txn_commit(writeTxn));
			writeTxn = 0;
		}
	}

	protected boolean storeRecord(long[] quad, boolean explicit) throws IOException {
		return update(quad, explicit, true);
	}

	protected void removeRecord(long[] quad, boolean explicit) throws IOException {
		update(quad, explicit, false);
	}

	protected boolean update(long[] quad, boolean explicit, boolean add) throws IOException {
		if (LmdbUtil.requiresResize(mapSize, pageSize, writeTxn, 0)) {
			// resize map if required
			E(mdb_txn_commit(writeTxn));
			mapSize = LmdbUtil.autoGrowMapSize(mapSize, pageSize, 0);
			E(mdb_env_set_mapsize(env, mapSize));
			try (MemoryStack stack = stackPush()) {
				PointerBuffer pp = stack.mallocPointer(1);
				E(mdb_txn_begin(env, NULL, 0, pp));
				writeTxn = pp.get(0);
			}
		}
		try (MemoryStack stack = MemoryStack.stackPush()) {
			MDBVal keyVal = MDBVal.malloc(stack);
			// use calloc to get an empty data value
			MDBVal dataVal = MDBVal.calloc(stack);
			ByteBuffer keyBuf = stack.malloc(TripleStore.MAX_KEY_LENGTH);
			Varint.writeListUnsigned(keyBuf, quad);
			keyBuf.flip();
			keyVal.mv_data(keyBuf);

			boolean foundExplicit = mdb_get(writeTxn, dbiExplicit, keyVal, dataVal) == 0 &&
					(dataVal.mv_data().get(0) & 0b1) != 0;
			boolean foundImplicit = !foundExplicit && mdb_get(writeTxn, dbiInferred, keyVal, dataVal) == 0 &&
					(dataVal.mv_data().get(0) & 0b1) != 0;

			boolean found = foundExplicit || foundImplicit;
			if (add) {
				if (!found || explicit && foundImplicit) {
					if (explicit && foundImplicit) {
						E(mdb_del(writeTxn, dbiInferred, keyVal, dataVal));
					}
					// mark as add
					dataVal.mv_data(stack.bytes((byte) 1));
					E(mdb_put(writeTxn, explicit ? dbiExplicit : dbiInferred, keyVal, dataVal, 0));
				}
				return !found;
			} else {
				if (foundExplicit && explicit || foundImplicit && !explicit) {
					// simply delete quad from cache
					E(mdb_del(writeTxn, explicit ? dbiExplicit : dbiInferred, keyVal, dataVal));
				} else {
					// mark as remove
					dataVal.mv_data(stack.bytes((byte) 0));
					E(mdb_put(writeTxn, explicit ? dbiExplicit : dbiInferred, keyVal, dataVal, 0));
				}
				return true;
			}
		}
	}

	protected RecordCacheIterator getRecords(boolean explicit) throws IOException {
		return new RecordCacheIterator(explicit ? dbiExplicit : dbiInferred);
	}

	static class Record {
		long quad[];
		boolean add;
	}

	protected class RecordCacheIterator {
		private final MDBVal keyData = MDBVal.malloc();
		private final MDBVal valueData = MDBVal.malloc();
		private long txn;
		private final long cursor;
		private final int dbi;
		private final long[] quad = new long[4];

		protected RecordCacheIterator(int dbi) throws IOException {
			this.dbi = dbi;
			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer pp = stack.mallocPointer(1);

				E(mdb_txn_begin(env, NULL, MDB_RDONLY, pp));
				txn = pp.get(0);

				E(mdb_cursor_open(txn, dbi, pp));
				cursor = pp.get(0);
			}
		}

		public Record next() throws IOException {
			if (mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT) == 0) {
				Varint.readListUnsigned(keyData.mv_data(), quad);
				byte op = valueData.mv_data().get(0);
				Record r = new Record();
				r.quad = quad;
				r.add = op == 1;
				return r;
			}
			close();
			return null;
		}

		public void close() {
			if (txn != 0) {
				keyData.close();
				valueData.close();
				mdb_cursor_close(cursor);
				mdb_txn_abort(txn);
				txn = 0;
			}
		}
	}
}
