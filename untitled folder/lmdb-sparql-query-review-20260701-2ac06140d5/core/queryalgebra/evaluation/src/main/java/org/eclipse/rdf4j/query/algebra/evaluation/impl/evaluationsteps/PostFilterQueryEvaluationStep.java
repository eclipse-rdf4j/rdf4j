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
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;

public class PostFilterQueryEvaluationStep implements QueryEvaluationStep {

	private final QueryEvaluationStep wrapped;
	private final Predicate<BindingSet> condition;
	private final QueryModelNode metricTarget;

	public PostFilterQueryEvaluationStep(QueryEvaluationStep wrapped, QueryValueEvaluationStep condition) {
		this(wrapped, condition, null);
	}

	public PostFilterQueryEvaluationStep(QueryEvaluationStep wrapped,
			QueryValueEvaluationStep condition, QueryModelNode metricTarget) {
		this.wrapped = wrapped;
		this.condition = condition.asPredicate();
		this.metricTarget = metricTarget;
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet leftBindings) {
		var rightIteration = wrapped.evaluate(leftBindings);

		if (rightIteration == QueryEvaluationStep.EMPTY_ITERATION) {
			return rightIteration;
		}

		return new FilterIteration<>(rightIteration) {

			@Override
			protected boolean accept(BindingSet bindings) {
				boolean accepted = condition.test(bindings);
				if (!accepted && metricTarget != null) {
					metricTarget.setLongMetricActual(TelemetryMetricNames.LEFT_JOIN_CONDITION_REJECTED_ROWS_ACTUAL,
							Math.max(0L,
									metricTarget.getLongMetricActual(
											TelemetryMetricNames.LEFT_JOIN_CONDITION_REJECTED_ROWS_ACTUAL))
									+ 1L);
				}
				return accepted;
			}

			@Override
			protected void handleClose() {
				// Nothing to close
			}
		};
	}
}
