/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTaskBase;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * A {@link ParallelTaskBase} for executing bind left joins.
 *
 * @author Andreas Schwarte
 * @see FederationEvalStrategy#evaluateLeftBoundJoinStatementPattern(StatementTupleExpr, List)
 */
public class ParallelBindLeftJoinTask extends ParallelTaskBase<BindingSet> {

	protected final FederationEvalStrategy strategy;
	protected final StatementTupleExpr rightArg;
	protected final List<BindingSet> bindings;
	protected final ParallelExecutor<BindingSet> joinControl;

	public ParallelBindLeftJoinTask(ParallelExecutor<BindingSet> joinControl, FederationEvalStrategy strategy,
			StatementTupleExpr expr, List<BindingSet> bindings) {
		this.strategy = strategy;
		this.rightArg = expr;
		this.bindings = bindings;
		this.joinControl = joinControl;
	}

	@Override
	public ParallelExecutor<BindingSet> getControl() {
		return joinControl;
	}

	@Override
	protected CloseableIteration<BindingSet> performTaskInternal() throws Exception {
		return strategy.evaluateLeftBoundJoinStatementPattern(rightArg, bindings);
	}

}
