/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DelayedIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A Step that may need to be executed in a EvaluationStrategy. The evaluate method should do the minimal work required
 * to evaluate given the bindings. As much as possible should be pre-computed (e.g. resolving constant values)
 */
@FunctionalInterface
public interface QueryEvaluationStep {
	/**
	 * Utility class that removes code duplication and makes a precompiled QueryEvaluationStep available as an iteration
	 * that may be created and used later.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	class DelayedEvaluationIteration
			extends DelayedIteration<BindingSet, QueryEvaluationException> {
		private final QueryEvaluationStep arg;
		private final BindingSet bs;

		public DelayedEvaluationIteration(QueryEvaluationStep arg, BindingSet bs) {
			this.arg = arg;
			this.bs = bs;
		}

		@Override
		protected CloseableIteration<? extends BindingSet, ? extends QueryEvaluationException> createIteration()
				throws QueryEvaluationException {
			return arg.evaluate(bs);
		}
	}

	EmptyIteration<BindingSet, QueryEvaluationException> EMPTY_ITERATION = new EmptyIteration<>();
	QueryEvaluationStep EMPTY = bindings -> EMPTY_ITERATION;

	CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings);

	/**
	 * A fall back implementation that wraps a pre-existing evaluate method on a strategy
	 *
	 * @param strategy that can evaluate the tuple expr.
	 * @param expr     that is going to be evaluated
	 * @return a thin wrapper arround the evaluation call.
	 */
	static QueryEvaluationStep minimal(EvaluationStrategy strategy, TupleExpr expr) {
		return bs -> strategy.evaluate(expr, bs);
	}

	static QueryEvaluationStep empty() {
		return bs -> new EmptyIteration<>();
	}

	/**
	 * Wrap an QueryEvalationStep: where we apply a function on every evaluation result of the wrapped EvaluationStep.
	 * Useful to add a timing function
	 *
	 * @param qes  an QueryEvaluationStep that needs to return modified evaluation results
	 * @param wrap the function that will do the modification
	 * @return a new evaluation step that executes wrap on the inner qes.
	 */
	static QueryEvaluationStep wrap(QueryEvaluationStep qes,
			Function<CloseableIteration<BindingSet, QueryEvaluationException>, CloseableIteration<BindingSet, QueryEvaluationException>> wrap) {
		return bs -> wrap.apply(qes.evaluate(bs));
	}
}
