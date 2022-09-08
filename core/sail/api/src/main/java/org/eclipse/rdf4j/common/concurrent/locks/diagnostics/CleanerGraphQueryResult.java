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
import java.util.Map;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InternalUseOnly
public class CleanerGraphQueryResult implements GraphQueryResult {

	private static final Logger logger = LoggerFactory.getLogger(CleanerGraphQueryResult.class);

	private final GraphQueryResult delegate;
	private final Cleaner.Cleanable cleanable;
	private final CleanableState state;

	public CleanerGraphQueryResult(GraphQueryResult delegate, ConcurrentCleaner cleaner) {
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
	public Statement next() {
		return delegate.next();
	}

	@Override
	public void remove() {
		delegate.remove();
	}

	@Override
	public Map<String, String> getNamespaces() throws QueryEvaluationException {
		return delegate.getNamespaces();
	}

	private final static class CleanableState implements Runnable {

		private final GraphQueryResult iteration;
		private boolean closed = false;

		public CleanableState(GraphQueryResult iteration) {
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
