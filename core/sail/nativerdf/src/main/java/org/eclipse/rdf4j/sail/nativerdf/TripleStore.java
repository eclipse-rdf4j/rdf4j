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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.nativerdf.TxnStatusFile.TxnStatus;
import org.eclipse.rdf4j.sail.nativerdf.btree.BTree;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordComparator;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File-based indexed storage and retrieval of RDF statements. TripleStore stores statements in the form of four integer
 * IDs. Each ID represent an RDF value that is stored in a {@link ValueStore}. The four IDs refer to the statement's
 * subject, predicate, object and context. The ID <var>0</var> is used to represent the "null" context and doesn't map
 * to an actual RDF value.
 *
 * @author Arjohn Kampman
 */
@SuppressWarnings("deprecation")
class TripleStore implements Closeable {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final int CHECK_MEMORY_PRESSURE_INTERVAL = isAssertionsEnabled() ? 3 : 1024;
	private static final long MIN_FREE_MEMORY_BEFORE_OVERFLOW = isAssertionsEnabled() ? Long.MAX_VALUE
			: 1024 * 1024 * 128;

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

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final Logger logger = LoggerFactory.getLogger(TripleStore.class);

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

	private final TxnStatusFile txnStatusFile;

	private volatile SortedRecordCache updatedTriplesCache;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public TripleStore(File dir, String indexSpecStr) throws IOException, SailException {
		this(dir, indexSpecStr, false, null);
	}

	public TripleStore(File dir, String indexSpecStr, boolean forceSync) throws IOException, SailException {
		this(dir, indexSpecStr, forceSync, null);
	}

	public TripleStore(File dir, String indexSpecStr, boolean forceSync, Boolean memoryMappedTxnStatusFileEnabled)
			throws IOException, SailException {
		this.dir = dir;
		this.forceSync = forceSync;
		this.txnStatusFile = createTxnStatusFile(dir, memoryMappedTxnStatusFileEnabled);

		File propFile = new File(dir, PROPERTIES_FILE);

		if (!propFile.exists()) {
			// newly created native store
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

			// Check transaction status
			TxnStatus txnStatus = txnStatusFile.getTxnStatus();
			if (txnStatus == TxnStatus.NONE) {
				logger.trace("No uncompleted transactions found");
			} else {
				processUncompletedTransaction(txnStatus);
			}

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

        private static TxnStatusFile createTxnStatusFile(File dir, Boolean memoryMappedTxnStatusFileEnabled)
                        throws IOException {
                boolean enabled = Boolean.TRUE.equals(memoryMappedTxnStatusFileEnabled);
                if (enabled) {
                        return new MemoryMappedTxnStatusFile(dir);
                }
		return new TxnStatusFile(dir);
	}

	/*---------*
	 * Methods *
	 *---------*/

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

		HashSet<String> invalidIndexes = new HashSet<>();

		for (String fieldSeq : indexSpecs) {
			logger.trace("Initializing index '{}'...", fieldSeq);
			try {
				indexes.add(new TripleIndex(fieldSeq, false));
			} catch (Exception e) {
				if (NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES) {
					invalidIndexes.add(fieldSeq);
					logger.warn("Ignoring index because it failed to initialize index '{}'", fieldSeq, e);
				} else {
					logger.error(
							"Failed to initialize index '{}', consider setting org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes to true.",
							fieldSeq, e);
					throw e;
				}

			}

		}

		if (NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES) {
			indexSpecs.removeAll(invalidIndexes);
		}

		List<TripleIndex> emptyIndexes = new ArrayList<>();
		List<TripleIndex> nonEmptyIndexes = new ArrayList<>();

		checkIfIndexesAreEmptyOrNot(nonEmptyIndexes, emptyIndexes);

		if (!emptyIndexes.isEmpty() && !nonEmptyIndexes.isEmpty()) {
			if (NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES) {
				indexes.removeAll(emptyIndexes);
			} else {
				for (TripleIndex index : emptyIndexes) {
					throw new IOException("Index '" + new String(index.getFieldSeq())
							+ "' is unexpectedly empty while other indexes are not. Consider setting the system property org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes to true. Index file: "
							+ index.getBTree().getFile().getAbsolutePath());
				}
			}
		}

	}

	private void checkIfIndexesAreEmptyOrNot(List<TripleIndex> nonEmptyIndexes, List<TripleIndex> emptyIndexes)
			throws IOException {
		for (TripleIndex index : indexes) {
			try (RecordIterator recordIterator = index.getBTree().iterateAll()) {
				try {
					byte[] next = recordIterator.next();
					if (next != null) {
						next = recordIterator.next();
						if (next != null) {
							nonEmptyIndexes.add(index);
						} else {
							emptyIndexes.add(index);
						}
					} else {
						emptyIndexes.add(index);
					}
				} catch (Throwable ignored) {
					emptyIndexes.add(index);
				}
			}
		}
	}

	private void processUncompletedTransaction(TxnStatus txnStatus) throws IOException {
		switch (txnStatus) {
		case COMMITTING:
			logger.info("Detected uncompleted commit, trying to complete");
			try {
				commit();
				logger.info("Uncompleted commit completed successfully");
			} catch (IOException e) {
				logger.error("Failed to restore from uncompleted commit", e);
				throw e;
			}
			break;
		case ROLLING_BACK:
			logger.info("Detected uncompleted rollback, trying to complete");
			try {
				rollback();
				logger.info("Uncompleted rollback completed successfully");
			} catch (IOException e) {
				logger.error("Failed to restore from uncompleted rollback", e);
				throw e;
			}
			break;
		case ACTIVE:
			logger.info("Detected unfinished transaction, trying to roll back");
			try {
				rollback();
				logger.info("Unfinished transaction rolled back successfully");
			} catch (IOException e) {
				logger.error("Failed to roll back unfinished transaction", e);
				throw e;
			}
			break;
		case UNKNOWN:
			logger.info("Read invalid or unknown transaction status, trying to roll back");
			try {
				rollback();
				logger.info("Successfully performed a rollback for invalid or unknown transaction status");
			} catch (IOException e) {
				logger.error("Failed to perform rollback for invalid or unknown transaction status", e);
				throw e;
			}
			break;
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

			for (String fieldSeq : addedIndexSpecs) {
				logger.debug("Initializing new index '{}'...", fieldSeq);

				TripleIndex addedIndex = new TripleIndex(fieldSeq, true);
				BTree addedBTree = null;
				RecordIterator sourceIter = null;
				try {
					addedBTree = addedIndex.getBTree();
					sourceIter = sourceIndex.getBTree().iterateAll();
					byte[] value;
					while ((value = sourceIter.next()) != null) {
						addedBTree.insert(value);
					}
				} finally {
					try {
						if (sourceIter != null) {
							sourceIter.close();
						}
					} finally {
						if (addedBTree != null) {
							addedBTree.sync();
						}
					}
				}

				currentIndexes.put(fieldSeq, addedIndex);
			}

			logger.debug("New index(es) initialized");
		}

		// Determine the set of removed indexes
		Set<String> removedIndexSpecs = new HashSet<>(currentIndexSpecs);
		removedIndexSpecs.removeAll(newIndexSpecs);

		List<Throwable> removedIndexExceptions = new ArrayList<>();
		// Delete files for removed indexes
		for (String fieldSeq : removedIndexSpecs) {
			try {
				TripleIndex removedIndex = currentIndexes.remove(fieldSeq);

				boolean deleted = removedIndex.getBTree().delete();

				if (deleted) {
					logger.debug("Deleted file(s) for removed {} index", fieldSeq);
				} else {
					logger.warn("Unable to delete file(s) for removed {} index", fieldSeq);
				}
			} catch (Throwable e) {
				removedIndexExceptions.add(e);
			}
		}

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
		try {
			List<Throwable> caughtExceptions = new ArrayList<>();
			for (TripleIndex index : indexes) {
				try {
					index.getBTree().close();
				} catch (Throwable e) {
					logger.warn("Failed to close file for {} index", new String(index.getFieldSeq()));
					caughtExceptions.add(e);
				}
			}
			if (!caughtExceptions.isEmpty()) {
				throw new IOException(caughtExceptions.get(0));
			}
		} finally {
			try {
				txnStatusFile.close();
			} finally {
				// Should have been removed upon commit() or rollback(), but just to be sure
				RecordCache toCloseUpdatedTriplesCache = updatedTriplesCache;
				updatedTriplesCache = null;
				if (toCloseUpdatedTriplesCache != null) {
					toCloseUpdatedTriplesCache.discard();
				}
			}
		}
	}

	public RecordIterator getTriples(int subj, int pred, int obj, int context) {
		// Return all triples except those that were added but not yet committed
		return getTriples(subj, pred, obj, context, 0, ADDED_FLAG);
	}

	public RecordIterator getTriples(int subj, int pred, int obj, int context, boolean readTransaction) {
		if (readTransaction) {
			// Don't read removed statements
			return getTriples(subj, pred, obj, context, 0, TripleStore.REMOVED_FLAG);
		} else {
			// Don't read added statements
			return getTriples(subj, pred, obj, context, 0, TripleStore.ADDED_FLAG);
		}
	}

	/**
	 * If an index exists by context - use it, otherwise return null.
	 *
	 * @param readTransaction
	 * @return All triples sorted by context or null if no context index exists
	 */
	public RecordIterator getAllTriplesSortedByContext(boolean readTransaction) {
		if (readTransaction) {
			// Don't read removed statements
			return getAllTriplesSortedByContext(0, TripleStore.REMOVED_FLAG);
		} else {
			// Don't read added statements
			return getAllTriplesSortedByContext(0, TripleStore.ADDED_FLAG);
		}
	}

	public RecordIterator getTriples(int subj, int pred, int obj, int context, boolean explicit,
			boolean readTransaction) {
		int flags = 0;
		int flagsMask = 0;

		if (readTransaction) {
			flagsMask |= TripleStore.REMOVED_FLAG;
			// 'explicit' is handled through an ExplicitStatementFilter
		} else {
			flagsMask |= TripleStore.ADDED_FLAG;

			if (explicit) {
				flags |= TripleStore.EXPLICIT_FLAG;
				flagsMask |= TripleStore.EXPLICIT_FLAG;
			}
		}

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

	public void disableTxnStatus() {
		txnStatusFile.disable();
	}

	/*-------------------------------------*
	 * Inner class ExplicitStatementFilter *
	 *-------------------------------------*/

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
		public void set(byte[] value) throws IOException {
			wrappedIter.set(value);
		}

		@Override
		public void close() throws IOException {
			wrappedIter.close();
		}
	} // end inner class ExplicitStatementFilter

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
		public void set(byte[] value) throws IOException {
			wrappedIter.set(value);
		}

		@Override
		public void close() throws IOException {
			wrappedIter.close();
		}
	} // end inner class ImplicitStatementFilter

	private RecordIterator getTriples(int subj, int pred, int obj, int context, int flags, int flagsMask) {
		TripleIndex index = getBestIndex(subj, pred, obj, context);
		boolean doRangeSearch = index.getPatternScore(subj, pred, obj, context) > 0;
		return getTriplesUsingIndex(subj, pred, obj, context, flags, flagsMask, index, doRangeSearch);
	}

	private RecordIterator getAllTriplesSortedByContext(int flags, int flagsMask) {
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

		if (rangeSearch) {
			// Use ranged search
			byte[] minValue = getMinValue(subj, pred, obj, context);
			byte[] maxValue = getMaxValue(subj, pred, obj, context);

			return index.getBTree().iterateRangedValues(searchKey, searchMask, minValue, maxValue);
		} else {
			// Use sequential scan
			return index.getBTree().iterateValues(searchKey, searchMask);
		}
	}

	protected double cardinality(int subj, int pred, int obj, int context) throws IOException {
		TripleIndex index = getBestIndex(subj, pred, obj, context);
		BTree btree = index.btree;

		double rangeSize;

		if (index.getPatternScore(subj, pred, obj, context) == 0) {
			rangeSize = btree.getValueCountEstimate();
		} else {
			byte[] minValue = getMinValue(subj, pred, obj, context);
			byte[] maxValue = getMaxValue(subj, pred, obj, context);
			rangeSize = btree.getValueCountEstimate(minValue, maxValue);
		}

		return rangeSize;
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

	public void clear() throws IOException {
		for (TripleIndex index : indexes) {
			index.getBTree().clear();
		}
	}

	public boolean storeTriple(int subj, int pred, int obj, int context) throws IOException {
		return storeTriple(subj, pred, obj, context, true);
	}

	public boolean storeTriple(int subj, int pred, int obj, int context, boolean explicit) throws IOException {
		boolean stAdded;

		byte[] data = getData(subj, pred, obj, context, 0);
		byte[] storedData = indexes.get(0).getBTree().get(data);

		if (storedData == null) {
			// Statement does not yet exist
			data[FLAG_IDX] |= ADDED_FLAG;
			if (explicit) {
				data[FLAG_IDX] |= EXPLICIT_FLAG;
			}

			stAdded = true;
		} else {
			// Statement already exists, only modify its flags, see txn-flags.txt
			// for a description of the flag transformations
			byte flags = storedData[FLAG_IDX];
			boolean wasExplicit = (flags & EXPLICIT_FLAG) != 0;
			boolean wasAdded = (flags & ADDED_FLAG) != 0;
			boolean wasRemoved = (flags & REMOVED_FLAG) != 0;
			boolean wasToggled = (flags & TOGGLE_EXPLICIT_FLAG) != 0;

			if (wasAdded) {
				// Statement has been added in the current transaction and is
				// invisible to other connections, we can simply modify its flags
				data[FLAG_IDX] |= ADDED_FLAG;
				if (explicit || wasExplicit) {
					data[FLAG_IDX] |= EXPLICIT_FLAG;
				}
			} else {
				// Committed statement, must keep explicit flag the same
				if (wasExplicit) {
					data[FLAG_IDX] |= EXPLICIT_FLAG;
				}

				if (explicit) {
					if (!wasExplicit) {
						// Make inferred statement explicit
						data[FLAG_IDX] |= TOGGLE_EXPLICIT_FLAG;
					}
				} else {
					if (wasRemoved) {
						if (wasExplicit) {
							// Re-add removed explicit statement as inferred
							data[FLAG_IDX] |= TOGGLE_EXPLICIT_FLAG;
						}
					} else if (wasToggled) {
						data[FLAG_IDX] |= TOGGLE_EXPLICIT_FLAG;
					}
				}
			}

			// Statement is new if it was removed before
			stAdded = wasRemoved;
		}

		if (storedData == null || !Arrays.equals(data, storedData)) {
			for (TripleIndex index : indexes) {
				index.getBTree().insert(data);
			}

			updatedTriplesCache.storeRecord(data);
		}

		return stAdded;
	}

	/**
	 * Remove triples
	 *
	 * @param subj    The subject for the pattern, or <var>-1</var> for a wildcard.
	 * @param pred    The predicate for the pattern, or <var>-1</var> for a wildcard.
	 * @param obj     The object for the pattern, or <var>-1</var> for a wildcard.
	 * @param context The context for the pattern, or <var>-1</var> for a wildcard.
	 * @return The number of triples that were removed.
	 * @throws IOException
	 * @deprecated Use {@link #removeTriplesByContext(int, int, int, int)} instead.
	 */
	@Deprecated(since = "2.5.3")
	public int removeTriples(int subj, int pred, int obj, int context) throws IOException {
		Map<Integer, Long> countPerContext = removeTriplesByContext(subj, pred, obj, context);
		return (int) countPerContext.values().stream().mapToLong(Long::longValue).sum();
	}

	/**
	 * Remove triples by context
	 *
	 * @param subj    The subject for the pattern, or <var>-1</var> for a wildcard.
	 * @param pred    The predicate for the pattern, or <var>-1</var> for a wildcard.
	 * @param obj     The object for the pattern, or <var>-1</var> for a wildcard.
	 * @param context The context for the pattern, or <var>-1</var> for a wildcard.
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
	 * @param subj     The subject for the pattern, or <var>-1</var> for a wildcard.
	 * @param pred     The predicate for the pattern, or <var>-1</var> for a wildcard.
	 * @param obj      The object for the pattern, or <var>-1</var> for a wildcard.
	 * @param context  The context for the pattern, or <var>-1</var> for a wildcard.
	 * @param explicit Flag indicating whether explicit or inferred statements should be removed; <var>true</var>
	 *                 removes explicit statements that match the pattern, <var>false</var> removes inferred statements
	 *                 that match the pattern.
	 * @return The number of triples that were removed.
	 * @throws IOException
	 * @deprecated since 2.5.3. use {@link #removeTriplesByContext(int, int, int, int, boolean)} instead.
	 */
	@Deprecated(since = "2.5.3")
	public int removeTriples(int subj, int pred, int obj, int context, boolean explicit) throws IOException {
		Map<Integer, Long> countPerContext = removeTriplesByContext(subj, pred, obj, context, explicit);
		return (int) countPerContext.values().stream().mapToLong(Long::longValue).sum();
	}

	/**
	 * @param subj     The subject for the pattern, or <var>-1</var> for a wildcard.
	 * @param pred     The predicate for the pattern, or <var>-1</var> for a wildcard.
	 * @param obj      The object for the pattern, or <var>-1</var> for a wildcard.
	 * @param context  The context for the pattern, or <var>-1</var> for a wildcard.
	 * @param explicit Flag indicating whether explicit or inferred statements should be removed; <var>true</var>
	 *                 removes explicit statements that match the pattern, <var>false</var> removes inferred statements
	 *                 that match the pattern.
	 * @return A mapping of each modified context to the number of statements removed in that context.
	 * @throws IOException
	 */
	public Map<Integer, Long> removeTriplesByContext(int subj, int pred, int obj, int context, boolean explicit)
			throws IOException {
		byte flags = explicit ? EXPLICIT_FLAG : 0;
		try (RecordIterator iter = getTriples(subj, pred, obj, context, flags, EXPLICIT_FLAG)) {
			return removeTriples(iter);
		}
	}

	private Map<Integer, Long> removeTriples(RecordIterator iter) throws IOException {

		byte[] data = iter.next();
		if (data == null) {
			// no triples to remove
			return Collections.emptyMap();
		}

		final HashMap<Integer, Long> perContextCounts = new HashMap<>();

		// Store the values that need to be removed in a tmp file and then
		// iterate over this file to set the REMOVED flag
		RecordCache removedTriplesCache = new InMemRecordCache();
		try {
			try (iter) {
				while (data != null) {
					if ((data[FLAG_IDX] & REMOVED_FLAG) == 0) {
						data[FLAG_IDX] |= REMOVED_FLAG;
						removedTriplesCache.storeRecord(data);
						int context = ByteArrayUtil.getInt(data, CONTEXT_IDX);
						perContextCounts.merge(context, 1L, Long::sum);
					}
					data = iter.next();

					if (shouldOverflowToDisk(removedTriplesCache)) {
						logger.debug("Overflowing RecordCache to disk due to low free mem.");
						assert removedTriplesCache instanceof InMemRecordCache;
						InMemRecordCache old = (InMemRecordCache) removedTriplesCache;
						removedTriplesCache = new SequentialRecordCache(dir, RECORD_LENGTH);
						removedTriplesCache.storeRecords(old);
						old.clear();
					}
				}
			}

			updatedTriplesCache.storeRecords(removedTriplesCache);

			// Set the REMOVED flag by overwriting the affected records
			for (TripleIndex index : indexes) {
				BTree btree = index.getBTree();

				try (RecordIterator recIter = removedTriplesCache.getRecords()) {
					while ((data = recIter.next()) != null) {
						btree.insert(data);
					}
				}
			}
		} finally {
			removedTriplesCache.discard();
		}

		return perContextCounts;
	}

	private boolean shouldOverflowToDisk(RecordCache removedTriplesCache) {
		if (removedTriplesCache instanceof InMemRecordCache
				&& removedTriplesCache.getRecordCount() % CHECK_MEMORY_PRESSURE_INTERVAL == 0) {
			Runtime runtime = Runtime.getRuntime();
			long allocatedMemory = runtime.totalMemory() - runtime.freeMemory();
			long presumableFreeMemory = runtime.maxMemory() - allocatedMemory;

			logger.trace("Free memory {} MB and required free memory {} MB", presumableFreeMemory / 1024 / 1024,
					MIN_FREE_MEMORY_BEFORE_OVERFLOW / 1024 / 1024);
			return presumableFreeMemory < MIN_FREE_MEMORY_BEFORE_OVERFLOW;
		}

		return false;
	}

	public void startTransaction() throws IOException {
		txnStatusFile.setTxnStatus(TxnStatus.ACTIVE, forceSync);

		// Create a record cache for storing updated triples with a maximum of
		// some 10% of the number of triples
		long maxRecords = indexes.get(0).getBTree().getValueCountEstimate() / 10L;
		if (updatedTriplesCache == null) {
			updatedTriplesCache = new SortedRecordCache(dir, RECORD_LENGTH, maxRecords, new TripleComparator("spoc"));
		} else {
			assert updatedTriplesCache
					.getRecordCount() == 0L : "updatedTripleCache should have been cleared upon commit or rollback";
			updatedTriplesCache.setMaxRecords(maxRecords);
		}
	}

	public void commit() throws IOException {
		txnStatusFile.setTxnStatus(TxnStatus.COMMITTING, forceSync);

		// updatedTriplesCache will be null when recovering from a crashed commit
		boolean validCache = updatedTriplesCache != null && updatedTriplesCache.isValid();

		for (TripleIndex index : indexes) {
			BTree btree = index.getBTree();

			RecordIterator iter;
			if (validCache) {
				// Use the cached set of updated triples
				iter = updatedTriplesCache.getRecords();
			} else {
				// Cache is invalid; too much updates(?). Iterate over all triples
				iter = btree.iterateAll();
			}

			try {
				byte[] data;
				while ((data = iter.next()) != null) {
					byte flags = data[FLAG_IDX];
					boolean wasAdded = (flags & ADDED_FLAG) != 0;
					boolean wasRemoved = (flags & REMOVED_FLAG) != 0;
					boolean wasToggled = (flags & TOGGLE_EXPLICIT_FLAG) != 0;

					if (wasRemoved) {
						btree.remove(data);
					} else if (wasAdded || wasToggled) {
						if (wasToggled) {
							data[FLAG_IDX] ^= EXPLICIT_FLAG;
						}
						if (wasAdded) {
							data[FLAG_IDX] ^= ADDED_FLAG;
						}

						if (validCache) {
							// We're iterating the cache
							btree.insert(data);
						} else {
							// We're iterating the BTree itself
							iter.set(data);
						}
					}
				}
			} finally {
				iter.close();
			}
		}

		if (updatedTriplesCache != null) {
			updatedTriplesCache.clear();
		}

		sync();

		txnStatusFile.setTxnStatus(TxnStatus.NONE, forceSync);
		// checkAllCommitted();
	}

	private void checkAllCommitted() throws IOException {
		for (TripleIndex index : indexes) {
			System.out.println("Checking " + index + " index");
			BTree btree = index.getBTree();
			try (RecordIterator iter = btree.iterateAll()) {
				for (byte[] data = iter.next(); data != null; data = iter.next()) {
					byte flags = data[FLAG_IDX];
					boolean wasAdded = (flags & ADDED_FLAG) != 0;
					boolean wasRemoved = (flags & REMOVED_FLAG) != 0;
					boolean wasToggled = (flags & TOGGLE_EXPLICIT_FLAG) != 0;
					if (wasAdded || wasRemoved || wasToggled) {
						System.out.println("unexpected triple: " + ByteArrayUtil.toHexString(data));
					}
				}
			}
		}
	}

	public void rollback() throws IOException {
		txnStatusFile.setTxnStatus(TxnStatus.ROLLING_BACK, forceSync);

		// updatedTriplesCache will be null when recovering from a crash
		boolean validCache = updatedTriplesCache != null && updatedTriplesCache.isValid();

		byte txnFlagsMask = ~(ADDED_FLAG | REMOVED_FLAG | TOGGLE_EXPLICIT_FLAG);

		for (TripleIndex index : indexes) {
			BTree btree = index.getBTree();

			RecordIterator iter;
			if (validCache) {
				// Use the cached set of updated triples
				iter = updatedTriplesCache.getRecords();
			} else {
				// Cache is invalid; too much updates(?). Iterate over all triples
				iter = btree.iterateAll();
			}

			try {
				byte[] data;
				while ((data = iter.next()) != null) {
					byte flags = data[FLAG_IDX];
					boolean wasAdded = (flags & ADDED_FLAG) != 0;
					boolean wasRemoved = (flags & REMOVED_FLAG) != 0;
					boolean wasToggled = (flags & TOGGLE_EXPLICIT_FLAG) != 0;

					if (wasAdded) {
						btree.remove(data);
					} else {
						if (wasRemoved || wasToggled) {
							data[FLAG_IDX] &= txnFlagsMask;

							if (validCache) {
								// We're iterating the cache
								btree.insert(data);
							} else {
								// We're iterating the BTree itself
								iter.set(data);
							}
						}
					}
				}
			} finally {
				iter.close();
			}
		}

		if (updatedTriplesCache != null) {
			updatedTriplesCache.clear();
		}

		sync();

		txnStatusFile.setTxnStatus(TxnStatus.NONE, forceSync);
	}

	protected void sync() throws IOException {
		List<Throwable> exceptions = new ArrayList<>();
		for (TripleIndex index : indexes) {
			try {
				index.getBTree().sync();
			} catch (Throwable e) {
				exceptions.add(e);
			}
		}
		if (!exceptions.isEmpty()) {
			throw new IOException(exceptions.get(0));
		}
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

	/*-------------------------*
	 * Inner class TripleIndex *
	 *-------------------------*/

	private class TripleIndex {

		private final TripleComparator tripleComparator;

		private final BTree btree;

		public TripleIndex(String fieldSeq, boolean deleteExistingIndexFile) throws IOException {
			if (deleteExistingIndexFile) {
				File indexFile = new File(dir, getFilenamePrefix(fieldSeq) + ".dat");
				if (indexFile.exists()) {
					indexFile.delete();
				}
				File alloxFile = new File(dir, getFilenamePrefix(fieldSeq) + ".alloc");
				if (alloxFile.exists()) {
					alloxFile.delete();
				}
			}
			tripleComparator = new TripleComparator(fieldSeq);
			btree = new BTree(dir, getFilenamePrefix(fieldSeq), 2048, RECORD_LENGTH, tripleComparator.compareStrategy,
					forceSync);
		}

		private String getFilenamePrefix(String fieldSeq) {
			return "triples-" + fieldSeq;
		}

		public char[] getFieldSeq() {
			return tripleComparator.getFieldSeq();
		}

		public BTree getBTree() {
			return btree;
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
	}

	/*------------------------------*
	 * Inner class TripleComparator *
	 *------------------------------*/

	/**
	 * A RecordComparator that can be used to create indexes with a configurable order of the subject, predicate, object
	 * and context fields.
	 */
	private static class TripleComparator implements RecordComparator {

		private final char[] fieldSeq;
		private final RecordComparator compareStrategy;

		public TripleComparator(String fieldSeq) {
			String normalized = normalizeFieldSequence(fieldSeq);
			this.fieldSeq = normalized.toCharArray();
			this.compareStrategy = getComparator(normalized);
		}

		private static final RecordComparator compareSPOC = TripleComparator::compareSPOC;
		private static final RecordComparator compareSPCO = TripleComparator::compareSPCO;
		private static final RecordComparator compareSOPC = TripleComparator::compareSOPC;
		private static final RecordComparator compareSOCP = TripleComparator::compareSOCP;
		private static final RecordComparator compareSCPO = TripleComparator::compareSCPO;
		private static final RecordComparator compareSCOP = TripleComparator::compareSCOP;
		private static final RecordComparator comparePSOC = TripleComparator::comparePSOC;
		private static final RecordComparator comparePSCO = TripleComparator::comparePSCO;
		private static final RecordComparator comparePOSC = TripleComparator::comparePOSC;
		private static final RecordComparator comparePOCS = TripleComparator::comparePOCS;
		private static final RecordComparator comparePCSO = TripleComparator::comparePCSO;
		private static final RecordComparator comparePCOS = TripleComparator::comparePCOS;
		private static final RecordComparator compareOSPC = TripleComparator::compareOSPC;
		private static final RecordComparator compareOSCP = TripleComparator::compareOSCP;
		private static final RecordComparator compareOPSC = TripleComparator::compareOPSC;
		private static final RecordComparator compareOPCS = TripleComparator::compareOPCS;
		private static final RecordComparator compareOCSP = TripleComparator::compareOCSP;
		private static final RecordComparator compareOCPS = TripleComparator::compareOCPS;
		private static final RecordComparator compareCSPO = TripleComparator::compareCSPO;
		private static final RecordComparator compareCSOP = TripleComparator::compareCSOP;
		private static final RecordComparator compareCPSO = TripleComparator::compareCPSO;
		private static final RecordComparator compareCPOS = TripleComparator::compareCPOS;
		private static final RecordComparator compareCOSP = TripleComparator::compareCOSP;
		private static final RecordComparator compareCOPS = TripleComparator::compareCOPS;

		private static RecordComparator getComparator(String order) {
			switch (order) {
			case "spoc":
				return compareSPOC;
			case "spco":
				return compareSPCO;
			case "sopc":
				return compareSOPC;
			case "socp":
				return compareSOCP;
			case "scpo":
				return compareSCPO;
			case "scop":
				return compareSCOP;
			case "psoc":
				return comparePSOC;
			case "psco":
				return comparePSCO;
			case "posc":
				return comparePOSC;
			case "pocs":
				return comparePOCS;
			case "pcso":
				return comparePCSO;
			case "pcos":
				return comparePCOS;
			case "ospc":
				return compareOSPC;
			case "oscp":
				return compareOSCP;
			case "opsc":
				return compareOPSC;
			case "opcs":
				return compareOPCS;
			case "ocsp":
				return compareOCSP;
			case "ocps":
				return compareOCPS;
			case "cspo":
				return compareCSPO;
			case "csop":
				return compareCSOP;
			case "cpso":
				return compareCPSO;
			case "cpos":
				return compareCPOS;
			case "cosp":
				return compareCOSP;
			case "cops":
				return compareCOPS;
			default:
				throw new IllegalArgumentException("Unknown field order: " + order);
			}
		}

		public char[] getFieldSeq() {
			return fieldSeq;
		}

		@Override
		public final int compareBTreeValues(byte[] key, byte[] data, int offset, int length) {
			return compareStrategy.compareBTreeValues(key, data, offset, length);
		}

		private static String normalizeFieldSequence(String fieldSeq) {
			if (fieldSeq == null) {
				throw new IllegalArgumentException("Field sequence must not be null");
			}
			String normalized = fieldSeq.trim().toLowerCase(Locale.ROOT);
			if (normalized.length() != 4) {
				throw new IllegalArgumentException(
						"Field sequence '" + fieldSeq + "' must be four characters long (permutation of 'spoc').");
			}
			return normalized;
		}

		private static int compareSPOC(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, SUBJ_IDX, PRED_IDX, OBJ_IDX, CONTEXT_IDX);
		}

		private static int compareSPCO(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, SUBJ_IDX, PRED_IDX, CONTEXT_IDX, OBJ_IDX);
		}

		private static int compareSOPC(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, SUBJ_IDX, OBJ_IDX, PRED_IDX, CONTEXT_IDX);
		}

		private static int compareSOCP(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, SUBJ_IDX, OBJ_IDX, CONTEXT_IDX, PRED_IDX);
		}

		private static int compareSCPO(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, SUBJ_IDX, CONTEXT_IDX, PRED_IDX, OBJ_IDX);
		}

		private static int compareSCOP(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, SUBJ_IDX, CONTEXT_IDX, OBJ_IDX, PRED_IDX);
		}

		private static int comparePSOC(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, PRED_IDX, SUBJ_IDX, OBJ_IDX, CONTEXT_IDX);
		}

		private static int comparePSCO(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, PRED_IDX, SUBJ_IDX, CONTEXT_IDX, OBJ_IDX);
		}

		private static int comparePOSC(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, PRED_IDX, OBJ_IDX, SUBJ_IDX, CONTEXT_IDX);
		}

		private static int comparePOCS(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, PRED_IDX, OBJ_IDX, CONTEXT_IDX, SUBJ_IDX);
		}

		private static int comparePCSO(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, PRED_IDX, CONTEXT_IDX, SUBJ_IDX, OBJ_IDX);
		}

		private static int comparePCOS(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, PRED_IDX, CONTEXT_IDX, OBJ_IDX, SUBJ_IDX);
		}

		private static int compareOSPC(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, OBJ_IDX, SUBJ_IDX, PRED_IDX, CONTEXT_IDX);
		}

		private static int compareOSCP(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, OBJ_IDX, SUBJ_IDX, CONTEXT_IDX, PRED_IDX);
		}

		private static int compareOPSC(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, OBJ_IDX, PRED_IDX, SUBJ_IDX, CONTEXT_IDX);
		}

		private static int compareOPCS(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, OBJ_IDX, PRED_IDX, CONTEXT_IDX, SUBJ_IDX);
		}

		private static int compareOCSP(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, OBJ_IDX, CONTEXT_IDX, SUBJ_IDX, PRED_IDX);
		}

		private static int compareOCPS(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, OBJ_IDX, CONTEXT_IDX, PRED_IDX, SUBJ_IDX);
		}

		private static int compareCSPO(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, CONTEXT_IDX, SUBJ_IDX, PRED_IDX, OBJ_IDX);
		}

		private static int compareCSOP(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, CONTEXT_IDX, SUBJ_IDX, OBJ_IDX, PRED_IDX);
		}

		private static int compareCPSO(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, CONTEXT_IDX, PRED_IDX, SUBJ_IDX, OBJ_IDX);
		}

		private static int compareCPOS(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, CONTEXT_IDX, PRED_IDX, OBJ_IDX, SUBJ_IDX);
		}

		private static int compareCOSP(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, CONTEXT_IDX, OBJ_IDX, SUBJ_IDX, PRED_IDX);
		}

		private static int compareCOPS(byte[] key, byte[] data, int offset, int length) {
			return compareFields(key, data, offset, CONTEXT_IDX, OBJ_IDX, PRED_IDX, SUBJ_IDX);
		}

		/**
		 * Lexicographically compares four 4-byte fields drawn from 'key' and 'data' at indices (first, second, third,
		 * fourth), where the data side is offset by 'offset'. Bytes are treated as unsigned, and the return value is
		 * the (unsigned) difference of the first mismatching bytes, or 0 if all four fields are equal.
		 */
		static int compareFields(byte[] key, byte[] data, int offset,
				int first, int second, int third, int fourth) {

			// Field 1
			int a = (int) INT_BE.get(key, first);
			int b = (int) INT_BE.get(data, offset + first);
			int x = a ^ b;
			if (x != 0)
				return diffFromXorInt(a, b, x);

			// Field 2
			a = (int) INT_BE.get(key, second);
			b = (int) INT_BE.get(data, offset + second);
			x = a ^ b;
			if (x != 0)
				return diffFromXorInt(a, b, x);

			// Field 3
			a = (int) INT_BE.get(key, third);
			b = (int) INT_BE.get(data, offset + third);
			x = a ^ b;
			if (x != 0)
				return diffFromXorInt(a, b, x);

			// Field 4
			a = (int) INT_BE.get(key, fourth);
			b = (int) INT_BE.get(data, offset + fourth);
			x = a ^ b;
			if (x != 0)
				return diffFromXorInt(a, b, x);

			return 0;
		}

		/**
		 * Given two big-endian-packed ints and their XOR (non-zero), return the (unsigned) difference of the first
		 * mismatching bytes.
		 *
		 * Trick: the first differing byte’s position is the number of leading zeros of x, rounded down to a multiple of
		 * 8. Left-shift both ints by that many bits so the mismatching byte moves into the top byte, then extract it.
		 */
		private static int diffFromXorInt(int a, int b, int x) {
			int n = Integer.numberOfLeadingZeros(x) & ~7; // 0,8,16,24
			return ((a << n) >>> 24) - ((b << n) >>> 24);
		}

		private static final VarHandle INT_BE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);

		public static int compareFieldLength4(byte[] key, byte[] data, int offset, int fieldIdx) {
			final int a = (int) INT_BE.get(key, fieldIdx);
			final int b = (int) INT_BE.get(data, offset + fieldIdx);

			final int x = a ^ b; // mask of differing bits
			if (x == 0)
				return 0; // all 4 bytes equal

			// Find the first differing *byte* from the left (k .. k+3).
			// With a big‑endian view, the first byte lives in bits 31..24, etc.
			final int byteIndex = Integer.numberOfLeadingZeros(x) >>> 3; // 0..3 equal-leading-byte count
			final int shift = 24 - (byteIndex << 3);

			// Extract that byte from each int (as unsigned) and return their difference.
			return ((a >>> shift) & 0xFF) - ((b >>> shift) & 0xFF);
		}
	}

	private static boolean isAssertionsEnabled() {
		try {
			assert false;
			return false;
		} catch (AssertionError ignored) {
			return true;
		}
	}
}
