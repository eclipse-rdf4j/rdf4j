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

import java.util.List;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

public final class ListMemberValueOperationStep implements QueryValueEvaluationStep {
	private final List<QueryValueEvaluationStep> compiledArgs;

	public ListMemberValueOperationStep(List<QueryValueEvaluationStep> compiledArgs) {
		this.compiledArgs = compiledArgs;
	}

	@Override
	public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		Value leftValue = compiledArgs.get(0).evaluate(bindings);
		boolean result = false;
		ValueExprEvaluationException typeError = null;
		for (int i = 1; i < compiledArgs.size(); i++) {
			QueryValueEvaluationStep arg = compiledArgs.get(i);
			try {
				Value rightValue = arg.evaluate(bindings);
				result = leftValue == null && rightValue == null;
				if (!result) {
					result = QueryEvaluationUtil.compare(leftValue, rightValue, CompareOp.EQ);
				}
				if (result) {
					break;
				}
			} catch (ValueExprEvaluationException caught) {
				typeError = caught;
			}
		}

		if (typeError != null && !result) {
			// cf. SPARQL spec a type error is thrown if the value is not in the
			// list and one of the list members caused a type error in the
			// comparison.
			throw typeError;
		}

		return BooleanLiteral.valueOf(result);
	}
}
