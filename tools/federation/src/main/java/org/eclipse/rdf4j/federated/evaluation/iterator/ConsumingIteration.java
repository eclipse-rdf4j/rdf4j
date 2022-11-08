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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * A specialized {@link CloseableIteration} that prefetches a certain number of results in a batch like manner.
 * <p>
 * This implementation can be used to avoid blocking behavior in HTTP connection streams, i.e. to process results in
 * memory and close the underlying HTTP stream.
 *
 * @author Andreas Schwarte
 */
public class ConsumingIteration implements CloseableIteration<BindingSet, QueryEvaluationException> {

	private final CloseableIteration<BindingSet, QueryEvaluationException> innerIter;
	private final int max;
	private final ConcurrentLinkedQueue<BindingSet> prefetched = new ConcurrentLinkedQueue<>();

	private CompletableFuture<Integer> future;

	private Status initialized = Status.uninitialized;

	/**
	 * @param innerIter iteration to be consumed
	 * @param max       the maximum number of results to be consumed.
	 */
	public ConsumingIteration(CloseableIteration<BindingSet, QueryEvaluationException> innerIter, int max) {
		this.innerIter = innerIter;
		this.max = max;
	}

	private void initialize() {
		switch (initialized) {

		case uninitialized:
			initialized = Status.retrievedFirstElement;
			if (innerIter.hasNext()) {
				prefetched.add(innerIter.next());
			} else {
				throw new NoSuchElementException();
			}
			break;

		case retrievedFirstElement:
			initialized = Status.fullyInitialized;
			prefetch();
			break;
		}
	}

	private void prefetch() {
		waitForBackgroundFetching();

		if (prefetched.isEmpty()) {
			if (innerIter.hasNext()) {
				prefetched.add(innerIter.next());
			} else {
				throw new NoSuchElementException();
			}

			assert future == null;

			future = CompletableFuture.supplyAsync(() -> {
				int i = 0;
				try {
					for (; i < max + 1 && innerIter.hasNext(); i++) {
						if (Thread.currentThread().isInterrupted()) {
							throw new QueryEvaluationException("ConsumingIteration was interrupted");
						}
						prefetched.add(innerIter.next());
					}

					if (!innerIter.hasNext()) {
						innerIter.close();
					}
				} catch (Throwable t) {
					innerIter.close();
					throw t;
				}

				return i;
			});
		}

	}

	private void waitForBackgroundFetching() {
		while (future != null && prefetched.isEmpty()) {
			try {
				Integer integer = future.get(10, TimeUnit.MILLISECONDS);
				future = null;
				assert integer != 0 || prefetched.isEmpty();
			} catch (InterruptedException e) {
				future.cancel(true);
				Thread.currentThread().interrupt();
				throw new QueryEvaluationException(e);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException) {
					throw ((RuntimeException) cause);
				}
				if (cause instanceof Error) {
					throw ((Error) cause);
				}
				throw new QueryEvaluationException(cause);
			} catch (TimeoutException ignored) {
				if (Thread.currentThread().isInterrupted()) {
					future.cancel(true);
					throw new QueryEvaluationException("ConsumingIteration was interrupted");
				}
			}
		}
	}

	@Override
	public boolean hasNext() {
		if (!prefetched.isEmpty()) {
			return true;
		}

		waitForBackgroundFetching();

		if (!prefetched.isEmpty() || innerIter.hasNext()) {
			return true;
		} else {
			innerIter.close();
			return false;
		}
	}

	@Override
	public BindingSet next() {
		if (initialized != Status.fullyInitialized) {
			initialize();
		} else if (prefetched.isEmpty()) {
			prefetch();
		}

		if (!prefetched.isEmpty()) {
			return prefetched.remove();
		}

		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
		try {
			if (future != null) {
				future.cancel(true);
			}
		} catch (Throwable ignored) {
			// when cancelling the background fetching we don't really care if there is an exception
		} finally {
			try {
				innerIter.close();
			} finally {
				prefetched.clear();
			}
		}

	}

	private enum Status {
		uninitialized,
		retrievedFirstElement,
		fullyInitialized;
	}
}
