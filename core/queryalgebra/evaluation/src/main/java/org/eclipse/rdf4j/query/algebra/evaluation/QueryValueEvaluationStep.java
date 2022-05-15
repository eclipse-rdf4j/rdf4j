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
import org.eclipse.rdf4j.query.algebra.ValueExpr;

/**
 * A step in the query evaluation that works on ValueExpresions.
 */
public interface QueryValueEvaluationStep {
	Value evaluate(BindingSet bindings)
			throws QueryEvaluationException;

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
	class ConstantQueryValueEvaluationStep implements QueryValueEvaluationStep {
		private final Value value;

		public ConstantQueryValueEvaluationStep(ValueConstant valueConstant) {
			this.value = valueConstant.getValue();
		}

		public ConstantQueryValueEvaluationStep(Value valueConstant) {
			this.value = valueConstant;
		}

		@Override
		public Value evaluate(BindingSet bindings) throws QueryEvaluationException {
			return value;
		}

		public boolean isConstant() {
			return true;
		}
	}

	/**
	 * A minimal implementation that falls back to calling evaluate in the strategy.
	 */
	final class Minimal implements QueryValueEvaluationStep {
		private final ValueExpr ve;
		private final EvaluationStrategy strategy;

		public Minimal(EvaluationStrategy strategy, ValueExpr ve) {
			super();
			this.strategy = strategy;
			this.ve = ve;
		}

		@Override
		public Value evaluate(BindingSet bindings) throws QueryEvaluationException {
			return strategy.evaluate(ve, bindings);
		}
	}
}
