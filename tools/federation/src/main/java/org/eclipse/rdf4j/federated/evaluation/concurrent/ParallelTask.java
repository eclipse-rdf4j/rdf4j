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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Interface for any parallel task that can be performed in Scheduler implementations.
 *
 * @author Andreas Schwarte
 *
 */
public interface ParallelTask<T> {

	CloseableIteration<T, QueryEvaluationException> performTask() throws Exception;

	/**
	 * return the controlling instance, e.g. in most cases the instance of a thread. Shared variables are used to inform
	 * the thread about new events.
	 *
	 * @return the control executor
	 */
	ParallelExecutor<T> getControl();

	/**
	 *
	 * @return the {@link QueryInfo}
	 */
	default QueryInfo getQueryInfo() {
		return getControl().getQueryInfo();
	}

	/**
	 * Optional implementation to cancel this task on a best effort basis
	 */
	void cancel();

	/**
	 * Optional implementation to close this task on a best effort basis.
	 */
	default void close() {

	}
}
