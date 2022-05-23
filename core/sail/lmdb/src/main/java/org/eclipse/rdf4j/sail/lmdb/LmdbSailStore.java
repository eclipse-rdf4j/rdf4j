/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

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
 *
 */
class LmdbSailStore implements SailStore {

	final Logger logger = LoggerFactory.getLogger(LmdbSailStore.class);

	private final TripleStore tripleStore;

	private final ValueStore valueStore;

	private final ExecutorService tripleStoreExecutor = Executors.newCachedThreadPool();
	private final CircularBuffer<Operation> opQueue = new CircularBuffer<>(1024);
	private volatile Throwable tripleStoreException;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private boolean multiThreadingActive;
	private volatile boolean asyncTransactionFinished;
	private volatile boolean nextTransactionAsync;

	private final boolean enableMultiThreading = true;

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
			boolean wasNew = tripleStore.storeTriple(s, p, o, c, explicit);
			if (wasNew && context != null) {
				contextStore.increment(context);
			}
		}
	}

	/**
	 * Super-class for operations that capture their finished state.
	 */
	abstract static class StatefulOperation implements Operation {
		volatile boolean finished = false;
	}

	private final NamespaceStore namespaceStore;

	private final ContextStore contextStore;

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
		boolean initialized = false;
		try {
			namespaceStore = new NamespaceStore(dataDir);
			valueStore = new ValueStore(new File(dataDir, "values"), config);
			tripleStore = new TripleStore(new File(dataDir, "triples"), config);
			contextStore = new ContextStore(this, dataDir);
			initialized = true;
		} finally {
			if (!initialized) {
				close();
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
					if (contextStore != null) {
						contextStore.close();
					}
				} finally {
					try {
						if (valueStore != null) {
							valueStore.close();
						}
					} finally {
						if (tripleStore != null) {
							running.set(false);
							tripleStoreExecutor.shutdown();
							while (!tripleStoreExecutor.isTerminated()) {
								Thread.yield();
							}
							tripleStore.close();
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

	CloseableIteration<Resource, SailException> getContexts() throws IOException {
		Txn txn = tripleStore.getTxnManager().createReadTxn();
		RecordIterator records = tripleStore.getAllTriplesSortedByContext(txn);
		CloseableIteration<? extends Statement, SailException> stIter1;
		if (records == null) {
			// Iterator over all statements
			stIter1 = createStatementIterator(txn, null, null, null, true);
		} else {
			stIter1 = new LmdbStatementIterator(records, valueStore);
		}

		FilterIteration<Statement, SailException> stIter2 = new FilterIteration<>(
				stIter1) {
			@Override
			protected boolean accept(Statement st) {
				return st.getContext() != null;
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
	CloseableIteration<? extends Statement, SailException> createStatementIterator(
			Txn txn, Resource subj, IRI pred, Value obj, boolean explicit, Resource... contexts) throws IOException {
		long subjID = LmdbValue.UNKNOWN_ID;
		if (subj != null) {
			subjID = valueStore.getId(subj);
			if (subjID == LmdbValue.UNKNOWN_ID) {
				return new EmptyIteration<>();
			}
		}

		long predID = LmdbValue.UNKNOWN_ID;
		if (pred != null) {
			predID = valueStore.getId(pred);
			if (predID == LmdbValue.UNKNOWN_ID) {
				return new EmptyIteration<>();
			}
		}

		long objID = LmdbValue.UNKNOWN_ID;
		if (obj != null) {
			objID = valueStore.getId(obj);

			if (objID == LmdbValue.UNKNOWN_ID) {
				return new EmptyIteration<>();
			}
		}

		List<Long> contextIDList = new ArrayList<>(contexts.length);
		if (contexts.length == 0) {
			contextIDList.add(LmdbValue.UNKNOWN_ID);
		} else {
			for (Resource context : contexts) {
				if (context == null) {
					contextIDList.add(0L);
				} else {
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
					try {
						contextStore.sync();
					} finally {
						if (activeTxn) {
							valueStore.commit();
							if (!multiThreadingActive) {
								tripleStore.commit();
							}
							// do not set flag to false until _after_ commit is successfully completed.
							storeTxnStarted.set(false);
						}
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
													Thread.yield();
												}
											}

											// keep thread running for at least 2ms to lock-free wait for the next
											// transaction
											long start = System.currentTimeMillis();
											while (running.get() && !nextTransactionAsync) {
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
						valueStore.startTransaction();
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
							Thread.yield();
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
			long removeCount = 0;
			for (long contextId : contexts) {
				Map<Long, Long> result = tripleStore.removeTriplesByContext(subj, pred, obj, contextId, explicit);

				for (Entry<Long, Long> entry : result.entrySet()) {
					Long entryContextId = entry.getKey();
					if (entryContextId > 0) {
						Resource modifiedContext = (Resource) valueStore.getValue(entryContextId);
						contextStore.decrementBy(modifiedContext, entry.getValue());
					}
					removeCount += entry.getValue();
				}
			}
			return removeCount;
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
		public CloseableIteration<? extends Namespace, SailException> getNamespaces() {
			return new CloseableIteratorIteration<Namespace, SailException>(namespaceStore.iterator());
		}

		@Override
		public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
			return new CloseableIteratorIteration<>(contextStore.iterator());
		}

		@Override
		public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) throws SailException {
			try {
				return createStatementIterator(txn, subj, pred, obj, explicit, contexts);
			} catch (IOException e) {
				throw new SailException("Unable to get statements", e);
			}
		}
	}

}
