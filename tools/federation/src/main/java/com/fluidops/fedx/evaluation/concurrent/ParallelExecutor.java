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
package com.fluidops.fedx.evaluation.concurrent;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import com.fluidops.fedx.evaluation.join.JoinExecutorBase;
import com.fluidops.fedx.evaluation.union.UnionExecutorBase;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * Interface for any parallel cursor, i.e. result iterations. Implementations can act 
 * as control for scheduler implementations, e.g. {@link ControlledWorkerScheduler}. 
 * The common use case is to pass results from the scheduler to the controlling
 * result iteration.
 * 
 * @author Andreas Schwarte
 * 
 * @see JoinExecutorBase
 * @see UnionExecutorBase
 */
public interface ParallelExecutor<T> extends Runnable {

	/**
	 * Handle the result appropriately, e.g. add it to the result iteration. Take care
	 * for synchronization in a multithreaded environment
	 * 
	 * @param res
	 */
	public void addResult(CloseableIteration<T, QueryEvaluationException> res);
	
	/**
	 * Toss some exception to the controlling instance
	 * 
	 * @param e
	 */
	public void toss(Exception e);
	
	/**
	 * Inform the controlling instance that some job is done from a different thread. In most cases this is a no-op.
	 */
	public void done();
	
	
	/**
	 * Return true if this executor is finished or aborted
	 * 
	 * @return whether the execution is finished
	 */
	public boolean isFinished();

	
	/**
	 * Return the query info of the associated query
	 * 
	 * @return the query info
	 */
	public QueryInfo getQueryInfo();
}
