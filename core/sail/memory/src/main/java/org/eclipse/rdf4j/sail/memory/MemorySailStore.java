/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.LockingIteration;
import org.eclipse.rdf4j.common.concurrent.locks.Properties;
import org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManager;
import org.eclipse.rdf4j.common.concurrent.locks.ReadWriteLockManager;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockDiagnostics;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.BackingSailSource;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.memory.model.MemBNode;
import org.eclipse.rdf4j.sail.memory.model.MemIRI;
import org.eclipse.rdf4j.sail.memory.model.MemResource;
import org.eclipse.rdf4j.sail.memory.model.MemStatement;
import org.eclipse.rdf4j.sail.memory.model.MemStatementIterator;
import org.eclipse.rdf4j.sail.memory.model.MemStatementIteratorCache;
import org.eclipse.rdf4j.sail.memory.model.MemStatementList;
import org.eclipse.rdf4j.sail.memory.model.MemTriple;
import org.eclipse.rdf4j.sail.memory.model.MemValue;
import org.eclipse.rdf4j.sail.memory.model.MemValueFactory;
import org.eclipse.rdf4j.sail.memory.model.WeakObjectRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link SailStore} that keeps committed statements in a {@link MemStatementList}.
 *
 * @author James Leigh
 */
class MemorySailStore implements SailStore {

	public static final EmptyIteration<MemStatement, SailException> EMPTY_ITERATION = new EmptyIteration<>();
	public static final EmptyIteration<MemTriple, SailException> EMPTY_TRIPLE_ITERATION = new EmptyIteration<>();
	public static final MemResource[] EMPTY_CONTEXT = new MemResource[0];
	private final static Logger logger = LoggerFactory.getLogger(MemorySailStore.class);

	private final MemStatementIteratorCache iteratorCache = new MemStatementIteratorCache(10);

	/**
	 * Factory/cache for MemValue objects.
	 */
	private final MemValueFactory valueFactory = new MemValueFactory();

	/**
	 * List containing all available statements.
	 */
	private final MemStatementList statements = new MemStatementList(256);

	/**
	 * This gets set to `true` when we add our first inferred statement. If the value is `false` we guarantee that there
	 * are no inferred statements in the MemorySailStore. If it is `true` then an inferred statement was added at some
	 * point, but we make no guarantees regarding if there still are inferred statements or if they are in the current
	 * snapshot.
	 *
	 * The purpose of this variable is to optimize read operations that only read inferred statements when there are no
	 * inferred statements.
	 */
	private volatile boolean mayHaveInferred = false;

	/**
	 * Identifies the current snapshot.
	 */
	private volatile int currentSnapshot;

	/**
	 * Store for namespace prefix info.
	 */
	private final MemNamespaceStore namespaceStore = new MemNamespaceStore();

	/**
	 * Lock manager used to give the snapshot cleanup thread exclusive access to the statement list.
	 */
	private final ReadWriteLockManager statementListLockManager;

	/**
	 * Lock manager used to prevent concurrent writes.
	 */
	private final ReentrantLock txnLockManager = new ReentrantLock();

	/**
	 * Cleanup thread that removes deprecated statements when no other threads are accessing this list. Seee
	 * {@link #scheduleSnapshotCleanup()}.
	 */
	private volatile Thread snapshotCleanupThread;

	/**
	 * Lock object used to synchronize concurrent access to {@link #snapshotCleanupThread}.
	 */
	private final Object snapshotCleanupThreadLockObject = new Object();

	public MemorySailStore(boolean debug) {
		if (debug || Properties.lockTrackingEnabled()) {
			statementListLockManager = new ReadPrefReadWriteLockManager("MemorySailStore statementListLockManager",
					LockDiagnostics.releaseAbandoned, LockDiagnostics.detectStalledOrDeadlock,
					LockDiagnostics.stackTrace);
		} else {
			statementListLockManager = new ReadPrefReadWriteLockManager("MemorySailStore statementListLockManager");
		}

	}

	@Override
	public ValueFactory getValueFactory() {
		return valueFactory;
	}

	@Override
	public void close() {
		try {
			synchronized (snapshotCleanupThreadLockObject) {
				if (snapshotCleanupThread != null) {
					snapshotCleanupThread.interrupt();
				}
			}
			Lock stLock = statementListLockManager.getWriteLock();
			try {
				valueFactory.clear();
				statements.clear();
				invalidateCache();
			} finally {
				if (stLock.isActive()) {
					stLock.release();
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void invalidateCache() {
		iteratorCache.invalidateCache();
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return new MemEvaluationStatistics(valueFactory, statements);
	}

	@Override
	public SailSource getExplicitSailSource() {
		return new MemorySailSource(true);
	}

	@Override
	public SailSource getInferredSailSource() {
		return new MemorySailSource(false);
	}

	private Lock openStatementsReadLock() throws SailException {
		try {
			return statementListLockManager.getReadLock();
		} catch (InterruptedException e) {
			throw convertToSailException(e);
		}
	}

	/**
	 * Creates a StatementIterator that contains the statements matching the specified pattern of subject, predicate,
	 * object, context. Inferred statements are excluded when <var>explicitOnly</var> is set to <var>true</var> .
	 * Statements from the null context are excluded when <var>namedContextsOnly</var> is set to <var>true</var>. The
	 * returned StatementIterator will assume the specified read mode.
	 */
	private CloseableIteration<MemStatement, SailException> createStatementIterator(Resource subj, IRI pred, Value obj,
			Boolean explicit, int snapshot, Resource... contexts) {
		// Perform look-ups for value-equivalents of the specified values

		MemResource memSubj = valueFactory.getMemResource(subj);
		if (subj != null && memSubj == null) {
			// non-existent subject
			return EMPTY_ITERATION;
		}

		MemIRI memPred = valueFactory.getMemURI(pred);
		if (pred != null && memPred == null) {
			// non-existent predicate
			return EMPTY_ITERATION;
		}

		MemValue memObj = valueFactory.getMemValue(obj);
		if (obj != null && memObj == null) {
			// non-existent object
			return EMPTY_ITERATION;
		}

		MemResource[] memContexts;
		MemStatementList smallestList;

		if (contexts.length == 0) {
			memContexts = EMPTY_CONTEXT;
			smallestList = statements;
		} else if (contexts.length == 1) {
			if (contexts[0] == null) {
				memContexts = new MemResource[] { null };
				smallestList = statements;

			} else {
				MemResource memContext = valueFactory.getMemResource(contexts[0]);
				if (memContext == null) {
					// non-existent context
					return EMPTY_ITERATION;
				}

				memContexts = new MemResource[] { memContext };
				smallestList = memContext.getContextStatementList();
			}

		} else {
			Set<MemResource> contextSet = new LinkedHashSet<>(2 * contexts.length);

			for (Resource context : contexts) {
				MemResource memContext = valueFactory.getMemResource(context);
				if (context == null || memContext != null) {
					contextSet.add(memContext);
				}
			}

			if (contextSet.isEmpty()) {
				// no known contexts specified
				return EMPTY_ITERATION;
			}

			memContexts = contextSet.toArray(new MemResource[0]);
			smallestList = statements;
		}

		return getMemStatementIterator(memSubj, memPred, memObj, explicit, snapshot, memContexts,
				smallestList);
	}

	private CloseableIteration<MemStatement, SailException> createStatementIterator(MemResource subj, MemIRI pred,
			MemValue obj, MemResource... contexts) {

		MemResource[] memContexts;
		MemStatementList smallestList;

		if (contexts.length == 0) {
			memContexts = EMPTY_CONTEXT;
			smallestList = statements;
		} else if (contexts.length == 1 && contexts[0] != null) {
			memContexts = contexts;
			smallestList = contexts[0].getContextStatementList();
		} else {
			memContexts = contexts;
			smallestList = statements;
		}

		return getMemStatementIterator(subj, pred, obj, null, Integer.MAX_VALUE - 1, memContexts, smallestList);
	}

	private CloseableIteration<MemStatement, SailException> getMemStatementIterator(MemResource subj, MemIRI pred,
			MemValue obj, Boolean explicit, int snapshot, MemResource[] memContexts, MemStatementList statementList) {

		if (explicit != null && !explicit) {
			// we are looking for inferred statements
			if (!mayHaveInferred) {
				return EMPTY_ITERATION;
			}
		}

		MemStatementList smallestList = statementList;

		if (subj != null) {
			MemStatementList l = subj.getSubjectStatementList();
			if (l.size() < smallestList.size()) {
				smallestList = l;
			}
		}

		if (pred != null) {
			MemStatementList l = pred.getPredicateStatementList();
			if (l.size() < smallestList.size()) {
				smallestList = l;
			}
		}

		if (obj != null) {
			MemStatementList l = obj.getObjectStatementList();
			if (l.size() < smallestList.size()) {
				smallestList = l;
			}
		}

		if (smallestList.isEmpty()) {
			return EMPTY_ITERATION;
		}

		return MemStatementIterator.cacheAwareInstance(smallestList, subj, pred, obj, explicit, snapshot, memContexts,
				iteratorCache);
	}

	/**
	 * Creates a TripleIterator that contains the triples matching the specified pattern of subject, predicate, object,
	 * context.
	 */
	private CloseableIteration<MemTriple, SailException> createTripleIterator(Resource subj, IRI pred, Value obj,
			int snapshot) {
		// Perform look-ups for value-equivalents of the specified values

		MemResource memSubj = valueFactory.getMemResource(subj);

		if (subj != null && memSubj == null) {
			// non-existent subject
			return EMPTY_TRIPLE_ITERATION;
		}

		MemIRI memPred = valueFactory.getMemURI(pred);
		if (pred != null && memPred == null) {
			// non-existent predicate
			return EMPTY_TRIPLE_ITERATION;
		}

		MemValue memObj = valueFactory.getMemValue(obj);
		if (obj != null && memObj == null) {
			// non-existent object
			return EMPTY_TRIPLE_ITERATION;
		}

		// TODO there is no separate index for Triples, so for now we iterate over all statements to find matches.
		return new MemTripleIterator<>(statements, memSubj, memPred, memObj, snapshot);
	}

	/**
	 * Removes statements from old snapshots from the main statement list and resets the snapshot to 1 for the rest of
	 * the statements.
	 *
	 * @throws InterruptedException
	 */
	protected void cleanSnapshots() throws InterruptedException {

		StopWatch stopWatch = null;
		if (logger.isDebugEnabled()) {
			stopWatch = StopWatch.createStarted();
			logger.debug("Started cleaning snapshots.");
		}

		// Sets used to keep track of which lists have already been processed
		HashSet<MemValue> processedSubjects = new HashSet<>();
		HashSet<MemValue> processedPredicates = new HashSet<>();
		HashSet<MemValue> processedObjects = new HashSet<>();
		HashSet<MemValue> processedContexts = new HashSet<>();

		int lastStmtPos;
		Lock stReadLock = statementListLockManager.getReadLock();
		try {
			lastStmtPos = statements.size() - 1;
		} finally {
			if (stReadLock.isActive()) {
				stReadLock.release();
			}
		}

		/*
		 * The order of the statement list won't change from lastStmtPos down while we don't have the write lock (it
		 * might shrink or grow) as (1) new statements are always appended last, (2) we are the only process that
		 * removes statements, (3) this list is cleared on close.
		 */

		int nextSnapshot = currentSnapshot;
		for (int i = lastStmtPos; i >= 0; i--) {
			if (Thread.currentThread().isInterrupted()) {
				break;
			}

			// As we are running in the background, yield the write lock frequently to other writers.
			Lock stWriteLock = statementListLockManager.getWriteLock();
			try {
				// guard against shrinkage, e.g. clear() on close()
				lastStmtPos = statements.size() - 1;
				i = Math.min(i, lastStmtPos);
				if (i >= 0) {
					MemStatement st = statements.get(i);

					if (st.getTillSnapshot() <= nextSnapshot) {
						MemResource subj = st.getSubject();
						if (processedSubjects.add(subj)) {
							subj.cleanSnapshotsFromSubjectStatements(nextSnapshot);
						}

						MemIRI pred = st.getPredicate();
						if (processedPredicates.add(pred)) {
							pred.cleanSnapshotsFromPredicateStatements(nextSnapshot);
						}

						MemValue obj = st.getObject();
						if (processedObjects.add(obj)) {
							obj.cleanSnapshotsFromObjectStatements(nextSnapshot);
						}

						MemResource context = st.getContext();
						if (context != null && processedContexts.add(context)) {
							context.cleanSnapshotsFromContextStatements(nextSnapshot);
						}

						// stale statement
						statements.remove(i);
					}
				}
			} finally {
				stWriteLock.release();

			}
		}

		if (logger.isDebugEnabled() && stopWatch != null) {
			stopWatch.stop();
			logger.debug("Cleaning snapshots took {} seconds.", stopWatch.getTime(TimeUnit.SECONDS));
		}
	}

	protected void scheduleSnapshotCleanup() {
		synchronized (snapshotCleanupThreadLockObject) {
			Thread toCheckSnapshotCleanupThread = snapshotCleanupThread;
			if (toCheckSnapshotCleanupThread == null || !toCheckSnapshotCleanupThread.isAlive()) {
				Runnable runnable = () -> {
					try {
						// sleep for 10 seconds because we don't need to start snapshot cleanup immediately
						Thread.sleep(10 * 1000);
						cleanSnapshots();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						logger.info("snapshot cleanup interrupted");
					}
				};

				toCheckSnapshotCleanupThread = snapshotCleanupThread = new Thread(runnable,
						"MemoryStore snapshot cleanup");
				toCheckSnapshotCleanupThread.setDaemon(true);
				toCheckSnapshotCleanupThread.start();
			}
		}
	}

	private final class MemorySailSource extends BackingSailSource {

		private final boolean explicit;

		public MemorySailSource(boolean explicit) {
			this.explicit = explicit;
		}

		@Override
		public SailSink sink(IsolationLevel level) throws SailException {
			return new MemorySailSink(explicit, level.isCompatibleWith(IsolationLevels.SERIALIZABLE));
		}

		@Override
		public MemorySailDataset dataset(IsolationLevel level) throws SailException {
			if (level.isCompatibleWith(IsolationLevels.SNAPSHOT_READ)) {
				return new MemorySailDataset(explicit, currentSnapshot);
			} else {
				return new MemorySailDataset(explicit);
			}
		}
	}

	private final class MemorySailSink implements SailSink {

		private final boolean explicit;

		private final int serializable;

		private final Lock txnStLock;

		private volatile int nextSnapshot;

		private volatile Set<StatementPattern> observations;

		private volatile boolean txnLock;

		private boolean requireCleanup;

		public MemorySailSink(boolean explicit, boolean serializable) throws SailException {
			this.explicit = explicit;
			if (serializable) {
				this.serializable = currentSnapshot;
			} else {
				this.serializable = Integer.MAX_VALUE;
			}
			txnStLock = openStatementsReadLock();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (explicit) {
				sb.append("explicit ");
			} else {
				sb.append("inferred ");
			}
			if (txnLock) {
				sb.append("snapshot ").append(nextSnapshot);
			} else {
				sb.append(super.toString());
			}
			return sb.toString();
		}

		@Override
		public synchronized void prepare() throws SailException {
			acquireExclusiveTransactionLock();
			if (observations != null) {
				for (StatementPattern p : observations) {
					Resource subj = (Resource) p.getSubjectVar().getValue();
					IRI pred = (IRI) p.getPredicateVar().getValue();
					Value obj = p.getObjectVar().getValue();
					Var ctxVar = p.getContextVar();
					Resource[] contexts;
					if (ctxVar == null) {
						contexts = new Resource[0];
					} else {
						contexts = new Resource[] { (Resource) ctxVar.getValue() };
					}
					try (CloseableIteration<MemStatement, SailException> iter = createStatementIterator(subj, pred, obj,
							null, -1, contexts)) {
						while (iter.hasNext()) {
							MemStatement st = iter.next();
							int since = st.getSinceSnapshot();
							int till = st.getTillSnapshot();
							if (serializable < since && since < nextSnapshot
									|| serializable < till && till < nextSnapshot) {
								throw new SailConflictException("Observed State has Changed");
							}
						}
					}
				}
			}
		}

		@Override
		public synchronized void flush() throws SailException {
			if (txnLock) {
				invalidateCache();
				currentSnapshot = Math.max(currentSnapshot, nextSnapshot);
				if (requireCleanup) {
					scheduleSnapshotCleanup();
				}
			}
		}

		@Override
		public void close() {
			try {
				boolean toCloseTxnLock = txnLock;
				txnLock = false;
				if (toCloseTxnLock) {
					txnLockManager.unlock();
				}
			} finally {
				if (txnStLock.isActive()) {
					txnStLock.release();
				}
			}
		}

		@Override
		public synchronized void setNamespace(String prefix, String name) throws SailException {
			acquireExclusiveTransactionLock();
			namespaceStore.setNamespace(prefix, name);
		}

		@Override
		public synchronized void removeNamespace(String prefix) throws SailException {
			acquireExclusiveTransactionLock();
			namespaceStore.removeNamespace(prefix);
		}

		@Override
		public synchronized void clearNamespaces() throws SailException {
			acquireExclusiveTransactionLock();
			namespaceStore.clear();
		}

		@Override
		public synchronized void observe(Resource subj, IRI pred, Value obj, Resource... contexts)
				throws SailException {
			if (observations == null) {
				observations = new HashSet<>();
			}
			if (contexts == null) {
				observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj),
						new Var("g", null)));
			} else if (contexts.length == 0) {
				observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj)));
			} else {
				for (Resource ctx : contexts) {
					observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj),
							new Var("g", ctx)));
				}
			}
		}

		@Override
		public synchronized void clear(Resource... contexts) throws SailException {
			acquireExclusiveTransactionLock();
			invalidateCache();
			requireCleanup = true;
			try (CloseableIteration<MemStatement, SailException> iter = createStatementIterator(null, null, null,
					explicit, nextSnapshot, contexts)) {
				while (iter.hasNext()) {
					MemStatement st = iter.next();
					st.setTillSnapshot(nextSnapshot);
				}
			}
		}

		@Override
		public synchronized void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {
			acquireExclusiveTransactionLock();
			invalidateCache();
			addStatement(subj, pred, obj, ctx, explicit);
		}

		@Override
		public synchronized void approve(Statement statement) throws SailException {
			acquireExclusiveTransactionLock();
			invalidateCache();
			addStatement(statement.getSubject(), statement.getPredicate(), statement.getObject(),
					statement.getContext(), explicit);
		}

		@Override
		public synchronized void deprecate(Statement statement) throws SailException {
			acquireExclusiveTransactionLock();
			invalidateCache();
			requireCleanup = true;
			if (statement instanceof MemStatement) {
				MemStatement toDeprecate = (MemStatement) statement;
				if ((nextSnapshot < 0 || toDeprecate.isInSnapshot(nextSnapshot))
						&& toDeprecate.isExplicit() == explicit) {
					toDeprecate.setTillSnapshot(nextSnapshot);
				}
			} else if (statement instanceof LinkedHashModel.ModelStatement
					&& ((LinkedHashModel.ModelStatement) statement).getStatement() instanceof MemStatement) {
				// The Changeset uses a LinkedHashModel to store it's changes. It still keeps a reference to the
				// original statement that can be retrieved here.
				MemStatement toDeprecate = (MemStatement) ((LinkedHashModel.ModelStatement) statement).getStatement();
				if ((nextSnapshot < 0 || toDeprecate.isInSnapshot(nextSnapshot))
						&& toDeprecate.isExplicit() == explicit) {
					toDeprecate.setTillSnapshot(nextSnapshot);
				}
			} else {
				try (CloseableIteration<MemStatement, SailException> iter = createStatementIterator(
						statement.getSubject(),
						statement.getPredicate(), statement.getObject(),
						explicit, nextSnapshot, statement.getContext())) {
					while (iter.hasNext()) {
						MemStatement st = iter.next();
						st.setTillSnapshot(nextSnapshot);
					}
				}
			}

		}

		private void acquireExclusiveTransactionLock() throws SailException {
			if (!txnLock) {
				txnLockManager.lock();
				nextSnapshot = currentSnapshot + 1;
				txnLock = true;
			}
		}

		private MemStatement addStatement(Resource subj, IRI pred, Value obj, Resource context, boolean explicit)
				throws SailException {
			if (!explicit) {
				mayHaveInferred = true;
			}

			// Get or create MemValues for the operands
			MemResource memSubj = valueFactory.getOrCreateMemResource(subj);
			MemIRI memPred = valueFactory.getOrCreateMemURI(pred);
			MemValue memObj = valueFactory.getOrCreateMemValue(obj);
			MemResource memContext = (context == null) ? null : valueFactory.getOrCreateMemResource(context);

			if (memSubj.hasSubjectStatements() && memPred.hasPredicateStatements() && memObj.hasObjectStatements()
					&& (memContext == null || memContext.hasContextStatements())) {
				// All values are used in at least one statement. Possibly, the
				// statement is already present. Check this.

				if (statementAlreadyExists(explicit, memSubj, memPred, memObj, memContext)) {
					return null;
				}
			}

			// completely new statement
			MemStatement st = new MemStatement(memSubj, memPred, memObj, memContext, explicit, nextSnapshot);
			statements.add(st);
			st.addToComponentLists();
			invalidateCache();
			return st;
		}

		private boolean statementAlreadyExists(boolean explicit, MemResource memSubj, MemIRI memPred, MemValue memObj,
				MemResource memContext) {
			try (CloseableIteration<MemStatement, SailException> stIter = createStatementIterator(memSubj, memPred,
					memObj, memContext)) {
				if (stIter.hasNext()) {
					// statement is already present, update its transaction
					// status if appropriate
					MemStatement st = stIter.next();

					if (!st.isExplicit() && explicit) {
						// Implicit statement is now added explicitly
						st.setTillSnapshot(nextSnapshot);
					} else if (!st.isInSnapshot(nextSnapshot)) {
						st.setSinceSnapshot(nextSnapshot);
					} else {
						// statement already exists
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean deprecateByQuery(Resource subj, IRI pred, Value obj, Resource[] contexts) {
			acquireExclusiveTransactionLock();
			boolean deprecated = false;
			requireCleanup = true;
			try (CloseableIteration<MemStatement, SailException> iter = createStatementIterator(subj, pred, obj,
					explicit, nextSnapshot, contexts)) {
				while (iter.hasNext()) {
					deprecated = true;
					MemStatement st = iter.next();
					st.setTillSnapshot(nextSnapshot);
				}
			}
			invalidateCache();

			return deprecated;
		}

	}

	/**
	 * @author James Leigh
	 */
	private final class MemorySailDataset implements SailDataset {

		private final boolean explicit;

		private final int snapshot;

		private final Lock lock;

		public MemorySailDataset(boolean explicit) throws SailException {
			this.explicit = explicit;
			this.snapshot = -1;
			this.lock = null;
		}

		public MemorySailDataset(boolean explicit, int snapshot) throws SailException {
			this.explicit = explicit;
			this.snapshot = snapshot;
			this.lock = openStatementsReadLock();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (explicit) {
				sb.append("explicit ");
			} else {
				sb.append("inferred ");
			}
			if (snapshot >= 0) {
				sb.append("snapshot ").append(snapshot);
			} else {
				sb.append(super.toString());
			}
			return sb.toString();
		}

		@Override
		public void close() {
			if (lock != null) {
				// serializable read or higher isolation
				lock.release();
			}
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
			// Note: we can't do this in a streaming fashion due to concurrency
			// issues; iterating over the set of IRIs or bnodes while another
			// thread
			// adds statements with new resources would result in
			// ConcurrentModificationException's (issue SES-544).

			// Create a list of all resources that are used as contexts
			ArrayList<MemResource> contextIDs = new ArrayList<>(32);

			boolean locked = false;
			try {
				locked = statementListLockManager.lockReadLock();
				int snapshot = getCurrentSnapshot();
				try (WeakObjectRegistry.AutoCloseableIterator<MemIRI> memIRIsIterator = valueFactory
						.getMemIRIsIterator()) {
					while (memIRIsIterator.hasNext()) {
						MemResource memResource = memIRIsIterator.next();
						if (isContextResource(memResource, snapshot)) {
							contextIDs.add(memResource);
						}
					}
				}

				try (WeakObjectRegistry.AutoCloseableIterator<MemBNode> memBNodesIterator = valueFactory
						.getMemBNodesIterator()) {
					while (memBNodesIterator.hasNext()) {
						MemResource memResource = memBNodesIterator.next();
						if (isContextResource(memResource, snapshot)) {
							contextIDs.add(memResource);
						}
					}
				}

			} catch (InterruptedException e) {
				throw convertToSailException(e);
			} finally {
				statementListLockManager.unlockReadLock(locked);
			}

			return new CloseableIteratorIteration<>(contextIDs.iterator());
		}

		@Override
		public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) throws SailException {
			CloseableIteration<? extends Statement, SailException> stIter1 = null;
			boolean allGood = false;
			Lock stLock = openStatementsReadLock();
			try {
				stIter1 = createStatementIterator(subj, pred, obj, explicit, getCurrentSnapshot(), contexts);
				CloseableIteration<? extends Statement, SailException> stIter2 = LockingIteration.getInstance(stLock,
						stIter1);
				allGood = true;
				return stIter2;
			} finally {
				if (!allGood) {
					try {
						if (stIter1 != null) {
							stIter1.close();
						}
					} finally {
						if (stLock != null) {
							stLock.release();
						}
					}
				}
			}
		}

		@Override
		public boolean hasStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
			boolean locked = false;
			try {
				locked = statementListLockManager.lockReadLock();

				try (CloseableIteration<MemStatement, SailException> iterator = createStatementIterator(subj, pred, obj,
						explicit, getCurrentSnapshot(), contexts)) {
					return iterator.hasNext();
				}

			} catch (InterruptedException e) {
				throw convertToSailException(e);
			} finally {
				statementListLockManager.unlockReadLock(locked);
			}
		}

		@Override
		public CloseableIteration<? extends Triple, SailException> getTriples(Resource subj, IRI pred, Value obj)
				throws SailException {
			CloseableIteration<? extends Triple, SailException> stIter1 = null;

			boolean allGood = false;
			Lock stLock = openStatementsReadLock();
			try {
				stIter1 = createTripleIterator(subj, pred, obj, getCurrentSnapshot());
				CloseableIteration<? extends Triple, SailException> stIter2 = LockingIteration.getInstance(stLock,
						stIter1);
				allGood = true;
				return stIter2;
			} finally {
				if (!allGood) {
					try {
						if (stIter1 != null) {
							stIter1.close();
						}
					} finally {
						if (stLock != null) {
							stLock.release();
						}
					}
				}
			}
		}

		private int getCurrentSnapshot() {
			if (snapshot >= 0) {
				return snapshot;
			} else {
				return currentSnapshot;
			}
		}

		private boolean isContextResource(MemResource memResource, int snapshot) throws SailException {
			MemStatementList contextStatements = memResource.getContextStatementList();

			// Filter resources that are not used as context identifier
			if (contextStatements.size() == 0) {
				return false;
			}

			// Filter more thoroughly by considering snapshot and read-mode
			// parameters
			try (MemStatementIterator<SailException> iter = new MemStatementIterator<>(contextStatements, null, null,
					null, null, snapshot)) {
				return iter.hasNext();
			}
		}

	}

	private SailException convertToSailException(InterruptedException e) {
		Thread.currentThread().interrupt();
		return new SailException(e);
	}
}
