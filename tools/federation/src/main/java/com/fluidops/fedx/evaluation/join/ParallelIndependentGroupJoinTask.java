/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package com.fluidops.fedx.evaluation.join;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import com.fluidops.fedx.algebra.IndependentJoinGroup;
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
public class ParallelIndependentGroupJoinTask extends ParallelTaskBase<BindingSet> {

	
	protected final FederationEvalStrategy strategy;
	protected final IndependentJoinGroup expr;
	protected final List<BindingSet> bindings;
	protected final ParallelExecutor<BindingSet> joinControl;
	
	public ParallelIndependentGroupJoinTask(ParallelExecutor<BindingSet> joinControl, FederationEvalStrategy strategy, IndependentJoinGroup expr, List<BindingSet> bindings) {
		this.strategy = strategy;
		this.expr = expr;
		this.bindings = bindings;
		this.joinControl = joinControl;
	}


	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> performTask() throws Exception {
		return strategy.evaluateIndependentJoinGroup(expr, bindings);		
	}


	@Override
	public ParallelExecutor<BindingSet> getControl() {
		return joinControl;
	}

}
