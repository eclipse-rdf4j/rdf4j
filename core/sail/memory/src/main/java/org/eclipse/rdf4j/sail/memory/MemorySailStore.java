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
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.LockingIteration;
import org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManager;
import org.eclipse.rdf4j.common.concurrent.locks.ReadWriteLockManager;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
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
import org.eclipse.rdf4j.sail.memory.model.MemIRI;
import org.eclipse.rdf4j.sail.memory.model.MemResource;
import org.eclipse.rdf4j.sail.memory.model.MemStatement;
import org.eclipse.rdf4j.sail.memory.model.MemStatementIterator;
import org.eclipse.rdf4j.sail.memory.model.MemStatementList;
import org.eclipse.rdf4j.sail.memory.model.MemValue;
import org.eclipse.rdf4j.sail.memory.model.MemValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link SailStore} that keeps committed statements in a
 * {@link MemStatementList}.
 * 
 * @author James Leigh
 */
class MemorySailStore implements SailStore {

	final Logger logger = LoggerFactory.getLogger(MemorySailStore.class);

	/**
	 * Factory/cache for MemValue objects.
	 */
	private final MemValueFactory valueFactory = new MemValueFactory();

	/**
	 * List containing all available statements.
	 */
	private final MemStatementList statements = new MemStatementList(256);

	/**
	 * Identifies the current snapshot.
	 */
	volatile int currentSnapshot;

	/**
	 * Store for namespace prefix info.
	 */
	private final MemNamespaceStore namespaceStore = new MemNamespaceStore();

	/**
	 * Lock manager used to give the snapshot cleanup thread exclusive access to
	 * the statement list.
	 */
	private final ReadWriteLockManager statementListLockManager;

	/**
	 * Lock manager used to prevent concurrent writes.
	 */
	final ReentrantLock txnLockManager = new ReentrantLock();

	/**
	 * Cleanup thread that removes deprecated statements when no other threads
	 * are accessing this list. Seee {@link #scheduleSnapshotCleanup()}.
	 */
	private volatile Thread snapshotCleanupThread;

	/**
	 * Semaphore used to synchronize concurrent access to
	 * {@link #snapshotCleanupThread}.
	 */
	private final Object snapshotCleanupThreadSemaphore = new Object();

	public MemorySailStore(boolean debug) {
		statementListLockManager = new ReadPrefReadWriteLockManager(debug);
	}

	@Override
	public ValueFactory getValueFactory() {
		return valueFactory;
	}

	@Override
	public void close() {
		try {
			Lock stLock = statementListLockManager.getWriteLock();
			try {
				valueFactory.clear();
				statements.clear();
			}
			finally {
				stLock.release();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return new MemEvaluationStatistics(valueFactory);
	}

	@Override
	public SailSource getExplicitSailSource() {
		return new MemorySailSource(true);
	}

	@Override
	public SailSource getInferredSailSource() {
		return new MemorySailSource(false);
	}

	Lock openStatementsReadLock()
		throws SailException
	{
		try {
			return statementListLockManager.getReadLock();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SailException(e);
		}
	}

	/**
	 * Creates a StatementIterator that contains the statements matching the
	 * specified pattern of subject, predicate, object, context. Inferred
	 * statements are excluded when <tt>explicitOnly</tt> is set to <tt>true</tt>
	 * . Statements from the null context are excluded when
	 * <tt>namedContextsOnly</tt> is set to <tt>true</tt>. The returned
	 * StatementIterator will assume the specified read mode.
	 */
	CloseableIteration<MemStatement, SailException> createStatementIterator(Resource subj, IRI pred,
			Value obj, Boolean explicit, int snapshot, Resource... contexts)
	{
		// Perform look-ups for value-equivalents of the specified values
		MemResource memSubj = valueFactory.getMemResource(subj);
		if (subj != null && memSubj == null) {
			// non-existent subject
			return new EmptyIteration<MemStatement, SailException>();
		}

		MemIRI memPred = valueFactory.getMemURI(pred);
		if (pred != null && memPred == null) {
			// non-existent predicate
			return new EmptyIteration<MemStatement, SailException>();
		}

		MemValue memObj = valueFactory.getMemValue(obj);
		if (obj != null && memObj == null) {
			// non-existent object
			return new EmptyIteration<MemStatement, SailException>();
		}

		MemResource[] memContexts;
		MemStatementList smallestList;

		if (contexts.length == 0) {
			memContexts = new MemResource[0];
			smallestList = statements;
		}
		else if (contexts.length == 1 && contexts[0] != null) {
			MemResource memContext = valueFactory.getMemResource(contexts[0]);
			if (memContext == null) {
				// non-existent context
				return new EmptyIteration<MemStatement, SailException>();
			}

			memContexts = new MemResource[] { memContext };
			smallestList = memContext.getContextStatementList();
		}
		else {
			Set<MemResource> contextSet = new LinkedHashSet<MemResource>(2 * contexts.length);

			for (Resource context : contexts) {
				MemResource memContext = valueFactory.getMemResource(context);
				if (context == null || memContext != null) {
					contextSet.add(memContext);
				}
			}

			if (contextSet.isEmpty()) {
				// no known contexts specified
				return new EmptyIteration<MemStatement, SailException>();
			}

			memContexts = contextSet.toArray(new MemResource[contextSet.size()]);
			smallestList = statements;
		}

		if (memSubj != null) {
			MemStatementList l = memSubj.getSubjectStatementList();
			if (l.size() < smallestList.size()) {
				smallestList = l;
			}
		}

		if (memPred != null) {
			MemStatementList l = memPred.getPredicateStatementList();
			if (l.size() < smallestList.size()) {
				smallestList = l;
			}
		}

		if (memObj != null) {
			MemStatementList l = memObj.getObjectStatementList();
			if (l.size() < smallestList.size()) {
				smallestList = l;
			}
		}

		return new MemStatementIterator<SailException>(smallestList, memSubj, memPred, memObj, explicit,
				snapshot, memContexts);
	}

	/**
	 * Removes statements from old snapshots from the main statement list and
	 * resets the snapshot to 1 for the rest of the statements.
	 * 
	 * @throws InterruptedException
	 */
	protected void cleanSnapshots()
		throws InterruptedException
	{
		// System.out.println("cleanSnapshots() starting...");
		// long startTime = System.currentTimeMillis();

		// Sets used to keep track of which lists have already been processed
		HashSet<MemValue> processedSubjects = new HashSet<MemValue>();
		HashSet<MemValue> processedPredicates = new HashSet<MemValue>();
		HashSet<MemValue> processedObjects = new HashSet<MemValue>();
		HashSet<MemValue> processedContexts = new HashSet<MemValue>();

		Lock stLock = statementListLockManager.getWriteLock();
		try {
			for (int i = statements.size() - 1; i >= 0; i--) {
				MemStatement st = statements.get(i);

				if (st.getTillSnapshot() <= currentSnapshot) {
					MemResource subj = st.getSubject();
					if (processedSubjects.add(subj)) {
						subj.cleanSnapshotsFromSubjectStatements(currentSnapshot);
					}

					MemIRI pred = st.getPredicate();
					if (processedPredicates.add(pred)) {
						pred.cleanSnapshotsFromPredicateStatements(currentSnapshot);
					}

					MemValue obj = st.getObject();
					if (processedObjects.add(obj)) {
						obj.cleanSnapshotsFromObjectStatements(currentSnapshot);
					}

					MemResource context = st.getContext();
					if (context != null && processedContexts.add(context)) {
						context.cleanSnapshotsFromContextStatements(currentSnapshot);
					}

					// stale statement
					statements.remove(i);
				}
			}
		}
		finally {
			stLock.release();
		}

		// long endTime = System.currentTimeMillis();
		// System.out.println("cleanSnapshots() took " + (endTime - startTime) +
		// " ms");
	}

	protected void scheduleSnapshotCleanup() {
		synchronized (snapshotCleanupThreadSemaphore) {
			if (snapshotCleanupThread == null || !snapshotCleanupThread.isAlive()) {
				Runnable runnable = new Runnable() {

					public void run() {
						try {
							cleanSnapshots();
						}
						catch (InterruptedException e) {
							logger.warn("snapshot cleanup interrupted");
						}
					}
				};

				snapshotCleanupThread = new Thread(runnable, "MemoryStore snapshot cleanup");
				snapshotCleanupThread.setDaemon(true);
				snapshotCleanupThread.start();
			}
		}
	}

	private final class MemorySailSource extends BackingSailSource {

		private final boolean explicit;

		public MemorySailSource(boolean explicit) {
			this.explicit = explicit;
		}

		@Override
		public SailSink sink(IsolationLevel level)
			throws SailException
		{
			return new MemorySailSink(explicit, level.isCompatibleWith(IsolationLevels.SERIALIZABLE));
		}

		@Override
		public MemorySailDataset dataset(IsolationLevel level)
			throws SailException
		{
			if (level.isCompatibleWith(IsolationLevels.SNAPSHOT_READ)) {
				return new MemorySailDataset(explicit, currentSnapshot);
			}
			else {
				return new MemorySailDataset(explicit);
			}
		}
	}

	private final class MemorySailSink implements SailSink {

		private boolean explicit;

		private final int serializable;

		private final Lock txnStLock;

		private int nextSnapshot;

		private Set<StatementPattern> observations;

		private boolean txnLock;

		public MemorySailSink(boolean explicit, boolean serializable)
			throws SailException
		{
			this.explicit = explicit;
			if (serializable) {
				this.serializable = currentSnapshot;
			}
			else {
				this.serializable = Integer.MAX_VALUE;
			}
			txnStLock = openStatementsReadLock();
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (explicit) {
				sb.append("explicit ");
			}
			else {
				sb.append("inferred ");
			}
			if (txnLock) {
				sb.append("snapshot ").append(nextSnapshot);
			}
			else {
				sb.append(super.toString());
			}
			return sb.toString();
		}

		@Override
		public void prepare()
			throws SailException
		{
			acquireExclusiveTransactionLock();
			if (observations != null) {
				for (StatementPattern p : observations) {
					Resource subj = (Resource)p.getSubjectVar().getValue();
					IRI pred = (IRI)p.getPredicateVar().getValue();
					Value obj = p.getObjectVar().getValue();
					Var ctxVar = p.getContextVar();
					Resource[] contexts;
					if (ctxVar == null) {
						contexts = new Resource[0];
					}
					else {
						contexts = new Resource[] { (Resource)ctxVar.getValue() };
					}
					CloseableIteration<MemStatement, SailException> iter;
					iter = createStatementIterator(subj, pred, obj, null, -1, contexts);
					try {
						while (iter.hasNext()) {
							MemStatement st = iter.next();
							int since = st.getSinceSnapshot();
							int till = st.getTillSnapshot();
							if (serializable < since && since < nextSnapshot || serializable < till
									&& till < nextSnapshot)
							{
								throw new SailConflictException("Observed State has Changed");
							}
						}
					}
					finally {
						iter.close();
					}
				}
			}
		}

		@Override
		public void flush()
			throws SailException
		{
			if (txnLock) {
				currentSnapshot = Math.max(currentSnapshot, nextSnapshot);
				scheduleSnapshotCleanup();
			}
		}

		@Override
		public void close() {
			if (txnLock) {
				txnLockManager.unlock();
				txnLock = false;
			}
			if (txnStLock != null) {
				txnStLock.release();
			}
		}

		@Override
		public synchronized void setNamespace(String prefix, String name)
			throws SailException
		{
			acquireExclusiveTransactionLock();
			namespaceStore.setNamespace(prefix, name);
		}

		@Override
		public void removeNamespace(String prefix)
			throws SailException
		{
			acquireExclusiveTransactionLock();
			namespaceStore.removeNamespace(prefix);
		}

		@Override
		public void clearNamespaces()
			throws SailException
		{
			acquireExclusiveTransactionLock();
			namespaceStore.clear();
		}

		@Override
		public void observe(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException
		{
			if (observations == null) {
				observations = new HashSet<StatementPattern>();
			}
			if (contexts == null) {
				observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj),
						new Var("g", null)));
			}
			else if (contexts.length == 0) {
				observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj)));
			}
			else {
				for (Resource ctx : contexts) {
					observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred),
							new Var("o", obj), new Var("g", ctx)));
				}
			}
		}

		@Override
		public void clear(Resource... contexts)
			throws SailException
		{
			acquireExclusiveTransactionLock();
			CloseableIteration<MemStatement, SailException> iter;
			iter = createStatementIterator(null, null, null, explicit, nextSnapshot, contexts);
			try {
				while (iter.hasNext()) {
					MemStatement st = iter.next();
					st.setTillSnapshot(nextSnapshot);
				}
			}
			finally {
				iter.close();
			}
		}

		@Override
		public synchronized void approve(Resource subj, IRI pred, Value obj, Resource ctx)
			throws SailException
		{
			acquireExclusiveTransactionLock();
			addStatement(subj, pred, obj, ctx, explicit);
		}

		@Override
		public synchronized void deprecate(Resource subj, IRI pred, Value obj, Resource ctx)
			throws SailException
		{
			acquireExclusiveTransactionLock();
			CloseableIteration<MemStatement, SailException> iter;
			iter = createStatementIterator(subj, pred, obj, explicit, nextSnapshot, ctx);
			try {
				while (iter.hasNext()) {
					MemStatement st = iter.next();
					st.setTillSnapshot(nextSnapshot);
				}
			}
			finally {
				iter.close();
			}
		}

		private void acquireExclusiveTransactionLock()
			throws SailException
		{
			if (!txnLock) {
				txnLockManager.lock();
				nextSnapshot = currentSnapshot + 1;
				txnLock = true;
			}
		}

		private MemStatement addStatement(Resource subj, IRI pred, Value obj, Resource context, boolean explicit)
			throws SailException
		{
			// Get or create MemValues for the operands
			MemResource memSubj = valueFactory.getOrCreateMemResource(subj);
			MemIRI memPred = valueFactory.getOrCreateMemURI(pred);
			MemValue memObj = valueFactory.getOrCreateMemValue(obj);
			MemResource memContext = (context == null) ? null : valueFactory.getOrCreateMemResource(context);

			if (memSubj.hasStatements() && memPred.hasStatements() && memObj.hasStatements()
					&& (memContext == null || memContext.hasStatements()))
			{
				// All values are used in at least one statement. Possibly, the
				// statement is already present. Check this.
				CloseableIteration<MemStatement, SailException> stIter = createStatementIterator(memSubj,
						memPred, memObj, null, Integer.MAX_VALUE - 1, memContext);

				try {
					if (stIter.hasNext()) {
						// statement is already present, update its transaction
						// status if appropriate
						MemStatement st = stIter.next();

						if (!st.isExplicit() && explicit) {
							// Implicit statement is now added explicitly
							st.setTillSnapshot(nextSnapshot);
						}
						else if (!st.isInSnapshot(nextSnapshot)) {
							st.setSinceSnapshot(nextSnapshot);
						}
						else {
							// statement already exists
							return null;
						}
					}
				}
				finally {
					stIter.close();
				}
			}

			// completely new statement
			MemStatement st = new MemStatement(memSubj, memPred, memObj, memContext, explicit, nextSnapshot);
			statements.add(st);
			st.addToComponentLists();
			return st;
		}
	}

	/**
	 * @author James Leigh
	 */
	private final class MemorySailDataset implements SailDataset {

		private final boolean explicit;

		private final int snapshot;

		private final Lock lock;

		public MemorySailDataset(boolean explicit)
			throws SailException
		{
			this.explicit = explicit;
			this.snapshot = -1;
			this.lock = null;
		}

		public MemorySailDataset(boolean explicit, int snapshot)
			throws SailException
		{
			this.explicit = explicit;
			this.snapshot = snapshot;
			this.lock = openStatementsReadLock();
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (explicit) {
				sb.append("explicit ");
			}
			else {
				sb.append("inferred ");
			}
			if (snapshot >= 0) {
				sb.append("snapshot ").append(snapshot);
			}
			else {
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
		public String getNamespace(String prefix)
			throws SailException
		{
			return namespaceStore.getNamespace(prefix);
		}

		@Override
		public CloseableIteration<? extends Namespace, SailException> getNamespaces() {
			return new CloseableIteratorIteration<Namespace, SailException>(namespaceStore.iterator());
		}

		@Override
		public CloseableIteration<? extends Resource, SailException> getContextIDs()
			throws SailException
		{
			// Note: we can't do this in a streaming fashion due to concurrency
			// issues; iterating over the set of IRIs or bnodes while another
			// thread
			// adds statements with new resources would result in
			// ConcurrentModificationException's (issue SES-544).

			// Create a list of all resources that are used as contexts
			ArrayList<MemResource> contextIDs = new ArrayList<MemResource>(32);

			Lock stLock = openStatementsReadLock();

			try {
				synchronized (valueFactory) {
					int snapshot = getCurrentSnapshot();
					for (MemResource memResource : valueFactory.getMemURIs()) {
						if (isContextResource(memResource, snapshot)) {
							contextIDs.add(memResource);
						}
					}

					for (MemResource memResource : valueFactory.getMemBNodes()) {
						if (isContextResource(memResource, snapshot)) {
							contextIDs.add(memResource);
						}
					}
				}
			}
			finally {
				stLock.release();
			}

			return new CloseableIteratorIteration<MemResource, SailException>(contextIDs.iterator());
		}

		@Override
		public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts)
			throws SailException
		{
			boolean releaseLock = true;
			Lock stLock = openStatementsReadLock();
			try {
				CloseableIteration<? extends Statement, SailException> ret;
				ret = createStatementIterator(subj, pred, obj, explicit, getCurrentSnapshot(), contexts);
				ret = new LockingIteration<Statement, SailException>(stLock, ret);
				releaseLock = false;
				return ret;
			}
			finally {
				if (releaseLock) {
					stLock.release();
				}
			}
		}

		private int getCurrentSnapshot() {
			if (snapshot >= 0) {
				return snapshot;
			}
			else {
				return currentSnapshot;
			}
		}

		private boolean isContextResource(MemResource memResource, int snapshot)
			throws SailException
		{
			MemStatementList contextStatements = memResource.getContextStatementList();

			// Filter resources that are not used as context identifier
			if (contextStatements.size() == 0) {
				return false;
			}

			// Filter more thoroughly by considering snapshot and read-mode
			// parameters
			MemStatementIterator<SailException> iter = new MemStatementIterator<SailException>(
					contextStatements, null, null, null, null, snapshot);
			try {
				return iter.hasNext();
			}
			finally {
				iter.close();
			}
		}
	}
}
