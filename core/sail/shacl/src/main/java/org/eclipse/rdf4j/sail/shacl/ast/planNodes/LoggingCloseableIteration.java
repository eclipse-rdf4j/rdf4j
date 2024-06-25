/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

public abstract class LoggingCloseableIteration implements CloseableIteration<ValidationTuple> {

	private final ValidationExecutionLogger validationExecutionLogger;
	private final PlanNode planNode;
	private boolean closed;
	private boolean initialized;

	public LoggingCloseableIteration(PlanNode planNode, ValidationExecutionLogger validationExecutionLogger) {
		this.planNode = planNode;
		this.validationExecutionLogger = validationExecutionLogger;
	}

	@Override
	public final ValidationTuple next() throws SailException {
		assert initialized;

		ValidationTuple tuple = loggingNext();

		if (validationExecutionLogger.isEnabled()) {
			validationExecutionLogger.log(planNode.depth(), planNode.getClass().getSimpleName() + ".next()", tuple,
					planNode, planNode.getId(), null);
		}
		return tuple;
	}

	@Override
	public final boolean hasNext() throws SailException {
		if (closed) {
			return false;
		}

		if (!initialized) {
			initialized = true;
			init();
		}

		boolean hasNext = localHasNext();

		if (!hasNext) {
			assert !localHasNext() : "Iterator was initially empty, but still has more elements! " + this.getClass();
			close();
		}

		return hasNext;
	}

	@Override
	public void close() throws SailException {
		if (!closed) {
			this.closed = true;
			localClose();
		}
	}

	protected abstract void init();

	protected abstract ValidationTuple loggingNext();

	protected abstract boolean localHasNext();

	protected abstract void localClose();

	/**
	 * A default method since the iterators in the ShaclSail don't support remove.
	 *
	 * @throws SailException
	 */
	@Override
	public void remove() throws SailException {
		throw new UnsupportedOperationException();
	}

	public boolean isClosed() {
		return closed;
	}

}
