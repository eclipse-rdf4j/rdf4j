/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
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

	private volatile boolean isOpen;

	private volatile boolean txnActive;

	private volatile boolean txnPrepared;

	/**
	 * Lock used to give the {@link #close()} method exclusive access to a connection.
	 * <ul>
	 * <li>write lock: close()
	 * <li>read lock: all other (public) methods
	 * </ul>
	 */
	protected final ReentrantReadWriteLock connectionLock = new ReentrantReadWriteLock();

	/**
	 * Lock used to prevent concurrent calls to update methods like addStatement, clear, commit, etc. within a
	 * transaction.
	 */
	protected final ReentrantLock updateLock = new ReentrantLock();

	private final Map<SailBaseIteration, Throwable> activeIterations = new IdentityHashMap<>();

	/**
	 * Statements that are currently being removed, but not yet realized, by an active operation.
	 */
	private final Map<UpdateContext, Collection<Statement>> removed = new HashMap<>();

	/**
	 * Statements that are currently being added, but not yet realized, by an active operation.
	 */
	private final Map<UpdateContext, Collection<Statement>> added = new HashMap<>();

	/**
	 * Used to indicate a removed statement from all contexts.
	 */
	private final BNode wildContext = SimpleValueFactory.getInstance().createBNode();

	private IsolationLevel transactionIsolationLevel;

	private boolean pendingAdds;

	private boolean pendingRemovals;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public AbstractSailConnection(AbstractSail sailBase) {
		this.sailBase = sailBase;
		isOpen = true;
		txnActive = false;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public final boolean isOpen() throws SailException {
		return isOpen;
	}

	protected void verifyIsOpen() throws SailException {
		if (!isOpen) {
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

		connectionLock.readLock().lock();
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
			connectionLock.readLock().unlock();
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
		connectionLock.writeLock().lock();

		try {
			if (isOpen) {
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
				} finally {
					isOpen = false;
					sailBase.connectionClosed(this);
				}
			}
		} finally {
			// Release the exclusive lock. Any threads waiting to obtain a
			// non-exclusive read lock will get one and then fail with an
			// IllegalStateException, because the connection is no longer open.
			connectionLock.writeLock().unlock();
		}
	}

	@Override
	public final CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		flushPendingUpdates();
		connectionLock.readLock().lock();
		try {
			verifyIsOpen();
			boolean registered = false;
			CloseableIteration<? extends BindingSet, QueryEvaluationException> iteration = null;
			CloseableIteration<? extends BindingSet, QueryEvaluationException> registeredIteration = null;
			try {
				iteration = evaluateInternal(tupleExpr, dataset, bindings, includeInferred);
				registeredIteration = registerIteration(iteration);
				registered = true;
				return registeredIteration;
			} finally {
				if (!registered) {
					try {
						try {
							if (registeredIteration != null) {
								registeredIteration.close();
							}
						} finally {
							if (iteration != null) {
								iteration.close();
							}
						}
					} catch (QueryEvaluationException e) {
						throw new SailException(e);
					}
				}
			}
		} finally {
			connectionLock.readLock().unlock();
		}
	}

	@Override
	public final CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
		flushPendingUpdates();
		connectionLock.readLock().lock();
		try {
			verifyIsOpen();
			return registerIteration(getContextIDsInternal());
		} finally {
			connectionLock.readLock().unlock();
		}
	}

	@Override
	public final CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred,
			Value obj, boolean includeInferred, Resource... contexts) throws SailException {
		flushPendingUpdates();
		connectionLock.readLock().lock();
		try {
			verifyIsOpen();
			boolean registered = false;
			CloseableIteration<? extends Statement, SailException> iteration = getStatementsInternal(subj, pred, obj,
					includeInferred, contexts);
			try {
				CloseableIteration<? extends Statement, SailException> registeredIteration = registerIteration(
						iteration);
				registered = true;
				return registeredIteration;
			} finally {
				if (!registered) {
					iteration.close();
				}
			}
		} finally {
			connectionLock.readLock().unlock();
		}
	}

	@Override
	public final long size(Resource... contexts) throws SailException {
		flushPendingUpdates();
		connectionLock.readLock().lock();
		try {
			verifyIsOpen();
			return sizeInternal(contexts);
		} finally {
			connectionLock.readLock().unlock();
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
	 * @deprecated since 2.7.0. Use {@link #verifyIsActive()} instead. We should not automatically start a transaction
	 *             at the sail level. Instead, an exception should be thrown when an update is executed without first
	 *             starting a transaction.
	 * @throws SailException if no transaction is active.
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
		connectionLock.readLock().lock();
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
			connectionLock.readLock().unlock();
		}
	}

	@Override
	public final void commit() throws SailException {
		if (isActive()) {
			endUpdate(null);
		}

		connectionLock.readLock().lock();
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
			connectionLock.readLock().unlock();
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
		connectionLock.readLock().lock();
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
			connectionLock.readLock().unlock();
		}
	}

	@Override
	public final void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (pendingRemovals()) {
			flushPendingUpdates();
		}
		pendingAdds = true;
		addStatement(null, subj, pred, obj, contexts);
	}

	@Override
	public final void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (pendingAdds()) {
			flushPendingUpdates();
		}
		pendingRemovals = true;
		removeStatement(null, subj, pred, obj, contexts);
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
	}

	@Override
	public final void endUpdate(UpdateContext op) throws SailException {
		connectionLock.readLock().lock();
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
			connectionLock.readLock().unlock();
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
		connectionLock.readLock().lock();
		try {
			verifyIsOpen();

			updateLock.lock();
			try {
				verifyIsActive();
				clearInternal(contexts);
			} finally {
				updateLock.unlock();
			}
		} finally {
			connectionLock.readLock().unlock();
		}
	}

	@Override
	public final CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException {
		connectionLock.readLock().lock();
		try {
			verifyIsOpen();
			return registerIteration(getNamespacesInternal());
		} finally {
			connectionLock.readLock().unlock();
		}
	}

	@Override
	public final String getNamespace(String prefix) throws SailException {
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}
		connectionLock.readLock().lock();
		try {
			verifyIsOpen();
			return getNamespaceInternal(prefix);
		} finally {
			connectionLock.readLock().unlock();
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
		connectionLock.readLock().lock();
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
			connectionLock.readLock().unlock();
		}
	}

	@Override
	public final void removeNamespace(String prefix) throws SailException {
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}
		connectionLock.readLock().lock();
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
			connectionLock.readLock().unlock();
		}
	}

	@Override
	public final void clearNamespaces() throws SailException {
		connectionLock.readLock().lock();
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
			connectionLock.readLock().unlock();
		}
	}

	@Override
	public boolean pendingRemovals() {
		return pendingRemovals;
	}

	protected boolean pendingAdds() {
		return pendingAdds;
	}

	/**
	 * @deprecated Use {@link #connectionLock} directly instead.
	 */
	@Deprecated
	protected org.eclipse.rdf4j.common.concurrent.locks.Lock getSharedConnectionLock() throws SailException {
		return new JavaLock(connectionLock.readLock());
	}

	/**
	 * @deprecated Use {@link #connectionLock} directly instead.
	 */
	@Deprecated
	protected org.eclipse.rdf4j.common.concurrent.locks.Lock getExclusiveConnectionLock() throws SailException {
		return new JavaLock(connectionLock.writeLock());
	}

	/**
	 * @deprecated Use {@link #updateLock} directly instead.
	 */
	@Deprecated
	protected org.eclipse.rdf4j.common.concurrent.locks.Lock getTransactionLock() throws SailException {
		return new JavaLock(updateLock);
	}

	/**
	 * Registers an iteration as active by wrapping it in a {@link SailBaseIteration} object and adding it to the list
	 * of active iterations.
	 */
	protected <T, E extends Exception> CloseableIteration<T, E> registerIteration(CloseableIteration<T, E> iter) {
		SailBaseIteration<T, E> result = new SailBaseIteration<>(iter, this);
		Throwable stackTrace = debugEnabled ? new Throwable() : null;
		synchronized (activeIterations) {
			activeIterations.put(result, stackTrace);
		}
		return result;
	}

	/**
	 * Called by {@link SailBaseIteration} to indicate that it has been closed.
	 */
	protected void iterationClosed(SailBaseIteration iter) {
		synchronized (activeIterations) {
			activeIterations.remove(iter);
		}
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
		synchronized (activeIterations) {
			return !activeIterations.isEmpty();
		}
	}

	private void forceCloseActiveOperations() throws SailException {
		final Map<SailBaseIteration, Throwable> activeIterationsCopy;

		synchronized (activeIterations) {
			// Copy the current contents of the map so that we don't have to
			// synchronize on activeIterations. This prevents a potential
			// deadlock with concurrent calls to connectionClosed()
			activeIterationsCopy = new IdentityHashMap<>(activeIterations);
			activeIterations.clear();
		}

		final List<SailException> toThrowExceptions = new ArrayList<>();

		for (Map.Entry<SailBaseIteration, Throwable> entry : activeIterationsCopy.entrySet()) {
			SailBaseIteration ci = entry.getKey();
			Throwable creatorTrace = entry.getValue();

			try {
				if (creatorTrace != null) {
					logger.warn("Forced closing of unclosed iteration that was created in:",
							debugEnabled ? creatorTrace : null);
				}
				ci.close();
			} catch (SailException e) {
				toThrowExceptions.add(e);
			} catch (Exception e) {
				toThrowExceptions.add(new SailException(e));
			}
		}

		if (!toThrowExceptions.isEmpty()) {
			throw toThrowExceptions.get(0);
		}
	}

	/**
	 * If there are no open operations.
	 *
	 * @throws SailException
	 */
	synchronized private void flushPendingUpdates() throws SailException {
		if (!isActiveOperation()
				|| isActive() && !getTransactionIsolation().isCompatibleWith(IsolationLevels.SNAPSHOT_READ)) {
			flush();
			pendingAdds = false;
			pendingRemovals = false;
		}
	}

	/**
	 * Statement pattern that uses null values as wild cards.
	 *
	 * @author James Leigh
	 */
	private class WildStatement implements Statement {

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
		 * @param subject   The statement's subject, may be <tt>null</tt>.
		 * @param predicate The statement's predicate, may be <tt>null</tt>.
		 * @param object    The statement's object, may be <tt>null</tt>.
		 */
		public WildStatement(Resource subject, IRI predicate, Value object) {
			this(subject, predicate, object, null);
		}

		/**
		 * Creates a new Statement with the supplied subject, predicate and object for the specified associated context.
		 *
		 * @param subject   The statement's subject, may be <tt>null</tt>.
		 * @param predicate The statement's predicate, may be <tt>null</tt>.
		 * @param object    The statement's object, may be <tt>null</tt>.
		 * @param context   The statement's context, <tt>null</tt> to indicate no context is associated.
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
			StringBuilder sb = new StringBuilder(256);

			sb.append("(");
			sb.append(getSubject());
			sb.append(", ");
			sb.append(getPredicate());
			sb.append(", ");
			sb.append(getObject());
			sb.append(")");
			sb.append(" [").append(getContext()).append("]");

			return sb.toString();
		}
	}

	private static class JavaLock implements org.eclipse.rdf4j.common.concurrent.locks.Lock {

		private final Lock javaLock;

		private boolean isActive = true;

		public JavaLock(Lock javaLock) {
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
