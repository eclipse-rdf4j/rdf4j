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
import org.eclipse.rdf4j.federated.algebra.EmptyStatementPattern;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTaskBase;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.sparql.federation.CollectionIteration;

/**
 * A {@link ParallelTaskBase} for executing bind left joins, where the join argument is an
 * {@link EmptyStatementPattern}. The effective result is that the input bindings from the left operand are passed
 * through.
 *
 * @author Andreas Schwarte
 */
public class ParallelEmptyBindLeftJoinTask extends ParallelTaskBase<BindingSet> {

	protected final FederationEvalStrategy strategy;
	protected final EmptyStatementPattern rightArg;
	protected final List<BindingSet> bindings;
	protected final ParallelExecutor<BindingSet> joinControl;

	public ParallelEmptyBindLeftJoinTask(ParallelExecutor<BindingSet> joinControl, FederationEvalStrategy strategy,
			EmptyStatementPattern expr, List<BindingSet> bindings) {
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
		// simply return the input bindings (=> the empty statement pattern cannot add results)
		return new CollectionIteration<BindingSet>(bindings);
	}

}
