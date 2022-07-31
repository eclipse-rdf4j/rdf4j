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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * A specialized {@link CloseableIteration} that allows repetitive iterations after resetting the cursor using
 * {@link #resetCursor()}.
 * <p>
 * Note that the inner iteration is lazily consumed.
 * </p>
 *
 * @author Andreas Schwarte
 *
 */
public class LazyMutableClosableIteration implements CloseableIteration<BindingSet, QueryEvaluationException> {

	protected final CloseableIteration<BindingSet, QueryEvaluationException> inner;

	protected List<BindingSet> consumed = new ArrayList<>();

	/**
	 * the cursor index, is used after the inner iteration is fully consumed
	 */
	protected volatile int cursorIdx = -1;

	public LazyMutableClosableIteration(CloseableIteration<BindingSet, QueryEvaluationException> inner) {
		super();
		this.inner = inner;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		if (cursorIdx == -1) {
			return inner.hasNext();
		}
		if (cursorIdx >= consumed.size()) {
			return inner.hasNext();
		}
		return cursorIdx < consumed.size();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		if (cursorIdx == -1 || cursorIdx >= consumed.size()) {
			BindingSet next = inner.next();
			consumed.add(next);
			return next;
		}
		return consumed.get(cursorIdx++);
	}

	@Override
	public void remove() throws QueryEvaluationException {
		throw new UnsupportedOperationException("Removal not supported.");
	}

	@Override
	public void close() throws QueryEvaluationException {
		inner.close();
	}

	/**
	 * Reset the cursor to read from the already consumed bindings.
	 */
	public void resetCursor() {
		cursorIdx = 0;
	}
}
