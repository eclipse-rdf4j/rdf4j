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
package org.eclipse.rdf4j.sail.lmdb;

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.openDatabase;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOMETASYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOSYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.MDB_PREV;
import static org.lwjgl.util.lmdb.LMDB.MDB_RESERVE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_info;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_mapsize;
import static org.lwjgl.util.lmdb.LMDB.mdb_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_put;
import static org.lwjgl.util.lmdb.LMDB.mdb_stat;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_commit;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_renew;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_reset;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.CRC32;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.sail.lmdb.LmdbUtil.Transaction;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbBNode;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbIRI;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbResource;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBEnvInfo;
import org.lwjgl.util.lmdb.MDBStat;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * LMDB-based indexed storage and retrieval of RDF values. ValueStore maps RDF values to integer IDs and vice-versa.
 */
class ValueStore extends AbstractValueFactory {

	private static final byte URI_VALUE = 0x0; // 00

	private static final byte LITERAL_VALUE = 0x1; // 01

	private static final byte BNODE_VALUE = 0x2; // 10

	private static final byte NAMESPACE_VALUE = 0x3; // 11

	private static final byte ID_KEY = 0x4;

	private static final byte HASH_KEY = 0x5;

	private static final byte HASHID_KEY = 0x6;

	/***
	 * Maximum size of keys before hashing is used (size of two long values)
	 */
	private static final int MAX_KEY_SIZE = 16;
	/**
	 * Used to do the actual storage of values, once they're translated to byte arrays.
	 */
	private final File dir;
	/**
	 * A simple cache containing the [VALUE_CACHE_SIZE] most-recently used values stored by their ID.
	 */
	private final LmdbValue[] valueCache;
	/**
	 * A simple cache containing the [ID_CACHE_SIZE] most-recently used value-IDs stored by their value.
	 */
	private final ConcurrentCache<LmdbValue, Long> valueIDCache;
	/**
	 * A simple cache containing the [NAMESPACE_CACHE_SIZE] most-recently used namespaces stored by their ID.
	 */
	private final ConcurrentCache<Long, String> namespaceCache;
	/**
	 * A simple cache containing the [NAMESPACE_ID_CACHE_SIZE] most-recently used namespace-IDs stored by their
	 * namespace.
	 */
	private final ConcurrentCache<String, Long> namespaceIDCache;
	/**
	 * Used to do the actual storage of values, once they're translated to byte arrays.
	 */
	private long env;
	private int pageSize;
	private long mapSize;
	private int dbi;
	private long writeTxn;
	private final boolean forceSync;
	private final boolean autoGrow;
	/** This lock is required to block transactions while auto-growing the map size. */
	private final ReadWriteLock txnLock = new ReentrantReadWriteLock();

	/**
	 * An object that indicates the revision of the value store, which is used to check if cached value IDs are still
	 * valid. In order to be valid, the ValueStoreRevision object of a LmdbValue needs to be equal to this object.
	 */
	private volatile ValueStoreRevision revision;
	/**
	 * The next ID that is associated with a stored value
	 */
	private long nextId;

	ValueStore(File dir, LmdbStoreConfig config) throws IOException {
		this.dir = dir;
		this.forceSync = config.getForceSync();
		this.autoGrow = config.getAutoGrow();
		this.mapSize = config.getValueDBSize();
		open();

		valueCache = new LmdbValue[config.getValueCacheSize()];
		valueIDCache = new ConcurrentCache<>(config.getValueIDCacheSize());
		namespaceCache = new ConcurrentCache<>(config.getNamespaceCacheSize());
		namespaceIDCache = new ConcurrentCache<>(config.getNamespaceIDCacheSize());

		setNewRevision();

		// read maximum id from store
		readTransaction(env, (stack, txn) -> {
			long cursor = 0;
			try {
				PointerBuffer pp = stack.mallocPointer(1);
				E(mdb_cursor_open(txn, dbi, pp));
				cursor = pp.get(0);

				MDBVal keyData = MDBVal.calloc(stack);
				// set cursor to min key
				keyData.mv_data(stack.bytes(new byte[] { ID_KEY, (byte) 0xFF }));
				MDBVal valueData = MDBVal.calloc(stack);
				if (mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE) == 0 &&
						mdb_cursor_get(cursor, keyData, valueData, MDB_PREV) == 0) {
					// remove lower 2 type bits
					nextId = (data2id(keyData.mv_data()) >> 2) + 1;
				} else {
					nextId = 1;
				}
				return null;
			} finally {
				if (cursor != 0) {
					mdb_cursor_close(cursor);
				}
			}
		});
	}

	private void open() throws IOException {
		// create directory if it not exists
		dir.mkdirs();

		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			env = pp.get(0);
		}

		// Open environment
		int flags = MDB_NOTLS;
		if (!forceSync) {
			flags |= MDB_NOSYNC | MDB_NOMETASYNC;
		}
		E(mdb_env_open(env, dir.getAbsolutePath(), flags, 0664));

		// Open database
		dbi = openDatabase(env, null, MDB_CREATE, null);

		// initialize page size and set map size for env
		readTransaction(env, (stack, txn) -> {
			MDBStat stat = MDBStat.malloc(stack);
			mdb_stat(txn, dbi, stat);

			boolean isEmpty = stat.ms_entries() == 0;
			pageSize = stat.ms_psize();
			// align map size with page size
			long configMapSize = (mapSize / pageSize) * pageSize;
			if (isEmpty) {
				// this is an empty db, use configured map size
				mdb_env_set_mapsize(env, configMapSize);
			}
			MDBEnvInfo info = MDBEnvInfo.malloc(stack);
			mdb_env_info(env, info);
			mapSize = info.me_mapsize();
			if (mapSize < configMapSize) {
				// configured map size is larger than map size stored in env, increase map size
				mdb_env_set_mapsize(env, configMapSize);
				mapSize = configMapSize;
			}
			return null;
		});
	}

	private long nextId(byte type) throws IOException {
		long result = nextId;
		nextId++;
		// encode type in lower 2 bits of id
		result = (result << 2) | type;
		return result;
	}

	protected ByteBuffer idBuffer(MemoryStack stack) {
		return stack.malloc(2 + Long.BYTES);
	}

	protected ByteBuffer id2data(ByteBuffer bb, long id) {
		bb.put(ID_KEY);
		Varint.writeUnsigned(bb, id);
		return bb;
	}

	protected long data2id(ByteBuffer bb) {
		// skip id marker
		bb.get();
		return Varint.readUnsigned(bb);
	}

	/**
	 * Creates a new revision object for this value store, invalidating any IDs cached in LmdbValue objects that were
	 * created by this value store.
	 */
	private void setNewRevision() {
		revision = new ValueStoreRevision(this);
	}

	public ValueStoreRevision getRevision() {
		return revision;
	}

	protected byte[] getData(long id) throws IOException {
		return readTransaction(env, (stack, txn) -> {
			MDBVal keyData = MDBVal.calloc(stack);
			keyData.mv_data(id2data(idBuffer(stack), id).flip());
			MDBVal valueData = MDBVal.calloc(stack);
			if (mdb_get(txn, dbi, keyData, valueData) == 0) {
				byte[] valueBytes = new byte[valueData.mv_data().remaining()];
				valueData.mv_data().get(valueBytes);
				return valueBytes;
			}
			return null;
		});
	}

	/**
	 * Get value from cache by ID.
	 *
	 * Thread-safety with synchronized is not required here.
	 *
	 * @param id ID of a value object
	 * @return the value object or <code>null</code> if not found
	 */
	LmdbValue cachedValue(long id) {
		LmdbValue value = valueCache[(int) (id % valueCache.length)];
		if (value != null && value.getInternalID() == id) {
			return value;
		}
		return null;
	}

	/**
	 * Cache value by ID.
	 *
	 * Thread-safety with synchronized is not required here.
	 *
	 * @param id    ID of a value object
	 * @param value ID of a value object
	 * @return the value object or <code>null</code> if not found
	 */
	void cacheValue(long id, LmdbValue value) {
		valueCache[(int) (id % valueCache.length)] = value;
	}

	/**
	 * Gets the value for the specified ID which is lazy initialized at a later point in time.
	 *
	 * @param id A value ID.
	 * @return The value for the ID, or <tt>null</tt> no such value could be found.
	 * @throws IOException If an I/O error occurred.
	 */
	public LmdbValue getLazyValue(long id) throws IOException {
		// Check value cache
		Long cacheID = id;
		LmdbValue resultValue = cachedValue(cacheID);

		if (resultValue == null) {
			switch ((byte) (id & 0x3)) {
			case URI_VALUE:
				resultValue = new LmdbIRI(revision, id);
				break;
			case LITERAL_VALUE:
				resultValue = new LmdbLiteral(revision, id);
				break;
			case BNODE_VALUE:
				resultValue = new LmdbBNode(revision, id);
				break;
			default:
				throw new IOException("Unsupported value with type id " + (id & 0x3));
			}
			// Store value in cache
			cacheValue(cacheID, resultValue);
		}

		return resultValue;
	}

	/**
	 * Gets the value for the specified ID.
	 *
	 * @param id A value ID.
	 * @return The value for the ID, or <tt>null</tt> no such value could be found.
	 * @throws IOException If an I/O error occurred.
	 */
	public LmdbValue getValue(long id) throws IOException {
		// Check value cache
		Long cacheID = id;
		LmdbValue resultValue = cachedValue(cacheID);

		if (resultValue == null) {
			// Value not in cache, fetch it from file
			byte[] data = getData(id);

			if (data != null) {
				resultValue = data2value(id, data, null);
				// Store value in cache
				cacheValue(cacheID, resultValue);
			}
		}

		return resultValue;
	}

	/**
	 * Initializes the value for the specified ID.
	 *
	 * @param id    A value ID.
	 * @param value Existing value that should be resolved.
	 * @return <code>true</code> if value could be successfully resolved, else <code>false</code>
	 */
	public boolean resolveValue(long id, LmdbValue value) {
		try {
			byte[] data = getData(id);
			if (data != null) {
				data2value(id, data, value);
				return true;
			}
		} catch (IOException e) {
			// should not happen
		}
		return false;
	}

	private void resizeMap(long txn, int requiredSize) throws IOException {
		if (autoGrow) {
			if (LmdbUtil.requiresResize(mapSize, pageSize, txn, requiredSize)) {
				// map is full, resize
				txnLock.readLock().unlock();
				txnLock.writeLock().lock();
				try {
					boolean activeWriteTxn = writeTxn != 0;
					boolean txnIsRead = txn != writeTxn;
					if (txnIsRead) {
						mdb_txn_reset(txn);
					}
					if (activeWriteTxn) {
						endTransaction(true);
					}
					mapSize = LmdbUtil.autoGrowMapSize(mapSize, pageSize, requiredSize);
					E(mdb_env_set_mapsize(env, mapSize));
					if (activeWriteTxn) {
						startTransaction();
					}
					if (txnIsRead) {
						E(mdb_txn_renew(txn));
					}
				} finally {
					txnLock.writeLock().unlock();
					txnLock.readLock().lock();
				}
			}
		}
	}

	private long findId(byte[] data, boolean create) throws IOException {
		Long id = readTransaction(env, (stack, txn) -> {
			if (data.length < MAX_KEY_SIZE) {
				MDBVal dataVal = MDBVal.calloc(stack);
				dataVal.mv_data(stack.bytes(data));
				MDBVal idVal = MDBVal.calloc(stack);
				if (mdb_get(txn, dbi, dataVal, idVal) == 0) {
					return data2id(idVal.mv_data());
				}
				if (!create) {
					return null;
				}
				// id was not found, create a new one
				resizeMap(txn, 2 * data.length + 2 * (2 + Long.BYTES));

				long newId = nextId(data[0]);
				writeTransaction((stack2, writeTxn) -> {
					idVal.mv_data(id2data(idBuffer(stack), newId).flip());

					E(mdb_put(writeTxn, dbi, dataVal, idVal, 0));
					E(mdb_put(writeTxn, dbi, idVal, dataVal, 0));
					return null;
				});
				return newId;
			} else {
				MDBVal idVal = MDBVal.calloc(stack);

				ByteBuffer dataBb = ByteBuffer.wrap(data);
				long dataHash = hash(data);
				int maxHashKeyLength = 2 + 2 * Long.BYTES + 2;
				ByteBuffer hashBb = stack.malloc(maxHashKeyLength);
				hashBb.put(HASH_KEY);
				Varint.writeUnsigned(hashBb, dataHash);
				int hashLength = hashBb.position();
				hashBb.flip();

				MDBVal hashVal = MDBVal.calloc(stack);
				hashVal.mv_data(hashBb);
				MDBVal dataVal = MDBVal.calloc(stack);

				// ID of first value is directly stored with hash as key
				if (mdb_get(txn, dbi, hashVal, dataVal) == 0) {
					idVal.mv_data(dataVal.mv_data());
					if (mdb_get(txn, dbi, idVal, dataVal) == 0 &&
							dataVal.mv_data().compareTo(dataBb) == 0) {
						return data2id(idVal.mv_data());
					}
				} else {
					// no value for hash exists
					if (!create) {
						return null;
					}

					resizeMap(txn, 2 * data.length + 2 * (2 + Long.BYTES));

					long newId = nextId(data[0]);
					writeTransaction((stack2, writeTxn) -> {
						dataVal.mv_size(data.length);
						idVal.mv_data(id2data(idBuffer(stack), newId).flip());

						// store mapping of hash -> ID
						E(mdb_put(txn, dbi, hashVal, idVal, 0));
						// store mapping of ID -> data
						E(mdb_put(writeTxn, dbi, idVal, dataVal, MDB_RESERVE));
						dataVal.mv_data().put(data);
						return null;
					});
					return newId;
				}

				// test existing entries for hash key against given value
				hashBb.put(0, HASHID_KEY);
				hashVal.mv_data(hashBb);

				long cursor = 0;
				try {
					PointerBuffer pp = stack.mallocPointer(1);
					E(mdb_cursor_open(txn, dbi, pp));
					cursor = pp.get(0);

					// iterate all entries for hash value
					if (mdb_cursor_get(cursor, hashVal, dataVal, MDB_SET_RANGE) == 0) {
						do {
							if (compareRegion(hashVal.mv_data(), 0, hashBb, 0, hashLength) != 0) {
								break;
							}

							// use only ID part of key for lookup of data
							ByteBuffer hashIdBb = hashVal.mv_data();
							hashIdBb.position(hashLength);
							idVal.mv_data(hashIdBb);
							if (mdb_get(txn, dbi, idVal, dataVal) == 0 &&
									dataVal.mv_data().compareTo(dataBb) == 0) {
								// id was found if stored value is equal to requested value
								return data2id(hashIdBb);
							}
						} while (mdb_cursor_get(cursor, hashVal, dataVal, MDB_NEXT) == 0);
					}
				} finally {
					if (cursor != 0) {
						mdb_cursor_close(cursor);
					}
				}

				if (!create) {
					return null;
				}

				// id was not found, create a new one
				resizeMap(txn, 1 + Long.BYTES + maxHashKeyLength + 2 * data.length);

				long newId = nextId(data[0]);
				writeTransaction((stack2, writeTxn) -> {
					// encode ID
					ByteBuffer idBb = id2data(idBuffer(stack), newId).flip();
					idVal.mv_data(idBb);

					// encode hash and ID
					hashBb.limit(hashBb.capacity());
					hashBb.position(hashLength);
					hashBb.put(idBb);
					idBb.rewind();
					hashBb.flip();
					hashVal.mv_data(hashBb);

					// store mapping of hash+ID -> []
					dataVal.mv_data(stack.bytes());
					E(mdb_put(txn, dbi, hashVal, dataVal, 0));

					dataVal.mv_size(data.length);
					// store mapping of ID -> data
					E(mdb_put(txn, dbi, idVal, dataVal, MDB_RESERVE));
					dataVal.mv_data().put(data);
					return null;
				});
				return newId;
			}
		});
		return id != null ? id : LmdbValue.UNKNOWN_ID;
	}

	<T> T readTransaction(long env, Transaction<T> transaction) throws IOException {
		txnLock.readLock().lock();
		try {
			return LmdbUtil.readTransaction(env, writeTxn, transaction);
		} finally {
			txnLock.readLock().unlock();
		}
	}

	<T> T writeTransaction(Transaction<T> transaction) throws IOException {
		if (writeTxn != 0) {
			try (MemoryStack stack = MemoryStack.stackPush()) {
				return transaction.exec(stack, writeTxn);
			}
		} else {
			return LmdbUtil.transaction(env, transaction);
		}
	}

	int compareRegion(ByteBuffer array1, int startIdx1, ByteBuffer array2, int startIdx2, int length) {
		int result = 0;
		for (int i = 0; result == 0 && i < length; i++) {
			result = (array1.get(startIdx1 + i) & 0xff) - (array2.get(startIdx2 + i) & 0xff);
		}
		return result;
	}

	/**
	 * Gets the ID for the specified value.
	 *
	 * @param value A value.
	 * @return The ID for the specified value, or {@link LmdbValue#UNKNOWN_ID} if no such ID could be found.
	 * @throws IOException If an I/O error occurred.
	 */
	public long getId(Value value) throws IOException {
		return getId(value, false);
	}

	/**
	 * Gets the ID for the specified value.
	 *
	 * @param value A value.
	 * @return The ID for the specified value, or {@link LmdbValue#UNKNOWN_ID} if no such ID could be found.
	 * @throws IOException If an I/O error occurred.
	 */
	public long getId(Value value, boolean create) throws IOException {
		// Try to get the internal ID from the value itself
		boolean isOwnValue = isOwnValue(value);

		if (isOwnValue) {
			LmdbValue lmdbValue = (LmdbValue) value;

			if (revisionIsCurrent(lmdbValue)) {
				long id = lmdbValue.getInternalID();

				if (id != LmdbValue.UNKNOWN_ID) {
					return id;
				}
			}
		}

		// Check cache
		Long cachedID = valueIDCache.get(value);

		if (cachedID != null) {
			long id = cachedID;

			if (isOwnValue) {
				// Store id in value for fast access in any consecutive calls
				((LmdbValue) value).setInternalID(id, revision);
			}

			return id;
		}

		// ID not cached, search in file
		byte[] data = value2data(value, create);
		if (data == null && value instanceof Literal) {
			data = literal2legacy((Literal) value);
		}

		if (data != null) {
			long id = findId(data, create);

			if (id != LmdbValue.UNKNOWN_ID) {
				if (isOwnValue) {
					// Store id in value for fast access in any consecutive calls
					((LmdbValue) value).setInternalID(id, revision);
					// Store id in cache
					valueIDCache.put((LmdbValue) value, id);
				} else {
					// Store id in cache
					LmdbValue nv = getLmdbValue(value);
					nv.setInternalID(id, revision);
					valueIDCache.put(nv, id);
				}
			}

			return id;
		}

		return LmdbValue.UNKNOWN_ID;
	}

	public void startTransaction() throws IOException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);

			E(mdb_txn_begin(env, NULL, 0, pp));
			writeTxn = pp.get(0);
		}
	}

	/**
	 * Closes the snapshot and the DB iterator if any was opened in the current transaction
	 */
	void endTransaction(boolean commit) throws IOException {
		if (writeTxn != 0) {
			if (commit) {
				E(mdb_txn_commit(writeTxn));
			} else {
				mdb_txn_abort(writeTxn);
			}
			writeTxn = 0;
		}
	}

	public void commit() throws IOException {
		endTransaction(true);
	}

	public void rollback() throws IOException {
		endTransaction(false);
	}

	/**
	 * Stores the supplied value and returns the ID that has been assigned to it. In case the value was already present,
	 * the value will not be stored again and the ID of the existing value is returned.
	 *
	 * @param value The Value to store.
	 * @return The ID that has been assigned to the value.
	 * @throws IOException If an I/O error occurred.
	 */
	public long storeValue(Value value) throws IOException {
		return getId(value, true);
	}

	/**
	 * Computes a hash code for the supplied data.
	 *
	 * @param data The data to calculate the hash code for.
	 * @return A hash code for the supplied data.
	 */
	private long hash(byte[] data) {
		CRC32 crc32 = new CRC32();
		crc32.update(data);
		return crc32.getValue();
	}

	/**
	 * Removes all values from the ValueStore.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public void clear() throws IOException {
		close();

		new File(dir, "data.mdb").delete();
		new File(dir, "lock.mdb").delete();

		Arrays.fill(valueCache, null);
		valueIDCache.clear();
		namespaceCache.clear();
		namespaceIDCache.clear();

		open();

		setNewRevision();
	}

	/**
	 * Closes the ValueStore, releasing any file references, etc. Once closed, the ValueStore can no longer be used.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public void close() throws IOException {
		if (env != 0) {
			endTransaction(false);
			mdb_env_close(env);
			env = 0;
		}
	}

	/**
	 * Checks if the supplied Value object is a LmdbValue object that has been created by this ValueStore.
	 */
	private boolean isOwnValue(Value value) {
		return value instanceof LmdbValue && ((LmdbValue) value).getValueStoreRevision().getValueStore() == this;
	}

	/**
	 * Checks if the revision of the supplied value object is still current.
	 */
	private boolean revisionIsCurrent(LmdbValue value) {
		return revision.equals(value.getValueStoreRevision());
	}

	private byte[] value2data(Value value, boolean create) throws IOException {
		if (value instanceof IRI) {
			return uri2data((IRI) value, create);
		} else if (value instanceof BNode) {
			return bnode2data((BNode) value, create);
		} else if (value instanceof Literal) {
			return literal2data((Literal) value, create);
		} else {
			throw new IllegalArgumentException("value parameter should be a URI, BNode or Literal");
		}
	}

	private byte[] uri2data(IRI uri, boolean create) throws IOException {
		long nsID = getNamespaceID(uri.getNamespace(), create);

		if (nsID == -1) {
			// Unknown namespace means unknown URI
			return null;
		}

		// Get local name in UTF-8
		byte[] localNameData = uri.getLocalName().getBytes(StandardCharsets.UTF_8);

		// Combine parts in a single byte array
		int nsIDLength = Varint.calcLengthUnsigned(nsID);
		byte[] uriData = new byte[1 + nsIDLength + localNameData.length];
		uriData[0] = URI_VALUE;
		Varint.writeUnsigned(ByteBuffer.wrap(uriData, 1, nsIDLength), nsID);
		ByteArrayUtil.put(localNameData, uriData, 1 + nsIDLength);

		return uriData;
	}

	private byte[] bnode2data(BNode bNode, boolean create) throws IOException {
		byte[] idData = bNode.getID().getBytes(StandardCharsets.UTF_8);

		byte[] bNodeData = new byte[1 + idData.length];
		bNodeData[0] = BNODE_VALUE;
		ByteArrayUtil.put(idData, bNodeData, 1);

		return bNodeData;
	}

	private byte[] literal2data(Literal literal, boolean create) throws IOException {
		return literal2data(literal.getLabel(), literal.getLanguage(), literal.getDatatype(), create);
	}

	private byte[] literal2legacy(Literal literal) throws IOException {
		IRI dt = literal.getDatatype();
		if (org.eclipse.rdf4j.model.vocabulary.XSD.STRING.equals(dt)
				|| org.eclipse.rdf4j.model.vocabulary.RDF.LANGSTRING.equals(dt)) {
			return literal2data(literal.getLabel(), literal.getLanguage(), null, false);
		}
		return literal2data(literal.getLabel(), literal.getLanguage(), dt, false);
	}

	private byte[] literal2data(String label, Optional<String> lang, IRI dt, boolean create)
			throws IOException {
		// Get datatype ID
		long datatypeID = LmdbValue.UNKNOWN_ID;

		if (dt != null) {
			datatypeID = getId(dt, create);

			if (datatypeID == LmdbValue.UNKNOWN_ID) {
				// Unknown datatype means unknown literal
				return null;
			}
		}

		// Get language tag in UTF-8
		byte[] langData = null;
		int langDataLength = 0;
		if (lang.isPresent()) {
			langData = lang.get().getBytes(StandardCharsets.UTF_8);
			langDataLength = langData.length;
		}

		// Get label in UTF-8
		byte[] labelData = label.getBytes(StandardCharsets.UTF_8);

		// Combine parts in a single byte array
		int datatypeIDLength = Varint.calcLengthUnsigned(datatypeID);
		byte[] literalData = new byte[2 + datatypeIDLength + langDataLength + labelData.length];
		ByteBuffer bb = ByteBuffer.wrap(literalData);
		bb.put(LITERAL_VALUE);
		Varint.writeUnsigned(bb, datatypeID);
		bb.put((byte) langDataLength);
		if (langData != null) {
			bb.put(langData);
		}
		bb.put(labelData);

		return literalData;
	}

	private boolean isNamespaceData(byte[] data) {
		return data[0] == NAMESPACE_VALUE;
	}

	private LmdbValue data2value(long id, byte[] data, LmdbValue value) throws IOException {
		switch (data[0]) {
		case URI_VALUE:
			return data2uri(id, data, (LmdbIRI) value);
		case BNODE_VALUE:
			return data2bnode(id, data, (LmdbBNode) value);
		case LITERAL_VALUE:
			return data2literal(id, data, (LmdbLiteral) value);
		default:
			throw new IllegalArgumentException("Invalid type " + data[0] + " for value with id " + id);
		}
	}

	private LmdbIRI data2uri(long id, byte[] data, LmdbIRI value) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(data);
		// skip type marker
		bb.get();
		long nsID = Varint.readUnsigned(bb);
		String namespace = getNamespace(nsID);
		String localName = new String(data, bb.position(), bb.remaining(), StandardCharsets.UTF_8);

		if (value == null) {
			return new LmdbIRI(revision, namespace, localName, id);
		} else {
			value.setIRIString(namespace + localName);
			return value;
		}
	}

	private LmdbBNode data2bnode(long id, byte[] data, LmdbBNode value) throws IOException {
		String nodeID = new String(data, 1, data.length - 1, StandardCharsets.UTF_8);
		if (value == null) {
			return new LmdbBNode(revision, nodeID, id);
		} else {
			value.setID(nodeID);
			return value;
		}
	}

	private LmdbLiteral data2literal(long id, byte[] data, LmdbLiteral value) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(data);
		// skip type marker
		bb.get();
		// Get datatype
		long datatypeID = Varint.readUnsigned(bb);
		IRI datatype = null;
		if (datatypeID != LmdbValue.UNKNOWN_ID) {
			datatype = (IRI) getValue(datatypeID);
		}

		// Get language tag
		String lang = null;
		int langLength = bb.get() & 0xFF;
		if (langLength > 0) {
			lang = new String(data, bb.position(), langLength, StandardCharsets.UTF_8);
		}

		// Get label
		String label = new String(data, bb.position() + langLength, data.length - bb.position() - langLength,
				StandardCharsets.UTF_8);

		if (value == null) {
			if (lang != null) {
				return new LmdbLiteral(revision, label, lang, id);
			} else if (datatype != null) {
				return new LmdbLiteral(revision, label, datatype, id);
			} else {
				return new LmdbLiteral(revision, label, org.eclipse.rdf4j.model.vocabulary.XSD.STRING, id);
			}
		} else {
			value.setLabel(label);
			if (lang != null) {
				value.setLanguage(lang);
				value.setDatatype(CoreDatatype.RDF.LANGSTRING);
			} else if (datatype != null) {
				value.setDatatype(datatype);
			} else {
				value.setDatatype(CoreDatatype.XSD.STRING);
			}
			return value;
		}
	}

	private String data2namespace(byte[] data) {
		return new String(data, 1, data.length - 1, StandardCharsets.UTF_8);
	}

	private long getNamespaceID(String namespace, boolean create) throws IOException {
		Long cacheID = namespaceIDCache.get(namespace);
		if (cacheID != null) {
			return cacheID;
		}

		byte[] namespaceBytes = namespace.getBytes(StandardCharsets.UTF_8);
		byte[] namespaceData = new byte[namespaceBytes.length + 1];
		namespaceData[0] = NAMESPACE_VALUE;
		System.arraycopy(namespaceBytes, 0, namespaceData, 1, namespaceBytes.length);

		long id = findId(namespaceData, create);
		if (id != LmdbValue.UNKNOWN_ID) {
			namespaceIDCache.put(namespace, id);
		}

		return id;
	}

	/*-------------------------------------*
	 * Methods from interface ValueFactory *
	 *-------------------------------------*/

	private String getNamespace(long id) throws IOException {
		Long cacheID = id;
		String namespace = namespaceCache.get(cacheID);

		if (namespace == null) {
			byte[] namespaceData = getData(id);
			if (namespaceData != null) {
				namespace = data2namespace(namespaceData);
				namespaceCache.put(cacheID, namespace);
			}
		}

		return namespace;
	}

	@Override
	public LmdbIRI createIRI(String uri) {
		return new LmdbIRI(revision, uri);
	}

	@Override
	public LmdbIRI createIRI(String namespace, String localName) {
		return new LmdbIRI(revision, namespace, localName);
	}

	@Override
	public LmdbBNode createBNode(String nodeID) {
		return new LmdbBNode(revision, nodeID);
	}

	@Override
	public LmdbLiteral createLiteral(String value) {
		return new LmdbLiteral(revision, value, CoreDatatype.XSD.STRING);
	}

	@Override
	public LmdbLiteral createLiteral(String value, String language) {
		return new LmdbLiteral(revision, value, language);
	}

	/*----------------------------------------------------------------------*
	 * Methods for converting model objects to LmdbStore-specific objects *
	 *----------------------------------------------------------------------*/

	@Override
	public LmdbLiteral createLiteral(String value, IRI datatype) {
		return new LmdbLiteral(revision, value, datatype);
	}

	public LmdbValue getLmdbValue(Value value) {
		if (value instanceof Resource) {
			return getLmdbResource((Resource) value);
		} else if (value instanceof Literal) {
			return getLmdbLiteral((Literal) value);
		} else {
			throw new IllegalArgumentException("Unknown value type: " + value.getClass());
		}
	}

	public LmdbResource getLmdbResource(Resource resource) {
		if (resource instanceof IRI) {
			return getLmdbURI((IRI) resource);
		} else if (resource instanceof BNode) {
			return getLmdbBNode((BNode) resource);
		} else {
			throw new IllegalArgumentException("Unknown resource type: " + resource.getClass());
		}
	}

	/**
	 * Creates a LmdbURI that is equal to the supplied URI. This method returns the supplied URI itself if it is already
	 * a LmdbURI that has been created by this ValueStore, which prevents unnecessary object creations.
	 *
	 * @return A LmdbURI for the specified URI.
	 */
	public LmdbIRI getLmdbURI(IRI uri) {
		if (isOwnValue(uri)) {
			return (LmdbIRI) uri;
		}

		return new LmdbIRI(revision, uri.toString());
	}

	/**
	 * Creates a LmdbBNode that is equal to the supplied bnode. This method returns the supplied bnode itself if it is
	 * already a LmdbBNode that has been created by this ValueStore, which prevents unnecessary object creations.
	 *
	 * @return A LmdbBNode for the specified bnode.
	 */
	public LmdbBNode getLmdbBNode(BNode bnode) {
		if (isOwnValue(bnode)) {
			return (LmdbBNode) bnode;
		}

		return new LmdbBNode(revision, bnode.getID());
	}

	/*--------------------*
	 * Test/debug methods *
	 *--------------------*/

	/**
	 * Creates an LmdbLiteral that is equal to the supplied literal. This method returns the supplied literal itself if
	 * it is already a LmdbLiteral that has been created by this ValueStore, which prevents unnecessary object
	 * creations.
	 *
	 * @return A LmdbLiteral for the specified literal.
	 */
	public LmdbLiteral getLmdbLiteral(Literal l) {
		if (isOwnValue(l)) {
			return (LmdbLiteral) l;
		}

		if (Literals.isLanguageLiteral(l)) {
			return new LmdbLiteral(revision, l.getLabel(), l.getLanguage().get());
		} else if (l.getCoreDatatype() != CoreDatatype.NONE) {
			return new LmdbLiteral(revision, l.getLabel(), l.getCoreDatatype());
		} else {
			LmdbIRI datatype = getLmdbURI(l.getDatatype());
			return new LmdbLiteral(revision, l.getLabel(), datatype, l.getCoreDatatype());
		}
	}
}
