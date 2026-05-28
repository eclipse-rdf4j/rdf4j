/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.Lateral;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.LateralIterator;

public final class LateralQueryEvaluationStep implements QueryEvaluationStep {
	private final QueryEvaluationStep left;
	private final QueryEvaluationStep right;
	private final Lateral lateral;

	public static QueryEvaluationStep supply(EvaluationStrategy strategy, Lateral lateral,
			QueryEvaluationContext context) {
		QueryEvaluationStep left = strategy.precompile(lateral.getLeftArg(), context);
		QueryEvaluationStep right = strategy.precompile(lateral.getRightArg(), context);
		return new LateralQueryEvaluationStep(left, right, lateral);
	}

	public LateralQueryEvaluationStep(QueryEvaluationStep left, QueryEvaluationStep right, Lateral lateral) {
		this.left = left;
		this.right = right;
		this.lateral = lateral;
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		// Evaluate left side
		CloseableIteration<BindingSet> leftResults = left.evaluate(bindings);

		// For each left result, evaluate right side with injected bindings and union all results
		return LateralIterator.getInstance(leftResults, right);
	}
}
