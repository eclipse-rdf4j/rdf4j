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
package org.eclipse.rdf4j.query.impl;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.TimeLimitIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.parser.ParsedQuery;

/**
 * @author Arjohn Kampman
 */
public abstract class AbstractParserQuery extends AbstractQuery {

	private final ParsedQuery parsedQuery;

	protected static final int DEFAULT_EXPLANATION_EXECUTION_TIMEOUT = 60;

	protected AbstractParserQuery(ParsedQuery parsedQuery) {
		this.parsedQuery = parsedQuery;
	}

	public ParsedQuery getParsedQuery() {
		return parsedQuery;
	}

	protected CloseableIteration<? extends BindingSet> enforceMaxQueryTime(
			CloseableIteration<? extends BindingSet> bindingsIter) {
		if (getMaxExecutionTime() > 0) {
			bindingsIter = new QueryInterruptIteration(bindingsIter, 1000L * getMaxExecutionTime());
		}

		return bindingsIter;
	}

	/**
	 * Gets the "active" dataset for this query. The active dataset is either the dataset that has been specified using
	 * {@link #setDataset(Dataset)} or the dataset that has been specified in the query, where the former takes
	 * precedence over the latter.
	 *
	 * @return The active dataset, or <var>null</var> if there is no dataset.
	 */
	public Dataset getActiveDataset() {
		if (dataset != null) {
			return FallbackDataset.fallback(dataset, parsedQuery.getDataset());
		}

		// No external dataset specified, use query's own dataset (if any)
		return parsedQuery.getDataset();
	}

	@Override
	public String toString() {
		return parsedQuery.toString();
	}

	private static class QueryInterruptIteration extends TimeLimitIteration<BindingSet> {

		public QueryInterruptIteration(CloseableIteration<? extends BindingSet> iter,
				long timeLimit) {
			super(iter, timeLimit);
		}

		@Override
		protected void throwInterruptedException() throws QueryEvaluationException {
			throw new QueryInterruptedException("Query evaluation took too long");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		AbstractParserQuery that = (AbstractParserQuery) o;

		return parsedQuery.equals(that.parsedQuery);
	}

	@Override
	public int hashCode() {
		return parsedQuery.hashCode();
	}
}
