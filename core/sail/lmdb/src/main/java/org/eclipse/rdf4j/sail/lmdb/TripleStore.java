/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.openDatabase;
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.transaction;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOMETASYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOSYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.MDB_RDONLY;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_del;
import static org.lwjgl.util.lmdb.LMDB.mdb_drop;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_mapsize;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxdbs;
import static org.lwjgl.util.lmdb.LMDB.mdb_put;
import static org.lwjgl.util.lmdb.LMDB.mdb_stat;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TxnRef.Mode;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
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
 * @author Arjohn Kampman
 * @author Ken Wenzel
 */
@SuppressWarnings("deprecation")
class TripleStore implements Closeable {

	/*-----------*
	 * Constants *
	 *-----------*/

	// 17 bytes are used to represent a triple:
	// byte 0-3 : subject
	// byte 4-7 : predicate
	// byte 8-11: object
	// byte 12-15: context
	// byte 16: additional flag(s)
	static final int RECORD_LENGTH = 17;
	static final int SUBJ_IDX = 0;
	static final int PRED_IDX = 4;
	static final int OBJ_IDX = 8;
	static final int CONTEXT_IDX = 12;
	static final int FLAG_IDX = 16;
	/**
	 * Bit field indicating that a statement has been explicitly added (instead of being inferred).
	 */
	static final byte EXPLICIT_FLAG = (byte) 0x1; // 0000 0001
	/**
	 * Bit field indicating that a statement has been added in a (currently active) transaction.
	 */
	static final byte ADDED_FLAG = (byte) 0x2; // 0000 0010
	/**
	 * Bit field indicating that a statement has been removed in a (currently active) transaction.
	 */
	static final byte REMOVED_FLAG = (byte) 0x4; // 0000 0100
	/**
	 * Bit field indicating that the explicit flag has been toggled (from true to false, or vice versa) in a (currently
	 * active) transaction.
	 */
	static final byte TOGGLE_EXPLICIT_FLAG = (byte) 0x8; // 0000 1000
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
	 * <li>version 0: The first version which used a single spo-index. This version did not have a properties file yet.
	 * <li>version 1: Introduces configurable triple indexes and the properties file.
	 * <li>version 10: Introduces a context field, essentially making this a quad store.
	 * <li>version 10a: Introduces transaction flags, this is backwards compatible with version 10.
	 * </ul>
	 */
	private static final int SCHEME_VERSION = 10;

	/*-----------*
	 * Variables *
	 *-----------*/
	private static final Logger logger = LoggerFactory.getLogger(TripleStore.class);
	private static final byte[] EMPTY = new byte[0];
	/**
	 * The directory that is used to store the index files.
	 */
	private final File dir;
	/**
	 * Object containing meta-data for the triple store. This includes
	 */
	private final Properties properties;
	/**
	 * The list of triple indexes that are used to store and retrieve triples.
	 */
	private final List<TripleIndex> indexes = new ArrayList<>();
	private final boolean forceSync;

	private long env;
	private long writeTxn = 0;

	public TripleStore(File dir, String indexSpecStr) throws IOException, SailException {
		this(dir, indexSpecStr, false);
	}

	public TripleStore(File dir, String indexSpecStr, boolean forceSync) throws IOException, SailException {
		this.dir = new File(dir, "triples");
		this.forceSync = forceSync;

		this.dir.mkdirs();
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			env = pp.get(0);
		}

		// 1 TB for 64-Bit systems
		// mdb_env_set_mapsize(env, 1_099_511_627_776L);
		mdb_env_set_mapsize(env, 1_099_511_627L);
		mdb_env_set_maxdbs(env, 6);

		// Open environment
		E(mdb_env_open(env, dir.getPath(), MDB_NOTLS | MDB_NOSYNC | MDB_NOMETASYNC, 0664));

		File propFile = new File(dir, PROPERTIES_FILE);

		if (!propFile.exists()) {
			// newly created lmdb store
			properties = new Properties();

			Set<String> indexSpecs = parseIndexSpecList(indexSpecStr);

			if (indexSpecs.isEmpty()) {
				logger.debug("No indexes specified, using default indexes: {}", DEFAULT_INDEXES);
				indexSpecStr = DEFAULT_INDEXES;
				indexSpecs = parseIndexSpecList(indexSpecStr);
			}

			initIndexes(indexSpecs);
		} else {
			// Read triple properties file and check format version number
			properties = loadProperties(propFile);
			checkVersion();

			// Initialize existing indexes
			Set<String> indexSpecs = getIndexSpecs();
			initIndexes(indexSpecs);

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
				if (version < 10) {
					throw new SailException("Directory contains incompatible triple data");
				} else if (version > SCHEME_VERSION) {
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

	private void initIndexes(Set<String> indexSpecs) throws IOException {
		for (String fieldSeq : indexSpecs) {
			logger.trace("Initializing index '{}'...", fieldSeq);
			indexes.add(new TripleIndex(fieldSeq));
		}
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
			transaction(env, (stack, txn) -> {
				MDBVal keyValue = MDBVal.callocStack(stack);
				keyValue.mv_data(stack.malloc(RECORD_LENGTH));

				MDBVal dataValue = MDBVal.callocStack(stack);
				dataValue.mv_data(stack.bytes());

				for (String fieldSeq : addedIndexSpecs) {
					logger.debug("Initializing new index '{}'...", fieldSeq);

					TripleIndex addedIndex = new TripleIndex(fieldSeq);
					RecordIterator[] sourceIter = { null };
					try {
						sourceIter[0] = new LmdbRecordIterator(sourceIndex.getDB(), new TxnRef(txn, Mode.NONE));

						RecordIterator it = sourceIter[0];
						byte[] value;
						while ((value = it.next()) != null) {
							ByteBuffer keyBuf = keyValue.mv_data();
							keyBuf.rewind();
							keyBuf.put(value);
							keyBuf.flip();

							mdb_put(txn, addedIndex.getDB(), keyValue, dataValue, 0);
						}
					} finally {
						try {
							if (sourceIter[0] != null) {
								sourceIter[0].close();
							}
						} finally {
							// if (addedDB != null) {
							// addedDB.sync();
							// }
						}
					}

					currentIndexes.put(fieldSeq, addedIndex);
				}

				return null;
			});

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
	 * @param readTransaction
	 * @return All triples sorted by context or null if no context index exists
	 * @throws IOException
	 */
	public RecordIterator getAllTriplesSortedByContext(boolean readTransaction) throws IOException {
		if (readTransaction) {
			// Don't read removed statements
			return getAllTriplesSortedByContext(0, TripleStore.REMOVED_FLAG);
		} else {
			// Don't read added statements
			return getAllTriplesSortedByContext(0, TripleStore.ADDED_FLAG);
		}
	}

	public RecordIterator getTriples(int subj, int pred, int obj, int context, boolean explicit,
			boolean readTransaction) throws IOException {
		int flags = 0;
		int flagsMask = 0;

		RecordIterator btreeIter = getTriples(subj, pred, obj, context, flags, flagsMask);

		if (readTransaction && explicit) {
			// Filter implicit statements from the result
			btreeIter = new ExplicitStatementFilter(btreeIter);
		} else if (!explicit) {
			// Filter out explicit statements from the result
			btreeIter = new ImplicitStatementFilter(btreeIter);
		}

		return btreeIter;
	}

	private RecordIterator getTriples(int subj, int pred, int obj, int context, int flags, int flagsMask)
			throws IOException {
		TripleIndex index = getBestIndex(subj, pred, obj, context);
		boolean doRangeSearch = index.getPatternScore(subj, pred, obj, context) > 0;
		return getTriplesUsingIndex(subj, pred, obj, context, flags, flagsMask, index, doRangeSearch);
	}

	private RecordIterator getAllTriplesSortedByContext(int flags, int flagsMask) throws IOException {
		for (TripleIndex index : indexes) {
			if (index.getFieldSeq()[0] == 'c') {
				// found a context-first index
				return getTriplesUsingIndex(-1, -1, -1, -1, flags, flagsMask, index, false);
			}
		}

		return null;
	}

	private RecordIterator getTriplesUsingIndex(int subj, int pred, int obj, int context, int flags, int flagsMask,
			TripleIndex index, boolean rangeSearch) {
		byte[] searchKey = getSearchKey(subj, pred, obj, context, flags);
		byte[] searchMask = getSearchMask(subj, pred, obj, context, flagsMask);

		TxnRef txnRef = getReadTxn();
		txnRef.begin();
		if (rangeSearch) {
			// Use ranged search
			byte[] minValue = getMinValue(subj, pred, obj, context);
			byte[] maxValue = getMaxValue(subj, pred, obj, context);

			return new LmdbRecordIterator(minValue, maxValue, index.tripleComparator, searchKey, searchMask,
					index.getDB(), txnRef);
		} else {
			// Use sequential scan
			return new LmdbRecordIterator(null, null, null, searchKey, searchMask, index.getDB(), txnRef);
		}
	}

	protected double cardinality(int subj, int pred, int obj, int context) throws IOException {
		TripleIndex index = getBestIndex(subj, pred, obj, context);

		if (index.getPatternScore(subj, pred, obj, context) == 0) {
			return LmdbUtil.<Long>readTransaction(env, (stack, txn) -> {
				MDBStat stat = MDBStat.callocStack(stack);
				mdb_stat(txn, index.getDB(), stat);
				return stat.ms_entries();
			});
		} else {
			// TODO currently uses a scan to determine range size
			byte[] minValue = getMinValue(subj, pred, obj, context);
			byte[] maxValue = getMaxValue(subj, pred, obj, context);
			ByteBuffer maxValueBuffer = ByteBuffer.wrap(maxValue);
			return LmdbUtil.<Long>readTransaction(env, (stack, txn) -> {
				long cursor = 0;
				try {
					PointerBuffer pp = stack.mallocPointer(1);
					E(mdb_cursor_open(txn, index.getDB(), pp));
					cursor = pp.get(0);

					MDBVal keyData = MDBVal.callocStack(stack);
					// set cursor to min key
					keyData.mv_data(stack.bytes(minValue));
					MDBVal valueData = MDBVal.callocStack(stack);
					long rangeSize = 0;
					int rc = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
					if (rc == 0) {
						rangeSize += 1;
						while (mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT) == 0) {
							if (index.tripleComparator.compare(keyData.mv_data(), maxValueBuffer) >= 0) {
								break;
							}
							rangeSize += 1;
						}
					}

					return rangeSize;
				} finally {
					if (cursor != 0) {
						mdb_cursor_close(cursor);
					}
				}
			});
		}
	}

	protected TripleIndex getBestIndex(int subj, int pred, int obj, int context) {
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

	/*
	 * public void clear() throws IOException { for (TripleIndex index : indexes) { index.clear(); } }
	 */

	public boolean storeTriple(int subj, int pred, int obj, int context) throws IOException {
		return storeTriple(subj, pred, obj, context, true);
	}

	public boolean storeTriple(int subj, int pred, int obj, int context, boolean explicit) throws IOException {
		byte[] data = getData(subj, pred, obj, context, 0);
		byte[] storedData = null;

		TripleIndex mainIndex = indexes.get(0);
		long cursor = 0;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			try {
				PointerBuffer pp = stack.mallocPointer(1);
				E(mdb_cursor_open(writeTxn, mainIndex.getDB(), pp));
				cursor = pp.get(0);

				MDBVal keyVal = MDBVal.callocStack(stack), dataVal = MDBVal.callocStack(stack);
				keyVal.mv_data(stack.bytes(data));

				if (mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET) == 0) {
					storedData = new byte[keyVal.mv_data().remaining()];
					keyVal.mv_data().get(storedData);
				}
			} finally {
				if (cursor != 0) {
					mdb_cursor_close(cursor);
				}
			}
		}

		if (explicit) {
			data[FLAG_IDX] |= EXPLICIT_FLAG;
		}

		boolean stAdded = storedData == null;

		if (storedData == null || !Arrays.equals(data, storedData)) {
			try (MemoryStack stack = MemoryStack.stackPush()) {
				MDBVal keyValue = MDBVal.callocStack(stack);
				keyValue.mv_data(stack.bytes(data));

				MDBVal dataValue = MDBVal.callocStack(stack);
				dataValue.mv_data(stack.bytes());

				for (TripleIndex index : indexes) {
					mdb_put(writeTxn, index.getDB(), keyValue, dataValue, 0);
				}
			}
		}

		return stAdded;
	}

	/**
	 * Remove triples
	 *
	 * @param subj    The subject for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param pred    The predicate for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param obj     The object for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param context The context for the pattern, or <tt>-1</tt> for a wildcard.
	 * @return The number of triples that were removed.
	 * @throws IOException
	 * @deprecated since 2.5.3. use {@link #removeTriplesByContext(int, int, int, int)} instead.
	 */
	@Deprecated
	public int removeTriples(int subj, int pred, int obj, int context) throws IOException {
		Map<Integer, Long> countPerContext = removeTriplesByContext(subj, pred, obj, context);
		return (int) countPerContext.values().stream().mapToLong(Long::longValue).sum();
	}

	/**
	 * Remove triples by context
	 *
	 * @param subj    The subject for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param pred    The predicate for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param obj     The object for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param context The context for the pattern, or <tt>-1</tt> for a wildcard.
	 * @return A mapping of each modified context to the number of statements removed in that context.
	 * @throws IOException
	 * @since 2.5.3
	 */
	public Map<Integer, Long> removeTriplesByContext(int subj, int pred, int obj, int context) throws IOException {
		RecordIterator iter = getTriples(subj, pred, obj, context, 0, 0);
		return removeTriples(iter);
	}

	/**
	 * Remove triples
	 *
	 * @param subj     The subject for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param pred     The predicate for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param obj      The object for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param context  The context for the pattern, or <tt>-1</tt> for a wildcard.
	 * @param explicit Flag indicating whether explicit or inferred statements should be removed; <tt>true</tt> removes
	 *                 explicit statements that match the pattern, <tt>false</tt> removes inferred statements that match
	 *                 the pattern.
	 * @return The number of triples that were removed.
	 * @throws IOException
	 * @deprecated since 2.5.3. use {@link #removeTriplesByContext(int, int, int, int, boolean)} instead.
	 */
	@Deprecated
	public int removeTriples(int subj, int pred, int obj, int context, boolean explicit) throws IOException {
		Map<Integer, Long> countPerContext = removeTriplesByContext(subj, pred, obj, context, explicit);
		return (int) countPerContext.values().stream().mapToLong(Long::longValue).sum();
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
	public Map<Integer, Long> removeTriplesByContext(int subj, int pred, int obj, int context, boolean explicit)
			throws IOException {
		byte flags = explicit ? EXPLICIT_FLAG : 0;
		RecordIterator iter = getTriples(subj, pred, obj, context, flags, EXPLICIT_FLAG);
		return removeTriples(iter);
	}

	private Map<Integer, Long> removeTriples(RecordIterator iter) throws IOException {
		final Map<Integer, Long> perContextCounts = new HashMap<>();

		try (MemoryStack stack = MemoryStack.stackPush()) {
			MDBVal keyValue = MDBVal.callocStack(stack);
			keyValue.mv_data(stack.malloc(RECORD_LENGTH));

			byte[] data;
			while ((data = iter.next()) != null) {
				ByteBuffer keyBuf = keyValue.mv_data();
				keyBuf.rewind();
				keyBuf.put(data);
				keyBuf.flip();
				for (TripleIndex index : indexes) {
					mdb_del(writeTxn, index.getDB(), keyValue, null);
				}

				int context = ByteArrayUtil.getInt(data, CONTEXT_IDX);
				perContextCounts.merge(context, 1L, (c, one) -> c + one);
			}
		} finally {
			iter.close();
		}

		return perContextCounts;
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
				mdb_txn_commit(writeTxn);
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

	private byte[] getData(int subj, int pred, int obj, int context, int flags) {
		byte[] data = new byte[RECORD_LENGTH];

		ByteArrayUtil.putInt(subj, data, SUBJ_IDX);
		ByteArrayUtil.putInt(pred, data, PRED_IDX);
		ByteArrayUtil.putInt(obj, data, OBJ_IDX);
		ByteArrayUtil.putInt(context, data, CONTEXT_IDX);
		data[FLAG_IDX] = (byte) flags;

		return data;
	}

	private byte[] getSearchKey(int subj, int pred, int obj, int context, int flags) {
		return getData(subj, pred, obj, context, flags);
	}

	private byte[] getSearchMask(int subj, int pred, int obj, int context, int flags) {
		byte[] mask = new byte[RECORD_LENGTH];

		if (subj != -1) {
			ByteArrayUtil.putInt(0xffffffff, mask, SUBJ_IDX);
		}
		if (pred != -1) {
			ByteArrayUtil.putInt(0xffffffff, mask, PRED_IDX);
		}
		if (obj != -1) {
			ByteArrayUtil.putInt(0xffffffff, mask, OBJ_IDX);
		}
		if (context != -1) {
			ByteArrayUtil.putInt(0xffffffff, mask, CONTEXT_IDX);
		}
		mask[FLAG_IDX] = (byte) flags;

		return mask;
	}

	private byte[] getMinValue(int subj, int pred, int obj, int context) {
		byte[] minValue = new byte[RECORD_LENGTH];

		ByteArrayUtil.putInt((subj == -1 ? 0x00000000 : subj), minValue, SUBJ_IDX);
		ByteArrayUtil.putInt((pred == -1 ? 0x00000000 : pred), minValue, PRED_IDX);
		ByteArrayUtil.putInt((obj == -1 ? 0x00000000 : obj), minValue, OBJ_IDX);
		ByteArrayUtil.putInt((context == -1 ? 0x00000000 : context), minValue, CONTEXT_IDX);
		minValue[FLAG_IDX] = (byte) 0;

		return minValue;
	}

	private byte[] getMaxValue(int subj, int pred, int obj, int context) {
		byte[] maxValue = new byte[RECORD_LENGTH];

		ByteArrayUtil.putInt((subj == -1 ? 0xffffffff : subj), maxValue, SUBJ_IDX);
		ByteArrayUtil.putInt((pred == -1 ? 0xffffffff : pred), maxValue, PRED_IDX);
		ByteArrayUtil.putInt((obj == -1 ? 0xffffffff : obj), maxValue, OBJ_IDX);
		ByteArrayUtil.putInt((context == -1 ? 0xffffffff : context), maxValue, CONTEXT_IDX);
		maxValue[FLAG_IDX] = (byte) 0xff;

		return maxValue;
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

	public TxnRef getReadTxn() {
		// TODO check if an already existing write txn should be used here if available
		long readTxn;
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);

			E(mdb_txn_begin(env, NULL, MDB_RDONLY, pp));
			readTxn = pp.get(0);
		}
		return new TxnRef(readTxn, Mode.ABORT);
	}

	private static class ExplicitStatementFilter implements RecordIterator {

		private final RecordIterator wrappedIter;

		public ExplicitStatementFilter(RecordIterator wrappedIter) {
			this.wrappedIter = wrappedIter;
		}

		@Override
		public byte[] next() throws IOException {
			byte[] result;

			while ((result = wrappedIter.next()) != null) {
				byte flags = result[TripleStore.FLAG_IDX];
				boolean explicit = (flags & TripleStore.EXPLICIT_FLAG) != 0;
				boolean toggled = (flags & TripleStore.TOGGLE_EXPLICIT_FLAG) != 0;

				if (explicit != toggled) {
					// Statement is either explicit and hasn't been toggled, or vice
					// versa
					break;
				}
			}

			return result;
		}

		@Override
		public void close() throws IOException {
			wrappedIter.close();
		}
	}

	private static class ImplicitStatementFilter implements RecordIterator {

		private final RecordIterator wrappedIter;

		public ImplicitStatementFilter(RecordIterator wrappedIter) {
			this.wrappedIter = wrappedIter;
		}

		@Override
		public byte[] next() throws IOException {
			byte[] result;

			while ((result = wrappedIter.next()) != null) {
				byte flags = result[TripleStore.FLAG_IDX];
				boolean explicit = (flags & TripleStore.EXPLICIT_FLAG) != 0;

				if (!explicit) {
					// Statement is implicit
					break;
				}
			}

			return result;
		}

		@Override
		public void close() throws IOException {
			wrappedIter.close();
		}
	}

	/**
	 * A DBComparator that can be used to create indexes with a configurable order of the subject, predicate, object and
	 * context fields.
	 */
	private static class TripleComparator implements Comparator<ByteBuffer> {

		private final char[] fieldSeq;

		public TripleComparator(String fieldSeq) {
			this.fieldSeq = fieldSeq.toCharArray();
		}

		public char[] getFieldSeq() {
			return fieldSeq;
		}

		public int compare(ByteBuffer key1, ByteBuffer key2) {
			for (char field : fieldSeq) {
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

				int diff = compareRegion(key1, fieldIdx, key2, fieldIdx, 4);

				if (diff != 0) {
					return diff;
				}
			}

			return 0;
		}

		int compareRegion(ByteBuffer array1, int startIdx1, ByteBuffer array2, int startIdx2, int length) {
			int result = 0;
			for (int i = 0; result == 0 && i < length; i++) {
				result = (array1.get(startIdx1 + i) & 0xff) - (array2.get(startIdx2 + i) & 0xff);
			}
			return result;
		}
	}

	private class TripleIndex {

		private final TripleComparator tripleComparator;
		private final String fieldSeq;
		private int dbi;

		public TripleIndex(String fieldSeq) throws IOException {
			this.fieldSeq = fieldSeq;
			this.tripleComparator = new TripleComparator(fieldSeq);
			open();
		}

		private void open() throws IOException {
			// Open database
			dbi = openDatabase(env, fieldSeq, MDB_CREATE, tripleComparator);
		}

		private String getFilenamePrefix(String fieldSeq) {
			return "triples-" + fieldSeq;
		}

		public char[] getFieldSeq() {
			return tripleComparator.getFieldSeq();
		}

		public int getDB() {
			return dbi;
		}

		/**
		 * Determines the 'score' of this index on the supplied pattern of subject, predicate, object and context IDs.
		 * The higher the score, the better the index is suited for matching the pattern. Lowest score is 0, which means
		 * that the index will perform a sequential scan.
		 */
		public int getPatternScore(int subj, int pred, int obj, int context) {
			int score = 0;

			for (char field : tripleComparator.getFieldSeq()) {
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
							+ new String(tripleComparator.getFieldSeq()));
				}
			}

			return score;
		}

		@Override
		public String toString() {
			return new String(getFieldSeq());
		}

		void close() {
			mdb_dbi_close(env, dbi);
		}

		void clear(long txn) throws IOException {
			mdb_drop(txn, dbi, false);
		}

		void destroy(long txn) throws IOException {
			mdb_drop(txn, dbi, true);
		}
	}
}