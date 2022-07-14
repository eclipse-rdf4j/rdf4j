/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;

public class StrictEvaluationStrategyFactory extends AbstractEvaluationStrategyFactory
		implements FederatedServiceResolverClient {

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

		return strategy;
	}

}
