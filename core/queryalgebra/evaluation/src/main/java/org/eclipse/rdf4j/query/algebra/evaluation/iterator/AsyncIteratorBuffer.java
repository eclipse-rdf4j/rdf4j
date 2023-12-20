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

import static java.util.concurrent.ForkJoinPool.commonPool;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
public class AsyncIteratorBuffer extends LookAheadIteration<BindingSet> {

	private final CloseableIteration<BindingSet> iteration;
	private final ConcurrentLinkedQueue<BindingSet> queue = new ConcurrentLinkedQueue<>();

	private Future<ArrayDeque<BindingSet>> future;

	public AsyncIteratorBuffer(CloseableIteration<BindingSet> iteration)
			throws QueryEvaluationException {
		this.iteration = iteration;
	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep iterationPrepared, BindingSet bindings,
			QueryEvaluationContext context) {
		CloseableIteration<BindingSet> iter = iterationPrepared.evaluate(bindings);
		if (iter == QueryEvaluationStep.EMPTY_ITERATION) {
			return iter;
		}

		return new AsyncIteratorBuffer(iter);
	}

	ArrayDeque<BindingSet> nextBuffer;
	BindingSet next;

	void calculateNext() {
		if (next != null) {
			return;
		}

		if (future == null) {
			try {
				async();
			} catch (ExecutionException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		next = queue.poll();

		if (next != null) {
			return;
		}

		while (!(future.isDone() || future.isCancelled()) && queue.isEmpty()) {
			Thread.onSpinWait();
		}

		next = queue.poll();

	}

	private void async() throws ExecutionException, InterruptedException {

		future = commonPool().submit(() -> {
			while (iteration.hasNext()) {
				queue.add(iteration.next());
			}
			return null;
		});

	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		calculateNext();
		BindingSet temp = next;
		next = null;
		return temp;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			if (future != null) {
				future.cancel(true);
			}
		} finally {
			iteration.close();
		}

	}

}
