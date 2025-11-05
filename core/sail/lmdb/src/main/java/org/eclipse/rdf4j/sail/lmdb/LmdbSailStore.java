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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.InterruptedSailException;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.BackingSailSource;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A disk based {@link SailStore} implementation that keeps committed statements in a {@link TripleStore}.
 */
class LmdbSailStore implements SailStore {

	private static final RecordIterator EMPTY_RECORD_ITERATOR = new RecordIterator() {
		@Override
		public long[] next() {
			return null;
		}

		@Override
		public void close() {
			// no-op
		}
	};

	private static final boolean SCRATCH_REUSE_ENABLED = !"false"
			.equalsIgnoreCase(System.getProperty("rdf4j.lmdb.experimentalScratchReuse", "true"));

	final Logger logger = LoggerFactory.getLogger(LmdbSailStore.class);

	private final TripleStore tripleStore;

	private final ValueStore valueStore;

	// Precomputed lookup: for each bound-mask (bits for S=1,P=2,O=4,C=8), the union of
	// supported StatementOrder across all configured indexes that are compatible with that mask.
	@SuppressWarnings("unchecked")
	private final EnumSet<StatementOrder>[] supportedOrdersLookup = (EnumSet<StatementOrder>[]) new EnumSet[16];

	@SuppressWarnings("unchecked")
	private final List<TripleStore.TripleIndex>[] compatibleIndexesByMask = (List<TripleStore.TripleIndex>[]) new List[16];

	// firstFreeOrderByIndexAndMask[indexPos][mask] -> first free StatementOrder in that index for that mask, or null
	private StatementOrder[][] firstFreeOrderByIndexAndMask;

	// Map index instance -> its stable position used in the lookup arrays
	private Map<TripleStore.TripleIndex, Integer> indexPositionMap;

	private final ExecutorService tripleStoreExecutor = Executors.newCachedThreadPool();
	private final CircularBuffer<Operation> opQueue = new CircularBuffer<>(1024);
	private volatile Throwable tripleStoreException;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private boolean multiThreadingActive;
	private volatile boolean asyncTransactionFinished;
	private volatile boolean nextTransactionAsync;
	private volatile boolean mayHaveInferred;

	boolean enableMultiThreading = true;

	private PersistentSetFactory<Long> setFactory;
	private PersistentSet<Long> unusedIds, nextUnusedIds;

	/**
	 * A fast non-blocking circular buffer backed by an array.
	 *
	 * @param <T> Type of elements within this buffer
	 */
	static final class CircularBuffer<T> {

		private final T[] elements;
		private volatile int head = 0;
		private volatile int tail = 0;

		CircularBuffer(int size) {
			this.elements = (T[]) new Object[size];
		}

		boolean add(T element) {
			// faster version of:
			// tail == Math.floorMod(head - 1, elements.length)
			if (head > 0 ? tail == head - 1 : tail == elements.length - 1) {
				return false;
			}
			elements[tail] = element;
			tail = (tail + 1) % elements.length;
			return true;
		}

		T remove() {
			T result = null;
			if (tail != head) {
				result = elements[head];
				head = (head + 1) % elements.length;
			}
			return result;
		}
	}

	/**
	 * An operation that can be executed asynchronously.
	 */
	interface Operation {
		void execute() throws Exception;
	}

	/**
	 * Special operation that commits the current transaction.
	 */
	static final Operation COMMIT_TRANSACTION = () -> {
	};

	/**
	 * Special operation that rolls the current transaction back.
	 */
	static final Operation ROLLBACK_TRANSACTION = () -> {
	};

	/**
	 * Operation for adding a new quad.
	 */
	class AddQuadOperation implements Operation {
		long s, p, o, c;
		boolean explicit;
		Resource context;

		@Override
		public void execute() throws IOException {
			if (!explicit) {
				mayHaveInferred = true;
			}
			if (!unusedIds.isEmpty()) {
				// these ids are used again
				unusedIds.remove(s);
				unusedIds.remove(p);
				unusedIds.remove(o);
				unusedIds.remove(c);
			}
			tripleStore.storeTriple(s, p, o, c, explicit);
		}
	}

	/**
	 * Super-class for operations that capture their finished state.
	 */
	abstract static class StatefulOperation implements Operation {
		volatile boolean finished = false;
	}

	private final NamespaceStore namespaceStore;

	/**
	 * A lock to control concurrent access by {@link LmdbSailSink} to the TripleStore, ValueStore, and NamespaceStore.
	 * Each sink method that directly accesses one of these store obtains the lock and releases it immediately when
	 * done.
	 */
	private final ReentrantLock sinkStoreAccessLock = new ReentrantLock();

	/**
	 * Boolean indicating whether any {@link LmdbSailSink} has started a transaction on the {@link TripleStore}.
	 */
	private final AtomicBoolean storeTxnStarted = new AtomicBoolean(false);

	/**
	 * Creates a new {@link LmdbSailStore}.
	 */
	public LmdbSailStore(File dataDir, LmdbStoreConfig config) throws IOException, SailException {
		this.setFactory = new PersistentSetFactory<>(dataDir);
		Function<Long, byte[]> encode = element -> {
			ByteBuffer bb = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN);
			bb.putLong(element);
			return bb.array();
		};
		Function<ByteBuffer, Long> decode = buffer -> buffer.order(ByteOrder.BIG_ENDIAN).getLong();
		this.unusedIds = setFactory.createSet("unusedIds", encode, decode);
		this.nextUnusedIds = setFactory.createSet("nextUnusedIds", encode, decode);
		boolean initialized = false;
		try {
			namespaceStore = new NamespaceStore(dataDir);
			var valueStore = new ValueStore(new File(dataDir, "values"), config);
			this.valueStore = valueStore;
			tripleStore = new TripleStore(new File(dataDir, "triples"), config, valueStore);
			mayHaveInferred = tripleStore.hasTriples(false);
			initialized = true;
		} finally {
			if (!initialized) {
				close();
			}
		}
	}

	private static int bitFor(char f) {
		switch (f) {
		case 's':
			return 1;
		case 'p':
			return 1 << 1;
		case 'o':
			return 1 << 2;
		case 'c':
			return 1 << 3;
		default:
			return 0;
		}
	}

	private static StatementOrder orderFor(char f) {
		switch (f) {
		case 's':
			return StatementOrder.S;
		case 'p':
			return StatementOrder.P;
		case 'o':
			return StatementOrder.O;
		case 'c':
			return StatementOrder.C;
		default:
			throw new IllegalArgumentException("Unknown field: " + f);
		}
	}

	private static boolean isIndexCompatible(char[] seq, int mask) {
		boolean seenUnbound = false;
		for (char f : seq) {
			boolean bound = (mask & bitFor(f)) != 0;
			if (!bound) {
				seenUnbound = true;
			} else if (seenUnbound) {
				return false;
			}
		}
		return true;
	}

	private EnumSet<StatementOrder>[] getSupportedOrdersLookup() {
		EnumSet<StatementOrder>[] local = supportedOrdersLookup;
		if (local[0] == null) {
			synchronized (this) {
				local = supportedOrdersLookup;
				if (local[0] == null) {
					buildSupportedOrdersLookup(local);
				}
			}
		}
		return local;
	}

	private void buildSupportedOrdersLookup(EnumSet<StatementOrder>[] table) {
		for (int i = 0; i < table.length; i++) {
			table[i] = EnumSet.noneOf(StatementOrder.class);
			compatibleIndexesByMask[i] = new ArrayList<>();
		}
		List<TripleStore.TripleIndex> indexes = tripleStore.getAllIndexes();
		char[][] seqs = new char[indexes.size()][];
		for (int i = 0; i < indexes.size(); i++) {
			seqs[i] = indexes.get(i).getFieldSeq();
		}
		// Build index position map
		Map<TripleStore.TripleIndex, Integer> posMap = new IdentityHashMap<>();
		for (int i = 0; i < indexes.size(); i++) {
			posMap.put(indexes.get(i), i);
		}
		StatementOrder[][] firstFree = new StatementOrder[indexes.size()][16];
		for (int i = 0; i < indexes.size(); i++) {
			char[] seq = seqs[i];
			for (int mask = 0; mask < 16; mask++) {
				StatementOrder first = null;
				for (char f : seq) {
					if ((mask & bitFor(f)) == 0) {
						first = orderFor(f);
						break;
					}
				}
				firstFree[i][mask] = first;
			}
		}
		for (int mask = 0; mask < 16; mask++) {
			EnumSet<StatementOrder> set = table[mask];
			boolean anyCompatible = false;
			for (int i = 0; i < indexes.size(); i++) {
				char[] seq = seqs[i];
				if (!isIndexCompatible(seq, mask)) {
					continue;
				}
				anyCompatible = true;
				compatibleIndexesByMask[mask].add(indexes.get(i));
				// add bound dimensions (trivial order)
				if ((mask & 1) != 0)
					set.add(StatementOrder.S);
				if ((mask & (1 << 1)) != 0)
					set.add(StatementOrder.P);
				if ((mask & (1 << 2)) != 0)
					set.add(StatementOrder.O);
				if ((mask & (1 << 3)) != 0)
					set.add(StatementOrder.C);
				// add first free variable for this index & mask if present
				StatementOrder first = firstFree[i][mask];
				if (first != null) {
					set.add(first);
				}
			}
			if (!anyCompatible) {
				set.clear();
			}
		}
		// publish
		this.firstFreeOrderByIndexAndMask = firstFree;
		this.indexPositionMap = posMap;
	}

	@Override
	public ValueFactory getValueFactory() {
		return valueStore;
	}

	void rollback() throws SailException {
		sinkStoreAccessLock.lock();
		try {
			try {
				valueStore.rollback();
			} finally {
				if (multiThreadingActive) {
					while (!opQueue.add(ROLLBACK_TRANSACTION)) {
						if (tripleStoreException != null) {
							throw wrapTripleStoreException();
						} else {
							Thread.yield();
						}
					}
				} else {
					tripleStore.rollback();
				}
			}
		} catch (Exception e) {
			logger.warn("Failed to rollback LMDB transaction", e);
			throw e instanceof SailException ? (SailException) e : new SailException(e);
		} finally {
			tripleStoreException = null;
			// Reset transaction-started flag after rollback so subsequent reads don't
			// assume pending uncommitted changes and disable LMDB ID join optimizations.
			storeTxnStarted.set(false);
			sinkStoreAccessLock.unlock();
		}
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
					if (valueStore != null) {
						valueStore.close();
					}
				} finally {
					try {
						if (tripleStore != null) {
							try {
								running.set(false);
								tripleStoreExecutor.shutdown();
								try {
									while (!tripleStoreExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
										logger.warn("Waiting for triple store executor to terminate");
									}
								} catch (InterruptedException e) {
									Thread.currentThread().interrupt();
									throw new InterruptedSailException(e);
								}
							} finally {
								tripleStore.close();
							}
						}
					} finally {
						if (setFactory != null) {
							setFactory.close();
							setFactory = null;
						}
					}
				}
			}
		} catch (IOException e) {
			logger.warn("Failed to close store", e);
			throw new SailException(e);
		}
	}

	SailException wrapTripleStoreException() {
		return tripleStoreException instanceof SailException ? (SailException) tripleStoreException
				: new SailException(tripleStoreException);
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return new LmdbEvaluationStatistics(valueStore, tripleStore);
	}

	@Override
	public SailSource getExplicitSailSource() {
		return new LmdbSailSource(true);
	}

	@Override
	public SailSource getInferredSailSource() {
		return new LmdbSailSource(false);
	}

	CloseableIteration<Resource> getContexts() throws IOException {
		Txn txn = tripleStore.getTxnManager().createReadTxn();
		RecordIterator records = tripleStore.getAllTriplesSortedByContext(txn);
		CloseableIteration<? extends Statement> stIter1;
		if (records == null) {
			// Iterator over all statements
			stIter1 = createStatementIterator(txn, null, null, null, true);
		} else {
			stIter1 = new LmdbStatementIterator(records, valueStore);
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

			@Override
			protected void handleClose() throws SailException {
				// correctly close read txn
				txn.close();
				super.handleClose();
			}
		};
	}

	/**
	 * Creates a statement iterator based on the supplied pattern.
	 *
	 * @param subj     The subject of the pattern, or <tt>null</tt> to indicate a wildcard.
	 * @param pred     The predicate of the pattern, or <tt>null</tt> to indicate a wildcard.
	 * @param obj      The object of the pattern, or <tt>null</tt> to indicate a wildcard.
	 * @param contexts The context(s) of the pattern. Note that this parameter is a vararg and as such is optional. If
	 *                 no contexts are supplied the method operates on the entire repository.
	 * @return A StatementIterator that can be used to iterate over the statements that match the specified pattern.
	 */
	CloseableIteration<? extends Statement> createStatementIterator(
			Txn txn, Resource subj, IRI pred, Value obj, boolean explicit, Resource... contexts) throws IOException {
		if (!explicit && !mayHaveInferred) {
			// there are no inferred statements and the iterator should only return inferred statements
			return CloseableIteration.EMPTY_STATEMENT_ITERATION;
		}
		long subjID = LmdbValue.UNKNOWN_ID;
		if (subj != null) {
			subjID = valueStore.getId(subj);
			if (subjID == LmdbValue.UNKNOWN_ID) {
				return CloseableIteration.EMPTY_STATEMENT_ITERATION;
			}
		}

		long predID = LmdbValue.UNKNOWN_ID;
		if (pred != null) {
			predID = valueStore.getId(pred);
			if (predID == LmdbValue.UNKNOWN_ID) {
				return CloseableIteration.EMPTY_STATEMENT_ITERATION;
			}
		}

		long objID = LmdbValue.UNKNOWN_ID;
		if (obj != null) {
			objID = valueStore.getId(obj);

			if (objID == LmdbValue.UNKNOWN_ID) {
				return CloseableIteration.EMPTY_STATEMENT_ITERATION;
			}
		}

		List<Long> contextIDList;
		if (contexts.length == 0) {
			RecordIterator records = tripleStore.getTriples(txn, subjID, predID, objID, LmdbValue.UNKNOWN_ID, explicit);
			boolean sBound = subj != null;
			boolean pBound = pred != null;
			boolean oBound = obj != null;
			Resource cachedS = null;
			IRI cachedP = null;
			Value cachedO = null;
			if (sBound && subj instanceof LmdbValue
					&& valueStore.getRevision()
							.equals(((LmdbValue) subj).getValueStoreRevision())) {
				cachedS = subj;
			}
			if (pBound && pred instanceof LmdbValue
					&& valueStore.getRevision()
							.equals(((LmdbValue) pred).getValueStoreRevision())) {
				cachedP = pred;
			}
			if (oBound && obj instanceof LmdbValue
					&& valueStore.getRevision()
							.equals(((LmdbValue) obj).getValueStoreRevision())) {
				cachedO = obj;
			}
			LmdbStatementIterator.StatementCreator creator = new LmdbStatementIterator.StatementCreator(valueStore,
					cachedS, cachedP, cachedO, null, sBound, pBound, oBound, false);
			return new LmdbStatementIterator(records, creator);
		} else {
			contextIDList = new ArrayList<>(contexts.length);
			for (Resource context : contexts) {
				if (context == null) {
					contextIDList.add(0L);
				} else if (!context.isTriple()) {
					long contextID = valueStore.getId(context);

					if (contextID != LmdbValue.UNKNOWN_ID) {
						contextIDList.add(contextID);
					}
				}
			}
		}

		ArrayList<LmdbStatementIterator> perContextIterList = new ArrayList<>(contextIDList.size());

		for (long contextID : contextIDList) {
			RecordIterator records = tripleStore.getTriples(txn, subjID, predID, objID, contextID, explicit);
			boolean sBound = subj != null;
			boolean pBound = pred != null;
			boolean oBound = obj != null;
			Resource cachedS = null;
			IRI cachedP = null;
			Value cachedO = null;
			Resource cachedC = null;
			if (sBound && subj instanceof LmdbValue
					&& valueStore.getRevision()
							.equals(((LmdbValue) subj).getValueStoreRevision())) {
				cachedS = subj;
			}
			if (pBound && pred instanceof LmdbValue
					&& valueStore.getRevision()
							.equals(((LmdbValue) pred).getValueStoreRevision())) {
				cachedP = pred;
			}
			if (oBound && obj instanceof LmdbValue
					&& valueStore.getRevision()
							.equals(((LmdbValue) obj).getValueStoreRevision())) {
				cachedO = obj;
			}
			// If exactly one context was provided and is revision-compatible LmdbValue, pass it
			if (contexts.length == 1) {
				Resource ctx = contexts[0];
				if (ctx != null && !ctx.isTriple() && ctx instanceof LmdbValue
						&& valueStore.getRevision()
								.equals(((LmdbValue) ctx).getValueStoreRevision())) {
					cachedC = ctx;
				}
			}
			LmdbStatementIterator.StatementCreator creator = new LmdbStatementIterator.StatementCreator(valueStore,
					cachedS, cachedP, cachedO, cachedC, sBound, pBound, oBound, true);
			perContextIterList.add(new LmdbStatementIterator(records, creator));
		}

		if (perContextIterList.size() == 1) {
			return perContextIterList.get(0);
		} else {
			return new UnionIteration<>(perContextIterList);
		}
	}

	private final class LmdbSailSource extends BackingSailSource {

		private final boolean explicit;

		public LmdbSailSource(boolean explicit) {
			this.explicit = explicit;
		}

		@Override
		public SailSource fork() {
			throw new UnsupportedOperationException("This store does not support multiple datasets");
		}

		@Override
		public SailSink sink(IsolationLevel level) throws SailException {
			return new LmdbSailSink(explicit);
		}

		@Override
		public LmdbSailDataset dataset(IsolationLevel level) throws SailException {
			return new LmdbSailDataset(explicit, level);
		}

	}

	private final class LmdbSailSink implements SailSink {

		private final boolean explicit;

		public LmdbSailSink(boolean explicit) throws SailException {
			this.explicit = explicit;
		}

		@Override
		public void close() {
			// do nothing
		}

		@Override
		public void prepare() throws SailException {
			// serializable is not supported at this level
		}

		protected void filterUsedIdsInTripleStore() throws IOException {
			if (!unusedIds.isEmpty()) {
				tripleStore.filterUsedIds(unusedIds);
			}
		}

		protected void handleRemovedIdsInValueStore() throws IOException {
			if (!unusedIds.isEmpty()) {
				do {
					valueStore.gcIds(unusedIds, nextUnusedIds);
					unusedIds.clear();
					if (!nextUnusedIds.isEmpty()) {
						// swap sets
						PersistentSet<Long> ids = unusedIds;
						unusedIds = nextUnusedIds;
						nextUnusedIds = ids;
						filterUsedIdsInTripleStore();
					}
				} while (!unusedIds.isEmpty());
			}
		}

		@Override
		public void flush() throws SailException {
			sinkStoreAccessLock.lock();
			boolean activeTxn = storeTxnStarted.get();
			try {
				if (multiThreadingActive) {
					while (!opQueue.add(COMMIT_TRANSACTION)) {
						if (tripleStoreException != null) {
							throw wrapTripleStoreException();
						} else {
							Thread.yield();
						}
					}
				}

				try {
					namespaceStore.sync();
				} finally {
					if (multiThreadingActive) {
						while (!asyncTransactionFinished) {
							if (tripleStoreException != null) {
								throw wrapTripleStoreException();
							} else {
								Thread.yield();
							}
						}
					}
					if (activeTxn) {
						if (!multiThreadingActive) {
							tripleStore.commit();
							filterUsedIdsInTripleStore();
						}
						handleRemovedIdsInValueStore();
						valueStore.commit();
						// do not set flag to false until _after_ commit is successfully completed.
						storeTxnStarted.set(false);
					}
				}
			} catch (IOException e) {
				rollback();
				running.set(false);
				logger.error("Encountered an unexpected problem while trying to commit", e);
				throw new SailException(e);
			} catch (RuntimeException e) {
				rollback();
				running.set(false);
				logger.error("Encountered an unexpected problem while trying to commit", e);
				throw e;
			} finally {
				multiThreadingActive = false;
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void setNamespace(String prefix, String name) throws SailException {
			sinkStoreAccessLock.lock();
			try {
				startTransaction(true);
				namespaceStore.setNamespace(prefix, name);
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void removeNamespace(String prefix) throws SailException {
			sinkStoreAccessLock.lock();
			try {
				startTransaction(true);
				namespaceStore.removeNamespace(prefix);
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		@Override
		public void clearNamespaces() throws SailException {
			sinkStoreAccessLock.lock();
			try {
				startTransaction(true);
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
			Statement last = null;

			sinkStoreAccessLock.lock();
			try {
				startTransaction(true);

				for (Statement statement : approved) {
					last = statement;
					Resource subj = statement.getSubject();
					IRI pred = statement.getPredicate();
					Value obj = statement.getObject();
					Resource context = statement.getContext();

					AddQuadOperation q = new AddQuadOperation();
					q.s = valueStore.storeValue(subj);
					q.p = valueStore.storeValue(pred);
					q.o = valueStore.storeValue(obj);
					q.c = context == null ? 0 : valueStore.storeValue(context);
					q.context = context;
					q.explicit = explicit;

					if (multiThreadingActive) {
						while (!opQueue.add(q)) {
							if (tripleStoreException != null) {
								throw wrapTripleStoreException();
							}
							Thread.onSpinWait();
						}

					} else {
						q.execute();
					}

				}
			} catch (IOException | RuntimeException e) {
				rollback();
				if (multiThreadingActive) {
					logger.error("Encountered an unexpected problem while trying to add a statement.", e);
				} else {
					logger.error(
							"Encountered an unexpected problem while trying to add a statement. Last statement that was attempted to be added: [ {} ]",
							last, e);
				}

				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
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
		private void startTransaction(boolean preferThreading) throws SailException {
			synchronized (storeTxnStarted) {
				if (storeTxnStarted.compareAndSet(false, true)) {
					multiThreadingActive = preferThreading && enableMultiThreading;
					nextTransactionAsync = multiThreadingActive;
					asyncTransactionFinished = false;
					try {
						if (multiThreadingActive) {
							if (running.compareAndSet(false, true)) {
								tripleStoreException = null;
								tripleStoreExecutor.submit(() -> {
									try {
										while (running.get()) {
											tripleStore.startTransaction();
											while (true) {
												Operation op = opQueue.remove();
												if (op != null) {
													if (op == COMMIT_TRANSACTION) {
														tripleStore.commit();
														filterUsedIdsInTripleStore();

														nextTransactionAsync = false;
														asyncTransactionFinished = true;
														break;
													} else if (op == ROLLBACK_TRANSACTION) {
														tripleStore.rollback();
														nextTransactionAsync = false;
														asyncTransactionFinished = true;
														break;
													} else {
														op.execute();
													}
												} else {
													if (!running.get()) {
														logger.warn(
																"LmdbSailStore was closed while active transaction was waiting for the next operation. Forcing a rollback!");
														rollback();
													} else if (Thread.interrupted()) {
														throw new InterruptedException();
													} else {
														Thread.yield();
													}
												}
											}

											// keep thread running for at least 2ms to lock-free wait for the next
											// transaction
											long start = 0;
											while (running.get() && !nextTransactionAsync) {
												if (start == 0) {
													// System.currentTimeMillis() is expensive, so only call it when we
													// are sure we need to wait
													start = System.currentTimeMillis();
												}

												if (System.currentTimeMillis() - start > 2) {
													synchronized (storeTxnStarted) {
														if (!nextTransactionAsync) {
															running.set(false);
															return;
														}
													}
												} else {
													Thread.yield();
												}
											}
										}
									} catch (Throwable e) {
										tripleStoreException = e;
										synchronized (storeTxnStarted) {
											running.set(false);
										}
									}
								});
							}
						} else {
							tripleStore.startTransaction();
						}
						valueStore.startTransaction(true);
					} catch (Exception e) {
						storeTxnStarted.set(false);
						throw new SailException(e);
					}
				}
			}
		}

		private void addStatement(Resource subj, IRI pred, Value obj, boolean explicit, Resource context)
				throws SailException {
			sinkStoreAccessLock.lock();
			try {
				startTransaction(true);

				AddQuadOperation q = new AddQuadOperation();
				q.s = valueStore.storeValue(subj);
				q.p = valueStore.storeValue(pred);
				q.o = valueStore.storeValue(obj);
				q.c = context == null ? 0 : valueStore.storeValue(context);
				q.context = context;
				q.explicit = explicit;

				if (multiThreadingActive) {
					while (!opQueue.add(q)) {
						if (tripleStoreException != null) {
							throw wrapTripleStoreException();
						} else {
							Thread.onSpinWait();
						}
					}
				} else {
					q.execute();
				}
			} catch (IOException e) {
				rollback();
				throw new SailException(e);
			} catch (RuntimeException e) {
				rollback();
				logger.error("Encountered an unexpected problem while trying to add a statement", e);
				throw e;
			} finally {
				sinkStoreAccessLock.unlock();
			}
		}

		private long removeStatements(long subj, long pred, long obj, boolean explicit, long[] contexts)
				throws IOException {
			long[] removeCount = { 0 };
			for (long contextId : contexts) {
				tripleStore.removeTriplesByContext(subj, pred, obj, contextId, explicit, quad -> {
					removeCount[0]++;
					for (long id : quad) {
						if (id != 0L) {
							unusedIds.add(id);
						}
					}
				});
			}
			return removeCount[0];
		}

		private long removeStatements(Resource subj, IRI pred, Value obj, boolean explicit, Resource... contexts)
				throws SailException {
			Objects.requireNonNull(contexts,
					"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

			sinkStoreAccessLock.lock();
			try {
				startTransaction(false);
				final long subjID;
				if (subj != null) {
					subjID = valueStore.getId(subj);
					if (subjID == LmdbValue.UNKNOWN_ID) {
						return 0;
					}
				} else {
					subjID = LmdbValue.UNKNOWN_ID;
				}
				final long predID;
				if (pred != null) {
					predID = valueStore.getId(pred);
					if (predID == LmdbValue.UNKNOWN_ID) {
						return 0;
					}
				} else {
					predID = LmdbValue.UNKNOWN_ID;
				}
				final long objID;
				if (obj != null) {
					objID = valueStore.getId(obj);
					if (objID == LmdbValue.UNKNOWN_ID) {
						return 0;
					}
				} else {
					objID = LmdbValue.UNKNOWN_ID;
				}

				final long[] contextIds = new long[contexts.length == 0 ? 1 : contexts.length];
				if (contexts.length == 0) { // remove from all contexts
					contextIds[0] = LmdbValue.UNKNOWN_ID;
				} else {
					for (int i = 0; i < contexts.length; i++) {
						Resource context = contexts[i];
						if (context == null) {
							contextIds[i] = 0;
						} else {
							long id = valueStore.getId(context);
							// unknown_id cannot be used (would result in removal from all contexts)
							// TODO check if Long.MAX_VALUE is correct here
							contextIds[i] = (id != LmdbValue.UNKNOWN_ID) ? id : Long.MAX_VALUE;
						}
					}
				}

				if (multiThreadingActive) {
					long[] removeCount = new long[1];
					StatefulOperation removeOp = new StatefulOperation() {
						@Override
						public void execute() throws Exception {
							try {
								removeCount[0] = removeStatements(subjID, predID, objID, explicit, contextIds);
							} finally {
								finished = true;
							}
						}
					};

					while (!opQueue.add(removeOp)) {
						if (tripleStoreException != null) {
							throw wrapTripleStoreException();
						} else {
							Thread.yield();
						}
					}

					while (!removeOp.finished) {
						if (tripleStoreException != null) {
							throw wrapTripleStoreException();
						} else {
							Thread.yield();
						}
					}
					return removeCount[0];
				} else {
					return removeStatements(subjID, predID, objID, explicit, contextIds);
				}
			} catch (IOException e) {
				rollback();
				throw new SailException(e);
			} catch (RuntimeException e) {
				rollback();
				logger.error("Encountered an unexpected problem while trying to remove statements", e);
				throw e;
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

	private final class LmdbSailDataset implements SailDataset, LmdbEvaluationDataset {
		private final boolean explicit;
		private final IsolationLevel isolationLevel;
		private final Txn txn;

		public LmdbSailDataset(boolean explicit, IsolationLevel isolationLevel) throws SailException {
			this.explicit = explicit;
			this.isolationLevel = isolationLevel;
			try {
				this.txn = tripleStore.getTxnManager().createReadTxn();
				LmdbEvaluationStrategy.setCurrentDataset(this);
			} catch (IOException e) {
				throw new SailException(e);
			}
		}

		@Override
		public void close() {
			try {
				// close the associated txn
				txn.close();
			} finally {
				LmdbEvaluationStrategy.clearCurrentDataset();
			}
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
			try {
				return new LmdbContextIterator(tripleStore.getContexts(txn), valueStore);
			} catch (IOException e) {
				throw new SailException("Unable to get contexts", e);
			}
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) throws SailException {
			try {
				return createStatementIterator(txn, subj, pred, obj, explicit, contexts);
			} catch (IOException e) {
				try {
					logger.warn("Failed to get statements, retrying", e);
					// try once more before giving up
					Thread.yield();
					return createStatementIterator(txn, subj, pred, obj, explicit, contexts);
				} catch (IOException e2) {
					throw new SailException("Unable to get statements", e);
				}
			}
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(StatementOrder statementOrder, Resource subj,
				IRI pred, Value obj, Resource... contexts) throws SailException {
			try {
				// Fast reject: inferred-only dataset but store has no inferred
				if (!explicit && !mayHaveInferred) {
					return CloseableIteration.EMPTY_STATEMENT_ITERATION;
				}

				// Resolve value ids
				long subjID = LmdbValue.UNKNOWN_ID;
				if (subj != null) {
					subjID = valueStore.getId(subj);
					if (subjID == LmdbValue.UNKNOWN_ID) {
						return CloseableIteration.EMPTY_STATEMENT_ITERATION;
					}
				}

				long predID = LmdbValue.UNKNOWN_ID;
				if (pred != null) {
					predID = valueStore.getId(pred);
					if (predID == LmdbValue.UNKNOWN_ID) {
						return CloseableIteration.EMPTY_STATEMENT_ITERATION;
					}
				}

				long objID = LmdbValue.UNKNOWN_ID;
				if (obj != null) {
					objID = valueStore.getId(obj);
					if (objID == LmdbValue.UNKNOWN_ID) {
						return CloseableIteration.EMPTY_STATEMENT_ITERATION;
					}
				}

				// Context handling: if more than one context is requested, we cannot efficiently guarantee a global
				// order
				// without a k-way merge. In that case, fall back to default behavior (unordered union).
				if (contexts != null && contexts.length > 1) {
					throw new IllegalArgumentException(
							"LMDB SailStore does not support ordered scans over multiple contexts");
				}

				long contextID;
				if (contexts == null || contexts.length == 0) {
					contextID = LmdbValue.UNKNOWN_ID; // wildcard over all contexts
				} else {
					Resource ctx = contexts[0];
					if (ctx == null) {
						contextID = 0L; // default graph
					} else if (!ctx.isTriple()) {
						contextID = valueStore.getId(ctx);
						if (contextID == LmdbValue.UNKNOWN_ID) {
							return CloseableIteration.EMPTY_STATEMENT_ITERATION;
						}
					} else {
						// RDF* triple as context not supported by LMDB index order; fall back to default behavior
						return createStatementIterator(txn, subj, pred, obj, explicit, contexts);
					}
				}

				// Pick an index that can provide the requested order given current bindings
				TripleStore.TripleIndex chosen = chooseIndexForOrder(statementOrder, subjID, predID, objID, contextID);
				if (chosen == null) {
					// No compatible index for ordered scan; fall back to default iterator
					return createStatementIterator(txn, subj, pred, obj, explicit, contexts);
				}

				boolean rangeSearch = chosen.getPatternScore(subjID, predID, objID, contextID) > 0;
				TripleStore.KeyBuilder keyBuilder = rangeSearch
						? chosen.keyBuilder(subjID, predID, objID, contextID)
						: null;
				RecordIterator records = keyBuilder != null
						? new LmdbRecordIterator(chosen, keyBuilder, rangeSearch, subjID, predID, objID, contextID,
								explicit, txn)
						: new LmdbRecordIterator(chosen, rangeSearch, subjID, predID, objID, contextID, explicit, txn);

				boolean sBound = subj != null;
				boolean pBound = pred != null;
				boolean oBound = obj != null;
				boolean cBound;
				// exactly one context allowed at this point
				cBound = contexts != null && contexts.length != 0;

				Resource cachedS = null;
				IRI cachedP = null;
				Value cachedO = null;
				Resource cachedC = null;

				if (sBound && subj instanceof LmdbValue
						&& valueStore.getRevision()
								.equals(((LmdbValue) subj).getValueStoreRevision())) {
					cachedS = subj;
				}
				if (pBound && pred instanceof LmdbValue
						&& valueStore.getRevision()
								.equals(((LmdbValue) pred).getValueStoreRevision())) {
					cachedP = pred;
				}
				if (oBound && obj instanceof LmdbValue
						&& valueStore.getRevision()
								.equals(((LmdbValue) obj).getValueStoreRevision())) {
					cachedO = obj;
				}

				if (cBound) {
					Resource ctx = contexts[0];
					if (ctx != null && !ctx.isTriple()
							&& ctx instanceof LmdbValue
							&& valueStore.getRevision()
									.equals(((LmdbValue) ctx)
											.getValueStoreRevision())) {
						cachedC = ctx;
					}
				}

				LmdbStatementIterator.StatementCreator creator = new LmdbStatementIterator.StatementCreator(valueStore,
						cachedS, cachedP, cachedO, cachedC, sBound, pBound, oBound, cBound);
				return new LmdbStatementIterator(records, creator);
			} catch (IOException e) {
				throw new SailException("Unable to get ordered statements", e);
			}
		}

		@Override
		public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
			// If multiple contexts are specified, LMDB currently returns a union without a global ordering guarantee
			if (contexts != null && contexts.length > 1) {
				return Set.of();
			}

			boolean sBound = subj != null;
			boolean pBound = pred != null;
			boolean oBound = obj != null;
			boolean cBound = false;
			if (contexts != null && contexts.length == 1) {
				Resource ctx = contexts[0];
				if (ctx == null) {
					cBound = true;
				} else if (!ctx.isTriple()) {
					cBound = true;
				} else {
					// triple context not supported for ordered scans
					return Set.of();
				}
			}

			int mask = (sBound ? 1 : 0) | (pBound ? (1 << 1) : 0) | (oBound ? (1 << 2) : 0) | (cBound ? (1 << 3) : 0);
			EnumSet<StatementOrder> res = getSupportedOrdersLookup()[mask];
			return res.isEmpty() ? Set.of() : EnumSet.copyOf(res);
		}

		@Override
		public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings)
				throws QueryEvaluationException {
			return getRecordIterator(pattern, bindings, null);
		}

		@Override
		public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings,
				KeyRangeBuffers keyBuffers) throws QueryEvaluationException {
			return getRecordIterator(pattern, bindings, keyBuffers, null);
		}

		@Override
		public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings,
				KeyRangeBuffers keyBuffers,
				RecordIterator iteratorReuse) throws QueryEvaluationException {
			try {
				PatternArrays arrays = describePattern(pattern);
				if (!arrays.valid) {
					return emptyRecordIterator();
				}
				long subjID = resolveIdWithBindings(arrays.ids[TripleStore.SUBJ_IDX],
						arrays.varNames[TripleStore.SUBJ_IDX],
						bindings, true, false);
				if (subjID == INVALID_ID) {
					return emptyRecordIterator();
				}

				long predID = resolveIdWithBindings(arrays.ids[TripleStore.PRED_IDX],
						arrays.varNames[TripleStore.PRED_IDX],
						bindings, false, true);
				if (predID == INVALID_ID) {
					return emptyRecordIterator();
				}

				long objID = resolveIdWithBindings(arrays.ids[TripleStore.OBJ_IDX],
						arrays.varNames[TripleStore.OBJ_IDX],
						bindings, false, false);
				if (objID == INVALID_ID) {
					return emptyRecordIterator();
				}

				long contextID = resolveContextWithBindings(arrays.ids[TripleStore.CONTEXT_IDX],
						arrays.varNames[TripleStore.CONTEXT_IDX], bindings);
				if (contextID == INVALID_ID) {
					return emptyRecordIterator();
				}

				ByteBuffer minKeyBuf = keyBuffers != null ? keyBuffers.minKey() : null;
				ByteBuffer maxKeyBuf = keyBuffers != null ? keyBuffers.maxKey() : null;
				LmdbRecordIterator reuse = (iteratorReuse instanceof LmdbRecordIterator)
						? (LmdbRecordIterator) iteratorReuse
						: null;
				return tripleStore.getTriples(txn, subjID, predID, objID, contextID, explicit, minKeyBuf, maxKeyBuf,
						null, reuse);
			} catch (IOException e) {
				throw new QueryEvaluationException("Unable to create LMDB record iterator", e);
			}
		}

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex,
				long[] patternIds) throws QueryEvaluationException {
			return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, null, null, null,
					null);
		}

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex,
				long[] patternIds, long[] reuse) throws QueryEvaluationException {
			return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, null, reuse, null,
					null);
		}

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex,
				long[] patternIds, long[] reuse, long[] quadReuse) throws QueryEvaluationException {
			return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, null, reuse,
					quadReuse, null);
		}

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex,
				long[] patternIds, KeyRangeBuffers keyBuffers, long[] reuse, long[] quadReuse)
				throws QueryEvaluationException {
			return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, keyBuffers, reuse,
					quadReuse, null);
		}

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex,
				long[] patternIds, KeyRangeBuffers keyBuffers, long[] reuse, long[] quadReuse,
				RecordIterator iteratorReuse)
				throws QueryEvaluationException {
			try {
				long subjQuery = selectQueryId(patternIds[TripleStore.SUBJ_IDX], binding, subjIndex);
				long predQuery = selectQueryId(patternIds[TripleStore.PRED_IDX], binding, predIndex);
				long objQuery = selectQueryId(patternIds[TripleStore.OBJ_IDX], binding, objIndex);
				long ctxQuery = selectQueryId(patternIds[TripleStore.CONTEXT_IDX], binding, ctxIndex);

				ByteBuffer minKeyBuf = keyBuffers != null ? keyBuffers.minKey() : null;
				ByteBuffer maxKeyBuf = keyBuffers != null ? keyBuffers.maxKey() : null;

				BindingProjectingIterator projectingReuse = iteratorReuse instanceof BindingProjectingIterator
						? (BindingProjectingIterator) iteratorReuse
						: null;
				LmdbRecordIterator baseReuse = projectingReuse != null ? projectingReuse.getBase()
						: (iteratorReuse instanceof LmdbRecordIterator ? (LmdbRecordIterator) iteratorReuse : null);

				RecordIterator raw = tripleStore.getTriples(txn, subjQuery, predQuery, objQuery, ctxQuery, explicit,
						minKeyBuf, maxKeyBuf, quadReuse, baseReuse);

				if (!SCRATCH_REUSE_ENABLED) {
					RecordIterator baseIterator = raw;
					return new RecordIterator() {
						@Override
						public long[] next() throws QueryEvaluationException {
							try {
								long[] quad;
								while ((quad = baseIterator.next()) != null) {
									long[] merged = mergeBinding(binding, quad[TripleStore.SUBJ_IDX],
											quad[TripleStore.PRED_IDX], quad[TripleStore.OBJ_IDX],
											quad[TripleStore.CONTEXT_IDX], subjIndex, predIndex, objIndex, ctxIndex);
									if (merged != null) {
										return merged;
									}
								}
								return null;
							} catch (QueryEvaluationException e) {
								throw e;
							} catch (Exception e) {
								throw new QueryEvaluationException(e);
							}
						}

						@Override
						public void close() {
							baseIterator.close();
						}
					};
				}

				BindingProjectingIterator result = projectingReuse != null ? projectingReuse
						: new BindingProjectingIterator();
				result.configure(raw, raw instanceof LmdbRecordIterator ? (LmdbRecordIterator) raw : null, binding,
						subjIndex, predIndex, objIndex, ctxIndex, reuse);
				return result;
			} catch (IOException e) {
				throw new QueryEvaluationException("Unable to create LMDB record iterator", e);
			}
		}

		private final class BindingProjectingIterator implements RecordIterator {
			private RecordIterator base;
			private LmdbRecordIterator lmdbBase;
			private long[] binding;
			private int subjIndex;
			private int predIndex;
			private int objIndex;
			private int ctxIndex;
			private long[] scratch;

			void configure(RecordIterator base, LmdbRecordIterator lmdbBase, long[] binding, int subjIndex,
					int predIndex, int objIndex,
					int ctxIndex, long[] bindingReuse) {
				this.base = base;
				this.lmdbBase = lmdbBase;
				this.binding = binding;
				this.subjIndex = subjIndex;
				this.predIndex = predIndex;
				this.objIndex = objIndex;
				this.ctxIndex = ctxIndex;
				int bindingLength = binding.length;
				if (bindingReuse != null && bindingReuse.length >= bindingLength) {
					System.arraycopy(binding, 0, bindingReuse, 0, bindingLength);
					this.scratch = bindingReuse;
				} else if (this.scratch == null || this.scratch.length != bindingLength) {
					this.scratch = Arrays.copyOf(binding, bindingLength);
				} else {
					System.arraycopy(binding, 0, this.scratch, 0, bindingLength);
				}
			}

			LmdbRecordIterator getBase() {
				return lmdbBase;
			}

			@Override
			public long[] next() throws QueryEvaluationException {
				if (base == null) {
					return null;
				}
				try {
					long[] quad;
					while ((quad = base.next()) != null) {
						System.arraycopy(binding, 0, scratch, 0, binding.length);
						boolean conflict = false;
						if (subjIndex >= 0) {
							long baseVal = binding[subjIndex];
							long v = quad[TripleStore.SUBJ_IDX];
							if (baseVal != LmdbValue.UNKNOWN_ID && baseVal != v) {
								conflict = true;
							} else {
								scratch[subjIndex] = (baseVal != LmdbValue.UNKNOWN_ID) ? baseVal : v;
							}
						}
						if (!conflict && predIndex >= 0) {
							long baseVal = binding[predIndex];
							long v = quad[TripleStore.PRED_IDX];
							if (baseVal != LmdbValue.UNKNOWN_ID && baseVal != v) {
								conflict = true;
							} else {
								scratch[predIndex] = (baseVal != LmdbValue.UNKNOWN_ID) ? baseVal : v;
							}
						}
						if (!conflict && objIndex >= 0) {
							long baseVal = binding[objIndex];
							long v = quad[TripleStore.OBJ_IDX];
							if (baseVal != LmdbValue.UNKNOWN_ID && baseVal != v) {
								conflict = true;
							} else {
								scratch[objIndex] = (baseVal != LmdbValue.UNKNOWN_ID) ? baseVal : v;
							}
						}
						if (!conflict && ctxIndex >= 0) {
							long baseVal = binding[ctxIndex];
							long v = quad[TripleStore.CONTEXT_IDX];
							if (baseVal != LmdbValue.UNKNOWN_ID && baseVal != v) {
								conflict = true;
							} else {
								scratch[ctxIndex] = (baseVal != LmdbValue.UNKNOWN_ID) ? baseVal : v;
							}
						}
						if (!conflict) {
							return scratch;
						}
					}
					return null;
				} catch (QueryEvaluationException e) {
					throw e;
				} catch (Exception e) {
					throw new QueryEvaluationException(e);
				}
			}

			@Override
			public void close() {
				if (base != null) {
					base.close();
					base = null;
				}
			}
		}

		@Override
		public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds, StatementOrder order) throws QueryEvaluationException {
			return getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, order, null,
					null, null);
		}

		@Override
		public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds, StatementOrder order, long[] reuse) throws QueryEvaluationException {
			return getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, order, null,
					reuse, null);
		}

		@Override
		public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds, StatementOrder order, long[] reuse, long[] quadReuse)
				throws QueryEvaluationException {
			return getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, order, null,
					reuse, quadReuse);
		}

		@Override
		public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds, StatementOrder order, KeyRangeBuffers keyBuffers, long[] bindingReuse,
				long[] quadReuse) throws QueryEvaluationException {
			if (order == null) {
				return getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, keyBuffers,
						bindingReuse, quadReuse);
			}
			try {
				long subjQuery = selectQueryId(patternIds[TripleStore.SUBJ_IDX], binding, subjIndex);
				long predQuery = selectQueryId(patternIds[TripleStore.PRED_IDX], binding, predIndex);
				long objQuery = selectQueryId(patternIds[TripleStore.OBJ_IDX], binding, objIndex);
				long ctxQuery = selectQueryId(patternIds[TripleStore.CONTEXT_IDX], binding, ctxIndex);

				RecordIterator orderedIter = orderedRecordIterator(order, subjQuery, predQuery, objQuery, ctxQuery,
						quadReuse);
				if (orderedIter != null) {
					return orderedIter;
				}

				if (order == StatementOrder.S) {
					int sortIndex = indexForOrder(order, subjIndex, predIndex, objIndex, ctxIndex);
					if (sortIndex >= 0) {
						RecordIterator fallback = getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex,
								patternIds, keyBuffers, bindingReuse, quadReuse);
						return sortedRecordIterator(fallback, sortIndex);
					}
				}
				return null;
			} catch (IOException e) {
				throw new QueryEvaluationException("Unable to create ordered LMDB record iterator", e);
			}
		}

		@Override
		public boolean supportsOrder(long[] binding, int subjIndex, int predIndex, int objIndex, int ctxIndex,
				long[] patternIds, StatementOrder order) {
			if (order == null) {
				return true;
			}
			long subjQuery = selectQueryId(patternIds[TripleStore.SUBJ_IDX], binding, subjIndex);
			long predQuery = selectQueryId(patternIds[TripleStore.PRED_IDX], binding, predIndex);
			long objQuery = selectQueryId(patternIds[TripleStore.OBJ_IDX], binding, objIndex);
			long ctxQuery = selectQueryId(patternIds[TripleStore.CONTEXT_IDX], binding, ctxIndex);
			try {
				try (RecordIterator recordIterator = orderedRecordIterator(order, subjQuery, predQuery, objQuery,
						ctxQuery)) {
					if (recordIterator != null) {
						return true;
					}
				}
				return order == StatementOrder.S && indexForOrder(order, subjIndex, predIndex, objIndex, ctxIndex) >= 0;
			} catch (IOException e) {
				return false;
			}
		}

		@Override
		public RecordIterator getOrderedRecordIterator(StatementPattern pattern, BindingSet bindings,
				StatementOrder order) throws QueryEvaluationException {
			return getOrderedRecordIterator(pattern, bindings, order, null);
		}

		@Override
		public RecordIterator getOrderedRecordIterator(StatementPattern pattern, BindingSet bindings,
				StatementOrder order, KeyRangeBuffers keyBuffers) throws QueryEvaluationException {
			if (order == null) {
				return getRecordIterator(pattern, bindings, keyBuffers);
			}
			try {
				Value subj = resolveValue(pattern.getSubjectVar(), bindings);
				if (subj != null && !(subj instanceof Resource)) {
					return emptyRecordIterator();
				}
				Value pred = resolveValue(pattern.getPredicateVar(), bindings);
				if (pred != null && !(pred instanceof IRI)) {
					return emptyRecordIterator();
				}
				Value obj = resolveValue(pattern.getObjectVar(), bindings);

				long subjID = resolveId(subj);
				if (subj != null && subjID == LmdbValue.UNKNOWN_ID) {
					return emptyRecordIterator();
				}

				long predID = resolveId(pred);
				if (pred != null && predID == LmdbValue.UNKNOWN_ID) {
					return emptyRecordIterator();
				}

				long objID = resolveId(obj);
				if (obj != null && objID == LmdbValue.UNKNOWN_ID) {
					return emptyRecordIterator();
				}

				Value contextValue = resolveValue(pattern.getContextVar(), bindings);
				long contextID;
				if (contextValue == null) {
					contextID = LmdbValue.UNKNOWN_ID;
				} else if (contextValue instanceof Resource) {
					Resource ctx = (Resource) contextValue;
					if (ctx.isTriple()) {
						return emptyRecordIterator();
					}
					contextID = resolveId(ctx);
					if (contextID == LmdbValue.UNKNOWN_ID) {
						return emptyRecordIterator();
					}
				} else {
					return emptyRecordIterator();
				}

				RecordIterator orderedIter = orderedRecordIterator(order, subjID, predID, objID, contextID);
				if (orderedIter != null) {
					return orderedIter;
				}
				return getRecordIterator(pattern, bindings, keyBuffers);
			} catch (IOException e) {
				throw new QueryEvaluationException("Unable to create ordered LMDB record iterator", e);
			}
		}

		@Override
		public ValueStore getValueStore() {
			return valueStore;
		}

		@Override
		public String selectBestIndex(long subj, long pred, long obj, long context) {
			TripleStore.TripleIndex index = tripleStore.getBestIndex(subj, pred, obj, context);
			return index == null ? null : new String(index.getFieldSeq());
		}

		@Override
		public IsolationLevel getIsolationLevel() {
			return isolationLevel;
		}

		@Override
		public void refreshSnapshot() throws QueryEvaluationException {
			if (isolationLevel == IsolationLevels.SNAPSHOT || isolationLevel == IsolationLevels.SERIALIZABLE) {
				try {
					txn.reset();
				} catch (IOException e) {
					throw new QueryEvaluationException("Unable to refresh LMDB read transaction", e);
				}
			}
		}

		@Override
		public boolean hasTransactionChanges() {
			// storeTxnStarted is flipped to true when a writer begins and only cleared
			// after commit/rollback, so a true value indicates pending uncommitted changes.
			return storeTxnStarted.get();
		}

		private RecordIterator emptyRecordIterator() {
			return EMPTY_RECORD_ITERATOR;
		}

		private Value resolveValue(Var var, BindingSet bindings) {
			if (var == null) {
				return null;
			}
			if (var.hasValue()) {
				return var.getValue();
			}
			if (bindings != null) {
				Value bound = bindings.getValue(var.getName());
				return bound;
			}
			return null;
		}

		private static final long INVALID_ID = Long.MIN_VALUE;

		private PatternArrays describePattern(StatementPattern pattern) throws IOException {
			long[] ids = new long[4];
			String[] varNames = new String[4];
			boolean valid = true;

			valid &= populateSubject(pattern.getSubjectVar(), ids, varNames);
			valid &= populatePredicate(pattern.getPredicateVar(), ids, varNames);
			valid &= populateObject(pattern.getObjectVar(), ids, varNames);
			valid &= populateContext(pattern.getContextVar(), ids, varNames);

			return new PatternArrays(ids, varNames, valid);
		}

		private boolean populateSubject(Var var, long[] ids, String[] varNames)
				throws IOException {
			if (var == null) {
				ids[TripleStore.SUBJ_IDX] = LmdbValue.UNKNOWN_ID;
				varNames[TripleStore.SUBJ_IDX] = null;
				return true;
			}
			if (var.hasValue()) {
				Value value = var.getValue();
				if (!(value instanceof Resource)) {
					return false;
				}
				long id = resolveId(value);
				if (id == LmdbValue.UNKNOWN_ID) {
					return false;
				}
				ids[TripleStore.SUBJ_IDX] = id;
				varNames[TripleStore.SUBJ_IDX] = null;
				return true;
			}
			ids[TripleStore.SUBJ_IDX] = LmdbValue.UNKNOWN_ID;
			varNames[TripleStore.SUBJ_IDX] = var.getName();
			return true;
		}

		private boolean populatePredicate(Var var, long[] ids, String[] varNames)
				throws IOException {
			if (var == null) {
				ids[TripleStore.PRED_IDX] = LmdbValue.UNKNOWN_ID;
				varNames[TripleStore.PRED_IDX] = null;
				return true;
			}
			if (var.hasValue()) {
				Value value = var.getValue();
				if (!(value instanceof IRI)) {
					return false;
				}
				long id = resolveId(value);
				if (id == LmdbValue.UNKNOWN_ID) {
					return false;
				}
				ids[TripleStore.PRED_IDX] = id;
				varNames[TripleStore.PRED_IDX] = null;
				return true;
			}
			ids[TripleStore.PRED_IDX] = LmdbValue.UNKNOWN_ID;
			varNames[TripleStore.PRED_IDX] = var.getName();
			return true;
		}

		private boolean populateObject(Var var, long[] ids, String[] varNames)
				throws IOException {
			if (var == null) {
				ids[TripleStore.OBJ_IDX] = LmdbValue.UNKNOWN_ID;
				varNames[TripleStore.OBJ_IDX] = null;
				return true;
			}
			if (var.hasValue()) {
				Value value = var.getValue();
				long id = resolveId(value);
				if (id == LmdbValue.UNKNOWN_ID) {
					return false;
				}
				ids[TripleStore.OBJ_IDX] = id;
				varNames[TripleStore.OBJ_IDX] = null;
				return true;
			}
			ids[TripleStore.OBJ_IDX] = LmdbValue.UNKNOWN_ID;
			varNames[TripleStore.OBJ_IDX] = var.getName();
			return true;
		}

		private boolean populateContext(Var var, long[] ids, String[] varNames)
				throws IOException {
			if (var == null) {
				ids[TripleStore.CONTEXT_IDX] = LmdbValue.UNKNOWN_ID;
				varNames[TripleStore.CONTEXT_IDX] = null;
				return true;
			}
			if (var.hasValue()) {
				Value value = var.getValue();
				if (!(value instanceof Resource)) {
					return false;
				}
				Resource ctx = (Resource) value;
				if (ctx.isTriple()) {
					return false;
				}
				long id = resolveId(ctx);
				if (id == LmdbValue.UNKNOWN_ID) {
					return false;
				}
				ids[TripleStore.CONTEXT_IDX] = id;
				varNames[TripleStore.CONTEXT_IDX] = null;
				return true;
			}
			ids[TripleStore.CONTEXT_IDX] = LmdbValue.UNKNOWN_ID;
			varNames[TripleStore.CONTEXT_IDX] = var.getName();
			return true;
		}

		private long selectQueryId(long patternId, long[] binding, int index) {
			if (patternId != LmdbValue.UNKNOWN_ID) {
				return patternId;
			}
			if (index >= 0 && index < binding.length) {
				long bound = binding[index];
				return bound;
			}
			return LmdbValue.UNKNOWN_ID;
		}

		private long[] mergeBinding(long[] binding, long subjId, long predId, long objId, long ctxId, int subjIndex,
				int predIndex, int objIndex, int ctxIndex) {
			long[] out = Arrays.copyOf(binding, binding.length);
			if (!applyValue(out, subjIndex, subjId)) {
				return null;
			}
			if (!applyValue(out, predIndex, predId)) {
				return null;
			}
			if (!applyValue(out, objIndex, objId)) {
				return null;
			}
			if (!applyValue(out, ctxIndex, ctxId)) {
				return null;
			}
			return out;
		}

		private boolean applyValue(long[] target, int index, long value) {
			if (index < 0) {
				return true;
			}
			long existing = target[index];
			if (existing != LmdbValue.UNKNOWN_ID && existing != value) {
				return false;
			}
			target[index] = value;
			return true;
		}

		private long resolveIdWithBindings(long patternId, String varName, BindingSet bindings, boolean requireResource,
				boolean requireIri) throws IOException {
			if (patternId == INVALID_ID) {
				return INVALID_ID;
			}
			if (varName == null) {
				return patternId;
			}
			if (bindings == null) {
				return LmdbValue.UNKNOWN_ID;
			}
			Value bound = bindings.getValue(varName);
			if (bound == null) {
				return LmdbValue.UNKNOWN_ID;
			}
			if (requireResource && !(bound.isResource())) {
				return INVALID_ID;
			}
			if (requireIri && !(bound.isIRI())) {
				return INVALID_ID;
			}
			if (bound.isTriple()) {
				return INVALID_ID;
			}
			long id = resolveId(bound);
			if (id == LmdbValue.UNKNOWN_ID) {
				return INVALID_ID;
			}
			return id;
		}

		private long resolveContextWithBindings(long patternId, String varName, BindingSet bindings)
				throws IOException {
			if (patternId == INVALID_ID) {
				return INVALID_ID;
			}
			if (varName == null) {
				return patternId;
			}
			if (bindings == null) {
				return LmdbValue.UNKNOWN_ID;
			}
			Value bound = bindings.getValue(varName);
			if (bound == null) {
				return LmdbValue.UNKNOWN_ID;
			}
			if (!(bound instanceof Resource)) {
				return INVALID_ID;
			}
			Resource ctx = (Resource) bound;
			if (ctx.isTriple()) {
				return INVALID_ID;
			}
			long id = resolveId(ctx);
			if (id == LmdbValue.UNKNOWN_ID) {
				return INVALID_ID;
			}
			return id;
		}

		private final class PatternArrays {
			private final long[] ids;
			private final String[] varNames;
			private final boolean valid;

			private PatternArrays(long[] ids, String[] varNames, boolean valid) {
				this.ids = ids;
				this.varNames = varNames;
				this.valid = valid;
			}
		}

		private long resolveId(Value value) throws IOException {
			if (value == null) {
				return LmdbValue.UNKNOWN_ID;
			}
			if (value instanceof LmdbValue) {
				LmdbValue lmdbValue = (LmdbValue) value;
				if (valueStore.getRevision().equals(lmdbValue.getValueStoreRevision())) {
					return lmdbValue.getInternalID();
				}
			}
			long id = valueStore.getId(value);
			return id;
		}

		private RecordIterator orderedRecordIterator(StatementOrder order, long subjID, long predID, long objID,
				long contextID) throws IOException {
			return orderedRecordIterator(order, subjID, predID, objID, contextID, null);
		}

		private RecordIterator orderedRecordIterator(StatementOrder order, long subjID, long predID, long objID,
				long contextID, long[] quadReuse) throws IOException {
			if (order == null) {
				return null;
			}
			TripleStore.TripleIndex chosen = chooseIndexForOrder(order, subjID, predID, objID, contextID);
			if (chosen == null) {
				return null;
			}
			boolean rangeSearch = chosen.getPatternScore(subjID, predID, objID, contextID) > 0;
			return new LmdbRecordIterator(chosen, rangeSearch, subjID, predID, objID, contextID, explicit, txn,
					quadReuse);
		}

		private int indexForOrder(StatementOrder order, int subjIndex, int predIndex, int objIndex, int ctxIndex) {
			switch (order) {
			case S:
				return subjIndex;
			case P:
				return predIndex;
			case O:
				return objIndex;
			case C:
				return ctxIndex;
			default:
				return -1;
			}
		}

		private RecordIterator sortedRecordIterator(RecordIterator base, int sortIndex)
				throws QueryEvaluationException {
			List<long[]> rows = new ArrayList<>();
			try (base) {
				long[] next;
				while ((next = base.next()) != null) {
					// Ensure buffered rows are immutable snapshots regardless of iterator reuse
					rows.add(next.clone());
				}
			}
			rows.sort(Comparator.comparingLong(a -> a[sortIndex]));

			Iterator<long[]> iterator = rows.iterator();
			return new RecordIterator() {
				@Override
				public long[] next() {
					if (!iterator.hasNext()) {
						return null;
					}
					return iterator.next();
				}

				@Override
				public void close() {
					// nothing to close
				}
			};
		}

		private TripleStore.TripleIndex chooseIndexForOrder(StatementOrder order, long s, long p, long o, long c)
				throws IOException {
			// ensure metadata initialized
			getSupportedOrdersLookup();
			boolean sBound = s >= 0;
			boolean pBound = p >= 0;
			boolean oBound = o >= 0;
			boolean cBound = c >= 0; // 0 is a concrete (null) context; unknown is -1

			int mask = (sBound ? 1 : 0) | (pBound ? (1 << 1) : 0) | (oBound ? (1 << 2) : 0) | (cBound ? (1 << 3) : 0);
			List<TripleStore.TripleIndex> compat = compatibleIndexesByMask[mask];
			if (compat == null || compat.isEmpty()) {
				return null;
			}
			// If requested order var is bound, any compatible index will do
			if ((order == StatementOrder.S && sBound) || (order == StatementOrder.P && pBound)
					|| (order == StatementOrder.O && oBound) || (order == StatementOrder.C && cBound)) {
				return compat.get(0);
			}
			// Else pick one whose first free variable matches
			for (TripleStore.TripleIndex index : compat) {
				Integer pos = indexPositionMap.get(index);
				if (pos != null) {
					StatementOrder first = firstFreeOrderByIndexAndMask[pos][mask];
					if (first == order) {
						return index;
					}
				}
			}
			return null;
		}

		@Override
		public Comparator<Value> getComparator() {
			return (v1, v2) -> {
				try {
					long id1 = valueStore.getId(v1);
					long id2 = valueStore.getId(v2);
					if (id1 != LmdbValue.UNKNOWN_ID && id2 != LmdbValue.UNKNOWN_ID) {
						return Long.compare(id1, id2);
					}
				} catch (IOException ignore) {
					// fall through to lexical comparator
				}
				// Fallback to standard SPARQL value comparator when IDs are not available
				return new ValueComparator().compare(v1, v2);
			};
		}
	}
}
