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

import com.fluidops.fedx.structures.QueryInfo;


/**
 * Interface for any parallel task that can be performed in Scheduler implementations.
 * 
 * @author Andreas Schwarte
 *
 */
public interface ParallelTask<T> {

	public CloseableIteration<T, QueryEvaluationException> performTask() throws Exception;
	
	/**
	 * return the controlling instance, e.g. in most cases the instance of a thread. Shared variables
	 * are used to inform the thread about new events.
	 * 
	 * @return the control executor
	 */
	public ParallelExecutor<T> getControl();

	/**
	 * 
	 * @return the {@link QueryInfo}
	 */
	public default QueryInfo getQueryInfo() {
		return getControl().getQueryInfo();
	}

	/**
	 * Optional implementation to cancel this task on a best effort basis
	 */
	public void cancel();
}
