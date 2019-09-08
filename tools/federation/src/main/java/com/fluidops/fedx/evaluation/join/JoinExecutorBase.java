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
package com.fluidops.fedx.evaluation.join;

import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.concurrent.ParallelExecutorBase;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * Base class for any join parallel join executor. 
 * 
 * Note that this class extends {@link LookAheadIteration} and thus any implementation of this 
 * class is applicable for pipelining when used in a different thread (access to shared
 * variables is synchronized).
 * 
 * @author Andreas Schwarte
 */
public abstract class JoinExecutorBase<T> extends ParallelExecutorBase<T> {

	
	/* Constants */
	protected final TupleExpr rightArg;						// the right argument for the join
	protected final BindingSet bindings;					// the bindings

	/* Variables */
	protected Set<String> joinVars; // might be unknown (i.e. null for some implementations)
	protected CloseableIteration<T, QueryEvaluationException> leftIter;

	
	public JoinExecutorBase(FederationEvalStrategy strategy, CloseableIteration<T, QueryEvaluationException> leftIter, TupleExpr rightArg,
			BindingSet bindings, QueryInfo queryInfo) throws QueryEvaluationException	{
		super(strategy, queryInfo);
		this.leftIter = leftIter;
		this.rightArg = rightArg;
		this.bindings = bindings;
	}
	
	@Override
	protected final void performExecution() throws Exception {

		handleBindings();
		
	}
	
	/**
	 * Implementations must implement this method to handle bindings.
	 * 
	 * Use the following as a template
	 * <code>
	 * while (!closed && leftIter.hasNext()) {
	 * 		// your code
	 * }
	 * </code>
	 * 
	 * and add results to rightQueue. Note that addResult() is implemented synchronized
	 * and thus thread safe. In case you can guarantee sequential access, it is also
	 * possible to directly access rightQueue
	 * 
	 * 
	 * Note that the implementation must block until the entire join is executed.
	 */
	protected abstract void handleBindings() throws Exception;
	
	
	@Override
	public void handleClose() throws QueryEvaluationException {
		

		try {
			super.handleClose();
		} finally {
			leftIter.close();
		}
	}
	
	@Override
	protected String getExecutorType() {
		return "Join";
	}
	
	/**
	 * @return the join variables, might be <code>null</code> if unknown in the
	 *         concrete implementation
	 */
	public Set<String> getJoinVars() {
		return joinVars;
	}

	/**
	 * Set the join variables
	 * 
	 * @param joinVars the join variables
	 */
	public void setJoinVars(Set<String> joinVars) {
		this.joinVars = joinVars;
	}
}
