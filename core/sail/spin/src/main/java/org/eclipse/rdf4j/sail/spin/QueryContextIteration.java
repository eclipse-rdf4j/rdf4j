/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;
import org.eclipse.rdf4j.spin.QueryContext;

public class QueryContextIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException> {

	private final CloseableIteration<? extends BindingSet, QueryEvaluationException> iter;

	private final QueryPreparer queryPreparer;

	public QueryContextIteration(CloseableIteration<? extends BindingSet, QueryEvaluationException> iter,
			QueryPreparer queryPreparer)
	{
		this.iter = iter;
		this.queryPreparer = queryPreparer;
	}

	@Override
	public boolean hasNext()
		throws QueryEvaluationException
	{
		if (isClosed()) {
			return false;
		}
		QueryContext qctx = QueryContext.begin(queryPreparer);
		try {
			return iter.hasNext();
		}
		finally {
			qctx.end();
		}
	}

	@Override
	public BindingSet next()
		throws QueryEvaluationException
	{
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		QueryContext qctx = QueryContext.begin(queryPreparer);
		try {
			return iter.next();
		}
		finally {
			qctx.end();
		}
	}

	@Override
	public void remove()
		throws QueryEvaluationException
	{
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		QueryContext qctx = QueryContext.begin(queryPreparer);
		try {
			iter.remove();
		}
		finally {
			qctx.end();
		}
	}

	@Override
	public void handleClose()
		throws QueryEvaluationException
	{
		try {
			super.handleClose();
		}
		finally {
			QueryContext qctx = QueryContext.begin(queryPreparer);
			try {
				iter.close();
			}
			finally {
				qctx.end();
			}
		}
	}
}
