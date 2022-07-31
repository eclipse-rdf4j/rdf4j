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

import org.eclipse.rdf4j.federated.algebra.PassThroughTupleExpr;
import org.eclipse.rdf4j.federated.repository.FedXRepositoryConnection;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.repository.sail.SailTupleQuery;

/**
 * Abstraction of a {@link SailTupleQuery} which takes care for tracking the
 * {@link FedXRepositoryConnection#BINDING_ORIGINAL_MAX_EXECUTION_TIME} during evaluation.
 *
 * All methods are delegated to the actual {@link SailTupleQuery}.
 *
 *
 * @author Andreas Schwarte
 *
 */
public class FedXTupleQuery extends SailTupleQuery {

	protected final SailTupleQuery delegate;

	public FedXTupleQuery(
			SailTupleQuery delegate) {
		super(delegate.getParsedQuery(), null);
		this.delegate = delegate;
	}

	@Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		FedXUtil.applyQueryBindings(this);
		return delegate.evaluate();
	}

	@Override
	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {

		// attach the handler to the query to allow pass through of results
		// for single source queries
		TupleExpr tupleExpr = getParsedQuery().getTupleExpr();
		PassThroughTupleExpr passThroughTupleExpr = new PassThroughTupleExpr(tupleExpr,
				handler);
		delegate.getParsedQuery().setTupleExpr(passThroughTupleExpr);

		FedXUtil.applyQueryBindings(this);
		TupleQueryResult tqr = delegate.evaluate();

		if (!passThroughTupleExpr.isPassedThrough()) {
			// if the result is not passed through to the handler directly,
			// we need to make sure to report the result. Note that only
			// SingleSourceQuery instances can be passed through
			QueryResults.report(tqr, handler);
		} else {
			// to be absolutely sure that everything is closed
			tqr.close();
		}
	}

	/*
	 * DELEGATE TO ACTUAL SailTupleQuery
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
	public ParsedTupleQuery getParsedQuery() {
		return delegate.getParsedQuery();
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
