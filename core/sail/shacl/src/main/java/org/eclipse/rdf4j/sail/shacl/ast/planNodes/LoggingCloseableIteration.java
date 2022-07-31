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

public abstract class LoggingCloseableIteration implements CloseableIteration<ValidationTuple, SailException> {

	private final ValidationExecutionLogger validationExecutionLogger;
	private final PlanNode planNode;
	private boolean empty = false;
	private boolean closed;

	public LoggingCloseableIteration(PlanNode planNode, ValidationExecutionLogger validationExecutionLogger) {
		this.planNode = planNode;
		this.validationExecutionLogger = validationExecutionLogger;
	}

	@Override
	public final ValidationTuple next() throws SailException {

		ValidationTuple tuple = loggingNext();

		if (validationExecutionLogger.isEnabled()) {
			validationExecutionLogger.log(planNode.depth(), planNode.getClass().getSimpleName() + ".next()", tuple,
					planNode, planNode.getId(), null);
		}
		return tuple;
	}

	@Override
	public final boolean hasNext() throws SailException {
		if (empty) {
			return false;
		}

		boolean hasNext = localHasNext();

		if (!hasNext) {
			empty = true;
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

	protected abstract ValidationTuple loggingNext() throws SailException;

	protected abstract boolean localHasNext() throws SailException;

	protected abstract void localClose() throws SailException;

	/**
	 * A default method since the iterators in the ShaclSail don't support remove.
	 *
	 * @throws SailException
	 */
	@Override
	public void remove() throws SailException {
		throw new UnsupportedOperationException();
	}
}
