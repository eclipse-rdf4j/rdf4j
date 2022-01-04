/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.FederationManager.FederationType;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;

/**
 * Factory class for retrieving the {@link FederationEvalStrategy} to be used
 *
 * @author Andreas Schwarte
 */
public class FederationEvaluationStrategyFactory extends StrictEvaluationStrategyFactory {

	private FederationType federationType;
	private FederationContext federationContext;

	public FederationType getFederationType() {
		return federationType;
	}

	public void setFederationType(FederationType federationType) {
		this.federationType = federationType;
	}

	public FederationContext getFederationContext() {
		return federationContext;
	}

	public void setFederationContext(FederationContext federationContext) {
		this.federationContext = federationContext;
	}

	/**
	 * Create the {@link FederationEvalStrategy} to be used.
	 * 
	 * Note: all parameters may be <code>null</code>
	 */
	@Override
	public FederationEvalStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics) {

		// Note: currently dataset, triplesource and statistics are explicitly ignored in the federation
		return getEvaluationStrategy(federationType, federationContext);
	}

	/**
	 * Return an instance of {@link FederationEvalStrategy} which is used for evaluating the query. The type depends on
	 * the {@link FederationType} as well as on the actual implementations given by the configuration, in particular
	 * this is {@link FedXConfig#getSailEvaluationStrategy()} and {@link FedXConfig#getSPARQLEvaluationStrategy()}.
	 *
	 * @param federationType
	 * @param federationContext
	 * @return the {@link FederationEvalStrategy}
	 */
	private static FederationEvalStrategy getEvaluationStrategy(FederationType federationType,
			FederationContext federationContext) {

		switch (federationType) {
		case LOCAL:
			return instantiate(federationContext.getConfig().getSailEvaluationStrategy(), federationContext);
		case REMOTE:
		case HYBRID:
		default:
			return instantiate(federationContext.getConfig().getSPARQLEvaluationStrategy(), federationContext);
		}
	}

	private static FederationEvalStrategy instantiate(Class<? extends FederationEvalStrategy> evalStrategyClass,
			FederationContext federationContext) {
		try {
			return (FederationEvalStrategy) evalStrategyClass
					.getDeclaredConstructor(FederationContext.class)
					.newInstance(federationContext);
		} catch (InstantiationException e) {
			throw new IllegalStateException("Class " + evalStrategyClass + " could not be instantiated.", e);
		} catch (Exception e) {
			throw new IllegalStateException("Unexpected error while instantiating " + evalStrategyClass + ":", e);
		}
	}
}
