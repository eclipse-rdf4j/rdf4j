/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.ConcurrentCleaner;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
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
import org.eclipse.rdf4j.sail.memory.model.MemTripleIterator;
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

	private final static Logger logger = LoggerFactory.getLogger(MemorySailStore.class);
	private static final Runtime RUNTIME = Runtime.getRuntime();

	// Maximum that can be allocated.
	private static final long MAX_MEMORY = RUNTIME.maxMemory();

	// Small heaps (small values for MAX_MEMORY) would trigger the cleanup priority too often. This is the threshold for
	// running the code that checks for low memory and priorities cleanup.
	private static final int CLEANUP_MAX_MEMORY_THRESHOLD = 256 * 1024 * 1024;

	// A constant for the absolute lowest amount of free memory before we prioritise cleanup.
	private static final int CLEANUP_MINIMUM_FREE_MEMORY = 64 * 1024 * 1024;

	// A ratio of how much free memory there is before we prioritise cleanup. For a 1 GB heap a ratio of 1/8 means that
	// we prioritise cleanup if there is less than 128 MB of free memory.
	private static final double CLEANUP_MINIMUM_FREE_MEMORY_RATIO = 1.0 / 8;

	public static final EmptyIteration<MemStatement, SailException> EMPTY_ITERATION = new EmptyIteration<>();
	public static final EmptyIteration<MemTriple, SailException> EMPTY_TRIPLE_ITERATION = new EmptyIteration<>();
	public static final MemResource[] EMPTY_CONTEXT = {};
	public static final MemResource[] NULL_CONTEXT = { null };

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
	 * <p>
	 * The purpose of this variable is to optimize read operations that only read inferred statements when there are no
	 * inferred statements.
	 */
	private volatile boolean mayHaveInferred = false;

	/**
	 * Identifies the current snapshot.
	 */
	private volatile int currentSnapshot;

	final SnapshotMonitor snapshotMonitor;

	/**
	 * Store for namespace prefix info.
	 */
	private final MemNamespaceStore namespaceStore = new MemNamespaceStore();

	/**
	 * Lock manager used to give the snapshot cleanup thread exclusive access to the statement list.
	 */

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
		snapshotMonitor = new SnapshotMonitor(debug);
	}

	@Override
	public ValueFactory getValueFactory() {
		return valueFactory;
	}

	@Override
	public void close() {
		synchronized (snapshotCleanupThreadLockObject) {
			if (snapshotCleanupThread != null) {
				snapshotCleanupThread.interrupt();
				snapshotCleanupThread = null;
			}
		}
		valueFactory.clear();
		statements.clear();
		namespaceStore.clear();
		invalidateCache();
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

	/**
	 * Creates a StatementIterator that contains the statements matching the specified pattern of subject, predicate,
	 * object, context. Inferred statements are excluded when <var>explicitOnly</var> is set to <var>true</var> .
	 * Statements from the null context are excluded when <var>namedContextsOnly</var> is set to <var>true</var>. The
	 * returned StatementIterator will assume the specified read mode.
	 */
	private CloseableIteration<MemStatement, SailException> createStatementIterator(Resource subj, IRI pred, Value obj,
			Boolean explicit, int snapshot, Resource... contexts) throws InterruptedException {
		// Perform look-ups for value-equivalents of the specified values

		if (explicit != null && !explicit && !mayHaveInferred && snapshot >= 0) {
			return EMPTY_ITERATION;
		}

		if (statements.isEmpty()) {
			return EMPTY_ITERATION;
		}

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
		} else if (contexts.length == 1 && contexts[0] == null) {
			memContexts = NULL_CONTEXT;
			smallestList = statements;
		} else if (contexts.length == 1) {
			MemResource memContext = valueFactory.getMemResource(contexts[0]);
			if (memContext == null) {
				// non-existent context
				return EMPTY_ITERATION;
			}

			memContexts = new MemResource[] { memContext };
			smallestList = memContext.getContextStatementList();
			if (smallestList.isEmpty()) {
				return EMPTY_ITERATION;
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

			memContexts = contextSet.toArray(new MemResource[contextSet.size()]);
			smallestList = statements;
		}

		return getMemStatementIterator(memSubj, memPred, memObj, explicit, snapshot, memContexts, smallestList);
	}

	private CloseableIteration<MemStatement, SailException> createStatementIterator(MemResource subj, MemIRI pred,
			MemValue obj, Boolean explicit, int snapshot, MemResource... contexts) throws InterruptedException {

		if (statements.isEmpty()) {
			return EMPTY_ITERATION;
		}

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

		return getMemStatementIterator(subj, pred, obj, explicit, snapshot, memContexts, smallestList);
	}

	private CloseableIteration<MemStatement, SailException> getMemStatementIterator(MemResource subj, MemIRI pred,
			MemValue obj, Boolean explicit, int snapshot, MemResource[] memContexts, MemStatementList statementList)
			throws InterruptedException {

		if (explicit != null && !explicit) {
			// we are looking for inferred statements
			if (!mayHaveInferred && snapshot >= 0) {
				return EMPTY_ITERATION;
			}
		}

		MemStatementList smallestList = getSmallestStatementList(subj, pred, obj);

		if (smallestList == null) {
			smallestList = statementList;
		} else if (smallestList.isEmpty()) {
			return EMPTY_ITERATION;
		}

		return MemStatementIterator.cacheAwareInstance(smallestList, subj, pred, obj, explicit, snapshot, memContexts,
				iteratorCache);
	}

	private MemStatementList getSmallestStatementList(MemResource subj, MemIRI pred, MemValue obj) {
		MemStatementList smallestList = null;

		if (subj != null) {
			smallestList = subj.getSubjectStatementList();
			if (smallestList.isEmpty()) {
				return smallestList;
			}
		}

		if (pred != null) {
			MemStatementList l = pred.getPredicateStatementList();
			if (smallestList == null) {
				smallestList = l;
				if (smallestList.isEmpty()) {
					return smallestList;
				}
			} else if (l.size() < smallestList.size()) {
				smallestList = l;
				if (smallestList.isEmpty()) {
					return smallestList;
				}
			}
		}

		if (obj != null) {
			MemStatementList l = obj.getObjectStatementList();
			if (smallestList == null) {
				smallestList = l;
			} else if (l.size() < smallestList.size()) {
				smallestList = l;
			}
		}
		return smallestList;
	}

	/**
	 * Creates a TripleIterator that contains the triples matching the specified pattern of subject, predicate, object,
	 * context.
	 */
	private CloseableIteration<MemTriple, SailException> createTripleIterator(Resource subj, IRI pred, Value obj,
			int snapshot) throws InterruptedException {
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
		int currentSnapshot = this.currentSnapshot;
		int highestUnusedTillSnapshot = snapshotMonitor.getFirstUnusedOrElse(currentSnapshot - 1);
		if (highestUnusedTillSnapshot >= currentSnapshot) {
			logger.debug("No old snapshot versions are currently unused, {} >= {} (currentSnapshot).",
					highestUnusedTillSnapshot, currentSnapshot);
		}

		try {

			boolean prioritiseCleaning = false;

			StopWatch stopWatch = null;
			if (logger.isDebugEnabled()) {
				stopWatch = StopWatch.createStarted();
				logger.debug("Started cleaning snapshots.");
			}

			prioritiseCleaning = prioritiseSnapshotCleaningIfLowOnMemory(prioritiseCleaning);

			// Sets used to keep track of which lists have already been processed
			HashSet<MemValue> processedSubjects = new HashSet<>();
			HashSet<MemValue> processedPredicates = new HashSet<>();
			HashSet<MemValue> processedObjects = new HashSet<>();
			HashSet<MemValue> processedContexts = new HashSet<>();

			MemStatement[] statements = this.statements.getStatements();

			/*
			 * The order of the statement list won't change from lastStmtPos down while we don't have the write lock (it
			 * might shrink or grow) as (1) new statements are always appended last, (2) we are the only process that
			 * removes statements, (3) this list is cleared on close.
			 */

			for (int i = statements.length - 1; i >= 0; i--) {
				if (Thread.currentThread().isInterrupted()) {
					break;
				}

				MemStatement st = statements[i];
				if (st == null) {
					continue;
				}

				if (st.getTillSnapshot() <= highestUnusedTillSnapshot) {
					MemResource subj = st.getSubject();
					if (processedSubjects.add(subj)) {
						subj.cleanSnapshotsFromSubjectStatements(highestUnusedTillSnapshot);
					}

					MemIRI pred = st.getPredicate();
					if (processedPredicates.add(pred)) {
						pred.cleanSnapshotsFromPredicateStatements(highestUnusedTillSnapshot);
					}

					MemValue obj = st.getObject();
					if (processedObjects.add(obj)) {
						obj.cleanSnapshotsFromObjectStatements(highestUnusedTillSnapshot);
					}

					MemResource context = st.getContext();
					if (context != null && processedContexts.add(context)) {
						context.cleanSnapshotsFromContextStatements(highestUnusedTillSnapshot);
					}

					// stale statement
					this.statements.remove(st, i);
					prioritiseCleaning = prioritiseSnapshotCleaningIfLowOnMemory(prioritiseCleaning);
				}

				if (i % 100_000 == 0) {
					if (getFreeToAllocateMemory() < CLEANUP_MINIMUM_FREE_MEMORY / 2) {
						prioritiseCleaning = prioritiseSnapshotCleaningIfLowOnMemory(prioritiseCleaning);
						processedSubjects = new HashSet<>();
						processedPredicates = new HashSet<>();
						processedObjects = new HashSet<>();
						processedContexts = new HashSet<>();
						System.gc();
					}
				}
			}

			processedSubjects.clear();
			processedPredicates.clear();
			processedObjects.clear();
			processedContexts.clear();

			if (logger.isDebugEnabled() && stopWatch != null) {
				stopWatch.stop();
				logger.debug("Cleaning snapshots took {} seconds.", stopWatch.getTime(TimeUnit.SECONDS));
			}

		} finally {
			statements.setPrioritiseCleanup(false);
		}
	}

	private boolean prioritiseSnapshotCleaningIfLowOnMemory(boolean prioritiseCleaning) {
		if (!prioritiseCleaning && MAX_MEMORY >= CLEANUP_MAX_MEMORY_THRESHOLD) {
			long freeToAllocateMemory = getFreeToAllocateMemory();

			if (memoryIsLow(freeToAllocateMemory)) {
				logger.debug(
						"Low free memory ({} MB)! Prioritising cleaning of removed statements from the MemoryStore.",
						freeToAllocateMemory / 1024 / 1024);
				prioritiseCleaning = true;
				this.statements.setPrioritiseCleanup(true);
			}
		}
		return prioritiseCleaning;
	}

	private static boolean memoryIsLow(long freeToAllocateMemory) {
		return freeToAllocateMemory < CLEANUP_MINIMUM_FREE_MEMORY
				|| (freeToAllocateMemory + 0.0) / MAX_MEMORY < CLEANUP_MINIMUM_FREE_MEMORY_RATIO;
	}

	private long getFreeToAllocateMemory() {
		// total currently allocated JVM memory
		long totalMemory = RUNTIME.totalMemory();

		// amount of memory free in the currently allocated JVM memory
		long freeMemory = RUNTIME.freeMemory();

		// estimated memory used
		long used = totalMemory - freeMemory;

		// amount of memory the JVM can still allocate from the OS (upper boundary is the max heap)
		return MAX_MEMORY - used;
	}

	protected void scheduleSnapshotCleanup() {
		// we don't schedule snapshot cleanup on small memory stores
		if (statements.size() < 1000) {
			return;
		}

		Thread toCheckSnapshotCleanupThread = snapshotCleanupThread;
		if (toCheckSnapshotCleanupThread == null || !toCheckSnapshotCleanupThread.isAlive()) {
			synchronized (snapshotCleanupThreadLockObject) {
				toCheckSnapshotCleanupThread = snapshotCleanupThread;
				if (toCheckSnapshotCleanupThread == null || !toCheckSnapshotCleanupThread.isAlive()) {
					Runnable runnable = () -> {
						try {
//							 sleep for up to 5 seconds unless we are low on memory
							for (int i = 0; i < 100 * 5 && !memoryIsLow(getFreeToAllocateMemory() * 2); i++) {
								Thread.sleep(10);
							}

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
					Thread.yield();
				}
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

		private volatile boolean closed = false;

		private final boolean explicit;

		private final int serializable;
		private final SnapshotMonitor.ReservedSnapshot reservedSnapshot;

		private int nextSnapshot;

		private Set<StatementPattern> observations;

		private volatile boolean txnLock;

		private boolean requireCleanup;

		public MemorySailSink(boolean explicit, boolean serializable) throws SailException {
			this.explicit = explicit;
			if (serializable) {
				this.serializable = currentSnapshot;
				reservedSnapshot = snapshotMonitor.reserve(this.serializable, this);
			} else {
				this.serializable = Integer.MAX_VALUE;
				reservedSnapshot = null;
			}
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
					} catch (InterruptedException e) {
						throw convertToSailException(e);
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
			if (!closed) {
				closed = true;
				try {
					if (reservedSnapshot != null) {
						reservedSnapshot.release();
					}
				} finally {
					boolean toCloseTxnLock = txnLock;
					txnLock = false;
					if (toCloseTxnLock) {
						txnLockManager.unlock();
					}
					observations = null;
				}

			}
		}

		@Override
		public synchronized void setNamespace(String prefix, String name) {
			acquireExclusiveTransactionLock();
			namespaceStore.setNamespace(prefix, name);
		}

		@Override
		public synchronized void removeNamespace(String prefix) {
			acquireExclusiveTransactionLock();
			namespaceStore.removeNamespace(prefix);
		}

		@Override
		public synchronized void clearNamespaces() {
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
		public synchronized void clear(Resource... contexts) {
			acquireExclusiveTransactionLock();
			invalidateCache();
			requireCleanup = true;
			try (CloseableIteration<MemStatement, SailException> iter = createStatementIterator(null, null, null,
					explicit, nextSnapshot, contexts)) {
				while (iter.hasNext()) {
					MemStatement st = iter.next();
					st.setTillSnapshot(nextSnapshot);
				}
			} catch (InterruptedException e) {
				throw convertToSailException(e);
			}
		}

		@Override
		public synchronized void approve(Resource subj, IRI pred, Value obj, Resource ctx) {
			acquireExclusiveTransactionLock();
			invalidateCache();
			try {
				addStatement(subj, pred, obj, ctx, explicit);
			} catch (InterruptedException e) {
				throw convertToSailException(e);
			}
		}

		@Override
		public synchronized void approveAll(Set<Statement> approved, Set<Resource> approvedContexts) {
			acquireExclusiveTransactionLock();
			invalidateCache();
			try {
				for (Statement statement : approved) {
					addStatement(statement.getSubject(), statement.getPredicate(), statement.getObject(),
							statement.getContext(), explicit);
				}
			} catch (InterruptedException e) {
				throw convertToSailException(e);
			}
		}

		@Override
		public synchronized void deprecateAll(Set<Statement> deprecated) {
			acquireExclusiveTransactionLock();
			invalidateCache();
			requireCleanup = true;
			int nextSnapshot = this.nextSnapshot;
			for (Statement statement : deprecated) {
				innerDeprecate(statement, nextSnapshot);
			}
		}

		@Override
		public synchronized void deprecate(Statement statement) throws SailException {
			acquireExclusiveTransactionLock();
			invalidateCache();
			requireCleanup = true;
			innerDeprecate(statement, nextSnapshot);
		}

		private void innerDeprecate(Statement statement, int nextSnapshot) {
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
						statement.getSubject(), statement.getPredicate(), statement.getObject(), explicit, nextSnapshot,
						statement.getContext())) {
					while (iter.hasNext()) {
						MemStatement st = iter.next();
						st.setTillSnapshot(nextSnapshot);
					}
				} catch (InterruptedException e) {
					throw convertToSailException(e);
				}
			}
		}

		private void acquireExclusiveTransactionLock() throws SailException {
			if (!txnLock) {
				synchronized (this) {
					if (!txnLock) {
						txnLockManager.lock();
						nextSnapshot = currentSnapshot + 1;
						txnLock = true;
					}
				}

			}
		}

		private MemStatement addStatement(Resource subj, IRI pred, Value obj, Resource context, boolean explicit)
				throws SailException, InterruptedException {
			if (!explicit) {
				mayHaveInferred = true;
			}

			// Get or create MemValues for the operands
			MemResource memSubj = valueFactory.getOrCreateMemResource(subj);
			MemIRI memPred = valueFactory.getOrCreateMemURI(pred);
			MemValue memObj = valueFactory.getOrCreateMemValue(obj);
			MemResource memContext = context == null ? null : valueFactory.getOrCreateMemResource(context);

			if (memSubj.hasSubjectStatements() && memPred.hasPredicateStatements() && memObj.hasObjectStatements()
					&& (memContext == null || memContext.hasContextStatements())) {
				// All values are used in at least one statement. Possibly, the
				// statement is already present. Check this.

				if (statementAlreadyExists(explicit, memSubj, memPred, memObj, memContext, nextSnapshot)) {
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
				MemResource memContext, int nextSnapshot) throws InterruptedException {

			MemStatementList statementList = getSmallestMemStatementList(memSubj, memPred, memObj, memContext);

			MemStatement memStatement = statementList.getExact(memSubj, memPred, memObj, memContext,
					nextSnapshot);
			if (memStatement != null) {
				if (!memStatement.isExplicit() && explicit) {
					// Implicit statement is now added explicitly
					memStatement.setTillSnapshot(this.nextSnapshot);
				} else {
					// statement already exists
					return true;
				}
			}

			return false;
		}

		private MemStatementList getSmallestMemStatementList(MemResource memSubj, MemIRI memPred, MemValue memObj,
				MemResource memContext) {
			MemStatementList statementList = memSubj.getSubjectStatementList();
			if (statementList.size() <= 1) {
				return statementList;
			}

			if (memPred.getPredicateStatementCount() < statementList.size()) {
				statementList = memPred.getPredicateStatementList();
				if (statementList.size() <= 1) {
					return statementList;
				}
			}

			if (memObj.getObjectStatementCount() < statementList.size()) {
				statementList = memObj.getObjectStatementList();
				if (statementList.size() <= 1) {
					return statementList;
				}
			}

			if (memContext != null && memContext.getContextStatementCount() < statementList.size()) {
				statementList = memContext.getContextStatementList();
			}

			return statementList;
		}

		@Override
		public boolean deprecateByQuery(Resource subj, IRI pred, Value obj, Resource[] contexts) {
			acquireExclusiveTransactionLock();
			boolean deprecated = false;
			requireCleanup = true;
			invalidateCache();

			try (CloseableIteration<MemStatement, SailException> iter = createStatementIterator(subj, pred, obj,
					explicit, nextSnapshot, contexts)) {
				while (iter.hasNext()) {
					deprecated = true;
					MemStatement st = iter.next();
					st.setTillSnapshot(nextSnapshot);
				}
			} catch (InterruptedException e) {
				throw convertToSailException(e);
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
		private final SnapshotMonitor.ReservedSnapshot reservedSnapshot;
		private volatile boolean closed;

		public MemorySailDataset(boolean explicit) throws SailException {
			this.explicit = explicit;
			this.snapshot = -1;
			this.reservedSnapshot = null;
		}

		public MemorySailDataset(boolean explicit, int snapshot) throws SailException {
			this.explicit = explicit;
			this.snapshot = snapshot;
			this.reservedSnapshot = snapshotMonitor.reserve(snapshot, this);
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
			if (closed) {
				return;
			}
			closed = true;
			if (reservedSnapshot != null) {
				reservedSnapshot.release();
			}
		}

		@Override
		public String getNamespace(String prefix) throws SailException {
			return namespaceStore.getNamespace(prefix);
		}

		@Override
		public CloseableIteration<? extends Namespace, SailException> getNamespaces() {
			return new CloseableIteratorIteration<>(namespaceStore.iterator());
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

			int snapshot = getCurrentSnapshot();

			try (WeakObjectRegistry.AutoCloseableIterator<MemIRI> memIRIsIterator = valueFactory
					.getMemIRIsIterator()) {
				while (memIRIsIterator.hasNext()) {
					MemResource memResource = memIRIsIterator.next();
					if (isContextResource(memResource, snapshot)) {
						contextIDs.add(memResource);
					}
				}
			} catch (InterruptedException e) {
				throw convertToSailException(e);
			}

			try (WeakObjectRegistry.AutoCloseableIterator<MemBNode> memBNodesIterator = valueFactory
					.getMemBNodesIterator()) {
				while (memBNodesIterator.hasNext()) {
					MemResource memResource = memBNodesIterator.next();
					if (isContextResource(memResource, snapshot)) {
						contextIDs.add(memResource);
					}
				}
			} catch (InterruptedException e) {
				throw convertToSailException(e);
			}

			return new CloseableIteratorIteration<>(contextIDs.iterator());
		}

		@Override
		public CloseableIteration<MemStatement, SailException> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) throws SailException {
			try {
				return createStatementIterator(subj, pred, obj, explicit, getCurrentSnapshot(), contexts);
			} catch (InterruptedException e) {
				throw convertToSailException(e);
			}
		}

		@Override
		public CloseableIteration<MemTriple, SailException> getTriples(Resource subj, IRI pred, Value obj)
				throws SailException {
			try {
				return createTripleIterator(subj, pred, obj, getCurrentSnapshot());
			} catch (InterruptedException e) {
				throw convertToSailException(e);
			}
		}

		private int getCurrentSnapshot() {
			if (snapshot >= 0) {
				return snapshot;
			} else {
				return currentSnapshot;
			}
		}

		private boolean isContextResource(MemResource memResource, int snapshot)
				throws SailException, InterruptedException {
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

	/**
	 * SnapshotMonitor is used to keep track of which snapshot version are no longer is use (read or write) so that we
	 * can safely clean that snapshot version.
	 */
	static class SnapshotMonitor {
		private static final ConcurrentCleaner cleaner = new ConcurrentCleaner();

		private final ConcurrentHashMap<Integer, LongAdder> activeSnapshots = new ConcurrentHashMap<>();
		private final boolean debug;

		// The LongAdder used to track the number of reservations (uses) for a snapshot version is kept in the
		// activeSnapshots map. When all reservations are released and the LongAdder.sum() == 0 we should be able to
		// safely remove it, this can however cause race conditions if the LongAdder can still be incremented (e.g. when
		// the snapshot version is the current snapshot). By assuming that there will never be any new reservations for
		// an "old" snapshot version, we can then safely remove the LongAdder if the snapshot version that it is
		// tracking is lower than the highestEverReservedSnapshot.
		private final AtomicInteger highestEverReservedSnapshot = new AtomicInteger(-1);

		public SnapshotMonitor(boolean debug) {
			this.debug = debug;
		}

		public int getFirstUnusedOrElse(int currentSnapshot) {

			int maximum = this.highestEverReservedSnapshot.getAcquire();

			int min = Integer.MAX_VALUE;
			for (Map.Entry<Integer, LongAdder> entry : activeSnapshots.entrySet()) {
				if (entry.getKey() <= min) {
					if (entry.getKey() < maximum && entry.getValue().sum() == 0) {
						activeSnapshots.computeIfPresent(entry.getKey(), (k, v) -> {
							if (v.sum() == 0) {
								return null;
							}
							return v;
						});
					} else {
						min = entry.getKey() - 1;
					}

				}
			}

			if (min == Integer.MAX_VALUE) {
				return currentSnapshot - 1;
			} else {
				return min;
			}
		}

		public ReservedSnapshot reserve(int snapshot, Object reservedBy) {
			int highestEverReservedSnapshot = this.highestEverReservedSnapshot.getAcquire();
			while (snapshot > highestEverReservedSnapshot) {
				if (this.highestEverReservedSnapshot.compareAndSet(highestEverReservedSnapshot, snapshot)) {
					highestEverReservedSnapshot = snapshot;
				} else {
					highestEverReservedSnapshot = this.highestEverReservedSnapshot.getAcquire();
				}
			}

			LongAdder longAdder = activeSnapshots.computeIfAbsent(snapshot, (k) -> new LongAdder());
			longAdder.increment();

			return new ReservedSnapshot(snapshot, reservedBy, debug, longAdder, activeSnapshots,
					this.highestEverReservedSnapshot);
		}

		static class ReservedSnapshot {

			private static final int SNAPSHOT_RELEASED = -1;
			private final ConcurrentHashMap<Integer, LongAdder> activeSnapshots;
			private final LongAdder frequency;
			private final AtomicInteger highestEverReservedSnapshot;

			private Cleaner.Cleanable cleanable;
			private final Throwable stackTraceForDebugging;

			@SuppressWarnings("FieldMayBeFinal")
			private volatile int snapshot;
			private final static VarHandle SNAPSHOT;

			static {
				try {
					SNAPSHOT = MethodHandles.lookup()
							.in(ReservedSnapshot.class)
							.findVarHandle(ReservedSnapshot.class, "snapshot", int.class);
				} catch (ReflectiveOperationException e) {
					throw new Error(e);
				}
			}

			public ReservedSnapshot(int snapshot, Object reservedBy, boolean debug,
					LongAdder frequency, ConcurrentHashMap<Integer, LongAdder> activeSnapshots,
					AtomicInteger highestEverReservedSnapshot) {
				this.snapshot = snapshot;
				if (debug) {
					stackTraceForDebugging = new Throwable("Unreleased snapshot version");
				} else {
					stackTraceForDebugging = null;
				}
				this.activeSnapshots = activeSnapshots;
				this.frequency = frequency;
				this.highestEverReservedSnapshot = highestEverReservedSnapshot;
				cleanable = cleaner.register(reservedBy, () -> {
					int tempSnapshot = ((int) SNAPSHOT.getVolatile(this));
					if (tempSnapshot != SNAPSHOT_RELEASED) {
						String message = "Releasing MemorySailStore snapshot {} which was reserved and never released (possibly unclosed MemorySailDataset or MemorySailSink).";
						if (stackTraceForDebugging != null) {
							logger.warn(message, tempSnapshot, stackTraceForDebugging);
						} else {
							logger.warn(message, tempSnapshot);
						}
						release();
					}
				});
			}

			public void release() {
				int snapshot = (int) SNAPSHOT.getAcquire(this);
				if (snapshot != SNAPSHOT_RELEASED) {
					if (SNAPSHOT.compareAndSet(this, snapshot, SNAPSHOT_RELEASED)) {
						frequency.decrement();
						assert frequency.sum() >= 0;
						if (snapshot < highestEverReservedSnapshot.getAcquire() && frequency.sum() == 0) {
							activeSnapshots.computeIfPresent(snapshot, (k, v) -> {
								if (v.sum() == 0) {
									return null;
								}
								return v;
							});
						}
					}
				}

				Cleaner.Cleanable cleanable = this.cleanable;
				if (cleanable != null) {
					this.cleanable = null;
					cleanable.clean();
				}
			}

		}
	}
}
