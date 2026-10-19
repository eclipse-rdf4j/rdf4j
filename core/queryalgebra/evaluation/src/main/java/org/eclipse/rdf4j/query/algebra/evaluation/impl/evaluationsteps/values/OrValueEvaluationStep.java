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
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;

public class OrValueEvaluationStep implements QueryValueEvaluationStep {
	private final QueryValueEvaluationStep leftArg;
	private final QueryValueEvaluationStep rightArg;
	private final QueryModelNode metricTarget;

	public OrValueEvaluationStep(QueryValueEvaluationStep leftArg, QueryValueEvaluationStep rightArg) {
		this(leftArg, rightArg, null);
	}

	public OrValueEvaluationStep(QueryValueEvaluationStep leftArg, QueryValueEvaluationStep rightArg,
			QueryModelNode metricTarget) {
		this.leftArg = leftArg;
		this.rightArg = rightArg;
		this.metricTarget = metricTarget;
	}

	@Override
	public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		try {
			Value leftValue = leftArg.evaluate(bindings);
			if (QueryEvaluationUtil.getEffectiveBooleanValue(leftValue)) {
				// Left argument evaluates to true, we don't need to look any
				// further
				incrementShortCircuitCount();
				return BooleanLiteral.TRUE;
			}
		} catch (ValueExprEvaluationException e) {
			Value rightValue = rightArg.evaluate(bindings);
			if (QueryEvaluationUtil.getEffectiveBooleanValue(rightValue)) {
				return BooleanLiteral.TRUE;
			} else {
				throw new ValueExprEvaluationException();
			}
		}
		// Left argument evaluated to 'false', result is determined
		// by the evaluation of the right argument.
		Value rightValue = rightArg.evaluate(bindings);
		return BooleanLiteral.valueOf(QueryEvaluationUtil.getEffectiveBooleanValue(rightValue));
	}

	private void incrementShortCircuitCount() {
		if (metricTarget == null) {
			return;
		}
		metricTarget.setLongMetricActual(TelemetryMetricNames.SHORT_CIRCUIT_COUNT_ACTUAL,
				Math.max(0L, metricTarget.getLongMetricActual(TelemetryMetricNames.SHORT_CIRCUIT_COUNT_ACTUAL)) + 1L);
	}
}
