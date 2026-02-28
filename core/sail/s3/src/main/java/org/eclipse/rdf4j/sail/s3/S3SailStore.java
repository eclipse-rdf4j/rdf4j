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
import org.eclipse.rdf4j.sail.s3.storage.FileSystemObjectStore;
import org.eclipse.rdf4j.sail.s3.storage.MemTable;
import org.eclipse.rdf4j.sail.s3.storage.MergeIterator;
import org.eclipse.rdf4j.sail.s3.storage.ObjectStore;
import org.eclipse.rdf4j.sail.s3.storage.ParquetFileBuilder;
import org.eclipse.rdf4j.sail.s3.storage.ParquetQuadSource;
import org.eclipse.rdf4j.sail.s3.storage.ParquetSchemas;
import org.eclipse.rdf4j.sail.s3.storage.QuadIndex;
import org.eclipse.rdf4j.sail.s3.storage.QuadStats;
import org.eclipse.rdf4j.sail.s3.storage.RawEntrySource;
import org.eclipse.rdf4j.sail.s3.storage.S3ObjectStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link SailStore} implementation that stores RDF quads using Parquet files on S3-compatible object storage with
 * stats-based pruning (no predicate partitioning).
 *
 * <p>
 * Architecture: single in-memory {@link MemTable} in SPOC order → on flush, write 3 Parquet files per epoch (SPOC,
 * OPSC, CSPO sort orders) → multi-tier cache (Caffeine heap + disk) → compaction.
 * </p>
 *
 * <p>
 * When S3 is not configured, operates in pure in-memory mode.
 * </p>
 */
class S3SailStore implements SailStore {

	final Logger logger = LoggerFactory.getLogger(S3SailStore.class);

	private static final QuadIndex SPOC_INDEX = new QuadIndex("spoc");
	private static final QuadIndex OPSC_INDEX = new QuadIndex("opsc");
	private static final QuadIndex CSPO_INDEX = new QuadIndex("cspo");
	private static final List<QuadIndex> ALL_INDEXES = List.of(SPOC_INDEX, OPSC_INDEX, CSPO_INDEX);

	private static final int DEFAULT_ROW_GROUP_SIZE = 8 * 1024 * 1024; // 8 MiB
	private static final int DEFAULT_PAGE_SIZE = 64 * 1024; // 64 KiB

	private final S3ValueStore valueStore;
	private final S3NamespaceStore namespaceStore;

	// Single MemTable in SPOC order
	private volatile MemTable memTable;
	private volatile boolean mayHaveInferred;

	// Persistence fields (null when S3 is not configured)
	private final ObjectStore objectStore;
	private final ObjectMapper jsonMapper;
	private final Catalog catalog;
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
		this(config, createObjectStore(config));
	}

	private static ObjectStore createObjectStore(S3StoreConfig config) {
		if (config.isS3Configured()) {
			return new S3ObjectStore(config.getS3Bucket(), config.getS3Endpoint(), config.getS3Region(),
					config.getS3Prefix(), config.getS3AccessKey(), config.getS3SecretKey(),
					config.isS3ForcePathStyle());
		}
		String dataDir = config.getDataDir();
		if (dataDir != null && !dataDir.isEmpty()) {
			return new FileSystemObjectStore(Path.of(dataDir));
		}
		return null; // in-memory only
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
		this.memTable = new MemTable(SPOC_INDEX);

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
			this.catalog = null;
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
	 * Flushes active MemTable to Parquet files on the object store. Writes one file per sort order (SPOC, OPSC, CSPO).
	 */
	private void flushToObjectStore() {
		if (objectStore == null) {
			return;
		}

		long epoch = epochCounter.getAndIncrement();

		if (memTable.size() > 0) {
			// Freeze active MemTable and swap in fresh one
			MemTable frozen = memTable;
			frozen.freeze();
			memTable = new MemTable(SPOC_INDEX);

			List<long[]> allQuads = collectQuads(frozen);
			QuadStats stats = QuadStats.fromQuads(allQuads);
			writeParquetFiles(epoch, allQuads, stats);
		}

		persistMetadata(epoch);
		runCompactionIfNeeded();
	}

	private static List<long[]> collectQuads(MemTable frozen) {
		List<long[]> allQuads = new ArrayList<>(frozen.size());
		long[] quad = new long[4];
		for (Map.Entry<byte[], byte[]> entry : frozen.getData().entrySet()) {
			long[] q = new long[5]; // s, p, o, c, flag
			frozen.getIndex().keyToQuad(entry.getKey(), quad);
			q[0] = quad[QuadIndex.SUBJ_IDX];
			q[1] = quad[QuadIndex.PRED_IDX];
			q[2] = quad[QuadIndex.OBJ_IDX];
			q[3] = quad[QuadIndex.CONTEXT_IDX];
			q[4] = entry.getValue()[0];
			allQuads.add(q);
		}
		return allQuads;
	}

	private void writeParquetFiles(long epoch, List<long[]> allQuads, QuadStats stats) {
		for (QuadIndex sortIndex : ALL_INDEXES) {
			String sortSuffix = sortIndex.getFieldSeqString();
			List<ParquetFileBuilder.QuadEntry> sorted = sortQuadEntries(allQuads, sortIndex);

			ParquetSchemas.SortOrder sortOrder = ParquetSchemas.SortOrder.fromSuffix(sortSuffix);
			byte[] parquetData = ParquetFileBuilder.build(sorted, ParquetSchemas.QUAD_SCHEMA,
					sortOrder, rowGroupSize, pageSize);

			String s3Key = "data/L0-" + String.format("%05d", epoch) + "-" + sortSuffix + ".parquet";
			objectStore.put(s3Key, parquetData);

			if (cache != null) {
				cache.writeThrough(s3Key, parquetData);
			}

			catalog.addFile(new Catalog.ParquetFileInfo(
					s3Key, 0, sortSuffix, sorted.size(), epoch, parquetData.length, stats));
		}
	}

	private void persistMetadata(long epoch) {
		valueStore.serialize(objectStore);
		namespaceStore.serialize(objectStore, jsonMapper);
		catalog.setNextValueId(valueStore.getNextId());
		catalog.setEpoch(epoch);
		catalog.save(objectStore, jsonMapper, epoch);
	}

	/**
	 * Sorts quad entries according to the given sort index.
	 */
	private static List<ParquetFileBuilder.QuadEntry> sortQuadEntries(List<long[]> quads, QuadIndex sortIndex) {
		List<long[]> sorted = new ArrayList<>(quads);
		String seq = sortIndex.getFieldSeqString();
		sorted.sort((a, b) -> {
			for (int i = 0; i < 4; i++) {
				int idx = QuadIndex.fieldCharToIdx(seq.charAt(i));
				int cmp = Long.compare(a[idx], b[idx]);
				if (cmp != 0) {
					return cmp;
				}
			}
			return 0;
		});

		List<ParquetFileBuilder.QuadEntry> result = new ArrayList<>(sorted.size());
		for (long[] q : sorted) {
			result.add(new ParquetFileBuilder.QuadEntry(q[0], q[1], q[2], q[3], (byte) q[4]));
		}
		return result;
	}

	/**
	 * Checks compaction triggers and runs compaction if needed.
	 */
	private void runCompactionIfNeeded() {
		if (compactionPolicy == null || compactor == null) {
			return;
		}

		List<Catalog.ParquetFileInfo> files = catalog.getFiles();

		// L0→L1 compaction
		if (compactionPolicy.shouldCompact(files, 0)) {
			List<Catalog.ParquetFileInfo> l0Files = CompactionPolicy.filesAtLevel(files, 0);
			long compactEpoch = epochCounter.getAndIncrement();
			compactor.compact(l0Files, 0, 1, compactEpoch, catalog);
			files = catalog.getFiles();
		}

		// L1→L2 compaction
		if (compactionPolicy.shouldCompact(files, 1)) {
			List<Catalog.ParquetFileInfo> l1Files = CompactionPolicy.filesAtLevel(files, 1);
			long compactEpoch = epochCounter.getAndIncrement();
			compactor.compact(l1Files, 1, 2, compactEpoch, catalog);
		}

		// Save catalog after compaction
		long epoch = epochCounter.getAndIncrement();
		catalog.setEpoch(epoch);
		catalog.save(objectStore, jsonMapper, epoch);
	}

	private boolean hasPersistence() {
		return objectStore != null;
	}

	/**
	 * Queries quads using the best available source (merged Parquet + MemTable, or MemTable only).
	 */
	private Iterator<long[]> queryQuads(long s, long p, long o, long c, boolean explicit) {
		return hasPersistence()
				? createMergedIterator(s, p, o, c, explicit)
				: memTable.scan(s, p, o, c, explicit);
	}

	/**
	 * Resolves a Value to its stored ID. Returns UNKNOWN_ID if the value is null, or the stored ID (which may be
	 * UNKNOWN_ID if the value is not in the store).
	 */
	private long resolveValueId(Value value) {
		if (value == null) {
			return S3ValueStore.UNKNOWN_ID;
		}
		return valueStore.getId(value);
	}

	/**
	 * Creates a statement iterator for the given pattern using stats-based pruning.
	 */
	CloseableIteration<? extends Statement> createStatementIterator(
			Resource subj, IRI pred, Value obj, boolean explicit, Resource... contexts) {

		if (!explicit && !mayHaveInferred) {
			return new EmptyIteration<>();
		}

		long subjID = resolveValueId(subj);
		if (subj != null && subjID == S3ValueStore.UNKNOWN_ID) {
			return new EmptyIteration<>();
		}

		long predID = resolveValueId(pred);
		if (pred != null && predID == S3ValueStore.UNKNOWN_ID) {
			return new EmptyIteration<>();
		}

		long objID = resolveValueId(obj);
		if (obj != null && objID == S3ValueStore.UNKNOWN_ID) {
			return new EmptyIteration<>();
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

		ArrayList<CloseableIteration<? extends Statement>> perContextIterList = new ArrayList<>(contextIDList.size());

		for (long contextID : contextIDList) {
			Iterator<long[]> quads = queryQuads(subjID, predID, objID, contextID, explicit);
			perContextIterList.add(new QuadToStatementIteration(quads, valueStore));
		}

		if (perContextIterList.size() == 1) {
			return perContextIterList.get(0);
		} else {
			return new UnionIteration<>(perContextIterList);
		}
	}

	/**
	 * Creates a merged iterator across MemTable and Parquet files for a given pattern. Selects the best QuadIndex,
	 * prunes files using catalog stats, and merges all sources.
	 */
	private Iterator<long[]> createMergedIterator(long subjID, long predID, long objID, long contextID,
			boolean explicit) {

		byte expectedFlag = explicit ? MemTable.FLAG_EXPLICIT : MemTable.FLAG_INFERRED;

		// Select best index for the query pattern
		QuadIndex bestIndex = QuadIndex.getBestIndex(ALL_INDEXES, subjID, predID, objID, contextID);
		String sortSuffix = bestIndex.getFieldSeqString();

		// Build sources: MemTable (newest) + Parquet files (newest epoch first)
		List<RawEntrySource> sources = new ArrayList<>();

		// MemTable source (always newest) — re-encoded in the best index order
		sources.add(memTable.asRawSource(bestIndex, subjID, predID, objID, contextID));

		// Parquet files for the selected sort order
		List<Catalog.ParquetFileInfo> sortOrderFiles = catalog.getFilesForSortOrder(sortSuffix);
		sortOrderFiles = sortOrderFiles.stream()
				.sorted(Comparator.comparingLong(Catalog.ParquetFileInfo::getEpoch).reversed())
				.toList();

		for (Catalog.ParquetFileInfo fileInfo : sortOrderFiles) {
			// Catalog-level pruning using per-file stats
			if (subjID >= 0 && (subjID < fileInfo.getMinSubject() || subjID > fileInfo.getMaxSubject())) {
				continue;
			}
			if (predID >= 0 && (predID < fileInfo.getMinPredicate() || predID > fileInfo.getMaxPredicate())) {
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

			sources.add(new ParquetQuadSource(fileData, bestIndex, subjID, predID, objID, contextID));
		}

		return new MergeIterator(sources, bestIndex, expectedFlag, subjID, predID, objID, contextID);
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
				long subjID = resolveValueId(subj);
				if (subj != null && subjID == S3ValueStore.UNKNOWN_ID) {
					return 0;
				}

				long predID = resolveValueId(pred);
				if (pred != null && predID == S3ValueStore.UNKNOWN_ID) {
					return 0;
				}

				long objID = resolveValueId(obj);
				if (obj != null && objID == S3ValueStore.UNKNOWN_ID) {
					return 0;
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
					Iterator<long[]> iter = queryQuads(subjID, predID, objID, contextId, explicit);

					// Buffer results before removing to avoid ConcurrentModificationException
					// when the iterator is backed by the MemTable's own map
					List<long[]> toRemove = new ArrayList<>();
					while (iter.hasNext()) {
						toRemove.add(iter.next());
					}
					for (long[] quad : toRemove) {
						memTable.remove(quad[0], quad[1], quad[2], quad[3]);
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
			Iterator<long[]> allQuads = queryQuads(-1, -1, -1, -1, explicit);

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
}
