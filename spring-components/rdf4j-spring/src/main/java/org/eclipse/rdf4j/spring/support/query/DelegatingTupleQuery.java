/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.support.query;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.explanation.Explanation;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public abstract class DelegatingTupleQuery implements TupleQuery {
	private final TupleQuery delegate;

	public DelegatingTupleQuery(TupleQuery delegate) {
		this.delegate = delegate;
	}

	protected TupleQuery getDelegate() {
		return delegate;
	}

	@Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		return delegate.evaluate();
	}

	@Override
	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {
		delegate.evaluate(handler);
	}

	@Override
	@Deprecated
	public void setMaxQueryTime(int maxQueryTime) {
		delegate.setMaxQueryTime(maxQueryTime);
	}

	@Override
	@Deprecated
	public int getMaxQueryTime() {
		return delegate.getMaxQueryTime();
	}

	@Override
	@Experimental
	public Explanation explain(Explanation.Level level) {
		return delegate.explain(level);
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
}
