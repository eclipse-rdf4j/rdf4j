/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapping iteration that attempts to close the dependent {@link RepositoryConnection} after consumption.
 *
 * @author Andreas Schwarte
 *
 */
public class CloseDependentConnectionIteration<T>
		extends AbstractCloseableIteration<T, QueryEvaluationException> {

	private static final Logger log = LoggerFactory.getLogger(CloseDependentConnectionIteration.class);

	protected final CloseableIteration<T, QueryEvaluationException> inner;
	protected final RepositoryConnection dependentConn;

	public CloseDependentConnectionIteration(
			CloseableIteration<T, QueryEvaluationException> inner,
			RepositoryConnection dependentConn) {
		super();
		this.inner = inner;
		this.dependentConn = dependentConn;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		try {
			boolean res = inner.hasNext();
			if (!res) {
				try {
					dependentConn.close();
				} catch (Throwable ignore) {
					log.trace("Failed to close dependent connection:", ignore);
				}
			}
			return res;
		} catch (Throwable t) {
			dependentConn.close();
			throw t;
		}
	}

	@Override
	public T next() throws QueryEvaluationException {
		try {
			return inner.next();
		} catch (Throwable t) {
			dependentConn.close();
			throw t;
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
				super.handleClose();
			} finally {
				dependentConn.close();
			}
		}
	}

}
