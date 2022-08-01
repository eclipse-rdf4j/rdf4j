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

import java.util.concurrent.Future;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTask;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * A wrapping iteration that attempts to close all running scheduled {@link Future}s for the given query evaluation.
 * <p>
 * This is required for instance if the resulting iteration is not fully consumed.
 * </p>
 *
 * @author Andreas Schwarte
 * @see QueryInfo#close()
 * @see ParallelTask#cancel()
 */
public class StopRemainingExecutionsOnCloseIteration
		extends AbstractCloseableIteration<BindingSet, QueryEvaluationException> {

	protected final CloseableIteration<? extends BindingSet, QueryEvaluationException> inner;
	protected final QueryInfo queryInfo;

	public StopRemainingExecutionsOnCloseIteration(
			CloseableIteration<? extends BindingSet, QueryEvaluationException> inner, QueryInfo queryInfo) {
		super();
		this.inner = inner;
		this.queryInfo = queryInfo;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		return inner.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		return inner.next();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		inner.remove();
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			super.handleClose();
		} finally {
			try {
				inner.close();
			} finally {
				// make sure to close all scheduled / running parallel executions
				// (e.g. if the query result is not fully consumed)
				queryInfo.close();
			}

		}
	}

}
