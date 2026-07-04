/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;

public final class IfValueEvaluationStep implements QueryValueEvaluationStep {
	private final QueryValueEvaluationStep result;
	private final QueryValueEvaluationStep condition;
	private final QueryValueEvaluationStep alternative;
	private final QueryModelNode metricTarget;

	public IfValueEvaluationStep(QueryValueEvaluationStep result, QueryValueEvaluationStep condition,
			QueryValueEvaluationStep alternative) {
		this(result, condition, alternative, null);
	}

	public IfValueEvaluationStep(QueryValueEvaluationStep result, QueryValueEvaluationStep condition,
			QueryValueEvaluationStep alternative, QueryModelNode metricTarget) {
		this.result = result;
		this.condition = condition;
		this.alternative = alternative;
		this.metricTarget = metricTarget;
	}

	@Override
	public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		boolean conditionIsTrue;

		try {
			Value value = condition.evaluate(bindings);
			conditionIsTrue = QueryEvaluationUtil.getEffectiveBooleanValue(value);
		} catch (ValueExprEvaluationException e) {
			// in case of type error, if-construction should result in empty
			// binding.
			return null;
		}

		if (conditionIsTrue) {
			incrementShortCircuitCount();
			return result.evaluate(bindings);
		} else {
			incrementShortCircuitCount();
			return alternative.evaluate(bindings);
		}
	}

	private void incrementShortCircuitCount() {
		if (metricTarget == null) {
			return;
		}
		metricTarget.setLongMetricActual(TelemetryMetricNames.SHORT_CIRCUIT_COUNT_ACTUAL,
				Math.max(0L, metricTarget.getLongMetricActual(TelemetryMetricNames.SHORT_CIRCUIT_COUNT_ACTUAL)) + 1L);
	}
}
