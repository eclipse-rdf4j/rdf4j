/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.TupleFunctionCall;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;

/**
 * An {@link EvaluationStrategy} that has support for {@link TupleFunction}s.
 */
public class TupleFunctionEvaluationStrategy extends StrictEvaluationStrategy {

	private final TupleFunctionRegistry tupleFuncRegistry;

	public TupleFunctionEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver) {
		this(tripleSource, dataset, serviceResolver, 0);
	}

	public TupleFunctionEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncThreshold) {
		this(tripleSource, dataset, serviceResolver, TupleFunctionRegistry.getInstance(), iterationCacheSyncThreshold);
	}

	public TupleFunctionEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, TupleFunctionRegistry tupleFuncRegistry,
			long iterationCacheSyncThreshold) {
		super(tripleSource, dataset, serviceResolver, iterationCacheSyncThreshold);
		this.tupleFuncRegistry = tupleFuncRegistry;
	}

	public TupleFunctionEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, TupleFunctionRegistry tupleFunctionRegistry) {
		this(tripleSource, dataset, serviceResolver, tupleFunctionRegistry, 0);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleExpr expr, BindingSet bindings)
			throws QueryEvaluationException {
		if (expr instanceof TupleFunctionCall) {
			return evaluate((TupleFunctionCall) expr, bindings);
		} else {
			return super.evaluate(expr, bindings);
		}
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleFunctionCall expr,
			BindingSet bindings) throws QueryEvaluationException {
		TupleFunction func = tupleFuncRegistry.get(expr.getURI())
				.orElseThrow(() -> new QueryEvaluationException("Unknown tuple function '" + expr.getURI() + "'"));

		List<ValueExpr> args = expr.getArgs();

		Value[] argValues = new Value[args.size()];
		for (int i = 0; i < args.size(); i++) {
			argValues[i] = evaluate(args.get(i), bindings);
		}

		return evaluate(func, expr.getResultVars(), bindings, tripleSource.getValueFactory(), argValues);
	}

	public static CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleFunction func,
			final List<Var> resultVars, final BindingSet bindings, ValueFactory valueFactory, Value... argValues)
			throws QueryEvaluationException {
		final CloseableIteration<? extends List<? extends Value>, QueryEvaluationException> iter = func
				.evaluate(valueFactory, argValues);
		return new LookAheadIteration<BindingSet, QueryEvaluationException>() {

			@Override
			public BindingSet getNextElement() throws QueryEvaluationException {
				QueryBindingSet resultBindings = null;
				while (resultBindings == null && iter.hasNext()) {
					resultBindings = new QueryBindingSet(bindings);
					List<? extends Value> values = iter.next();
					if (resultVars.size() != values.size()) {
						throw new QueryEvaluationException("Incorrect number of result vars: require " + values.size());
					}
					for (int i = 0; i < values.size(); i++) {
						Value result = values.get(i);
						Var resultVar = resultVars.get(i);
						Value varValue = resultVar.getValue();
						String varName = resultVar.getName();
						Value boundValue = bindings.getValue(varName);
						if ((varValue == null || result.equals(varValue))
								&& (boundValue == null || result.equals(boundValue))) {
							if (boundValue == null) { // if not already present
								resultBindings.addBinding(varName, result);
							}
						} else {
							resultBindings = null;
							break;
						}
					}
				}
				return resultBindings;
			}

			@Override
			protected void handleClose() throws QueryEvaluationException {
				try {
					super.handleClose();
				} finally {
					iter.close();
				}
			}
		};
	}
}
