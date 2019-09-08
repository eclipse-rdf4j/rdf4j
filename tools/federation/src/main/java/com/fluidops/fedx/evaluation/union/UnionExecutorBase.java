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

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.concurrent.ParallelExecutorBase;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * Base class for any parallel union executor.
 * 
 * Note that this class extends {@link LookAheadIteration} and thus any implementation of this 
 * class is applicable for pipelining when used in a different thread (access to shared
 * variables is synchronized).
 * 
 * @author Andreas Schwarte
 *
 */
public abstract class UnionExecutorBase<T> extends ParallelExecutorBase<T> {

	
	
	public UnionExecutorBase(FederationEvalStrategy strategy, QueryInfo queryInfo) {
		super(strategy, queryInfo);
	}
	

	@Override
	protected final void performExecution() throws Exception {
		union();
	}
	

	/**
	 * 
	 * Note: this method must block until the union is executed completely. Otherwise
	 * the result queue is marked as committed while this isn't the case. The blocking
	 * behavior in general is no problem: If you need concurrent access to the result
	 * (i.e. pipelining) just run the union in a separate thread. Access to the result
	 * iteration is synchronized.
	 * 
	 * @throws Exception
	 */
	protected abstract void union() throws Exception;
	
	@Override
	protected String getExecutorType() {
		return "Union";
	}

}
