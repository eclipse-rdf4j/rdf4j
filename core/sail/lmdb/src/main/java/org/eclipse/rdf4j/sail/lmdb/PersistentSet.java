/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
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
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_MAP_FULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOOVERWRITE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;
import static org.lwjgl.util.lmdb.LMDB.mdb_del;
import static org.lwjgl.util.lmdb.LMDB.mdb_drop;
import static org.lwjgl.util.lmdb.LMDB.mdb_put;
import static org.lwjgl.util.lmdb.LMDB.mdb_strerror;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.StampedLock;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A LMDB-based persistent set.
 */
class PersistentSet<T extends Serializable> extends AbstractSet<T> {

	private static final Logger logger = LoggerFactory.getLogger(PersistentSet.class);

	private PersistentSetFactory<T> factory;
	private final int dbi;
	private int size;

	public PersistentSet(PersistentSetFactory<T> factory, int dbi) {
		this.factory = factory;
		this.dbi = dbi;
	}

	public synchronized void clear() {
		if (factory.writeTxn != 0) {
			mdb_txn_abort(factory.writeTxn);
			factory.writeTxn = 0;
		}
		try {
			// start a write transaction
			E(mdb_txn_begin(factory.env, NULL, 0, factory.writeTxnPp));
			factory.writeTxn = factory.writeTxnPp.get(0);
			mdb_drop(factory.writeTxn, dbi, false);
			factory.commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		size = 0;
	}

	@Override
	public Iterator<T> iterator() {
		try {
			factory.commit();
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

	private synchronized boolean update(Object element, boolean add) throws IOException {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			if (factory.writeTxn == 0) {
				// start a write transaction
				E(mdb_txn_begin(factory.env, NULL, 0, factory.writeTxnPp));
				factory.writeTxn = factory.writeTxnPp.get(0);
			}
			factory.ensureResize();

			MDBVal keyVal = MDBVal.malloc(stack);
			// use calloc to get an empty data value
			MDBVal dataVal = MDBVal.calloc(stack);

			byte[] data = write((T) element);
			ByteBuffer keyBuf = stack.malloc(data.length);
			keyBuf.put(data);
			keyBuf.flip();
			keyVal.mv_data(keyBuf);

			if (add) {
				int rc = mdb_put(factory.writeTxn, dbi, keyVal, dataVal, MDB_NOOVERWRITE);
				if (rc == MDB_SUCCESS) {
					size++;
					return true;
				} else if (rc == MDB_MAP_FULL) {
					factory.ensureResize();
					if (mdb_put(factory.writeTxn, dbi, keyVal, dataVal, MDB_NOOVERWRITE) == MDB_SUCCESS) {
						size++;
						return true;
					}
					return false;
				} else {
					logger.debug("Failed to add element due to error {}: {}", mdb_strerror(rc), element);
				}
			} else {
				// delete element
				int rc = mdb_del(factory.writeTxn, dbi, keyVal, dataVal);
				if (rc == MDB_SUCCESS) {
					size--;
					return true;
				} else if (rc == MDB_MAP_FULL) {
					factory.ensureResize();
					if (mdb_del(factory.writeTxn, dbi, keyVal, dataVal) == MDB_SUCCESS) {
						size--;
						return true;
					}
					return false;
				} else {
					logger.debug("Failed to remove element due to error {}: {}", mdb_strerror(rc), element);
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

	private class ElementIterator implements Iterator<T> {

		private final MDBVal keyData = MDBVal.malloc();
		private final MDBVal valueData = MDBVal.malloc();
		private final long cursor;

		private final StampedLock txnLock;
		private Txn txnRef;
		private long txnRefVersion;

		private T next;
		private T current;

		private ElementIterator(int dbi) {
			try {
				this.txnRef = factory.txnManager.createReadTxn();
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

		private T computeNext() throws IOException {
			long stamp = txnLock.readLock();
			try {
				if (txnRefVersion != txnRef.version()) {
					// cursor must be renewed
					mdb_cursor_renew(txnRef.get(), cursor);

					try (MemoryStack stack = MemoryStack.stackPush()) {
						keyData.mv_data(stack.bytes(write(current)));
						if (mdb_cursor_get(cursor, keyData, valueData, MDB_SET) != MDB_SUCCESS) {
							// use MDB_SET_RANGE if key was deleted
							if (mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE) == MDB_SUCCESS) {
								return read(keyData.mv_data());
							}
						}
					}
				}

				if (mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT) == MDB_SUCCESS) {
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
