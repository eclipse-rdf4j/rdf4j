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
package org.eclipse.rdf4j.sail.inferencer;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;

/**
 * An extension of ConnectionWrapper that implements the {@link InferencerConnection} interface.
 *
 * @author Arjohn Kampman
 */
public class InferencerConnectionWrapper extends NotifyingSailConnectionWrapper implements InferencerConnection {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new InferencerConnectionWrapper object that wraps the supplied transaction.
	 */
	public InferencerConnectionWrapper(InferencerConnection con) {
		super(con);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the connection that is wrapped by this object.
	 *
	 * @return The connection that was supplied to the constructor of this class.
	 */
	@Override
	public InferencerConnection getWrappedConnection() {
		return (InferencerConnection) super.getWrappedConnection();
	}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		return getWrappedConnection().addInferredStatement(subj, pred, obj, contexts);
	}

	@Override
	public boolean removeInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		return getWrappedConnection().removeInferredStatement(subj, pred, obj, contexts);
	}

	@Override
	public void clearInferred(Resource... contexts) throws SailException {
		getWrappedConnection().clearInferred(contexts);
	}

	@Override
	public void flush() throws SailException {
		getWrappedConnection().flush();
		flushUpdates();
	}

	@Override
	public void flushUpdates() throws SailException {
		getWrappedConnection().flushUpdates();
	}

	/**
	 * Calls {@link #flushUpdates()} before forwarding the call to the wrapped connection.
	 */
	@Override
	public void prepare() throws SailException {
		flushUpdates();
		super.prepare();
	}

	/**
	 * Calls {@link #flushUpdates()} before forwarding the call to the wrapped connection.
	 */
	@Override
	public void commit() throws SailException {
		flushUpdates();
		super.commit();
	}

	/**
	 * Calls {@link #flushUpdates()} before forwarding the call to the wrapped connection.
	 */
	@Override
	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		flushUpdates();
		return super.evaluate(tupleExpr, dataset, bindings, includeInferred);
	}

	/**
	 * Calls {@link #flushUpdates()} before forwarding the call to the wrapped connection.
	 */
	@Override
	public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
		flushUpdates();
		return super.getContextIDs();
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws SailException {
		flushUpdates();
		return super.hasStatement(subj, pred, obj, includeInferred, contexts);
	}

	/**
	 * Calls {@link #flushUpdates()} before forwarding the call to the wrapped connection.
	 */
	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {
		flushUpdates();
		return super.getStatements(subj, pred, obj, includeInferred, contexts);
	}

	/**
	 * Calls {@link #flushUpdates()} before forwarding the call to the wrapped connection.
	 */
	@Override
	public long size(Resource... contexts) throws SailException {
		flushUpdates();
		return super.size(contexts);
	}
}
