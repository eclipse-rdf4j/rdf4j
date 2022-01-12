/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.helpers.impl;

import java.util.Random;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.eclipse.rdf4j.sail.helpers.AbstractSailConnection;

public class SailConnectionImpl extends AbstractSailConnection {
	public SailConnectionImpl(AbstractSail sailBase) {
		super(sailBase);
	}

	@Override
	protected void closeInternal() throws SailException {

	}

	@Override
	protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		return new EmptyIteration<>();
	}

	@Override
	protected CloseableIteration<? extends Resource, SailException> getContextIDsInternal() throws SailException {
		return new EmptyIteration<>();
	}

	@Override
	protected CloseableIteration<? extends Statement, SailException> getStatementsInternal(Resource subj, IRI pred,
			Value obj, boolean includeInferred, Resource... contexts) throws SailException {

		return new CloseableIteration<>() {

			@Override
			public void close() throws SailException {
			}

			@Override
			public boolean hasNext() throws SailException {
//				Thread.onSpinWait();
				return false;
			}

			@Override
			public Statement next() throws SailException {
				return null;
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	protected long sizeInternal(Resource... contexts) throws SailException {
		return 0;
	}

	@Override
	protected void startTransactionInternal() throws SailException {

	}

	@Override
	protected void commitInternal() throws SailException {

	}

	@Override
	protected void rollbackInternal() throws SailException {

	}

	@Override
	protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {

	}

	@Override
	protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {

	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {

	}

	@Override
	protected CloseableIteration<? extends Namespace, SailException> getNamespacesInternal() throws SailException {
		return new EmptyIteration<>();
	}

	@Override
	protected String getNamespaceInternal(String prefix) throws SailException {
		return null;
	}

	@Override
	protected void setNamespaceInternal(String prefix, String name) throws SailException {

	}

	@Override
	protected void removeNamespaceInternal(String prefix) throws SailException {

	}

	@Override
	protected void clearNamespacesInternal() throws SailException {

	}
}
