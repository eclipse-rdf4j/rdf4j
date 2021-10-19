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
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A Step that may need to be executed in a EvaluationStrategy. The evaluate method should do the minimal work required
 * to evaluate given the bindings.
 */
@FunctionalInterface
public interface QueryEvaluationStep {
	public class DelayedEvaluationIteration
			extends DelayedIteration<BindingSet, QueryEvaluationException> {
		private final QueryEvaluationStep arg;
		private final BindingSet bs;

		public DelayedEvaluationIteration(QueryEvaluationStep arg, BindingSet bs) {
			this.arg = arg;
			this.bs = bs;
		}

		@Override
		protected Iteration<? extends BindingSet, ? extends QueryEvaluationException> createIteration()
				throws QueryEvaluationException {
			return arg.evaluate(bs);
		}
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings);

	public static QueryEvaluationStep minimal(EvaluationStrategy strategy, TupleExpr expr) {
		return new QueryEvaluationStep() {
			@Override
			public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
				return strategy.evaluate(expr, bs);
			}
		};
	}

	public static QueryEvaluationStep wrap(QueryEvaluationStep qes, TupleExpr expr,
			Function<CloseableIteration<BindingSet, QueryEvaluationException>, CloseableIteration<BindingSet, QueryEvaluationException>> wrap) {
		return new QueryEvaluationStep() {
			@Override
			public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
				return wrap.apply(qes.evaluate(bs));
			}
		};
	}
}
