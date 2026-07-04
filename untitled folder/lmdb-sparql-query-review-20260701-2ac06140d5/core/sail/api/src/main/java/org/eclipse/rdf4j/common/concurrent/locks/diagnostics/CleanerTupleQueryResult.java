/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks.diagnostics;

import java.lang.ref.Cleaner;
import java.util.List;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InternalUseOnly
public class CleanerTupleQueryResult implements TupleQueryResult {

	private static final Logger logger = LoggerFactory.getLogger(CleanerTupleQueryResult.class);

	private final TupleQueryResult delegate;
	private final Cleaner.Cleanable cleanable;
	private final CleanableState state;

	public CleanerTupleQueryResult(TupleQueryResult delegate, ConcurrentCleaner cleaner) {
		this.delegate = delegate;
		this.state = new CleanableState(delegate);
		this.cleanable = cleaner.register(this, state);
	}

	@Override
	public void close() {
		state.close();
		cleanable.clean();
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public BindingSet next() {
		return delegate.next();
	}

	@Override
	public void remove() {
		delegate.remove();
	}

	@Override
	public List<String> getBindingNames() throws QueryEvaluationException {
		return delegate.getBindingNames();
	}

	private final static class CleanableState implements Runnable {

		private final TupleQueryResult iteration;
		private boolean closed = false;

		public CleanableState(TupleQueryResult iteration) {
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
					if (e instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					throw new RuntimeException(e);
				}
			}
		}

		public void close() {
			closed = true;
			iteration.close();
		}
	}

}
