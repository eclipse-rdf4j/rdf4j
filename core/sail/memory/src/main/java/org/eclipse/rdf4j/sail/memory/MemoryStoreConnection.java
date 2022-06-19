/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

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
 * Implementation of a Sail Connection for memory stores.
 *
 * @author Arjohn Kampman
 * @author jeen
 */
public class MemoryStoreConnection extends SailSourceConnection implements ThreadSafetyAware {

	/*-----------*
	 * Variables *
	 *-----------*/

	protected final MemoryStore sail;

	private volatile DefaultSailChangedEvent sailChangedEvent;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected MemoryStoreConnection(MemoryStore sail) {
		super(sail, sail.getSailStore(), sail.getEvaluationStrategyFactory());
		this.sail = sail;
		sailChangedEvent = new DefaultSailChangedEvent(sail);
		useConnectionLock = false;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void startTransactionInternal() throws SailException {
		if (!sail.isWritable()) {
			throw new SailReadOnlyException("Unable to start transaction: data file is locked or read-only");
		}
		super.startTransactionInternal();
		sail.cancelSyncTask();
	}

	@Override
	protected void commitInternal() throws SailException {
		super.commitInternal();

		sail.notifySailChanged(sailChangedEvent);
		sail.scheduleSyncTask();

		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(sail);
	}

	@Override
	protected void rollbackInternal() throws SailException {
		super.rollbackInternal();
		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(sail);
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

	public MemoryStore getSail() {
		return sail;
	}

	@Override
	public boolean supportsConcurrentReads() {
		return getTransactionIsolation() != null && getTransactionIsolation() != IsolationLevels.SERIALIZABLE;
	}
}
