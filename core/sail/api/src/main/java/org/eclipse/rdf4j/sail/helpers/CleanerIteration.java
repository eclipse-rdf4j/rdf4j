/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.helpers;

import java.lang.ref.Cleaner;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.ConcurrentCleaner;
import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CleanerIteration<E, X extends Exception> implements CloseableIteration<E, X> {

	private static final Logger logger = LoggerFactory.getLogger(CleanerIteration.class);

	private final AbstractCloseableIteration<E, X> delegate;
	private final Cleaner.Cleanable cleanable;

	public CleanerIteration(AbstractCloseableIteration<E, X> delegate, ConcurrentCleaner cleaner) {
		this.delegate = delegate;
		this.cleanable = cleaner.register(this, new CleanableState(delegate));
	}

	@Override
	public void close() throws X {
		delegate.close();
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

	static class CleanableState implements Runnable {

		private final AbstractCloseableIteration iteration;

		public CleanableState(AbstractCloseableIteration iteration) {
			this.iteration = iteration;
		}

		@Override
		public void run() {
			if (!iteration.isClosed()) {
				try {
					logger.warn(
							"Forced closing of unclosed iteration. Set the system property 'org.eclipse.rdf4j.repository.debug' to 'true' to get stack traces.");
					iteration.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
