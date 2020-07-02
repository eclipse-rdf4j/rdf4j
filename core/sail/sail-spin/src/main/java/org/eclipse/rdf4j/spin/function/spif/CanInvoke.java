/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function.spif;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.model.vocabulary.SPL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.AbstractQueryPreparer;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.AbstractFederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.BindingAssigner;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.CompareOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ConjunctiveConstraintSplitter;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ConstantOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DisjunctiveConstraintOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.FilterOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.IterativeEvaluationOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.OrderLimitOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryJoinOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryModelNormalizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SameTermFilterOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.TupleFunctionEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.util.TripleSources;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.spin.SpinFunctionInterpreter;
import org.eclipse.rdf4j.sail.spin.SpinInferencing;
import org.eclipse.rdf4j.sail.spin.SpinMagicPropertyInterpreter;
import org.eclipse.rdf4j.spin.Argument;
import org.eclipse.rdf4j.spin.ConstraintViolation;
import org.eclipse.rdf4j.spin.SpinParser;
import org.eclipse.rdf4j.spin.function.AbstractSpinFunction;

public class CanInvoke extends AbstractSpinFunction implements Function {

	private SpinParser parser;

	public CanInvoke() {
		super(SPIF.CAN_INVOKE_FUNCTION.stringValue());
	}

	public CanInvoke(SpinParser parser) {
		this();
		this.parser = parser;
	}

	public SpinParser getSpinParser() {
		return parser;
	}

	public void setSpinParser(SpinParser parser) {
		this.parser = parser;
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		QueryPreparer qp = getCurrentQueryPreparer();
		final TripleSource qpTripleSource = qp.getTripleSource();
		if (args.length == 0) {
			throw new ValueExprEvaluationException("At least one argument is required");
		}
		if (!(args[0] instanceof IRI)) {
			throw new ValueExprEvaluationException("The first argument must be a function IRI");
		}

		IRI func = (IRI) args[0];

		try {
			Function instanceOfFunc = getFunction("http://spinrdf.org/spl#instanceOf", qpTripleSource,
					FunctionRegistry.getInstance());
			Map<IRI, Argument> funcArgs = parser.parseArguments(func, qpTripleSource);
			List<IRI> funcArgList = SpinParser.orderArguments(funcArgs.keySet());
			final Map<IRI, Value> argValues = new HashMap<>(funcArgList.size() * 3);
			for (int i = 0; i < funcArgList.size(); i++) {
				IRI argName = funcArgList.get(i);
				Argument funcArg = funcArgs.get(argName);
				int argIndex = i + 1;
				if (argIndex < args.length) {
					Value argValue = args[argIndex];
					IRI valueType = funcArg.getValueType();
					Value isInstance = instanceOfFunc.evaluate(valueFactory, argValue, valueType);
					if (((Literal) isInstance).booleanValue()) {
						argValues.put(argName, argValue);
					} else {
						return BooleanLiteral.FALSE;
					}
				} else if (!funcArg.isOptional()) {
					return BooleanLiteral.FALSE;
				}
			}

			/*
			 * in order to check any additional constraints we have to create a virtual function instance to run them
			 * against
			 */
			final Resource funcInstance = qpTripleSource.getValueFactory().createBNode();

			TripleSource tempTripleSource = new TripleSource() {

				private final ValueFactory vf = qpTripleSource.getValueFactory();

				@Override
				public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
						IRI pred, Value obj, Resource... contexts) throws QueryEvaluationException {
					if (funcInstance.equals(subj)) {
						if (pred != null) {
							Value v = argValues.get(pred);
							if (v != null && (obj == null || v.equals(obj))) {
								return new SingletonIteration<>(vf.createStatement(subj, pred, v));
							}
						}

						return new EmptyIteration<>();
					} else {
						return qpTripleSource.getStatements(subj, pred, obj, contexts);
					}
				}

				@Override
				public ValueFactory getValueFactory() {
					return vf;
				}
			};

			QueryPreparer tempQueryPreparer = new AbstractQueryPreparer(tempTripleSource) {

				private final FunctionRegistry functionRegistry = FunctionRegistry.getInstance();

				private final TupleFunctionRegistry tupleFunctionRegistry = TupleFunctionRegistry.getInstance();

				private final AbstractFederatedServiceResolver serviceResolver = new SPARQLServiceResolver();

				@Override
				protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(
						TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred,
						int maxExecutionTime) throws QueryEvaluationException {
					// Clone the tuple expression to allow for more aggressive
					// optimizations
					tupleExpr = tupleExpr.clone();

					if (!(tupleExpr instanceof QueryRoot)) {
						// Add a dummy root node to the tuple expressions to allow the
						// optimizers to modify the actual root node
						tupleExpr = new QueryRoot(tupleExpr);
					}

					new SpinFunctionInterpreter(parser, getTripleSource(), functionRegistry).optimize(tupleExpr,
							dataset, bindings);
					new SpinMagicPropertyInterpreter(parser, getTripleSource(), tupleFunctionRegistry, serviceResolver)
							.optimize(tupleExpr, dataset, bindings);

					EvaluationStrategy strategy = new TupleFunctionEvaluationStrategy(getTripleSource(), dataset,
							serviceResolver, tupleFunctionRegistry);

					// do standard optimizations
					new BindingAssigner().optimize(tupleExpr, dataset, bindings);
					new ConstantOptimizer(strategy).optimize(tupleExpr, dataset, bindings);
					new CompareOptimizer().optimize(tupleExpr, dataset, bindings);
					new ConjunctiveConstraintSplitter().optimize(tupleExpr, dataset, bindings);
					new DisjunctiveConstraintOptimizer().optimize(tupleExpr, dataset, bindings);
					new SameTermFilterOptimizer().optimize(tupleExpr, dataset, bindings);
					new QueryModelNormalizer().optimize(tupleExpr, dataset, bindings);
					new QueryJoinOptimizer(new EvaluationStatistics()).optimize(tupleExpr, dataset, bindings);
					// new SubSelectJoinOptimizer().optimize(tupleExpr, dataset,
					// bindings);
					new IterativeEvaluationOptimizer().optimize(tupleExpr, dataset, bindings);
					new FilterOptimizer().optimize(tupleExpr, dataset, bindings);
					new OrderLimitOptimizer().optimize(tupleExpr, dataset, bindings);

					return strategy.evaluate(tupleExpr, bindings);
				}

				@Override
				protected void execute(UpdateExpr updateExpr, Dataset dataset, BindingSet bindings,
						boolean includeInferred, int maxExecutionTime) throws UpdateExecutionException {
					throw new UnsupportedOperationException();
				}
			};

			try (CloseableIteration<Resource, QueryEvaluationException> iter = TripleSources
					.getObjectResources(func, SPIN.CONSTRAINT_PROPERTY, qpTripleSource)) {
				while (iter.hasNext()) {
					Resource constraint = iter.next();
					Set<IRI> constraintTypes = Iterations
							.asSet(TripleSources.getObjectURIs(constraint, RDF.TYPE, qpTripleSource));
					// skip over argument constraints that we have already checked
					if (!constraintTypes.contains(SPL.ARGUMENT_TEMPLATE)) {
						ConstraintViolation violation = SpinInferencing.checkConstraint(funcInstance, constraint,
								tempQueryPreparer, parser);
						if (violation != null) {
							return BooleanLiteral.FALSE;
						}
					}
				}
			}
		} catch (RDF4JException e) {
			throw new ValueExprEvaluationException(e);
		}

		return BooleanLiteral.TRUE;
	}

	private Function getFunction(String name, TripleSource tripleSource, FunctionRegistry functionRegistry)
			throws RDF4JException {
		Function func = functionRegistry.get(name).orElse(null);
		if (func == null) {
			IRI funcUri = tripleSource.getValueFactory().createIRI(name);
			func = parser.parseFunction(funcUri, tripleSource);
			functionRegistry.add(func);
		}
		return func;
	}
}
