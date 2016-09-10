/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

public class QueryContextIteration implements CloseableIteration<BindingSet, QueryEvaluationException> {

	private final CloseableIteration<? extends BindingSet, QueryEvaluationException> iter;

	private final SearchIndex searchIndex;

	public QueryContextIteration(CloseableIteration<? extends BindingSet, QueryEvaluationException> iter,
			SearchIndex searchIndex)
	{
		this.iter = iter;
		this.searchIndex = searchIndex;
	}

	@Override
	public boolean hasNext()
		throws QueryEvaluationException
	{
		QueryContext qctx = QueryContext.begin(searchIndex);
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
		QueryContext qctx = QueryContext.begin(searchIndex);
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
		QueryContext qctx = QueryContext.begin(searchIndex);
		try {
			iter.remove();
		}
		finally {
			qctx.end();
		}
	}

	@Override
	public void close()
		throws QueryEvaluationException
	{
		QueryContext qctx = QueryContext.begin(searchIndex);
		try {
			iter.close();
		}
		finally {
			qctx.end();
		}
	}
}
