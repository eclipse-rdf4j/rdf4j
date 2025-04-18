/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.function.Predicate;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;

public class PreFilterQueryEvaluationStep implements QueryEvaluationStep {

	private final QueryEvaluationStep wrapped;
	private final Predicate<BindingSet> condition;

	public PreFilterQueryEvaluationStep(QueryEvaluationStep wrapped,
			QueryValueEvaluationStep condition) {
		this.wrapped = wrapped;
		this.condition = condition.asPredicate();
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet leftBindings) {
		if (!condition.test(leftBindings)) {
			// Usage of this method assume this instance is returned
			return QueryEvaluationStep.EMPTY_ITERATION;
		}

		return wrapped.evaluate(leftBindings);
	}
}
