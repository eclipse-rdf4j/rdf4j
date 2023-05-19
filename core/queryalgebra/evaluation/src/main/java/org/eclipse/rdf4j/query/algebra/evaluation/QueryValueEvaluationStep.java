/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.function.Function;

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

	/**
	 * A minimal implementation that falls is known to throw an ValueExprEvaluationException. This can't be a constant
	 * as the downstream code needs to catch and deal with it and that needs re-evaluation.
	 */
	public static final class Fail implements QueryValueEvaluationStep {

		private final String message;

		public Fail(String message) {
			super();
			this.message = message;
		}

		@Override
		public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
			throw new ValueExprEvaluationException(message);
		}
	}

	/**
	 * A minimal implementation that falls calls a function that should return a value per passed in bindingsets.
	 */
	public static final class ApplyFunctionForEachBinding implements QueryValueEvaluationStep {

		private final Function<BindingSet, Value> function;

		public ApplyFunctionForEachBinding(Function<BindingSet, Value> function) {
			super();
			this.function = function;
		}

		@Override
		public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
			return function.apply(bindings);
		}
	}
}
