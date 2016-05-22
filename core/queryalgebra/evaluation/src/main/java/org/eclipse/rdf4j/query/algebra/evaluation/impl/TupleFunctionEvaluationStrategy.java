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
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.TupleFunctionCall;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;

/**
 * An {@link EvaluationStrategy} that adds support for {@link TupleFunction}s to an existing
 * EvaluationStrategy.
 */
public class TupleFunctionEvaluationStrategy implements EvaluationStrategy {

	private final EvaluationStrategy delegate;

	private final ValueFactory valueFactory;

	private final TupleFunctionRegistry tupleFuncRegistry;

	public TupleFunctionEvaluationStrategy(EvaluationStrategy delegate, ValueFactory valueFactory) {
		this(delegate, valueFactory, TupleFunctionRegistry.getInstance());
	}

	public TupleFunctionEvaluationStrategy(EvaluationStrategy delegate, ValueFactory valueFactory,
			TupleFunctionRegistry tupleFuncRegistry)
	{
		this.delegate = delegate;
		this.valueFactory = valueFactory;
		this.tupleFuncRegistry = tupleFuncRegistry;
	}

	@Override
	public FederatedService getService(String serviceUrl)
		throws QueryEvaluationException
	{
		return delegate.getService(serviceUrl);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service expr, String serviceUri,
			CloseableIteration<BindingSet, QueryEvaluationException> bindings)
		throws QueryEvaluationException
	{
		return delegate.evaluate(expr, serviceUri, bindings);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleExpr expr,
			BindingSet bindings)
		throws QueryEvaluationException
	{
		if (expr instanceof TupleFunctionCall) {
			return evaluate((TupleFunctionCall)expr, bindings);
		}
		else {
			return delegate.evaluate(expr, bindings);
		}
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleFunctionCall expr,
			BindingSet bindings)
		throws QueryEvaluationException
	{
		TupleFunction func = tupleFuncRegistry.get(expr.getURI());

		if (func == null) {
			throw new QueryEvaluationException("Unknown tuple function '" + expr.getURI() + "'");
		}

		List<ValueExpr> args = expr.getArgs();

		Value[] argValues = new Value[args.size()];
		for (int i = 0; i < args.size(); i++) {
			argValues[i] = evaluate(args.get(i), bindings);
		}

		return evaluate(func, expr.getResultVars(), bindings, valueFactory, argValues);
	}

	@Override
	public Value evaluate(ValueExpr expr, BindingSet bindings)
		throws QueryEvaluationException
	{
		return delegate.evaluate(expr, bindings);
	}

	@Override
	public boolean isTrue(ValueExpr expr, BindingSet bindings)
		throws QueryEvaluationException
	{
		return delegate.isTrue(expr, bindings);
	}

	public static CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleFunction func,
			final List<Var> resultVars, final BindingSet bindings, ValueFactory valueFactory,
			Value... argValues)
		throws QueryEvaluationException
	{
		final CloseableIteration<? extends List<? extends Value>, QueryEvaluationException> iter = func.evaluate(
				valueFactory, argValues);
		return new LookAheadIteration<BindingSet, QueryEvaluationException>() {

			@Override
			public BindingSet getNextElement()
				throws QueryEvaluationException
			{
				QueryBindingSet resultBindings;
				if (iter.hasNext()) {
					resultBindings = new QueryBindingSet(bindings);
					List<? extends Value> values = iter.next();
					if (resultVars.size() != values.size()) {
						throw new QueryEvaluationException(
								"Incorrect number of result vars: require " + values.size());
					}
					for (int i = 0; i < values.size(); i++) {
						Value result = values.get(i);
						Var resultVar = resultVars.get(i);
						Value varValue = resultVar.getValue();
						String varName = resultVar.getName();
						Value boundValue = bindings.getValue(varName);
						if ((varValue == null || result.equals(varValue))
								&& (boundValue == null || result.equals(boundValue)))
						{
							resultBindings.addBinding(varName, result);
						}
						else {
							resultBindings = null;
							break;
						}
					}
				}
				else {
					resultBindings = null;
				}
				return resultBindings;
			}

			@Override
			protected void handleClose()
				throws QueryEvaluationException
			{
				iter.close();
			}
		};
	}
}
