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
package org.eclipse.rdf4j.federated.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.QueryManager;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * An iteration which wraps the final result and in case of exceptions aborts query evaluation for the corresponding
 * query in fedx (potentially subqueries are still running, and jobs are scheduled).
 *
 * If some external component calls close() on this iteration AND if the corresponding query is still running, the query
 * is aborted within FedX. An example case would be Sesame's QueryInteruptIterations, which is used to enforce
 * maxQueryTime.
 *
 * If the query is finished, the FederationManager is notified that the query is done, and the query is removed from the
 * set of running queries.
 *
 * @author Andreas Schwarte
 *
 */
public class QueryResultIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException> {

	// TODO apply this class and provide test case

	protected final CloseableIteration<BindingSet, QueryEvaluationException> inner;
	protected final QueryInfo queryInfo;
	protected final QueryManager qm;

	public QueryResultIteration(
			CloseableIteration<BindingSet, QueryEvaluationException> inner, QueryInfo queryInfo) {
		super();
		this.inner = inner;
		this.queryInfo = queryInfo;
		this.qm = queryInfo.getFederationContext().getQueryManager();
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		if (inner.hasNext()) {
			return true;
		} else {
			// inform the query manager that this query is done
			qm.finishQuery(queryInfo);
			return false;
		}
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		try {
			BindingSet next = inner.next();
			if (next == null) {
				qm.finishQuery(queryInfo);
			}
			return next;
		} catch (QueryEvaluationException e) {
			abortQuery();
			throw e;
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {
		inner.remove();
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			inner.close();
		} finally {
			try {
				abortQuery();
			} finally {
				queryInfo.close();
			}
		}
	}

	/**
	 * Abort the query in the schedulers if it is still running.
	 */
	protected void abortQuery() {
		if (qm.isRunning(queryInfo)) {
			qm.abortQuery(queryInfo);
		}
	}
}
