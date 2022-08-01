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
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.TripleSource;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTaskBase;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

/**
 * A task implementation to retrieve statements for a given {@link StatementPattern} using the provided triple source.
 *
 * @author Andreas Schwarte
 */
public class ParallelGetStatementsTask extends ParallelTaskBase<Statement> {

	protected final ParallelExecutor<Statement> unionControl;
	protected final Endpoint endpoint;
	protected final Resource subj;

	protected final IRI pred;
	protected final Value obj;
	protected final QueryInfo queryInfo;
	protected Resource[] contexts;

	public ParallelGetStatementsTask(ParallelExecutor<Statement> unionControl,
			Endpoint endpoint,
			Resource subj, IRI pred, Value obj, QueryInfo queryInfo, Resource... contexts) {
		super();
		this.unionControl = unionControl;
		this.endpoint = endpoint;
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.queryInfo = queryInfo;
		this.contexts = contexts;

	}

	@Override
	public ParallelExecutor<Statement> getControl() {
		return unionControl;
	}

	@Override
	protected CloseableIteration<Statement, QueryEvaluationException> performTaskInternal() throws Exception {
		TripleSource tripleSource = endpoint.getTripleSource();
		return tripleSource.getStatements(subj, pred, obj, queryInfo, contexts);
	}
}
