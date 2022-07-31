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
package org.eclipse.rdf4j.federated.evaluation.union;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.TripleSource;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTaskBase;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * A task implementation representing a prepared union, i.e. the prepared query is executed on the provided triple
 * source.
 *
 * @author Andreas Schwarte
 */
public class ParallelPreparedUnionTask extends ParallelTaskBase<BindingSet> {

	protected final Endpoint endpoint;
	protected final String preparedQuery;
	protected final BindingSet bindings;
	protected final ParallelExecutor<BindingSet> unionControl;
	protected final FilterValueExpr filterExpr;
	protected final QueryInfo queryInfo;

	public ParallelPreparedUnionTask(ParallelExecutor<BindingSet> unionControl, String preparedQuery, Endpoint endpoint,
			BindingSet bindings, FilterValueExpr filterExpr, QueryInfo queryInfo) {
		this.endpoint = endpoint;
		this.preparedQuery = preparedQuery;
		this.bindings = bindings;
		this.unionControl = unionControl;
		this.filterExpr = filterExpr;
		this.queryInfo = queryInfo;
	}

	@Override
	protected CloseableIteration<BindingSet, QueryEvaluationException> performTaskInternal() throws Exception {
		TripleSource tripleSource = endpoint.getTripleSource();
		return tripleSource.getStatements(preparedQuery, bindings, filterExpr, queryInfo);
	}

	@Override
	public ParallelExecutor<BindingSet> getControl() {
		return unionControl;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " @" + endpoint.getId() + ": " + preparedQuery;
	}
}
