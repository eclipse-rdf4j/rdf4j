/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.IOException;

import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailReadOnlyException;
import org.eclipse.rdf4j.sail.base.SailSourceConnection;
import org.eclipse.rdf4j.sail.features.ThreadSafetyAware;
import org.eclipse.rdf4j.sail.helpers.DefaultSailChangedEvent;

/**
 * @author Arjohn Kampman
 */
public class NativeStoreConnection extends SailSourceConnection implements ThreadSafetyAware {

	/*-----------*
	 * Constants *
	 *-----------*/

	protected final NativeStore nativeStore;

	/*-----------*
	 * Variables *
	 *-----------*/

	private volatile DefaultSailChangedEvent sailChangedEvent;

	/**
	 * The transaction lock held by this connection during transactions.
	 */
	private Lock txnLock;

	private int addedCount;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected NativeStoreConnection(NativeStore sail) throws IOException {
		super(sail, sail.getSailStore(), sail.getEvaluationStrategyFactory());
		this.nativeStore = sail;
		sailChangedEvent = new DefaultSailChangedEvent(sail);
		useConnectionLock = false;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void startTransactionInternal() throws SailException {
		addedCount = 0;
		if (!nativeStore.isWritable()) {
			throw new SailReadOnlyException("Unable to start transaction: data file is locked or read-only");
		}

		assert txnLock == null : "Can not start another transaction before the previous one finishes!";
		txnLock = nativeStore.getTransactionLock(getTransactionIsolation());
		super.startTransactionInternal();
	}

	@Override
	protected void commitInternal() throws SailException {
		try {
			super.commitInternal();
		} finally {
			txnLock.release();
			txnLock = null;
		}

		nativeStore.notifySailChanged(sailChangedEvent);

		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(nativeStore);
	}

	@Override
	protected void rollbackInternal() throws SailException {
		try {
			super.rollbackInternal();
		} finally {
			txnLock.release();
			txnLock = null;
		}
		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(nativeStore);
	}

	@Override
	protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		// assume the triple is not yet present in the triple store
		sailChangedEvent.setStatementsAdded(true);

		if (getTransactionIsolation() == IsolationLevels.NONE) {
			if (++addedCount % 100000 == 0) {
				flushUpdates();
				addedCount = 0;
			}
		}

	}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {

		boolean ret = super.addInferredStatement(subj, pred, obj, contexts);
		// assume the triple is not yet present in the triple store
		sailChangedEvent.setStatementsAdded(true);
		if (getTransactionIsolation() == IsolationLevels.NONE) {
			if (++addedCount % 100000 == 0) {
				flushUpdates();
				addedCount = 0;
			}
		}

		return ret;
	}

	@Override
	protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		sailChangedEvent.setStatementsRemoved(true);
	}

	@Override
	public boolean removeInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		boolean ret = super.removeInferredStatement(subj, pred, obj, contexts);
		sailChangedEvent.setStatementsRemoved(true);
		return ret;
	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		super.clearInternal(contexts);
		sailChangedEvent.setStatementsRemoved(true);
	}

	@Override
	public void clearInferred(Resource... contexts) throws SailException {
		super.clearInferred(contexts);
		sailChangedEvent.setStatementsRemoved(true);
	}

	@Override
	public boolean supportsConcurrentReads() {
		return getTransactionIsolation() != null && getTransactionIsolation() != IsolationLevels.SERIALIZABLE;
	}

}
