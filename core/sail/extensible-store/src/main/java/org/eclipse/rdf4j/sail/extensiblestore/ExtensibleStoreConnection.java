/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSourceConnection;
import org.eclipse.rdf4j.sail.helpers.DefaultSailChangedEvent;

/**
 * @author Håvard Mikkelsen Ottestad
 */
@Experimental
public class ExtensibleStoreConnection<E extends ExtensibleStore> extends SailSourceConnection {

	protected final E sail;

	private volatile DefaultSailChangedEvent sailChangedEvent;

	protected ExtensibleStoreConnection(E sail) {
		super(sail, sail.getSailStore(), sail.getEvaluationStrategyFactory());
		this.sail = sail;
		sailChangedEvent = new DefaultSailChangedEvent(sail);
		useConnectionLock = false;
	}

	public E getSail() {
		return sail;
	}

	@Override
	protected void startTransactionInternal() throws SailException {
		super.startTransactionInternal();
	}

	@Override
	protected void commitInternal() throws SailException {
		super.commitInternal();

		sail.notifySailChanged(sailChangedEvent);

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

}
