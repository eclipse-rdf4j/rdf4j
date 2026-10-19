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
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility.Result;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

public class AndValueEvaluationStep implements QueryValueEvaluationStep {

	private final QueryValueEvaluationStep leftStep;
	private final QueryValueEvaluationStep rightStep;
	private final QueryModelNode metricTarget;

	public AndValueEvaluationStep(QueryValueEvaluationStep leftStep, QueryValueEvaluationStep rightStep) {
		this(leftStep, rightStep, null);
	}

	public AndValueEvaluationStep(QueryValueEvaluationStep leftStep, QueryValueEvaluationStep rightStep,
			QueryModelNode metricTarget) {
		super();
		this.leftStep = leftStep;
		this.rightStep = rightStep;
		this.metricTarget = metricTarget;
	}

	@Override
	public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		try {

			if (QueryEvaluationUtility
					.getEffectiveBooleanValue(leftStep.evaluate(bindings)) == QueryEvaluationUtility.Result._false) {
				// Left argument evaluates to false, we don't need to look any
				// further
				incrementShortCircuitCount();
				return BooleanLiteral.FALSE;
			}
		} catch (ValueExprEvaluationException e) {
			// Failed to evaluate the left argument. Result is 'false' when
			// the right argument evaluates to 'false', failure otherwise.
			Value rightValue = rightStep.evaluate(bindings);
			if (QueryEvaluationUtility.getEffectiveBooleanValue(rightValue) == QueryEvaluationUtility.Result._false) {
				return BooleanLiteral.FALSE;
			} else {
				throw new ValueExprEvaluationException();
			}
		}

		// Left argument evaluated to 'true', result is determined
		// by the evaluation of the right argument.
		Value rightValue = rightStep.evaluate(bindings);
		return BooleanLiteral.valueOf(QueryEvaluationUtil.getEffectiveBooleanValue(rightValue));
	}

	public static QueryValueEvaluationStep supply(QueryValueEvaluationStep leftStep,
			QueryValueEvaluationStep rightStep) {
		return supply(leftStep, rightStep, null);
	}

	public static QueryValueEvaluationStep supply(QueryValueEvaluationStep leftStep,
			QueryValueEvaluationStep rightStep, QueryModelNode metricTarget) {
		if (leftStep.isConstant()) {
			Result constantLeftValue = QueryEvaluationUtility
					.getEffectiveBooleanValue(leftStep.evaluate(EmptyBindingSet.getInstance()));
			if (constantLeftValue == QueryEvaluationUtility.Result._false) {
				return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.FALSE);
			} else if (constantLeftValue == QueryEvaluationUtility.Result._true && rightStep.isConstant()) {
				Result constantRightValue = QueryEvaluationUtility
						.getEffectiveBooleanValue(rightStep.evaluate(EmptyBindingSet.getInstance()));
				if (constantRightValue == QueryEvaluationUtility.Result._false) {
					return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.FALSE);
				} else if (constantRightValue == QueryEvaluationUtility.Result._true) {
					return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.TRUE);
				}
			}
		}
		if (rightStep.isConstant()) {
			Result constantRightValue = QueryEvaluationUtility
					.getEffectiveBooleanValue(rightStep.evaluate(EmptyBindingSet.getInstance()));
			if (constantRightValue == QueryEvaluationUtility.Result._false) {
				return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.FALSE);
			}
		}
		return new AndValueEvaluationStep(leftStep, rightStep, metricTarget);
	}

	private void incrementShortCircuitCount() {
		if (metricTarget == null) {
			return;
		}
		metricTarget.setLongMetricActual(TelemetryMetricNames.SHORT_CIRCUIT_COUNT_ACTUAL,
				Math.max(0L, metricTarget.getLongMetricActual(TelemetryMetricNames.SHORT_CIRCUIT_COUNT_ACTUAL)) + 1L);
	}
}
