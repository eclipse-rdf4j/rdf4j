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
package org.eclipse.rdf4j.federated.evaluation.concurrent;

import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.impl.QueueCursor;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized variants of {@link QueueCursor} which avoids converting any exception if it is already of
 * type{@link QueryEvaluationException}.
 *
 * @param <T>
 * @author Andreas Schwarte
 */
public class FedXQueueCursor<T> extends QueueCursor<CloseableIteration<T, QueryEvaluationException>> {

	private static final Logger log = LoggerFactory.getLogger(FedXQueueCursor.class);

	public static <T> FedXQueueCursor<T> create(int capacity, WeakReference<?> callerReference) {
		BlockingQueue<CloseableIteration<T, QueryEvaluationException>> queue = new ArrayBlockingQueue<>(capacity,
				false);
		return new FedXQueueCursor<>(queue, callerReference);
	}

	/**
	 * Reference to the queue such that we can access it in {@link #handleClose()}. This is required to make sure that
	 * we can close the non-consumed iterations from the queue. Note that the private queue of the super class is not
	 * accessible.
	 */
	private final BlockingQueue<CloseableIteration<T, QueryEvaluationException>> queueRef;

	private FedXQueueCursor(BlockingQueue<CloseableIteration<T, QueryEvaluationException>> queue,
			WeakReference<?> callerRef) {
		super(queue, callerRef);
		this.queueRef = queue;
	}

	@Override
	protected QueryEvaluationException convert(Exception e) {
		if (e instanceof QueryEvaluationException) {
			return (QueryEvaluationException) e;
		}
		if (e instanceof InterruptedException) {
			Thread.currentThread().interrupt();
		}
		return super.convert(e);
	}

	@Override
	public void handleClose() throws QueryEvaluationException {

		try {
			Throwable throwable = null;

			// consume all remaining elements from the queue and make sure to close them
			// Note: unfortunately we cannot access "afterLast" of the super class
			// => thus have to make a check whether the polled object is actually a
			// closable iteration
			while (!queueRef.isEmpty()) {
				try {
					Object take = queueRef.poll();
					if (take instanceof CloseableIteration) {
						((CloseableIteration<?, ?>) take).close();
					}
				} catch (Throwable t) {
					if (t instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					if (throwable != null) {
						t.addSuppressed(throwable);
					}
					throwable = t;
				}
			}
			done(); // re-add after-last

			if (throwable != null) {
				if (throwable instanceof RuntimeException) {
					throw ((RuntimeException) throwable);
				}
				if (throwable instanceof Error) {
					throw ((Error) throwable);
				}
				throw new SailException(throwable);
			}
		} finally {
			super.handleClose();
		}
	}

}
