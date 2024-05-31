/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.function.BooleanSupplier;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;

public class StopableIteration<T> implements CloseableIteration<T> {

	private static class StoppedQueryEvaluationException extends QueryEvaluationException {

		private static final long serialVersionUID = 1L;

		public StoppedQueryEvaluationException() {
			super("Query evaluation was stopped from an external context");
		}

	}

	private volatile boolean closed;

	private final CloseableIteration<T> wrapped;
	private final BooleanSupplier areWeStopped;

	public StopableIteration(CloseableIteration<T> wrapped, BooleanSupplier areWeStopped) {
		super();
		this.wrapped = wrapped;
		this.areWeStopped = areWeStopped;
	}

	@Override
	public boolean hasNext() {
		if (areWeStopped.getAsBoolean()) {
			if (!closed) {
				close();
			}
			return false;
		} else
			return wrapped.hasNext();
	}

	@Override
	public T next() {
		if (areWeStopped.getAsBoolean()) {
			if (!closed) {
				close();
			}
			throw new StoppedQueryEvaluationException();
		}
		try {
			return wrapped.next();
		} catch (StoppedQueryEvaluationException e) {
			close();
			throw e;
		}
	}

	@Override
	public void close() {
		closed = true;
		wrapped.close();
	}

}
