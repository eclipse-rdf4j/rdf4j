/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.federated.FedX;
import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.CheckStatementPattern;
import org.eclipse.rdf4j.federated.algebra.ConjunctiveFilterExpr;
import org.eclipse.rdf4j.federated.algebra.EmptyResult;
import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.ExclusiveTupleExpr;
import org.eclipse.rdf4j.federated.algebra.ExclusiveTupleExprRenderer;
import org.eclipse.rdf4j.federated.algebra.FedXLeftJoin;
import org.eclipse.rdf4j.federated.algebra.FedXService;
import org.eclipse.rdf4j.federated.algebra.FederatedDescribeOperator;
import org.eclipse.rdf4j.federated.algebra.FilterExpr;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.algebra.NJoin;
import org.eclipse.rdf4j.federated.algebra.NUnion;
import org.eclipse.rdf4j.federated.algebra.SingleSourceQuery;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.cache.CacheUtils;
import org.eclipse.rdf4j.federated.cache.SourceSelectionCache;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelServiceExecutor;
import org.eclipse.rdf4j.federated.evaluation.iterator.FederatedDescribeIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.SingleBindingSetIteration;
import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerBoundJoin;
import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerJoin;
import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerLeftJoin;
import org.eclipse.rdf4j.federated.evaluation.join.SynchronousBoundJoin;
import org.eclipse.rdf4j.federated.evaluation.join.SynchronousJoin;
import org.eclipse.rdf4j.federated.evaluation.union.ControlledWorkerUnion;
import org.eclipse.rdf4j.federated.evaluation.union.ParallelGetStatementsTask;
import org.eclipse.rdf4j.federated.evaluation.union.ParallelPreparedAlgebraUnionTask;
import org.eclipse.rdf4j.federated.evaluation.union.ParallelPreparedUnionTask;
import org.eclipse.rdf4j.federated.evaluation.union.ParallelUnionOperatorTask;
import org.eclipse.rdf4j.federated.evaluation.union.SynchronousWorkerUnion;
import org.eclipse.rdf4j.federated.evaluation.union.WorkerUnionBase;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.exception.IllegalQueryException;
import org.eclipse.rdf4j.federated.optimizer.DefaultFedXCostModel;
import org.eclipse.rdf4j.federated.optimizer.ExclusiveTupleExprOptimizer;
import org.eclipse.rdf4j.federated.optimizer.FilterOptimizer;
import org.eclipse.rdf4j.federated.optimizer.GenericInfoOptimizer;
import org.eclipse.rdf4j.federated.optimizer.LimitOptimizer;
import org.eclipse.rdf4j.federated.optimizer.ServiceOptimizer;
import org.eclipse.rdf4j.federated.optimizer.SourceSelection;
import org.eclipse.rdf4j.federated.optimizer.StatementGroupAndJoinOptimizer;
import org.eclipse.rdf4j.federated.optimizer.UnionOptimizer;
import org.eclipse.rdf4j.federated.structures.FedXDataset;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.structures.QueryType;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.ServiceJoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.BadlyDesignedLeftJoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.HashJoinIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.ConstantOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.DisjunctiveConstraintOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.federation.CollectionIteration;
import org.eclipse.rdf4j.repository.sparql.federation.RepositoryFederatedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for the Evaluation strategies.
 *
 * @author Andreas Schwarte
 *
 * @see SailFederationEvalStrategy
 * @see SparqlFederationEvalStrategy
 *
 */
public abstract class FederationEvalStrategy extends StrictEvaluationStrategy {

	private static final Logger log = LoggerFactory.getLogger(FederationEvalStrategy.class);

	protected Executor executor;
	protected SourceSelectionCache cache;

	protected FederationContext federationContext;

	public FederationEvalStrategy(FederationContext federationContext) {
		super(new org.eclipse.rdf4j.query.algebra.evaluation.TripleSource() {

			@Override
			public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(
					Resource subj, IRI pred, Value obj, Resource... contexts)
					throws QueryEvaluationException {
				throw new FedXRuntimeException(
						"Federation Strategy does not support org.openrdf.query.algebra.evaluation.TripleSource#getStatements."
								+
								" If you encounter this exception, please report it.");
			}

			@Override
			public ValueFactory getValueFactory() {
				return SimpleValueFactory.getInstance();
			}
		}, federationContext.getFederatedServiceResolver());
		this.federationContext = federationContext;
		this.executor = federationContext.getManager().getExecutor();
		this.cache = federationContext.getSourceSelectionCache();
	}

	@Override
	public TupleExpr optimize(TupleExpr expr, EvaluationStatistics evaluationStatistics,
			BindingSet bindings) {

		if (!(evaluationStatistics instanceof FederationEvaluationStatistics)) {
			throw new FedXRuntimeException(
					"Expected FederationEvaluationStatistics, was " + evaluationStatistics.getClass());
		}

		FederationEvaluationStatistics stats = (FederationEvaluationStatistics) evaluationStatistics;
		QueryInfo queryInfo = stats.getQueryInfo();
		Dataset dataset = stats.getDataset();

		FederationContext federationContext = queryInfo.getFederationContext();
		List<Endpoint> members;
		if (dataset instanceof FedXDataset) {
			// run the query against a selected set of endpoints
			FedXDataset ds = (FedXDataset) dataset;
			members = federationContext.getEndpointManager().getEndpoints(ds.getEndpoints());
		} else {
			// evaluate against entire federation
			FedX fed = federationContext.getFederation();
			members = fed.getMembers();
		}

		// Clone the tuple expression to allow for more aggressive optimizations
		TupleExpr clone = expr.clone();
		TupleExpr query;
		if (clone instanceof QueryRoot) {
			query = clone;
		} else {
			query = new QueryRoot(clone);
		}

		GenericInfoOptimizer info = new GenericInfoOptimizer(queryInfo);

		// collect information and perform generic optimizations
		info.optimize(query);

		// if the federation has a single member only, evaluate the entire query there
		if (members.size() == 1 && queryInfo.getQuery() != null && propagateServices(info.getServices())
				&& queryInfo.getQueryType() != QueryType.UPDATE) {
			return new SingleSourceQuery(expr, members.get(0), queryInfo);
		}

		if (log.isTraceEnabled()) {
			log.trace("Query before Optimization: " + query);
		}

		/* original RDF4J optimizers */
		new ConstantOptimizer(this).optimize(query, dataset, bindings); // maybe remove this optimizer later

		new DisjunctiveConstraintOptimizer().optimize(query, dataset, bindings);

		/*
		 * TODO add some generic optimizers: - FILTER ?s=1 && ?s=2 => EmptyResult - Remove variables that are not
		 * occurring in query stmts from filters
		 */

		/* custom optimizers, execute only when needed */

		// if the query has a single relevant source (and if it is not a SERVICE query), evaluate at this source only
		// Note: UPDATE queries are always handled in the federation engine to adhere to the configured
		// write strategy. Also DESCRIBE queries are handled in the federation
		Set<Endpoint> relevantSources = performSourceSelection(members, cache, queryInfo, info);
		if (relevantSources.size() == 1 && propagateServices(info.getServices())
				&& queryInfo.getQueryType() != QueryType.UPDATE && queryInfo.getQueryType() != QueryType.DESCRIBE) {
			return new SingleSourceQuery(query, relevantSources.iterator().next(), queryInfo);
		}

		if (info.hasService()) {
			new ServiceOptimizer(queryInfo).optimize(query);
		}

		// optimize unions, if available
		if (info.hasUnion()) {
			new UnionOptimizer(queryInfo).optimize(query);
		}

		optimizeExclusiveExpressions(query, queryInfo, info);

		// optimize statement groups and join order
		optimizeJoinOrder(query, queryInfo, info);

		// potentially push limits (if applicable)
		if (info.hasLimit()) {
			new LimitOptimizer().optimize(query);
		}

		// optimize Filters, if available
		// Note: this is done after the join order is determined to ease filter pushing
		if (info.hasFilter()) {
			new FilterOptimizer().optimize(query);
		}

		if (log.isTraceEnabled()) {
			log.trace("Query after Optimization: " + query);
		}

		return query;
	}

	/**
	 * Perform source selection for all statements of the query. As a result of this method all statement nodes are
	 * annotated with their relevant sources.
	 *
	 * @param members
	 * @param cache
	 * @param queryInfo
	 * @param info
	 * @return the set of relevant endpoints for the entire query
	 */
	protected Set<Endpoint> performSourceSelection(List<Endpoint> members, SourceSelectionCache cache,
			QueryInfo queryInfo,
			GenericInfoOptimizer info) {

		// Source Selection: all nodes are annotated with their source
		SourceSelection sourceSelection = new SourceSelection(members, cache, queryInfo);
		sourceSelection.doSourceSelection(info.getStatements());

		return sourceSelection.getRelevantSources();
	}

	protected void optimizeJoinOrder(TupleExpr query, QueryInfo queryInfo, GenericInfoOptimizer info) {
		// optimize statement groups and join order
		new StatementGroupAndJoinOptimizer(queryInfo, DefaultFedXCostModel.INSTANCE).optimize(query);
	}

	/**
	 * Whether to propagate a {@link SingleSourceQuery} containing SERVICE clauses. By default, the query is always
	 * evaluated within the FedX engine if it contains a SERVICE clause.
	 * <p>
	 * Customized implementation may propagate a {@link SingleSourceQuery} including the SERVICE clause (e.g. for
	 * Wikidata the Label service can only be accessed in the wikidata endpoint.
	 * </p>
	 *
	 * @param serviceNodes
	 * @return if <code>true</code>, a {@link SingleSourceQuery} containing SERVICE clauses is propagated as-is
	 */
	protected boolean propagateServices(List<Service> serviceNodes) {
		boolean hasServices = serviceNodes != null && !serviceNodes.isEmpty();
		if (hasServices) {
			return false;
		}
		return true;
	}

	/**
	 * Optimize {@link ExclusiveTupleExpr}, e.g. restructure the exclusive parts of the query AST.
	 *
	 * @param query
	 * @param queryInfo
	 * @param info
	 */
	protected void optimizeExclusiveExpressions(TupleExpr query, QueryInfo queryInfo, GenericInfoOptimizer info) {
		// identify exclusive expressions
		new ExclusiveTupleExprOptimizer().optimize(query);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(
			TupleExpr expr, BindingSet bindings)
			throws QueryEvaluationException {

		if (expr instanceof StatementTupleExpr) {
			return ((StatementTupleExpr) expr).evaluate(bindings);
		}

		if (expr instanceof NJoin) {
			return evaluateNJoin((NJoin) expr, bindings);
		}

		if (expr instanceof NUnion) {
			return evaluateNaryUnion((NUnion) expr, bindings);
		}

		if (expr instanceof ExclusiveGroup) {
			return ((ExclusiveGroup) expr).evaluate(bindings);
		}

		if (expr instanceof ExclusiveTupleExpr) {
			return evaluateExclusiveTupleExpr((ExclusiveTupleExpr) expr, bindings);
		}

		if (expr instanceof FedXLeftJoin) {
			return evaluateLeftJoin((FedXLeftJoin) expr, bindings);
		}

		if (expr instanceof SingleSourceQuery) {
			return evaluateSingleSourceQuery((SingleSourceQuery) expr, bindings);
		}

		if (expr instanceof FedXService) {
			return evaluateService((FedXService) expr, bindings);
		}

		if (expr instanceof EmptyResult) {
			return new EmptyIteration<>();
		}

		return super.evaluate(expr, bindings);
	}

	@Override
	public QueryEvaluationStep precompile(
			TupleExpr expr, QueryEvaluationContext context)
			throws QueryEvaluationException {
		if (expr instanceof Join) {
			return QueryEvaluationStep.minimal(this, expr);
		}
		if (expr instanceof StatementTupleExpr) {
			return QueryEvaluationStep.minimal(this, expr);
		}

		if (expr instanceof NJoin) {
			return prepareNJoin((NJoin) expr, context);
		}

		if (expr instanceof NUnion) {
			return prepareNaryUnion((NUnion) expr, context);
		}

		if (expr instanceof ExclusiveGroup) {
			return QueryEvaluationStep.minimal(this, expr);
		}

		if (expr instanceof ExclusiveTupleExpr) {
			return prepareExclusiveTupleExpr((ExclusiveTupleExpr) expr, context);
		}

		if (expr instanceof FedXLeftJoin) {
			return prepareLeftJoin((FedXLeftJoin) expr, context);
		}

		if (expr instanceof SingleSourceQuery) {
			return QueryEvaluationStep.minimal(this, expr);
		}

		if (expr instanceof FedXService) {
			return QueryEvaluationStep.minimal(this, expr);
		}

		if (expr instanceof EmptyResult) {
			return QueryEvaluationStep.minimal(this, expr);
		}

		return super.precompile(expr, context);
	}

	/**
	 * Retrieve the statements matching the provided subject, predicate and object value from the federation members.
	 * <p>
	 *
	 * For a bound statement, i.e. a statement with no free variables, the statement itself is returned if some member
	 * has this statement, an empty iteration otherwise.
	 * <p>
	 *
	 * If the statement has free variables, i.e. one of the provided arguments in <code>null</code>, the union of
	 * results from relevant statement sources is constructed.
	 *
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param contexts
	 * @return the statement iteration
	 *
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	public CloseableIteration<Statement, QueryEvaluationException> getStatements(QueryInfo queryInfo, Resource subj,
			IRI pred, Value obj, Resource... contexts)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {

		List<Endpoint> members = federationContext.getFederation().getMembers();

		// a bound query: if at least one fed member provides results
		// return the statement, otherwise empty result
		if (subj != null && pred != null && obj != null) {
			if (CacheUtils.checkCacheUpdateCache(cache, members, subj, pred, obj, queryInfo, contexts)) {
				return new SingletonIteration<>(
						FedXUtil.valueFactory().createStatement(subj, pred, obj));
			}
			return new EmptyIteration<>();
		}

		// form the union of results from relevant endpoints
		List<StatementSource> sources = CacheUtils.checkCacheForStatementSourcesUpdateCache(cache, members, subj, pred,
				obj, queryInfo, contexts);

		if (sources.isEmpty()) {
			return new EmptyIteration<>();
		}

		if (sources.size() == 1) {
			Endpoint e = federationContext.getEndpointManager().getEndpoint(sources.get(0).getEndpointID());
			return e.getTripleSource().getStatements(subj, pred, obj, queryInfo, contexts);
		}

		// TODO why not collect in parallel?
		WorkerUnionBase<Statement> union = new SynchronousWorkerUnion<>(queryInfo);

		for (StatementSource source : sources) {
			Endpoint e = federationContext.getEndpointManager().getEndpoint(source.getEndpointID());
			ParallelGetStatementsTask task = new ParallelGetStatementsTask(union, e, subj, pred, obj, queryInfo,
					contexts);
			union.addTask(task);
		}

		// run the union in a separate thread
		executor.execute(union);

		// TODO distinct iteration ?

		return union;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateService(FedXService service,
			BindingSet bindings) throws QueryEvaluationException {

		ParallelServiceExecutor pe = new ParallelServiceExecutor(service, this, bindings, federationContext);
		pe.run(); // non-blocking (blocking happens in the iterator)
		return pe;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateSingleSourceQuery(SingleSourceQuery query,
			BindingSet bindings) throws QueryEvaluationException {

		try {
			Endpoint source = query.getSource();
			return source.getTripleSource()
					.getStatements(query.getQueryString(), bindings, query.getQueryInfo().getQueryType(),
							query.getQueryInfo());
		} catch (RepositoryException | MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}

	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateNJoin(NJoin join, BindingSet bindings)
			throws QueryEvaluationException {

		return precompile(join).evaluate(bindings);
	}

	protected QueryEvaluationStep prepareNJoin(NJoin join, QueryEvaluationContext context)
			throws QueryEvaluationException {

		QueryEvaluationStep resultProvider = precompile(join.getArg(0), context);

		ControlledWorkerScheduler<BindingSet> joinScheduler = federationContext.getManager().getJoinScheduler();

		return bindings -> {
			boolean completed = false;
			CloseableIteration<BindingSet, QueryEvaluationException> result = resultProvider.evaluate(bindings);
			try {
				for (int i = 1, n = join.getNumberOfArguments(); i < n; i++) {

					result = executeJoin(joinScheduler, result, join.getArg(i), join.getJoinVariables(i), bindings,
							join.getQueryInfo());
				}
				completed = true;
			} finally {
				if (!completed) {
					result.close();
				}
			}
			return result;
		};

	}

	/**
	 * Evaluate a {@link FedXLeftJoin} (i.e. an OPTIONAL clause)
	 *
	 * @param leftJoin
	 * @param bindings
	 * @return the resulting iteration
	 * @throws QueryEvaluationException
	 * @see StrictEvaluationStrategy#evaluate(org.eclipse.rdf4j.query.algebra.LeftJoin, BindingSet)
	 */
	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateLeftJoin(FedXLeftJoin leftJoin,
			final BindingSet bindings) throws QueryEvaluationException {

		return precompile(leftJoin).evaluate(bindings);
	}

	protected QueryEvaluationStep prepareLeftJoin(FedXLeftJoin leftJoin, QueryEvaluationContext context)
			throws QueryEvaluationException {
		/*
		 * NOTE: this implementation is taken from StrictEvaluationStrategy.evaluate(LeftJoin, BindingSet)
		 *
		 * However, we have to take care for some concurrency scheduling to guarantee the order in which subqueries are
		 * executed.
		 */

		if (TupleExprs.containsSubquery(leftJoin.getRightArg())) {
			return new QueryEvaluationStep() {
				final QueryEvaluationStep leftES = precompile(leftJoin.getLeftArg(), context);
				final QueryEvaluationStep rightES = precompile(leftJoin.getRightArg(), context);

				@Override
				public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
					String[] hashJoinAttributeNames = HashJoinIteration.hashJoinAttributeNames(leftJoin);
					return new HashJoinIteration(leftES, rightES, bindings, true, hashJoinAttributeNames, context);
				}

			};
		} else {
			// Check whether optional join is "well designed" as defined in section
			// 4.2 of "Semantics and Complexity of SPARQL", 2006, Jorge Pérez et al.
			VarNameCollector optionalVarCollector = new VarNameCollector();
			leftJoin.getRightArg().visit(optionalVarCollector);
			if (leftJoin.hasCondition()) {
				leftJoin.getCondition().visit(optionalVarCollector);
			}

			Set<String> leftBindingNames = leftJoin.getLeftArg().getBindingNames();
			Set<String> problemVars = retainAll(optionalVarCollector.getVarNames(), leftBindingNames);

			QueryEvaluationStep leftPrepared = precompile(leftJoin.getLeftArg(), context);
			// left join is "well designed"
			ControlledWorkerScheduler<BindingSet> scheduler = federationContext.getManager().getLeftJoinScheduler();
			return bindings -> {

				if (problemVars.containsAll(bindings.getBindingNames())) {
					var leftIter = leftPrepared.evaluate(bindings);
					ControlledWorkerLeftJoin join = new ControlledWorkerLeftJoin(scheduler, FederationEvalStrategy.this,
							leftIter, leftJoin, bindings, leftJoin.getQueryInfo());
					executor.execute(join);
					return join;
				} else {
					Set<String> problemVarsClone = new HashSet<>(problemVars);
					problemVarsClone.retainAll(bindings.getBindingNames());
					return new BadlyDesignedLeftJoinIterator(FederationEvalStrategy.this, leftJoin, bindings,
							problemVarsClone, context);
				}
			};
		}
	}

	private Set<String> retainAll(Set<String> problemVars, Set<String> leftBindingNames) {
		if (!leftBindingNames.isEmpty() && !problemVars.isEmpty()) {
			if (leftBindingNames.size() > problemVars.size()) {
				for (String problemVar : problemVars) {
					if (leftBindingNames.contains(problemVar)) {
						HashSet<String> ret = new HashSet<>(problemVars);
						ret.removeAll(leftBindingNames);
						return ret;
					}
				}
			} else {
				for (String leftBindingName : leftBindingNames) {
					if (problemVars.contains(leftBindingName)) {
						HashSet<String> ret = new HashSet<>(problemVars);
						ret.removeAll(leftBindingNames);
						return ret;
					}
				}
			}
		}
		return problemVars;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateNaryUnion(NUnion union, BindingSet bindings)
			throws QueryEvaluationException {
		return precompile(union).evaluate(bindings);
	}

	public QueryEvaluationStep prepareNaryUnion(NUnion union, QueryEvaluationContext context)
			throws QueryEvaluationException {

		ControlledWorkerScheduler<BindingSet> unionScheduler = federationContext.getManager().getUnionScheduler();
		ControlledWorkerUnion<BindingSet> unionRunnable = new ControlledWorkerUnion<>(unionScheduler,
				union.getQueryInfo());

		int numberOfArguments = union.getNumberOfArguments();
		QueryEvaluationStep[] args = new QueryEvaluationStep[numberOfArguments];
		for (int i = 0; i < numberOfArguments; i++) {
			args[i] = precompile(union.getArg(i), context);
		}
		return bindings -> {
			for (int i = 0; i < numberOfArguments; i++) {
				unionRunnable.addTask(new ParallelUnionOperatorTask(unionRunnable, args[i], bindings));
			}
			executor.execute(unionRunnable);

			return unionRunnable;
		};
	}

	/**
	 * Execute the join in a separate thread using some join executor.
	 *
	 * Join executors are for instance: - {@link SynchronousJoin} - {@link SynchronousBoundJoin} -
	 * {@link ControlledWorkerJoin} - {@link ControlledWorkerBoundJoin}
	 *
	 * For endpoint federation use controlled worker bound join, for local federation use controlled worker join. The
	 * other operators are there for completeness.
	 *
	 * Use {@link FederationEvalStrategy#executor} to execute the join (it is a runnable).
	 *
	 * @param joinScheduler
	 * @param leftIter
	 * @param rightArg
	 * @param joinVariables
	 * @param bindings
	 * @return the result
	 * @throws QueryEvaluationException
	 */
	protected abstract CloseableIteration<BindingSet, QueryEvaluationException> executeJoin(
			ControlledWorkerScheduler<BindingSet> joinScheduler,
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter, TupleExpr rightArg,
			Set<String> joinVariables, BindingSet bindings, QueryInfo queryInfo) throws QueryEvaluationException;

	public abstract CloseableIteration<BindingSet, QueryEvaluationException> evaluateExclusiveGroup(
			ExclusiveGroup group, BindingSet bindings)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException;

	/**
	 * Evaluate an {@link ExclusiveTupleExpr}. The default implementation converts the given expression to a SELECT
	 * query string and evaluates it at the source.
	 *
	 * @param expr
	 * @param bindings
	 * @return the result
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateExclusiveTupleExpr(
			ExclusiveTupleExpr expr,
			BindingSet bindings) throws RepositoryException, MalformedQueryException, QueryEvaluationException {

		return precompile(expr).evaluate(bindings);
	}

	protected QueryEvaluationStep prepareExclusiveTupleExpr(
			ExclusiveTupleExpr expr,
			QueryEvaluationContext context)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {

		if (expr instanceof StatementTupleExpr) {
			return bindings -> ((StatementTupleExpr) expr).evaluate(bindings);
		}

		if (!(expr instanceof ExclusiveTupleExprRenderer)) {
			return super.precompile(expr);
		}

		Endpoint ownedEndpoint = federationContext
				.getEndpointManager()
				.getEndpoint(expr.getOwner().getEndpointID());
		TripleSource t = ownedEndpoint.getTripleSource();

		return bindings -> {
			AtomicBoolean isEvaluated = new AtomicBoolean(false);
			FilterValueExpr filterValueExpr = null; // TODO consider optimization using FilterTuple
			try {
				String preparedQuery = QueryStringUtil.selectQueryString((ExclusiveTupleExprRenderer) expr,
						bindings, filterValueExpr, isEvaluated, expr.getQueryInfo().getDataset());
				return t.getStatements(preparedQuery, bindings, (isEvaluated.get() ? null : filterValueExpr),
						expr.getQueryInfo());
			} catch (IllegalQueryException e) {
				/* no projection vars, e.g. local vars only, can occur in joins */
				if (t.hasStatements(expr, bindings)) {
					return new SingleBindingSetIteration(bindings);
				}
				return new EmptyIteration<>();
			}

		};

	}

	/**
	 * Evaluate a bound join at the relevant endpoint, i.e. i.e. for a group of bindings retrieve results for the bound
	 * statement from the relevant endpoints
	 *
	 * @param stmt
	 * @param bindings
	 * @return the result iteration
	 * @throws QueryEvaluationException
	 */
	public abstract CloseableIteration<BindingSet, QueryEvaluationException> evaluateBoundJoinStatementPattern(
			StatementTupleExpr stmt, final List<BindingSet> bindings) throws QueryEvaluationException;

	/**
	 * Perform a grouped check at the relevant endpoints, i.e. for a group of bindings keep only those for which at
	 * least one endpoint provides a result to the bound statement.
	 *
	 * @param stmt
	 * @param bindings
	 * @return the result iteration
	 * @throws QueryEvaluationException
	 */
	public abstract CloseableIteration<BindingSet, QueryEvaluationException> evaluateGroupedCheck(
			CheckStatementPattern stmt, final List<BindingSet> bindings) throws QueryEvaluationException;

	/**
	 * Evaluate a SERVICE using vectored evaluation, taking the provided bindings as input.
	 *
	 * See {@link ControlledWorkerBoundJoin} and {@link FedXConfig#getEnableServiceAsBoundJoin()}
	 *
	 * @param service
	 * @param bindings
	 * @return the result iteration
	 * @throws QueryEvaluationException
	 */
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateService(FedXService service,
			final List<BindingSet> bindings) throws QueryEvaluationException {

		Var serviceRef = service.getService().getServiceRef();
		String serviceUri;
		if (serviceRef.hasValue()) {
			serviceUri = serviceRef.getValue().stringValue();
		} else {
			return new ServiceJoinIterator(new CollectionIteration<>(bindings),
					service.getService(), EmptyBindingSet.getInstance(), this);
		}

		// use vectored evaluation
		FederatedService fs = getService(serviceUri);
		if (fs instanceof RepositoryFederatedService) {
			// set the bound join block size to 0 => leave block size up to FedX engine
			((RepositoryFederatedService) fs).setBoundJoinBlockSize(0);
		}
		return fs.evaluate(service.getService(), new CollectionIteration<>(bindings),
				service.getService().getBaseURI());
	}

	@Override
	public Value evaluate(ValueExpr expr, BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException {

		if (expr instanceof FilterExpr) {
			return evaluate((FilterExpr) expr, bindings);
		}
		if (expr instanceof ConjunctiveFilterExpr) {
			return evaluate((ConjunctiveFilterExpr) expr, bindings);
		}

		return super.evaluate(expr, bindings);
	}

	@Override
	public QueryValueEvaluationStep precompile(ValueExpr expr, QueryEvaluationContext context)
			throws ValueExprEvaluationException, QueryEvaluationException {

		if (expr instanceof FilterExpr) {
			return prepare((FilterExpr) expr, context);
		}
		if (expr instanceof ConjunctiveFilterExpr) {
			return prepare((ConjunctiveFilterExpr) expr, context);
		}

		return super.precompile(expr, context);
	}

	public Value evaluate(FilterExpr node, BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException {

		Value v = evaluate(node.getExpression(), bindings);
		return BooleanLiteral.valueOf(QueryEvaluationUtil.getEffectiveBooleanValue(v));
	}

	protected QueryValueEvaluationStep prepare(FilterExpr node, QueryEvaluationContext context)
			throws ValueExprEvaluationException, QueryEvaluationException {

		QueryValueEvaluationStep expr = precompile(node.getExpression(), context);
		return bindings -> {
			Value v = expr.evaluate(bindings);
			return BooleanLiteral.valueOf(QueryEvaluationUtil.getEffectiveBooleanValue(v));
		};
	}

	public Value evaluate(ConjunctiveFilterExpr node, BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException {

		return prepare(node, new QueryEvaluationContext.Minimal(dataset)).evaluate(bindings);
	}

	protected QueryValueEvaluationStep prepare(ConjunctiveFilterExpr node, QueryEvaluationContext context)
			throws ValueExprEvaluationException, QueryEvaluationException {

		List<QueryValueEvaluationStep> collect = node.getExpressions()
				.stream()
				.map((e) -> precompile(e, context))
				.collect(Collectors.toList());

		return bindings -> {
			ValueExprEvaluationException error = null;
			try {
				for (QueryValueEvaluationStep ves : collect) {
					Value v = ves.evaluate(bindings);
					if (QueryEvaluationUtil.getEffectiveBooleanValue(v) == false) {
						return BooleanLiteral.FALSE;
					}
				}
			} catch (ValueExprEvaluationException e) {
				error = e;
			}
			if (error != null)
				throw error;
			return BooleanLiteral.TRUE;
		};
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(DescribeOperator operator,
			final BindingSet bindings) throws QueryEvaluationException {

		if (!(operator instanceof FederatedDescribeOperator)) {
			throw new FedXRuntimeException(
					"Expected a FedXDescribeOperator Node. Found " + operator.getClass() + " instead.");
		}
		CloseableIteration<BindingSet, QueryEvaluationException> iter = evaluate(operator.getArg(), bindings);
		// Note: we need to evaluate the DESCRIBE over the entire federation
		return new FederatedDescribeIteration(iter, this, operator.getBindingNames(), bindings,
				((FederatedDescribeOperator) operator).getQueryInfo());
	}

	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateAtStatementSources(Object preparedQuery,
			List<StatementSource> statementSources, QueryInfo queryInfo) throws QueryEvaluationException {
		if (preparedQuery instanceof String) {
			return evaluateAtStatementSources((String) preparedQuery, statementSources, queryInfo);
		}
		if (preparedQuery instanceof TupleExpr) {
			return evaluateAtStatementSources((TupleExpr) preparedQuery, statementSources, queryInfo);
		}
		throw new RuntimeException(
				"Unsupported type for prepared query: " + preparedQuery.getClass().getCanonicalName());
	}

	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateAtStatementSources(String preparedQuery,
			List<StatementSource> statementSources, QueryInfo queryInfo) throws QueryEvaluationException {

		try {
			CloseableIteration<BindingSet, QueryEvaluationException> result;

			if (statementSources.size() == 1) {
				Endpoint ownedEndpoint = federationContext.getEndpointManager()
						.getEndpoint(statementSources.get(0).getEndpointID());
				org.eclipse.rdf4j.federated.evaluation.TripleSource t = ownedEndpoint.getTripleSource();
				result = t.getStatements(preparedQuery, EmptyBindingSet.getInstance(), (FilterValueExpr) null,
						queryInfo);
			} else {
				WorkerUnionBase<BindingSet> union = federationContext.getManager().createWorkerUnion(queryInfo);

				for (StatementSource source : statementSources) {
					Endpoint ownedEndpoint = federationContext.getEndpointManager().getEndpoint(source.getEndpointID());
					union.addTask(new ParallelPreparedUnionTask(union, preparedQuery, ownedEndpoint,
							EmptyBindingSet.getInstance(), null, queryInfo));
				}

				union.run();
				result = union;

				// TODO we should add some DISTINCT here to have SET semantics
			}

			return result;

		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new QueryEvaluationException(e);
		}
	}

	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateAtStatementSources(
			TupleExpr preparedQuery, List<StatementSource> statementSources, QueryInfo queryInfo)
			throws QueryEvaluationException {

		try {
			CloseableIteration<BindingSet, QueryEvaluationException> result;

			if (statementSources.size() == 1) {
				Endpoint ownedEndpoint = federationContext.getEndpointManager()
						.getEndpoint(statementSources.get(0).getEndpointID());
				org.eclipse.rdf4j.federated.evaluation.TripleSource t = ownedEndpoint.getTripleSource();
				result = t.getStatements(preparedQuery, EmptyBindingSet.getInstance(), null, queryInfo);
			} else {
				WorkerUnionBase<BindingSet> union = federationContext.getManager().createWorkerUnion(queryInfo);

				for (StatementSource source : statementSources) {
					Endpoint ownedEndpoint = federationContext.getEndpointManager().getEndpoint(source.getEndpointID());
					union.addTask(new ParallelPreparedAlgebraUnionTask(union, preparedQuery, ownedEndpoint,
							EmptyBindingSet.getInstance(), null, queryInfo));
				}

				union.run();
				result = union;

				// TODO we should add some DISTINCT here to have SET semantics
			}

			return result;

		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new QueryEvaluationException(e);
		}
	}

}
