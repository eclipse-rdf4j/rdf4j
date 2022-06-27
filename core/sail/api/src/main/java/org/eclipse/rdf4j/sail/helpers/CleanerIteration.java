/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.helpers;

import java.lang.ref.Cleaner;

import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.ConcurrentCleaner;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CleanerIteration<E, X extends Exception> implements CloseableIteration<E, X> {

	private static final Logger logger = LoggerFactory.getLogger(CleanerIteration.class);

	private final CloseableIteration<E, X> delegate;
	private final Cleaner.Cleanable cleanable;
	private final CleanableState<E, X> state;

	public CleanerIteration(CloseableIteration<E, X> delegate, ConcurrentCleaner cleaner) {
		this.delegate = delegate;
		this.state = new CleanableState<>(delegate);
		this.cleanable = cleaner.register(this, state);
	}

	@Override
	public void close() throws X {
		state.close();
		cleanable.clean();
	}

	@Override
	public boolean hasNext() throws X {
		return delegate.hasNext();
	}

	@Override
	public E next() throws X {
		return delegate.next();
	}

	@Override
	public void remove() throws X {
		delegate.remove();
	}

	private final static class CleanableState<E, X extends Exception> implements Runnable {

		private final CloseableIteration<E, X> iteration;
		private boolean closed = false;

		public CleanableState(CloseableIteration<E, X> iteration) {
			this.iteration = iteration;
		}

		@Override
		public void run() {
			if (!closed) {
				try {
					logger.warn(
							"Forced closing of unclosed iteration. Set the system property 'org.eclipse.rdf4j.repository.debug' to 'true' to get stack traces.");
					iteration.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		public void close() throws X {
			closed = true;
			iteration.close();
		}
	}

}
