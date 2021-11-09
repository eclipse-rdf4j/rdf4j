/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.DelayedIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.IntersectIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.common.iteration.LimitIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.iteration.OffsetIteration;
import org.eclipse.rdf4j.common.iteration.ReducedIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
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
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.ValueExprTripleRef;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.evaluation.RDFStarTripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.ServiceJoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.function.datetime.Now;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.BadlyDesignedLeftJoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.DescribeIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ExtensionIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.FilterIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.GroupIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.HashJoinIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.JoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.LeftJoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.MultiProjectionIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.OrderIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.PathIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ProjectionIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.SPARQLMinusIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ZeroLengthPathIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.util.EvaluationStrategies;
import org.eclipse.rdf4j.query.algebra.evaluation.util.MathUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.OrderComparator;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;
import org.eclipse.rdf4j.query.algebra.helpers.VarNameCollector;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
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
	private Value sharedValueOfNow;

	private final long iterationCacheSyncThreshold;

	// track the results size that each node in the query plan produces during execution
	private boolean trackResultSize;

	// track the exeution time of each node in the plan
	private boolean trackTime;

	private final UUID uuid;

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
		this.uuid = UUID.randomUUID();
		EvaluationStrategies.register(this);
		this.trackResultSize = trackResultSize;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public UUID getUUID() {
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

	@Override
	public QueryOptimizerPipeline getOptimizerPipeline() {
		return pipeline;
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
		} else if (expr instanceof ExternalSet) {
			ret = evaluate((ExternalSet) expr, bindings);
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

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(ArbitraryLengthPath alp,
			final BindingSet bindings) throws QueryEvaluationException {
		final Scope scope = alp.getScope();
		final Var subjectVar = alp.getSubjectVar();
		final TupleExpr pathExpression = alp.getPathExpression();
		final Var objVar = alp.getObjectVar();
		final Var contextVar = alp.getContextVar();
		final long minLength = alp.getMinLength();

		return new PathIteration(this, scope, subjectVar, pathExpression, objVar, contextVar, minLength, bindings);
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(ZeroLengthPath zlp,
			final BindingSet bindings) throws QueryEvaluationException {

		final Var subjectVar = zlp.getSubjectVar();
		final Var objVar = zlp.getObjectVar();
		final Var contextVar = zlp.getContextVar();

		Value subj = null;
		try {
			subj = evaluate(subjectVar, bindings);
		} catch (QueryEvaluationException ignored) {
		}

		Value obj = null;
		try {
			obj = evaluate(objVar, bindings);
		} catch (QueryEvaluationException ignored) {
		}

		if (subj != null && obj != null) {
			if (!subj.equals(obj)) {
				return new EmptyIteration<>();
			}
		}

		return getZeroLengthPathIterator(bindings, subjectVar, objVar, contextVar, subj, obj);
	}

	protected ZeroLengthPathIteration getZeroLengthPathIterator(final BindingSet bindings, final Var subjectVar,
			final Var objVar, final Var contextVar, Value subj, Value obj) {
		return new ZeroLengthPathIteration(this, subjectVar, objVar, subj, obj, contextVar, bindings);
	}

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

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service service, BindingSet bindings)
			throws QueryEvaluationException {
		Var serviceRef = service.getServiceRef();

		String serviceUri;
		if (serviceRef.hasValue()) {
			serviceUri = serviceRef.getValue().stringValue();
		} else {
			if (bindings != null && bindings.getValue(serviceRef.getName()) != null) {
				serviceUri = bindings.getBinding(serviceRef.getName()).getValue().stringValue();
			} else {
				throw new QueryEvaluationException("SERVICE variables must be bound at evaluation time.");
			}
		}

		try {

			FederatedService fs = serviceResolver.getService(serviceUri);

			// create a copy of the free variables, and remove those for which
			// bindings are available (we can set them as constraints!)
			Set<String> freeVars = new HashSet<>(service.getServiceVars());
			freeVars.removeAll(bindings.getBindingNames());

			// Get bindings from values pre-bound into variables.
			MapBindingSet allBindings = new MapBindingSet();
			for (Binding binding : bindings) {
				allBindings.addBinding(binding.getName(), binding.getValue());
			}

			Set<Var> boundVars = getBoundVariables(service);
			for (Var boundVar : boundVars) {
				freeVars.remove(boundVar.getName());
				allBindings.addBinding(boundVar.getName(), boundVar.getValue());
			}
			bindings = allBindings;

			String baseUri = service.getBaseURI();

			// special case: no free variables => perform ASK query
			if (freeVars.isEmpty()) {
				boolean exists = fs.ask(service, bindings, baseUri);

				// check if triples are available (with inserted bindings)
				if (exists) {
					return new SingletonIteration<>(bindings);
				} else {
					return new EmptyIteration<>();
				}

			}

			// otherwise: perform a SELECT query
			return fs.select(service, freeVars, bindings,
					baseUri);

		} catch (RuntimeException e) {
			// suppress exceptions if silent
			if (service.isSilent()) {
				return new SingletonIteration<>(bindings);
			} else {
				throw e;
			}
		}

	}

	private Set<Var> getBoundVariables(Service service) {
		BoundVarVisitor visitor = new BoundVarVisitor();
		visitor.meet(service);
		return visitor.boundVars;
	}

	private static class BoundVarVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		private final Set<Var> boundVars = new HashSet<>();

		@Override
		public void meet(Var var) {
			if (var.hasValue()) {
				boundVars.add(var);
			}
		}
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(DescribeOperator operator,
			final BindingSet bindings) throws QueryEvaluationException {
		CloseableIteration<BindingSet, QueryEvaluationException> iter = evaluate(operator.getArg(), bindings);
		return new DescribeIteration(iter, this, operator.getBindingNames(), bindings);
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(StatementPattern statementPattern,
			final BindingSet bindings) throws QueryEvaluationException {
		final Var subjVar = statementPattern.getSubjectVar();
		final Var predVar = statementPattern.getPredicateVar();
		final Var objVar = statementPattern.getObjectVar();
		final Var conVar = statementPattern.getContextVar();

		final Value subjValue = getVarValue(subjVar, bindings);
		final Value predValue = getVarValue(predVar, bindings);
		final Value objValue = getVarValue(objVar, bindings);
		final Value contextValue = getVarValue(conVar, bindings);

		CloseableIteration<? extends Statement, QueryEvaluationException> stIter1 = null;
		CloseableIteration<? extends Statement, QueryEvaluationException> stIter2 = null;
		CloseableIteration<? extends Statement, QueryEvaluationException> stIter3 = null;
		ConvertingIteration<Statement, BindingSet, QueryEvaluationException> resultingIterator = null;

		if (isUnbound(subjVar, bindings) || isUnbound(predVar, bindings) || isUnbound(objVar, bindings)
				|| isUnbound(conVar, bindings)) {
			// the variable must remain unbound for this solution see https://www.w3.org/TR/sparql11-query/#assignment
			return new EmptyIteration<>();
		}

		boolean allGood = false;
		try {
			try {
				Resource[] contexts;

				Set<IRI> graphs = null;
				boolean emptyGraph = false;

				if (dataset != null) {
					if (statementPattern.getScope() == Scope.DEFAULT_CONTEXTS) {
						graphs = dataset.getDefaultGraphs();
						emptyGraph = graphs.isEmpty() && !dataset.getNamedGraphs().isEmpty();
					} else {
						graphs = dataset.getNamedGraphs();
						emptyGraph = graphs.isEmpty() && !dataset.getDefaultGraphs().isEmpty();
					}
				}

				if (emptyGraph) {
					// Search zero contexts
					return new EmptyIteration<>();
				} else if (graphs == null || graphs.isEmpty()) {
					// store default behaviour
					if (contextValue != null) {
						if (RDF4J.NIL.equals(contextValue) || SESAME.NIL.equals(contextValue)) {
							contexts = new Resource[] { (Resource) null };
						} else {
							contexts = new Resource[] { (Resource) contextValue };
						}
					}
					/*
					 * TODO activate this to have an exclusive (rather than inclusive) interpretation of the default
					 * graph in SPARQL querying. else if (statementPattern.getScope() == Scope.DEFAULT_CONTEXTS ) {
					 * contexts = new Resource[] { (Resource)null }; }
					 */
					else {
						contexts = new Resource[0];
					}
				} else if (contextValue != null) {
					if (graphs.contains(contextValue)) {
						contexts = new Resource[] { (Resource) contextValue };
					} else {
						// Statement pattern specifies a context that is not part of
						// the dataset
						return new EmptyIteration<>();
					}
				} else {
					contexts = new Resource[graphs.size()];
					int i = 0;
					for (IRI graph : graphs) {
						IRI context = null;
						if (!(RDF4J.NIL.equals(graph) || SESAME.NIL.equals(graph))) {
							context = graph;
						}
						contexts[i++] = context;
					}
				}

				stIter1 = tripleSource.getStatements((Resource) subjValue, (IRI) predValue, objValue, contexts);

				if (contexts.length == 0 && statementPattern.getScope() == Scope.NAMED_CONTEXTS) {
					// Named contexts are matched by retrieving all statements from
					// the store and filtering out the statements that do not have a
					// context.
					stIter2 = new FilterIteration<Statement, QueryEvaluationException>(stIter1) {

						@Override
						protected boolean accept(Statement st) {
							return st.getContext() != null;
						}

					}; // end anonymous class
				} else {
					stIter2 = stIter1;
				}
			} catch (ClassCastException e) {
				// Invalid value type for subject, predicate and/or context
				return new EmptyIteration<>();
			}

			// The same variable might have been used multiple times in this
			// StatementPattern, verify value equality in those cases.
			// TODO: skip this filter if not necessary
			stIter3 = new FilterIteration<Statement, QueryEvaluationException>(stIter2) {

				@Override
				protected boolean accept(Statement st) {
					Resource subj = st.getSubject();
					IRI pred = st.getPredicate();
					Value obj = st.getObject();
					Resource context = st.getContext();

					if (subjVar != null && subjValue == null) {
						if (subjVar.equals(predVar) && !subj.equals(pred)) {
							return false;
						}
						if (subjVar.equals(objVar) && !subj.equals(obj)) {
							return false;
						}
						if (subjVar.equals(conVar) && !subj.equals(context)) {
							return false;
						}
					}

					if (predVar != null && predValue == null) {
						if (predVar.equals(objVar) && !pred.equals(obj)) {
							return false;
						}
						if (predVar.equals(conVar) && !pred.equals(context)) {
							return false;
						}
					}

					if (objVar != null && objValue == null) {
						if (objVar.equals(conVar) && !obj.equals(context)) {
							return false;
						}
					}

					return true;
				}
			};

			// Return an iterator that converts the statements to var bindings
			resultingIterator = new ConvertingIteration<Statement, BindingSet, QueryEvaluationException>(stIter3) {

				@Override
				protected BindingSet convert(Statement st) {
					QueryBindingSet result = new QueryBindingSet(bindings);

					if (subjVar != null && !subjVar.isConstant() && !result.hasBinding(subjVar.getName())) {
						result.addBinding(subjVar.getName(), st.getSubject());
					}
					if (predVar != null && !predVar.isConstant() && !result.hasBinding(predVar.getName())) {
						result.addBinding(predVar.getName(), st.getPredicate());
					}
					if (objVar != null && !objVar.isConstant() && !result.hasBinding(objVar.getName())) {
						result.addBinding(objVar.getName(), st.getObject());
					}
					if (conVar != null && !conVar.isConstant() && !result.hasBinding(conVar.getName())
							&& st.getContext() != null) {
						result.addBinding(conVar.getName(), st.getContext());
					}

					return result;
				}
			};
			allGood = true;

			return resultingIterator;

		} finally {
			if (!allGood) {
				try {
					if (resultingIterator != null) {
						resultingIterator.close();
					}
				} finally {
					try {
						if (stIter3 != null) {
							stIter3.close();
						}
					} finally {
						try {
							if (stIter2 != null) {
								stIter2.close();
							}
						} finally {
							if (stIter1 != null) {
								stIter1.close();
							}
						}
					}
				}
			}
		}
	}

	protected boolean isUnbound(Var var, BindingSet bindings) {
		if (var == null) {
			return false;
		} else {
			return bindings.hasBinding(var.getName()) && bindings.getValue(var.getName()) == null;
		}
	}

	protected Value getVarValue(Var var, BindingSet bindings) {
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

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSetAssignment bsa,
			BindingSet bindings) throws QueryEvaluationException {
		final Iterator<BindingSet> assignments = bsa.getBindingSets().iterator();
		if (bindings.size() == 0) {
			// we can just return the assignments directly without checking existing bindings
			return new CloseableIteratorIteration<>(assignments);
		}

		// we need to verify that new binding assignments do not overwrite existing bindings
		CloseableIteration<BindingSet, QueryEvaluationException> result;

		result = new LookAheadIteration<BindingSet, QueryEvaluationException>() {

			@Override
			protected BindingSet getNextElement() throws QueryEvaluationException {
				QueryBindingSet nextResult = null;
				while (nextResult == null && assignments.hasNext()) {
					final BindingSet assignedBindings = assignments.next();

					for (String name : assignedBindings.getBindingNames()) {
						if (nextResult == null) {
							nextResult = new QueryBindingSet(bindings);
						}

						final Value assignedValue = assignedBindings.getValue(name);
						if (assignedValue != null) {
							// check that the binding assignment does not overwrite existing bindings.
							Value existingValue = bindings.getValue(name);
							if (existingValue == null || assignedValue.equals(existingValue)) {
								if (existingValue == null) {
									// we are not overwriting an existing binding.
									nextResult.addBinding(name, assignedValue);
								}
							} else {
								// if values are not equal there is no compatible merge and we should return no next
								// element.
								nextResult = null;
								break;
							}
						}
					}
				}
				return nextResult;
			}
		};

		return result;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Projection projection, BindingSet bindings)
			throws QueryEvaluationException {
		CloseableIteration<BindingSet, QueryEvaluationException> result;

		result = this.evaluate(projection.getArg(), bindings);
		result = new ProjectionIterator(projection, result, bindings);
		return result;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(MultiProjection multiProjection,
			BindingSet bindings) throws QueryEvaluationException {
		CloseableIteration<BindingSet, QueryEvaluationException> result;
		result = this.evaluate(multiProjection.getArg(), bindings);
		result = new MultiProjectionIterator(multiProjection, result, bindings);
		return result;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Filter filter, BindingSet bindings)
			throws QueryEvaluationException {
		CloseableIteration<BindingSet, QueryEvaluationException> result;
		result = this.evaluate(filter.getArg(), bindings);
		result = new FilterIterator(filter, result, this);
		return result;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Slice slice, BindingSet bindings)
			throws QueryEvaluationException {
		CloseableIteration<BindingSet, QueryEvaluationException> result = evaluate(slice.getArg(), bindings);

		if (slice.hasOffset()) {
			result = new OffsetIteration<>(result, slice.getOffset());
		}

		if (slice.hasLimit()) {
			result = new LimitIteration<>(result, slice.getLimit());
		}

		return result;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Extension extension, BindingSet bindings)
			throws QueryEvaluationException {
		CloseableIteration<BindingSet, QueryEvaluationException> result;
		try {
			result = this.evaluate(extension.getArg(), bindings);
		} catch (ValueExprEvaluationException e) {
			// a type error in an extension argument should be silently ignored
			// and
			// result in zero bindings.
			result = new EmptyIteration<>();
		}

		result = new ExtensionIterator(extension, result, this);
		return result;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Distinct distinct, BindingSet bindings)
			throws QueryEvaluationException {
		return new DistinctIteration<>(evaluate(distinct.getArg(), bindings));
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Reduced reduced, BindingSet bindings)
			throws QueryEvaluationException {
		return new ReducedIteration<>(evaluate(reduced.getArg(), bindings));
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Group node, BindingSet bindings)
			throws QueryEvaluationException {
		return new GroupIterator(this, node, bindings, iterationCacheSyncThreshold);
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Order node, BindingSet bindings)
			throws QueryEvaluationException {
		ValueComparator vcmp = new ValueComparator();
		OrderComparator cmp = new OrderComparator(this, node, vcmp);
		boolean reduced = isReducedOrDistinct(node);
		long limit = getLimit(node);
		return new OrderIterator(evaluate(node.getArg(), bindings), cmp, limit, reduced, iterationCacheSyncThreshold);
	}

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

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Join join, BindingSet bindings)
			throws QueryEvaluationException {
		// efficient computation of a SERVICE join using vectored evaluation
		// TODO maybe we can create a ServiceJoin node already in the parser?
		if (join.getRightArg() instanceof Service) {
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter = evaluate(join.getLeftArg(), bindings);
			return new ServiceJoinIterator(leftIter, (Service) join.getRightArg(), bindings, this);
		}

		if (isOutOfScopeForLeftArgBindings(join.getRightArg())) {
			return new HashJoinIteration(this, join, bindings);
		} else {
			return new JoinIterator(this, join, bindings);
		}
	}

	private boolean isOutOfScopeForLeftArgBindings(TupleExpr expr) {
		return (TupleExprs.isVariableScopeChange(expr) || TupleExprs.containsSubquery(expr));
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(LeftJoin leftJoin,
			final BindingSet bindings) throws QueryEvaluationException {
		if (TupleExprs.containsSubquery(leftJoin.getRightArg())) {
			return new HashJoinIteration(this, leftJoin, bindings);
		}

		// Check whether optional join is "well designed" as defined in section
		// 4.2 of "Semantics and Complexity of SPARQL", 2006, Jorge Pérez et al.
		VarNameCollector optionalVarCollector = new VarNameCollector();
		leftJoin.getRightArg().visit(optionalVarCollector);
		if (leftJoin.hasCondition()) {
			leftJoin.getCondition().visit(optionalVarCollector);
		}

		Set<String> problemVars = optionalVarCollector.getVarNames();
		problemVars.removeAll(leftJoin.getLeftArg().getBindingNames());
		problemVars.retainAll(bindings.getBindingNames());

		if (problemVars.isEmpty()) {
			// left join is "well designed"
			return new LeftJoinIterator(this, leftJoin, bindings);
		} else {
			return new BadlyDesignedLeftJoinIterator(this, leftJoin, bindings, problemVars);
		}
	}

	@SuppressWarnings("unchecked")
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(final Union union,
			final BindingSet bindings) throws QueryEvaluationException {
		Iteration<BindingSet, QueryEvaluationException> leftArg, rightArg;

		leftArg = new DelayedIteration<BindingSet, QueryEvaluationException>() {

			@Override
			protected Iteration<BindingSet, QueryEvaluationException> createIteration()
					throws QueryEvaluationException {
				return evaluate(union.getLeftArg(), bindings);
			}
		};

		rightArg = new DelayedIteration<BindingSet, QueryEvaluationException>() {

			@Override
			protected Iteration<BindingSet, QueryEvaluationException> createIteration()
					throws QueryEvaluationException {
				return evaluate(union.getRightArg(), bindings);
			}
		};

		return new UnionIteration<>(leftArg, rightArg);
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(final Intersection intersection,
			final BindingSet bindings) throws QueryEvaluationException {
		Iteration<BindingSet, QueryEvaluationException> leftArg, rightArg;

		leftArg = new DelayedIteration<BindingSet, QueryEvaluationException>() {

			@Override
			protected Iteration<BindingSet, QueryEvaluationException> createIteration()
					throws QueryEvaluationException {
				return evaluate(intersection.getLeftArg(), bindings);
			}
		};

		rightArg = new DelayedIteration<BindingSet, QueryEvaluationException>() {

			@Override
			protected Iteration<BindingSet, QueryEvaluationException> createIteration()
					throws QueryEvaluationException {
				return evaluate(intersection.getRightArg(), bindings);
			}
		};

		return new IntersectIteration<>(leftArg, rightArg);
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(final Difference difference,
			final BindingSet bindings) throws QueryEvaluationException {
		Iteration<BindingSet, QueryEvaluationException> leftArg, rightArg;

		leftArg = new DelayedIteration<BindingSet, QueryEvaluationException>() {

			@Override
			protected Iteration<BindingSet, QueryEvaluationException> createIteration()
					throws QueryEvaluationException {
				return evaluate(difference.getLeftArg(), bindings);
			}
		};

		rightArg = new DelayedIteration<BindingSet, QueryEvaluationException>() {

			@Override
			protected Iteration<BindingSet, QueryEvaluationException> createIteration()
					throws QueryEvaluationException {
				return evaluate(difference.getRightArg(), bindings);
			}
		};

		return new SPARQLMinusIteration<>(leftArg, rightArg);
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(SingletonSet singletonSet,
			BindingSet bindings) throws QueryEvaluationException {
		return new SingletonIteration<>(bindings);
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(EmptySet emptySet, BindingSet bindings)
			throws QueryEvaluationException {
		return new EmptyIteration<>();
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(ExternalSet external, BindingSet bindings)
			throws QueryEvaluationException {
		return external.evaluate(bindings);
	}

	@Override
	public Value evaluate(ValueExpr expr, BindingSet bindings)
			throws QueryEvaluationException {
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

	public Value evaluate(ValueConstant valueConstant, BindingSet bindings)
			throws QueryEvaluationException {
		return valueConstant.getValue();
	}

	public Value evaluate(BNodeGenerator node, BindingSet bindings)
			throws QueryEvaluationException {
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

	public Value evaluate(Bound node, BindingSet bindings) throws QueryEvaluationException {
		try {
			Value argValue = evaluate(node.getArg(), bindings);
			return BooleanLiteral.valueOf(argValue != null);
		} catch (ValueExprEvaluationException e) {
			return BooleanLiteral.FALSE;
		}
	}

	public Value evaluate(Str node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);

		if (argValue instanceof IRI) {
			return tripleSource.getValueFactory().createLiteral(argValue.toString());
		} else if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			if (QueryEvaluationUtil.isSimpleLiteral(literal)) {
				return literal;
			} else {
				return tripleSource.getValueFactory().createLiteral(literal.getLabel());
			}
		} else if (argValue instanceof Triple) {
			return tripleSource.getValueFactory().createLiteral(argValue.toString());
		} else {
			throw new ValueExprEvaluationException();
		}
	}

	public Value evaluate(Label node, BindingSet bindings)
			throws QueryEvaluationException {
		// FIXME: deprecate Label in favour of Str(?)
		Value argValue = evaluate(node.getArg(), bindings);

		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			if (QueryEvaluationUtil.isSimpleLiteral(literal)) {
				return literal;
			} else {
				return tripleSource.getValueFactory().createLiteral(literal.getLabel());
			}
		} else {
			throw new ValueExprEvaluationException();
		}
	}

	public Value evaluate(Lang node, BindingSet bindings)
			throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);

		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;
			return tripleSource.getValueFactory().createLiteral(literal.getLanguage().orElse(""));
		}

		throw new ValueExprEvaluationException();
	}

	public Value evaluate(Datatype node, BindingSet bindings)
			throws QueryEvaluationException {
		Value v = evaluate(node.getArg(), bindings);

		if (v instanceof Literal) {
			Literal literal = (Literal) v;

			if (literal.getDatatype() != null) {
				// literal with datatype
				return literal.getDatatype();
			} else if (literal.getLanguage().isPresent()) {
				return RDF.LANGSTRING;
			} else {
				// simple literal
				return XSD.STRING;
			}

		}

		throw new ValueExprEvaluationException();
	}

	public Value evaluate(Namespace node, BindingSet bindings)
			throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);

		if (argValue instanceof IRI) {
			IRI uri = (IRI) argValue;
			return tripleSource.getValueFactory().createIRI(uri.getNamespace());
		} else {
			throw new ValueExprEvaluationException();
		}
	}

	public Value evaluate(LocalName node, BindingSet bindings)
			throws QueryEvaluationException {
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
	public Value evaluate(IsResource node, BindingSet bindings)
			throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		return BooleanLiteral.valueOf(argValue instanceof Resource);
	}

	/**
	 * Determines whether the operand (a variable) contains a URI.
	 *
	 * @return <var>true</var> if the operand contains a URI, <var>false</var> otherwise.
	 */
	public Value evaluate(IsURI node, BindingSet bindings)
			throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		return BooleanLiteral.valueOf(argValue instanceof IRI);
	}

	/**
	 * Determines whether the operand (a variable) contains a BNode.
	 *
	 * @return <var>true</var> if the operand contains a BNode, <var>false</var> otherwise.
	 */
	public Value evaluate(IsBNode node, BindingSet bindings)
			throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		return BooleanLiteral.valueOf(argValue instanceof BNode);
	}

	/**
	 * Determines whether the operand (a variable) contains a Literal.
	 *
	 * @return <var>true</var> if the operand contains a Literal, <var>false</var> otherwise.
	 */
	public Value evaluate(IsLiteral node, BindingSet bindings)
			throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		return BooleanLiteral.valueOf(argValue instanceof Literal);
	}

	/**
	 * Determines whether the operand (a variable) contains a numeric datatyped literal, i.e. a literal with datatype
	 * xsd:float, xsd:double, xsd:decimal, or a derived datatype of xsd:decimal.
	 *
	 * @return <var>true</var> if the operand contains a numeric datatyped literal, <var>false</var> otherwise.
	 */
	public Value evaluate(IsNumeric node, BindingSet bindings)
			throws QueryEvaluationException {
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
	public IRI evaluate(IRIFunction node, BindingSet bindings)
			throws QueryEvaluationException {
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
	public Value evaluate(Regex node, BindingSet bindings)
			throws QueryEvaluationException {
		Value arg = evaluate(node.getArg(), bindings);
		Value parg = evaluate(node.getPatternArg(), bindings);
		Value farg = null;
		ValueExpr flagsArg = node.getFlagsArg();
		if (flagsArg != null) {
			farg = evaluate(flagsArg, bindings);
		}

		if (QueryEvaluationUtil.isStringLiteral(arg) && QueryEvaluationUtil.isSimpleLiteral(parg)
				&& (farg == null || QueryEvaluationUtil.isSimpleLiteral(farg))) {
			String text = ((Literal) arg).getLabel();
			String ptn = ((Literal) parg).getLabel();
			String flags = "";
			if (farg != null) {
				flags = ((Literal) farg).getLabel();
			}
			// TODO should this Pattern be cached?
			int f = 0;
			for (char c : flags.toCharArray()) {
				switch (c) {
				case 's':
					f |= Pattern.DOTALL;
					break;
				case 'm':
					f |= Pattern.MULTILINE;
					break;
				case 'i':
					f |= Pattern.CASE_INSENSITIVE;
					f |= Pattern.UNICODE_CASE;
					break;
				case 'x':
					f |= Pattern.COMMENTS;
					break;
				case 'd':
					f |= Pattern.UNIX_LINES;
					break;
				case 'u':
					f |= Pattern.UNICODE_CASE;
					break;
				case 'q':
					f |= Pattern.LITERAL;
					break;
				default:
					throw new ValueExprEvaluationException(flags);
				}
			}
			Pattern pattern = Pattern.compile(ptn, f);
			boolean result = pattern.matcher(text).find();
			return BooleanLiteral.valueOf(result);
		}

		throw new ValueExprEvaluationException();
	}

	public Value evaluate(LangMatches node, BindingSet bindings)
			throws QueryEvaluationException {
		Value langTagValue = evaluate(node.getLeftArg(), bindings);
		Value langRangeValue = evaluate(node.getRightArg(), bindings);

		if (QueryEvaluationUtil.isSimpleLiteral(langTagValue) && QueryEvaluationUtil.isSimpleLiteral(langRangeValue)) {
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
	public Value evaluate(Like node, BindingSet bindings)
			throws QueryEvaluationException {
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
	public Value evaluate(FunctionCall node, BindingSet bindings)
			throws QueryEvaluationException {
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

	public Value evaluate(And node, BindingSet bindings) throws QueryEvaluationException {
		try {
			Value leftValue = evaluate(node.getLeftArg(), bindings);
			if (!QueryEvaluationUtil.getEffectiveBooleanValue(leftValue)) {
				// Left argument evaluates to false, we don't need to look any
				// further
				return BooleanLiteral.FALSE;
			}
		} catch (ValueExprEvaluationException e) {
			// Failed to evaluate the left argument. Result is 'false' when
			// the right argument evaluates to 'false', failure otherwise.
			Value rightValue = evaluate(node.getRightArg(), bindings);
			if (!QueryEvaluationUtil.getEffectiveBooleanValue(rightValue)) {
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

	public Value evaluate(Not node, BindingSet bindings) throws QueryEvaluationException {
		Value argValue = evaluate(node.getArg(), bindings);
		boolean argBoolean = QueryEvaluationUtil.getEffectiveBooleanValue(argValue);
		return BooleanLiteral.valueOf(!argBoolean);
	}

	public Value evaluate(Now node, BindingSet bindings) throws QueryEvaluationException {
		if (sharedValueOfNow == null) {
			sharedValueOfNow = node.evaluate(tripleSource.getValueFactory());
		}
		return sharedValueOfNow;
	}

	public Value evaluate(SameTerm node, BindingSet bindings)
			throws QueryEvaluationException {
		Value leftVal = evaluate(node.getLeftArg(), bindings);
		Value rightVal = evaluate(node.getRightArg(), bindings);

		return BooleanLiteral.valueOf(leftVal != null && leftVal.equals(rightVal));
	}

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

	public Value evaluate(Compare node, BindingSet bindings)
			throws QueryEvaluationException {
		Value leftVal = evaluate(node.getLeftArg(), bindings);
		Value rightVal = evaluate(node.getRightArg(), bindings);

		return BooleanLiteral.valueOf(QueryEvaluationUtil.compare(leftVal, rightVal, node.getOperator()));
	}

	public Value evaluate(MathExpr node, BindingSet bindings)
			throws QueryEvaluationException {
		// Do the math
		Value leftVal = evaluate(node.getLeftArg(), bindings);
		Value rightVal = evaluate(node.getRightArg(), bindings);

		if (leftVal instanceof Literal && rightVal instanceof Literal) {
			return MathUtil.compute((Literal) leftVal, (Literal) rightVal, node.getOperator());
		}

		throw new ValueExprEvaluationException("Both arguments must be numeric literals");
	}

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

	public Value evaluate(ListMemberOperator node, BindingSet bindings)
			throws QueryEvaluationException {
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

	public Value evaluate(CompareAny node, BindingSet bindings)
			throws QueryEvaluationException {
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

	public Value evaluate(CompareAll node, BindingSet bindings)
			throws QueryEvaluationException {
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

	public Value evaluate(Exists node, BindingSet bindings)
			throws QueryEvaluationException {
		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = evaluate(node.getSubQuery(), bindings)) {
			return BooleanLiteral.valueOf(iter.hasNext());
		}
	}

	@Override
	public boolean isTrue(ValueExpr expr, BindingSet bindings) throws QueryEvaluationException {
		try {
			Value value = evaluate(expr, bindings);
			return QueryEvaluationUtil.getEffectiveBooleanValue(value);
		} catch (ValueExprEvaluationException e) {
			return false;
		}
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

	public Value evaluate(ValueExprTripleRef node, BindingSet bindings)
			throws QueryEvaluationException {
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
		// Naive implementation that walks over all statements matching (x rdf:type rdf:Statement)
		// and filter those that do not match the bindings for subject, predicate and object vars (if bound)
		final org.eclipse.rdf4j.query.algebra.Var subjVar = ref.getSubjectVar();
		final org.eclipse.rdf4j.query.algebra.Var predVar = ref.getPredicateVar();
		final org.eclipse.rdf4j.query.algebra.Var objVar = ref.getObjectVar();
		final org.eclipse.rdf4j.query.algebra.Var extVar = ref.getExprVar();

		final Value subjValue = getVarValue(subjVar, bindings);
		final Value predValue = getVarValue(predVar, bindings);
		final Value objValue = getVarValue(objVar, bindings);
		final Value extValue = getVarValue(extVar, bindings);

		// case1: when we have a binding for extVar we use it in the reified nodes lookup
		// case2: in which we have unbound extVar
		// in both cases:
		// 1. iterate over all statements matching ((* | extValue), rdf:type, rdf:Statement)
		// 2. construct a look ahead iteration and filter these solutions that do not match the
		// bindings for the subject, predicate and object vars (if these are bound)
		// return set of solution where the values of the statements (extVar, rdf:subject/predicate/object, value)
		// are bound to the variables of the respective TripleRef variables for subject, predicate, object
		// NOTE: if the tripleSource is extended to allow for lookup over asserted Triple values in the underlying sail
		// the evaluation of the TripleRef should be suitably forwarded down the sail and filter/construct
		// the correct solution out of the results of that call
		if (extValue != null && !(extValue instanceof Resource)) {
			return new EmptyIteration<>();
		}

		// whether the TripleSouce support access to RDF star
		final boolean sourceSupportsRdfStar = tripleSource instanceof RDFStarTripleSource;

		// in case the
		if (sourceSupportsRdfStar) {
			CloseableIteration<? extends Triple, QueryEvaluationException> sourceIter = ((RDFStarTripleSource) tripleSource)
					.getRdfStarTriples((Resource) subjValue, (IRI) predValue, objValue);

			FilterIteration<Triple, QueryEvaluationException> filterIter = new FilterIteration<Triple, QueryEvaluationException>(
					sourceIter) {
				@Override
				protected boolean accept(Triple triple) throws QueryEvaluationException {
					if (subjValue != null && !subjValue.equals(triple.getSubject())) {
						return false;
					}
					if (predValue != null && !predValue.equals(triple.getPredicate())) {
						return false;
					}
					if (objValue != null && !objValue.equals(triple.getObject())) {
						return false;
					}
					if (extValue != null && !extValue.equals(triple)) {
						return false;
					}
					return true;
				}
			};

			return new ConvertingIteration<Triple, BindingSet, QueryEvaluationException>(filterIter) {
				@Override
				protected BindingSet convert(Triple triple) throws QueryEvaluationException {
					QueryBindingSet result = new QueryBindingSet(bindings);
					if (subjValue == null) {
						result.addBinding(subjVar.getName(), triple.getSubject());
					}
					if (predValue == null) {
						result.addBinding(predVar.getName(), triple.getPredicate());
					}
					if (objValue == null) {
						result.addBinding(objVar.getName(), triple.getObject());
					}
					// add the extVar binding if we do not have a value bound.
					if (extValue == null) {
						result.addBinding(extVar.getName(), triple);
					}
					return result;
				}
			};
		} else {
			// standard reification iteration
			// 1. walk over resources used as subjects of (x rdf:type rdf:Statement)
			final CloseableIteration<? extends Resource, QueryEvaluationException> iter = new ConvertingIteration<Statement, Resource, QueryEvaluationException>(
					tripleSource.getStatements((Resource) extValue, RDF.TYPE, RDF.STATEMENT)) {

				@Override
				protected Resource convert(Statement sourceObject)
						throws QueryEvaluationException {
					return sourceObject.getSubject();
				}
			};
			// for each reification node, fetch and check the subject, predicate and object values against
			// the expected values from TripleRef pattern and supplied bindings collection
			return new LookAheadIteration<BindingSet, QueryEvaluationException>() {
				@Override
				protected void handleClose()
						throws QueryEvaluationException {
					super.handleClose();
					iter.close();
				}

				@Override
				protected BindingSet getNextElement()
						throws QueryEvaluationException {
					while (iter.hasNext()) {
						Resource theNode = iter.next();
						QueryBindingSet result = new QueryBindingSet(bindings);
						// does it match the subjectValue/subjVar
						if (!matchValue(theNode, subjValue, subjVar, result, RDF.SUBJECT)) {
							continue;
						}
						// the predicate, if not, remove the binding that hass been added
						// when the subjValue has been checked and its value added to the solution
						if (!matchValue(theNode, predValue, predVar, result, RDF.PREDICATE)) {
							continue;
						}
						// check the object, if it do not match
						// remove the bindings added for subj and pred
						if (!matchValue(theNode, objValue, objVar, result, RDF.OBJECT)) {
							continue;
						}
						// add the extVar binding if we do not have a value bound.
						if (extValue == null) {
							result.addBinding(extVar.getName(), theNode);
						} else if (!extValue.equals(theNode)) {
							// the extVar value do not match theNode
							continue;
						}
						return result;
					}
					return null;
				}

				private boolean matchValue(Resource theNode, Value value, Var var, QueryBindingSet result,
						IRI predicate) {
					try (CloseableIteration<? extends Statement, QueryEvaluationException> valueiter = tripleSource
							.getStatements(theNode, predicate, null)) {
						while (valueiter.hasNext()) {
							Statement valueStatement = valueiter.next();
							if (theNode.equals(valueStatement.getSubject())) {
								if (value == null || value.equals(valueStatement.getObject())) {
									if (value == null) {
										result.addBinding(var.getName(), valueStatement.getObject());
									}
									return true;
								}
							}
						}
						return false;
					}
				}

			};
		} // else standard reification iteration
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
	public void setTrackTime(boolean trackTime) {
		this.trackTime = trackTime;
	}
}
