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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
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
import org.eclipse.rdf4j.common.iteration.DualUnionIteration;
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

	final Logger logger = LoggerFactory.getLogger(LmdbSailStore.class);

	private final TripleStore tripleStore;

	private final ValueStore valueStore;

	// Precomputed lookup: for each bound-mask (bits for S=1,P=2,O=4,C=8), the union of
	// supported StatementOrder across all configured indexes that are compatible with that mask.
	@SuppressWarnings("unchecked")
	private volatile EnumSet<StatementOrder>[] supportedOrdersLookup = (EnumSet<StatementOrder>[]) new EnumSet[16];

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
	final class CircularBuffer<T> {

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
		}
		List<TripleStore.TripleIndex> indexes = tripleStore.getAllIndexes();
		char[][] seqs = new char[indexes.size()][];
		for (int i = 0; i < indexes.size(); i++) {
			seqs[i] = indexes.get(i).getFieldSeq();
		}
		for (int mask = 0; mask < 16; mask++) {
			EnumSet<StatementOrder> set = table[mask];
			boolean anyCompatible = false;
			for (char[] seq : seqs) {
				if (!isIndexCompatible(seq, mask)) {
					continue;
				}
				anyCompatible = true;
				// add bound dimensions once per compatible index; set deduplicates
				if ((mask & 1) != 0)
					set.add(StatementOrder.S);
				if ((mask & (1 << 1)) != 0)
					set.add(StatementOrder.P);
				if ((mask & (1 << 2)) != 0)
					set.add(StatementOrder.O);
				if ((mask & (1 << 3)) != 0)
					set.add(StatementOrder.C);

				// add first free variable per index
				for (char f : seq) {
					if ((mask & bitFor(f)) == 0) {
						set.add(orderFor(f));
						break;
					}
				}
			}
			if (!anyCompatible) {
				set.clear();
			}
		}
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
			return new LmdbStatementIterator(records, valueStore);
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
			perContextIterList.add(new LmdbStatementIterator(records, valueStore));
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
			return new LmdbSailDataset(explicit);
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

	private final class LmdbSailDataset implements SailDataset {

		private final boolean explicit;
		private final Txn txn;

		public LmdbSailDataset(boolean explicit) throws SailException {
			this.explicit = explicit;
			try {
				this.txn = tripleStore.getTxnManager().createReadTxn();
			} catch (IOException e) {
				throw new SailException(e);
			}
		}

		@Override
		public void close() {
			// close the associated txn
			txn.close();
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
				RecordIterator records = new LmdbRecordIterator(chosen, rangeSearch, subjID, predID, objID, contextID,
						explicit, txn);
				return new LmdbStatementIterator(records, valueStore);
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

		private boolean isIndexCompatible(char[] seq, boolean sBound, boolean pBound, boolean oBound, boolean cBound) {
			boolean seenUnbound = false;
			for (char f : seq) {
				boolean bound = isBound(f, sBound, pBound, oBound, cBound);
				if (!bound) {
					seenUnbound = true;
				} else if (seenUnbound) {
					// bound after an unbound earlier field -> cannot use index prefix, not compatible
					return false;
				}
			}
			return true;
		}

		private boolean isBound(char f, boolean sBound, boolean pBound, boolean oBound, boolean cBound) {
			switch (f) {
			case 's':
				return sBound;
			case 'p':
				return pBound;
			case 'o':
				return oBound;
			case 'c':
				return cBound;
			default:
				return false;
			}
		}

		private StatementOrder toStatementOrder(char f) {
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

		private TripleStore.TripleIndex chooseIndexForOrder(StatementOrder order, long s, long p, long o, long c)
				throws IOException {
			boolean sBound = s >= 0;
			boolean pBound = p >= 0;
			boolean oBound = o >= 0;
			boolean cBound = c >= 0; // 0 is a concrete (null) context; unknown is -1

			for (TripleStore.TripleIndex index : tripleStore.getAllIndexes()) {
				char[] seq = index.getFieldSeq();
				if (!isIndexCompatible(seq, sBound, pBound, oBound, cBound)) {
					continue;
				}

				// If requested order variable is bound, any compatible index will do
				if ((order == StatementOrder.S && sBound) || (order == StatementOrder.P && pBound)
						|| (order == StatementOrder.O && oBound) || (order == StatementOrder.C && cBound)) {
					return index;
				}

				// Otherwise, ensure the requested variable is the first unbound dimension in this index
				for (char f : seq) {
					boolean bound = isBound(f, sBound, pBound, oBound, cBound);
					if (!bound) {
						if (toStatementOrder(f) == order) {
							return index;
						} else {
							break; // first unbound is different -> this index can't provide the requested order
						}
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
