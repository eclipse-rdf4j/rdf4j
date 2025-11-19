/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.eclipse.rdf4j.sail.nativerdf.NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.zip.CRC32C;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.ReadWriteLockManager;
import org.eclipse.rdf4j.common.concurrent.locks.WritePrefReadWriteLockManager;
import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore;
import org.eclipse.rdf4j.sail.nativerdf.datastore.RecoveredDataException;
import org.eclipse.rdf4j.sail.nativerdf.model.CorruptIRI;
import org.eclipse.rdf4j.sail.nativerdf.model.CorruptIRIOrBNode;
import org.eclipse.rdf4j.sail.nativerdf.model.CorruptLiteral;
import org.eclipse.rdf4j.sail.nativerdf.model.CorruptUnknownValue;
import org.eclipse.rdf4j.sail.nativerdf.model.CorruptValue;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeBNode;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeIRI;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeLiteral;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeResource;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeValue;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWAL;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalConfig;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalReader;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalRecord;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalRecovery;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalSearch;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalValueKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File-based indexed storage and retrieval of RDF values. ValueStore maps RDF values to integer IDs and vice-versa.
 *
 * @author Arjohn Kampman
 * @apiNote This feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class ValueStore extends SimpleValueFactory implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ValueStore.class);

	private static final String WAL_RECOVERY_LOG_PROP = "org.eclipse.rdf4j.sail.nativerdf.valuestorewal.recoveryLog";
	private static final String WAL_RECOVERY_LOG = System.getProperty(WAL_RECOVERY_LOG_PROP, "debug").toLowerCase();

	/**
	 * The default value cache size.
	 */
	public static final int VALUE_CACHE_SIZE = 512;

	/**
	 * The default value id cache size.
	 */
	public static final int VALUE_ID_CACHE_SIZE = 128;

	/**
	 * The default namespace cache size.
	 */
	public static final int NAMESPACE_CACHE_SIZE = 64;

	/**
	 * The default namespace id cache size.
	 */
	public static final int NAMESPACE_ID_CACHE_SIZE = 32;

	private static final String FILENAME_PREFIX = "values";

	private static final byte URI_VALUE = 0x1; // 0000 0001

	private static final byte BNODE_VALUE = 0x2; // 0000 0010

	private static final byte LITERAL_VALUE = 0x3; // 0000 0011

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Used to do the actual storage of values, once they're translated to byte arrays.
	 */
	private final File dataDir;
	private final DataStore dataStore;
	private final ValueStoreWAL wal;
	private final ThreadLocal<Long> walPendingLsn;
	private volatile CompletableFuture<Void> walBootstrapFuture;
	private volatile ValueStoreWalSearch walSearch;

	/**
	 * Lock manager used to prevent the removal of values over multiple method calls. Note that values can still be
	 * added when read locks are active.
	 */
	private final ReadWriteLockManager lockManager = new WritePrefReadWriteLockManager();

	/**
	 * An object that indicates the revision of the value store, which is used to check if cached value IDs are still
	 * valid. In order to be valid, the ValueStoreRevision object of a NativeValue needs to be equal to this object.
	 */
	private volatile ValueStoreRevision revision;

	/**
	 * A simple cache containing the [VALUE_CACHE_SIZE] most-recently used values stored by their ID.
	 */
	private final ConcurrentCache<Integer, NativeValue> valueCache;

	/**
	 * A simple cache containing the [ID_CACHE_SIZE] most-recently used value-IDs stored by their value.
	 */
	private final ConcurrentCache<NativeValue, Integer> valueIDCache;

	/**
	 * A simple cache containing the [NAMESPACE_CACHE_SIZE] most-recently used namespaces stored by their ID.
	 */
	private final ConcurrentCache<Integer, String> namespaceCache;

	/**
	 * A simple cache containing the [NAMESPACE_ID_CACHE_SIZE] most-recently used namespace-IDs stored by their
	 * namespace.
	 */
	private final ConcurrentCache<String, Integer> namespaceIDCache;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ValueStore(File dataDir) throws IOException {
		this(dataDir, false);
	}

	public ValueStore(File dataDir, boolean forceSync) throws IOException {
		this(dataDir, forceSync, VALUE_CACHE_SIZE, VALUE_ID_CACHE_SIZE, NAMESPACE_CACHE_SIZE, NAMESPACE_ID_CACHE_SIZE);
	}

	public ValueStore(File dataDir, boolean forceSync, int valueCacheSize, int valueIDCacheSize, int namespaceCacheSize,
			int namespaceIDCacheSize) throws IOException {
		this(dataDir, forceSync, valueCacheSize, valueIDCacheSize, namespaceCacheSize, namespaceIDCacheSize, null);
	}

	public ValueStore(File dataDir, boolean forceSync, int valueCacheSize, int valueIDCacheSize, int namespaceCacheSize,
			int namespaceIDCacheSize, ValueStoreWAL wal) throws IOException {
		super();
		this.dataDir = dataDir;
		dataStore = new DataStore(dataDir, FILENAME_PREFIX, forceSync, this);

		valueCache = new ConcurrentCache<>(valueCacheSize);
		valueIDCache = new ConcurrentCache<>(valueIDCacheSize);
		namespaceCache = new ConcurrentCache<>(namespaceCacheSize);
		namespaceIDCache = new ConcurrentCache<>(namespaceIDCacheSize);

		this.wal = wal;
		this.walPendingLsn = wal != null ? ThreadLocal.withInitial(() -> ValueStoreWAL.NO_LSN) : null;

		autoRecoverValueStoreIfConfigured();

		setNewRevision();
		maybeScheduleWalBootstrap();

	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Creates a new revision object for this value store, invalidating any IDs cached in NativeValue objects that were
	 * created by this value store.
	 */
	private void setNewRevision() {
		revision = new ValueStoreRevision(this);
	}

	public ValueStoreRevision getRevision() {
		return revision;
	}

	/**
	 * Gets a read lock on this value store that can be used to prevent values from being removed while the lock is
	 * active.
	 */
	public Lock getReadLock() throws InterruptedException {
		return lockManager.getReadLock();
	}

	/**
	 * Gets the value for the specified ID.
	 *
	 * @param id A value ID.
	 * @return The value for the ID, or <var>null</var> no such value could be found.
	 * @throws IOException If an I/O error occurred.
	 */
	public NativeValue getValue(int id) throws IOException {

		// Check value cache
		Integer cacheID = id;
		NativeValue resultValue = valueCache.get(cacheID);

		if (resultValue == null) {
			boolean recoveredDirectlyFromWal = false;
			try {
				// Value not in cache, fetch it from file
				byte[] data = dataStore.getData(id);

				if (data != null) {
					resultValue = data2value(id, data);
					if (resultValue instanceof CorruptValue) {
						NativeValue recovered = ((CorruptValue) resultValue).getRecovered();
						if (recovered != null) {
							resultValue = recovered;
						}
					} else if (shouldValidateAgainstWal()) {
						NativeValue walValue = recoverValueFromWal(id, false);
						if (walValue != null && !valuesMatch(resultValue, walValue)) {
							resultValue = walValue;
							recoveredDirectlyFromWal = true;
						}
					}
				} else {
					resultValue = recoverValueFromWal(id, false);
					recoveredDirectlyFromWal = resultValue != null;
				}

			} catch (RecoveredDataException rde) {
				byte[] recovered = rde.getData();
				CorruptValue corruptValue;
				if (recovered != null && recovered.length > 0) {
					byte t = recovered[0];
					if (t == URI_VALUE) {
						corruptValue = new CorruptIRI(revision, id, null, recovered);
					} else if (t == BNODE_VALUE) {
						corruptValue = new CorruptIRIOrBNode(revision, id, recovered);
					} else if (t == LITERAL_VALUE) {
						corruptValue = new CorruptLiteral(revision, id, recovered);
					} else {
						corruptValue = new CorruptUnknownValue(revision, id, recovered);
					}
				} else {
					corruptValue = new CorruptUnknownValue(revision, id, recovered);
				}

				tryRecoverFromWal(id, corruptValue);
				NativeValue recoveredValue = corruptValue.getRecovered();
				if (recoveredValue != null) {
					resultValue = recoveredValue;
					recoveredDirectlyFromWal = true;
				} else {
					resultValue = corruptValue;
				}
			}

			if (recoveredDirectlyFromWal && resultValue != null) {
				logRecovered(id, resultValue);
				logWalRepairHint(id);
			}

			if (resultValue != null && !(resultValue instanceof CorruptValue)) {
				// Store value in cache
				valueCache.put(cacheID, resultValue);
			}
		}

		return resultValue;

	}

	/**
	 * Gets the Resource for the specified ID.
	 *
	 * @param id A value ID.
	 * @return The Resource for the ID, or <var>null</var> no such value could be found.
	 * @throws IOException If an I/O error occurred.
	 */
	public <T extends NativeValue & Resource> T getResource(int id) throws IOException {

		NativeValue resultValue = getValue(id);

		if (resultValue != null && !(resultValue instanceof Resource)) {
			if (SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES && resultValue instanceof CorruptValue) {
				return (T) new CorruptIRIOrBNode(revision, id, ((CorruptValue) resultValue).getData());
			}
			logger.warn(
					"NativeStore is possibly corrupt. To attempt to repair or retrieve the data, read the documentation on http://rdf4j.org about the system property org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes");
		}

		return (T) resultValue;
	}

	/**
	 * Gets the IRI for the specified ID.
	 *
	 * @param id A value ID.
	 * @return The IRI for the ID, or <var>null</var> no such value could be found.
	 * @throws IOException If an I/O error occurred.
	 */
	public <T extends NativeValue & IRI> T getIRI(int id) throws IOException {

		NativeValue resultValue = getValue(id);

		if (resultValue != null && !(resultValue instanceof IRI)) {
			if (SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES && resultValue instanceof CorruptValue) {
				if (resultValue instanceof CorruptIRI) {
					return (T) resultValue;
				}
				return (T) new CorruptIRI(revision, id, null, ((CorruptValue) resultValue).getData());
			}
			logger.warn(
					"NativeStore is possibly corrupt. To attempt to repair or retrieve the data, read the documentation on http://rdf4j.org about the system property org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes");
		}

		return (T) resultValue;
	}

	/**
	 * Gets the ID for the specified value.
	 *
	 * @param value A value.
	 * @return The ID for the specified value, or {@link NativeValue#UNKNOWN_ID} if no such ID could be found.
	 * @throws IOException If an I/O error occurred.
	 */
	public int getID(Value value) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("getID start thread={} value={}", threadName(), describeValue(value));
		}
		// Try to get the internal ID from the value itself
		boolean isOwnValue = isOwnValue(value);

		if (isOwnValue) {
			NativeValue nativeValue = (NativeValue) value;

			if (revisionIsCurrent(nativeValue)) {
				int id = nativeValue.getInternalID();

				if (id != NativeValue.UNKNOWN_ID) {
					if (logger.isDebugEnabled()) {
						logger.debug("getID returning cached internal id {} for value={} thread={}", id,
								describeValue(value), threadName());
					}
					return id;
				}
			}
		}

		// Check cache
		Integer cachedID = valueIDCache.get(value);

		if (cachedID != null) {
			int id = cachedID.intValue();

			if (isOwnValue) {
				// Store id in value for fast access in any consecutive calls
				((NativeValue) value).setInternalID(id, revision);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("getID returning cache id {} for value={} thread={}", id, describeValue(value),
						threadName());
			}

			return id;
		}

		// ID not cached, search in file
		byte[] data = value2data(value, false);

		if (data == null && value instanceof Literal) {
			data = literal2legacy((Literal) value);
		}

		if (data != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("getID querying datastore for value={} thread={} dataSummary={}", describeValue(value),
						threadName(), summarize(data));
			}
			int id = dataStore.getID(data);

			if (id == NativeValue.UNKNOWN_ID && value instanceof Literal) {
				id = dataStore.getID(literal2legacy((Literal) value));
			}

			if (id != NativeValue.UNKNOWN_ID) {
				if (isOwnValue) {
					// Store id in value for fast access in any consecutive calls
					((NativeValue) value).setInternalID(id, revision);
				} else {
					// Store id in cache
					NativeValue nv = getNativeValue(value);
					nv.setInternalID(id, revision);
					valueIDCache.put(nv, Integer.valueOf(id));
				}

				if (logger.isDebugEnabled()) {
					logger.debug("getID resolved value={} id={} thread={}", describeValue(value), id, threadName());
				}
			}

			return id;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getID returning UNKNOWN for value={} thread={}", describeValue(value), threadName());
		}

		return NativeValue.UNKNOWN_ID;
	}

	private static String summarize(byte[] data) {
		if (data == null) {
			return "null";
		}
		return "len=" + data.length + ",hash=" + Arrays.hashCode(data);
	}

	private static String threadName() {
		return Thread.currentThread().getName();
	}

	/**
	 * Stores the supplied value and returns the ID that has been assigned to it. In case the value was already present,
	 * the value will not be stored again and the ID of the existing value is returned.
	 *
	 * @param value The Value to store.
	 * @return The ID that has been assigned to the value.
	 * @throws IOException If an I/O error occurred.
	 */
	public synchronized int storeValue(Value value) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("storeValue start thread={} value={}", threadName(), describeValue(value));
		}
		// Try to get the internal ID from the value itself
		boolean isOwnValue = isOwnValue(value);

		if (isOwnValue) {
			NativeValue nativeValue = (NativeValue) value;

			if (revisionIsCurrent(nativeValue)) {
				// Value's ID is still current
				int id = nativeValue.getInternalID();

				if (id != NativeValue.UNKNOWN_ID) {
					if (logger.isDebugEnabled()) {
						logger.debug("storeValue returning cached internal id {} for value={} thread={}", id,
								describeValue(value), threadName());
					}
					return id;
				}
			}
		}

		// ID not stored in value itself, try the ID cache
		Integer cachedID = valueIDCache.get(value);

		if (cachedID != null) {
			int id = cachedID.intValue();

			if (isOwnValue) {
				// Store id in value for fast access in any consecutive calls
				((NativeValue) value).setInternalID(id, revision);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("storeValue returning cached id {} for value={} thread={}", id, describeValue(value),
						threadName());
			}

			return id;
		}

		// Unable to get internal ID in a cheap way, just store it in the data
		// store which will handle duplicates
		byte[] valueData = value2data(value, true);

		int previousMaxID = walEnabled() ? dataStore.getMaxID() : 0;
		if (valueData == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("storeValue computed no data for value={} thread={}", describeValue(value), threadName());
			}
			return NativeValue.UNKNOWN_ID;
		}

		int id = dataStore.storeData(valueData);

		NativeValue nv = isOwnValue ? (NativeValue) value : getNativeValue(value);

		// Store id in value for fast access in any consecutive calls
		nv.setInternalID(id, revision);

		// Update cache
		valueIDCache.put(nv, id);

		if (walEnabled() && id > previousMaxID) {
			logMintedValue(id, nv);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("storeValue stored value={} assigned id={} thread={} dataSummary={}", describeValue(nv), id,
					threadName(), summarize(valueData));
		}

		return id;
	}

	/**
	 * Removes all values from the ValueStore.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public void clear() throws IOException {
		try {
			Lock writeLock = lockManager.getWriteLock();
			try {

				// Purge any existing WAL segments so a subsequent WAL recovery cannot
				// resurrect values that were present before the clear().
				if (walEnabled()) {
					try {
						wal.purgeAllSegments();
					} catch (IOException e) {
						logger.warn("Failed to purge ValueStore WAL during clear for {}", dataDir, e);
						throw e;
					}
				}

				dataStore.clear();

				valueCache.clear();
				valueIDCache.clear();
				namespaceCache.clear();
				namespaceIDCache.clear();

				setNewRevision();
			} finally {
				writeLock.release();
			}
		} catch (InterruptedException e) {
			throw new IOException("Failed to acquire write lock", e);
		}
	}

	/**
	 * Synchronizes any changes that are cached in memory to disk.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public void sync() throws IOException {
		dataStore.sync();
	}

	/**
	 * Closes the ValueStore, releasing any file references, etc. Once closed, the ValueStore can no longer be used.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	@Override
	public void close() throws IOException {
		CompletableFuture<Void> bootstrap = walBootstrapFuture;
		if (bootstrap != null) {
			try {
				bootstrap.join();
			} catch (CompletionException e) {
				Throwable cause = e.getCause() == null ? e : e.getCause();
				logger.warn("ValueStore WAL bootstrap failed during close", cause);
			} catch (CancellationException e) {
				logger.warn("ValueStore WAL bootstrap was cancelled during close");
			}
		}
		dataStore.close();
	}

	/**
	 * Checks that every value has exactly one ID.
	 *
	 * @throws IOException
	 */
	public void checkConsistency() throws SailException, IOException {
		int maxID = dataStore.getMaxID();
		for (int id = 1; id <= maxID; id++) {
			try {
				byte[] data = dataStore.getData(id);
				if (data == null || data.length == 0) {
					// Defensive guard against truncated/empty records which otherwise cause AIOOBE in isNamespaceData
					throw new SailException("Empty data array for value with id " + id);
				}
				if (isNamespaceData(data)) {
					String namespace = data2namespace(data);
					try {
						if (id == getNamespaceID(namespace, false)
								&& URI.create(namespace + "part").isAbsolute()) {
							continue;
						}
					} catch (IllegalArgumentException e) {
						// throw SailException
					}
					logger.error("Inconsistent namespace data for id {} (also id {}): {}", id,
							getNamespaceID(namespace, false), namespace);
					throw new SailException(
							"Store must be manually exported and imported to fix namespaces like " + namespace);
				} else {
					Value value = this.data2value(id, data);
					if (id != this.getID(copy(value))) {
						throw new SailException(
								"Store must be manually exported and imported to merge values like " + value);
					}
				}
			} catch (RecoveredDataException rde) {
				// Treat as a corrupt unknown value during consistency check
				Value value = new CorruptUnknownValue(revision, id, rde.getData());
				if (id != this.getID(copy(value))) {
					throw new SailException(
							"Store must be manually exported and imported to merge values like " + value);
				}
			}
		}
	}

	private Value copy(Value value) {
		if (value instanceof IRI) {
			return createIRI(value.stringValue());
		} else if (value instanceof Literal) {
			Literal lit = (Literal) value;
			if (Literals.isLanguageLiteral(lit)) {
				return createLiteral(value.stringValue(), lit.getLanguage().orElse(null));
			} else {
				return createLiteral(value.stringValue(), lit.getDatatype());
			}
		} else {
			return createBNode(value.stringValue());
		}
	}

	/**
	 * Checks if the supplied Value object is a NativeValue object that has been created by this ValueStore.
	 */
	private boolean isOwnValue(Value value) {
		return value instanceof NativeValue && ((NativeValue) value).getValueStoreRevision().getValueStore() == this;
	}

	/**
	 * Checks if the revision of the supplied value object is still current.
	 */
	private boolean revisionIsCurrent(NativeValue value) {
		return revision.equals(value.getValueStoreRevision());
	}

	private byte[] value2data(Value value, boolean create) throws IOException {
		byte[] data;
		if (value instanceof IRI) {
			data = uri2data((IRI) value, create);
		} else if (value instanceof BNode) {
			data = bnode2data((BNode) value, create);
		} else if (value instanceof Literal) {
			data = literal2data((Literal) value, create);
		} else {
			throw new IllegalArgumentException("value parameter should be a URI, BNode or Literal");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("value2data thread={} value={} create={} summary={}", threadName(), describeValue(value),
					create, summarize(data));
		}

		return data;
	}

	private byte[] uri2data(IRI uri, boolean create) throws IOException {
		int nsID = getNamespaceID(uri.getNamespace(), create);

		if (logger.isDebugEnabled()) {
			logger.debug("uri2data thread={} namespace='{}' nsId={} create={}", threadName(), uri.getNamespace(), nsID,
					create);
		}

		if (nsID == -1) {
			// Unknown namespace means unknown URI
			return null;
		}

		// Get local name in UTF-8
		byte[] localNameData = uri.getLocalName().getBytes(StandardCharsets.UTF_8);

		// Combine parts in a single byte array
		byte[] uriData = new byte[5 + localNameData.length];
		uriData[0] = URI_VALUE;
		ByteArrayUtil.putInt(nsID, uriData, 1);
		ByteArrayUtil.put(localNameData, uriData, 5);

		if (logger.isDebugEnabled()) {
			logger.debug("uri2data produced len={} summary={} thread={}", uriData.length, summarize(uriData),
					threadName());
		}

		return uriData;
	}

	private byte[] bnode2data(BNode bNode, boolean create) {
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
		if (XSD.STRING.equals(dt) || RDF.LANGSTRING.equals(dt)) {
			return literal2data(literal.getLabel(), literal.getLanguage(), null, false);
		}
		return literal2data(literal.getLabel(), literal.getLanguage(), dt, false);
	}

	private byte[] literal2data(String label, Optional<String> lang, IRI dt, boolean create)
			throws IOException, UnsupportedEncodingException {
		// Get datatype ID
		int datatypeID = NativeValue.UNKNOWN_ID;

		if (create) {
			datatypeID = storeValue(dt);
		} else if (dt != null) {
			datatypeID = getID(dt);

			if (datatypeID == NativeValue.UNKNOWN_ID) {
				// Unknown datatype means unknown literal
				return null;
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("literal2data thread={} valueLength={} langPresent={} datatype={} datatypeId={} create={}",
					threadName(), label.length(), lang.isPresent(), dt, datatypeID, create);
		}

		// Get language tag in UTF-8
		byte[] langData = null;
		int langDataLength = 0;
		if (lang.isPresent()) {
			langData = lang.get().getBytes(StandardCharsets.UTF_8);
			langDataLength = langData.length;
			if (langDataLength > 255) {
				throw new IllegalArgumentException(
						"Language tag too long (length " + langDataLength + " > maximum 255): " + lang.get());
			}
		}

		// Get label in UTF-8
		byte[] labelData = label.getBytes(StandardCharsets.UTF_8);

		// Combine parts in a single byte array
		byte[] literalData = new byte[6 + langDataLength + labelData.length];
		literalData[0] = LITERAL_VALUE;
		ByteArrayUtil.putInt(datatypeID, literalData, 1);
		literalData[5] = (byte) (langDataLength & 0xFF);
		if (langData != null) {
			ByteArrayUtil.put(langData, literalData, 6);
		}
		ByteArrayUtil.put(labelData, literalData, 6 + langDataLength);

		if (logger.isDebugEnabled()) {
			logger.debug("literal2data produced len={} summary={} thread={}", literalData.length,
					summarize(literalData), threadName());
		}

		return literalData;
	}

	private boolean isNamespaceData(byte[] data) {
		return data[0] != URI_VALUE && data[0] != BNODE_VALUE && data[0] != LITERAL_VALUE;
	}

	@InternalUseOnly
	public NativeValue data2value(int id, byte[] data) throws IOException {
		if (data.length == 0) {
			if (SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES) {
				logger.error("Soft fail on corrupt data: Empty data array for value with id {}", id);
				CorruptUnknownValue v = new CorruptUnknownValue(revision, id, data);
				tryRecoverFromWal(id, v);
				return v;
			}
			throw new SailException("Empty data array for value with id " + id
					+ " consider setting the system property org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes to true");
		}
		switch (data[0]) {
		case URI_VALUE:
			return data2uri(id, data);
		case BNODE_VALUE:
			return data2bnode(id, data);
		case LITERAL_VALUE:
			return data2literal(id, data);
		default:
			if (SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES) {
				logger.error("Soft fail on corrupt data: Invalid type {} for value with id {}", data[0], id);
				CorruptUnknownValue v = new CorruptUnknownValue(revision, id, data);
				tryRecoverFromWal(id, v);
				return v;
			}
			throw new SailException("Invalid type " + data[0] + " for value with id " + id
					+ "  consider setting the system property org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes to true");
		}
	}

	private <T extends IRI & NativeValue> T data2uri(int id, byte[] data) throws IOException {
		String namespace = null;

		try {
			int nsID = ByteArrayUtil.getInt(data, 1);
			namespace = getNamespace(nsID);

			String localName = new String(data, 5, data.length - 5, StandardCharsets.UTF_8);

			return (T) new NativeIRI(revision, namespace, localName, id);
		} catch (Throwable e) {
			if (SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES
					&& (e instanceof Exception || e instanceof AssertionError)) {
				CorruptIRI v = new CorruptIRI(revision, id, namespace, data);
				tryRecoverFromWal(id, v);
				return (T) v;
			}
			logger.warn(
					"NativeStore is possibly corrupt. To attempt to repair or retrieve the data, read the documentation on http://rdf4j.org about the system property org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes");
			throw e;
		}
	}

	private NativeBNode data2bnode(int id, byte[] data) {
		String nodeID = new String(data, 1, data.length - 1, StandardCharsets.UTF_8);
		return new NativeBNode(revision, nodeID, id);
	}

	private <T extends NativeValue & Literal> T data2literal(int id, byte[] data) throws IOException {
		try {
			// Get datatype
			int datatypeID = ByteArrayUtil.getInt(data, 1);
			IRI datatype = null;
			if (datatypeID != NativeValue.UNKNOWN_ID) {
				datatype = (IRI) getValue(datatypeID);
			}

			// Get language tag
			String lang = null;
			int langLength = data[5] & 0xFF;
			if (langLength > 0) {
				lang = new String(data, 6, langLength, StandardCharsets.UTF_8);
			}

			// Get label
			String label = new String(data, 6 + langLength, data.length - 6 - langLength, StandardCharsets.UTF_8);

			if (lang != null) {
				return (T) new NativeLiteral(revision, label, lang, id);
			} else if (datatype != null) {
				return (T) new NativeLiteral(revision, label, datatype, id);
			} else {
				return (T) new NativeLiteral(revision, label, CoreDatatype.XSD.STRING, id);
			}
		} catch (Throwable e) {
			if (SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES
					&& (e instanceof Exception || e instanceof AssertionError)) {
				CorruptLiteral v = new CorruptLiteral(revision, id, data);
				tryRecoverFromWal(id, v);
				return (T) v;
			}
			throw e;
		}

	}

	private void tryRecoverFromWal(int id, CorruptValue holder) {
		NativeValue recovered = recoverValueFromWal(id);
		if (recovered != null) {
			holder.setRecovered(recovered);
		}
	}

	private NativeValue recoverValueFromWal(int id) {
		return recoverValueFromWal(id, true);
	}

	private NativeValue recoverValueFromWal(int id, boolean log) {
		ValueStoreWalSearch search = getOrCreateWalSearch();
		if (search == null) {
			return null;
		}
		try {
			Value v = search.findValueById(id);
			if (v == null) {
				return null;
			}
			NativeValue nv = getNativeValue(v);
			if (nv != null) {
				nv.setInternalID(id, revision);
				if (log) {
					logRecovered(id, nv);
					logWalRepairHint(id);
				}
				return nv;
			}
		} catch (IOException ioe) {
			// ignore recovery failures
		}
		return null;
	}

	private ValueStoreWalSearch getOrCreateWalSearch() {
		if (wal == null) {
			return null;
		}
		ValueStoreWalSearch search = walSearch;
		if (search != null) {
			return search;
		}
		synchronized (this) {
			search = walSearch;
			if (search == null) {
				search = ValueStoreWalSearch.open(wal.config());
				walSearch = search;
			}
			return search;
		}
	}

	private boolean shouldValidateAgainstWal() {
		return walEnabled() && SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES;
	}

	private boolean valuesMatch(NativeValue storeValue, NativeValue walValue) {
		if (storeValue == walValue) {
			return true;
		}
		if (storeValue == null || walValue == null) {
			return false;
		}
		if (storeValue instanceof Literal && walValue instanceof Literal) {
			Literal a = (Literal) storeValue;
			Literal b = (Literal) walValue;
			return Objects.equals(a.getLabel(), b.getLabel())
					&& Objects.equals(a.getLanguage().orElse(null), b.getLanguage().orElse(null))
					&& Objects.equals(datatypeIri(a), datatypeIri(b));
		}
		if (storeValue instanceof IRI && walValue instanceof IRI) {
			return Objects.equals(storeValue.stringValue(), walValue.stringValue());
		}
		if (storeValue instanceof BNode && walValue instanceof BNode) {
			return Objects.equals(storeValue.stringValue(), walValue.stringValue());
		}
		return Objects.equals(storeValue.stringValue(), walValue.stringValue());
	}

	private String datatypeIri(Literal literal) {
		return literal.getDatatype() == null ? "" : literal.getDatatype().stringValue();
	}

	private void logRecovered(int id, NativeValue nv) {
		switch (WAL_RECOVERY_LOG) {
		case "trace":
			if (logger.isTraceEnabled()) {
				logger.trace("Recovered value for id {} from WAL as {}", id, nv.stringValue());
			}
			break;
		case "debug":
			if (logger.isDebugEnabled()) {
				logger.debug("Recovered value for id {} from WAL as {}", id, nv.stringValue());
			}
			break;
		default:
			// off or unknown: no-op
		}
	}

	private void logWalRepairHint(int id) {
		logger.error(
				"ValueStore {} recovered value id {} from WAL because the values.* files are corrupt. Enable NativeStore#setWalAutoRecoverOnOpen(true) (config:native.walAutoRecoverOnOpen) and restart, or run ValueStoreWalRecovery to replay the WAL and rebuild values.dat/values.id/values.hash so the on-disk data matches the WAL again.",
				dataDir, id);
	}

	private NativeValue fromWalRecord(ValueStoreWalRecord rec) {
		switch (rec.valueKind()) {
		case IRI:
			return createIRI(rec.lexical());
		case BNODE:
			return createBNode(rec.lexical());
		case LITERAL: {
			String lang = rec.language();
			String dt = rec.datatype();
			if (lang != null && !lang.isEmpty()) {
				return createLiteral(rec.lexical(), lang);
			} else if (dt != null && !dt.isEmpty()) {
				return createLiteral(rec.lexical(), createIRI(dt));
			} else {
				return createLiteral(rec.lexical());
			}
		}
		case NAMESPACE:
			// not a value; nothing to recover
			return null;
		default:
			return null;
		}
	}

	private String data2namespace(byte[] data) {
		return new String(data, StandardCharsets.UTF_8);
	}

	private int getNamespaceID(String namespace, boolean create) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("getNamespaceID thread={} namespace='{}' create={}", threadName(), namespace, create);
		}
		Integer cacheID = namespaceIDCache.get(namespace);
		if (cacheID != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("getNamespaceID cache hit namespace='{}' id={} thread={}", namespace, cacheID,
						threadName());
			}
			return cacheID;
		}

		byte[] namespaceData = namespace.getBytes(StandardCharsets.UTF_8);

		int id;
		if (create) {
			int previousMaxID = walEnabled() ? dataStore.getMaxID() : 0;
			id = dataStore.storeData(namespaceData);
			if (walEnabled() && id > previousMaxID) {
				logNamespaceMint(id, namespace);
			}
		} else {
			id = dataStore.getID(namespaceData);
		}

		if (id != -1) {
			namespaceIDCache.put(namespace, id);
			if (logger.isDebugEnabled()) {
				logger.debug("getNamespaceID resolved namespace='{}' id={} thread={}", namespace, id, threadName());
			}
		} else if (logger.isDebugEnabled()) {
			logger.debug("getNamespaceID unresolved namespace='{}' thread={}", namespace, threadName());
		}

		return id;
	}

	public OptionalLong drainPendingWalHighWaterMark() {
		if (walPendingLsn == null) {
			return OptionalLong.empty();
		}
		long lsn = walPendingLsn.get();
		if (lsn <= ValueStoreWAL.NO_LSN) {
			return OptionalLong.empty();
		}
		walPendingLsn.set(ValueStoreWAL.NO_LSN);
		return OptionalLong.of(lsn);
	}

	public void awaitWalDurable(long lsn) throws IOException {
		if (!walEnabled() || lsn <= ValueStoreWAL.NO_LSN) {
			return;
		}
		try {
			wal.awaitDurable(lsn);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while awaiting WAL durability", e);
		}
	}

	private void logMintedValue(int id, Value value) throws IOException {
		ValueStoreWalDescription description = describeValue(value);
		int hash = computeWalHash(description.kind, description.lexical, description.datatype, description.language);
		long lsn = wal.logMint(id, description.kind, description.lexical, description.datatype, description.language,
				hash);
		recordWalLsn(lsn);
	}

	private void logNamespaceMint(int id, String namespace) throws IOException {
		int hash = computeWalHash(ValueStoreWalValueKind.NAMESPACE, namespace, "", "");
		long lsn = wal.logMint(id, ValueStoreWalValueKind.NAMESPACE, namespace, "", "", hash);
		recordWalLsn(lsn);
	}

	private void maybeScheduleWalBootstrap() {
		if (!walEnabled()) {
			return;
		}
		int maxId = dataStore.getMaxID();
		if (maxId <= 0) {
			return;
		}
		boolean needsBootstrap = !wal.hasInitialSegments() || walNeedsBootstrap(maxId);
		if (!needsBootstrap) {
			return;
		}
		boolean syncBootstrap = false;
		try {
			syncBootstrap = wal.config().syncBootstrapOnOpen();
		} catch (Throwable ignore) {
			// defensive: if config not accessible, default to async
		}
		if (syncBootstrap) {
			// Perform bootstrap synchronously before allowing any further operations
			rebuildWalFromExistingValues(maxId);
		} else {
			if (walBootstrapFuture != null) {
				return;
			}
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> rebuildWalFromExistingValues(maxId));
			walBootstrapFuture = future;
			future.whenComplete((unused, throwable) -> {
				if (throwable != null) {
					logger.warn("ValueStore WAL bootstrap failed", throwable);
				}
			});
		}
	}

	private void rebuildWalFromExistingValues(int maxId) {
		try {
			for (int id = 1; id <= maxId; id++) {
				if (Thread.currentThread().isInterrupted()) {
					Thread.currentThread().interrupt();
					return;
				}
				if (wal.isClosed()) {
					return;
				}
				byte[] data;
				try {
					data = dataStore.getData(id);
				} catch (IOException e) {
					logger.warn("Failed to read value {} while rebuilding WAL", id, e);
					continue;
				}
				if (data == null) {
					continue;
				}
				try {
					if (isNamespaceData(data)) {
						String namespace = data2namespace(data);
						logNamespaceMint(id, namespace);
					} else {
						NativeValue value = data2value(id, data);
						if (value != null) {
							logMintedValue(id, value);
						}
					}
				} catch (IOException e) {
					if (wal.isClosed()) {
						return;
					}
					logger.warn("Failed to rebuild WAL entry for id {}", id, e);
				} catch (RuntimeException e) {
					logger.warn("Unexpected failure while rebuilding WAL entry for id {}", id, e);
				}
			}
			if (!wal.isClosed()) {
				OptionalLong pending = drainPendingWalHighWaterMark();
				if (pending.isPresent()) {
					awaitWalDurable(pending.getAsLong());
				}
			}
		} catch (Throwable t) {
			logger.warn("Error while rebuilding ValueStore WAL", t);
		}
	}

	private boolean walNeedsBootstrap(int maxId) {
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(wal.config())) {
			ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
			ValueStoreWalRecovery.ReplayReport report = recovery.replayWithReport(reader);
			Map<Integer, ValueStoreWalRecord> dict = report.dictionary();
			if (dict.isEmpty()) {
				return true;
			}
			if (!report.complete()) {
				return true;
			}
			for (int id = 1; id <= maxId; id++) {
				if (!dict.containsKey(id)) {
					return true;
				}
			}
			return false;
		} catch (IOException e) {
			// if we cannot inspect WAL, avoid scheduling to not interfere with normal operations
			return false;
		}
	}

	private void recordWalLsn(long lsn) {
		if (walPendingLsn == null || lsn <= ValueStoreWAL.NO_LSN) {
			return;
		}
		long current = walPendingLsn.get();
		if (lsn > current) {
			walPendingLsn.set(lsn);
		}
	}

	private ValueStoreWalDescription describeValue(Value value) {
		if (value instanceof IRI) {
			return new ValueStoreWalDescription(ValueStoreWalValueKind.IRI, value.stringValue(), "", "");
		} else if (value instanceof BNode) {
			return new ValueStoreWalDescription(ValueStoreWalValueKind.BNODE, value.stringValue(), "", "");
		} else if (value instanceof Literal) {
			Literal literal = (Literal) value;
			String lang = literal.getLanguage().orElse("");
			String datatype = literal.getDatatype() != null ? literal.getDatatype().stringValue() : "";
			return new ValueStoreWalDescription(ValueStoreWalValueKind.LITERAL, literal.getLabel(), datatype, lang);
		} else {
			throw new IllegalArgumentException("value parameter should be a URI, BNode or Literal");
		}
	}

	private int computeWalHash(ValueStoreWalValueKind kind, String lexical, String datatype, String language) {
		CRC32C crc32c = CRC32C_HOLDER.get();
		// Reset the checksum to ensure each computed hash reflects only the current value
		crc32c.reset();
		crc32c.update((byte) kind.code());
		updateCrc(crc32c, lexical);
		crc32c.update((byte) 0);
		updateCrc(crc32c, datatype);
		crc32c.update((byte) 0);
		updateCrc(crc32c, language);
		return (int) crc32c.getValue();
	}

	private void updateCrc(CRC32C crc32c, String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		crc32c.update(bytes, 0, bytes.length);
	}

	private boolean walEnabled() {
		return wal != null;
	}

	private static final ThreadLocal<CRC32C> CRC32C_HOLDER = ThreadLocal.withInitial(CRC32C::new);

	private static final class ValueStoreWalDescription {
		final ValueStoreWalValueKind kind;
		final String lexical;
		final String datatype;
		final String language;

		ValueStoreWalDescription(ValueStoreWalValueKind kind, String lexical, String datatype, String language) {
			this.kind = kind;
			this.lexical = lexical == null ? "" : lexical;
			this.datatype = datatype == null ? "" : datatype;
			this.language = language == null ? "" : language;
		}
	}

	private String getNamespace(int id) throws IOException {
		Integer cacheID = id;
		String namespace = namespaceCache.get(cacheID);

		if (namespace == null) {
			try {
				byte[] namespaceData = dataStore.getData(id);
				namespace = data2namespace(namespaceData);
			} catch (RecoveredDataException rde) {
				namespace = data2namespace(rde.getData());
			}

			namespaceCache.put(cacheID, namespace);
		}

		return namespace;
	}

	/*-------------------------------------*
	 * Methods from interface ValueFactory *
	 *-------------------------------------*/

	@Override
	public NativeIRI createIRI(String uri) {
		return new NativeIRI(revision, uri);
	}

	@Override
	public NativeIRI createIRI(String namespace, String localName) {
		return new NativeIRI(revision, namespace, localName);
	}

	@Override
	public NativeBNode createBNode(String nodeID) {
		return new NativeBNode(revision, nodeID);
	}

	@Override
	public NativeLiteral createLiteral(String value) {
		return new NativeLiteral(revision, value, CoreDatatype.XSD.STRING);
	}

	@Override
	public NativeLiteral createLiteral(String value, String language) {
		return new NativeLiteral(revision, value, language);
	}

	@Override
	public NativeLiteral createLiteral(String value, IRI datatype) {
		return new NativeLiteral(revision, value, datatype);
	}

	/*----------------------------------------------------------------------*
	 * Methods for converting model objects to NativeStore-specific objects *
	 *----------------------------------------------------------------------*/

	public NativeValue getNativeValue(Value value) {
		if (value instanceof Resource) {
			return getNativeResource((Resource) value);
		} else if (value instanceof Literal) {
			return getNativeLiteral((Literal) value);
		} else {
			throw new IllegalArgumentException("Unknown value type: " + value.getClass());
		}
	}

	public NativeResource getNativeResource(Resource resource) {
		if (resource instanceof IRI) {
			return getNativeURI((IRI) resource);
		} else if (resource instanceof BNode) {
			return getNativeBNode((BNode) resource);
		} else {
			throw new IllegalArgumentException("Unknown resource type: " + resource.getClass());
		}
	}

	/**
	 * Creates a NativeURI that is equal to the supplied URI. This method returns the supplied URI itself if it is
	 * already a NativeURI that has been created by this ValueStore, which prevents unnecessary object creations.
	 *
	 * @return A NativeURI for the specified URI.
	 */
	public NativeIRI getNativeURI(IRI uri) {
		if (isOwnValue(uri)) {
			return (NativeIRI) uri;
		}

		return new NativeIRI(revision, uri.toString());
	}

	/**
	 * Creates a NativeBNode that is equal to the supplied bnode. This method returns the supplied bnode itself if it is
	 * already a NativeBNode that has been created by this ValueStore, which prevents unnecessary object creations.
	 *
	 * @return A NativeBNode for the specified bnode.
	 */
	public NativeBNode getNativeBNode(BNode bnode) {
		if (isOwnValue(bnode)) {
			return (NativeBNode) bnode;
		}

		return new NativeBNode(revision, bnode.getID());
	}

	/**
	 * Creates an NativeLiteral that is equal to the supplied literal. This method returns the supplied literal itself
	 * if it is already a NativeLiteral that has been created by this ValueStore, which prevents unnecessary object
	 * creations.
	 *
	 * @return A NativeLiteral for the specified literal.
	 */
	public NativeLiteral getNativeLiteral(Literal l) {
		if (isOwnValue(l)) {
			return (NativeLiteral) l;
		}

		if (Literals.isLanguageLiteral(l)) {
			return new NativeLiteral(revision, l.getLabel(), l.getLanguage().get());
		} else {
			NativeIRI datatype = getNativeURI(l.getDatatype());
			return new NativeLiteral(revision, l.getLabel(), datatype);
		}
	}

	/*--------------------*
	 * Test/debug methods *
	 *--------------------*/

	public static void main(String[] args) throws Exception {
		File dataDir = new File(args[0]);
		ValueStore valueStore = new ValueStore(dataDir);

		int maxID = valueStore.dataStore.getMaxID();
		for (int id = 1; id <= maxID; id++) {
			try {
				byte[] data = valueStore.dataStore.getData(id);
				if (valueStore.isNamespaceData(data)) {
					String ns = valueStore.data2namespace(data);
					System.out.println("[" + id + "] " + ns);
				} else {
					Value value = valueStore.data2value(id, data);
					System.out.println("[" + id + "] " + value.toString());
				}
			} catch (RecoveredDataException rde) {
				System.out.println("[" + id + "] CorruptUnknownValue:"
						+ new CorruptUnknownValue(valueStore.revision, id, rde.getData()));
			}
		}
	}

	private void autoRecoverValueStoreIfConfigured() {
		if (wal == null) {
			return;
		}
		ValueStoreWalConfig config;
		try {
			config = wal.config();
		} catch (Throwable t) {
			logger.warn("ValueStore WAL configuration unavailable for {}", dataDir, t);
			return;
		}
		if (!config.recoverValueStoreOnOpen()) {
			return;
		}
		try {
			ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
			ValueStoreWalRecovery.ReplayReport report;
			try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
				report = recovery.replayWithReport(reader);
			}
			Map<Integer, ValueStoreWalRecord> dictionary = report.dictionary();
			if (dictionary.isEmpty()) {
				return;
			}
			if (!report.complete()) {
				logger.warn("Skipping ValueStore WAL recovery for {}: WAL segments incomplete", dataDir);
				return;
			}
			if (hasDictionaryGaps(dictionary)) {
				logger.warn("Skipping ValueStore WAL recovery for {}: WAL dictionary has gaps", dataDir);
				return;
			}
			if (!shouldRecoverFromWalDictionary(dictionary)) {
				return;
			}
			recoverValueStoreFromWal(dictionary);
			logAutoRecovery(dictionary.size());
		} catch (IOException e) {
			logger.warn("ValueStore WAL recovery failed for {}", dataDir, e);
		}
	}

	private boolean hasDictionaryGaps(Map<Integer, ValueStoreWalRecord> dictionary) {
		int maxId = dictionary.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
		if (maxId <= 0) {
			return false;
		}
		if (dictionary.size() == maxId) {
			return false;
		}
		for (int expected = 1; expected <= maxId; expected++) {
			if (!dictionary.containsKey(expected)) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldRecoverFromWalDictionary(Map<Integer, ValueStoreWalRecord> dictionary) {
		int maxWalId = dictionary.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
		if (maxWalId <= 0) {
			return false;
		}
		int currentMaxId = dataStore.getMaxID();
		if (currentMaxId == 0 && maxWalId > 0) {
			return true;
		}
		if (currentMaxId < maxWalId) {
			return true;
		}
		List<Integer> ids = new ArrayList<>(dictionary.keySet());
		if (ids.isEmpty()) {
			return false;
		}
		ids.sort(Integer::compareTo);
		for (Integer id : ids) {
			if (isMissingValueData(id)) {
				return true;
			}
		}
		return false;
	}

	private boolean isMissingValueData(int id) {
		if (id <= 0) {
			return false;
		}
		try {
			byte[] data = dataStore.getData(id);
			return data == null || data.length == 0;
		} catch (IOException e) {
			return true;
		}
	}

	private void recoverValueStoreFromWal(Map<Integer, ValueStoreWalRecord> dictionary) throws IOException {
		dataStore.clear();
		valueCache.clear();
		valueIDCache.clear();
		namespaceCache.clear();
		namespaceIDCache.clear();

		List<Map.Entry<Integer, ValueStoreWalRecord>> entries = dictionary.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
				.collect(Collectors.toList());

		for (Map.Entry<Integer, ValueStoreWalRecord> entry : entries) {
			ValueStoreWalRecord record = entry.getValue();
			byte[] data;
			switch (record.valueKind()) {
			case NAMESPACE:
				data = record.lexical().getBytes(StandardCharsets.UTF_8);
				break;
			case IRI:
				data = encodeIri(record.lexical(), dataStore);
				break;
			case BNODE: {
				byte[] idBytes = record.lexical().getBytes(StandardCharsets.UTF_8);
				data = new byte[1 + idBytes.length];
				data[0] = BNODE_VALUE;
				ByteArrayUtil.put(idBytes, data, 1);
				break;
			}
			case LITERAL:
				data = encodeLiteral(record.lexical(), record.datatype(), record.language(), dataStore);
				break;
			default:
				continue;
			}
			if (data == null) {
				continue;
			}
			int assigned = dataStore.storeData(data);
			if (assigned != record.id()) {
				throw new IOException("ValueStore WAL recovery produced mismatched id " + assigned
						+ " (expected " + record.id() + ")");
			}
		}
		dataStore.sync();
	}

	private void logAutoRecovery(int recoveredCount) {
		switch (WAL_RECOVERY_LOG) {
		case "trace":
			if (logger.isTraceEnabled()) {
				logger.trace("Recovered {} ValueStore entries from WAL for {}", recoveredCount, dataDir);
			}
			break;
		case "debug":
			if (logger.isDebugEnabled()) {
				logger.debug("Recovered {} ValueStore entries from WAL for {}", recoveredCount, dataDir);
			}
			break;
		default:
			// off
		}
	}

	private byte[] encodeIri(String lexical, DataStore ds) throws IOException {
		IRI iri = createIRI(lexical);
		String ns = iri.getNamespace();
		String local = iri.getLocalName();
		int nsId = ds.getID(ns.getBytes(StandardCharsets.UTF_8));
		if (nsId == -1) {
			nsId = ds.storeData(ns.getBytes(StandardCharsets.UTF_8));
		}
		byte[] localBytes = local.getBytes(StandardCharsets.UTF_8);
		byte[] data = new byte[1 + 4 + localBytes.length];
		data[0] = URI_VALUE;
		ByteArrayUtil.putInt(nsId, data, 1);
		ByteArrayUtil.put(localBytes, data, 5);
		return data;
	}

	private byte[] encodeLiteral(String label, String datatype, String language, DataStore ds) throws IOException {
		int dtId = NativeValue.UNKNOWN_ID;
		if (datatype != null && !datatype.isEmpty()) {
			byte[] dtBytes = encodeIri(datatype, ds);
			int id = ds.getID(dtBytes);
			dtId = id == -1 ? ds.storeData(dtBytes) : id;
		}
		byte[] langBytes = language == null ? new byte[0] : language.getBytes(StandardCharsets.UTF_8);
		byte[] labelBytes = label.getBytes(StandardCharsets.UTF_8);
		byte[] data = new byte[1 + 4 + 1 + langBytes.length + labelBytes.length];
		data[0] = LITERAL_VALUE;
		ByteArrayUtil.putInt(dtId, data, 1);
		data[5] = (byte) (langBytes.length & 0xFF);
		if (langBytes.length > 0) {
			ByteArrayUtil.put(langBytes, data, 6);
		}
		ByteArrayUtil.put(labelBytes, data, 6 + langBytes.length);
		return data;
	}

}
