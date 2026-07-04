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

import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.CompareAny;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

public final class CompareAnyValueEvaluationStep implements QueryValueEvaluationStep {
	private final QueryValueEvaluationStep arg;
	private final CompareAny node;
	private final QueryEvaluationStep subquery;

	private final Function<BindingSet, Value> getValue;

	public CompareAnyValueEvaluationStep(QueryValueEvaluationStep arg, CompareAny node,
			QueryEvaluationStep subquery, QueryEvaluationContext context) {
		this.arg = arg;
		this.node = node;
		this.subquery = subquery;
		String bindingName = node.getSubQuery().getBindingNames().iterator().next();
		this.getValue = context.getValue(bindingName);
	}

	@Override
	public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		Value leftValue = arg.evaluate(bindings);
		// Result is false until a match has been found
		boolean result = false;

		try (CloseableIteration<BindingSet> iter = subquery.evaluate(bindings)) {
			while (!result && iter.hasNext()) {
				BindingSet bindingSet = iter.next();
				Value rightValue = getValue.apply(bindingSet);
				try {
					result = QueryEvaluationUtil.compare(leftValue, rightValue, node.getOperator());
				} catch (ValueExprEvaluationException e) {
					// ignore, maybe next value will match
				}
			}
		}
		return BooleanLiteral.valueOf(result);
	}
}
