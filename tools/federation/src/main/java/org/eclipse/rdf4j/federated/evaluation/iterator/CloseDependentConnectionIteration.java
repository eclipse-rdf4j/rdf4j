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
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * A wrapping iteration that attempts to close the dependent {@link RepositoryConnection} after consumption.
 *
 * @author Andreas Schwarte
 */
public class CloseDependentConnectionIteration<T> extends AbstractCloseableIteration<T> {

	protected final CloseableIteration<T> inner;
	protected final RepositoryConnection dependentConn;

	public CloseDependentConnectionIteration(CloseableIteration<T> inner,
			RepositoryConnection dependentConn) {
		this.inner = inner;
		this.dependentConn = dependentConn;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		try {
			if (Thread.interrupted()) {
				Thread.currentThread().interrupt();
				close();
				return false;
			}

			boolean res = inner.hasNext();
			if (!res) {
				close();
			}
			return res;
		} catch (Throwable t) {
			close();
			throw t;
		}
	}

	@Override
	public T next() throws QueryEvaluationException {
		try {
			return inner.next();
		} catch (Throwable t) {
			close();
			throw t;
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {
		try {
			inner.remove();
		} catch (Throwable t) {
			close();
			throw t;
		}
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			inner.close();
		} finally {
			dependentConn.close();
		}
	}

}
