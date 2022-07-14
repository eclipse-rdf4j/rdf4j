/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.endpoint.provider;

import java.util.Optional;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.PrecompiledQueryNode;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.BindingAssignerOptimizer;

/**
 * An {@link EvaluationStrategyFactory} which allows the evaluation of {@link PrecompiledQueryNode} without prior
 * optimization.
 * <p>
 * All other types of {@link TupleExpr} are optimized and evaluated through the configured delegate strategy, i.e.
 * typically the one provided by the sail itself.
 * </p>
 *
 * @author Andreas Schwarte
 * @see NativeStoreProvider
 */
/* package */ class SailSourceEvaluationStrategyFactory implements EvaluationStrategyFactory {

	private final EvaluationStrategyFactory delegate;

	public SailSourceEvaluationStrategyFactory(EvaluationStrategyFactory delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public void setQuerySolutionCacheThreshold(long threshold) {
		delegate.setQuerySolutionCacheThreshold(threshold);
	}

	@Override
	public long getQuerySolutionCacheThreshold() {
		return delegate.getQuerySolutionCacheThreshold();
	}

	@Override
	public void setOptimizerPipeline(QueryOptimizerPipeline pipeline) {
		delegate.setOptimizerPipeline(pipeline);
	}

	@Override
	public Optional<QueryOptimizerPipeline> getOptimizerPipeline() {
		return delegate.getOptimizerPipeline();
	}

	@Override
	public EvaluationStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics) {
		EvaluationStrategy delegateStrategy = delegate.createEvaluationStrategy(dataset, tripleSource,
				evaluationStatistics);
		return new SailSourceEvaluationStrategy(delegateStrategy, dataset);
	}

	/**
	 * {@link EvaluationStrategy} that can handle {@link PrecompiledQueryNode} without prior optimization. All other
	 * {@link TupleExpr} are handled in the respective delegate.
	 *
	 * @author Andreas Schwarte
	 *
	 */
	private static class SailSourceEvaluationStrategy implements EvaluationStrategy {

		private final EvaluationStrategy delegate;
		private final Dataset dataset;

		public SailSourceEvaluationStrategy(EvaluationStrategy delegate, Dataset dataset) {
			super();
			this.delegate = delegate;
			this.dataset = dataset;
		}

		@Override
		public FederatedService getService(String serviceUrl) throws QueryEvaluationException {
			return delegate.getService(serviceUrl);
		}

		@Override
		public void setOptimizerPipeline(QueryOptimizerPipeline pipeline) {
			delegate.setOptimizerPipeline(pipeline);
		}

		@Override
		public TupleExpr optimize(TupleExpr expr, EvaluationStatistics evaluationStatistics, BindingSet bindings) {

			if (expr instanceof PrecompiledQueryNode) {
				return optimizePreparedQuery((PrecompiledQueryNode) expr, bindings);
			}
			return delegate.optimize(expr, evaluationStatistics, bindings);
		}

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service expr, String serviceUri,
				CloseableIteration<BindingSet, QueryEvaluationException> bindings) throws QueryEvaluationException {
			return delegate.evaluate(expr, serviceUri, bindings);
		}

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleExpr expr, BindingSet bindings)
				throws QueryEvaluationException {
			return delegate.evaluate(expr, bindings);
		}

		@Override
		public Value evaluate(ValueExpr expr, BindingSet bindings)
				throws ValueExprEvaluationException, QueryEvaluationException {
			return delegate.evaluate(expr, bindings);
		}

		@Override
		public boolean isTrue(ValueExpr expr, BindingSet bindings)
				throws ValueExprEvaluationException, QueryEvaluationException {
			return delegate.isTrue(expr, bindings);
		}

		@Override
		public boolean isTrue(QueryValueEvaluationStep expr, BindingSet bindings)
				throws ValueExprEvaluationException, QueryEvaluationException {
			return delegate.isTrue(expr, bindings);
		}

		protected TupleExpr optimizePreparedQuery(PrecompiledQueryNode preparedQuery, BindingSet bindings) {

			TupleExpr actualQuery = preparedQuery.getQuery();

			if (bindings != null) {
				new BindingAssignerOptimizer().optimize(actualQuery, dataset, bindings);
			}

			return actualQuery;
		}
	}
}
