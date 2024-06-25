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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.In;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

public final class InValueEvaluationStep implements QueryValueEvaluationStep {
	private final In node;
	private final QueryEvaluationStep subquery;
	private final QueryValueEvaluationStep left;

	public InValueEvaluationStep(In node, QueryEvaluationStep subquery,
			QueryValueEvaluationStep left) {
		this.node = node;
		this.subquery = subquery;
		this.left = left;
	}

	@Override
	public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		Value leftValue = left.evaluate(bindings);

		// Result is false until a match has been found
		boolean result = false;
		// Use first binding name from tuple expr to compare values
		String bindingName = node.getSubQuery().getBindingNames().iterator().next();
		try (CloseableIteration<BindingSet> iter = subquery.evaluate(bindings)) {
			while (!result && iter.hasNext()) {
				BindingSet bindingSet = iter.next();
				Value rightValue = bindingSet.getValue(bindingName);
				result = leftValue == null && rightValue == null
						|| leftValue != null && leftValue.equals(rightValue);
			}
		}
		return BooleanLiteral.valueOf(result);
	}
}
