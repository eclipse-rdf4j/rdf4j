/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.impl.DefaultCollectionFactory;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.iteration.ReducedIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.BinaryValueOperator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Coalesce;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.CompareAll;
import org.eclipse.rdf4j.query.algebra.CompareAny;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.In;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsResource;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Label;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.LocalName;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Namespace;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.TupleFunctionCall;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.UnaryValueOperator;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.ValueExprTripleRef;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep.ConstantQueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.RDFStarTripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.function.datetime.Now;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.BindingSetAssignmentQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.IntersectionQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.JoinQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.LeftJoinQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.MinusQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.OrderQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.ProjectionQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.RdfStarQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.RegexValueEvaluationStepSupplier;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.ReificationRdfStarQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.ServiceQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.SliceQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.StatementPatternQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.UnionQueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.ZeroLengthPathEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.AndValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.CompareAllQueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.CompareAnyValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.ExistsQueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.IfValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.InValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.ListMemberValueOperationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.OrValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.QueryValueEvaluationStepSupplier;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.ValueExprTripleRefEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.DescribeIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ExtensionIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.FilterIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.GroupIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.MultiProjectionIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.PathIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.util.MathUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.OrderComparator;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.query.algebra.evaluation.util.XMLDatatypeMathUtil;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

import com.google.common.base.Stopwatch;

/**
 * Default SPARQL 1.1 Query Evaluation strategy, to evaluate one {@link TupleExpr} on the given {@link TripleSource},
 * optionally using the given {@link Dataset}. The behaviour of this strategy can be modified by setting the
 * {@link #setQueryEvaluationMode(QueryEvaluationMode) QueryEvaluationMode}, which determines if the SPARQL query is
 * evaluated using {@link QueryEvaluationMode#STRICT strict} compliance or {@link QueryEvaluationMode#STANDARD standard}
 * compliance.
 *
 * @author Jeen Broekstra
 * @author James Leigh
 * @author Arjohn Kampman
 * @author David Huynh
 * @author Andreas Schwarte
 */
public class DefaultEvaluationStrategy implements EvaluationStrategy, FederatedServiceResolverClient {

	protected final TripleSource tripleSource;

	protected final Dataset dataset;

	protected FederatedServiceResolver serviceResolver;

	// shared return value for successive calls of the NOW() function within the
	// same query. Will be reset upon each new query being evaluated. See
	// SES-869.
	private Literal sharedValueOfNow;

	private final long iterationCacheSyncThreshold;

	// track the results size that each node in the query plan produces during execution
	private boolean trackResultSize;

	// track the exeution time of each node in the plan
	private boolean trackTime;

	private UUID uuid;

	private QueryOptimizerPipeline pipeline;

	private final TupleFunctionRegistry tupleFuncRegistry;

	private QueryEvaluationMode queryEvaluationMode;

	private Supplier<CollectionFactory> collectionFactory = DefaultCollectionFactory::new;

	protected static CloseableIteration<BindingSet> evaluate(TupleFunction func,
			final List<Var> resultVars, final BindingSet bindings, ValueFactory valueFactory, Value... argValues)
			throws QueryEvaluationException {
		final CloseableIteration<? extends List<? extends Value>> iter = func
				.evaluate(valueFactory, argValues);
		return new LookAheadIteration<BindingSet>() {

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

	public DefaultEvaluationStrategy(TripleSource tripleSource, FederatedServiceResolver serviceResolver) {
		this(tripleSource, null, serviceResolver);
	}

	public DefaultEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver) {
		this(tripleSource, dataset, serviceResolver, 0, new EvaluationStatistics());
	}

	public DefaultEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold,
			EvaluationStatistics evaluationStatistics) {
		this(tripleSource, dataset, serviceResolver, iterationCacheSyncTreshold, evaluationStatistics, false);
	}

	public DefaultEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold,
			EvaluationStatistics evaluationStatistics, boolean trackResultSize) {
		this(tripleSource, dataset, serviceResolver, iterationCacheSyncTreshold, evaluationStatistics, trackResultSize,
				TupleFunctionRegistry.getInstance());
	}

	public DefaultEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold,
			EvaluationStatistics evaluationStatistics, boolean trackResultSize,
			TupleFunctionRegistry tupleFunctionRegistry) {
		this.tripleSource = tripleSource;
		this.dataset = dataset;
		this.serviceResolver = serviceResolver;
		this.iterationCacheSyncThreshold = iterationCacheSyncTreshold;
		this.pipeline = new org.eclipse.rdf4j.query.algebra.evaluation.optimizer.StandardQueryOptimizerPipeline(this,
				tripleSource, evaluationStatistics);
		this.trackResultSize = trackResultSize;
		this.tupleFuncRegistry = tupleFunctionRegistry;
		this.setQueryEvaluationMode(QueryEvaluationMode.STANDARD);
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		serviceResolver = resolver;
	}

	@Override
	public FederatedServiceResolver getFederatedServiceResolver() {
		return serviceResolver;
	}

	@Override
	public FederatedService getService(String serviceUrl) throws QueryEvaluationException {
		return serviceResolver.getService(serviceUrl);
	}

	@Override
	public void setOptimizerPipeline(QueryOptimizerPipeline pipeline) {
		Objects.requireNonNull(pipeline);
		this.pipeline = pipeline;
	}

	/**
	 * Execute the {@link QueryOptimizerPipeline} on the given {@link TupleExpr} to optimize its execution plan.
	 *
	 * @param expr                 the {@link TupleExpr} to optimize.
	 * @param evaluationStatistics this param is ignored!
	 * @param bindings             a-priori bindings supplied for the query, which can potentially be inlined.
	 * @return the optimized {@link TupleExpr}.
	 * @see #setOptimizerPipeline(QueryOptimizerPipeline)
	 * @since 3.0
	 */
	@Override
	public TupleExpr optimize(TupleExpr expr, EvaluationStatistics evaluationStatistics, BindingSet bindings) {

		for (QueryOptimizer optimizer : pipeline.getOptimizers()) {
			optimizer.optimize(expr, dataset, bindings);
		}
		return expr;
	}

	@Deprecated(forRemoval = true) // this method is still in use and I think that there is quite a lot of work left
	// before it can be removed
	@Override
	public CloseableIteration<BindingSet> evaluate(TupleExpr expr, BindingSet bindings)
			throws QueryEvaluationException {

		CloseableIteration<BindingSet> result = null;
		try {
			if (expr instanceof StatementPattern) {
				result = precompile(expr).evaluate(bindings);
			} else if (expr instanceof UnaryTupleOperator) {
				if (expr instanceof Projection) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof MultiProjection) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Filter) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Service) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Slice) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Extension) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Distinct) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Reduced) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Group) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Order) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof QueryRoot) {
					// new query, reset shared return value for successive calls of
					// NOW()
					this.sharedValueOfNow = null;
					result = evaluate(((UnaryTupleOperator) expr).getArg(), bindings);
				} else if (expr instanceof DescribeOperator) {
					result = precompile(expr).evaluate(bindings);
				} else {
					throw new QueryEvaluationException(
							"Unknown unary tuple operator type: " + ((UnaryTupleOperator) expr).getClass());
				}
			} else if (expr instanceof BinaryTupleOperator) {
				if (expr instanceof Join) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof LeftJoin) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Union) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Intersection) {
					result = precompile(expr).evaluate(bindings);
				} else if (expr instanceof Difference) {
					result = precompile(expr).evaluate(bindings);
				} else {
					throw new QueryEvaluationException(
							"Unsupported binary tuple operator type: " + ((BinaryTupleOperator) expr).getClass());
				}
			} else if (expr instanceof SingletonSet) {
				result = precompile(expr).evaluate(bindings);
			} else if (expr instanceof EmptySet) {
				result = QueryEvaluationStep.EMPTY_ITERATION;
			} else if (expr instanceof ZeroLengthPath) {
				result = precompile(expr).evaluate(bindings);
			} else if (expr instanceof ArbitraryLengthPath) {
				result = precompile(expr).evaluate(bindings);
			} else if (expr instanceof BindingSetAssignment) {
				result = precompile(expr).evaluate(bindings);
			} else if (expr instanceof TripleRef) {
				result = evaluate((TripleRef) expr, bindings);
			} else if (expr instanceof TupleFunctionCall) {
				if (getQueryEvaluationMode().compareTo(QueryEvaluationMode.STANDARD) < 0) {
					throw new QueryEvaluationException(
							"Tuple function call not supported in query evaluation mode " + getQueryEvaluationMode());
				}
				return evaluate(expr, bindings);
			} else if (expr == null) {
				throw new IllegalArgumentException("expr must not be null");
			} else {
				throw new QueryEvaluationException("Unsupported tuple expr type: " + expr.getClass());
			}

			if (trackTime) {
				// set resultsSizeActual to at least be 0 so we can track iterations that don't procude anything
				expr.setTotalTimeNanosActual(Math.max(0, expr.getTotalTimeNanosActual()));
				result = new TimedIterator(result, expr);
			}

			if (trackResultSize) {
				// set resultsSizeActual to at least be 0 so we can track iterations that don't procude anything
				expr.setResultSizeActual(Math.max(0, expr.getResultSizeActual()));
				result = new ResultSizeCountingIterator(result, expr);
			}
			return result;
		} catch (Throwable t) {
			if (result != null) {
				result.close();
			}
			throw t;
		}
	}

	@Override
	public QueryEvaluationStep precompile(TupleExpr expr) {
		QueryEvaluationContext context = new QueryEvaluationContext.Minimal(dataset, tripleSource.getValueFactory(),
				tripleSource.getComparator());
		if (expr instanceof QueryRoot) {
			String[] allVariables = ArrayBindingBasedQueryEvaluationContext
					.findAllVariablesUsedInQuery((QueryRoot) expr);
			context = new ArrayBindingBasedQueryEvaluationContext(context, allVariables, tripleSource.getComparator());
		}
		return precompile(expr, context);
	}

	@Override
	public QueryEvaluationStep precompile(TupleExpr expr, QueryEvaluationContext context) {
		QueryEvaluationStep ret;

		if (expr instanceof StatementPattern) {
			ret = prepare((StatementPattern) expr, context);
		} else if (expr instanceof UnaryTupleOperator) {
			ret = prepare((UnaryTupleOperator) expr, context);
		} else if (expr instanceof BinaryTupleOperator) {
			ret = prepare((BinaryTupleOperator) expr, context);
		} else if (expr instanceof SingletonSet) {
			ret = prepare((SingletonSet) expr, context);
		} else if (expr instanceof EmptySet) {
			ret = prepare((EmptySet) expr, context);
		} else if (expr instanceof ZeroLengthPath) {
			ret = prepare((ZeroLengthPath) expr, context);
		} else if (expr instanceof ArbitraryLengthPath) {
			ret = prepare((ArbitraryLengthPath) expr, context);
		} else if (expr instanceof BindingSetAssignment) {
			ret = prepare((BindingSetAssignment) expr, context);
		} else if (expr instanceof TripleRef) {
			ret = prepare((TripleRef) expr, context);
		} else if (expr instanceof TupleFunctionCall) {
			ret = prepare((TupleFunctionCall) expr, context);
		} else if (expr == null) {
			throw new IllegalArgumentException("expr must not be null");
		} else {
			throw new QueryEvaluationException("Unsupported tuple expr type: " + expr.getClass());
		}

		if (ret != null) {
			if (trackTime) {
				ret = trackTime(expr, ret);
			}
			if (trackResultSize) {
				ret = trackResultSize(expr, ret);
			}
			return ret;
		} else {
			return QueryEvaluationStep.minimal(this, expr);
		}
	}

	private QueryEvaluationStep trackResultSize(TupleExpr expr, QueryEvaluationStep qes) {
		return QueryEvaluationStep.wrap(qes, iter -> {
			expr.setResultSizeActual(Math.max(0, expr.getResultSizeActual()));
			return new ResultSizeCountingIterator(iter, expr);
		});
	}

	private QueryEvaluationStep trackTime(TupleExpr expr, QueryEvaluationStep qes) {
		return QueryEvaluationStep.wrap(qes, iter -> {
			expr.setTotalTimeNanosActual(Math.max(0, expr.getTotalTimeNanosActual()));
			return new TimedIterator(iter, expr);
		});
	}

	protected QueryEvaluationStep prepare(ArbitraryLengthPath alp, QueryEvaluationContext context)
			throws QueryEvaluationException {
		final Scope scope = alp.getScope();
		final Var subjectVar = alp.getSubjectVar();
		final TupleExpr pathExpression = alp.getPathExpression();
		final Var objVar = alp.getObjectVar();
		final Var contextVar = alp.getContextVar();
		final long minLength = alp.getMinLength();
		return bindings -> new PathIteration(DefaultEvaluationStrategy.this, scope, subjectVar, pathExpression, objVar,
				contextVar, minLength, bindings);
	}

	protected QueryEvaluationStep prepare(ZeroLengthPath zlp, QueryEvaluationContext context)
			throws QueryEvaluationException {

		final Var subjectVar = zlp.getSubjectVar();
		final Var objVar = zlp.getObjectVar();
		final Var contextVar = zlp.getContextVar();
		QueryValueEvaluationStep subPrep = precompile(subjectVar, context);
		QueryValueEvaluationStep objPrep = precompile(objVar, context);

		return new ZeroLengthPathEvaluationStep(subjectVar, objVar, contextVar, subPrep, objPrep, this, context);
	}

	protected QueryEvaluationStep prepare(Difference node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return new MinusQueryEvaluationStep(precompile(node.getLeftArg(), context),
				precompile(node.getRightArg(), context));
	}

	protected QueryEvaluationStep prepare(Group node, QueryEvaluationContext context) throws QueryEvaluationException {
		return bindings -> new GroupIterator(DefaultEvaluationStrategy.this, node, bindings,
				iterationCacheSyncThreshold,
				context);
	}

	protected QueryEvaluationStep prepare(Intersection node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep leftArg = precompile(node.getLeftArg(), context);
		QueryEvaluationStep rightArg = precompile(node.getRightArg(), context);
		return new IntersectionQueryEvaluationStep(leftArg, rightArg, this::makeSet);
	}

	protected QueryEvaluationStep prepare(Join node, QueryEvaluationContext context) throws QueryEvaluationException {
		return new JoinQueryEvaluationStep(this, node, context);
	}

	protected QueryEvaluationStep prepare(LeftJoin node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return LeftJoinQueryEvaluationStep.supply(this, node, context);
	}

	protected QueryEvaluationStep prepare(MultiProjection node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep arg = precompile(node.getArg(), context);
		return bindings -> {
			CloseableIteration<BindingSet> evaluate = null;
			try {
				evaluate = arg.evaluate(bindings);
				return new MultiProjectionIterator(node, evaluate, bindings);
			} catch (Throwable t) {
				if (evaluate != null) {
					evaluate.close();
				}
				throw t;
			}
		};
	}

	protected QueryEvaluationStep prepare(Projection node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep temp = precompile(node.getArg(), context);
		return new ProjectionQueryEvaluationStep(node, temp, context);
	}

	protected QueryEvaluationStep prepare(QueryRoot node, QueryEvaluationContext context)
			throws QueryEvaluationException {

		QueryEvaluationStep arg = precompile(node.getArg(), context);
		return new QueryRootQueryEvaluationStep(arg, context);
	}

	protected QueryEvaluationStep prepare(StatementPattern node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return new StatementPatternQueryEvaluationStep(node, context, tripleSource);
	}

	protected QueryEvaluationStep prepare(Union node, QueryEvaluationContext context) throws QueryEvaluationException {
		QueryEvaluationStep leftQes = precompile(node.getLeftArg(), context);
		QueryEvaluationStep rightQes = precompile(node.getRightArg(), context);

		return new UnionQueryEvaluationStep(leftQes, rightQes);
	}

	protected QueryEvaluationStep prepare(Slice node, QueryEvaluationContext context) throws QueryEvaluationException {
		QueryEvaluationStep arg = precompile(node.getArg(), context);
		return SliceQueryEvaluationStep.supply(node, arg);
	}

	protected QueryEvaluationStep prepare(Extension node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep arg = precompile(node.getArg(), context);
		Consumer<MutableBindingSet> consumer = ExtensionIterator.buildLambdaToEvaluateTheExpressions(node, this,
				context);
		return new ExtensionQueryEvaluationStep(arg, consumer, context);
	}

	protected QueryEvaluationStep prepare(Service service, QueryEvaluationContext context)
			throws QueryEvaluationException {
		Var serviceRef = service.getServiceRef();
		return new ServiceQueryEvaluationStep(service, serviceRef, serviceResolver);
	}

	protected QueryEvaluationStep prepare(Filter node, QueryEvaluationContext context) throws QueryEvaluationException {

		return FilterIterator.supply(node, DefaultEvaluationStrategy.this, context);

	}

	protected QueryEvaluationStep prepare(Order node, QueryEvaluationContext context) throws QueryEvaluationException {
		ValueComparator vcmp = new ValueComparator();
		OrderComparator cmp = new OrderComparator(this, node, vcmp, context);
		boolean reduced = isReducedOrDistinct(node);
		long limit = getLimit(node);
		QueryEvaluationStep preparedArg = precompile(node.getArg(), context);
		return new OrderQueryEvaluationStep(cmp, limit, reduced, preparedArg, iterationCacheSyncThreshold);
	}

	protected QueryEvaluationStep prepare(BindingSetAssignment node, QueryEvaluationContext context)
			throws QueryEvaluationException {

		return new BindingSetAssignmentQueryEvaluationStep(node, context);
	}

	private final class QueryRootQueryEvaluationStep implements QueryEvaluationStep {
		private final QueryEvaluationStep arg;
		private final QueryEvaluationContext context;

		private QueryRootQueryEvaluationStep(QueryEvaluationStep arg, QueryEvaluationContext context) {
			this.arg = arg;
			this.context = context;
		}

		@Override
		public CloseableIteration<BindingSet> evaluate(BindingSet bs) {
			// TODO fix the sharing of the now element to be safe
			DefaultEvaluationStrategy.this.sharedValueOfNow = null;
			CloseableIteration<BindingSet> evaluate = null;
			try {
				evaluate = arg.evaluate(bs);
				var eval = evaluate;

				CloseableIteration<BindingSet> closeContext = new CloseableIteration<>() {

					@Override
					public boolean hasNext() throws QueryEvaluationException {
						return eval.hasNext();
					}

					@Override
					public BindingSet next() throws QueryEvaluationException {
						return eval.next();
					}

					@Override
					public void remove() throws QueryEvaluationException {
						eval.remove();

					}

					@Override
					public void close() throws QueryEvaluationException {
						eval.close();
					}
				};
				return closeContext;
			} catch (Throwable t) {
				if (evaluate != null) {
					evaluate.close();
				}
				throw t;
			}
		}

	}

	protected QueryEvaluationStep prepare(DescribeOperator node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep child = precompile(node.getArg(), context);
		return bs -> {
			CloseableIteration<BindingSet> evaluate = null;

			try {
				evaluate = child.evaluate(bs);
				return new DescribeIteration(evaluate, DefaultEvaluationStrategy.this, node.getBindingNames(), bs);
			} catch (Throwable t) {
				if (evaluate != null) {
					evaluate.close();
				}
				throw t;
			}

		};
	}

	protected QueryEvaluationStep prepare(Distinct node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		final QueryEvaluationStep child = precompile(node.getArg(), context);
		return bindings -> {
			final CloseableIteration<BindingSet> evaluate = child.evaluate(bindings);
			return new DistinctIteration<BindingSet>(evaluate,
					DefaultEvaluationStrategy.this::makeSet);
		};

	}

	protected QueryEvaluationStep prepare(Reduced node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep arg = precompile(node.getArg(), context);
		return bindings -> new ReducedIteration<>(arg.evaluate(bindings));
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

		return bindings -> {
			Value[] argValues = new Value[args.size()];
			for (int i = 0; i < args.size(); i++) {
				argValues[i] = argEpresions[i].evaluate(bindings);
			}

			return evaluate(func, expr.getResultVars(), bindings,
					tripleSource.getValueFactory(), argValues);
		};
	}

	public static Value getVarValue(Var var, BindingSet bindings) {
		if (var == null) {
			return null;
		} else if (var.hasValue()) {
			return var.getValue();
		} else {
			return bindings.getValue(var.getName());
		}
	}

	protected QueryEvaluationStep prepare(UnaryTupleOperator expr, QueryEvaluationContext context)
			throws QueryEvaluationException {
		if (expr instanceof Projection) {
			return prepare((Projection) expr, context);
		} else if (expr instanceof MultiProjection) {
			return prepare((MultiProjection) expr, context);
		} else if (expr instanceof Filter) {
			return prepare((Filter) expr, context);
		} else if (expr instanceof Service) {
			return prepare((Service) expr, context);
		} else if (expr instanceof Slice) {
			return prepare((Slice) expr, context);
		} else if (expr instanceof Extension) {
			return prepare((Extension) expr, context);
		} else if (expr instanceof Distinct) {
			return prepare((Distinct) expr, context);
		} else if (expr instanceof Reduced) {
			return prepare((Reduced) expr, context);
		} else if (expr instanceof Group) {
			return prepare((Group) expr, context);
		} else if (expr instanceof Order) {
			return prepare((Order) expr, context);
		} else if (expr instanceof QueryRoot) {
			// new query, reset shared return value for successive calls of
			// NOW()
			this.sharedValueOfNow = null;
			return precompile(expr.getArg(), context);
		} else if (expr instanceof DescribeOperator) {
			return prepare((DescribeOperator) expr, context);
		} else if (expr == null) {
			throw new IllegalArgumentException("expr must not be null");
		} else {
			throw new QueryEvaluationException("Unknown unary tuple operator type: " + expr.getClass());
		}
	}

	protected QueryEvaluationStep prepare(BinaryTupleOperator expr, QueryEvaluationContext context)
			throws QueryEvaluationException {
		if (expr instanceof Join) {
			return prepare((Join) expr, context);
		} else if (expr instanceof LeftJoin) {
			return prepare((LeftJoin) expr, context);
		} else if (expr instanceof Union) {
			return prepare((Union) expr, context);
		} else if (expr instanceof Intersection) {
			return prepare((Intersection) expr, context);
		} else if (expr instanceof Difference) {
			return prepare((Difference) expr, context);
		} else if (expr == null) {
			throw new IllegalArgumentException("expr must not be null");
		} else {
			throw new QueryEvaluationException("Unsupported binary tuple operator type: " + expr.getClass());
		}
	}

	protected QueryEvaluationStep prepare(SingletonSet singletonSet, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return SingletonIteration::new;

	}

	protected QueryEvaluationStep prepare(EmptySet emptySet, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return bindings -> QueryEvaluationStep.EMPTY_ITERATION;
	}

	@Override
	public QueryValueEvaluationStep precompile(ValueExpr expr,
			QueryEvaluationContext context)
			throws QueryEvaluationException {
		if (expr instanceof Var) {
			return prepare((Var) expr, context);
		} else if (expr instanceof ValueConstant) {
			return prepare((ValueConstant) expr, context);
		} else if (expr instanceof BNodeGenerator) {
			return prepare((BNodeGenerator) expr, context);
		} else if (expr instanceof Bound) {
			return prepare((Bound) expr, context);
//			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Str) {
			return prepare((Str) expr, context);
		} else if (expr instanceof Label) {
			return prepare((Label) expr, context);
		} else if (expr instanceof Lang) {
			return prepare((Lang) expr, context);
		} else if (expr instanceof LangMatches) {
			return prepare((LangMatches) expr, context);
		} else if (expr instanceof Datatype) {
			return prepare((Datatype) expr, context);
		} else if (expr instanceof Namespace) {
			return prepare((Namespace) expr, context);
		} else if (expr instanceof LocalName) {
			return prepare((LocalName) expr, context);
		} else if (expr instanceof IsResource) {
			return prepare((IsResource) expr, context);
		} else if (expr instanceof IsURI) {
			return prepare((IsURI) expr, context);
		} else if (expr instanceof IsBNode) {
			return prepare((IsBNode) expr, context);
		} else if (expr instanceof IsLiteral) {
			return prepare((IsLiteral) expr, context);
		} else if (expr instanceof IsNumeric) {
			return prepare((IsNumeric) expr, context);
		} else if (expr instanceof IRIFunction) {
			return prepare((IRIFunction) expr, context);
		} else if (expr instanceof Regex) {
			return prepare((Regex) expr, context);
		} else if (expr instanceof Coalesce) {
			return prepare((Coalesce) expr, context);
		} else if (expr instanceof FunctionCall) {
			return prepare((FunctionCall) expr, context);
		} else if (expr instanceof And) {
			return prepare((And) expr, context);
		} else if (expr instanceof Or) {
			return prepare((Or) expr, context);
		} else if (expr instanceof Not) {
			return prepare((Not) expr, context);
		} else if (expr instanceof SameTerm) {
			return prepare((SameTerm) expr, context);
		} else if (expr instanceof Compare) {
			return prepare((Compare) expr, context);
		} else if (expr instanceof MathExpr) {
			return prepare((MathExpr) expr, context);
		} else if (expr instanceof In) {
			return prepare((In) expr, context);
		} else if (expr instanceof CompareAny) {
			return prepare((CompareAny) expr, context);
		} else if (expr instanceof CompareAll) {
			return prepare((CompareAll) expr, context);
		} else if (expr instanceof Exists) {
			return prepare((Exists) expr, context);
		} else if (expr instanceof If) {
			return prepare((If) expr, context);
		} else if (expr instanceof ListMemberOperator) {
			return prepare((ListMemberOperator) expr, context);
		} else if (expr instanceof ValueExprTripleRef) {
			return prepare((ValueExprTripleRef) expr, context);
		} else if (expr == null) {
			throw new IllegalArgumentException("expr must not be null");
		} else {
			throw new QueryEvaluationException("Unsupported value expr type: " + expr.getClass());
		}
	}

	@Deprecated(forRemoval = true)
	@Override
	public Value evaluate(ValueExpr expr, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(expr,
				new QueryEvaluationContext.Minimal(DefaultEvaluationStrategy.this.sharedValueOfNow, dataset,
						tripleSource.getComparator()))
				.evaluate(bindings);
	}

	protected QueryValueEvaluationStep prepare(Var var, QueryEvaluationContext context)
			throws QueryEvaluationException {

		Value value = var.getValue();

		if (value != null) {
			return new ConstantQueryValueEvaluationStep(value);
		} else {
			java.util.function.Function<BindingSet, Value> getValue = context.getValue(var.getName());
			Predicate<BindingSet> hasValue = context.hasBinding(var.getName());
			return bindings -> {
				if (hasValue.test(bindings)) {
					return getValue.apply(bindings);
				} else {
					throw new ValueExprEvaluationException();
				}
			};
		}

	}

	protected QueryValueEvaluationStep prepare(ValueConstant valueConstant, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return new ConstantQueryValueEvaluationStep(valueConstant);
	}

	protected QueryValueEvaluationStep prepare(BNodeGenerator node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		ValueFactory vf = tripleSource.getValueFactory();
		ValueExpr nodeIdExpr = node.getNodeIdExpr();
		if (nodeIdExpr != null) {
			QueryValueEvaluationStep nodeVes = precompile(nodeIdExpr, context);
			return QueryValueEvaluationStepSupplier.bnode(nodeVes, vf);
		} else {
			return new QueryValueEvaluationStep.ApplyFunctionForEachBinding(bs -> vf.createBNode());
		}
	}

	protected QueryValueEvaluationStep prepare(Bound node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		try {
			QueryValueEvaluationStep arg = precompile(node.getArg(), context);
			return QueryValueEvaluationStepSupplier.prepareBound(arg, context);
		} catch (QueryEvaluationException e) {
			return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.FALSE);
		}
	}

	protected QueryValueEvaluationStep prepare(Str node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		ValueFactory valueFactory = tripleSource.getValueFactory();
		return QueryValueEvaluationStepSupplier.prepareStr(arg, valueFactory);
	}

	protected QueryValueEvaluationStep prepare(Label node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareLabel(arg, tripleSource.getValueFactory());
	}

	protected QueryValueEvaluationStep prepare(Lang node, QueryEvaluationContext context) {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareLang(arg, tripleSource.getValueFactory());
	}

	public Value evaluate(Datatype node, BindingSet bindings) throws QueryEvaluationException {
		return prepare(node, new QueryEvaluationContext.Minimal(dataset, tripleSource.getComparator()))
				.evaluate(bindings);
	}

	protected QueryValueEvaluationStep prepare(Datatype node, QueryEvaluationContext context) {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareDatatype(arg, context);
	}

	protected QueryValueEvaluationStep prepare(Namespace node, QueryEvaluationContext context) {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareNamespace(arg, tripleSource.getValueFactory());
	}

	protected QueryValueEvaluationStep prepare(LocalName node, QueryEvaluationContext context) {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareLocalName(arg, tripleSource.getValueFactory());
	}

	protected QueryValueEvaluationStep prepare(IsResource node, QueryEvaluationContext context) {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareIs(arg, v -> v instanceof Resource);
	}

	protected QueryValueEvaluationStep prepare(IsURI node, QueryEvaluationContext context) {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareIs(arg, v -> v instanceof IRI);
	}

	protected QueryValueEvaluationStep prepare(IsBNode node, QueryEvaluationContext context) {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareIs(arg, v -> v instanceof BNode);
	}

	protected QueryValueEvaluationStep prepare(IsLiteral node, QueryEvaluationContext context) {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareIs(arg, v -> v instanceof Literal);
	}

	protected QueryValueEvaluationStep prepare(IsNumeric node, QueryEvaluationContext context) {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareIs(arg, DefaultEvaluationStrategy::isNumeric);
	}

	private static boolean isNumeric(Value argValue) {
		if (argValue instanceof Literal) {
			Literal lit = (Literal) argValue;
			CoreDatatype.XSD datatype = lit.getCoreDatatype().asXSDDatatypeOrNull();
			return datatype != null && datatype.isNumericDatatype();
		} else {
			return false;
		}
	}

	protected QueryValueEvaluationStep prepare(IRIFunction node, QueryEvaluationContext context) {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		return QueryValueEvaluationStepSupplier.prepareIriFunction(node, arg, tripleSource.getValueFactory());
	}

	/**
	 * Determines whether the two operands match according to the <code>regex</code> operator.
	 *
	 * @return <var>true</var> if the operands match according to the <var>regex</var> operator, <var>false</var>
	 *         otherwise.
	 */
	@Deprecated(forRemoval = true)
	public Value evaluate(Regex node, BindingSet bindings)
			throws QueryEvaluationException {
		return prepare(node,
				new QueryEvaluationContext.Minimal(sharedValueOfNow, dataset, tripleSource.getComparator()))
				.evaluate(bindings);
	}

	/**
	 * Determines whether the two operands match according to the <code>regex</code> operator.
	 *
	 * @return <var>true</var> if the operands match according to the <var>regex</var> operator, <var>false</var>
	 *         otherwise.
	 */
	protected QueryValueEvaluationStep prepare(Regex node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return RegexValueEvaluationStepSupplier.make(this, node, context);
	}

	protected QueryValueEvaluationStep prepare(LangMatches node, QueryEvaluationContext context) {
		return supplyBinaryValueEvaluation(node, this::evaluateLangMatch, context);
	}

	private Value evaluateLangMatch(Value langTagValue, Value langRangeValue) {
		if (QueryEvaluationUtility.isSimpleLiteral(langTagValue)
				&& QueryEvaluationUtility.isSimpleLiteral(langRangeValue)) {
			String langTag = ((Literal) langTagValue).getLabel();
			String langRange = ((Literal) langRangeValue).getLabel();

			boolean result = Literals.langMatches(langTag, langRange);

			return BooleanLiteral.valueOf(result);
		}

		throw new ValueExprEvaluationException();
	}

	public QueryValueEvaluationStep prepare(FunctionCall node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		Function function = FunctionRegistry.getInstance()
				.get(node.getURI())
				.orElseThrow(() -> new QueryEvaluationException("Unknown function '" + node.getURI() + "'"));

		// the NOW function is a special case as it needs to keep a shared
		// return
		// value for the duration of the query.
		if (function instanceof Now) {
			return prepare((Now) function, context);
		}

		List<ValueExpr> args = node.getArgs();

		QueryValueEvaluationStep[] argSteps = new QueryValueEvaluationStep[args.size()];

		boolean allConstant = determineIfFunctionCallWillBeAConstant(context, function, args, argSteps);
		if (allConstant) {
			try {
				Value[] argValues = evaluateAllArguments(args, argSteps, EmptyBindingSet.getInstance());
				Value res = function.evaluate(tripleSource, argValues);
				return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(res);
			} catch (QueryEvaluationException ex) {
				return new QueryValueEvaluationStep.Fail("Constant failure: " + ex.getMessage());
			}
		} else {
			return bindings -> {
				Value[] argValues = evaluateAllArguments(args, argSteps, bindings);
				return function.evaluate(tripleSource, argValues);
			};
		}
	}

	/**
	 * If all input is constant normally the function call output will be constant as well.
	 *
	 * @param context  used to precompile arguments of the function
	 * @param function that might be constant
	 * @param args     that the function must evaluate
	 * @param argSteps side effect this array is filled
	 * @return if this function resolves to a constant value
	 */
	private boolean determineIfFunctionCallWillBeAConstant(QueryEvaluationContext context, Function function,
			List<ValueExpr> args, QueryValueEvaluationStep[] argSteps) {
		boolean allConstant = true;
		if (function.mustReturnDifferentResult()) {
			allConstant = false;
			for (int i = 0; i < args.size(); i++) {
				argSteps[i] = precompile(args.get(i), context);
			}
		} else {
			for (int i = 0; i < args.size(); i++) {
				argSteps[i] = precompile(args.get(i), context);
				if (!argSteps[i].isConstant()) {
					allConstant = false;
				}
			}
		}
		return allConstant;
	}

	private Value[] evaluateAllArguments(List<ValueExpr> args, QueryValueEvaluationStep[] argSteps,
			BindingSet bindings) {
		Value[] argValues = new Value[argSteps.length];
		for (int i = 0; i < args.size(); i++) {
			argValues[i] = argSteps[i].evaluate(bindings);
		}
		return argValues;
	}

	private QueryValueEvaluationStep prepare(And node, QueryEvaluationContext context) throws QueryEvaluationException {
		QueryValueEvaluationStep leftStep = precompile(node.getLeftArg(), context);
		QueryValueEvaluationStep rightStep = precompile(node.getRightArg(), context);

		return AndValueEvaluationStep.supply(leftStep, rightStep);
	}

	protected QueryValueEvaluationStep prepare(Or node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryValueEvaluationStep leftArg = null;
		QueryValueEvaluationStep rightArg = null;
		try {
			try {
				leftArg = precompile(node.getLeftArg(), context);
			} catch (ValueExprEvaluationException e) {
				// leftArg would always be false in this case so no need to evaluate it.
				return precompile(node.getRightArg(), context);
			}
			rightArg = precompile(node.getRightArg(), context);
		} catch (ValueExprEvaluationException e) {
			// Both failed to compile so we know we will always throw an exception
			return new QueryValueEvaluationStep.Fail("Value Expressions in OR both failed to prepare/precompile");
		}
		return new OrValueEvaluationStep(leftArg, rightArg);
	}

	protected QueryValueEvaluationStep prepare(Not node, QueryEvaluationContext context) {
		return supplyUnaryValueEvaluation(node,
				v -> BooleanLiteral.valueOf(!QueryEvaluationUtil.getEffectiveBooleanValue(v)), context);
	}

	/**
	 * During the execution of a single query NOW() should always return the same result and is in practical terms a
	 * constant during evaluation.
	 *
	 * @param node    that represent the NOW() function
	 * @param context that holds the shared now() of the query invocation
	 * @return a constant value evaluation step
	 */
	protected QueryValueEvaluationStep prepare(Now node, QueryEvaluationContext context) {
		return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(context.getNow());
	}

	protected QueryValueEvaluationStep prepare(SameTerm node, QueryEvaluationContext context) {
		return supplyBinaryValueEvaluation(node,
				(leftVal, rightVal) -> BooleanLiteral.valueOf(leftVal != null && leftVal.equals(rightVal)), context);
	}

	protected QueryValueEvaluationStep prepare(Coalesce node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		List<ValueExpr> args = node.getArguments();
		List<QueryValueEvaluationStep> compiledArgs = new ArrayList<>(args.size());
		for (ValueExpr arg : args) {
			try {
				compiledArgs.add(precompile(arg, context));
			} catch (QueryEvaluationException e) {
				// no need to add as it would have been ignored anyway.
			}
		}

		return bindings -> {

			for (QueryValueEvaluationStep expr : compiledArgs) {
				try {
					// return first result that does not produce an error on
					// evaluation.
					return expr.evaluate(bindings);

				} catch (QueryEvaluationException ignored) {
				}
			}

			throw new ValueExprEvaluationException(
					"COALESCE arguments do not evaluate to a value: " + node.getSignature());
		};
	}

	protected QueryValueEvaluationStep prepare(Compare node, QueryEvaluationContext context) {
		boolean strict = QueryEvaluationMode.STRICT == getQueryEvaluationMode();
		return supplyBinaryValueEvaluation(node, (leftVal, rightVal) -> BooleanLiteral
				.valueOf(QueryEvaluationUtil.compare(leftVal, rightVal, node.getOperator(), strict)), context);
	}

	private BiFunction<Value, Value, Value> mathOperationApplier(MathExpr node,
			QueryEvaluationMode queryEvaluationMode, ValueFactory vf) {
		final MathOp operator = node.getOperator();
		switch (queryEvaluationMode) {
		case STRICT: {
			return (l, r) -> {
				if (l instanceof Literal && r instanceof Literal) {
					return MathUtil.compute((Literal) l, (Literal) r, operator);
				} else {
					throw new ValueExprEvaluationException("Both arguments must be literals");
				}
			};
		}
		case STANDARD:
		default:

			return (l, r) -> {
				if (l instanceof Literal && r instanceof Literal) {
					return XMLDatatypeMathUtil.compute((Literal) l, (Literal) r, operator, vf);
				} else {
					throw new ValueExprEvaluationException("Both arguments must be literals");
				}
			};
		}
	}

	protected QueryValueEvaluationStep prepare(MathExpr node, QueryEvaluationContext context) {
		final BiFunction<Value, Value, Value> mathOperationApplier = mathOperationApplier(node,
				getQueryEvaluationMode(),
				tripleSource.getValueFactory());
		return supplyBinaryValueEvaluation(node, mathOperationApplier, context);
	}

	protected QueryValueEvaluationStep prepare(If node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryValueEvaluationStep condition;
		try {
			condition = precompile(node.getCondition(), context);
		} catch (ValueExprEvaluationException e) {
			// in case of type error, if-construction should result in empty
			// binding.
			return new QueryValueEvaluationStep.ApplyFunctionForEachBinding(bs -> null);
		}
		QueryValueEvaluationStep result = precompile(node.getResult(), context);
		QueryValueEvaluationStep alternative = precompile(node.getAlternative(), context);
		return new IfValueEvaluationStep(result, condition, alternative);
	}

	protected QueryValueEvaluationStep prepare(In node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryValueEvaluationStep left = precompile(node.getArg(), context);
		QueryEvaluationStep subquery = precompile(node.getSubQuery(), context);
		return new InValueEvaluationStep(node, subquery, left);
	}

	protected QueryValueEvaluationStep prepare(ListMemberOperator node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		List<ValueExpr> args = node.getArguments();
		List<QueryValueEvaluationStep> compiledArgs = new ArrayList<>(args.size());
		for (ValueExpr arg : args) {
			try {
				compiledArgs.add(precompile(arg, context));
			} catch (ValueExprEvaluationException e) {
				compiledArgs.add(new QueryValueEvaluationStep.Fail(""));
			}
		}
		return new ListMemberValueOperationStep(compiledArgs);
	}

	protected QueryValueEvaluationStep prepare(CompareAny node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		QueryEvaluationStep subquery = precompile(node.getSubQuery(), context);
		return new CompareAnyValueEvaluationStep(arg, node, subquery, context);
	}

	protected QueryValueEvaluationStep prepare(CompareAll node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryValueEvaluationStep arg = precompile(node.getArg(), context);
		QueryEvaluationStep subquery = precompile(node.getSubQuery(), context);
		return new CompareAllQueryValueEvaluationStep(arg, node, subquery, context);
	}

	protected QueryValueEvaluationStep prepare(Exists node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep subquery = precompile(node.getSubQuery(), context);
		return new ExistsQueryValueEvaluationStep(subquery);
	}

	@Override
	public boolean isTrue(ValueExpr expr, BindingSet bindings) throws QueryEvaluationException {
		Value value = evaluate(expr, bindings);
		return QueryEvaluationUtility.getEffectiveBooleanValue(value).orElse(false);
	}

	public boolean isTrue(QueryValueEvaluationStep expr, BindingSet bindings) throws QueryEvaluationException {
		Value value = expr.evaluate(bindings);
		return QueryEvaluationUtility.getEffectiveBooleanValue(value).orElse(false);
	}

	protected boolean isReducedOrDistinct(QueryModelNode node) {
		QueryModelNode parent = node.getParentNode();
		if (parent instanceof Slice) {
			return isReducedOrDistinct(parent);
		}
		return parent instanceof Distinct || parent instanceof Reduced;
	}

	/**
	 * Returns the limit of the current variable bindings before any further projection.
	 */
	protected long getLimit(QueryModelNode node) {
		long offset = 0;
		if (node instanceof Slice) {
			Slice slice = (Slice) node;
			if (slice.hasOffset() && slice.hasLimit()) {
				return slice.getOffset() + slice.getLimit();
			} else if (slice.hasLimit()) {
				return slice.getLimit();
			} else if (slice.hasOffset()) {
				offset = slice.getOffset();
			}
		}
		QueryModelNode parent = node.getParentNode();
		if (parent instanceof Distinct || parent instanceof Reduced || parent instanceof Slice) {
			long limit = getLimit(parent);
			if (offset > 0L && limit < Long.MAX_VALUE) {
				return offset + limit;
			} else {
				return limit;
			}
		}
		return Long.MAX_VALUE;
	}

	protected QueryValueEvaluationStep prepare(ValueExprTripleRef node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryValueEvaluationStep subject = precompile(node.getSubjectVar(), context);
		QueryValueEvaluationStep predicate = precompile(node.getPredicateVar(), context);
		QueryValueEvaluationStep object = precompile(node.getObjectVar(), context);
		ValueFactory valueFactory = tripleSource.getValueFactory();
		return new ValueExprTripleRefEvaluationStep(subject, valueFactory, predicate, object);

	}

	/**
	 * evaluates a TripleRef node returning bindingsets from the matched Triple nodes in the dataset (or explore
	 * standard reification)
	 *
	 * @param ref      to evaluate
	 * @param bindings with the solutions
	 * @return iteration over the solutions
	 */
	public CloseableIteration<BindingSet> evaluate(TripleRef ref, BindingSet bindings) {
		return precompile(ref).evaluate(bindings);
	}

	protected QueryEvaluationStep prepare(TripleRef ref, QueryEvaluationContext context) {
		// Naive implementation that walks over all statements matching (x rdf:type rdf:Statement)
		// and filter those that do not match the bindings for subject, predicate and object vars (if bound)
		final org.eclipse.rdf4j.query.algebra.Var subjVar = ref.getSubjectVar();
		final org.eclipse.rdf4j.query.algebra.Var predVar = ref.getPredicateVar();
		final org.eclipse.rdf4j.query.algebra.Var objVar = ref.getObjectVar();
		final org.eclipse.rdf4j.query.algebra.Var extVar = ref.getExprVar();
		// whether the TripleSouce support access to RDF star
		final boolean sourceSupportsRdfStar = tripleSource instanceof RDFStarTripleSource;
		if (sourceSupportsRdfStar) {
			return new RdfStarQueryEvaluationStep(subjVar, predVar, objVar, extVar, (RDFStarTripleSource) tripleSource,
					context);
		} else {
			return new ReificationRdfStarQueryEvaluationStep(subjVar, predVar, objVar, extVar, tripleSource, context);
		}
	}

	/**
	 * This class wraps an iterator and increments the "resultSizeActual" of the query model node that the iterator
	 * represents. This means we can track the number of tuples that have been retrieved from this node.
	 */
	private static class ResultSizeCountingIterator extends IterationWrapper<BindingSet> {

		CloseableIteration<BindingSet> iterator;
		QueryModelNode queryModelNode;

		public ResultSizeCountingIterator(CloseableIteration<BindingSet> iterator,
				QueryModelNode queryModelNode) {
			super(iterator);
			this.iterator = iterator;
			this.queryModelNode = queryModelNode;
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			return iterator.hasNext();
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			queryModelNode.setResultSizeActual(queryModelNode.getResultSizeActual() + 1);
			return iterator.next();
		}

	}

	/**
	 * This class wraps an iterator and tracks the time used to execute next() and hasNext()
	 */
	private static class TimedIterator extends IterationWrapper<BindingSet> {

		CloseableIteration<BindingSet> iterator;
		QueryModelNode queryModelNode;
		Stopwatch stopwatch = Stopwatch.createStarted();

		public TimedIterator(CloseableIteration<BindingSet> iterator,
				QueryModelNode queryModelNode) {
			super(iterator);
			this.iterator = iterator;
			this.queryModelNode = queryModelNode;
			stopwatch.stop();
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			stopwatch.start();
			BindingSet next = iterator.next();
			stopwatch.stop();
			return next;
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			stopwatch.start();
			boolean hasNext = super.hasNext();
			stopwatch.stop();
			return hasNext;
		}

		@Override
		protected void handleClose() throws QueryEvaluationException {
			try {
				long totalTimeNanosActual = queryModelNode.getTotalTimeNanosActual();
				queryModelNode
						.setTotalTimeNanosActual((totalTimeNanosActual + stopwatch.elapsed(TimeUnit.NANOSECONDS)));
			} finally {
				super.handleClose();

			}
		}
	}

	@Override
	public void setTrackResultSize(boolean trackResultSize) {
		this.trackResultSize = trackResultSize;
	}

	@Override
	public void setTrackTime(boolean trackTime) {
		this.trackTime = trackTime;
	}

	/**
	 * Supply a QueryValueEvalationStep that will invoke the function (operator passed in). It will try to optimise
	 * constant argument to be called only once per query run,
	 *
	 * @param node      the node to evaluate
	 * @param operation the function that wraps the operator.
	 * @param context   in which the query is running.
	 * @return a potential constant evaluation step.
	 */
	protected QueryValueEvaluationStep supplyBinaryValueEvaluation(BinaryValueOperator node,
			BiFunction<Value, Value, Value> operation, QueryEvaluationContext context) {
		QueryValueEvaluationStep leftStep = precompile(node.getLeftArg(), context);
		QueryValueEvaluationStep rightStep = precompile(node.getRightArg(), context);
		if (leftStep.isConstant() && rightStep.isConstant()) {
			Value leftVal = leftStep.evaluate(EmptyBindingSet.getInstance());
			Value rightVal = rightStep.evaluate(EmptyBindingSet.getInstance());
			Value value = operation.apply(leftVal, rightVal);
			return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(value);
		} else if (leftStep.isConstant()) {
			Value leftVal = leftStep.evaluate(EmptyBindingSet.getInstance());
			return bindings -> {
				Value rightVal = rightStep.evaluate(bindings);
				return operation.apply(leftVal, rightVal);
			};
		} else if (rightStep.isConstant()) {
			Value rightVal = rightStep.evaluate(EmptyBindingSet.getInstance());
			return bindings -> {
				Value leftVal = leftStep.evaluate(bindings);
				Value result = operation.apply(leftVal, rightVal);
				return result;
			};
		} else {
			return bindings -> {
				Value leftVal = leftStep.evaluate(bindings);
				Value rightVal = rightStep.evaluate(bindings);
				return operation.apply(leftVal, rightVal);
			};
		}
	}

	/**
	 * Return a QueryEvaluationStep that applies constant propegation.
	 *
	 * @param node      that will be evaluated/prepared
	 * @param operation the task to be done
	 * @param context   in which the evaluation takes place
	 * @return a potentially constant step
	 */
	protected QueryValueEvaluationStep supplyUnaryValueEvaluation(UnaryValueOperator node,
			java.util.function.Function<Value, Value> operation, QueryEvaluationContext context) {
		QueryValueEvaluationStep argStep = precompile(node.getArg(), context);
		if (argStep.isConstant()) {
			Value argValue = argStep.evaluate(EmptyBindingSet.getInstance());

			return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(operation.apply(argValue));
		} else {
			return bindings -> {
				Value argValue = argStep.evaluate(bindings);
				return operation.apply(argValue);
			};
		}
	}

	/**
	 * @return the queryEvaluationMode
	 */
	public QueryEvaluationMode getQueryEvaluationMode() {
		return queryEvaluationMode;
	}

	/**
	 * @param queryEvaluationMode the queryEvaluationMode to set
	 */
	public void setQueryEvaluationMode(QueryEvaluationMode queryEvaluationMode) {
		this.queryEvaluationMode = Objects.requireNonNull(queryEvaluationMode);
	}

	@Override
	public Supplier<CollectionFactory> getCollectionFactory() {
		return collectionFactory;
	}

	@Override
	public void setCollectionFactory(Supplier<CollectionFactory> cf) {
		this.collectionFactory = cf;
	}
}
