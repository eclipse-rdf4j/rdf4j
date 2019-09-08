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

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import com.fluidops.fedx.algebra.StatementTupleExpr;
import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.concurrent.ParallelExecutor;
import com.fluidops.fedx.evaluation.concurrent.ParallelTaskBase;


/**
 * A task implementation representing a bound join, see {@link FederationEvalStrategy#evaluateBoundJoinStatementPattern(StatementTupleExpr, List)}
 * for further details on the evaluation process.
 * 
 * @author Andreas Schwarte
 */
public class ParallelBoundJoinTask extends ParallelTaskBase<BindingSet> {

	
	protected final FederationEvalStrategy strategy;
	protected final StatementTupleExpr expr;
	protected final List<BindingSet> bindings;
	protected final ParallelExecutor<BindingSet> joinControl;
	
	public ParallelBoundJoinTask(ParallelExecutor<BindingSet> joinControl, FederationEvalStrategy strategy, StatementTupleExpr expr, List<BindingSet> bindings) {
		this.strategy = strategy;
		this.expr = expr;
		this.bindings = bindings;
		this.joinControl = joinControl;
	}


	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> performTask() throws Exception {
		return strategy.evaluateBoundJoinStatementPattern(expr, bindings);		
	}


	@Override
	public ParallelExecutor<BindingSet> getControl() {
		return joinControl;
	}

}
