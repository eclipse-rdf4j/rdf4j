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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.common.iteration.ReducedIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
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
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
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
import org.eclipse.rdf4j.query.algebra.Like;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.LocalName;
import org.eclipse.rdf4j.query.algebra.MathExpr;
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
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.UnaryValueOperator;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.ValueExprTripleRef;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
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
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.DescribeIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ExtensionIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.FilterIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.GroupIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.MultiProjectionIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.PathIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.StandardQueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.evaluation.util.MathUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.OrderComparator;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.util.UUIDable;

import com.google.common.base.Stopwatch;

/**
 * Minimally-conforming SPARQL 1.1 Query Evaluation strategy, to evaluate one {@link TupleExpr} on the given
 * {@link TripleSource}, optionally using the given {@link Dataset}.
 *
 * @author Jeen Broekstra
 * @author James Leigh
 * @author Arjohn Kampman
 * @author David Huynh
 * @author Andreas Schwarte
 * @see ExtendedEvaluationStrategy
 */
public class StrictEvaluationStrategy implements EvaluationStrategy, FederatedServiceResolverClient, UUIDable {

	/*-----------*
	 * Constants *
	 *-----------*/

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

	/*--------------*
	 * Constructors *
	 *--------------*/

	public StrictEvaluationStrategy(TripleSource tripleSource, FederatedServiceResolver serviceResolver) {
		this(tripleSource, null, serviceResolver);
	}

	public StrictEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver) {
		this(tripleSource, dataset, serviceResolver, 0, new EvaluationStatistics());
	}

	public StrictEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold,
			EvaluationStatistics evaluationStatistics) {
		this(tripleSource, dataset, serviceResolver, iterationCacheSyncTreshold, evaluationStatistics, false);

	}

	public StrictEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold,
			EvaluationStatistics evaluationStatistics, boolean trackResultSize) {
		this.tripleSource = tripleSource;
		this.dataset = dataset;
		this.serviceResolver = serviceResolver;
		this.iterationCacheSyncThreshold = iterationCacheSyncTreshold;
		this.pipeline = new StandardQueryOptimizerPipeline(this, tripleSource, evaluationStatistics);
		this.trackResultSize = trackResultSize;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Deprecated(forRemoval = true, since = "4.0.0")
	@Override
	synchronized public UUID getUUID() {
		if (uuid == null) {
			uuid = UUID.randomUUID();
		}
		return uuid;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		serviceResolver = resolver;
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

	@Deprecated(forRemoval = true)
	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleExpr expr, BindingSet bindings)
			throws QueryEvaluationException {

		CloseableIteration<BindingSet, QueryEvaluationException> ret;

		if (expr instanceof StatementPattern) {
			ret = evaluate((StatementPattern) expr, bindings);
		} else if (expr instanceof UnaryTupleOperator) {
			ret = evaluate((UnaryTupleOperator) expr, bindings);
		} else if (expr instanceof BinaryTupleOperator) {
			ret = evaluate((BinaryTupleOperator) expr, bindings);
		} else if (expr instanceof SingletonSet) {
			ret = evaluate((SingletonSet) expr, bindings);
		} else if (expr instanceof EmptySet) {
			ret = evaluate((EmptySet) expr, bindings);
		} else if (expr instanceof ZeroLengthPath) {
			ret = evaluate((ZeroLengthPath) expr, bindings);
		} else if (expr instanceof ArbitraryLengthPath) {
			ret = evaluate((ArbitraryLengthPath) expr, bindings);
		} else if (expr instanceof BindingSetAssignment) {
			ret = evaluate((BindingSetAssignment) expr, bindings);
		} else if (expr instanceof TripleRef) {
			ret = evaluate((TripleRef) expr, bindings);
		} else if (expr == null) {
			throw new IllegalArgumentException("expr must not be null");
		} else {
			throw new QueryEvaluationException("Unsupported tuple expr type: " + expr.getClass());
		}

		if (trackTime) {
			// set resultsSizeActual to at least be 0 so we can track iterations that don't procude anything
			expr.setTotalTimeNanosActual(Math.max(0, expr.getTotalTimeNanosActual()));
			ret = new TimedIterator(ret, expr);
		}

		if (trackResultSize) {
			// set resultsSizeActual to at least be 0 so we can track iterations that don't procude anything
			expr.setResultSizeActual(Math.max(0, expr.getResultSizeActual()));
			ret = new ResultSizeCountingIterator(ret, expr);
		}
		return ret;
	}

	@Override
	public QueryEvaluationStep precompile(TupleExpr expr) {
		QueryEvaluationContext context = new QueryEvaluationContext.Minimal(dataset, tripleSource.getValueFactory());
		if (expr instanceof QueryRoot) {
			String[] allVariables = ArrayBindingBasedQueryEvaluationContext
					.findAllVariablesUsedInQuery((QueryRoot) expr);
			context = new ArrayBindingBasedQueryEvaluationContext(context, allVariables);
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
			return EvaluationStrategy.super.precompile(expr, context);
		}
	}

	private QueryEvaluationStep trackResultSize(TupleExpr expr, QueryEvaluationStep qes) {
		return QueryEvaluationStep.wrap(qes, (iter) -> {
			expr.setResultSizeActual(Math.max(0, expr.getResultSizeActual()));
			return new ResultSizeCountingIterator(iter, expr);
		});
	}

	private QueryEvaluationStep trackTime(TupleExpr expr, QueryEvaluationStep qes) {
		return QueryEvaluationStep.wrap(qes, (iter) -> {
			expr.setTotalTimeNanosActual(Math.max(0, expr.getTotalTimeNanosActual()));
			return new TimedIterator(iter, expr);
		});
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(ArbitraryLengthPath alp,
			final BindingSet bindings) throws QueryEvaluationException {
		return precompile(alp).evaluate(bindings);
	}

	protected QueryEvaluationStep prepare(ArbitraryLengthPath alp, QueryEvaluationContext context)
			throws QueryEvaluationException {
		final Scope scope = alp.getScope();
		final Var subjectVar = alp.getSubjectVar();
		final TupleExpr pathExpression = alp.getPathExpression();
		final Var objVar = alp.getObjectVar();
		final Var contextVar = alp.getContextVar();
		final long minLength = alp.getMinLength();
		return bindings -> new PathIteration(StrictEvaluationStrategy.this, scope, subjectVar, pathExpression, objVar,
				contextVar, minLength, bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(ZeroLengthPath zlp,
			final BindingSet bindings) throws QueryEvaluationException {
		return precompile(zlp).evaluate(bindings);
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

	@Deprecated(forRemoval = true)
	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service service, String serviceUri,
			CloseableIteration<BindingSet, QueryEvaluationException> bindings) throws QueryEvaluationException {
		try {
			FederatedService fs = serviceResolver.getService(serviceUri);
			return fs.evaluate(service, bindings, service.getBaseURI());
		} catch (QueryEvaluationException e) {
			// suppress exceptions if silent
			if (service.isSilent()) {
				return bindings;
			} else {
				throw new QueryEvaluationException(e);
			}
		}
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service service, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(service).evaluate(bindings);
	}

	protected QueryEvaluationStep prepare(Difference node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return new MinusQueryEvaluationStep(precompile(node.getLeftArg(), context),
				precompile(node.getRightArg(), context));
	}

	protected QueryEvaluationStep prepare(Group node, QueryEvaluationContext context) throws QueryEvaluationException {
		return bindings -> new GroupIterator(StrictEvaluationStrategy.this, node, bindings, iterationCacheSyncThreshold,
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
		return bindings -> new MultiProjectionIterator(node, arg.evaluate(bindings), bindings);
	}

	protected QueryEvaluationStep prepare(Projection node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep temp = precompile(node.getArg(), context);
		return new ProjectionQueryEvaluationStep(node, temp, context);
	}

	protected QueryEvaluationStep prepare(QueryRoot node, QueryEvaluationContext context)
			throws QueryEvaluationException {

		QueryEvaluationStep arg = precompile(node.getArg(), context);
		return new QueryRootQueryEvaluationStep(arg);
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

		if (FilterIterator.isPartOfSubQuery(node)) {
			context = new FilterIterator.RetainedVariableFilteredQueryEvaluationContext(node, context);
		}
		QueryEvaluationStep arg = precompile(node.getArg(), context);
		QueryValueEvaluationStep ves;
		try {
			ves = precompile(node.getCondition(), context);
		} catch (QueryEvaluationException e) {
			// If we have a failed compilation we always return false.
			// Which means empty. so let's short circuit that.
//			ves = new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.FALSE);
			return QueryEvaluationStep.EMPTY;
		}
		// if the query evaluation is constant it is either FILTER(true) or FILTER(false)
		// in one case we can remove this step from the evaluated plan
		// in the other case nothing can pass the filter so we can return the empty set.
		if (ves.isConstant()) {
			if (StrictEvaluationStrategy.this.isTrue(ves, EmptyBindingSet.getInstance())) {
				return arg;
			} else {
				return QueryEvaluationStep.EMPTY;
			}
		}
		return bs -> new FilterIterator(node, arg.evaluate(bs), ves, StrictEvaluationStrategy.this);
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

		private QueryRootQueryEvaluationStep(QueryEvaluationStep arg) {
			this.arg = arg;
		}

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
			// TODO fix the sharing of the now element to be safe
			StrictEvaluationStrategy.this.sharedValueOfNow = null;
			return arg.evaluate(bs);
		}
	}

	protected QueryEvaluationStep prepare(DescribeOperator node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep child = precompile(node.getArg(), context);
		return bs -> new DescribeIteration(child.evaluate(bs), StrictEvaluationStrategy.this, node.getBindingNames(),
				bs);
	}

	protected QueryEvaluationStep prepare(Distinct node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep child = precompile(node.getArg(), context);
		return bindings -> {
			CloseableIteration<BindingSet, QueryEvaluationException> evaluate = child.evaluate(bindings);
			return new DistinctIteration<>(evaluate, StrictEvaluationStrategy.this::makeSet);
		};

	}

	protected QueryEvaluationStep prepare(Reduced node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep arg = precompile(node.getArg(), context);
		return bindings -> new ReducedIteration<>(arg.evaluate(bindings));
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(DescribeOperator operator,
			final BindingSet bindings) throws QueryEvaluationException {
		return precompile(operator).evaluate(bindings);
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(StatementPattern statementPattern,
			BindingSet bindings) throws QueryEvaluationException {
		return precompile(statementPattern).evaluate(bindings);
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

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(UnaryTupleOperator expr,
			BindingSet bindings) throws QueryEvaluationException {
		if (expr instanceof Projection) {
			return evaluate((Projection) expr, bindings);
		} else if (expr instanceof MultiProjection) {
			return evaluate((MultiProjection) expr, bindings);
		} else if (expr instanceof Filter) {
			return evaluate((Filter) expr, bindings);
		} else if (expr instanceof Service) {
			return evaluate((Service) expr, bindings);
		} else if (expr instanceof Slice) {
			return evaluate((Slice) expr, bindings);
		} else if (expr instanceof Extension) {
			return evaluate((Extension) expr, bindings);
		} else if (expr instanceof Distinct) {
			return evaluate((Distinct) expr, bindings);
		} else if (expr instanceof Reduced) {
			return evaluate((Reduced) expr, bindings);
		} else if (expr instanceof Group) {
			return evaluate((Group) expr, bindings);
		} else if (expr instanceof Order) {
			return evaluate((Order) expr, bindings);
		} else if (expr instanceof QueryRoot) {
			// new query, reset shared return value for successive calls of
			// NOW()
			this.sharedValueOfNow = null;
			return evaluate(expr.getArg(), bindings);
		} else if (expr instanceof DescribeOperator) {
			return evaluate((DescribeOperator) expr, bindings);
		} else if (expr == null) {
			throw new IllegalArgumentException("expr must not be null");
		} else {
			throw new QueryEvaluationException("Unknown unary tuple operator type: " + expr.getClass());
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

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSetAssignment bsa,
			BindingSet bindings) throws QueryEvaluationException {
		return precompile(bsa).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Projection projection, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(projection).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(MultiProjection multiProjection,
			BindingSet bindings) throws QueryEvaluationException {
		return precompile(multiProjection).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Filter filter, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(filter).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Slice slice, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(slice).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Extension extension, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(extension).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Distinct distinct, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(distinct).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Reduced reduced, BindingSet bindings)
			throws QueryEvaluationException {
		return new ReducedIteration<>(evaluate(reduced.getArg(), bindings));
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Group node, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(node).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Order node, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(node).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BinaryTupleOperator expr,
			BindingSet bindings) throws QueryEvaluationException {
		if (expr instanceof Join) {
			return evaluate((Join) expr, bindings);
		} else if (expr instanceof LeftJoin) {
			return evaluate((LeftJoin) expr, bindings);
		} else if (expr instanceof Union) {
			return evaluate((Union) expr, bindings);
		} else if (expr instanceof Intersection) {
			return evaluate((Intersection) expr, bindings);
		} else if (expr instanceof Difference) {
			return evaluate((Difference) expr, bindings);
		} else if (expr == null) {
			throw new IllegalArgumentException("expr must not be null");
		} else {
			throw new QueryEvaluationException("Unsupported binary tuple operator type: " + expr.getClass());
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

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Join join, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(join).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(LeftJoin leftJoin,
			final BindingSet bindings) throws QueryEvaluationException {
		return precompile(leftJoin).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(final Union union,
			final BindingSet bindings) throws QueryEvaluationException {
		return precompile(union).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(final Intersection intersection,
			final BindingSet bindings) throws QueryEvaluationException {
		return precompile(intersection).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(final Difference difference,
			final BindingSet bindings) throws QueryEvaluationException {
		return precompile(difference).evaluate(bindings);
	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(SingletonSet singletonSet,
			BindingSet bindings) throws QueryEvaluationException {
		return new SingletonIteration<>(bindings);
	}

	protected QueryEvaluationStep prepare(SingletonSet singletonSet, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return SingletonIteration::new;

	}

	@Deprecated(forRemoval = true)
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(EmptySet emptySet, BindingSet bindings)
			throws QueryEvaluationException {
		return QueryEvaluationStep.EMPTY_ITERATION;
	}

	protected QueryEvaluationStep prepare(EmptySet emptySet, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return bindings -> QueryEvaluationStep.EMPTY_ITERATION;
	}

	@Override
	public QueryValueEvaluationStep precompile(ValueExpr expr, QueryEvaluationContext context)
			throws QueryEvaluationException {
		if (expr instanceof Var) {
			return prepare((Var) expr, context);
		} else if (expr instanceof ValueConstant) {
			return prepare((ValueConstant) expr, context);
		} else if (expr instanceof BNodeGenerator) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Bound) {
			return prepare((Bound) expr, context);
//			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Str) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Label) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Lang) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof LangMatches) {
			return prepare((LangMatches) expr, context);
		} else if (expr instanceof Datatype) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Namespace) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof LocalName) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof IsResource) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof IsURI) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof IsBNode) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof IsLiteral) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof IsNumeric) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof IRIFunction) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Regex) {
			return prepare((Regex) expr, context);
		} else if (expr instanceof Coalesce) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Like) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof FunctionCall) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof And) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Or) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Not) {
			return prepare((Not) expr, context);
		} else if (expr instanceof SameTerm) {
			return prepare((SameTerm) expr, context);
		} else if (expr instanceof Compare) {
			return prepare((Compare) expr, context);
		} else if (expr instanceof MathExpr) {
			return prepare((MathExpr) expr, context);
		} else if (expr instanceof In) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof CompareAny) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof CompareAll) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof Exists) {
			return prepare((Exists) expr, context);
		} else if (expr instanceof If) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof ListMemberOperator) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr instanceof ValueExprTripleRef) {
			return new QueryValueEvaluationStep.Minimal(this, expr);
		} else if (expr == null) {
			throw new IllegalArgumentException("expr must not be null");
		} else {
			throw new QueryEvaluationException("Unsupported value expr type: " + expr.getClass());
		}
	}

	@Deprecated(forRemoval = true)
	@Override
	public Value evaluate(ValueExpr expr, BindingSet bindings) throws QueryEvaluationException {
		if (expr instanceof Var) {
			return evaluate((Var) expr, bindings);
		} else if (expr instanceof ValueConstant) {
			return evaluate((ValueConstant) expr, bindings);
		} else if (expr instanceof BNodeGenerator) {
			return evaluate((BNodeGenerator) expr, bindings);
		} else if (expr instanceof Bound) {
			return evaluate((Bound) expr, bindings);
		} else if (expr instanceof Str) {
			return evaluate((Str) expr, bindings);
		} else if (expr instanceof Label) {
			return evaluate((Label) expr, bindings);
		} else if (expr instanceof Lang) {
			return evaluate((Lang) expr, bindings);
		} else if (expr instanceof LangMatches) {
			return evaluate((LangMatches) expr, bindings);
		} else if (expr instanceof Datatype) {
			return evaluate((Datatype) expr, bindings);
		} else if (expr instanceof Namespace) {
			return evaluate((Namespace) expr, bindings);
		} else if (expr instanceof LocalName) {
			return evaluate((LocalName) expr, bindings);
		} else if (expr instanceof IsResource) {
			return evaluate((IsResource) expr, bindings);
		} else if (expr instanceof IsURI) {
			return evaluate((IsURI) expr, bindings);
		} else if (expr instanceof IsBNode) {
			return evaluate((IsBNode) expr, bindings);
		} else if (expr instanceof IsLiteral) {
			return evaluate((IsLiteral) expr, bindings);
		} else if (expr instanceof IsNumeric) {
			return evaluate((IsNumeric) expr, bindings);
		} else if (expr instanceof IRIFunction) {
			return evaluate((IRIFunction) expr, bindings);
		} else if (expr instanceof Regex) {
			return evaluate((Regex) expr, bindings);
		} else if (expr instanceof Coalesce) {
			return evaluate((Coalesce) expr, bindings);
		} else if (expr instanceof Like) {
			return evaluate((Like) expr, bindings);
		} else if (expr instanceof FunctionCall) {
			return evaluate((FunctionCall) expr, bindings);
		} else if (expr instanceof And) {
			return evaluate((And) expr, bindings);
		} else if (expr instanceof Or) {
			return evaluate((Or) expr, bindings);
		} else if (expr instanceof Not) {
			return evaluate((Not) expr, bindings);
		} else if (expr instanceof SameTerm) {
			return evaluate((SameTerm) expr, bindings);
		} else if (expr instanceof Compare) {
			return evaluate((Compare) expr, bindings);
		} else if (expr instanceof MathExpr) {
			return evaluate((MathExpr) expr, bindings);
		} else if (expr instanceof In) {
			return evaluate((In) expr, bindings);
		} else if (expr instanceof CompareAny) {
			return evaluate((CompareAny) expr, bindings);
		} else if (expr instanceof CompareAll) {
			return evaluate((CompareAll) expr, bindings);
		} else if (expr instanceof Exists) {
			return evaluate((Exists) expr, bindings);
		} else if (expr instanceof If) {
			return evaluate((If) expr, bindings);
		} else if (expr instanceof ListMemberOperator) {
			return evaluate((ListMemberOperator) expr, bindings);
		} else if (expr instanceof ValueExprTripleRef) {
			return evaluate((ValueExprTripleRef) expr, bindings);
		} else if (expr == null) {
			throw new IllegalArgumentException("expr must not be null");
		} else {
			throw new QueryEvaluationException("Unsupported value expr type: " + expr.getClass());
		}
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Var var, BindingSet bindings) throws QueryEvaluationException {
		Value value = var.getValue();

		if (value == null) {
			value = bindings.getValue(var.getName());
		}

		if (value == null) {
			throw new ValueExprEvaluationException();
		}

		return value;
	}

	protected QueryValueEvaluationStep prepare(Var var, QueryEvaluationContext context)
			throws QueryEvaluationException {

		Value value = var.getValue();

		if (value != null) {
			return new ConstantQueryValueEvaluationStep(value);
		} else {
			java.util.function.Function<BindingSet, Value> getValue = context.getValue(var.getName());
			return bindings -> {
				Value value1 = getValue.apply(bindings);
				if (value1 == null) {
					throw new ValueExprEvaluationException();
				}
				return value1;
			};
		}

	}

	@Deprecated(forRemoval = true)
	public Value evaluate(ValueConstant valueConstant, BindingSet bindings) throws QueryEvaluationException {
		return valueConstant.getValue();
	}

	protected QueryValueEvaluationStep prepare(ValueConstant valueConstant, QueryEvaluationContext context)
			throws QueryEvaluationException {
		return new ConstantQueryValueEvaluationStep(valueConstant);
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(BNodeGenerator node, BindingSet bindings) throws QueryEvaluationException {
		ValueExpr nodeIdExpr = node.getNodeIdExpr();

		if (nodeIdExpr != null) {
			Value nodeId = evaluate(nodeIdExpr, bindings);

			if (nodeId instanceof Literal) {
				String nodeLabel = ((Literal) nodeId).getLabel() + (bindings.toString().hashCode());
				return tripleSource.getValueFactory().createBNode(nodeLabel);
			} else {
				throw new ValueExprEvaluationException("BNODE function argument must be a literal");
			}
		}
		return tripleSource.getValueFactory().createBNode();
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Bound node, BindingSet bindings) throws QueryEvaluationException {
		try {
			Value argValue = evaluate(node.getArg(), bindings);
			return BooleanLiteral.valueOf(argValue != null);
		} catch (ValueExprEvaluationException e) {
			return BooleanLiteral.FALSE;
		}
	}

	private QueryValueEvaluationStep prepare(Bound node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		try {
			QueryValueEvaluationStep arg = precompile(node.getArg(), context);
			return bindings -> {
				try {
					Value argValue = arg.evaluate(bindings);
					return BooleanLiteral.valueOf(argValue != null);
				} catch (ValueExprEvaluationException e) {
					return BooleanLiteral.FALSE;
				}
			};
		} catch (QueryEvaluationException e) {
			return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.FALSE);
		}
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Str node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		if (argValue != null) {

			if (argValue.isIRI()) {
				return tripleSource.getValueFactory().createLiteral(argValue.toString());
			} else if (argValue.isLiteral()) {
				Literal literal = (Literal) argValue;

				if (QueryEvaluationUtility.isSimpleLiteral(literal)) {
					return literal;
				} else {
					return tripleSource.getValueFactory().createLiteral(literal.getLabel());
				}
			} else if (argValue.isTriple()) {
				return tripleSource.getValueFactory().createLiteral(argValue.toString());
			}
		}
		throw new ValueExprEvaluationException();

	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Label node, BindingSet bindings) throws QueryEvaluationException {
		// FIXME: deprecate Label in favour of Str(?)
		Value argValue = evaluate(node.getArg(), bindings);

		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			if (QueryEvaluationUtility.isSimpleLiteral(literal)) {
				return literal;
			} else {
				return tripleSource.getValueFactory().createLiteral(literal.getLabel());
			}
		} else {
			throw new ValueExprEvaluationException();
		}
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Lang node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);

		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;
			return tripleSource.getValueFactory().createLiteral(literal.getLanguage().orElse(""));
		}

		throw new ValueExprEvaluationException();
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Datatype node, BindingSet bindings) throws QueryEvaluationException {
		Value v = evaluate(node.getArg(), bindings);

		if (v instanceof Literal) {
			Literal literal = (Literal) v;

			if (literal.getDatatype() != null) {
				// literal with datatype
				return literal.getDatatype();
			} else if (literal.getLanguage().isPresent()) {
				return CoreDatatype.RDF.LANGSTRING.getIri();
			} else {
				// simple literal
				return CoreDatatype.XSD.STRING.getIri();
			}

		}

		throw new ValueExprEvaluationException();
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Namespace node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);

		if (argValue instanceof IRI) {
			IRI uri = (IRI) argValue;
			return tripleSource.getValueFactory().createIRI(uri.getNamespace());
		} else {
			throw new ValueExprEvaluationException();
		}
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(LocalName node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);

		if (argValue instanceof IRI) {
			IRI uri = (IRI) argValue;
			return tripleSource.getValueFactory().createLiteral(uri.getLocalName());
		} else {
			throw new ValueExprEvaluationException();
		}
	}

	/**
	 * Determines whether the operand (a variable) contains a Resource.
	 *
	 * @return <var>true</var> if the operand contains a Resource, <var>false</var> otherwise.
	 */
	@Deprecated(forRemoval = true)
	public Value evaluate(IsResource node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		return BooleanLiteral.valueOf(argValue instanceof Resource);
	}

	/**
	 * Determines whether the operand (a variable) contains a URI.
	 *
	 * @return <var>true</var> if the operand contains a URI, <var>false</var> otherwise.
	 */
	@Deprecated(forRemoval = true)
	public Value evaluate(IsURI node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		return BooleanLiteral.valueOf(argValue instanceof IRI);
	}

	/**
	 * Determines whether the operand (a variable) contains a BNode.
	 *
	 * @return <var>true</var> if the operand contains a BNode, <var>false</var> otherwise.
	 */
	@Deprecated(forRemoval = true)
	public Value evaluate(IsBNode node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		return BooleanLiteral.valueOf(argValue instanceof BNode);
	}

	/**
	 * Determines whether the operand (a variable) contains a Literal.
	 *
	 * @return <var>true</var> if the operand contains a Literal, <var>false</var> otherwise.
	 */
	@Deprecated(forRemoval = true)
	public Value evaluate(IsLiteral node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		return BooleanLiteral.valueOf(argValue instanceof Literal);
	}

	/**
	 * Determines whether the operand (a variable) contains a numeric datatyped literal, i.e. a literal with datatype
	 * CoreDatatype.XSD:float, CoreDatatype.XSD:double, CoreDatatype.XSD:decimal, or a derived datatype of
	 * CoreDatatype.XSD:decimal.
	 *
	 * @return <var>true</var> if the operand contains a numeric datatyped literal, <var>false</var> otherwise.
	 */
	@Deprecated(forRemoval = true)
	public Value evaluate(IsNumeric node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);

		if (argValue instanceof Literal) {
			Literal lit = (Literal) argValue;
			IRI datatype = lit.getDatatype();

			return BooleanLiteral.valueOf(XMLDatatypeUtil.isNumericDatatype(datatype));
		} else {
			return BooleanLiteral.FALSE;
		}

	}

	/**
	 * Creates a URI from the operand value (a plain literal or a URI).
	 *
	 * @param node     represents an invocation of the SPARQL IRI function
	 * @param bindings used to generate the value that the URI is based on
	 * @return a URI generated from the given arguments
	 * @throws QueryEvaluationException
	 */
	@Deprecated(forRemoval = true)
	public IRI evaluate(IRIFunction node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);

		if (argValue instanceof Literal) {
			final Literal lit = (Literal) argValue;

			String uriString = lit.getLabel();
			final String baseURI = node.getBaseURI();
			try {
				ParsedIRI iri = ParsedIRI.create(uriString);
				if (!iri.isAbsolute() && baseURI != null) {
					// uri string may be a relative reference.
					uriString = ParsedIRI.create(baseURI).resolve(iri).toString();
				} else if (!iri.isAbsolute()) {
					throw new ValueExprEvaluationException("not an absolute IRI reference: " + uriString);
				}
			} catch (IllegalArgumentException e) {
				throw new ValueExprEvaluationException("not a valid IRI reference: " + uriString);
			}

			IRI result;

			try {
				result = tripleSource.getValueFactory().createIRI(uriString);
			} catch (IllegalArgumentException e) {
				throw new ValueExprEvaluationException(e.getMessage());
			}
			return result;
		} else if (argValue instanceof IRI) {
			return ((IRI) argValue);
		}

		throw new ValueExprEvaluationException();
	}

	/**
	 * Determines whether the two operands match according to the <code>regex</code> operator.
	 *
	 * @return <var>true</var> if the operands match according to the <var>regex</var> operator, <var>false</var>
	 *         otherwise.
	 */
	@Deprecated(forRemoval = true)
	public Value evaluate(Regex node, BindingSet bindings) throws QueryEvaluationException {
		return prepare(node, new QueryEvaluationContext.Minimal(sharedValueOfNow, dataset)).evaluate(bindings);
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

	@Deprecated(forRemoval = true)
	public Value evaluate(LangMatches node, BindingSet bindings) throws QueryEvaluationException {
		Value langTagValue = evaluate(node.getLeftArg(), bindings);
		Value langRangeValue = evaluate(node.getRightArg(), bindings);

		return evaluateLangMatch(langTagValue, langRangeValue);
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

	/**
	 * Determines whether the two operands match according to the <code>like</code> operator. The operator is defined as
	 * a string comparison with the possible use of an asterisk (*) at the end and/or the start of the second operand to
	 * indicate substring matching.
	 *
	 * @return <var>true</var> if the operands match according to the <var>like</var> operator, <var>false</var>
	 *         otherwise.
	 */
	@Deprecated(forRemoval = true)
	public Value evaluate(Like node, BindingSet bindings) throws QueryEvaluationException {
		Value val = evaluate(node.getArg(), bindings);
		String strVal = null;

		if (val instanceof IRI) {
			strVal = val.toString();
		} else if (val instanceof Literal) {
			strVal = ((Literal) val).getLabel();
		}

		if (strVal == null) {
			throw new ValueExprEvaluationException();
		}

		if (!node.isCaseSensitive()) {
			// Convert strVal to lower case, just like the pattern has been done
			strVal = strVal.toLowerCase();
		}

		int valIndex = 0;
		int prevPatternIndex = -1;
		int patternIndex = node.getOpPattern().indexOf('*');

		if (patternIndex == -1) {
			// No wildcards
			return BooleanLiteral.valueOf(node.getOpPattern().equals(strVal));
		}

		String snippet;

		if (patternIndex > 0) {
			// Pattern does not start with a wildcard, first part must match
			snippet = node.getOpPattern().substring(0, patternIndex);
			if (!strVal.startsWith(snippet)) {
				return BooleanLiteral.FALSE;
			}

			valIndex += snippet.length();
			prevPatternIndex = patternIndex;
			patternIndex = node.getOpPattern().indexOf('*', patternIndex + 1);
		}

		while (patternIndex != -1) {
			// Get snippet between previous wildcard and this wildcard
			snippet = node.getOpPattern().substring(prevPatternIndex + 1, patternIndex);

			// Search for the snippet in the value
			valIndex = strVal.indexOf(snippet, valIndex);
			if (valIndex == -1) {
				return BooleanLiteral.FALSE;
			}

			valIndex += snippet.length();
			prevPatternIndex = patternIndex;
			patternIndex = node.getOpPattern().indexOf('*', patternIndex + 1);
		}

		// Part after last wildcard
		snippet = node.getOpPattern().substring(prevPatternIndex + 1);

		if (snippet.length() > 0) {
			// Pattern does not end with a wildcard.

			// Search last occurence of the snippet.
			valIndex = strVal.indexOf(snippet, valIndex);
			int i;
			while ((i = strVal.indexOf(snippet, valIndex + 1)) != -1) {
				// A later occurence was found.
				valIndex = i;
			}

			if (valIndex == -1) {
				return BooleanLiteral.FALSE;
			}

			valIndex += snippet.length();

			if (valIndex < strVal.length()) {
				// Some characters were not matched
				return BooleanLiteral.FALSE;
			}
		}

		return BooleanLiteral.TRUE;
	}

	/**
	 * Evaluates a function.
	 */
	@Deprecated(forRemoval = true)
	public Value evaluate(FunctionCall node, BindingSet bindings) throws QueryEvaluationException {
		Function function = FunctionRegistry.getInstance()
				.get(node.getURI())
				.orElseThrow(() -> new QueryEvaluationException("Unknown function '" + node.getURI() + "'"));

		// the NOW function is a special case as it needs to keep a shared
		// return
		// value for the duration of the query.
		if (function instanceof Now) {
			return evaluate((Now) function, bindings);
		}

		List<ValueExpr> args = node.getArgs();

		Value[] argValues = new Value[args.size()];

		for (int i = 0; i < args.size(); i++) {
			argValues[i] = evaluate(args.get(i), bindings);
		}

		return function.evaluate(tripleSource, argValues);
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
			Value[] argValues = evaluateAllArguments(args, argSteps, EmptyBindingSet.getInstance());
			Value res = function.evaluate(tripleSource, argValues);
			return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(res);
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

	@Deprecated(forRemoval = true)
	public Value evaluate(And node, BindingSet bindings) throws QueryEvaluationException {
		try {
			Value leftValue = evaluate(node.getLeftArg(), bindings);
			if (QueryEvaluationUtility.getEffectiveBooleanValue(leftValue) == QueryEvaluationUtility.Result._false) {
				// Left argument evaluates to false, we don't need to look any
				// further
				return BooleanLiteral.FALSE;
			}
		} catch (ValueExprEvaluationException e) {
			// Failed to evaluate the left argument. Result is 'false' when
			// the right argument evaluates to 'false', failure otherwise.
			Value rightValue = evaluate(node.getRightArg(), bindings);
			if (QueryEvaluationUtility.getEffectiveBooleanValue(rightValue) == QueryEvaluationUtility.Result._false) {
				return BooleanLiteral.FALSE;
			} else {
				throw new ValueExprEvaluationException();
			}
		}

		// Left argument evaluated to 'true', result is determined
		// by the evaluation of the right argument.
		Value rightValue = evaluate(node.getRightArg(), bindings);
		return BooleanLiteral.valueOf(QueryEvaluationUtil.getEffectiveBooleanValue(rightValue));
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Or node, BindingSet bindings) throws QueryEvaluationException {
		try {
			Value leftValue = evaluate(node.getLeftArg(), bindings);
			if (QueryEvaluationUtil.getEffectiveBooleanValue(leftValue)) {
				// Left argument evaluates to true, we don't need to look any
				// further
				return BooleanLiteral.TRUE;
			}
		} catch (ValueExprEvaluationException e) {
			// Failed to evaluate the left argument. Result is 'true' when
			// the right argument evaluates to 'true', failure otherwise.
			Value rightValue = evaluate(node.getRightArg(), bindings);
			if (QueryEvaluationUtil.getEffectiveBooleanValue(rightValue)) {
				return BooleanLiteral.TRUE;
			} else {
				throw new ValueExprEvaluationException();
			}
		}

		// Left argument evaluated to 'false', result is determined
		// by the evaluation of the right argument.
		Value rightValue = evaluate(node.getRightArg(), bindings);
		return BooleanLiteral.valueOf(QueryEvaluationUtil.getEffectiveBooleanValue(rightValue));
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Not node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		boolean argBoolean = QueryEvaluationUtil.getEffectiveBooleanValue(argValue);
		return BooleanLiteral.valueOf(!argBoolean);
	}

	protected QueryValueEvaluationStep prepare(Not node, QueryEvaluationContext context) {
		return supplyUnaryValueEvaluation(node,
				(v) -> BooleanLiteral.valueOf(!QueryEvaluationUtil.getEffectiveBooleanValue(v)), context);
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Now node, BindingSet bindings) throws QueryEvaluationException {
		if (sharedValueOfNow == null) {
			sharedValueOfNow = node.evaluate(tripleSource.getValueFactory());
		}
		return sharedValueOfNow;
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

	@Deprecated(forRemoval = true)
	public Value evaluate(SameTerm node, BindingSet bindings) throws QueryEvaluationException {
		Value leftVal = evaluate(node.getLeftArg(), bindings);
		Value rightVal = evaluate(node.getRightArg(), bindings);

		return BooleanLiteral.valueOf(leftVal != null && leftVal.equals(rightVal));
	}

	protected QueryValueEvaluationStep prepare(SameTerm node, QueryEvaluationContext context) {
		return supplyBinaryValueEvaluation(node,
				(leftVal, rightVal) -> BooleanLiteral.valueOf(leftVal != null && leftVal.equals(rightVal)), context);
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Coalesce node, BindingSet bindings) throws ValueExprEvaluationException {
		Value result = null;

		for (ValueExpr expr : node.getArguments()) {
			try {
				result = evaluate(expr, bindings);

				// return first result that does not produce an error on
				// evaluation.
				break;
			} catch (QueryEvaluationException ignored) {
			}
		}

		if (result == null) {
			throw new ValueExprEvaluationException(
					"COALESCE arguments do not evaluate to a value: " + node.getSignature());
		}

		return result;
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Compare node, BindingSet bindings) throws QueryEvaluationException {
		Value leftVal = evaluate(node.getLeftArg(), bindings);
		Value rightVal = evaluate(node.getRightArg(), bindings);

		return BooleanLiteral.valueOf(QueryEvaluationUtil.compare(leftVal, rightVal, node.getOperator()));
	}

	protected QueryValueEvaluationStep prepare(Compare node, QueryEvaluationContext context) {
		return supplyBinaryValueEvaluation(node, (leftVal, rightVal) -> BooleanLiteral
				.valueOf(QueryEvaluationUtil.compare(leftVal, rightVal, node.getOperator())), context);
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(MathExpr node, BindingSet bindings) throws QueryEvaluationException {
		// Do the math
		Value leftVal = evaluate(node.getLeftArg(), bindings);
		Value rightVal = evaluate(node.getRightArg(), bindings);

		if (leftVal instanceof Literal && rightVal instanceof Literal) {
			return MathUtil.compute((Literal) leftVal, (Literal) rightVal, node.getOperator());
		}

		throw new ValueExprEvaluationException("Both arguments must be numeric literals");
	}

	private Value mathOperationApplier(MathExpr node, Value leftVal, Value rightVal) {
		if (leftVal instanceof Literal && rightVal instanceof Literal) {
			return MathUtil.compute((Literal) leftVal, (Literal) rightVal, node.getOperator());
		}

		throw new ValueExprEvaluationException("Both arguments must be literals");
	}

	protected QueryValueEvaluationStep prepare(MathExpr node, QueryEvaluationContext context) {
		return supplyBinaryValueEvaluation(node, (leftVal, rightVal) -> mathOperationApplier(node, leftVal, rightVal),
				context);
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(If node, BindingSet bindings) throws QueryEvaluationException {
		Value result;

		boolean conditionIsTrue;

		try {
			Value value = evaluate(node.getCondition(), bindings);
			conditionIsTrue = QueryEvaluationUtil.getEffectiveBooleanValue(value);
		} catch (ValueExprEvaluationException e) {
			// in case of type error, if-construction should result in empty
			// binding.
			return null;
		}

		if (conditionIsTrue) {
			result = evaluate(node.getResult(), bindings);
		} else {
			result = evaluate(node.getAlternative(), bindings);
		}
		return result;
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(In node, BindingSet bindings) throws QueryEvaluationException {
		Value leftValue = evaluate(node.getArg(), bindings);

		// Result is false until a match has been found
		boolean result = false;

		// Use first binding name from tuple expr to compare values
		String bindingName = node.getSubQuery().getBindingNames().iterator().next();

		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = evaluate(node.getSubQuery(), bindings)) {
			while (!result && iter.hasNext()) {
				BindingSet bindingSet = iter.next();

				Value rightValue = bindingSet.getValue(bindingName);

				result = leftValue == null && rightValue == null || leftValue != null && leftValue.equals(rightValue);
			}
		}

		return BooleanLiteral.valueOf(result);
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(ListMemberOperator node, BindingSet bindings) throws QueryEvaluationException {
		List<ValueExpr> args = node.getArguments();
		Value leftValue = evaluate(args.get(0), bindings);

		boolean result = false;
		ValueExprEvaluationException typeError = null;
		for (int i = 1; i < args.size(); i++) {
			ValueExpr arg = args.get(i);
			try {
				Value rightValue = evaluate(arg, bindings);
				result = leftValue == null && rightValue == null;
				if (!result) {
					result = QueryEvaluationUtil.compare(leftValue, rightValue, CompareOp.EQ);
				}
				if (result) {
					break;
				}
			} catch (ValueExprEvaluationException caught) {
				typeError = caught;
			}
		}

		if (typeError != null && !result) {
			// cf. SPARQL spec a type error is thrown if the value is not in the
			// list and one of the list members caused a type error in the
			// comparison.
			throw typeError;
		}

		return BooleanLiteral.valueOf(result);
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(CompareAny node, BindingSet bindings) throws QueryEvaluationException {
		Value leftValue = evaluate(node.getArg(), bindings);

		// Result is false until a match has been found
		boolean result = false;

		// Use first binding name from tuple expr to compare values
		String bindingName = node.getSubQuery().getBindingNames().iterator().next();

		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = evaluate(node.getSubQuery(), bindings)) {
			while (!result && iter.hasNext()) {
				BindingSet bindingSet = iter.next();

				Value rightValue = bindingSet.getValue(bindingName);

				try {
					result = QueryEvaluationUtil.compare(leftValue, rightValue, node.getOperator());
				} catch (ValueExprEvaluationException e) {
					// ignore, maybe next value will match
				}
			}
		}

		return BooleanLiteral.valueOf(result);
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(CompareAll node, BindingSet bindings) throws QueryEvaluationException {
		Value leftValue = evaluate(node.getArg(), bindings);

		// Result is true until a mismatch has been found
		boolean result = true;

		// Use first binding name from tuple expr to compare values
		String bindingName = node.getSubQuery().getBindingNames().iterator().next();

		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = evaluate(node.getSubQuery(), bindings)) {
			while (result && iter.hasNext()) {
				BindingSet bindingSet = iter.next();

				Value rightValue = bindingSet.getValue(bindingName);

				try {
					result = QueryEvaluationUtil.compare(leftValue, rightValue, node.getOperator());
				} catch (ValueExprEvaluationException e) {
					// Exception thrown by ValueCompare.isTrue(...)
					result = false;
				}
			}
		}

		return BooleanLiteral.valueOf(result);
	}

	@Deprecated(forRemoval = true)
	public Value evaluate(Exists node, BindingSet bindings) throws QueryEvaluationException {
		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = evaluate(node.getSubQuery(), bindings)) {
			return BooleanLiteral.valueOf(iter.hasNext());
		}
	}

	private QueryValueEvaluationStep prepare(Exists node, QueryEvaluationContext context)
			throws QueryEvaluationException {
		QueryEvaluationStep subQuery = precompile(node.getSubQuery(), context);
		return bindings -> {
			try (CloseableIteration<BindingSet, QueryEvaluationException> iter = subQuery.evaluate(bindings)) {
				return BooleanLiteral.valueOf(iter.hasNext());
			}
		};
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

	@Deprecated(forRemoval = true)
	public Value evaluate(ValueExprTripleRef node, BindingSet bindings) throws QueryEvaluationException {
		Value subj = evaluate(node.getSubjectVar(), bindings);
		if (!(subj instanceof Resource)) {
			throw new ValueExprEvaluationException("no subject value");
		}
		Value pred = evaluate(node.getPredicateVar(), bindings);
		if (!(pred instanceof IRI)) {
			throw new ValueExprEvaluationException("no predicate value");
		}
		Value obj = evaluate(node.getObjectVar(), bindings);
		if (obj == null) {
			throw new ValueExprEvaluationException("no object value");
		}
		return tripleSource.getValueFactory().createTriple((Resource) subj, (IRI) pred, obj);

	}

	/**
	 * evaluates a TripleRef node returning bindingsets from the matched Triple nodes in the dataset (or explore
	 * standard reification)
	 *
	 * @param ref      to evaluate
	 * @param bindings with the solutions
	 * @return iteration over the solutions
	 */
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TripleRef ref, BindingSet bindings) {
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
	private static class ResultSizeCountingIterator extends IterationWrapper<BindingSet, QueryEvaluationException> {

		CloseableIteration<BindingSet, QueryEvaluationException> iterator;
		QueryModelNode queryModelNode;

		public ResultSizeCountingIterator(CloseableIteration<BindingSet, QueryEvaluationException> iterator,
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
	private static class TimedIterator extends IterationWrapper<BindingSet, QueryEvaluationException> {

		CloseableIteration<BindingSet, QueryEvaluationException> iterator;
		QueryModelNode queryModelNode;

		Stopwatch stopwatch = Stopwatch.createUnstarted();

		public TimedIterator(CloseableIteration<BindingSet, QueryEvaluationException> iterator,
				QueryModelNode queryModelNode) {
			super(iterator);
			this.iterator = iterator;
			this.queryModelNode = queryModelNode;
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
				queryModelNode.setTotalTimeNanosActual(
						queryModelNode.getTotalTimeNanosActual() + stopwatch.elapsed(TimeUnit.NANOSECONDS));
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
	public boolean isTrackResultSize() {
		return trackResultSize;
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
			return bindings -> operation.apply(leftVal, rightStep.evaluate(bindings));
		} else if (rightStep.isConstant()) {
			Value rightVal = rightStep.evaluate(EmptyBindingSet.getInstance());
			return bindings -> operation.apply(leftStep.evaluate(bindings), rightVal);
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

}
