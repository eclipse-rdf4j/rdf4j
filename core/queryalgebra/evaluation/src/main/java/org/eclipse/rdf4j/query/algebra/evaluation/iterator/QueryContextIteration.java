/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryContext;

/**
 * Makes a {@link QueryContext} available during iteration.
 */
public class QueryContextIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException> {

	private final CloseableIteration<? extends BindingSet, QueryEvaluationException> iter;

	private final QueryContext queryContext;

	public QueryContextIteration(CloseableIteration<? extends BindingSet, QueryEvaluationException> iter,
			QueryContext queryContext) {
		this.iter = iter;
		this.queryContext = queryContext;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		if (isClosed()) {
			return false;
		}
		queryContext.begin();
		try {
			return iter.hasNext();
		} finally {
			queryContext.end();
		}
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		queryContext.begin();
		try {
			return iter.next();
		} finally {
			queryContext.end();
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		queryContext.begin();
		try {
			iter.remove();
		} finally {
			queryContext.end();
		}
	}

	@Override
	public void handleClose() throws QueryEvaluationException {
		try {
			super.handleClose();
		} finally {
			queryContext.begin();
			try {
				iter.close();
			} finally {
				queryContext.end();
			}
		}
	}
}
