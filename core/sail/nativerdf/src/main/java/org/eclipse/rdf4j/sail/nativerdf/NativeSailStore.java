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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.BackingSailSource;
import org.eclipse.rdf4j.sail.base.Changeset;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;
import org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeValue;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWAL;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A disk based {@link SailStore} implementation that keeps committed statements in a {@link TripleStore}.
 *
 * @author James Leigh
 */
class NativeSailStore implements SailStore {

	final Logger logger = LoggerFactory.getLogger(NativeSailStore.class);
	private static final Pattern WAL_SEGMENT_PATTERN = Pattern.compile("wal-\\d+\\.v1(?:\\.gz)?");

	private final TripleStore tripleStore;

	private final ValueStoreWAL valueStoreWal;

	private final ValueStore valueStore;

	private final NamespaceStore namespaceStore;

	private final ContextStore contextStore;
	private final boolean walEnabled;

	/**
	 * A lock to control concurrent access by {@link NativeSailSink} to the TripleStore, ValueStore, and NamespaceStore.
	 * Each sink method that directly accesses one of these store obtains the lock and releases it immediately when
	 * done.
	 */
	private final ReentrantLock sinkStoreAccessLock = new ReentrantLock();

	/**
	 * Boolean indicating whether any {@link NativeSailSink} has started a transaction on the {@link TripleStore}.
	 */
	private final AtomicBoolean storeTxnStarted = new AtomicBoolean(false);

	/**
	 * Creates a new {@link NativeSailStore} with the default cache sizes.
	 */
	public NativeSailStore(File dataDir, String tripleIndexes) throws IOException, SailException {
		this(dataDir, tripleIndexes, false, ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
				ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE,
				-1L, -1, -1, null, -1L, -1L, null, false, false, true);
	}

	/**
	 * Creates a new {@link NativeSailStore}.
	 */

	public NativeSailStore(File dataDir, String tripleIndexes, boolean forceSync, int valueCacheSize,
			int valueIDCacheSize, int namespaceCacheSize, int namespaceIDCacheSize, long walMaxSegmentBytes,
			int walQueueCapacity, int walBatchBufferBytes,
			ValueStoreWalConfig.SyncPolicy walSyncPolicy,
			long walSyncIntervalMillis, long walIdlePollIntervalMillis, String walDirectoryName)
			throws IOException, SailException {
		this(dataDir, tripleIndexes, forceSync, valueCacheSize, valueIDCacheSize, namespaceCacheSize,
				namespaceIDCacheSize, walMaxSegmentBytes, walQueueCapacity, walBatchBufferBytes, walSyncPolicy,
				walSyncIntervalMillis, walIdlePollIntervalMillis, walDirectoryName, false, false, true);
	}

	public NativeSailStore(File dataDir, String tripleIndexes, boolean forceSync, int valueCacheSize,
			int valueIDCacheSize, int namespaceCacheSize, int namespaceIDCacheSize, long walMaxSegmentBytes,
			int walQueueCapacity, int walBatchBufferBytes,
			ValueStoreWalConfig.SyncPolicy walSyncPolicy,
			long walSyncIntervalMillis, long walIdlePollIntervalMillis, String walDirectoryName,
			boolean walSyncBootstrapOnOpen, boolean walAutoRecoverOnOpen, boolean walEnabled)
			throws IOException, SailException {
		this.walEnabled = walEnabled;
		NamespaceStore createdNamespaceStore = null;
		ValueStoreWAL createdWal = null;
		ValueStore createdValueStore = null;
		TripleStore createdTripleStore = null;
		ContextStore createdContextStore = null;
		boolean initialized = false;
		try {
			createdNamespaceStore = new NamespaceStore(dataDir);
			Path walDir = dataDir.toPath()
					.resolve(walDirectoryName != null && !walDirectoryName.isEmpty() ? walDirectoryName
							: ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
			boolean enableWal = shouldEnableWal(dataDir, walDir);
			ValueStoreWalConfig walConfig = null;
			if (enableWal) {
				String storeUuid = loadOrCreateWalUuid(walDir);
				ValueStoreWalConfig.Builder walBuilder = ValueStoreWalConfig.builder()
						.walDirectory(walDir)
						.storeUuid(storeUuid);
				if (walMaxSegmentBytes > 0) {
					walBuilder.maxSegmentBytes(walMaxSegmentBytes);
				}
				if (walQueueCapacity > 0) {
					walBuilder.queueCapacity(walQueueCapacity);
				}
				if (walBatchBufferBytes > 0) {
					walBuilder.batchBufferBytes(walBatchBufferBytes);
				}
				if (walSyncPolicy != null) {
					walBuilder.syncPolicy(walSyncPolicy);
				}
				if (walSyncIntervalMillis >= 0) {
					walBuilder.syncInterval(Duration.ofMillis(walSyncIntervalMillis));
				}
				if (walIdlePollIntervalMillis >= 0) {
					walBuilder.idlePollInterval(Duration.ofMillis(walIdlePollIntervalMillis));
				}
				// propagate bootstrap mode
				walBuilder.syncBootstrapOnOpen(walSyncBootstrapOnOpen);
				walBuilder.recoverValueStoreOnOpen(walAutoRecoverOnOpen);
				walConfig = walBuilder.build();
				createdWal = ValueStoreWAL.open(walConfig);
			} else {
				createdWal = null;
			}
			createdValueStore = new ValueStore(dataDir, forceSync, valueCacheSize, valueIDCacheSize,
					namespaceCacheSize, namespaceIDCacheSize, createdWal);
			createdTripleStore = new TripleStore(dataDir, tripleIndexes, forceSync);

			// Assign fields required by ContextStore before constructing it
			namespaceStore = createdNamespaceStore;
			valueStoreWal = createdWal;
			valueStore = createdValueStore;
			tripleStore = createdTripleStore;

			// Now ContextStore can safely read from this store
			createdContextStore = new ContextStore(this, dataDir);
			initialized = true;
		} finally {
			if (!initialized) {
				closeQuietly(createdContextStore);
				closeQuietly(createdTripleStore);
				closeQuietly(createdValueStore);
				closeQuietly(createdWal);
				closeQuietly(createdNamespaceStore);
			}
		}
		// Finalize assignment of contextStore
		contextStore = createdContextStore;
	}

	private String loadOrCreateWalUuid(Path walDir) throws IOException {
		Files.createDirectories(walDir);
		Path file = walDir.resolve("store.uuid");
		if (Files.exists(file)) {
			return Files.readString(file, StandardCharsets.UTF_8).trim();
		}
		String uuid = UUID.randomUUID().toString();
		Files.writeString(file, uuid, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
		return uuid;
	}

	private boolean shouldEnableWal(File dataDir, Path walDir) throws IOException {
		if (!walEnabled) {
			if (logger.isDebugEnabled()) {
				if (hasExistingWalSegments(walDir)) {
					logger.debug(
							"ValueStore WAL is disabled via configuration but {} contains WAL segments; ignoring them.",
							walDir);
				} else {
					logger.debug("ValueStore WAL disabled via configuration for {}", dataDir);
				}
			}

			return false;
		}
		// Respect read-only data directories: do not enable WAL when we can't write
		if (!dataDir.canWrite()) {
			return false;
		}
		if (hasExistingWalSegments(walDir)) {
//			writeBootstrapMarker(walDir, "enabled-existing-wal");
			return true;
		}
		try (DataStore values = new DataStore(dataDir, "values", false)) {
			if (values.getMaxID() > 0) {
//				writeBootstrapMarker(walDir, "enabled-rebuild-existing-values");
				return true;
			}
		}
//		writeBootstrapMarker(walDir, "enabled-empty-store");
		return true;
	}

	private boolean hasExistingWalSegments(Path walDir) throws IOException {
		if (!Files.isDirectory(walDir)) {
			return false;
		}
		try (var stream = Files.list(walDir)) {
			return stream.anyMatch(path -> WAL_SEGMENT_PATTERN.matcher(path.getFileName().toString()).matches());
		}
	}

	private void writeBootstrapMarker(Path walDir, String state) {
		try {
			Files.createDirectories(walDir);
			Path marker = walDir.resolve("bootstrap.info");
			String content = "state=" + state + "\n";
			Files.writeString(marker, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			logger.warn("Failed to write WAL bootstrap marker", e);
		}
	}

	private void closeQuietly(ContextStore store) {
		if (store != null) {
			store.close();
		}
	}

	private void closeQuietly(TripleStore store) {
		if (store != null) {
			try {
				store.close();
			} catch (IOException e) {
				logger.warn("Failed to close triple store", e);
			}
		}
	}

	private void closeQuietly(ValueStore store) {
		if (store != null) {
			try {
				store.close();
			} catch (IOException e) {
				logger.warn("Failed to close value store", e);
			}
		}
	}

	private void closeQuietly(ValueStoreWAL wal) {
		if (wal != null) {
			try {
				wal.close();
			} catch (IOException e) {
				logger.warn("Failed to close value store WAL", e);
			}
		}
	}

	private void closeQuietly(NamespaceStore store) {
		if (store != null) {
			store.close();
		}
	}

	@Override
	public ValueFactory getValueFactory() {
		return valueStore;
	}

	@Override
	public void close() throws SailException {
		try {
			try {
				if (namespaceStore != null) {
					namespaceStore.close();
				}
			} finally {
				try {
					if (contextStore != null) {
						contextStore.close();
					}
				} finally {
					try {
						if (valueStore != null) {
							valueStore.close();
						}
					} finally {
						try {
							if (valueStoreWal != null) {
								valueStoreWal.close();
							}
						} finally {
							if (tripleStore != null) {
								tripleStore.close();
							}
						}
					}
				}

			}
		} catch (IOException e) {
			logger.warn("Failed to close store", e);
			throw new SailException(e);
		}
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return new NativeEvaluationStatistics(valueStore, tripleStore);
	}

	@Override
	public SailSource getExplicitSailSource() {
		return new NativeSailSource(true);
	}

	@Override
	public SailSource getInferredSailSource() {
		return new NativeSailSource(false);
	}

	List<Integer> getContextIDs(Resource... contexts) throws IOException {
		assert contexts.length > 0 : "contexts must not be empty";

		// Filter duplicates
		LinkedHashSet<Resource> contextSet = new LinkedHashSet<>();
		Collections.addAll(contextSet, contexts);

		// Fetch IDs, filtering unknown resources from the result
		List<Integer> contextIDs = new ArrayList<>(contextSet.size());
		for (Resource context : contextSet) {
			if (context == null) {
				contextIDs.add(0);
			} else {
				int contextID = valueStore.getID(context);
				if (contextID != NativeValue.UNKNOWN_ID) {
					contextIDs.add(contextID);
				}
			}
		}

		return contextIDs;
	}

	CloseableIteration<Resource> getContexts() throws IOException {
		RecordIterator btreeIter = tripleStore.getAllTriplesSortedByContext(false);
		CloseableIteration<? extends Statement> stIter1;
		if (btreeIter == null) {
			// Iterator over all statements
			stIter1 = createStatementIterator(null, null, null, true);
		} else {
			stIter1 = new NativeStatementIterator(btreeIter, valueStore);
		}

		FilterIteration<Statement> stIter2 = new FilterIteration<>(
				stIter1) {
			@Override
			protected boolean accept(Statement st) {
				return st.getContext() != null;
			}

			@Override
			protected void handleClose() {

			}
		};

		return new ConvertingIteration<>(stIter2) {
			@Override
			protected Resource convert(Statement sourceObject) throws SailException {
				return sourceObject.getContext();
			}
		};
	}

	/**
	 * Creates a statement iterator based on the supplied pattern.
	 *
	 * @param subj     The subject of the pattern, or <var>null</var> to indicate a wildcard.
	 * @param pred     The predicate of the pattern, or <var>null</var> to indicate a wildcard.
	 * @param obj      The object of the pattern, or <var>null</var> to indicate a wildcard.
	 * @param contexts The context(s) of the pattern. Note that this parameter is a vararg and as such is optional. If
	 *                 no contexts are supplied the method operates on the entire repository.
	 * @return A StatementIterator that can be used to iterate over the statements that match the specified pattern.
	 */
	CloseableIteration<? extends Statement> createStatementIterator(Resource subj, IRI pred, Value obj,
			boolean explicit, Resource... contexts) throws IOException {
		int subjID = NativeValue.UNKNOWN_ID;
		if (subj != null) {
			subjID = valueStore.getID(subj);
			if (subjID == NativeValue.UNKNOWN_ID) {
				return new EmptyIteration<>();
			}
		}

		int predID = NativeValue.UNKNOWN_ID;
		if (pred != null) {
			predID = valueStore.getID(pred);
			if (predID == NativeValue.UNKNOWN_ID) {
				return new EmptyIteration<>();
			}
		}

		int objID = NativeValue.UNKNOWN_ID;
		if (obj != null) {
			objID = valueStore.getID(obj);

			if (objID == NativeValue.UNKNOWN_ID) {
				return new EmptyIteration<>();
			}
		}

		List<Integer> contextIDList = new ArrayList<>(contexts.length);
		if (contexts.length == 0) {
			contextIDList.add(NativeValue.UNKNOWN_ID);
		} else {
			for (Resource context : contexts) {
				if (context == null) {
					contextIDList.add(0);
				} else if (!context.isTriple()) {
					int contextID = valueStore.getID(context);

					if (contextID != NativeValue.UNKNOWN_ID) {
						contextIDList.add(contextID);
					}
				}
			}
		}

		ArrayList<NativeStatementIterator> perContextIterList = new ArrayList<>(contextIDList.size());

		for (int contextID : contextIDList) {
			RecordIterator btreeIter = tripleStore.getTriples(subjID, predID, objID, contextID, explicit, false);

			perContextIterList.add(new NativeStatementIterator(btreeIter, valueStore));
		}

		if (perContextIterList.size() == 1) {
			return perContextIterList.get(0);
		} else {
			return new UnionIteration<>(perContextIterList);
		}
	}

	double cardinality(Resource subj, IRI pred, Value obj, Resource context) throws IOException {
		int subjID = NativeValue.UNKNOWN_ID;
		if (subj != null) {
			subjID = valueStore.getID(subj);
			if (subjID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		int predID = NativeValue.UNKNOWN_ID;
		if (pred != null) {
			predID = valueStore.getID(pred);
			if (predID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		int objID = NativeValue.UNKNOWN_ID;
		if (obj != null) {
			objID = valueStore.getID(obj);
			if (objID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		int contextID = NativeValue.UNKNOWN_ID;
		if (context != null) {
			contextID = valueStore.getID(context);
			if (contextID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		return tripleStore.cardinality(subjID, predID, objID, contextID);
	}

	public void disableTxnStatus() {
		this.tripleStore.disableTxnStatus();
	}

	private final class NativeSailSource extends BackingSailSource {

		private final boolean explicit;

		public NativeSailSource(boolean explicit) {
			this.explicit = explicit;
		}

		@Override
		public SailSource fork() {
			throw new UnsupportedOperationException("This store does not support multiple datasets");
		}

		@Override
		public SailSink sink(IsolationLevel level) throws SailException {
			return new NativeSailSink(explicit);
		}

		@Override
		public NativeSailDataset dataset(IsolationLevel level) throws SailException {
			return new NativeSailDataset(explicit);
		}

	}

	private final class NativeSailSink implements SailSink {

		private final boolean explicit;

		public NativeSailSink(boolean explicit) throws SailException {
			this.explicit = explicit;
		}

		private long walHighWaterMark = ValueStoreWAL.NO_LSN;

		@Override
		public void close() {
			// no-op
		}

		private int storeValueId(Value value) throws IOException {
			int id = valueStore.storeValue(value);
			OptionalLong walLsn = valueStore.drainPendingWalHighWaterMark();
			if (walLsn.isPresent()) {
				walHighWaterMark = Math.max(walHighWaterMark, walLsn.getAsLong());
			}
			return id;
		}

		@Override
		public void prepare() throws SailException {
			// serializable is not supported at this level
		}

		@Override
		public synchronized void flush() throws SailException {
			sinkStoreAccessLock.lock();
			try {
				try {
					if (walHighWaterMark > ValueStoreWAL.NO_LSN) {
						valueStore.awaitWalDurable(walHighWaterMark);
						walHighWaterMark = ValueStoreWAL.NO_LSN;
					}
					valueStore.sync();
				} finally {
					try {
						namespaceStore.sync();
					} finally {
						try {
							contextStore.sync();
						} finally {
							if (storeTxnStarted.get()) {
								tripleStore.commit();
								// do not set flag to false until _after_ commit is succesfully completed.
								storeTxnStarted.set(false);
							}
						}
					}
				}
			} catch (IOException e) {
				logger.error("Encountered an unexpected problem while trying to commit", e);
				throw new SailException(e);
			} catch (RuntimeException e) {
				logger.error("Encountered an unexpected problem while trying to commit", e);
				if (e instanceof SailException) {
					throw e;
				}
				// Ensure upstream handles this as a SailException so branch flush clears pending changes
				throw new SailException(e);
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void setNamespace(String prefix, String name) throws SailException {
			sinkStoreAccessLock.lock();
			try {
				startTriplestoreTransaction();
				namespaceStore.setNamespace(prefix, name);
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void removeNamespace(String prefix) throws SailException {
			sinkStoreAccessLock.lock();
			try {
				startTriplestoreTransaction();
				namespaceStore.removeNamespace(prefix);
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void clearNamespaces() throws SailException {
			sinkStoreAccessLock.lock();
			try {
				startTriplestoreTransaction();
				namespaceStore.clear();
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void observe(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
			// serializable is not supported at this level
		}

		@Override
		public void observeAll(Set<Changeset.SimpleStatementPattern> observed) {
			// serializable is not supported at this level
		}

		@Override
		public void clear(Resource... contexts) throws SailException {
			removeStatements(null, null, null, explicit, contexts);
		}

		@Override
		public void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {
			try {
				addStatement(subj, pred, obj, explicit, ctx);
			} catch (RuntimeException e) {
				if (e instanceof SailException) {
					throw e;
				}
				// Ensure upstream handles this as a SailException so branch flush clears pending changes
				throw new SailException(e);
			}
		}

		@Override
		public void approveAll(Set<Statement> approved, Set<Resource> approvedContexts) {
			sinkStoreAccessLock.lock();
			startTriplestoreTransaction();

			try {
				for (Statement statement : approved) {
					Resource subj = statement.getSubject();
					IRI pred = statement.getPredicate();
					Value obj = statement.getObject();
					Resource context = statement.getContext();

					int subjID = storeValueId(subj);
					int predID = storeValueId(pred);
					int objID = storeValueId(obj);

					int contextID = 0;
					if (context != null) {
						contextID = storeValueId(context);
					}

					boolean wasNew = tripleStore.storeTriple(subjID, predID, objID, contextID, explicit);
					if (wasNew && context != null) {
						contextStore.increment(context);
					}

				}
			} catch (IOException e) {
				throw new SailException(e);
			} catch (RuntimeException e) {
				if (e instanceof SailException) {
					throw e;
				}
				logger.error("Encountered an unexpected problem while trying to add a statement", e);
				throw new SailException(e);
			} finally {
				sinkStoreAccessLock.unlock();
			}

		}

		@Override
		public void deprecate(Statement statement) throws SailException {
			removeStatements(statement.getSubject(), statement.getPredicate(), statement.getObject(), explicit,
					statement.getContext());
		}

		/**
		 * Starts a transaction on the triplestore, if necessary.
		 *
		 * @throws SailException if a transaction could not be started.
		 */
		private synchronized void startTriplestoreTransaction() throws SailException {

			if (storeTxnStarted.compareAndSet(false, true)) {
				try {
					tripleStore.startTransaction();
				} catch (IOException e) {
					storeTxnStarted.set(false);
					throw new SailException(e);
				}
			}
		}

		private boolean addStatement(Resource subj, IRI pred, Value obj, boolean explicit, Resource... contexts)
				throws SailException {
			Objects.requireNonNull(contexts,
					"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
			boolean result = false;
			sinkStoreAccessLock.lock();
			try {
				startTriplestoreTransaction();
				int subjID = storeValueId(subj);
				int predID = storeValueId(pred);
				int objID = storeValueId(obj);

				if (contexts.length == 0) {
					contexts = new Resource[] { null };
				}

				for (Resource context : contexts) {
					int contextID = 0;
					if (context != null) {
						contextID = storeValueId(context);
					}

					boolean wasNew = tripleStore.storeTriple(subjID, predID, objID, contextID, explicit);
					if (wasNew && context != null) {
						contextStore.increment(context);
					}
					result |= wasNew;
				}
			} catch (IOException e) {
				throw new SailException(e);
			} catch (RuntimeException e) {
				if (e instanceof SailException) {
					throw e;
				}
				logger.error("Encountered an unexpected problem while trying to add a statement", e);
				throw new SailException(e);
			} finally {
				sinkStoreAccessLock.unlock();
			}

			return result;
		}

		private long removeStatements(Resource subj, IRI pred, Value obj, boolean explicit, Resource... contexts)
				throws SailException {
			Objects.requireNonNull(contexts,
					"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

			sinkStoreAccessLock.lock();
			try {
				startTriplestoreTransaction();
				int subjID = NativeValue.UNKNOWN_ID;
				if (subj != null) {
					subjID = valueStore.getID(subj);
					if (subjID == NativeValue.UNKNOWN_ID) {
						return 0;
					}
				}
				int predID = NativeValue.UNKNOWN_ID;
				if (pred != null) {
					predID = valueStore.getID(pred);
					if (predID == NativeValue.UNKNOWN_ID) {
						return 0;
					}
				}
				int objID = NativeValue.UNKNOWN_ID;
				if (obj != null) {
					objID = valueStore.getID(obj);
					if (objID == NativeValue.UNKNOWN_ID) {
						return 0;
					}
				}

				final int[] contextIds = new int[contexts.length == 0 ? 1 : contexts.length];
				if (contexts.length == 0) { // remove from all contexts
					contextIds[0] = NativeValue.UNKNOWN_ID;
				} else {
					for (int i = 0; i < contexts.length; i++) {
						Resource context = contexts[i];
						if (context == null) {
							contextIds[i] = 0;
						} else {
							int id = valueStore.getID(context);
							// unknown_id cannot be used (would result in removal from all contexts)
							contextIds[i] = (id != NativeValue.UNKNOWN_ID) ? id : Integer.MIN_VALUE;
						}
					}
				}

				long removeCount = 0;
				for (int contextId : contextIds) {
					Map<Integer, Long> result = tripleStore.removeTriplesByContext(subjID, predID, objID, contextId,
							explicit);

					for (Entry<Integer, Long> entry : result.entrySet()) {
						Integer entryContextId = entry.getKey();
						if (entryContextId > 0) {
							Resource modifiedContext = (Resource) valueStore.getValue(entryContextId);
							contextStore.decrementBy(modifiedContext, entry.getValue());
						}
						removeCount += entry.getValue();
					}
				}
				return removeCount;
			} catch (IOException e) {
				throw new SailException(e);
			} catch (RuntimeException e) {
				logger.error("Encountered an unexpected problem while trying to remove statements", e);
				if (e instanceof SailException) {
					throw e;
				}
				// Ensure upstream handles this as a SailException so branch flush clears pending changes
				throw new SailException(e);
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public boolean deprecateByQuery(Resource subj, IRI pred, Value obj, Resource[] contexts) {
			return removeStatements(subj, pred, obj, explicit, contexts) > 0;
		}

		@Override
		public boolean supportsDeprecateByQuery() {
			return true;
		}
	}

	/**
	 * @author James Leigh
	 */
	private final class NativeSailDataset implements SailDataset {

		private final boolean explicit;

		public NativeSailDataset(boolean explicit) throws SailException {
			this.explicit = explicit;
		}

		@Override
		public void close() {
			// no-op
		}

		@Override
		public String getNamespace(String prefix) throws SailException {
			return namespaceStore.getNamespace(prefix);
		}

		@Override
		public CloseableIteration<? extends Namespace> getNamespaces() {
			return new CloseableIteratorIteration<Namespace>(namespaceStore.iterator());
		}

		@Override
		public CloseableIteration<? extends Resource> getContextIDs() throws SailException {
			return new CloseableIteratorIteration<>(contextStore.iterator());
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) throws SailException {
			try {
				return createStatementIterator(subj, pred, obj, explicit, contexts);
			} catch (IOException e) {
				throw new SailException("Unable to get statements", e);
			}
		}
	}

}
