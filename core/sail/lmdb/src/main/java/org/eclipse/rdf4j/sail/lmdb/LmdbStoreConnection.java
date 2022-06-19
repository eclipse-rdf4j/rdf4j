/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;

import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailReadOnlyException;
import org.eclipse.rdf4j.sail.base.SailSourceConnection;
import org.eclipse.rdf4j.sail.helpers.DefaultSailChangedEvent;

/**
 * Connection to an {@link LmdbStore}.
 *
 */
public class LmdbStoreConnection extends SailSourceConnection {

	/*-----------*
	 * Constants *
	 *-----------*/

	protected final LmdbStore lmdbStore;

	/*-----------*
	 * Variables *
	 *-----------*/

	private volatile DefaultSailChangedEvent sailChangedEvent;

	/**
	 * The transaction lock held by this connection during transactions.
	 */
	private volatile Lock txnLock;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected LmdbStoreConnection(LmdbStore sail) throws IOException {
		super(sail, sail.getSailStore(), sail.getEvaluationStrategyFactory());
		this.lmdbStore = sail;
		sailChangedEvent = new DefaultSailChangedEvent(sail);
		useConnectionLock = false;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void startTransactionInternal() throws SailException {
		if (!lmdbStore.isWritable()) {
			throw new SailReadOnlyException("Unable to start transaction: data file is locked or read-only");
		}
		boolean releaseLock = true;
		try {
			if (txnLock == null || !txnLock.isActive()) {
				txnLock = lmdbStore.getTransactionLock(getTransactionIsolation());
				if (lmdbStore.isIsolationDisabled()) {
					// if the transaction isn't isolated then we need to keep holding our exclusive lock until commit
					releaseLock = false;
				}
			}
			super.startTransactionInternal();
		} finally {
			if (releaseLock && txnLock != null && txnLock.isActive()) {
				txnLock.release();
			}
		}
	}

	@Override
	protected void commitInternal() throws SailException {
		try {
			super.commitInternal();
		} finally {
			if (txnLock != null && txnLock.isActive()) {
				txnLock.release();
			}
		}

		lmdbStore.notifySailChanged(sailChangedEvent);

		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(lmdbStore);
	}

	@Override
	protected void rollbackInternal() throws SailException {
		try {
			super.rollbackInternal();
		} finally {
			if (txnLock != null && txnLock.isActive()) {
				txnLock.release();
			}
		}
		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(lmdbStore);
	}

	@Override
	protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		// assume the triple is not yet present in the triple store
		sailChangedEvent.setStatementsAdded(true);
	}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		boolean ret = super.addInferredStatement(subj, pred, obj, contexts);
		// assume the triple is not yet present in the triple store
		sailChangedEvent.setStatementsAdded(true);
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

}
