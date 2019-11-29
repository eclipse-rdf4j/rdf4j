/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import org.eclipse.rdf4j.federated.Config;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.FederationManager.FederationType;

/**
 * Factory class for retrieving the {@link FederationEvalStrategy} to be used
 * 
 * @author Andreas Schwarte
 */
public class FederationEvaluationStrategyFactory {

	/**
	 * Return an instance of {@link FederationEvalStrategy} which is used for evaluating the query. The type depends on
	 * the {@link FederationType} as well as on the actual implementations given by the configuration, in particular
	 * this is {@link Config#getSailEvaluationStrategy()} and {@link Config#getSPARQLEvaluationStrategy()}.
	 * 
	 * @param federationType
	 * @param federationContext
	 * @return the {@link FederationEvalStrategy}
	 */
	public static FederationEvalStrategy getEvaluationStrategy(FederationType federationType,
			FederationContext federationContext) {

		switch (federationType) {
		case LOCAL:
			return instantiate(Config.getConfig().getSailEvaluationStrategy(), federationContext);
		case REMOTE:
		case HYBRID:
		default:
			return instantiate(Config.getConfig().getSPARQLEvaluationStrategy(), federationContext);
		}
	}

	private static FederationEvalStrategy instantiate(String evalStrategyClass, FederationContext federationContext) {
		try {
			return (FederationEvalStrategy) Class.forName(evalStrategyClass)
					.getDeclaredConstructor(FederationContext.class)
					.newInstance(federationContext);
		} catch (InstantiationException e) {
			throw new IllegalStateException("Class " + evalStrategyClass + " could not be instantiated.", e);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(
					"Class " + evalStrategyClass + " could not be found, check whether the name is correct.", e);
		} catch (Exception e) {
			throw new IllegalStateException("Unexpected error while instantiating " + evalStrategyClass + ":", e);
		}
	}
}
