/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.helpers;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.ConcurrentCleaner;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Class offering base functionality for SailConnection implementations.
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 */
public abstract class AbstractSailConnection implements SailConnection {

	private static final ConcurrentCleaner cleaner = new ConcurrentCleaner();

	/**
	 * Size of write queue before auto flushing changes within write operation
	 */
	private static final int BLOCK_SIZE = 1000;

	private static final Logger logger = LoggerFactory.getLogger(AbstractSailConnection.class);

	/*-----------*
	 * Variables *
	 *-----------*/

	// System.getProperty maybe too expensive to call every time
	private final boolean debugEnabled = AbstractSail.debugEnabled();

	private final AbstractSail sailBase;

	private volatile boolean txnActive;

	private volatile boolean txnPrepared;

	/**
	 * Lock used to give the {@link #close()} method exclusive access to a connection.
	 * <ul>
	 * <li>write lock: close()
	 * <li>read lock: all other (public) methods
	 * </ul>
	 */
	private final LongAdder blockClose = new LongAdder();
	private final LongAdder unblockClose = new LongAdder();

	@SuppressWarnings("FieldMayBeFinal")
	private boolean isOpen = true;
	private static final VarHandle IS_OPEN;

	static {
		try {
			IS_OPEN = MethodHandles.lookup()
					.in(AbstractSailConnection.class)
					.findVarHandle(AbstractSailConnection.class, "isOpen", boolean.class);
		} catch (ReflectiveOperationException e) {
			throw new Error(e);
		}
	}

	/**
	 * Lock used to prevent concurrent calls to update methods like addStatement, clear, commit, etc. within a
	 * transaction.
	 *
	 * @deprecated Will be made private.
	 */
	@Deprecated(since = "4.1.0")
	protected final ReentrantLock updateLock = new ReentrantLock();

	@Deprecated(since = "4.1.0", forRemoval = true)
	protected final ReentrantReadWriteLock connectionLock = new ReentrantReadWriteLock();

	@InternalUseOnly
	protected boolean useConnectionLock = true;

	private final LongAdder iterationsOpened = new LongAdder();
	private final LongAdder iterationsClosed = new LongAdder();

	private final Map<SailBaseIteration<?, ?>, Throwable> activeIterationsDebug;

	/**
	 * Statements that are currently being removed, but not yet realized, by an active operation.
	 */
	private final Map<UpdateContext, Collection<Statement>> removed = new IdentityHashMap<>(0);

	/**
	 * Statements that are currently being added, but not yet realized, by an active operation.
	 */
	private final Map<UpdateContext, Collection<Statement>> added = new IdentityHashMap<>(0);

	/**
	 * Used to indicate a removed statement from all contexts.
	 */
	private static final BNode wildContext = SimpleValueFactory.getInstance().createBNode();

	private IsolationLevel transactionIsolationLevel;

	// used to decide if we need to call flush()
	private volatile boolean statementsAdded;
	private volatile boolean statementsRemoved;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public AbstractSailConnection(AbstractSail sailBase) {
		this.sailBase = sailBase;
		txnActive = false;
		if (debugEnabled) {
			activeIterationsDebug = new ConcurrentHashMap<>();
		} else {
			activeIterationsDebug = Collections.emptyMap();
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public final boolean isOpen() throws SailException {
		return ((boolean) IS_OPEN.getAcquire(this));
	}

	protected void verifyIsOpen() throws SailException {
		if (!((boolean) IS_OPEN.getAcquire(this))) {
			throw new IllegalStateException("Connection has been closed");
		}
	}

	/**
	 * Verifies if a transaction is currently active. Throws a {@link SailException} if no transaction is active.
	 *
	 * @throws SailException if no transaction is active.
	 */
	protected void verifyIsActive() throws SailException {
		if (!isActive()) {
			throw new SailException("No active transaction");
		}
	}

	@Override
	public void begin() throws SailException {
		begin(sailBase.getDefaultIsolationLevel());
	}

	@Override
	public void begin(IsolationLevel isolationLevel) throws SailException {
		if (isolationLevel == null) {
			isolationLevel = sailBase.getDefaultIsolationLevel();
		}

		IsolationLevel compatibleLevel = IsolationLevels.getCompatibleIsolationLevel(isolationLevel,
				sailBase.getSupportedIsolationLevels());
		if (compatibleLevel == null) {
			throw new UnknownSailTransactionStateException(
					"Isolation level " + isolationLevel + " not compatible with this Sail");
		}
		this.transactionIsolationLevel = compatibleLevel;

		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();

			updateLock.lock();
			try {
				if (isActive()) {
					throw new SailException("a transaction is already active on this connection.");
				}

				startTransactionInternal();
				txnActive = true;
			} finally {
				updateLock.unlock();
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}

		}
		startUpdate(null);
	}

	/**
	 * Retrieve the currently set {@link IsolationLevel}.
	 *
	 * @return the current {@link IsolationLevel}. If no transaction is active, this may be <code>null</code>.
	 */
	protected IsolationLevel getTransactionIsolation() {
		return this.transactionIsolationLevel;
	}

	@Override
	public boolean isActive() throws UnknownSailTransactionStateException {
		return transactionActive();
	}

	@Override
	public final void close() throws SailException {
		// obtain an exclusive lock so that any further operations on this
		// connection (including those from any concurrent threads) are blocked.
		if (!IS_OPEN.compareAndSet(this, true, false)) {
			return;
		}
		if (useConnectionLock) {
			connectionLock.writeLock().lock();
		}
		try {

			while (true) {
				long sumDone = unblockClose.sum();
				long sumBlocking = blockClose.sum();
				if (sumDone == sumBlocking) {
					break;
				} else {
					Thread.onSpinWait();
				}
			}

			try {
				forceCloseActiveOperations();

				if (txnActive) {
					logger.warn("Rolling back transaction due to connection close",
							debugEnabled ? new Throwable() : null);
					try {
						// Use internal method to avoid deadlock: the public
						// rollback method will try to obtain a connection lock
						rollbackInternal();
					} finally {
						txnActive = false;
						txnPrepared = false;
					}
				}

				closeInternal();

				if (isActiveOperation()) {
					throw new SailException("Connection closed before all iterations were closed.");
				}
			} finally {
				sailBase.connectionClosed(this);
			}
		} finally {
			if (useConnectionLock) {
				connectionLock.writeLock().unlock();
			}
		}

	}

	@Override
	public final CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		flushPendingUpdates();
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();
			CloseableIteration<? extends BindingSet, QueryEvaluationException> iteration = null;
			try {
				iteration = evaluateInternal(tupleExpr, dataset, bindings, includeInferred);
				return registerIteration(iteration);
			} catch (Throwable t) {
				if (iteration != null) {
					iteration.close();
				}
				throw t;
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
		flushPendingUpdates();
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();
			return registerIteration(getContextIDsInternal());
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred,
			Value obj, boolean includeInferred, Resource... contexts) throws SailException {
		flushPendingUpdates();
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();
			CloseableIteration<? extends Statement, SailException> iteration = null;
			try {
				iteration = getStatementsInternal(subj, pred, obj, includeInferred, contexts);
				return registerIteration(iteration);
			} catch (Throwable t) {
				if (iteration != null) {
					iteration.close();
				}
				throw t;
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws SailException {
		flushPendingUpdates();
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();
			return hasStatementInternal(subj, pred, obj, includeInferred, contexts);
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}

	}

	protected boolean hasStatementInternal(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource[] contexts) {
		try (var iteration = getStatementsInternal(subj, pred, obj, includeInferred, contexts)) {
			return iteration.hasNext();
		}
	}

	@Override
	public final long size(Resource... contexts) throws SailException {
		flushPendingUpdates();
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();
			return sizeInternal(contexts);
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	protected final boolean transactionActive() {
		return txnActive;
	}

	/**
	 * <B>IMPORTANT</B> Since Sesame 2.7.0. this method no longer automatically starts a transaction, but instead
	 * verifies if a transaction is active and if not throws an exception. The method is left in for transitional
	 * purposes only. Sail implementors are advised that by contract, any update operation on the Sail should check if a
	 * transaction has been started via {@link SailConnection#isActive} and throw a SailException if not. Implementors
	 * can use {@link AbstractSailConnection#verifyIsActive()} as a convenience method for this check.
	 *
	 * @throws SailException if no transaction is active.
	 * @deprecated since 2.7.0. Use {@link #verifyIsActive()} instead. We should not automatically start a transaction
	 *             at the sail level. Instead, an exception should be thrown when an update is executed without first
	 *             starting a transaction.
	 */
	@Deprecated
	protected void autoStartTransaction() throws SailException {
		verifyIsActive();
	}

	@Override
	public void flush() throws SailException {
		if (isActive()) {
			endUpdate(null);
			startUpdate(null);
		}
	}

	@Override
	public final void prepare() throws SailException {
		if (isActive()) {
			endUpdate(null);
		}
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();

			updateLock.lock();
			try {
				if (txnActive) {
					prepareInternal();
					txnPrepared = true;
				}
			} finally {
				updateLock.unlock();
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final void commit() throws SailException {
		if (isActive()) {
			endUpdate(null);
		}

		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();

			updateLock.lock();
			try {
				if (txnActive) {
					if (!txnPrepared) {
						prepareInternal();
					}
					commitInternal();
					txnActive = false;
					txnPrepared = false;
				}
			} finally {
				updateLock.unlock();
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final void rollback() throws SailException {
		synchronized (added) {
			added.clear();
		}
		synchronized (removed) {
			removed.clear();
		}
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();

			updateLock.lock();
			try {
				if (txnActive) {
					try {
						rollbackInternal();
					} finally {
						txnActive = false;
						txnPrepared = false;
					}
				} else {
					logger.warn("Cannot rollback transaction on connection because transaction is not active",
							debugEnabled ? new Throwable() : null);
				}
			} finally {
				updateLock.unlock();
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (pendingRemovals()) {
			flushPendingUpdates();
		}
		addStatement(null, subj, pred, obj, contexts);
		statementsAdded = true;
	}

	@Override
	public final void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (pendingAdds()) {
			flushPendingUpdates();
		}
		removeStatement(null, subj, pred, obj, contexts);
		statementsRemoved = true;
	}

	@Override
	public void startUpdate(UpdateContext op) throws SailException {
		if (op != null) {
			flushPendingUpdates();
		}
		synchronized (removed) {
			assert !removed.containsKey(op);
			removed.put(op, new LinkedList<>());
		}

		synchronized (added) {
			assert !added.containsKey(op);
			added.put(op, new LinkedList<>());
		}
	}

	/**
	 * The default implementation buffers added statements until the update operation is complete.
	 */
	@Override
	public void addStatement(UpdateContext op, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		verifyIsOpen();
		verifyIsActive();
		synchronized (added) {
			assert added.containsKey(op);
			Collection<Statement> pending = added.get(op);
			if (contexts == null || contexts.length == 0) {
				pending.add(sailBase.getValueFactory().createStatement(subj, pred, obj));
			} else {
				for (Resource ctx : contexts) {
					pending.add(sailBase.getValueFactory().createStatement(subj, pred, obj, ctx));
				}
			}
			if (pending.size() % BLOCK_SIZE == 0 && !isActiveOperation()) {
				endUpdate(op);
				startUpdate(op);
			}
		}
		statementsAdded = true;
	}

	/**
	 * The default implementation buffers removed statements until the update operation is complete.
	 */
	@Override
	public void removeStatement(UpdateContext op, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		verifyIsOpen();
		verifyIsActive();
		synchronized (removed) {
			assert removed.containsKey(op);
			Collection<Statement> pending = removed.get(op);
			if (contexts == null) {
				pending.add(new WildStatement(subj, pred, obj));
			} else if (contexts.length == 0) {
				pending.add(new WildStatement(subj, pred, obj, wildContext));
			} else {
				for (Resource ctx : contexts) {
					pending.add(new WildStatement(subj, pred, obj, ctx));
				}
			}
			if (pending.size() % BLOCK_SIZE == 0 && !isActiveOperation()) {
				endUpdate(op);
				startUpdate(op);
			}
		}
		statementsRemoved = true;
	}

	@Override
	public final void endUpdate(UpdateContext op) throws SailException {
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();

			updateLock.lock();
			try {
				verifyIsActive();
				endUpdateInternal(op);
			} finally {
				updateLock.unlock();
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
			if (op != null) {
				flush();
			}
		}
	}

	protected void endUpdateInternal(UpdateContext op) throws SailException {
		Collection<Statement> model;
		// realize DELETE
		synchronized (removed) {
			model = removed.remove(op);
		}
		if (model != null) {
			for (Statement st : model) {
				Resource ctx = st.getContext();
				if (wildContext.equals(ctx)) {
					removeStatementsInternal(st.getSubject(), st.getPredicate(), st.getObject());
				} else {
					removeStatementsInternal(st.getSubject(), st.getPredicate(), st.getObject(), ctx);
				}
			}
		}
		// realize INSERT
		synchronized (added) {
			model = added.remove(op);
		}
		if (model != null) {
			for (Statement st : model) {
				addStatementInternal(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			}
		}
	}

	@Override
	public final void clear(Resource... contexts) throws SailException {
		flushPendingUpdates();
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();

			updateLock.lock();
			try {
				verifyIsActive();
				clearInternal(contexts);
				statementsRemoved = true;
			} finally {
				updateLock.unlock();
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException {
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();
			return registerIteration(getNamespacesInternal());
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final String getNamespace(String prefix) throws SailException {
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();
			return getNamespaceInternal(prefix);
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final void setNamespace(String prefix, String name) throws SailException {
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}
		if (name == null) {
			throw new NullPointerException("name must not be null");
		}
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();

			updateLock.lock();
			try {
				verifyIsActive();
				setNamespaceInternal(prefix, name);
			} finally {
				updateLock.unlock();
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final void removeNamespace(String prefix) throws SailException {
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();

			updateLock.lock();
			try {
				verifyIsActive();
				removeNamespaceInternal(prefix);
			} finally {
				updateLock.unlock();
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}
		}
	}

	@Override
	public final void clearNamespaces() throws SailException {
		if (useConnectionLock) {
			connectionLock.readLock().lock();
		}
		blockClose.increment();
		try {
			verifyIsOpen();

			updateLock.lock();
			try {
				verifyIsActive();
				clearNamespacesInternal();
			} finally {
				updateLock.unlock();
			}
		} finally {
			unblockClose.increment();
			if (useConnectionLock) {
				connectionLock.readLock().unlock();
			}

		}
	}

	@Override
	public boolean pendingRemovals() {
		return statementsRemoved;
	}

	protected boolean pendingAdds() {
		return statementsAdded;
	}

	protected void setStatementsAdded() {
		statementsAdded = true;
	}

	protected void setStatementsRemoved() {
		statementsRemoved = true;
	}

	@Deprecated(forRemoval = true)
	protected Lock getSharedConnectionLock() throws SailException {
		return new JavaLock(connectionLock.readLock());
	}

	@Deprecated(forRemoval = true)
	protected Lock getExclusiveConnectionLock() throws SailException {
		return new JavaLock(connectionLock.writeLock());
	}

	@Deprecated(forRemoval = true)
	protected Lock getTransactionLock() throws SailException {
		return new JavaLock(updateLock);
	}

	/**
	 * Registers an iteration as active by wrapping it in a {@link SailBaseIteration} object and adding it to the list
	 * of active iterations.
	 */
	protected <T, E extends Exception> CloseableIteration<T, E> registerIteration(CloseableIteration<T, E> iter) {
		if (iter instanceof EmptyIteration) {
			return iter;
		}

		iterationsOpened.increment();

		if (debugEnabled) {
			var result = new SailBaseIteration<>(iter, this);
			activeIterationsDebug.put(result, new Throwable("Unclosed iteration"));
			return result;
		} else {
			return new CleanerIteration<>(new SailBaseIteration<>(iter, this), cleaner);
		}
	}

	/**
	 * Called by {@link SailBaseIteration} to indicate that it has been closed.
	 */
	protected void iterationClosed(SailBaseIteration<?, ?> iter) {
		if (debugEnabled) {
			activeIterationsDebug.remove(iter);
		}
		iterationsClosed.increment();
	}

	protected abstract void closeInternal() throws SailException;

	protected abstract CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(
			TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException;

	protected abstract CloseableIteration<? extends Resource, SailException> getContextIDsInternal()
			throws SailException;

	protected abstract CloseableIteration<? extends Statement, SailException> getStatementsInternal(Resource subj,
			IRI pred, Value obj, boolean includeInferred, Resource... contexts) throws SailException;

	protected abstract long sizeInternal(Resource... contexts) throws SailException;

	protected abstract void startTransactionInternal() throws SailException;

	protected void prepareInternal() throws SailException {
		// do nothing
	}

	protected abstract void commitInternal() throws SailException;

	protected abstract void rollbackInternal() throws SailException;

	protected abstract void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException;

	protected abstract void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException;

	protected abstract void clearInternal(Resource... contexts) throws SailException;

	protected abstract CloseableIteration<? extends Namespace, SailException> getNamespacesInternal()
			throws SailException;

	protected abstract String getNamespaceInternal(String prefix) throws SailException;

	protected abstract void setNamespaceInternal(String prefix, String name) throws SailException;

	protected abstract void removeNamespaceInternal(String prefix) throws SailException;

	protected abstract void clearNamespacesInternal() throws SailException;

	protected boolean isActiveOperation() {
		long closed = iterationsClosed.sum();
		long opened = iterationsOpened.sum();
		return closed != opened;
	}

	private void forceCloseActiveOperations() throws SailException {
		for (int i = 0; i < 10 && isActiveOperation() && !debugEnabled; i++) {
			System.gc();
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		if (debugEnabled) {

			var activeIterationsCopy = new IdentityHashMap<>(activeIterationsDebug);
			activeIterationsDebug.clear();

			if (!activeIterationsCopy.isEmpty()) {
				for (var entry : activeIterationsCopy.entrySet()) {
					try {
						logger.warn("Unclosed iteration", entry.getValue());
						entry.getKey().close();
					} catch (Exception e) {
						if (e instanceof InterruptedException) {
							Thread.currentThread().interrupt();
						}
						logger.warn("Exception occurred while closing unclosed iterations.", e);
					}
				}

				var entry = activeIterationsCopy.entrySet().stream().findAny().orElseThrow();

				throw new SailException(
						"Connection closed before all iterations were closed: " + entry.getKey().toString(),
						entry.getValue());
			}

		}
	}

	/**
	 * If there are no open operations.
	 *
	 * @throws SailException
	 */
	private void flushPendingUpdates() throws SailException {

		if ((statementsAdded || statementsRemoved) && isActive()) {
			if (isActive()) {
				synchronized (this) {
					if ((statementsAdded || statementsRemoved) && isActive()) {
						flush();
						statementsAdded = false;
						statementsRemoved = false;
					}
				}
			}
		}
	}

	/**
	 * Statement pattern that uses null values as wild cards.
	 *
	 * @author James Leigh
	 */
	private static class WildStatement implements Statement {

		private static final long serialVersionUID = 3363010521961228565L;

		/**
		 * The statement's subject.
		 */
		private final Resource subject;

		/**
		 * The statement's predicate.
		 */
		private final IRI predicate;

		/**
		 * The statement's object.
		 */
		private final Value object;

		/**
		 * The statement's context, if applicable.
		 */
		private final Resource context;

		/*--------------*
		 * Constructors *
		 *--------------*/

		/**
		 * Creates a new Statement with the supplied subject, predicate and object.
		 *
		 * @param subject   The statement's subject, may be <var>null</var>.
		 * @param predicate The statement's predicate, may be <var>null</var>.
		 * @param object    The statement's object, may be <var>null</var>.
		 */
		public WildStatement(Resource subject, IRI predicate, Value object) {
			this(subject, predicate, object, null);
		}

		/**
		 * Creates a new Statement with the supplied subject, predicate and object for the specified associated context.
		 *
		 * @param subject   The statement's subject, may be <var>null</var>.
		 * @param predicate The statement's predicate, may be <var>null</var>.
		 * @param object    The statement's object, may be <var>null</var>.
		 * @param context   The statement's context, <var>null</var> to indicate no context is associated.
		 */
		public WildStatement(Resource subject, IRI predicate, Value object, Resource context) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
			this.context = context;
		}

		/*---------*
		 * Methods *
		 *---------*/

		// Implements Statement.getSubject()
		@Override
		public Resource getSubject() {
			return subject;
		}

		// Implements Statement.getPredicate()
		@Override
		public IRI getPredicate() {
			return predicate;
		}

		// Implements Statement.getObject()
		@Override
		public Value getObject() {
			return object;
		}

		@Override
		public Resource getContext() {
			return context;
		}

		@Override
		public String toString() {
			return "(" +
					getSubject() +
					", " +
					getPredicate() +
					", " +
					getObject() +
					")" +
					" [" + getContext() + "]";
		}
	}

	private static class JavaLock implements Lock {

		private final java.util.concurrent.locks.Lock javaLock;

		private boolean isActive = true;

		public JavaLock(java.util.concurrent.locks.Lock javaLock) {
			this.javaLock = javaLock;
			javaLock.lock();
		}

		@Override
		public synchronized boolean isActive() {
			return isActive;
		}

		@Override
		public synchronized void release() {
			if (isActive) {
				javaLock.unlock();
				isActive = false;
			}
		}
	}
}
