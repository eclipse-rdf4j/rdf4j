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

import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.FederationManager.FederationType;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;

/**
 * Factory class for retrieving the {@link FederationEvalStrategy} to be used.
 *
 * <p>
 * Default strategies:
 * </p>
 *
 * <ul>
 * <li>local federation: {@link SailFederationEvalStrategy}</li>
 * <li>endpoint federation: {@link SparqlFederationEvalStrategy}</li>
 * <li>hybrid federation: {@link SparqlFederationEvalStrategy}</li>
 * </ul>
 *
 * <p>
 * Customized strategies can be supplied to the federation using
 * {@link FedXFactory#withFederationEvaluationStrategyFactory(FederationEvaluationStrategyFactory)}
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

		switch (federationType) {
		case LOCAL:
			return new SailFederationEvalStrategy(federationContext);
		case REMOTE:
		case HYBRID:
		default:
			return new SparqlFederationEvalStrategy(federationContext);
		}
	}
}
