/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.TupleFunctionCall;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.XMLDatatypeMathUtil;

/**
 * SPARQL 1.1 extended query evaluation strategy. This strategy adds the use of virtual properties, as well as extended
 * comparison and mathematical operators to the minimally-conforming {@link StrictEvaluationStrategy}.
 *
 * @author Jeen Broekstra
 * @deprecated Use {@link DefaultEvaluationStrategy} instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class ExtendedEvaluationStrategy extends StrictEvaluationStrategy {

	private final TupleFunctionRegistry tupleFuncRegistry;

	public ExtendedEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncThreshold,
			EvaluationStatistics evaluationStatistics) {
		this(tripleSource, dataset, serviceResolver, TupleFunctionRegistry.getInstance(), iterationCacheSyncThreshold,
				evaluationStatistics);

		this.setQueryEvaluationMode(QueryEvaluationMode.STANDARD);
	}

	public ExtendedEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, TupleFunctionRegistry tupleFuncRegistry,
			long iterationCacheSyncThreshold, EvaluationStatistics evaluationStatistics) {
		super(tripleSource, dataset, serviceResolver, iterationCacheSyncThreshold, evaluationStatistics);
		this.tupleFuncRegistry = tupleFuncRegistry;
		this.setQueryEvaluationMode(QueryEvaluationMode.STANDARD);
	}

	public static CloseableIteration<BindingSet> evaluate(TupleFunction func,
			final List<Var> resultVars, final BindingSet bindings, ValueFactory valueFactory, Value... argValues)
			throws QueryEvaluationException {
		return new LookAheadIteration<>() {
			private final CloseableIteration<? extends List<? extends Value>> iter = func
					.evaluate(valueFactory, argValues);

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
				iter.close();
			}
		};
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Compare node, BindingSet bindings)
			throws QueryEvaluationException {
		Value leftVal = evaluate(node.getLeftArg(), bindings);
		Value rightVal = evaluate(node.getRightArg(), bindings);

		// return result of non-strict comparisson.
		return BooleanLiteral.valueOf(QueryEvaluationUtil.compare(leftVal, rightVal, node.getOperator(), false));
	}

	@Override
	protected QueryValueEvaluationStep prepare(Compare node, QueryEvaluationContext context) {
		return supplyBinaryValueEvaluation(node, (leftVal, rightVal) -> BooleanLiteral
				.valueOf(QueryEvaluationUtil.compare(leftVal, rightVal, node.getOperator(), false)), context);
	}

	private Value mathOperationApplier(MathExpr node, Value leftVal, Value rightVal) {
		if (leftVal.isLiteral() && rightVal.isLiteral()) {
			return XMLDatatypeMathUtil.compute((Literal) leftVal, (Literal) rightVal, node.getOperator());
		}

		throw new ValueExprEvaluationException("Both arguments must be literals");
	}

	@Override
	protected QueryValueEvaluationStep prepare(MathExpr node, QueryEvaluationContext context) {
		return supplyBinaryValueEvaluation(node, (leftVal, rightVal) -> mathOperationApplier(node, leftVal, rightVal),
				context);
	}

	@Deprecated(forRemoval = true)
	@Override
	public CloseableIteration<BindingSet> evaluate(TupleExpr expr, BindingSet bindings)
			throws QueryEvaluationException {
		if (expr instanceof TupleFunctionCall) {
			return precompile(expr).evaluate(bindings);
		} else {
			return super.evaluate(expr, bindings);
		}
	}

	@Override
	public QueryEvaluationStep precompile(TupleExpr expr, QueryEvaluationContext context)
			throws QueryEvaluationException {
		if (expr instanceof TupleFunctionCall) {
			return prepare((TupleFunctionCall) expr, context);
		} else {
			return super.precompile(expr, context);
		}
	}

	protected QueryEvaluationStep prepare(TupleFunctionCall expr, QueryEvaluationContext context)
			throws QueryEvaluationException {
		TupleFunction func = tupleFuncRegistry.get(expr.getURI())
				.orElseThrow(() -> new QueryEvaluationException("Unknown tuple function '" + expr.getURI() + "'"));

		List<ValueExpr> args = expr.getArgs();
		QueryValueEvaluationStep[] argEpresions = new QueryValueEvaluationStep[args.size()];
		for (int i = 0; i < args.size(); i++) {
			argEpresions[i] = precompile(args.get(i), context);
		}

		return new QueryEvaluationStep() {

			@Override
			public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
				Value[] argValues = new Value[args.size()];
				for (int i = 0; i < args.size(); i++) {
					argValues[i] = argEpresions[i].evaluate(bindings);
				}

				return ExtendedEvaluationStrategy.evaluate(func, expr.getResultVars(), bindings,
						tripleSource.getValueFactory(), argValues);
			}
		};
	}
}
