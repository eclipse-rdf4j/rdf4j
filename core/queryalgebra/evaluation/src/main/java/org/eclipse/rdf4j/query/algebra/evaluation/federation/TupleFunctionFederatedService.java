/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.SilentIteration;
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
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
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

	private boolean isInitialized;

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
	public boolean ask(Service service, BindingSet bindings, String baseUri) throws QueryEvaluationException {
		try (final CloseableIteration<BindingSet, QueryEvaluationException> iter = evaluate(service,
				new SingletonIteration<>(bindings), baseUri)) {
			if (iter.hasNext()) {
				BindingSet bs = iter.next();
				String firstVar = service.getBindingNames().iterator().next();
				return QueryEvaluationUtil.getEffectiveBooleanValue(bs.getValue(firstVar));
			}
		}
		return false;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> select(Service service,
			final Set<String> projectionVars, BindingSet bindings, String baseUri) throws QueryEvaluationException {
		final CloseableIteration<BindingSet, QueryEvaluationException> iter, eval;
		eval = evaluate(service, new SingletonIteration<>(bindings), baseUri);
		iter = service.isSilent() ? new SilentIteration<>(eval) : eval;
		if (service.getBindingNames().equals(projectionVars)) {
			return iter;
		}

		return new AbstractCloseableIteration<>() {

			@Override
			public boolean hasNext() throws QueryEvaluationException {
				if (isClosed()) {
					return false;
				}
				boolean result = iter.hasNext();
				if (!result) {
					close();
				}
				return result;
			}

			@Override
			public BindingSet next() throws QueryEvaluationException {
				if (isClosed()) {
					throw new NoSuchElementException("The iteration has been closed.");
				}
				try {
					QueryBindingSet projected = new QueryBindingSet();
					BindingSet result = iter.next();
					for (String var : projectionVars) {
						Value v = result.getValue(var);
						projected.addBinding(var, v);
					}
					return projected;
				} catch (NoSuchElementException e) {
					close();
					throw e;
				}
			}

			@Override
			public void remove() throws QueryEvaluationException {
				if (isClosed()) {
					throw new IllegalStateException("The iteration has been closed.");
				}
				try {
					iter.remove();
				} catch (IllegalStateException e) {
					close();
					throw e;
				}
			}

			@Override
			protected void handleClose() throws QueryEvaluationException {
				iter.close();
			}
		};
	}

	@Override
	public final CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service service,
			CloseableIteration<BindingSet, QueryEvaluationException> bindings, String baseUri)
			throws QueryEvaluationException {
		if (!bindings.hasNext()) {
			return QueryEvaluationStep.EMPTY_ITERATION;
		}

		TupleExpr expr = service.getArg();
		if (!(expr instanceof TupleFunctionCall)) {
			return QueryEvaluationStep.EMPTY_ITERATION;
		}

		TupleFunctionCall funcCall = (TupleFunctionCall) expr;
		TupleFunction func = tupleFunctionRegistry.get(funcCall.getURI())
				.orElseThrow(() -> new QueryEvaluationException("Unknown tuple function '" + funcCall.getURI() + "'"));

		List<ValueExpr> argExprs = funcCall.getArgs();

		List<CloseableIteration<BindingSet, QueryEvaluationException>> resultIters = new ArrayList<>();
		while (bindings.hasNext()) {
			BindingSet bs = bindings.next();
			Value[] argValues = new Value[argExprs.size()];
			for (int i = 0; i < argExprs.size(); i++) {
				ValueExpr argExpr = argExprs.get(i);
				Value argValue;
				if (argExpr instanceof Var) {
					argValue = getValue((Var) argExpr, bs);
				} else if (argExpr instanceof ValueConstant) {
					argValue = ((ValueConstant) argExpr).getValue();
				} else {
					throw new ValueExprEvaluationException(
							"Unsupported ValueExpr for argument " + i + ": " + argExpr.getClass().getSimpleName());
				}
				argValues[i] = argValue;
			}
			resultIters
					.add(TupleFunctionEvaluationStrategy.evaluate(func, funcCall.getResultVars(), bs, vf, argValues));
		}
		return (resultIters.size() > 1) ? new DistinctIteration<>(new UnionIteration<>(resultIters))
				: resultIters.get(0);
	}

	private static Value getValue(Var var, BindingSet bs) throws ValueExprEvaluationException {
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
