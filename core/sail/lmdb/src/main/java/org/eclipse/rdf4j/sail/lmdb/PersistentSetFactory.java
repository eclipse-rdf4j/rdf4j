/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
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
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.openDatabase;
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.readTransaction;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOMETASYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOSYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_mapsize;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxdbs;
import static org.lwjgl.util.lmdb.LMDB.mdb_stat;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_commit;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Mode;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBStat;

/**
 * A factory for LMDB-based persistent sets.
 */
class PersistentSetFactory<T extends Serializable> {

	final long env;
	private final Path dbDir;
	TxnManager txnManager;
	long writeTxn;
	PointerBuffer writeTxnPp = PointerBuffer.allocateDirect(1);
	private int defaultDbi;
	private long mapSize = 1048576; // 1 MiB
	private long pageSize;

	PersistentSetFactory(File cacheDir) throws IOException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			env = pp.get(0);

			txnManager = new TxnManager(env, Mode.ABORT);

			E(mdb_env_set_maxdbs(env, 2));
			E(mdb_env_set_mapsize(env, mapSize));

			int flags = MDB_NOTLS | MDB_NOSYNC | MDB_NOMETASYNC;

			dbDir = Files.createTempDirectory(cacheDir.toPath(), "set");
			E(mdb_env_open(env, dbDir.toAbsolutePath().toString(), flags, 0664));
			this.defaultDbi = openDatabase(env, null, MDB_CREATE, null);

			MDBStat stat = MDBStat.malloc(stack);
			readTransaction(env, (stack2, txn) -> {
				E(mdb_stat(txn, this.defaultDbi, stat));
				pageSize = stat.ms_psize();
				return null;
			});
		}
	}

	public synchronized void close() throws IOException {
		if (writeTxn != 0) {
			mdb_txn_abort(writeTxn);
			writeTxn = 0;
		}

		// We don't need to free the pointer because it was allocated
		// by java.nio.ByteBuffer, which will handle freeing for us.
		// writeTxnPp.free();

		mdb_env_close(env);
		FileUtils.deleteDirectory(dbDir.toFile());
	}

	synchronized void commit() throws IOException {
		if (writeTxn != 0) {
			E(mdb_txn_commit(writeTxn));
			writeTxn = 0;
		}
	}

	void ensureResize() throws IOException {
		if (LmdbUtil.requiresResize(mapSize, pageSize, writeTxn, 0)) {
			StampedLock lock = txnManager.lock();
			long stamp = lock.writeLock();
			try {
				txnManager.deactivate();

				// resize map
				E(mdb_txn_commit(writeTxn));
				mapSize = LmdbUtil.autoGrowMapSize(mapSize, pageSize, 0);
				E(mdb_env_set_mapsize(env, mapSize));

				E(mdb_txn_begin(env, NULL, 0, writeTxnPp));
				writeTxn = writeTxnPp.get(0);
			} finally {
				try {
					txnManager.activate();
				} finally {
					lock.unlockWrite(stamp);
				}
			}
		}
	}

	PersistentSet<T> createSet(String name, Function<T, byte[]> writeFunc, Function<ByteBuffer, T> readFunc)
			throws IOException {
		int dbi = openDatabase(env, name, MDB_CREATE, null);
		return new PersistentSet<>(this, dbi) {
			@Override
			protected byte[] write(T element) throws IOException {
				try {
					return writeFunc.apply(element);
				} catch (UncheckedIOException ioe) {
					throw ioe.getCause();
				}
			}

			@Override
			protected T read(ByteBuffer buffer) throws IOException {
				try {
					return readFunc.apply(buffer);
				} catch (UncheckedIOException ioe) {
					throw ioe.getCause();
				}
			}
		};
	}
}
