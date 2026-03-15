/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3;

import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSourceConnection;
import org.eclipse.rdf4j.sail.helpers.DefaultSailChangedEvent;

/**
 * Connection to an {@link S3Store}.
 */
public class S3StoreConnection extends SailSourceConnection {

	protected final S3Store s3Store;

	private volatile DefaultSailChangedEvent sailChangedEvent;

	/**
	 * The transaction lock held by this connection during transactions.
	 */
	private volatile Lock txnLock;

	protected S3StoreConnection(S3Store sail) {
		super(sail, sail.getSailStore(), sail.getEvaluationStrategyFactory());
		this.s3Store = sail;
		sailChangedEvent = new DefaultSailChangedEvent(sail);
	}

	@Override
	protected void startTransactionInternal() throws SailException {
		boolean releaseLock = true;
		try {
			if (txnLock == null || !txnLock.isActive()) {
				txnLock = s3Store.getTransactionLock(getTransactionIsolation());
				if (s3Store.isIsolationDisabled()) {
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

		s3Store.notifySailChanged(sailChangedEvent);

		// create a fresh event object
		sailChangedEvent = new DefaultSailChangedEvent(s3Store);
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
		// create a fresh event object
		sailChangedEvent = new DefaultSailChangedEvent(s3Store);
	}

	@Override
	protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		sailChangedEvent.setStatementsAdded(true);
	}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		boolean ret = super.addInferredStatement(subj, pred, obj, contexts);
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
