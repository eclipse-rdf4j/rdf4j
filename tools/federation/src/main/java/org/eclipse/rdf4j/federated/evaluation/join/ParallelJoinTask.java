/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTaskBase;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A task implementation representing a join, i.e. the provided expression is evaluated with the given bindings.
 *
 * @author Andreas Schwarte
 */
public class ParallelJoinTask extends ParallelTaskBase<BindingSet> {

	protected final FederationEvalStrategy strategy;
	protected final TupleExpr expr;
	protected final BindingSet bindings;
	protected final ParallelExecutor<BindingSet> joinControl;

	public ParallelJoinTask(ParallelExecutor<BindingSet> joinControl, FederationEvalStrategy strategy, TupleExpr expr,
			BindingSet bindings) {
		this.strategy = strategy;
		this.expr = expr;
		this.bindings = bindings;
		this.joinControl = joinControl;
	}

	@Override
	protected CloseableIteration<BindingSet, QueryEvaluationException> performTaskInternal() throws Exception {
		return strategy.evaluate(expr, bindings);
	}

	@Override
	public ParallelExecutor<BindingSet> getControl() {
		return joinControl;
	}
}
