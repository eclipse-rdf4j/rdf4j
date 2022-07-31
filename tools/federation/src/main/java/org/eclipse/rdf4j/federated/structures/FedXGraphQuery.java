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
package org.eclipse.rdf4j.federated.structures;

import org.eclipse.rdf4j.federated.repository.FedXRepositoryConnection;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.repository.sail.SailGraphQuery;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/**
 * Abstraction of a {@link SailGraphQuery} which takes care for tracking the
 * {@link FedXRepositoryConnection#BINDING_ORIGINAL_MAX_EXECUTION_TIME} during evaluation.
 *
 * All methods are delegated to the actual {@link SailGraphQuery}.
 *
 *
 * @author Andreas Schwarte
 *
 */
public class FedXGraphQuery extends SailGraphQuery {

	protected final SailGraphQuery delegate;

	public FedXGraphQuery(SailGraphQuery delegate) {
		super(delegate.getParsedQuery(), null);
		this.delegate = delegate;
	}

	@Override
	public GraphQueryResult evaluate() throws QueryEvaluationException {
		FedXUtil.applyQueryBindings(this);
		return delegate.evaluate();
	}

	@Override
	public void evaluate(RDFHandler handler) throws QueryEvaluationException, RDFHandlerException {
		FedXUtil.applyQueryBindings(this);
		delegate.evaluate(handler);
	}

	/*
	 * DELEGATE TO ACTUAL SailGraphQuery
	 */

	@SuppressWarnings("deprecation")
	@Override
	public void setMaxQueryTime(int maxQueryTime) {
		delegate.setMaxQueryTime(maxQueryTime);
	}

	@SuppressWarnings("deprecation")
	@Override
	public int getMaxQueryTime() {
		return delegate.getMaxQueryTime();
	}

	@Override
	public void setBinding(String name, Value value) {
		delegate.setBinding(name, value);
	}

	@Override
	public void removeBinding(String name) {
		delegate.removeBinding(name);
	}

	@Override
	public void clearBindings() {
		delegate.clearBindings();
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public BindingSet getBindings() {
		return delegate.getBindings();
	}

	@Override
	public void setDataset(Dataset dataset) {
		delegate.setDataset(dataset);
	}

	@Override
	public Dataset getDataset() {
		return delegate.getDataset();
	}

	@Override
	public void setIncludeInferred(boolean includeInferred) {
		delegate.setIncludeInferred(includeInferred);
	}

	@Override
	public Dataset getActiveDataset() {
		return delegate.getActiveDataset();
	}

	@Override
	public ParsedGraphQuery getParsedQuery() {
		return delegate.getParsedQuery();
	}

	@Override
	public boolean getIncludeInferred() {
		return delegate.getIncludeInferred();
	}

	@Override
	public void setMaxExecutionTime(int maxExecutionTimeSeconds) {
		delegate.setMaxExecutionTime(maxExecutionTimeSeconds);
	}

	@Override
	public int getMaxExecutionTime() {
		return delegate.getMaxExecutionTime();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}
}
