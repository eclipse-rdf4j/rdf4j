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

import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters iteration according to specified filterExpr and inserts original bindings into filtered results.
 *
 * @author Andreas Schwarte
 */
public class FilteringInsertBindingsIteration implements CloseableIteration<BindingSet> {

	private static final Logger log = LoggerFactory.getLogger(FilteringInsertBindingsIteration.class);
	private final BindingSet bindings;
	private final FilterValueExpr filterExpr;
	private final FederationEvalStrategy strategy;
	private final CloseableIteration<? extends BindingSet> wrappedIter;
	private BindingSet nextElement;
	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private boolean closed = false;

	public FilteringInsertBindingsIteration(FilterValueExpr filterExpr, BindingSet bindings,
			CloseableIteration<BindingSet> iter, FederationEvalStrategy strategy)
			throws QueryEvaluationException {
		assert iter != null;
		this.wrappedIter = iter;
		this.filterExpr = filterExpr;
		this.strategy = strategy;
		this.bindings = bindings;
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		BindingSet res1;
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		findNextElement();

		BindingSet result = nextElement;

		if (result != null) {
			nextElement = null;
			res1 = result;
		} else {
			close();
			throw new NoSuchElementException("The iteration has been closed.");
		}
		BindingSet next = res1;
		QueryBindingSet res = new QueryBindingSet(bindings.size() + next.size());
		res.addAll(bindings);
		res.addAll(next);
		return res;
	}

	protected boolean accept(BindingSet bindings) throws QueryEvaluationException {
		try {
			return strategy.isTrue(filterExpr, bindings);
		} catch (ValueExprEvaluationException e) {
			log.warn("Failed to evaluate filter expr: " + e.getMessage());
			// failed to evaluate condition
			return false;
		}
	}

	@Override
	public boolean hasNext() {
		if (isClosed()) {
			return false;
		}
		findNextElement();

		boolean result = nextElement != null;
		if (!result) {
			close();
		}
		return result;
	}

	private void findNextElement() {
		if (nextElement != null) {
			return;
		}

		try {
			if (!isClosed()) {
				if (Thread.currentThread().isInterrupted()) {
					close();
					return;
				} else {
					boolean result = wrappedIter.hasNext();
					if (!result) {
						close();
						return;
					}
				}
			}
			while (nextElement == null && wrappedIter.hasNext()) {
				BindingSet result;
				if (Thread.currentThread().isInterrupted()) {
					close();
					return;
				}
				try {
					result = wrappedIter.next();
				} catch (NoSuchElementException e) {
					close();
					throw e;
				}
				BindingSet candidate = result;

				if (accept(candidate)) {
					nextElement = candidate;
				}
			}
		} finally {
			if (isClosed()) {
				nextElement = null;
			}
		}
	}

	/**
	 * Removes the last element that has been returned from the wrapped Iteration.
	 *
	 * @throws UnsupportedOperationException If the wrapped Iteration does not support the <var>remove</var> operation.
	 * @throws IllegalStateException         if the Iteration has been closed, or if {@link #next} has not yet been
	 *                                       called, or {@link #remove} has already been called after the last call to
	 *                                       {@link #next}.
	 */
	@Override
	public void remove() {
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		} else if (Thread.currentThread().isInterrupted()) {
			close();
			throw new IllegalStateException("The iteration has been interrupted.");
		}
		try {
			wrappedIter.remove();
		} catch (IllegalStateException e) {
			close();
			throw e;
		}
	}

	private boolean isClosed() {
		return closed;
	}

	/**
	 * Calls {@link #handleClose()} upon first call and makes sure the resource closures are only executed once.
	 */
	@Override
	public final void close() {
		if (!closed) {
			closed = true;
			wrappedIter.close();
		}
	}
}
