/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerFunctionalInterface;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;

public class StrictEvaluationStrategyFactory extends AbstractEvaluationStrategyFactory
		implements EvaluationStrategyFactory, FederatedServiceResolverClient {

	private FederatedServiceResolver serviceResolver;

	public StrictEvaluationStrategyFactory() {
	}

	public StrictEvaluationStrategyFactory(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
	}

	public FederatedServiceResolver getFederatedServiceResolver() {
		return serviceResolver;
	}

	@Override
	public EvaluationStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics) {

		StrictEvaluationStrategy strategy = new StrictEvaluationStrategy(tripleSource, dataset, serviceResolver,
				getQuerySolutionCacheThreshold(), evaluationStatistics, isTrackResultSize());

		getOptimizerPipeline().ifPresent(strategy::setOptimizerPipeline);

		if (!getQueryOptimizersPre().isEmpty() || !getQueryOptimizersPost().isEmpty()) {

			Iterable<QueryOptimizer> optimizers = strategy.getOptimizerPipeline().getOptimizers();
			List<QueryOptimizer> queryOptimizersPre = getQueryOptimizersPre().stream()
					.map(i -> i.getOptimizer(strategy, tripleSource, evaluationStatistics))
					.collect(Collectors.toList());
			List<QueryOptimizer> queryOptimizersPost = getQueryOptimizersPost().stream()
					.map(i -> i.getOptimizer(strategy, tripleSource, evaluationStatistics))
					.collect(Collectors.toList());

			strategy.setOptimizerPipeline(() -> () -> new Iterator<>() {

				final Iterator<QueryOptimizer> preIterator = queryOptimizersPre.iterator();
				final Iterator<QueryOptimizer> iterator = optimizers.iterator();
				final Iterator<QueryOptimizer> postIterator = queryOptimizersPost.iterator();

				@Override
				public boolean hasNext() {
					return preIterator.hasNext() || iterator.hasNext() || postIterator.hasNext();
				}

				@Override
				public QueryOptimizer next() {
					if (preIterator.hasNext()) {
						return preIterator.next();
					}
					if (iterator.hasNext()) {
						return iterator.next();
					}
					if (postIterator.hasNext()) {
						return postIterator.next();
					}
					throw new NoSuchElementException();
				}
			});

		}

		return strategy;

	}

}
