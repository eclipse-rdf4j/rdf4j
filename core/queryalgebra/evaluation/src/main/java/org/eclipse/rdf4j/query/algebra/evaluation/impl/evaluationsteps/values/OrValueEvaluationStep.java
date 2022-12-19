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
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

public class OrValueEvaluationStep implements QueryValueEvaluationStep {
	private final QueryValueEvaluationStep leftArg;
	private final QueryValueEvaluationStep rightArg;

	public OrValueEvaluationStep(QueryValueEvaluationStep leftArg, QueryValueEvaluationStep rightArg) {
		this.leftArg = leftArg;
		this.rightArg = rightArg;
	}

	@Override
	public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		try {
			Value leftValue = leftArg.evaluate(bindings);
			if (QueryEvaluationUtil.getEffectiveBooleanValue(leftValue)) {
				// Left argument evaluates to true, we don't need to look any
				// further
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
}
