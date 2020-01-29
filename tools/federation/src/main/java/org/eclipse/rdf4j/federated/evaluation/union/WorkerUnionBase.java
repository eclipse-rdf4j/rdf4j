/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.union;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTask;
import org.eclipse.rdf4j.federated.structures.QueryInfo;

/**
 * Base class for worker unions providing convenience functions to add tasks.
 * 
 * @author Andreas Schwarte
 * 
 * @see SynchronousWorkerUnion
 * @see ControlledWorkerUnion
 */
public abstract class WorkerUnionBase<T> extends UnionExecutorBase<T> {

	protected List<ParallelTask<T>> tasks = new ArrayList<>();

	public WorkerUnionBase(FederationEvalStrategy strategy, QueryInfo queryInfo) {
		super(strategy, queryInfo);
	}

	/**
	 * Add a generic parallel task. Note that it is required that the task has this instance as its control.
	 * 
	 * @param task
	 */
	public void addTask(ParallelTask<T> task) {
		if (task.getControl() != this)
			throw new RuntimeException("Controlling instance of task must be the same as this ControlledWorkerUnion.");
		tasks.add(task);
	}
}
