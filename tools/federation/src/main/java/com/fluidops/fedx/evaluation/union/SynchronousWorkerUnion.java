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

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.concurrent.ParallelTask;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * Synchronous execution of union tasks, i.e. one after the other. The union result is contained in 
 * this iteration. Note that the union operation is to be executed with the {@link #run()} method
 * 
 * @author Andreas Schwarte
 */
public class SynchronousWorkerUnion<T> extends WorkerUnionBase<T> {

	public SynchronousWorkerUnion(FederationEvalStrategy strategy, QueryInfo queryInfo) {
		super(strategy, queryInfo);
	}	
	
	@Override
	protected void union() throws Exception {
		for (ParallelTask<T> task : tasks)
			addResult(task.performTask());
	}
}
