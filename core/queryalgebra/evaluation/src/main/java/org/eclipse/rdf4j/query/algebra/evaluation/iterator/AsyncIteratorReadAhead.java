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

import java.util.ArrayDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class AsyncIteratorReadAhead extends LookAheadIteration<BindingSet> {

	private final int READ_AHEAD_LIMIT = 1024 * 1024 * 16;

	private final ExecutorService executorService;
	private int readAhead = 4;

	private final CloseableIteration<BindingSet> iteration;

	private Future<ArrayDeque<BindingSet>> future;

	public AsyncIteratorReadAhead(CloseableIteration<BindingSet> iteration)
			throws QueryEvaluationException {
		this.iteration = iteration;
		this.executorService = Executors.newSingleThreadExecutor();

	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep iterationPrepared, BindingSet bindings,
			QueryEvaluationContext context) {
		CloseableIteration<BindingSet> iter = iterationPrepared.evaluate(bindings);
		if (iter == QueryEvaluationStep.EMPTY_ITERATION) {
			return iter;
		}

		return new AsyncIteratorReadAhead(iter);
	}

	ArrayDeque<BindingSet> nextBuffer;
	BindingSet next;

	void calculateNext() {
		if (next != null) {
			return;
		}

		if (nextBuffer != null && !nextBuffer.isEmpty()) {
			next = nextBuffer.removeFirst();
			return;
		}
		try {
			nextBuffer = async();
			if (nextBuffer != null && !nextBuffer.isEmpty()) {
				next = nextBuffer.removeFirst();
				return;
			}
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private ArrayDeque<BindingSet> async() throws ExecutionException, InterruptedException {
		ArrayDeque<BindingSet> ret = null;

		if (future != null) {
			ret = future.get();
			future = null;
		} else {
			if (iteration.hasNext()) {
				ret = new ArrayDeque<>(1);
				ret.add(iteration.next());
			} else {
				return null;
			}
		}

		if (readAhead < READ_AHEAD_LIMIT) {
			readAhead *= 2;
		}

		ArrayDeque<BindingSet> buffer;
		if (nextBuffer != null) {
			nextBuffer.clear();
			buffer = nextBuffer;
		} else {
			buffer = new ArrayDeque<>();
		}

		future = executorService.submit(() -> {
			int currentReadAhead = readAhead;

			for (int i = 0; i < currentReadAhead && iteration.hasNext(); i++) {
				buffer.addLast(iteration.next());
			}

			if (buffer.isEmpty()) {
				return null;
			}
			return buffer;
		});

		return ret;
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
			try {
				executorService.shutdownNow();
			} finally {
				iteration.close();
			}
		}

	}

}
