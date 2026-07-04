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
package org.eclipse.rdf4j.federated.evaluation.union;

import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTask;
import org.eclipse.rdf4j.federated.structures.QueryInfo;

/**
 * Synchronous execution of union tasks, i.e. one after the other. The union result is contained in this iteration. Note
 * that the union operation is to be executed with the {@link #run()} method
 *
 * @author Andreas Schwarte
 */
public class SynchronousWorkerUnion<T> extends WorkerUnionBase<T> {

	public SynchronousWorkerUnion(QueryInfo queryInfo) {
		super(queryInfo);
	}

	@Override
	protected void union() throws Exception {
		for (ParallelTask<T> task : tasks) {
			try {
				task.getQueryInfo().registerScheduledTask(task);
			} catch (Throwable e) {
				task.cancel();
				throw e;
			}
			addResult(task.performTask());
		}
	}
}
