/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
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
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.s3.cache.TieredCache;
import org.eclipse.rdf4j.sail.s3.config.S3StoreConfig;
import org.eclipse.rdf4j.sail.s3.storage.Catalog;
import org.eclipse.rdf4j.sail.s3.storage.CompactionPolicy;
import org.eclipse.rdf4j.sail.s3.storage.Compactor;
import org.eclipse.rdf4j.sail.s3.storage.MemTable;
import org.eclipse.rdf4j.sail.s3.storage.ObjectStore;
import org.eclipse.rdf4j.sail.s3.storage.ParquetFileBuilder;
import org.eclipse.rdf4j.sail.s3.storage.ParquetQuadSource;
import org.eclipse.rdf4j.sail.s3.storage.ParquetSchemas;
import org.eclipse.rdf4j.sail.s3.storage.PartitionIndexSelector;
import org.eclipse.rdf4j.sail.s3.storage.PartitionMergeIterator;
import org.eclipse.rdf4j.sail.s3.storage.QuadIndex;
import org.eclipse.rdf4j.sail.s3.storage.RawEntrySource;
import org.eclipse.rdf4j.sail.s3.storage.S3ObjectStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link SailStore} implementation that stores RDF quads using Parquet files on S3-compatible object storage with
 * predicate-based vertical partitioning.
 *
 * <p>
 * Architecture: single in-memory {@link MemTable} in SPOC order → on flush, partition by predicate and write 3 Parquet
 * files per partition (SOC, OSC, CSO sort orders) → multi-tier cache (Caffeine heap + disk) → compaction.
 * </p>
 *
 * <p>
 * When S3 is not configured, operates in pure in-memory mode.
 * </p>
 */
class S3SailStore implements SailStore {

	final Logger logger = LoggerFactory.getLogger(S3SailStore.class);

	private static final String[] SORT_ORDERS = { "soc", "osc", "cso" };
	private static final int DEFAULT_ROW_GROUP_SIZE = 8 * 1024 * 1024; // 8 MiB
	private static final int DEFAULT_PAGE_SIZE = 64 * 1024; // 64 KiB

	private final S3ValueStore valueStore;
	private final S3NamespaceStore namespaceStore;

	// Single MemTable in SPOC order (new design: 1x memory, partition on flush)
	private final QuadIndex spocIndex;
	private volatile MemTable memTable;
	private volatile boolean mayHaveInferred;

	// Persistence fields (null when S3 is not configured)
	private final ObjectStore objectStore;
	private final ObjectMapper jsonMapper;
	private Catalog catalog;
	private final AtomicLong epochCounter;
	private final long memTableFlushSize;
	private final TieredCache cache;
	private final CompactionPolicy compactionPolicy;
	private final Compactor compactor;
	private final int rowGroupSize;
	private final int pageSize;

	/**
	 * A lock to control concurrent access by {@link S3SailSink} to the stores.
	 */
	private final ReentrantLock sinkStoreAccessLock = new ReentrantLock();

	S3SailStore(S3StoreConfig config) {
		this(config, config.isS3Configured()
				? new S3ObjectStore(config.getS3Bucket(), config.getS3Endpoint(), config.getS3Region(),
						config.getS3Prefix(), config.getS3AccessKey(), config.getS3SecretKey(),
						config.isS3ForcePathStyle())
				: null);
	}

	/**
	 * Package-private constructor for testing with a custom ObjectStore.
	 */
	S3SailStore(S3StoreConfig config, ObjectStore objectStore) {
		this.valueStore = new S3ValueStore();
		this.namespaceStore = new S3NamespaceStore();
		this.objectStore = objectStore;
		this.memTableFlushSize = config.getMemTableSize();
		this.rowGroupSize = DEFAULT_ROW_GROUP_SIZE;
		this.pageSize = DEFAULT_PAGE_SIZE;

		// Single SPOC index for the MemTable
		this.spocIndex = new QuadIndex("spoc");
		this.memTable = new MemTable(spocIndex);

		// Initialize persistence
		if (objectStore != null) {
			this.jsonMapper = new ObjectMapper();
			this.catalog = Catalog.load(objectStore, jsonMapper);
			this.epochCounter = new AtomicLong(catalog.getEpoch() + 1);

			// Initialize cache
			Path diskCachePath = config.getDiskCachePath() != null ? Path.of(config.getDiskCachePath()) : null;
			this.cache = new TieredCache(config.getMemoryCacheSize(), diskCachePath,
					config.getDiskCacheSize(), objectStore);

			this.compactionPolicy = new CompactionPolicy();
			this.compactor = new Compactor(objectStore, cache, rowGroupSize, pageSize);

			// Deserialize value store and namespaces
			if (catalog.getNextValueId() > 0) {
				valueStore.deserialize(objectStore, catalog.getNextValueId());
			}
			namespaceStore.deserialize(objectStore, jsonMapper);
		} else {
			this.jsonMapper = null;
			this.epochCounter = null;
			this.cache = null;
			this.compactionPolicy = null;
			this.compactor = null;
		}
	}

	@Override
	public ValueFactory getValueFactory() {
		return valueStore;
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return new S3EvaluationStatistics();
	}

	@Override
	public SailSource getExplicitSailSource() {
		return new S3SailSource(true);
	}

	@Override
	public SailSource getInferredSailSource() {
		return new S3SailSource(false);
	}

	@Override
	public void close() throws SailException {
		try {
			if (objectStore != null) {
				flushToObjectStore();
				if (cache != null) {
					cache.close();
				}
				objectStore.close();
			}
		} catch (IOException e) {
			throw new SailException(e);
		}
		valueStore.close();
		memTable.clear();
	}

	/**
	 * Flushes active MemTable to Parquet files on the object store, partitioned by predicate.
	 */
	private void flushToObjectStore() {
		if (objectStore == null) {
			return;
		}

		if (memTable.size() == 0) {
			// Still persist value store and namespaces
			long epoch = epochCounter.getAndIncrement();
			valueStore.serialize(objectStore);
			namespaceStore.serialize(objectStore, jsonMapper);
			catalog.setNextValueId(valueStore.getNextId());
			catalog.setEpoch(epoch);
			catalog.save(objectStore, jsonMapper, epoch);
			return;
		}

		long epoch = epochCounter.getAndIncrement();

		// Freeze active MemTable and swap in fresh one
		MemTable frozen = memTable;
		frozen.freeze();
		memTable = new MemTable(spocIndex);

		// Partition by predicate
		Map<Long, List<MemTable.QuadEntry>> partitions = frozen.partitionByPredicate();

		// For each predicate partition, write 3 Parquet files (all sort orders)
		for (Map.Entry<Long, List<MemTable.QuadEntry>> partEntry : partitions.entrySet()) {
			long predId = partEntry.getKey();
			List<MemTable.QuadEntry> entries = partEntry.getValue();

			// Set predicate label for debugging
			Value predValue = valueStore.getValue(predId);
			if (predValue != null) {
				catalog.getPredicateLabels().put(String.valueOf(predId), predValue.stringValue());
			}

			for (String sortOrder : SORT_ORDERS) {
				// Sort entries according to sort order
				List<MemTable.QuadEntry> sorted = sortEntries(entries, sortOrder);

				// Build Parquet file
				List<ParquetFileBuilder.QuadEntry> pqEntries = new ArrayList<>(sorted.size());
				for (MemTable.QuadEntry e : sorted) {
					pqEntries.add(new ParquetFileBuilder.QuadEntry(e.subject, e.object, e.context, e.flag));
				}

				byte[] parquetData = ParquetFileBuilder.build(pqEntries, ParquetSchemas.PARTITIONED_SCHEMA,
						ParquetSchemas.SortOrder.fromSuffix(sortOrder), predId, rowGroupSize, pageSize);

				String s3Key = "data/predicates/" + predId + "/L0-"
						+ String.format("%05d", epoch) + "-" + sortOrder + ".parquet";

				objectStore.put(s3Key, parquetData);

				// Write-through to cache
				if (cache != null) {
					cache.writeThrough(s3Key, parquetData);
				}

				// Compute stats
				long minSubject = Long.MAX_VALUE, maxSubject = Long.MIN_VALUE;
				long minObject = Long.MAX_VALUE, maxObject = Long.MIN_VALUE;
				long minContext = Long.MAX_VALUE, maxContext = Long.MIN_VALUE;
				for (MemTable.QuadEntry e : sorted) {
					minSubject = Math.min(minSubject, e.subject);
					maxSubject = Math.max(maxSubject, e.subject);
					minObject = Math.min(minObject, e.object);
					maxObject = Math.max(maxObject, e.object);
					minContext = Math.min(minContext, e.context);
					maxContext = Math.max(maxContext, e.context);
				}

				catalog.addFile(predId, new Catalog.ParquetFileInfo(
						s3Key, 0, sortOrder, sorted.size(), epoch, parquetData.length,
						minSubject, maxSubject, minObject, maxObject, minContext, maxContext));
			}
		}

		// Persist value store and namespaces
		valueStore.serialize(objectStore);
		namespaceStore.serialize(objectStore, jsonMapper);

		// Atomic catalog update
		catalog.setNextValueId(valueStore.getNextId());
		catalog.setEpoch(epoch);
		catalog.save(objectStore, jsonMapper, epoch);

		// Check compaction triggers
		runCompactionIfNeeded();
	}

	/**
	 * Sorts entries according to the given sort order.
	 */
	private static List<MemTable.QuadEntry> sortEntries(List<MemTable.QuadEntry> entries, String sortOrder) {
		List<MemTable.QuadEntry> sorted = new ArrayList<>(entries);
		Comparator<MemTable.QuadEntry> cmp;
		switch (sortOrder) {
		case "osc":
			cmp = Comparator.comparingLong((MemTable.QuadEntry e) -> e.object)
					.thenComparingLong(e -> e.subject)
					.thenComparingLong(e -> e.context);
			break;
		case "cso":
			cmp = Comparator.comparingLong((MemTable.QuadEntry e) -> e.context)
					.thenComparingLong(e -> e.subject)
					.thenComparingLong(e -> e.object);
			break;
		case "soc":
		default:
			cmp = Comparator.comparingLong((MemTable.QuadEntry e) -> e.subject)
					.thenComparingLong(e -> e.object)
					.thenComparingLong(e -> e.context);
			break;
		}
		sorted.sort(cmp);
		return sorted;
	}

	/**
	 * Checks compaction triggers and runs compaction if needed.
	 */
	private void runCompactionIfNeeded() {
		if (compactionPolicy == null || compactor == null) {
			return;
		}

		for (long predId : catalog.getPredicateIds()) {
			List<Catalog.ParquetFileInfo> files = catalog.getFilesForPredicate(predId);

			// L0→L1 compaction
			if (compactionPolicy.shouldCompactL0(files)) {
				List<Catalog.ParquetFileInfo> l0Files = CompactionPolicy.filesAtLevel(files, 0);
				long compactEpoch = epochCounter.getAndIncrement();
				compactor.compact(predId, l0Files, 0, 1, compactEpoch, catalog);

				// Re-fetch files after compaction and check L1→L2
				files = catalog.getFilesForPredicate(predId);
			}

			// L1→L2 compaction
			if (compactionPolicy.shouldCompactL1(files)) {
				List<Catalog.ParquetFileInfo> l1Files = CompactionPolicy.filesAtLevel(files, 1);
				long compactEpoch = epochCounter.getAndIncrement();
				compactor.compact(predId, l1Files, 1, 2, compactEpoch, catalog);
			}
		}

		// Save catalog after compaction
		long epoch = epochCounter.getAndIncrement();
		catalog.setEpoch(epoch);
		catalog.save(objectStore, jsonMapper, epoch);
	}

	/**
	 * Creates a statement iterator for the given pattern using predicate partitioning.
	 */
	CloseableIteration<? extends Statement> createStatementIterator(
			Resource subj, IRI pred, Value obj, boolean explicit, Resource... contexts) {

		if (!explicit && !mayHaveInferred) {
			return new EmptyIteration<>();
		}

		long subjID = S3ValueStore.UNKNOWN_ID;
		if (subj != null) {
			subjID = valueStore.getId(subj);
			if (subjID == S3ValueStore.UNKNOWN_ID) {
				return new EmptyIteration<>();
			}
		}

		long predID = S3ValueStore.UNKNOWN_ID;
		if (pred != null) {
			predID = valueStore.getId(pred);
			if (predID == S3ValueStore.UNKNOWN_ID) {
				return new EmptyIteration<>();
			}
		}

		long objID = S3ValueStore.UNKNOWN_ID;
		if (obj != null) {
			objID = valueStore.getId(obj);
			if (objID == S3ValueStore.UNKNOWN_ID) {
				return new EmptyIteration<>();
			}
		}

		List<Long> contextIDList = new ArrayList<>(contexts.length == 0 ? 1 : contexts.length);
		if (contexts.length == 0) {
			contextIDList.add(S3ValueStore.UNKNOWN_ID);
		} else {
			for (Resource context : contexts) {
				if (context == null) {
					contextIDList.add(0L);
				} else if (!context.isTriple()) {
					long contextID = valueStore.getId(context);
					if (contextID != S3ValueStore.UNKNOWN_ID) {
						contextIDList.add(contextID);
					}
				}
			}
		}

		if (contextIDList.isEmpty()) {
			return new EmptyIteration<>();
		}

		boolean hasPersistence = objectStore != null && catalog != null;

		ArrayList<CloseableIteration<? extends Statement>> perContextIterList = new ArrayList<>(contextIDList.size());

		for (long contextID : contextIDList) {
			Iterator<long[]> quads;
			if (hasPersistence) {
				quads = createMergedIterator(subjID, predID, objID, contextID, explicit);
			} else {
				quads = memTable.scan(subjID, predID, objID, contextID, explicit);
			}
			perContextIterList.add(new QuadToStatementIteration(quads, valueStore));
		}

		if (perContextIterList.size() == 1) {
			return perContextIterList.get(0);
		} else {
			return new UnionIteration<>(perContextIterList);
		}
	}

	/**
	 * Creates a merged iterator across MemTable and Parquet files for a given pattern.
	 */
	private Iterator<long[]> createMergedIterator(long subjID, long predID, long objID, long contextID,
			boolean explicit) {

		boolean subjectBound = subjID >= 0;
		boolean objectBound = objID >= 0;
		boolean contextBound = contextID >= 0;

		// Select best sort order for within-partition queries
		String bestSortOrder = PartitionIndexSelector.selectSortOrder(subjectBound, objectBound, contextBound);

		if (predID >= 0) {
			// Predicate bound → single partition
			return createPartitionIterator(predID, subjID, objID, contextID, bestSortOrder, explicit);
		} else {
			// Predicate unbound → fan out to all partitions
			Set<Long> predIds = catalog.getPredicateIds();
			if (predIds.isEmpty()) {
				// Only MemTable data
				return memTable.scan(subjID, predID, objID, contextID, explicit);
			}

			List<Iterator<long[]>> partitionIters = new ArrayList<>();
			for (long pid : predIds) {
				partitionIters.add(createPartitionIterator(pid, subjID, objID, contextID, bestSortOrder, explicit));
			}

			// Union all partitions (each partition's iterator handles dedup internally)
			return new UnionIterator(partitionIters);
		}
	}

	/**
	 * Creates a merged iterator for a single predicate partition. All sources produce 3-varint keys in the partition
	 * sort order (predicate is implicit in the partition).
	 */
	private Iterator<long[]> createPartitionIterator(long predId, long subjID, long objID, long contextID,
			String sortOrder, boolean explicit) {

		byte expectedFlag = explicit ? MemTable.FLAG_EXPLICIT : MemTable.FLAG_INFERRED;

		// Build sources: MemTable (newest) + Parquet files (newest epoch first)
		// All sources produce 3-varint keys in the same partition sort order
		List<RawEntrySource> sources = new ArrayList<>();

		// MemTable source (always newest) — re-encoded as 3-varint partition keys
		sources.add(memTable.asPartitionRawSource(predId, subjID, objID, contextID, sortOrder));

		// Parquet files for this predicate partition and sort order
		List<Catalog.ParquetFileInfo> files = catalog.getFilesForPredicate(predId);
		List<Catalog.ParquetFileInfo> sortOrderFiles = files.stream()
				.filter(f -> sortOrder.equals(f.getSortOrder()))
				.sorted(Comparator.comparingLong(Catalog.ParquetFileInfo::getEpoch).reversed())
				.toList();

		for (Catalog.ParquetFileInfo fileInfo : sortOrderFiles) {
			// Catalog-level pruning using per-file stats
			if (subjID >= 0 && (subjID < fileInfo.getMinSubject() || subjID > fileInfo.getMaxSubject())) {
				continue;
			}
			if (objID >= 0 && (objID < fileInfo.getMinObject() || objID > fileInfo.getMaxObject())) {
				continue;
			}
			if (contextID >= 0 && (contextID < fileInfo.getMinContext() || contextID > fileInfo.getMaxContext())) {
				continue;
			}

			byte[] fileData = cache != null ? cache.get(fileInfo.getS3Key()) : objectStore.get(fileInfo.getS3Key());
			if (fileData == null) {
				logger.warn("Missing Parquet file: {}", fileInfo.getS3Key());
				continue;
			}

			sources.add(new ParquetQuadSource(fileData, sortOrder, subjID, objID, contextID));
		}

		// Use PartitionMergeIterator: all sources produce 3-varint keys, predicate injected on decode
		return new PartitionMergeIterator(sources, predId, sortOrder, expectedFlag, subjID, objID, contextID);
	}

	// =========================================================================
	// Inner classes
	// =========================================================================

	private final class S3SailSource extends BackingSailSource {

		private final boolean explicit;

		S3SailSource(boolean explicit) {
			this.explicit = explicit;
		}

		@Override
		public SailSource fork() {
			throw new UnsupportedOperationException("This store does not support multiple datasets");
		}

		@Override
		public SailSink sink(IsolationLevel level) throws SailException {
			return new S3SailSink(explicit);
		}

		@Override
		public SailDataset dataset(IsolationLevel level) throws SailException {
			return new S3SailDataset(explicit);
		}
	}

	private final class S3SailSink implements SailSink {

		private final boolean explicit;

		S3SailSink(boolean explicit) {
			this.explicit = explicit;
		}

		@Override
		public void close() {
			// no-op
		}

		@Override
		public void prepare() throws SailException {
			// serializable is not supported at this level
		}

		@Override
		public void flush() throws SailException {
			sinkStoreAccessLock.lock();
			try {
				flushToObjectStore();
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void setNamespace(String prefix, String name) throws SailException {
			sinkStoreAccessLock.lock();
			try {
				namespaceStore.setNamespace(prefix, name);
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void removeNamespace(String prefix) throws SailException {
			sinkStoreAccessLock.lock();
			try {
				namespaceStore.removeNamespace(prefix);
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void clearNamespaces() throws SailException {
			sinkStoreAccessLock.lock();
			try {
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
		public void clear(Resource... contexts) throws SailException {
			removeStatements(null, null, null, explicit, contexts);
		}

		@Override
		public void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {
			addStatement(subj, pred, obj, explicit, ctx);
		}

		@Override
		public void approveAll(Set<Statement> approved, Set<Resource> approvedContexts) {
			sinkStoreAccessLock.lock();
			try {
				for (Statement statement : approved) {
					Resource subj = statement.getSubject();
					IRI pred = statement.getPredicate();
					Value obj = statement.getObject();
					Resource context = statement.getContext();

					long s = valueStore.storeValue(subj);
					long p = valueStore.storeValue(pred);
					long o = valueStore.storeValue(obj);
					long c = context == null ? 0 : valueStore.storeValue(context);

					if (!explicit) {
						mayHaveInferred = true;
					}

					memTable.put(s, p, o, c, explicit);
				}

				// Size-triggered flush
				if (objectStore != null && memTable.approximateSizeInBytes() >= memTableFlushSize) {
					flushToObjectStore();
				}
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void deprecate(Statement statement) throws SailException {
			removeStatements(statement.getSubject(), statement.getPredicate(), statement.getObject(), explicit,
					statement.getContext());
		}

		@Override
		public boolean deprecateByQuery(Resource subj, IRI pred, Value obj, Resource[] contexts) {
			return removeStatements(subj, pred, obj, explicit, contexts) > 0;
		}

		@Override
		public boolean supportsDeprecateByQuery() {
			return true;
		}

		private void addStatement(Resource subj, IRI pred, Value obj, boolean explicit, Resource context) {
			sinkStoreAccessLock.lock();
			try {
				long s = valueStore.storeValue(subj);
				long p = valueStore.storeValue(pred);
				long o = valueStore.storeValue(obj);
				long c = context == null ? 0 : valueStore.storeValue(context);

				if (!explicit) {
					mayHaveInferred = true;
				}

				memTable.put(s, p, o, c, explicit);
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		private long removeStatements(Resource subj, IRI pred, Value obj, boolean explicit, Resource... contexts) {
			Objects.requireNonNull(contexts,
					"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

			sinkStoreAccessLock.lock();
			try {
				final long subjID;
				if (subj != null) {
					subjID = valueStore.getId(subj);
					if (subjID == S3ValueStore.UNKNOWN_ID) {
						return 0;
					}
				} else {
					subjID = S3ValueStore.UNKNOWN_ID;
				}

				final long predID;
				if (pred != null) {
					predID = valueStore.getId(pred);
					if (predID == S3ValueStore.UNKNOWN_ID) {
						return 0;
					}
				} else {
					predID = S3ValueStore.UNKNOWN_ID;
				}

				final long objID;
				if (obj != null) {
					objID = valueStore.getId(obj);
					if (objID == S3ValueStore.UNKNOWN_ID) {
						return 0;
					}
				} else {
					objID = S3ValueStore.UNKNOWN_ID;
				}

				final long[] contextIds;
				if (contexts.length == 0) {
					contextIds = new long[] { S3ValueStore.UNKNOWN_ID };
				} else {
					contextIds = new long[contexts.length];
					for (int i = 0; i < contexts.length; i++) {
						Resource context = contexts[i];
						if (context == null) {
							contextIds[i] = 0;
						} else {
							long id = valueStore.getId(context);
							contextIds[i] = (id != S3ValueStore.UNKNOWN_ID) ? id : Long.MAX_VALUE;
						}
					}
				}

				long removeCount = 0;
				for (long contextId : contextIds) {
					boolean hasPersistence = objectStore != null && catalog != null;

					Iterator<long[]> iter;
					if (hasPersistence) {
						iter = createMergedIterator(subjID, predID, objID, contextId, explicit);
					} else {
						iter = memTable.scan(subjID, predID, objID, contextId, explicit);
					}

					List<long[]> toRemove = new ArrayList<>();
					while (iter.hasNext()) {
						toRemove.add(iter.next());
					}
					for (long[] quad : toRemove) {
						memTable.remove(quad[0], quad[1], quad[2], quad[3], explicit);
						removeCount++;
					}
				}

				return removeCount;
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}
	}

	private final class S3SailDataset implements SailDataset {

		private final boolean explicit;

		S3SailDataset(boolean explicit) {
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
			return new CloseableIteratorIteration<>(namespaceStore.iterator());
		}

		@Override
		public CloseableIteration<? extends Resource> getContextIDs() throws SailException {
			// Scan all quads and collect distinct non-null contexts
			boolean hasPersistence = objectStore != null && catalog != null;

			Iterator<long[]> allQuads;
			if (hasPersistence) {
				allQuads = createMergedIterator(-1, -1, -1, -1, explicit);
			} else {
				allQuads = memTable.scan(-1, -1, -1, -1, explicit);
			}

			return new FilterIteration<Resource>(
					new ConvertingIteration<long[], Resource>(
							new CloseableIteratorIteration<>(allQuads)) {
						@Override
						protected Resource convert(long[] quad) {
							if (quad[3] == 0) {
								return null;
							}
							Value val = valueStore.getValue(quad[3]);
							return val instanceof Resource ? (Resource) val : null;
						}
					}) {
				private final java.util.Set<Resource> seen = new java.util.HashSet<>();

				@Override
				protected boolean accept(Resource ctx) {
					return ctx != null && seen.add(ctx);
				}

				@Override
				protected void handleClose() {
					// no-op
				}
			};
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) throws SailException {
			return createStatementIterator(subj, pred, obj, explicit, contexts);
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(StatementOrder statementOrder, Resource subj,
				IRI pred, Value obj, Resource... contexts) throws SailException {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
			return Set.of();
		}

		@Override
		public Comparator<Value> getComparator() {
			return null;
		}
	}

	/**
	 * Converts quad ID arrays from iteration into Statement objects by resolving IDs through the ValueStore.
	 */
	static final class QuadToStatementIteration implements CloseableIteration<Statement> {

		private final Iterator<long[]> quads;
		private final S3ValueStore valueStore;

		QuadToStatementIteration(Iterator<long[]> quads, S3ValueStore valueStore) {
			this.quads = quads;
			this.valueStore = valueStore;
		}

		@Override
		public boolean hasNext() {
			return quads.hasNext();
		}

		@Override
		public Statement next() {
			long[] quad = quads.next();
			Resource subj = (Resource) valueStore.getValue(quad[0]);
			IRI pred = (IRI) valueStore.getValue(quad[1]);
			Value obj = valueStore.getValue(quad[2]);
			Resource ctx = quad[3] == 0 ? null : (Resource) valueStore.getValue(quad[3]);
			return valueStore.createStatement(subj, pred, obj, ctx);
		}

		@Override
		public void close() {
			// no-op
		}
	}

	/**
	 * Simple union iterator that concatenates multiple iterators. Used for fan-out across predicate partitions.
	 */
	private static class UnionIterator implements Iterator<long[]> {
		private final List<Iterator<long[]>> iterators;
		private int currentIdx;

		UnionIterator(List<Iterator<long[]>> iterators) {
			this.iterators = iterators;
			this.currentIdx = 0;
			advanceToNonEmpty();
		}

		private void advanceToNonEmpty() {
			while (currentIdx < iterators.size() && !iterators.get(currentIdx).hasNext()) {
				currentIdx++;
			}
		}

		@Override
		public boolean hasNext() {
			return currentIdx < iterators.size();
		}

		@Override
		public long[] next() {
			long[] result = iterators.get(currentIdx).next();
			if (!iterators.get(currentIdx).hasNext()) {
				currentIdx++;
				advanceToNonEmpty();
			}
			return result;
		}
	}
}
