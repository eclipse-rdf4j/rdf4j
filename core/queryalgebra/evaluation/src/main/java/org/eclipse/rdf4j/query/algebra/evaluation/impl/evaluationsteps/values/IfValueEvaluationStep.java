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
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

public final class IfValueEvaluationStep implements QueryValueEvaluationStep {
	private final QueryValueEvaluationStep result;
	private final QueryValueEvaluationStep condition;
	private final QueryValueEvaluationStep alternative;

	public IfValueEvaluationStep(QueryValueEvaluationStep result, QueryValueEvaluationStep condition,
			QueryValueEvaluationStep alternative) {
		this.result = result;
		this.condition = condition;
		this.alternative = alternative;
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
			return result.evaluate(bindings);
		} else {
			return alternative.evaluate(bindings);
		}
	}
}