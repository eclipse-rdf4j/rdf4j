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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
import org.eclipse.rdf4j.sail.s3.config.S3StoreConfig;
import org.eclipse.rdf4j.sail.s3.storage.Manifest;
import org.eclipse.rdf4j.sail.s3.storage.MemTable;
import org.eclipse.rdf4j.sail.s3.storage.MergeIterator;
import org.eclipse.rdf4j.sail.s3.storage.ObjectStore;
import org.eclipse.rdf4j.sail.s3.storage.QuadIndex;
import org.eclipse.rdf4j.sail.s3.storage.RawEntrySource;
import org.eclipse.rdf4j.sail.s3.storage.S3ObjectStore;
import org.eclipse.rdf4j.sail.s3.storage.SSTable;
import org.eclipse.rdf4j.sail.s3.storage.SSTableWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link SailStore} implementation that stores RDF quads in {@link MemTable}s with optional persistence to
 * S3-compatible object storage via SSTables. When S3 is not configured, operates in pure in-memory mode.
 */
class S3SailStore implements SailStore {

	final Logger logger = LoggerFactory.getLogger(S3SailStore.class);

	private final S3ValueStore valueStore;
	private final S3NamespaceStore namespaceStore;
	private final List<QuadIndex> indexes;
	private List<MemTable> memTables;
	private volatile boolean mayHaveInferred;

	// Persistence fields (null when S3 is not configured)
	private final ObjectStore objectStore;
	private final ObjectMapper jsonMapper;
	private Manifest manifest;
	private final List<List<SSTable>> sstablesByIndex; // per-index list, newest first
	private final AtomicLong epochCounter;
	private final long memTableFlushSize;

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

		// Parse index specifications from config
		String indexSpec = config.getQuadIndexes();
		Set<String> indexSpecs = QuadIndex.parseIndexSpecList(indexSpec);
		this.indexes = new ArrayList<>(indexSpecs.size());
		this.memTables = new ArrayList<>(indexSpecs.size());
		for (String spec : indexSpecs) {
			QuadIndex qi = new QuadIndex(spec);
			indexes.add(qi);
			memTables.add(new MemTable(qi));
		}

		if (indexes.isEmpty()) {
			QuadIndex defaultIndex = new QuadIndex("spoc");
			indexes.add(defaultIndex);
			memTables.add(new MemTable(defaultIndex));
		}

		// Initialize persistence
		if (objectStore != null) {
			this.jsonMapper = new ObjectMapper();
			this.manifest = Manifest.load(objectStore, jsonMapper);
			this.epochCounter = new AtomicLong(computeMaxEpoch(manifest) + 1);
			this.sstablesByIndex = new ArrayList<>(indexes.size());
			for (int i = 0; i < indexes.size(); i++) {
				sstablesByIndex.add(new ArrayList<>());
			}

			// Deserialize value store and namespaces
			if (manifest.getNextValueId() > 0) {
				valueStore.deserialize(objectStore, manifest.getNextValueId());
			}
			namespaceStore.deserialize(objectStore, jsonMapper);

			// Load existing SSTables from manifest
			for (Manifest.SSTableInfo info : manifest.getSstables()) {
				int idxPos = findIndexByName(info.getIndexName());
				if (idxPos >= 0) {
					byte[] sstData = objectStore.get(info.getS3Key());
					if (sstData != null) {
						SSTable sst = new SSTable(sstData, indexes.get(idxPos));
						sstablesByIndex.get(idxPos).add(sst);
					}
				}
			}
		} else {
			this.jsonMapper = null;
			this.manifest = null;
			this.epochCounter = null;
			this.sstablesByIndex = null;
		}
	}

	private static long computeMaxEpoch(Manifest manifest) {
		long max = 0;
		for (Manifest.SSTableInfo info : manifest.getSstables()) {
			if (info.getEpoch() > max) {
				max = info.getEpoch();
			}
		}
		return max;
	}

	private int findIndexByName(String indexName) {
		for (int i = 0; i < indexes.size(); i++) {
			if (indexes.get(i).getFieldSeqString().equals(indexName)) {
				return i;
			}
		}
		return -1;
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
				objectStore.close();
			}
		} catch (IOException e) {
			throw new SailException(e);
		}
		valueStore.close();
		for (MemTable mt : memTables) {
			mt.clear();
		}
	}

	/**
	 * Selects the best MemTable for the given query pattern.
	 */
	private int getBestIndex(long subj, long pred, long obj, long context) {
		int bestScore = -1;
		int bestIdx = 0;
		for (int i = 0; i < indexes.size(); i++) {
			int score = indexes.get(i).getPatternScore(subj, pred, obj, context);
			if (score > bestScore) {
				bestScore = score;
				bestIdx = i;
			}
		}
		return bestIdx;
	}

	/**
	 * Flushes active MemTables to SSTables on the object store.
	 */
	private void flushToObjectStore() {
		if (objectStore == null) {
			return;
		}

		// Check if any MemTable has data
		boolean hasMemTableData = false;
		for (MemTable mt : memTables) {
			if (mt.size() > 0) {
				hasMemTableData = true;
				break;
			}
		}

		long epoch = epochCounter.getAndIncrement();

		List<Manifest.SSTableInfo> newInfos = new ArrayList<>();

		if (hasMemTableData) {
			// Freeze active MemTables and swap in fresh ones
			List<MemTable> frozenTables = memTables;
			List<MemTable> newTables = new ArrayList<>(indexes.size());
			for (int i = 0; i < indexes.size(); i++) {
				frozenTables.get(i).freeze();
				newTables.add(new MemTable(indexes.get(i)));
			}
			memTables = newTables;

			// Write each frozen MemTable as an SSTable
			for (int i = 0; i < indexes.size(); i++) {
				MemTable frozen = frozenTables.get(i);
				if (frozen.size() == 0) {
					continue;
				}
				String indexName = indexes.get(i).getFieldSeqString();
				String s3Key = "sstables/L0-" + epoch + "-" + indexName + ".sst";

				byte[] sstData = SSTableWriter.write(frozen);
				objectStore.put(s3Key, sstData);

				SSTable sst = new SSTable(sstData, indexes.get(i));
				sstablesByIndex.get(i).add(0, sst); // prepend (newest first)

				newInfos.add(new Manifest.SSTableInfo(
						s3Key, 0, indexName,
						bytesToHex(sst.getMinKey()), bytesToHex(sst.getMaxKey()),
						sst.getEntryCount(), epoch));
			}
		}

		// Always persist value store and namespaces
		valueStore.serialize(objectStore);
		namespaceStore.serialize(objectStore, jsonMapper);

		// Update and save manifest
		List<Manifest.SSTableInfo> allInfos = new ArrayList<>(newInfos);
		allInfos.addAll(manifest.getSstables());
		manifest.setSstables(allInfos);
		manifest.setNextValueId(valueStore.getNextId());
		manifest.save(objectStore, jsonMapper, epoch);
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b & 0xFF));
		}
		return sb.toString();
	}

	/**
	 * Creates a statement iterator for the given pattern.
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

		int bestIdx = getBestIndex(subjID, predID, objID,
				contextIDList.size() == 1 ? contextIDList.get(0) : S3ValueStore.UNKNOWN_ID);

		boolean hasSSTables = sstablesByIndex != null && !sstablesByIndex.get(bestIdx).isEmpty();

		ArrayList<CloseableIteration<? extends Statement>> perContextIterList = new ArrayList<>(contextIDList.size());

		for (long contextID : contextIDList) {
			Iterator<long[]> quads;
			if (hasSSTables) {
				// Build merged source: MemTable (newest) + SSTables (newest first)
				List<RawEntrySource> sources = new ArrayList<>();
				sources.add(memTables.get(bestIdx).asRawSource(subjID, predID, objID, contextID));
				for (SSTable sst : sstablesByIndex.get(bestIdx)) {
					sources.add(sst.asRawSource(subjID, predID, objID, contextID));
				}
				byte expectedFlag = explicit ? MemTable.FLAG_EXPLICIT : MemTable.FLAG_INFERRED;
				quads = new MergeIterator(sources, indexes.get(bestIdx), expectedFlag,
						subjID, predID, objID, contextID);
			} else {
				quads = memTables.get(bestIdx).scan(subjID, predID, objID, contextID, explicit);
			}
			perContextIterList.add(new QuadToStatementIteration(quads, valueStore));
		}

		if (perContextIterList.size() == 1) {
			return perContextIterList.get(0);
		} else {
			return new UnionIteration<>(perContextIterList);
		}
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

					for (MemTable mt : memTables) {
						mt.put(s, p, o, c, explicit);
					}
				}

				// Size-triggered flush
				if (objectStore != null) {
					long totalSize = 0;
					for (MemTable mt : memTables) {
						totalSize += mt.approximateSizeInBytes();
					}
					if (totalSize >= memTableFlushSize) {
						flushToObjectStore();
					}
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

				for (MemTable mt : memTables) {
					mt.put(s, p, o, c, explicit);
				}
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

				int bestIdx = getBestIndex(subjID, predID, objID,
						contextIds.length == 1 ? contextIds[0] : S3ValueStore.UNKNOWN_ID);
				MemTable scanTable = memTables.get(bestIdx);

				long removeCount = 0;
				for (long contextId : contextIds) {
					// When SSTables exist, use merged iterator for remove scan
					Iterator<long[]> iter;
					boolean hasSSTables = sstablesByIndex != null && !sstablesByIndex.get(bestIdx).isEmpty();
					if (hasSSTables) {
						List<RawEntrySource> sources = new ArrayList<>();
						sources.add(scanTable.asRawSource(subjID, predID, objID, contextId));
						for (SSTable sst : sstablesByIndex.get(bestIdx)) {
							sources.add(sst.asRawSource(subjID, predID, objID, contextId));
						}
						byte expectedFlag = explicit ? MemTable.FLAG_EXPLICIT : MemTable.FLAG_INFERRED;
						iter = new MergeIterator(sources, indexes.get(bestIdx), expectedFlag,
								subjID, predID, objID, contextId);
					} else {
						iter = scanTable.scan(subjID, predID, objID, contextId, explicit);
					}

					List<long[]> toRemove = new ArrayList<>();
					while (iter.hasNext()) {
						toRemove.add(iter.next());
					}
					for (long[] quad : toRemove) {
						for (MemTable mt : memTables) {
							mt.remove(quad[0], quad[1], quad[2], quad[3], explicit);
						}
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
			// Use the merged read path (createStatementIterator covers this)
			int bestIdx = 0; // use first index for full scan
			boolean hasSSTables = sstablesByIndex != null && !sstablesByIndex.get(bestIdx).isEmpty();

			Iterator<long[]> allQuads;
			if (hasSSTables) {
				List<RawEntrySource> sources = new ArrayList<>();
				sources.add(memTables.get(bestIdx).asRawSource(-1, -1, -1, -1));
				for (SSTable sst : sstablesByIndex.get(bestIdx)) {
					sources.add(sst.asRawSource(-1, -1, -1, -1));
				}
				byte expectedFlag = explicit ? MemTable.FLAG_EXPLICIT : MemTable.FLAG_INFERRED;
				allQuads = new MergeIterator(sources, indexes.get(bestIdx), expectedFlag, -1, -1, -1, -1);
			} else {
				allQuads = memTables.get(bestIdx).scan(-1, -1, -1, -1, explicit);
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
}
