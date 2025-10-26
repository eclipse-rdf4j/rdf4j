/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.util.function.Supplier;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.impl.DefaultCollectionFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;

/**
 * Evaluation strategy factory that installs LMDB-specific join behaviour.
 */
class LmdbEvaluationStrategyFactory extends StrictEvaluationStrategyFactory {

	private FederatedServiceResolver serviceResolver;
	private Supplier<CollectionFactory> collectionFactorySupplier = DefaultCollectionFactory::new;

	LmdbEvaluationStrategyFactory(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
	}

	@Override
	public FederatedServiceResolver getFederatedServiceResolver() {
		return serviceResolver;
	}

	@Override
	public void setCollectionFactory(Supplier<CollectionFactory> collectionFactory) {
		this.collectionFactorySupplier = collectionFactory;
	}

	@Override
	public EvaluationStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics) {
		LmdbEvaluationStrategy strategy = new LmdbEvaluationStrategy(tripleSource, dataset, serviceResolver,
				getQuerySolutionCacheThreshold(), evaluationStatistics, isTrackResultSize());
		getOptimizerPipeline().ifPresent(strategy::setOptimizerPipeline);
		strategy.setCollectionFactory(collectionFactorySupplier);
		return strategy;
	}
}
