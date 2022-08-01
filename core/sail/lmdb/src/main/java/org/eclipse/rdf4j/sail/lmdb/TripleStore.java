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
import static org.eclipse.rdf4j.sail.lmdb.Varint.readListUnsigned;
import static org.eclipse.rdf4j.sail.lmdb.Varint.writeListUnsigned;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_LAST;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOMETASYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOSYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.MDB_PREV;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.mdb_cmp;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_del;
import static org.lwjgl.util.lmdb.LMDB.mdb_drop;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_info;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_mapsize;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxdbs;
import static org.lwjgl.util.lmdb.LMDB.mdb_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_put;
import static org.lwjgl.util.lmdb.LMDB.mdb_stat;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_commit;
import static org.lwjgl.util.lmdb.LMDB.nmdb_env_set_maxreaders;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.StampedLock;

import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Mode;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.TxnRecordCache.Record;
import org.eclipse.rdf4j.sail.lmdb.TxnRecordCache.RecordCacheIterator;
import org.eclipse.rdf4j.sail.lmdb.Varint.GroupMatcher;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBEnvInfo;
import org.lwjgl.util.lmdb.MDBStat;
import org.lwjgl.util.lmdb.MDBVal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LMDB-based indexed storage and retrieval of RDF statements. TripleStore stores statements in the form of four integer
 * IDs. Each ID represent an RDF value that is stored in a {@link ValueStore}. The four IDs refer to the statement's
 * subject, predicate, object and context. The ID <tt>0</tt> is used to represent the "null" context and doesn't map to
 * an actual RDF value.
 *
 */
@SuppressWarnings("deprecation")
class TripleStore implements Closeable {

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

	private long env;
	private int pageSize;
	private final boolean forceSync;
	private final boolean autoGrow;
	private long mapSize;
	private long writeTxn;
	private final TxnManager txnManager;
	private final Pool pool = new Pool();

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

	TripleStore(File dir, LmdbStoreConfig config) throws IOException, SailException {
		this.dir = dir;
		this.forceSync = config.getForceSync();
		this.autoGrow = config.getAutoGrow();

		// create directory if it not exists
		this.dir.mkdirs();

		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			env = pp.get(0);
		}

		mdb_env_set_maxdbs(env, 12);
		nmdb_env_set_maxreaders(env, 256);

		// Open environment
		int flags = MDB_NOTLS;
		if (!forceSync) {
			flags |= MDB_NOSYNC | MDB_NOMETASYNC;
		}
		E(mdb_env_open(env, this.dir.getAbsolutePath(), flags, 0664));

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
							sourceIter[0] = new LmdbRecordIterator(pool, sourceIndex, false, -1, -1, -1, -1,
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

	private RecordIterator getTriplesUsingIndex(Txn txn, long subj, long pred, long obj, long context,
			boolean explicit, TripleIndex index, boolean rangeSearch) throws IOException {
		return new LmdbRecordIterator(pool, index, rangeSearch, subj, pred, obj, context, explicit, txn);
	}

	protected double cardinality(long subj, long pred, long obj, long context) throws IOException {
		TripleIndex index = getBestIndex(subj, pred, obj, context);

		double cardinality = 0;
		for (boolean explicit : new boolean[] { true, false }) {
			int dbi = index.getDB(explicit);
			if (index.getPatternScore(subj, pred, obj, context) == 0) {
				cardinality += txnManager.doWith((stack, txn) -> {
					MDBStat stat = MDBStat.mallocStack(stack);
					mdb_stat(txn, dbi, stat);
					return (double) stat.ms_entries();
				});
			} else {
				// TODO currently uses a scan to determine range size
				long[] startValues = new long[4];
				cardinality += txnManager.doWith((stack, txn) -> {
					MDBVal maxKey = MDBVal.malloc(stack);
					ByteBuffer maxKeyBuf = stack.malloc(TripleStore.MAX_KEY_LENGTH);
					index.getMaxKey(maxKeyBuf, subj, pred, obj, context);
					maxKeyBuf.flip();
					maxKey.mv_data(maxKeyBuf);

					long cursor = 0;
					try {
						PointerBuffer pp = stack.mallocPointer(1);
						E(mdb_cursor_open(txn, dbi, pp));
						cursor = pp.get(0);

						MDBVal keyData = MDBVal.mallocStack(stack);
						ByteBuffer keyBuf = stack.malloc(TripleStore.MAX_KEY_LENGTH);
						index.getMinKey(keyBuf, subj, pred, obj, context);
						keyBuf.flip();

						// set cursor to min key
						keyData.mv_data(keyBuf);
						MDBVal valueData = MDBVal.mallocStack(stack);

						GroupMatcher matcher = index.createMatcher(subj, pred, obj, context);

						long rangeSize = 0;
						int rc = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
						while (rc == 0) {
							if (mdb_cmp(txn, dbi, keyData, maxKey) >= 0) {
								break;
							} else if (!matcher.matches(keyData.mv_data())) {
								// value doesn't match search key/mask, fetch next value
								rc = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
								continue;
							} else {
								if (rangeSize == 0) {
									Varint.readListUnsigned(keyData.mv_data(), startValues);
								}
								rangeSize++;
								rc = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
							}

							if (rangeSize == 1000) {
								long[] lastValues = new long[4];
								long[] values = new long[4];

								Varint.readListUnsigned(keyData.mv_data(), lastValues);

								keyData.mv_data(maxKeyBuf);
								rc = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
								if (rc != 0) {
									// directly go to last value
									rc = mdb_cursor_get(cursor, keyData, valueData, MDB_LAST);
								} else {
									// go to previous value of selected key
									rc = mdb_cursor_get(cursor, keyData, valueData, MDB_PREV);
								}
								if (rc == 0) {
									Varint.readListUnsigned(keyData.mv_data(), values);
									int pos = 0;
									while (pos < values.length && values[pos] == lastValues[pos]) {
										pos++;
									}
									if (pos < values.length) {
										rangeSize += (values[pos] - lastValues[pos])
												/ Math.max(1, lastValues[pos] - startValues[pos])
												* 1000;
									}
								}
								return (double) rangeSize;
							}
						}
						return (double) rangeSize;
					} finally {
						if (cursor != 0) {
							mdb_cursor_close(cursor);
						}
					}
				});
			}
		}
		return cardinality;
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
		try (MemoryStack stack = MemoryStack.stackPush()) {
			MDBVal keyVal = MDBVal.malloc(stack);
			// use calloc to get an empty data value
			MDBVal dataVal = MDBVal.calloc(stack);
			ByteBuffer keyBuf = stack.malloc(MAX_KEY_LENGTH);
			mainIndex.toKey(keyBuf, subj, pred, obj, context);
			keyBuf.flip();
			keyVal.mv_data(keyBuf);

			boolean foundExplicit = mdb_get(writeTxn, mainIndex.getDB(true), keyVal, dataVal) == 0;
			boolean foundImplicit = !foundExplicit && mdb_get(writeTxn, mainIndex.getDB(false), keyVal, dataVal) == 0;

			boolean stAdded = !(foundExplicit || foundImplicit);
			if (stAdded || explicit && foundImplicit) {
				if (recordCache == null) {
					if (requiresResize()) {
						// map is full, resize required
						recordCache = new TxnRecordCache(dir);
					}
				}
				if (recordCache != null) {
					long quad[] = new long[] { subj, pred, obj, context };
					if (explicit && foundImplicit) {
						// remove implicit statement
						recordCache.removeRecord(quad, false);
					}
					// put record in cache and return immediately
					return recordCache.storeRecord(quad, explicit);
				}

				if (explicit && foundImplicit) {
					E(mdb_del(writeTxn, mainIndex.getDB(false), keyVal, dataVal));
				}
				E(mdb_put(writeTxn, mainIndex.getDB(explicit), keyVal, dataVal, 0));

				for (int i = 1; i < indexes.size(); i++) {
					TripleIndex index = indexes.get(i);
					keyBuf.clear();
					index.toKey(keyBuf, subj, pred, obj, context);
					keyBuf.flip();

					// update buffer positions in MDBVal
					keyVal.mv_data(keyBuf);

					if (explicit && foundImplicit) {
						E(mdb_del(writeTxn, mainIndex.getDB(false), keyVal, dataVal));
					}
					E(mdb_put(writeTxn, index.getDB(explicit), keyVal, dataVal, 0));
				}
			}

			return stAdded;
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
	 * @return A mapping of each modified context to the number of statements removed in that context.
	 * @throws IOException
	 */
	public Map<Long, Long> removeTriplesByContext(long subj, long pred, long obj, long context,
			boolean explicit) throws IOException {
		RecordIterator records = getTriples(txnManager.createTxn(writeTxn), subj, pred, obj, context, explicit);
		return removeTriples(records, explicit);
	}

	private Map<Long, Long> removeTriples(RecordIterator iter, boolean explicit) throws IOException {
		final Map<Long, Long> perContextCounts = new HashMap<>();

		try (iter; MemoryStack stack = MemoryStack.stackPush()) {
			MDBVal keyValue = MDBVal.callocStack(stack);
			ByteBuffer keyBuf = stack.malloc(MAX_KEY_LENGTH);

			long[] quad;
			while ((quad = iter.next()) != null) {
				if (recordCache == null) {
					if (requiresResize()) {
						// map is full, resize required
						recordCache = new TxnRecordCache(dir);
					}
				}
				if (recordCache != null) {
					recordCache.removeRecord(quad, explicit);
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

				perContextCounts.merge(quad[CONTEXT_IDX], 1L, Long::sum);
			}
		}

		return perContextCounts;
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
			if (commit) {
				E(mdb_txn_commit(writeTxn));
				if (recordCache != null) {
					StampedLock lock = txnManager.lock();
					long stamp = lock.writeLock();
					try {
						txnManager.deactivate();
						mapSize = LmdbUtil.autoGrowMapSize(mapSize, pageSize, 0);
						E(mdb_env_set_mapsize(env, mapSize));
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
							lock.unlockWrite(stamp);
						}
					}
				} else {
					// invalidate open read transaction so that they are not re-used
					// otherwise iterators won't see the updated data
					txnManager.reset();
				}
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
		private final int dbiExplicit, dbiInferred;
		private final int[] indexMap;

		public TripleIndex(String fieldSeq) throws IOException {
			this.fieldSeq = fieldSeq.toCharArray();
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
			ByteBuffer bb = ByteBuffer.allocate(TripleStore.MAX_KEY_LENGTH);
			toKey(bb, subj == -1 ? 0 : subj, pred == -1 ? 0 : pred, obj == -1 ? 0 : obj, context == -1 ? 0 : context);
			bb.flip();

			boolean[] shouldMatch = new boolean[4];
			for (int i = 0; i < fieldSeq.length; i++) {
				switch (fieldSeq[i]) {
				case 's':
					shouldMatch[i] = subj > 0;
					break;
				case 'p':
					shouldMatch[i] = pred > 0;
					break;
				case 'o':
					shouldMatch[i] = obj > 0;
					break;
				case 'c':
					shouldMatch[i] = context >= 0;
					break;
				}
			}
			return new GroupMatcher(bb, shouldMatch);
		}

		void toKey(ByteBuffer bb, long subj, long pred, long obj, long context) {
			long[] values = new long[4];
			for (int i = 0; i < fieldSeq.length; i++) {
				switch (fieldSeq[i]) {
				case 's':
					values[i] = subj;
					break;
				case 'p':
					values[i] = pred;
					break;
				case 'o':
					values[i] = obj;
					break;
				case 'c':
					values[i] = context;
					break;
				}
			}
			writeListUnsigned(bb, values);
		}

		void keyToQuad(ByteBuffer key, long[] quad) {
			// directly use index map to read values in to correct positions
			readListUnsigned(key, indexMap, quad);
		}

		@Override
		public String toString() {
			return new String(getFieldSeq());
		}

		void close() {
			mdb_dbi_close(env, dbiExplicit);
			mdb_dbi_close(env, dbiInferred);
			pool.close();
		}

		void clear(long txn) throws IOException {
			mdb_drop(txn, dbiExplicit, false);
			mdb_drop(txn, dbiInferred, false);
		}

		void destroy(long txn) throws IOException {
			mdb_drop(txn, dbiExplicit, true);
			mdb_drop(txn, dbiInferred, true);
		}
	}
}
