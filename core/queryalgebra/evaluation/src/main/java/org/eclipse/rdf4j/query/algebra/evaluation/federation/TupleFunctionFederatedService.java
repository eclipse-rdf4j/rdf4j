/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.TupleFunctionCall;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.TupleFunctionEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

/**
 * A federated service that can evaluate {@link TupleFunction}s.
 */
public class TupleFunctionFederatedService implements FederatedService {

	private final TupleFunctionRegistry tupleFunctionRegistry;

	private final ValueFactory vf;

	private volatile boolean isInitialized;

	public TupleFunctionFederatedService(TupleFunctionRegistry tupleFunctionRegistry, ValueFactory vf) {
		this.tupleFunctionRegistry = tupleFunctionRegistry;
		this.vf = vf;
	}

	@Override
	public boolean isInitialized() {
		return isInitialized;
	}

	@Override
	public void initialize() {
		isInitialized = true;
	}

	@Override
	public void shutdown() {
		isInitialized = false;
	}

	@Override
	public boolean ask(Service service, BindingSet bindings, String baseUri)
		throws QueryEvaluationException
	{
		final CloseableIteration<BindingSet, QueryEvaluationException> iter = evaluate(service,
				new SingletonIteration<BindingSet, QueryEvaluationException>(bindings), baseUri);
		try {
			while (iter.hasNext()) {
				BindingSet bs = iter.next();
				String firstVar = service.getBindingNames().iterator().next();
				return QueryEvaluationUtil.getEffectiveBooleanValue(bs.getValue(firstVar));
			}
		}
		finally {
			iter.close();
		}
		return false;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> select(Service service,
			final Set<String> projectionVars, BindingSet bindings, String baseUri)
		throws QueryEvaluationException
	{
		final CloseableIteration<BindingSet, QueryEvaluationException> iter = evaluate(service,
				new SingletonIteration<BindingSet, QueryEvaluationException>(bindings), baseUri);
		if (service.getBindingNames().equals(projectionVars)) {
			return iter;
		}

		return new CloseableIteration<BindingSet, QueryEvaluationException>() {

			@Override
			public boolean hasNext()
				throws QueryEvaluationException
			{
				return iter.hasNext();
			}

			@Override
			public BindingSet next()
				throws QueryEvaluationException
			{
				QueryBindingSet projected = new QueryBindingSet();
				BindingSet result = iter.next();
				for (String var : projectionVars) {
					Value v = result.getValue(var);
					projected.addBinding(var, v);
				}
				return projected;
			}

			@Override
			public void remove()
				throws QueryEvaluationException
			{
				iter.remove();
			}

			@Override
			public void close()
				throws QueryEvaluationException
			{
				iter.close();
			}
		};
	}

	@Override
	public final CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service service,
			CloseableIteration<BindingSet, QueryEvaluationException> bindings, String baseUri)
		throws QueryEvaluationException
	{
		if (!bindings.hasNext()) {
			return new EmptyIteration<BindingSet, QueryEvaluationException>();
		}

		TupleExpr expr = service.getArg();
		if (!(expr instanceof TupleFunctionCall)) {
			return new EmptyIteration<BindingSet, QueryEvaluationException>();
		}

		TupleFunctionCall funcCall = (TupleFunctionCall)expr;
		TupleFunction func = tupleFunctionRegistry.get(funcCall.getURI());

		if (func == null) {
			throw new QueryEvaluationException("Unknown tuple function '" + funcCall.getURI() + "'");
		}

		List<ValueExpr> argExprs = funcCall.getArgs();

		List<CloseableIteration<BindingSet, QueryEvaluationException>> resultIters = new ArrayList<CloseableIteration<BindingSet, QueryEvaluationException>>();
		while (bindings.hasNext()) {
			BindingSet bs = bindings.next();
			Value[] argValues = new Value[argExprs.size()];
			for (int i = 0; i < argExprs.size(); i++) {
				ValueExpr argExpr = argExprs.get(i);
				Value argValue;
				if (argExpr instanceof Var) {
					argValue = getValue((Var)argExpr, bs);
				}
				else if (argExpr instanceof ValueConstant) {
					argValue = ((ValueConstant)argExpr).getValue();
				}
				else {
					throw new ValueExprEvaluationException("Unsupported ValueExpr for argument " + i + ": "
							+ argExpr.getClass().getSimpleName());
				}
				argValues[i] = argValue;
			}
			resultIters.add(TupleFunctionEvaluationStrategy.evaluate(func, funcCall.getResultVars(), bs, vf,
					argValues));
		}
		return (resultIters.size() > 1)
				? new DistinctIteration<BindingSet, QueryEvaluationException>(
						new UnionIteration<BindingSet, QueryEvaluationException>(resultIters))
				: resultIters.get(0);
	}

	private static Value getValue(Var var, BindingSet bs)
		throws ValueExprEvaluationException
	{
		Value v = var.getValue();
		if (v == null) {
			v = bs.getValue(var.getName());
		}
		if (v == null) {
			throw new ValueExprEvaluationException("No value for binding: " + var.getName());
		}
		return v;
	}
}
