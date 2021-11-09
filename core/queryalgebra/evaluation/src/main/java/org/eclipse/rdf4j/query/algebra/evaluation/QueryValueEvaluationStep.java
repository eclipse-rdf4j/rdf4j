/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.ValueConstant;

/**
 * A step in the query evaluation that works on ValueExpresions.
 */
public interface QueryValueEvaluationStep {
	Value evaluate(BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException;

	/**
	 * If an value expression results in a constant then it may be executed once per query invocation. This can reduce
	 * computation time significantly.
	 * 
	 * @return if this ValueExpresionStep will always return the same value
	 */
	default boolean isConstant() {
		return false;
	}

	/**
	 * A QueryValueEvalationStep that will return the same constant value throughout the query execution. As these
	 * rather result just in a value we set the value at precompile time.
	 */
	public static class ConstantQueryValueEvaluationStep implements QueryValueEvaluationStep {
		private final Value value;

		public ConstantQueryValueEvaluationStep(ValueConstant valueConstant) {
			this.value = valueConstant.getValue();
		}

		public ConstantQueryValueEvaluationStep(Value valueConstant) {
			this.value = valueConstant;
		}

		@Override
		public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
			return value;
		}

		public boolean isConstant() {
			return true;
		}
	}
}
