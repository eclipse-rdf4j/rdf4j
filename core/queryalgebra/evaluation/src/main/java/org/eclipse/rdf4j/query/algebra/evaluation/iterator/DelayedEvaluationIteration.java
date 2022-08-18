/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;

/**
 * Utility class that removes code duplication and makes a precompiled QueryEvaluationStep available as an iteration
 * that may be created and used later.
 */
public class DelayedEvaluationIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException> {

	private final QueryEvaluationStep arg;
	private final BindingSet bs;
	private CloseableIteration<BindingSet, QueryEvaluationException> iter;

	public DelayedEvaluationIteration(QueryEvaluationStep arg, BindingSet bs) {
		super();
		this.arg = arg;
		this.bs = bs;
	}

	protected CloseableIteration<BindingSet, QueryEvaluationException> createIteration()
			throws QueryEvaluationException {
		return arg.evaluate(bs);
	}

	/**
	 * Calls the <var>hasNext</var> method of the underlying iteration.
	 */
	@Override
	public boolean hasNext() throws QueryEvaluationException {
		if (isClosed()) {
			return false;
		}

		initialise();

		return iter.hasNext();
	}

	/**
	 * Calls the <var>next</var> method of the underlying iteration.
	 */
	@Override
	public BindingSet next() throws QueryEvaluationException {
		if (isClosed()) {
			throw new NoSuchElementException("Iteration has been closed");
		}
		initialise();

		return iter.next();
	}

	private void initialise() throws QueryEvaluationException {
		if (iter == null) {
			// Underlying iterator has not yet been initialized
			iter = createIteration();
		}
	}

	/**
	 * Calls the <var>remove</var> method of the underlying iteration.
	 */
	@Override
	public void remove() throws QueryEvaluationException {
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		if (iter == null) {
			throw new IllegalStateException("Underlying iteration was null");
		}

		iter.remove();
	}

	/**
	 * Closes this iteration as well as the underlying iteration if it has already been created and happens to be a
	 * {@link CloseableIteration}.
	 */
	@Override
	protected final void handleClose() throws QueryEvaluationException {
		if (iter != null) {
			iter.close();
		}
	}
}
