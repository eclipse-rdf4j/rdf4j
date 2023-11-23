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
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOMETASYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOOVERWRITE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOSYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.MDB_RDONLY;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;
import static org.lwjgl.util.lmdb.LMDB.mdb_del;
import static org.lwjgl.util.lmdb.LMDB.mdb_drop;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.StampedLock;

import javax.swing.text.ElementIterator;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Mode;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.TxnRecordCache.Record;
import org.eclipse.rdf4j.sail.lmdb.TxnRecordCache.RecordCacheIterator;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBStat;
import org.lwjgl.util.lmdb.MDBVal;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;

/**
 * A LMDB-based persistent set.
 */
class PersistentSet<T extends Serializable> extends AbstractSet<T> {

	private final Path dbDir;
	private final long env;
	private final int dbi;
	private TxnManager txnManager;
	private long writeTxn;
	private PointerBuffer writeTxnPp = PointerBuffer.allocateDirect(1);
	private long mapSize = 1048576; // 1 MiB
	private long pageSize;

	private int size;

	public PersistentSet(File cacheDir) throws IOException {
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
			dbi = openDatabase(env, "elements", MDB_CREATE, null);

			MDBStat stat = MDBStat.malloc(stack);
			readTransaction(env, (stack2, txn) -> {
				E(mdb_stat(txn, dbi, stat));
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

	protected synchronized void commit() throws IOException {
		if (writeTxn != 0) {
			E(mdb_txn_commit(writeTxn));
			writeTxn = 0;
		}
	}

	public synchronized void clear() {
		if (writeTxn != 0) {
			mdb_txn_abort(writeTxn);
			writeTxn = 0;
		}
		try {
			// start a write transaction
			E(mdb_txn_begin(env, NULL, 0, writeTxnPp));
			writeTxn = writeTxnPp.get(0);
			mdb_drop(writeTxn, dbi, false);
			commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		size = 0;
	}

	@Override
	public Iterator<T> iterator() {
		try {
			commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return new ElementIterator(dbi);
	}

	@Override
	public int size() {
		return size;
	}

	public boolean add(T element) {
		try {
			return update(element, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean remove(Object element) {
		try {
			return update(element, false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected synchronized boolean update(Object element, boolean add) throws IOException {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			if (writeTxn == 0) {
				// start a write transaction
				E(mdb_txn_begin(env, NULL, 0, writeTxnPp));
				writeTxn = writeTxnPp.get(0);
			}
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

			MDBVal keyVal = MDBVal.malloc(stack);
			// use calloc to get an empty data value
			MDBVal dataVal = MDBVal.calloc(stack);

			byte[] data = write((T) element);
			ByteBuffer keyBuf = stack.malloc(data.length);
			keyBuf.put(data);
			keyBuf.flip();
			keyVal.mv_data(keyBuf);

			if (add) {
				if (mdb_put(writeTxn, dbi, keyVal, dataVal, MDB_NOOVERWRITE) == MDB_SUCCESS) {
					size++;
					return true;
				}
			} else {
				// delete element
				if (mdb_del(writeTxn, dbi, keyVal, dataVal) == MDB_SUCCESS) {
					size--;
					return true;
				}
			}
			return false;
		}
	}

	protected byte[] write(T element) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(element);
		out.close();
		return baos.toByteArray();
	}

	protected T read(ByteBuffer buffer) throws IOException {
		try {
			return (T) new ObjectInputStream(new ByteBufferBackedInputStream(buffer)).readObject();
		} catch (ClassNotFoundException cnfe) {
			throw new IOException(cnfe);
		}
	}

	protected class ElementIterator implements Iterator<T> {

		private final MDBVal keyData = MDBVal.malloc();
		private final MDBVal valueData = MDBVal.malloc();
		private final long cursor;

		private final StampedLock txnLock;
		private Txn txnRef;
		private long txnRefVersion;

		private T next;
		private T current;

		protected ElementIterator(int dbi) {
			try {
				this.txnRef = txnManager.createReadTxn();
				this.txnLock = txnRef.lock();

				long stamp = txnLock.readLock();
				try {
					this.txnRefVersion = txnRef.version();

					try (MemoryStack stack = MemoryStack.stackPush()) {
						PointerBuffer pp = stack.mallocPointer(1);
						E(mdb_cursor_open(txnRef.get(), dbi, pp));
						cursor = pp.get(0);
					}
				} finally {
					txnLock.unlockRead(stamp);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean hasNext() {
			if (next == null && txnRef != null) {
				try {
					next = computeNext();
				} catch (Exception e) {
					next = null;
				}
				if (next == null) {
					close();
				}
			}
			return next != null;
		}

		@Override
		public T next() {
			if (next == null) {
				throw new NoSuchElementException();
			}
			current = next;
			next = null;
			return current;
		}

		public T computeNext() throws IOException {
			long stamp = txnLock.readLock();
			try {
				if (txnRefVersion != txnRef.version()) {
					// cursor must be renewed
					mdb_cursor_renew(txnRef.get(), cursor);

					try (MemoryStack stack = MemoryStack.stackPush()) {
						keyData.mv_data(stack.bytes(write(current)));
						if (mdb_cursor_get(cursor, keyData, valueData, MDB_SET) != 0) {
							// use MDB_SET_RANGE if key was deleted
							if (mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE) == 0) {
								return read(keyData.mv_data());
							}
						}
					}
				}

				if (mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT) == 0) {
					return read(keyData.mv_data());
				}
				close();
				return null;
			} finally {
				txnLock.unlockRead(stamp);
			}
		}

		public void close() {
			if (txnRef != null) {
				keyData.close();
				valueData.close();
				long stamp = txnLock.readLock();
				try {
					mdb_cursor_close(cursor);
					txnRef.close();
					txnRef = null;
				} finally {
					txnLock.unlockRead(stamp);
				}
			}
		}

		@Override
		public void remove() {
			PersistentSet.this.remove(current);
		}
	}

	public class ByteBufferBackedInputStream extends InputStream {

		final ByteBuffer buf;

		public ByteBufferBackedInputStream(ByteBuffer buf) {
			this.buf = buf;
		}

		public int read() throws IOException {
			if (!buf.hasRemaining()) {
				return -1;
			}
			return buf.get() & 0xFF;
		}

		public int read(byte[] bytes, int off, int len)
				throws IOException {
			if (!buf.hasRemaining()) {
				return -1;
			}

			len = Math.min(len, buf.remaining());
			buf.get(bytes, off, len);
			return len;
		}
	}
}
