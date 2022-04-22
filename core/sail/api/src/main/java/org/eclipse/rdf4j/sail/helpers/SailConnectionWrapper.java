/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.helpers;

import java.util.Optional;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.features.ThreadSafetyAware;

/**
 * An implementation of the SailConnection interface that wraps another SailConnection object and forwards any method
 * calls to the wrapped connection.
 *
 * @author Jeen Broekstra
 */
public class SailConnectionWrapper
		implements SailConnection, FederatedServiceResolverClient, ThreadSafetyAware {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The wrapped SailConnection.
	 */
	private final SailConnection wrappedCon;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new TransactionWrapper object that wraps the supplied connection.
	 */
	public SailConnectionWrapper(SailConnection wrappedCon) {
		this.wrappedCon = wrappedCon;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the connection that is wrapped by this object.
	 *
	 * @return The SailConnection object that was supplied to the constructor of this class.
	 */
	public SailConnection getWrappedConnection() {
		return wrappedCon;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		if (wrappedCon instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient) wrappedCon).setFederatedServiceResolver(resolver);
		}
	}

	@Override
	public boolean isOpen() throws SailException {
		return wrappedCon.isOpen();
	}

	@Override
	public void close() throws SailException {
		wrappedCon.close();
	}

	@Override
	public Optional<TupleExpr> prepareQuery(QueryLanguage ql, Query.QueryType type, String query, String baseURI) {
		return wrappedCon.prepareQuery(ql, type, query, baseURI);
	}

	@Override
	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		return wrappedCon.evaluate(tupleExpr, dataset, bindings, includeInferred);
	}

	@Override
	public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
		return wrappedCon.getContextIDs();
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {
		return wrappedCon.getStatements(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws SailException {
		return wrappedCon.hasStatement(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public long size(Resource... contexts) throws SailException {
		return wrappedCon.size(contexts);
	}

	/*
	 * Not in the API, preserving for binary compatibility. Will be removed in future. Should use {@link
	 * #size(Resource...)} instead, which is called by this method.
	 */
	public long size(Resource context) throws SailException {
		return wrappedCon.size(context);
	}

	@Override
	public void commit() throws SailException {
		wrappedCon.commit();
	}

	@Override
	public void rollback() throws SailException {
		wrappedCon.rollback();
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		wrappedCon.addStatement(subj, pred, obj, contexts);
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		wrappedCon.removeStatements(subj, pred, obj, contexts);
	}

	@Override
	public void startUpdate(UpdateContext modify) throws SailException {
		wrappedCon.startUpdate(modify);
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		wrappedCon.addStatement(modify, subj, pred, obj, contexts);
	}

	@Override
	public void removeStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		wrappedCon.removeStatement(modify, subj, pred, obj, contexts);
	}

	@Override
	public void endUpdate(UpdateContext modify) throws SailException {
		wrappedCon.endUpdate(modify);
	}

	@Override
	public void clear(Resource... contexts) throws SailException {
		wrappedCon.clear(contexts);
	}

	@Override
	public CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException {
		return wrappedCon.getNamespaces();
	}

	@Override
	public String getNamespace(String prefix) throws SailException {
		return wrappedCon.getNamespace(prefix);
	}

	@Override
	public void setNamespace(String prefix, String name) throws SailException {
		wrappedCon.setNamespace(prefix, name);
	}

	@Override
	public void removeNamespace(String prefix) throws SailException {
		wrappedCon.removeNamespace(prefix);
	}

	@Override
	public void clearNamespaces() throws SailException {
		wrappedCon.clearNamespaces();
	}

	@Override
	public boolean pendingRemovals() {
		return false;
	}

	@Override
	public Explanation explain(Explanation.Level level, TupleExpr tupleExpr, Dataset dataset,
			BindingSet bindings, boolean includeInferred, int timeoutSeconds) {
		return wrappedCon.explain(level, tupleExpr, dataset, bindings, includeInferred, timeoutSeconds);
	}

	@Override
	public void begin() throws SailException {
		wrappedCon.begin();
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {
		wrappedCon.begin(level);
	}

	@Override
	public void setTransactionSettings(TransactionSetting... settings) {
		wrappedCon.setTransactionSettings(settings);
	}

	@Override
	public void flush() throws SailException {
		wrappedCon.flush();
	}

	@Override
	public void prepare() throws SailException {
		wrappedCon.prepare();
	}

	@Override
	public boolean isActive() throws UnknownSailTransactionStateException {
		return wrappedCon.isActive();
	}

	@Override
	public boolean supportsConcurrentReads() {
		if (wrappedCon instanceof ThreadSafetyAware)
			return ((ThreadSafetyAware) wrappedCon).supportsConcurrentReads();
		return false;
	}
}
