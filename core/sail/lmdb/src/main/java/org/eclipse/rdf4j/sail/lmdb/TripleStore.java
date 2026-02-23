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
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.readTransaction;
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.transaction;
import static org.eclipse.rdf4j.sail.lmdb.Varint.readQuadUnsigned;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_FIRST;
import static org.lwjgl.util.lmdb.LMDB.MDB_KEYEXIST;
import static org.lwjgl.util.lmdb.LMDB.MDB_LAST;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOMETASYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOOVERWRITE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOSYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.MDB_PREV;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cmp;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_del;
import static org.lwjgl.util.lmdb.LMDB.mdb_drop;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_info;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_mapsize;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxdbs;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxreaders;
import static org.lwjgl.util.lmdb.LMDB.mdb_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_put;
import static org.lwjgl.util.lmdb.LMDB.mdb_stat;
import static org.lwjgl.util.lmdb.LMDB.mdb_strerror;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_commit;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Mode;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.TxnRecordCache.Record;
import org.eclipse.rdf4j.sail.lmdb.TxnRecordCache.RecordCacheIterator;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.util.GroupMatcher;
import org.eclipse.rdf4j.sail.lmdb.util.IndexKeyWriters;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBEnvInfo;
import org.lwjgl.util.lmdb.MDBStat;
import org.lwjgl.util.lmdb.MDBVal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LMDB-based indexed storage and retrieval of RDF statements. TripleStore stores statements in the form of four long
 * IDs. Each ID represent an RDF value that is stored in a {@link ValueStore}. The four IDs refer to the statement's
 * subject, predicate, object and context. The ID <tt>0</tt> is used to represent the "null" context and doesn't map to
 * an actual RDF value.
 */
@SuppressWarnings("deprecation")
class TripleStore implements Closeable {

	static ConcurrentHashMap<TripleIndex.KeyStats, TripleIndex.KeyStats> stats = new ConcurrentHashMap<>();
	static long hit = 0;
	static long fullHit = 0;
	static long miss = 0;

	/*-----------*
	 * Constants *
	 *-----------*/

	// triples are represented by 4 varints for subject, predicate, object and context
	static final int SUBJ_IDX = 0;
	static final int PRED_IDX = 1;
	static final int OBJ_IDX = 2;
	static final int CONTEXT_IDX = 3;

	static final int MAX_KEY_LENGTH = 4 * 9;

	/**
	 * The default triple indexes.
	 */
	private static final String DEFAULT_INDEXES = "spoc,posc";
	/**
	 * The file name for the properties file.
	 */
	private static final String PROPERTIES_FILE = "triples.prop";
	/**
	 * The key used to store the triple store version in the properties file.
	 */
	private static final String VERSION_KEY = "version";
	/**
	 * The key used to store the triple indexes specification that specifies which triple indexes exist.
	 */
	private static final String INDEXES_KEY = "triple-indexes";
	/**
	 * The version number for the current triple store.
	 * <ul>
	 * <li>version 1: The first version with configurable triple indexes, a context field and a properties file.
	 * </ul>
	 */
	private static final int SCHEME_VERSION = 1;
	/*-----------*
	 * Variables *
	 *-----------*/
	private static final Logger logger = LoggerFactory.getLogger(TripleStore.class);
	/**
	 * The directory that is used to store the index files.
	 */
	private final File dir;
	/**
	 * Object containing meta-data for the triple store.
	 */
	private final Properties properties;
	/**
	 * The list of triple indexes that are used to store and retrieve triples.
	 */
	private final List<TripleIndex> indexes = new ArrayList<>();
	private final ValueStore valueStore;

	private long env;
	private final int contextsDbi;
	private int pageSize;
	private final boolean autoGrow;
	private final boolean pageCardinalityEstimator;
	private long mapSize;
	private long writeTxn;
	private final TxnManager txnManager;

	private TxnRecordCache recordCache = null;

	static final Comparator<ByteBuffer> COMPARATOR = new Comparator<ByteBuffer>() {
		@Override
		public int compare(ByteBuffer b1, ByteBuffer b2) {
			int b1Len = b1.remaining();
			int b2Len = b2.remaining();
			int diff = compareRegion(b1, b1.position(), b2, b2.position(), Math.min(b1Len, b2Len));
			if (diff != 0) {
				return diff;
			}
			return b1Len > b2Len ? 1 : -1;
		}

		public int compareRegion(ByteBuffer array1, int startIdx1, ByteBuffer array2, int startIdx2, int length) {
			int result = 0;
			for (int i = 0; result == 0 && i < length; i++) {
				result = (array1.get(startIdx1 + i) & 0xff) - (array2.get(startIdx2 + i) & 0xff);
			}
			return result;
		}
	};

	TripleStore(File dir, LmdbStoreConfig config, ValueStore valueStore) throws IOException, SailException {
		this.dir = dir;
		boolean forceSync = config.getForceSync();
		this.autoGrow = config.getAutoGrow();
		this.pageCardinalityEstimator = config.getPageCardinalityEstimator();
		this.valueStore = valueStore;

		// create directory if it not exists
		this.dir.mkdirs();

		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			env = pp.get(0);
		}

		// 1 for contexts, 12 for triple indexes (2 per index)
		E(mdb_env_set_maxdbs(env, 13));
		E(mdb_env_set_maxreaders(env, 256));

		// Open environment
		int flags = MDB_NOTLS;
		if (!forceSync) {
			flags |= MDB_NOSYNC | MDB_NOMETASYNC;
		}
		E(mdb_env_open(env, this.dir.getAbsolutePath(), flags, 0664));
		// open contexts database
		contextsDbi = transaction(env, (stack, txn) -> {
			String name = "contexts";
			IntBuffer ip = stack.mallocInt(1);
			if (mdb_dbi_open(txn, name, 0, ip) == MDB_NOTFOUND) {
				E(mdb_dbi_open(txn, name, MDB_CREATE, ip));
			}
			return ip.get(0);
		});

		txnManager = new TxnManager(env, Mode.RESET);

		File propFile = new File(this.dir, PROPERTIES_FILE);
		String indexSpecStr = config.getTripleIndexes();
		if (!propFile.exists()) {
			// newly created lmdb store
			properties = new Properties();

			Set<String> indexSpecs = parseIndexSpecList(indexSpecStr);

			if (indexSpecs.isEmpty()) {
				logger.debug("No indexes specified, using default indexes: {}", DEFAULT_INDEXES);
				indexSpecStr = DEFAULT_INDEXES;
				indexSpecs = parseIndexSpecList(indexSpecStr);
			}

			initIndexes(indexSpecs, config.getTripleDBSize());
		} else {
			// Read triple properties file and check format version number
			properties = loadProperties(propFile);
			checkVersion();

			// Initialize existing indexes
			Set<String> indexSpecs = getIndexSpecs();
			initIndexes(indexSpecs, config.getTripleDBSize());

			// Compare the existing indexes with the requested indexes
			Set<String> reqIndexSpecs = parseIndexSpecList(indexSpecStr);

			if (reqIndexSpecs.isEmpty()) {
				// No indexes specified, use the existing ones
				indexSpecStr = properties.getProperty(INDEXES_KEY);
			} else if (!reqIndexSpecs.equals(indexSpecs)) {
				// Set of indexes needs to be changed
				reindex(indexSpecs, reqIndexSpecs);
			}
		}

		if (!String.valueOf(SCHEME_VERSION).equals(properties.getProperty(VERSION_KEY))
				|| !indexSpecStr.equals(properties.getProperty(INDEXES_KEY))) {
			// Store up-to-date properties
			properties.setProperty(VERSION_KEY, String.valueOf(SCHEME_VERSION));
			properties.setProperty(INDEXES_KEY, indexSpecStr);
			storeProperties(propFile);
		}
	}

	private void checkVersion() throws SailException {
		// Check version number
		String versionStr = properties.getProperty(VERSION_KEY);
		if (versionStr == null) {
			logger.warn("{} missing in TripleStore's properties file", VERSION_KEY);
		} else {
			try {
				int version = Integer.parseInt(versionStr);
				if (version > SCHEME_VERSION) {
					throw new SailException("Directory contains data that uses a newer data format");
				}
			} catch (NumberFormatException e) {
				logger.warn("Malformed version number in TripleStore's properties file");
			}
		}
	}

	private Set<String> getIndexSpecs() throws SailException {
		String indexesStr = properties.getProperty(INDEXES_KEY);

		if (indexesStr == null) {
			throw new SailException(INDEXES_KEY + " missing in TripleStore's properties file");
		}

		Set<String> indexSpecs = parseIndexSpecList(indexesStr);

		if (indexSpecs.isEmpty()) {
			throw new SailException("No " + INDEXES_KEY + " found in TripleStore's properties file");
		}

		return indexSpecs;
	}

	TxnManager getTxnManager() {
		return txnManager;
	}

	/**
	 * Parses a comma/whitespace-separated list of index specifications. Index specifications are required to consists
	 * of 4 characters: 's', 'p', 'o' and 'c'.
	 *
	 * @param indexSpecStr A string like "spoc, pocs, cosp".
	 * @return A Set containing the parsed index specifications.
	 */
	private Set<String> parseIndexSpecList(String indexSpecStr) throws SailException {
		Set<String> indexes = new HashSet<>();

		if (indexSpecStr != null) {
			StringTokenizer tok = new StringTokenizer(indexSpecStr, ", \t");
			while (tok.hasMoreTokens()) {
				String index = tok.nextToken().toLowerCase();

				// sanity checks
				if (index.length() != 4 || index.indexOf('s') == -1 || index.indexOf('p') == -1
						|| index.indexOf('o') == -1 || index.indexOf('c') == -1) {
					throw new SailException("invalid value '" + index + "' in index specification: " + indexSpecStr);
				}

				indexes.add(index);
			}
		}

		return indexes;
	}

	private void initIndexes(Set<String> indexSpecs, long tripleDbSize) throws IOException {
		for (String fieldSeq : indexSpecs) {
			logger.trace("Initializing index '{}'...", fieldSeq);
			indexes.add(new TripleIndex(fieldSeq));
		}

		// initialize page size and set map size for env
		readTransaction(env, (stack, txn) -> {
			MDBStat stat = MDBStat.malloc(stack);
			TripleIndex mainIndex = indexes.get(0);
			mdb_stat(txn, mainIndex.getDB(true), stat);

			boolean isEmpty = stat.ms_entries() == 0;
			pageSize = stat.ms_psize();
			// align map size with page size
			long configMapSize = (tripleDbSize / pageSize) * pageSize;
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

	private void reindex(Set<String> currentIndexSpecs, Set<String> newIndexSpecs) throws IOException, SailException {
		Map<String, TripleIndex> currentIndexes = new HashMap<>();
		for (TripleIndex index : indexes) {
			currentIndexes.put(new String(index.getFieldSeq()), index);
		}

		// Determine the set of newly added indexes and initialize these using an
		// existing index as source
		Set<String> addedIndexSpecs = new HashSet<>(newIndexSpecs);
		addedIndexSpecs.removeAll(currentIndexSpecs);

		if (!addedIndexSpecs.isEmpty()) {
			TripleIndex sourceIndex = indexes.get(0);
			for (boolean explicit : new boolean[] { true, false }) {
				transaction(env, (stack, txn) -> {
					MDBVal keyValue = MDBVal.callocStack(stack);
					ByteBuffer keyBuf = stack.malloc(MAX_KEY_LENGTH);
					keyValue.mv_data(keyBuf);
					MDBVal dataValue = MDBVal.callocStack(stack);
					for (String fieldSeq : addedIndexSpecs) {
						logger.debug("Initializing new index '{}'...", fieldSeq);

						TripleIndex addedIndex = new TripleIndex(fieldSeq);
						RecordIterator[] sourceIter = { null };
						try {
							sourceIter[0] = new LmdbRecordIterator(sourceIndex, false, -1, -1, -1, -1,
									explicit, txnManager.createTxn(txn));

							RecordIterator it = sourceIter[0];
							long[] quad;
							while ((quad = it.next()) != null) {
								keyBuf.clear();
								addedIndex.toKey(keyBuf, quad[SUBJ_IDX], quad[PRED_IDX], quad[OBJ_IDX],
										quad[CONTEXT_IDX]);
								keyBuf.flip();

								E(mdb_put(txn, addedIndex.getDB(explicit), keyValue, dataValue, 0));
							}
						} finally {
							if (sourceIter[0] != null) {
								sourceIter[0].close();
							}
						}

						currentIndexes.put(fieldSeq, addedIndex);
					}

					return null;
				});
			}

			logger.debug("New index(es) initialized");
		}

		// Determine the set of removed indexes
		Set<String> removedIndexSpecs = new HashSet<>(currentIndexSpecs);
		removedIndexSpecs.removeAll(newIndexSpecs);

		List<Throwable> removedIndexExceptions = new ArrayList<>();
		transaction(env, (stack, txn) -> {
			// Delete files for removed indexes
			for (String fieldSeq : removedIndexSpecs) {
				try {
					TripleIndex removedIndex = currentIndexes.remove(fieldSeq);
					removedIndex.destroy(txn);
					logger.debug("Deleted file(s) for removed {} index", fieldSeq);
				} catch (Throwable e) {
					removedIndexExceptions.add(e);
				}
			}
			return null;
		});

		if (!removedIndexExceptions.isEmpty()) {
			throw new IOException(removedIndexExceptions.get(0));
		}

		// Update the indexes variable, using the specified index order
		indexes.clear();
		for (String fieldSeq : newIndexSpecs) {
			indexes.add(currentIndexes.remove(fieldSeq));
		}
	}

	@Override
	public void close() throws IOException {
		if (env != 0) {
			endTransaction(false);

			List<Throwable> caughtExceptions = new ArrayList<>();
			for (TripleIndex index : indexes) {
				try {
					index.close();
				} catch (Throwable e) {
					logger.warn("Failed to close file for {} index", new String(index.getFieldSeq()));
					caughtExceptions.add(e);
				}
			}

			mdb_env_close(env);
			env = 0;

			if (!caughtExceptions.isEmpty()) {
				throw new IOException(caughtExceptions.get(0));
			}
		}
	}

	/**
	 * Returns an iterator of all registered contexts.
	 *
	 * @param txn Active transaction
	 * @return All registered contexts
	 * @throws IOException
	 */
	public LmdbContextIdIterator getContexts(Txn txn) throws IOException {
		return new LmdbContextIdIterator(this.contextsDbi, txn);
	}

	/**
	 * If an index exists by context - use it, otherwise return null.
	 *
	 * @return All triples sorted by context or null if no context index exists
	 * @throws IOException
	 */
	public RecordIterator getAllTriplesSortedByContext(Txn txn) throws IOException {
		for (TripleIndex index : indexes) {
			if (index.getFieldSeq()[0] == 'c') {
				// found a context-first index
				return getTriplesUsingIndex(txn, -1, -1, -1, -1, true, index, false);
			}
		}
		return null;
	}

	public RecordIterator getTriples(Txn txn, long subj, long pred, long obj, long context, boolean explicit)
			throws IOException {
		TripleIndex index = getBestIndex(subj, pred, obj, context);
		// System.out.println("get triples: " + Arrays.asList(subj, pred, obj,context));
		boolean doRangeSearch = index.getPatternScore(subj, pred, obj, context) > 0;
		return getTriplesUsingIndex(txn, subj, pred, obj, context, explicit, index, doRangeSearch);
	}

	boolean hasTriples(boolean explicit) throws IOException {
		TripleIndex mainIndex = indexes.get(0);
		return txnManager.doWith((stack, txn) -> {
			MDBStat stat = MDBStat.mallocStack(stack);
			mdb_stat(txn, mainIndex.getDB(explicit), stat);
			return stat.ms_entries() > 0;
		});
	}

	private RecordIterator getTriplesUsingIndex(Txn txn, long subj, long pred, long obj, long context,
			boolean explicit, TripleIndex index, boolean rangeSearch) throws IOException {
		return new LmdbRecordIterator(index, rangeSearch, subj, pred, obj, context, explicit, txn);
	}

	/**
	 * Computes start key for a bucket by linear interpolation between a lower and an upper bound.
	 *
	 * @param fraction    Value between 0 and 1
	 * @param lowerValues The lower bound
	 * @param upperValues The upper Bound
	 * @param startValues The interpolated values
	 */
	protected void bucketStart(double fraction, long[] lowerValues, long[] upperValues, long[] startValues) {
		long diff = 0;
		for (int i = 0; i < lowerValues.length; i++) {
			if (diff == 0) {
				// only interpolate the first value that is different
				diff = upperValues[i] - lowerValues[i];
				startValues[i] = diff == 0 ? lowerValues[i] : (long) (lowerValues[i] + diff * fraction);
			} else {
				// set rest of the values to 0
				startValues[i] = 0;
			}
		}
	}

	/**
	 * Checks if any of <code>ids</code> is used and removes it from the collection.
	 *
	 * @param ids Collection with possibly removed IDs
	 * @throws IOException
	 */
	protected void filterUsedIds(Collection<Long> ids) throws IOException {
		readTransaction(env, (stack, txn) -> {
			MDBVal maxKey = MDBVal.malloc(stack);
			ByteBuffer maxKeyBuf = stack.malloc(TripleStore.MAX_KEY_LENGTH);
			MDBVal keyData = MDBVal.malloc(stack);
			ByteBuffer keyBuf = stack.malloc(TripleStore.MAX_KEY_LENGTH);

			MDBVal valueData = MDBVal.mallocStack(stack);

			PointerBuffer pp = stack.mallocPointer(1);

			// test contexts list if it contains the id
			for (Iterator<Long> it = ids.iterator(); it.hasNext();) {
				long id = it.next();
				if (id < 0) {
					it.remove();
					continue;
				}
				keyBuf.clear();
				Varint.writeUnsigned(keyBuf, id);
				keyData.mv_data(keyBuf.flip());
				if (mdb_get(txn, contextsDbi, keyData, valueData) == MDB_SUCCESS) {
					it.remove();
				}
			}

			// TODO currently this does not test for contexts (component == 3)
			// because in most cases context indexes do not exist
			for (int component = 0; component <= 2; component++) {
				int c = component;

				TripleIndex index = getBestIndex(component == 0 ? 1 : -1, component == 1 ? 1 : -1,
						component == 2 ? 1 : -1, component == 3 ? 1 : -1);

				boolean fullScan = index.getPatternScore(component == 0 ? 1 : -1, component == 1 ? 1 : -1,
						component == 2 ? 1 : -1, component == 3 ? 1 : -1) == 0;

				for (boolean explicit : new boolean[] { true, false }) {
					int dbi = index.getDB(explicit);

					long cursor = 0;
					try {
						E(mdb_cursor_open(txn, dbi, pp));
						cursor = pp.get(0);

						if (fullScan) {
							long[] quad = new long[4];
							int rc = mdb_cursor_get(cursor, keyData, valueData, MDB_FIRST);
							while (rc == MDB_SUCCESS && !ids.isEmpty()) {
								index.keyToQuad(keyData.mv_data(), quad);
								ids.remove(quad[0]);
								ids.remove(quad[1]);
								ids.remove(quad[2]);
								ids.remove(quad[3]);

								rc = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
							}
						} else {
							for (Iterator<Long> it = ids.iterator(); it.hasNext();) {
								long id = it.next();
								if (id < 0) {
									it.remove();
									continue;
								}
								if (component != 2 && (id & 1) == 1) {
									// id is a literal and can only appear in object position
									continue;
								}

								long subj = c == 0 ? id : -1, pred = c == 1 ? id : -1,
										obj = c == 2 ? id : -1, context = c == 3 ? id : -1;

								GroupMatcher matcher = index.createMatcher(subj, pred, obj, context);

								maxKeyBuf.clear();
								index.getMaxKey(maxKeyBuf, subj, pred, obj, context);
								maxKeyBuf.flip();
								maxKey.mv_data(maxKeyBuf);

								keyBuf.clear();
								index.getMinKey(keyBuf, subj, pred, obj, context);
								keyBuf.flip();

								// set cursor to min key
								keyData.mv_data(keyBuf);
								int rc = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
								boolean exists = false;
								while (!exists && rc == MDB_SUCCESS) {
									if (mdb_cmp(txn, dbi, keyData, maxKey) > 0) {
										// id was not found
										break;
									} else if (!matcher.matches(keyData.mv_data())) {
										// value doesn't match search key/mask, fetch next value
										rc = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
									} else {
										exists = true;
									}
								}

								if (exists) {
									it.remove();
								}
							}
						}
					} finally {
						if (cursor != 0) {
							mdb_cursor_close(cursor);
						}
					}
				}
			}
			return null;
		});
	}

	protected double cardinality(long subj, long pred, long obj, long context) throws IOException {
		if (!pageCardinalityEstimator) {
			return exactCardinality(subj, pred, obj, context);
		}

		TripleIndex index = getBestIndex(subj, pred, obj, context);

		int relevantParts = index.getPatternScore(subj, pred, obj, context);
		if (relevantParts == 0) {
			// it's worthless to use the index, just retrieve all entries in the db
			return txnManager.doWith((stack, txn) -> {
				double cardinality = 0;
				for (boolean explicit : new boolean[] { true, false }) {
					int dbi = index.getDB(explicit);
					MDBStat stat = MDBStat.mallocStack(stack);
					mdb_stat(txn, dbi, stat);
					cardinality += (double) stat.ms_entries();
				}
				return cardinality;
			});
		}

		return txnManager.doWith((stack, txn) -> {
			Pool pool = Pool.get();
			final Statistics s = pool.getStatistics();
			try {
				MDBVal maxKey = MDBVal.malloc(stack);
				ByteBuffer maxKeyBuf = stack.malloc(TripleStore.MAX_KEY_LENGTH);
				index.getMaxKey(maxKeyBuf, subj, pred, obj, context);
				maxKeyBuf.flip();
				maxKey.mv_data(maxKeyBuf);

				PointerBuffer pp = stack.mallocPointer(1);

				MDBVal keyData = MDBVal.mallocStack(stack);
				ByteBuffer keyBuf = stack.malloc(TripleStore.MAX_KEY_LENGTH);
				MDBVal valueData = MDBVal.mallocStack(stack);

				double cardinality = 0;
				for (boolean explicit : new boolean[] { true, false }) {
					Arrays.fill(s.avgRowsPerValue, 1.0);
					Arrays.fill(s.avgRowsPerValueCounts, 0);

					keyBuf.clear();
					index.getMinKey(keyBuf, subj, pred, obj, context);
					keyBuf.flip();

					int dbi = index.getDB(explicit);

					int pos = 0;
					long cursor = 0;

					try {
						E(mdb_cursor_open(txn, dbi, pp));
						cursor = pp.get(0);

						// set cursor to min key
						keyData.mv_data(keyBuf);
						int rc = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
						if (rc != MDB_SUCCESS || mdb_cmp(txn, dbi, keyData, maxKey) >= 0) {
							break;
						} else {
							Varint.readListUnsigned(keyData.mv_data(), s.minValues);
						}

						// set cursor to max key
						keyData.mv_data(maxKeyBuf);
						rc = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
						if (rc != MDB_SUCCESS) {
							// directly go to last value
							rc = mdb_cursor_get(cursor, keyData, valueData, MDB_LAST);
						} else {
							// go to previous value of selected key
							rc = mdb_cursor_get(cursor, keyData, valueData, MDB_PREV);
						}
						if (rc == MDB_SUCCESS) {
							Varint.readListUnsigned(keyData.mv_data(), s.maxValues);
							// this is required to correctly estimate the range size at a later point
							s.startValues[s.MAX_BUCKETS] = s.maxValues;
						} else {
							break;
						}

						long allSamplesCount = 0;
						int bucket = 0;
						boolean endOfRange = false;
						for (; bucket < s.MAX_BUCKETS && !endOfRange; bucket++) {
							if (bucket != 0) {
								bucketStart((double) bucket / s.MAX_BUCKETS, s.minValues, s.maxValues, s.values);
								keyBuf.clear();
								Varint.writeListUnsigned(keyBuf, s.values);
								keyBuf.flip();
							}
							// this is the min key for the first iteration
							keyData.mv_data(keyBuf);

							int currentSamplesCount = 0;
							rc = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
							while (rc == MDB_SUCCESS && currentSamplesCount < s.MAX_SAMPLES_PER_BUCKET) {
								if (mdb_cmp(txn, dbi, keyData, maxKey) >= 0) {
									endOfRange = true;
									break;
								} else {
									allSamplesCount++;
									currentSamplesCount++;

									System.arraycopy(s.values, 0, s.lastValues[bucket], 0, s.values.length);
									Varint.readListUnsigned(keyData.mv_data(), s.values);

									if (currentSamplesCount == 1) {
										Arrays.fill(s.counts, 1);
										System.arraycopy(s.values, 0, s.startValues[bucket], 0, s.values.length);
									} else {
										for (int i = 0; i < s.values.length; i++) {
											if (s.values[i] == s.lastValues[bucket][i]) {
												s.counts[i]++;
											} else {
												long diff = s.values[i] - s.lastValues[bucket][i];
												s.avgRowsPerValueCounts[i]++;
												s.avgRowsPerValue[i] = (s.avgRowsPerValue[i]
														* (s.avgRowsPerValueCounts[i] - 1) +
														(double) s.counts[i] / diff) / s.avgRowsPerValueCounts[i];
												s.counts[i] = 0;
											}
										}
									}
									rc = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
									if (rc != MDB_SUCCESS) {
										// no more elements are available
										endOfRange = true;
									}
								}
							}
						}

						// at least the seen samples must be counted
						cardinality += allSamplesCount;

						// the actual number of buckets (bucket - 1 "real" buckets and one for the last element within
						// the range)
						int buckets = bucket;
						for (bucket = 1; bucket < buckets; bucket++) {
							// find first element that has been changed
							pos = 0;
							while (pos < s.lastValues[bucket].length
									&& s.startValues[bucket][pos] == s.lastValues[bucket - 1][pos]) {
								pos++;
							}
							if (pos < s.lastValues[bucket].length) {
								// this may be < 0 if two groups are overlapping
								long diffBetweenGroups = Math
										.max(s.startValues[bucket][pos] - s.lastValues[bucket - 1][pos], 0);
								// estimate number of elements between last element of previous bucket and first element
								// of current bucket
								cardinality += s.avgRowsPerValue[pos] * diffBetweenGroups;
							}
						}
					} finally {
						if (cursor != 0) {
							mdb_cursor_close(cursor);
						}
					}
				}
				return cardinality;
			} finally {
				pool.free(s);
			}
		});
	}

	private double exactCardinality(long subj, long pred, long obj, long context) throws IOException {
		return txnManager.doWith((stack, txn) -> {
			double cardinality = 0.0;
			TxnManager.Txn txnRef = txnManager.createTxn(txn);
			for (boolean explicit : new boolean[] { true, false }) {
				try (RecordIterator triples = getTriples(txnRef, subj, pred, obj, context, explicit)) {
					while (triples.next() != null) {
						cardinality++;
					}
				}
			}
			return cardinality;
		});
	}

	protected TripleIndex getBestIndex(long subj, long pred, long obj, long context) {
		int bestScore = -1;
		TripleIndex bestIndex = null;

		for (TripleIndex index : indexes) {
			int score = index.getPatternScore(subj, pred, obj, context);
			if (score > bestScore) {
				bestScore = score;
				bestIndex = index;
			}
		}

		return bestIndex;
	}

	private boolean requiresResize() {
		if (autoGrow) {
			return LmdbUtil.requiresResize(mapSize, pageSize, writeTxn, 0);
		} else {
			return false;
		}
	}

	public boolean storeTriple(long subj, long pred, long obj, long context, boolean explicit) throws IOException {
		TripleIndex mainIndex = indexes.get(0);
		boolean stAdded;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			MDBVal keyVal = MDBVal.malloc(stack);
			// use calloc to get an empty data value
			MDBVal dataVal = MDBVal.calloc(stack);
			ByteBuffer keyBuf = stack.malloc(MAX_KEY_LENGTH);
			mainIndex.toKey(keyBuf, subj, pred, obj, context);
			keyBuf.flip();
			keyVal.mv_data(keyBuf);

			if (recordCache == null) {
				if (requiresResize()) {
					// map is full, resize required
					recordCache = new TxnRecordCache(dir);
					logger.debug("resize of map size {} required while adding - initialize record cache", mapSize);
				}
			}

			if (recordCache != null) {
				long quad[] = new long[] { subj, pred, obj, context };
				if (explicit) {
					// remove implicit statement
					recordCache.removeRecord(quad, false);
				}
				// put record in cache and return immediately
				return recordCache.storeRecord(quad, explicit);
			}

			int rc = mdb_put(writeTxn, mainIndex.getDB(explicit), keyVal, dataVal, MDB_NOOVERWRITE);
			if (rc != MDB_SUCCESS && rc != MDB_KEYEXIST) {
				throw new IOException(mdb_strerror(rc));
			}
			stAdded = rc == MDB_SUCCESS;
			boolean foundImplicit = false;
			if (explicit && stAdded) {
				foundImplicit = mdb_del(writeTxn, mainIndex.getDB(false), keyVal, dataVal) == MDB_SUCCESS;
			}

			if (stAdded) {
				for (int i = 1; i < indexes.size(); i++) {

					TripleIndex index = indexes.get(i);
					keyBuf.clear();
					index.toKey(keyBuf, subj, pred, obj, context);
					keyBuf.flip();

					// update buffer positions in MDBVal
					keyVal.mv_data(keyBuf);

					if (foundImplicit) {
						E(mdb_del(writeTxn, mainIndex.getDB(false), keyVal, dataVal));
					}
					E(mdb_put(writeTxn, index.getDB(explicit), keyVal, dataVal, 0));
				}

				if (stAdded) {
					incrementContext(stack, context);
				}
			}
		}

		return stAdded;
	}

	private void incrementContext(MemoryStack stack, long context) throws IOException {
		try {
			stack.push();

			MDBVal idVal = MDBVal.calloc(stack);
			ByteBuffer bb = stack.malloc(1 + Long.BYTES);
			Varint.writeUnsigned(bb, context);
			bb.flip();
			idVal.mv_data(bb);
			MDBVal dataVal = MDBVal.calloc(stack);
			long newCount = 1;
			if (mdb_get(writeTxn, contextsDbi, idVal, dataVal) == MDB_SUCCESS) {
				// update count
				newCount = Varint.readUnsigned(dataVal.mv_data()) + 1;
			}
			// write count
			ByteBuffer countBb = stack.malloc(Varint.calcLengthUnsigned(newCount));
			Varint.writeUnsigned(countBb, newCount);
			dataVal.mv_data(countBb.flip());
			E(mdb_put(writeTxn, contextsDbi, idVal, dataVal, 0));
		} finally {
			stack.pop();
		}
	}

	private boolean decrementContext(MemoryStack stack, long context) throws IOException {
		try {
			stack.push();

			MDBVal idVal = MDBVal.calloc(stack);
			ByteBuffer bb = stack.malloc(1 + Long.BYTES);
			Varint.writeUnsigned(bb, context);
			bb.flip();
			idVal.mv_data(bb);
			MDBVal dataVal = MDBVal.calloc(stack);
			if (mdb_get(writeTxn, contextsDbi, idVal, dataVal) == MDB_SUCCESS) {
				// update count
				long newCount = Varint.readUnsigned(dataVal.mv_data()) - 1;
				if (newCount <= 0) {
					E(mdb_del(writeTxn, contextsDbi, idVal, null));
					return true;
				} else {
					// write count
					ByteBuffer countBb = stack.malloc(Varint.calcLengthUnsigned(newCount));
					Varint.writeUnsigned(countBb, newCount);
					dataVal.mv_data(countBb.flip());
					E(mdb_put(writeTxn, contextsDbi, idVal, dataVal, 0));
				}
			}
			return false;
		} finally {
			stack.pop();
		}
	}

	/**
	 * @param subj     The subject for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param pred     The predicate for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param obj      The object for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param context  The context for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param explicit Flag indicating whether explicit or inferred statements should be removed; <tt>true</tt> removes
	 *                 explicit statements that match the pattern, <tt>false</tt> removes inferred statements that match
	 *                 the pattern.
	 * @param handler  Function that gets notified about each deleted quad
	 * @throws IOException
	 */
	public void removeTriplesByContext(long subj, long pred, long obj, long context,
			boolean explicit, Consumer<long[]> handler) throws IOException {
		RecordIterator records = getTriples(txnManager.createTxn(writeTxn), subj, pred, obj, context, explicit);
		removeTriples(records, explicit, handler);
	}

	public void removeTriples(RecordIterator it, boolean explicit, Consumer<long[]> handler) throws IOException {
		try (it; MemoryStack stack = MemoryStack.stackPush()) {
			MDBVal keyValue = MDBVal.callocStack(stack);
			ByteBuffer keyBuf = stack.malloc(MAX_KEY_LENGTH);

			long[] quad;
			while ((quad = it.next()) != null) {
				if (recordCache == null) {
					if (requiresResize()) {
						// map is full, resize required
						recordCache = new TxnRecordCache(dir);
						logger.debug("resize of map size {} required while removing - initialize record cache",
								mapSize);
					}
				}
				if (recordCache != null) {
					recordCache.removeRecord(quad, explicit);
					handler.accept(quad);
					continue;
				}

				for (TripleIndex index : indexes) {
					keyBuf.clear();
					index.toKey(keyBuf, quad[SUBJ_IDX], quad[PRED_IDX], quad[OBJ_IDX], quad[CONTEXT_IDX]);
					keyBuf.flip();
					// update buffer positions in MDBVal
					keyValue.mv_data(keyBuf);

					E(mdb_del(writeTxn, index.getDB(explicit), keyValue, null));
				}

				decrementContext(stack, quad[CONTEXT_IDX]);
				handler.accept(quad);
			}
		}
	}

	protected void updateFromCache() throws IOException {
		recordCache.commit();
		for (boolean explicit : new boolean[] { true, false }) {
			RecordCacheIterator it = recordCache.getRecords(explicit);
			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer pp = stack.mallocPointer(1);
				MDBVal keyVal = MDBVal.mallocStack(stack);
				// use calloc to get an empty data value
				MDBVal dataVal = MDBVal.callocStack(stack);
				ByteBuffer keyBuf = stack.malloc(MAX_KEY_LENGTH);

				Record r;
				while ((r = it.next()) != null) {
					if (requiresResize()) {
						// resize map if required
						E(mdb_txn_commit(writeTxn));
						mapSize = LmdbUtil.autoGrowMapSize(mapSize, pageSize, 0);
						E(mdb_env_set_mapsize(env, mapSize));
						logger.debug("resized map to {}", mapSize);
						E(mdb_txn_begin(env, NULL, 0, pp));
						writeTxn = pp.get(0);
					}

					for (int i = 0; i < indexes.size(); i++) {
						TripleIndex index = indexes.get(i);
						keyBuf.clear();
						index.toKey(keyBuf, r.quad[0], r.quad[1], r.quad[2], r.quad[3]);
						keyBuf.flip();
						// update buffer positions in MDBVal
						keyVal.mv_data(keyBuf);

						if (r.add) {
							E(mdb_put(writeTxn, index.getDB(explicit), keyVal, dataVal, 0));
						} else {
							E(mdb_del(writeTxn, index.getDB(explicit), keyVal, null));
						}
					}
				}
			}
		}
		recordCache.close();
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
			try {
				if (commit) {
					try {
						E(mdb_txn_commit(writeTxn));
						if (recordCache != null) {
							StampedLongAdderLockManager lockManager = txnManager.lockManager();
							long readStamp;
							try {
								readStamp = lockManager.readLock();
							} catch (InterruptedException e) {
								throw new SailException(e);
							}
							try {
								txnManager.deactivate();
								mapSize = LmdbUtil.autoGrowMapSize(mapSize, pageSize, 0);
								E(mdb_env_set_mapsize(env, mapSize));
								logger.debug("resized map to {}", mapSize);
								// restart write transaction
								try (MemoryStack stack = stackPush()) {
									PointerBuffer pp = stack.mallocPointer(1);
									mdb_txn_begin(env, NULL, 0, pp);
									writeTxn = pp.get(0);
								}
								updateFromCache();
								// finally, commit write transaction
								E(mdb_txn_commit(writeTxn));
							} finally {
								recordCache = null;
								try {
									txnManager.activate();
								} finally {
									lockManager.unlockRead(readStamp);
								}
							}
						} else {
							// invalidate open read transaction so that they are not re-used
							// otherwise iterators won't see the updated data
							txnManager.reset();
						}
					} catch (IOException e) {
						// abort transaction if exception occurred while committing
						mdb_txn_abort(writeTxn);
						throw e;
					}
				} else {
					mdb_txn_abort(writeTxn);
				}
			} finally {
				writeTxn = 0;
				// ensure that record cache is always reset
				if (recordCache != null) {
					try {
						recordCache.close();
					} finally {
						recordCache = null;
					}
				}
			}
		}
	}

	public void commit() throws IOException {
		endTransaction(true);
	}

	public void rollback() throws IOException {
		endTransaction(false);
	}

	private Properties loadProperties(File propFile) throws IOException {
		try (InputStream in = new FileInputStream(propFile)) {
			Properties properties = new Properties();
			properties.load(in);
			return properties;
		}
	}

	private void storeProperties(File propFile) throws IOException {
		try (OutputStream out = new FileOutputStream(propFile)) {
			properties.store(out, "triple indexes meta-data, DO NOT EDIT!");
		}
	}

	class TripleIndex {

		private final char[] fieldSeq;
		private final IndexKeyWriters.KeyWriter keyWriter;
		private final IndexKeyWriters.MatcherFactory matcherFactory;
		private final int dbiExplicit, dbiInferred;
		private final int[] indexMap;

		public TripleIndex(String fieldSeq) throws IOException {
			this.fieldSeq = fieldSeq.toCharArray();
			this.keyWriter = IndexKeyWriters.forFieldSeq(fieldSeq);
			this.matcherFactory = IndexKeyWriters.matcherFactory(fieldSeq);
			this.indexMap = getIndexes(this.fieldSeq);
			// open database and use native sort order without comparator
			dbiExplicit = openDatabase(env, fieldSeq, MDB_CREATE, null);
			dbiInferred = openDatabase(env, fieldSeq + "-inf", MDB_CREATE, null);
		}

		public char[] getFieldSeq() {
			return fieldSeq;
		}

		public int getDB(boolean explicit) {
			return explicit ? dbiExplicit : dbiInferred;
		}

		protected int[] getIndexes(char[] fieldSeq) {
			int[] indexes = new int[fieldSeq.length];
			for (int i = 0; i < fieldSeq.length; i++) {
				char field = fieldSeq[i];
				int fieldIdx;
				switch (field) {
				case 's':
					fieldIdx = SUBJ_IDX;
					break;
				case 'p':
					fieldIdx = PRED_IDX;
					break;
				case 'o':
					fieldIdx = OBJ_IDX;
					break;
				case 'c':
					fieldIdx = CONTEXT_IDX;
					break;
				default:
					throw new IllegalArgumentException(
							"invalid character '" + field + "' in field sequence: " + new String(fieldSeq));
				}
				indexes[i] = fieldIdx;
			}
			return indexes;
		}

		/**
		 * Determines the 'score' of this index on the supplied pattern of subject, predicate, object and context IDs.
		 * The higher the score, the better the index is suited for matching the pattern. Lowest score is 0, which means
		 * that the index will perform a sequential scan.
		 */
		public int getPatternScore(long subj, long pred, long obj, long context) {
			int score = 0;

			for (char field : fieldSeq) {
				switch (field) {
				case 's':
					if (subj >= 0) {
						score++;
					} else {
						return score;
					}
					break;
				case 'p':
					if (pred >= 0) {
						score++;
					} else {
						return score;
					}
					break;
				case 'o':
					if (obj >= 0) {
						score++;
					} else {
						return score;
					}
					break;
				case 'c':
					if (context >= 0) {
						score++;
					} else {
						return score;
					}
					break;
				default:
					throw new RuntimeException("invalid character '" + field + "' in field sequence: "
							+ new String(fieldSeq));
				}
			}

			return score;
		}

		void getMinKey(ByteBuffer bb, long subj, long pred, long obj, long context) {
			subj = subj <= 0 ? 0 : subj;
			pred = pred <= 0 ? 0 : pred;
			obj = obj <= 0 ? 0 : obj;
			context = context <= 0 ? 0 : context;
			toKey(bb, subj, pred, obj, context);
		}

		void getMaxKey(ByteBuffer bb, long subj, long pred, long obj, long context) {
			subj = subj <= 0 ? Long.MAX_VALUE : subj;
			pred = pred <= 0 ? Long.MAX_VALUE : pred;
			obj = obj <= 0 ? Long.MAX_VALUE : obj;
			context = context < 0 ? Long.MAX_VALUE : context;
			toKey(bb, subj, pred, obj, context);
		}

		GroupMatcher createMatcher(long subj, long pred, long obj, long context) {
			int length = getLength(subj, pred, obj, context);

			ByteBuffer bb = ByteBuffer.allocate(length);
			toKey(bb, subj == -1 ? 0 : subj, pred == -1 ? 0 : pred, obj == -1 ? 0 : obj, context == -1 ? 0 : context);
			bb.flip();

			return new GroupMatcher(bb.array(), matcherFactory.create(subj, pred, obj, context));
		}

		private int getLength(long subj, long pred, long obj, long context) {
			int length = 4;
			if (subj > 240) {
				length += 8;
			}
			if (pred > 240) {
				length += 8;

			}
			if (obj > 240) {
				length += 8;

			}
			if (context > 240) {
				length += 8;

			}
			return length;
		}

		class KeyStats {
			long subj;
			long pred;
			long obj;
			long context;
			public LongAdder count = new LongAdder();

			public KeyStats(long subj, long pred, long obj, long context) {
				this.subj = subj;
				this.pred = pred;
				this.obj = obj;
				this.context = context;
			}

			@Override
			public final boolean equals(Object o) {
				if (!(o instanceof KeyStats)) {
					return false;
				}

				KeyStats keyStats = (KeyStats) o;
				return subj == keyStats.subj && pred == keyStats.pred && obj == keyStats.obj
						&& context == keyStats.context;
			}

			@Override
			public int hashCode() {
				int result = Long.hashCode(subj);
				result = 31 * result + Long.hashCode(pred);
				result = 31 * result + Long.hashCode(obj);
				result = 31 * result + Long.hashCode(context);
				return result;
			}

			public void print() {
				if (count.sum() % 1000000 == 0) {

					try {
						System.out.println("Key " + new String(getFieldSeq()) + " "
								+ Arrays.asList(valueStore.getValue(subj), valueStore.getValue(pred),
										valueStore.getValue(obj), valueStore.getValue(context))
								+ " count: " + count.sum());
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

			}
		}

		void toKey(ByteBuffer bb, long subj, long pred, long obj, long context) {

			boolean shouldCache = threeOfFourAreZeroOrMax(subj, pred, obj, context);
			if (shouldCache) {
				long sum = subj + pred + obj + context;
				if (sum == 0 && subj == pred && obj == context) {
					bb.put(Varint.ALL_ZERO_QUAD);
					return;
				}

				if (sum < 241) { // keys with sum < 241 only need 4 bytes to write and don't need caching
					shouldCache = false;
				}

			}

			// Pass through to the keyWriter with caching hint
			keyWriter.write(bb, subj, pred, obj, context, shouldCache);
		}

		void keyToQuad(ByteBuffer key, long[] quad) {
			readQuadUnsigned(key, indexMap, quad);
		}

		void keyToQuad(ByteBuffer key, long[] originalQuad, long[] quad) {
			// directly use index map to read values in to correct positions
			if (originalQuad[indexMap[0]] != -1) {
				Varint.skipUnsigned(key);
			} else {
				quad[indexMap[0]] = Varint.readUnsigned(key);
			}
			if (originalQuad[indexMap[1]] != -1) {
				Varint.skipUnsigned(key);
			} else {
				quad[indexMap[1]] = Varint.readUnsigned(key);
			}
			if (originalQuad[indexMap[2]] != -1) {
				Varint.skipUnsigned(key);
			} else {
				quad[indexMap[2]] = Varint.readUnsigned(key);
			}
			if (originalQuad[indexMap[3]] != -1) {
				Varint.skipUnsigned(key);
			} else {
				quad[indexMap[3]] = Varint.readUnsigned(key);
			}
		}

		@Override
		public String toString() {
			return new String(getFieldSeq());
		}

		void close() {
			mdb_dbi_close(env, dbiExplicit);
			mdb_dbi_close(env, dbiInferred);
		}

		void clear(long txn) {
			mdb_drop(txn, dbiExplicit, false);
			mdb_drop(txn, dbiInferred, false);
		}

		void destroy(long txn) {
			mdb_drop(txn, dbiExplicit, true);
			mdb_drop(txn, dbiInferred, true);
		}
	}

	static boolean threeOfFourAreZeroOrMax(long subj, long pred, long obj, long context) {
		// Precompute the 8 equalities once (cheapest operations here)
		boolean zS = subj == 0L, zP = pred == 0L, zO = obj == 0L, zC = context == 0L;
		boolean mS = subj == Long.MAX_VALUE, mP = pred == Long.MAX_VALUE, mO = obj == Long.MAX_VALUE,
				mC = context == Long.MAX_VALUE;

		// ≥3-of-4 ≡ ab(c∨d) ∨ cd(a∨b). Apply once for zeros and once for maxes.
		// Using '&' and '|' (not &&/||) keeps it branchless and predictable.

		return (((zS & zP & (zO | zC)) | (zO & zC & (zS | zP)))// ≥3 zeros
				| ((mS & mP & (mO | mC)) | (mO & mC & (mS | mP))));// ≥3 Long.MAX_VALUE
//				& !(zS & zP & zO & zC)    // not all zeros
//				& !(mS & mP & mO & mC);   // not all max
	}

}
