/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.union;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTaskBase;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

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
