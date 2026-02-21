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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import org.eclipse.rdf4j.sail.s3.storage.MemTable;
import org.eclipse.rdf4j.sail.s3.storage.QuadIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory {@link SailStore} implementation that stores RDF quads in {@link MemTable}s. Each configured index
 * permutation gets its own MemTable for efficient query patterns.
 *
 * <p>
 * This is the Phase 1b in-memory-only implementation. Later phases will add SSTable persistence and S3 integration.
 * </p>
 */
class S3SailStore implements SailStore {

	final Logger logger = LoggerFactory.getLogger(S3SailStore.class);

	private final S3ValueStore valueStore;
	private final S3NamespaceStore namespaceStore;
	private final List<QuadIndex> indexes;
	private final List<MemTable> memTables;
	private volatile boolean mayHaveInferred;

	/**
	 * A lock to control concurrent access by {@link S3SailSink} to the stores.
	 */
	private final ReentrantLock sinkStoreAccessLock = new ReentrantLock();

	S3SailStore(S3StoreConfig config) {
		this.valueStore = new S3ValueStore();
		this.namespaceStore = new S3NamespaceStore();

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
			// Fallback: always ensure at least one index
			QuadIndex defaultIndex = new QuadIndex("spoc");
			indexes.add(defaultIndex);
			memTables.add(new MemTable(defaultIndex));
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
		MemTable bestTable = memTables.get(bestIdx);

		ArrayList<CloseableIteration<? extends Statement>> perContextIterList = new ArrayList<>(contextIDList.size());

		for (long contextID : contextIDList) {
			Iterator<long[]> quads = bestTable.scan(subjID, predID, objID, contextID, explicit);
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
				// In-memory only: nothing to persist yet.
				// In later phases this will flush MemTable to SSTable and upload to S3.
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

				// Use the first MemTable as the source of truth for scanning, then remove from all
				int bestIdx = getBestIndex(subjID, predID, objID,
						contextIds.length == 1 ? contextIds[0] : S3ValueStore.UNKNOWN_ID);
				MemTable scanTable = memTables.get(bestIdx);

				long removeCount = 0;
				for (long contextId : contextIds) {
					Iterator<long[]> iter = scanTable.scan(subjID, predID, objID, contextId, explicit);
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
			// no-op for in-memory implementation
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
			MemTable table = memTables.get(0);
			Iterator<long[]> allQuads = table.scan(-1, -1, -1, -1, explicit);

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
	 * Converts quad ID arrays from MemTable iteration into Statement objects by resolving IDs through the ValueStore.
	 */
	private static final class QuadToStatementIteration implements CloseableIteration<Statement> {

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
			// no-op: MemTable iterators don't hold resources
		}
	}
}
