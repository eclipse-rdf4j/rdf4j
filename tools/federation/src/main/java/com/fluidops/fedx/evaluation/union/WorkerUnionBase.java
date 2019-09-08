/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.evaluation.union;

import java.util.ArrayList;
import java.util.List;

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.concurrent.ParallelTask;
import com.fluidops.fedx.structures.QueryInfo;

/**
 * Base class for worker unions providing convenience functions to add tasks.
 * 
 * @author Andreas Schwarte
 * 
 * @see SynchronousWorkerUnion
 * @see ControlledWorkerUnion
 */
public abstract class WorkerUnionBase<T> extends UnionExecutorBase<T> {

	protected List<ParallelTask<T>> tasks = new ArrayList<ParallelTask<T>>();
	
	public WorkerUnionBase(FederationEvalStrategy strategy, QueryInfo queryInfo) {
		super(strategy, queryInfo);
	}
	

	/**
	 * Add a generic parallel task. Note that it is required that the task has 
	 * this instance as its control.
	 * 
	 * @param task
	 */
	public void addTask(ParallelTask<T> task) {
		if (task.getControl() != this)
			throw new RuntimeException("Controlling instance of task must be the same as this ControlledWorkerUnion.");
		tasks.add( task);
	}
}
