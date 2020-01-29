/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.concurrent;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParallelTaskBase<T> implements ParallelTask<T> {

	private static final Logger _log = LoggerFactory.getLogger(ParallelExecutorBase.class);

	protected Future<?> scheduledFuture;

	@Override
	public void cancel() {
		if (scheduledFuture != null) {
			if (scheduledFuture.isDone()) {
				_log.trace("Task is already done: " + toString());
			} else {
				_log.debug("Attempting to cancel task " + toString());
				boolean successfullyCanceled = scheduledFuture.cancel(true);
				if (!successfullyCanceled) {
					_log.debug("Task " + toString() + " could not be cancelled properly.");
				}
			}
		}
	}

	public void setScheduledFuture(Future<?> future) {
		this.scheduledFuture = future;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (Query: " + getQueryInfo().getQueryID() + ")";
	}
}
