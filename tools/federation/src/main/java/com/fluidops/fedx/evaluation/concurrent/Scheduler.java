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

/**
 * Interface for any scheduler. 
 * 
 * @author Andreas Schwarte
 *
 * @see ControlledWorkerScheduler
 */
public interface Scheduler<T> {

	/**
	 * Schedule the provided task.
	 * 
	 * @param task
	 */
	public void schedule(ParallelTask<T> task);
	
	/**
	 * Callback to handle the result.
	 * 
	 * @param res
	 */
	public void handleResult(CloseableIteration<T, QueryEvaluationException> res);
	
	/**
	 * Inform the scheduler that a certain task is done.
	 * 
	 */
	public void done();
	
	/**
	 * Toss an exception to the scheduler.
	 * 
	 * @param e
	 */
	public void toss(Exception e);
	
	/**
	 * Abort the execution of running and queued tasks.
	 * 
	 */
	public void abort();
	
	public void shutdown();

	/**
	 * Inform the scheduler that no more tasks will be scheduled.
	 */
	public void informFinish();
	
	/**
	 * Determine if the scheduler has unfinished tasks.
	 * 
	 * @return whether the scheduler is running
	 */
	public boolean isRunning();
	
}
