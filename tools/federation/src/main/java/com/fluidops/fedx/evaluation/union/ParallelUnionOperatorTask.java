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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.concurrent.ParallelExecutor;
import com.fluidops.fedx.evaluation.concurrent.ParallelTaskBase;

/**
 * A task implementation representing a UNION operator expression to be evaluated.
 * 
 * @author Andreas Schwarte
 */
public class ParallelUnionOperatorTask extends ParallelTaskBase<BindingSet> {

	protected final ParallelExecutor<BindingSet> unionControl;
	protected final FederationEvalStrategy strategy;
	protected final TupleExpr expr;
	protected final BindingSet bindings;
	
	public ParallelUnionOperatorTask(ParallelExecutor<BindingSet> unionControl,
			FederationEvalStrategy strategy, TupleExpr expr, BindingSet bindings) {
		super();
		this.unionControl = unionControl;
		this.strategy = strategy;
		this.expr = expr;
		this.bindings = bindings;
	}
	
	@Override
	public ParallelExecutor<BindingSet> getControl() {
		return unionControl;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> performTask()
			throws Exception {
		return strategy.evaluate(expr, bindings);
	}
}
