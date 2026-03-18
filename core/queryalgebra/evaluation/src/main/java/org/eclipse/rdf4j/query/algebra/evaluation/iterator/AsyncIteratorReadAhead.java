/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

/**
 * @author Håvard M. Ottestad
 */
@Experimental
public class AsyncIteratorReadAhead extends LookAheadIteration<BindingSet> {

	private final int readAheadDepth;
	private final Supplier<CloseableIteration<BindingSet>> iterationSupplier;
	private final BindingSet[] batch;
	private CloseableIteration<BindingSet> iteration;
	private int batchIndex;
	private int batchSize;

	public AsyncIteratorReadAhead(CloseableIteration<BindingSet> iteration, int readAheadDepth)
			throws QueryEvaluationException {
		this(() -> Objects.requireNonNull(iteration, "iteration"), readAheadDepth);
	}

	private AsyncIteratorReadAhead(Supplier<CloseableIteration<BindingSet>> iterationSupplier, int readAheadDepth)
			throws QueryEvaluationException {
		if (readAheadDepth <= 0) {
			throw new IllegalArgumentException("readAheadDepth must be > 0");
		}
		this.iterationSupplier = Objects.requireNonNull(iterationSupplier, "iterationSupplier");
		this.readAheadDepth = readAheadDepth;
		this.batch = new BindingSet[readAheadDepth];
	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep iterationPrepared, BindingSet bindings,
			QueryEvaluationContext context) {
		int readAheadDepth = context.getJoinReadAheadDepth();
		if (readAheadDepth <= 0) {
			return iterationPrepared.evaluate(bindings);
		}
		return new AsyncIteratorReadAhead(() -> iterationPrepared.evaluate(bindings), readAheadDepth);
	}

	static CloseableIteration<BindingSet> getInstance(CloseableIteration<BindingSet> iteration, int readAheadDepth) {
		if (iteration == QueryEvaluationStep.EMPTY_ITERATION || readAheadDepth <= 0) {
			return iteration;
		}
		return new AsyncIteratorReadAhead(iteration, readAheadDepth);
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		if (batchIndex < batchSize) {
			return batch[batchIndex++];
		}
		if (!readNextBatch()) {
			return null;
		}
		return batch[batchIndex++];
	}

	private boolean readNextBatch() {
		batchIndex = 0;
		batchSize = 0;
		if (isClosed()) {
			return false;
		}
		CloseableIteration<BindingSet> currentIteration = getOrCreateIteration();
		if (currentIteration == QueryEvaluationStep.EMPTY_ITERATION) {
			return false;
		}
		while (batchSize < readAheadDepth && !isClosed() && currentIteration.hasNext()) {
			batch[batchSize] = currentIteration.next();
			batchSize++;
		}
		return batchSize > 0;
	}

	private CloseableIteration<BindingSet> getOrCreateIteration() {
		CloseableIteration<BindingSet> currentIteration = iteration;
		if (currentIteration == null) {
			currentIteration = Objects.requireNonNull(iterationSupplier.get(), "iteration");
			iteration = currentIteration;
		}
		return currentIteration;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		batchIndex = 0;
		batchSize = 0;
		try {
			if (iteration != null && iteration != QueryEvaluationStep.EMPTY_ITERATION) {
				iteration.close();
			}
		} finally {
			iteration = null;
		}
	}

}
