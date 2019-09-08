/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package com.fluidops.fedx.evaluation;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FederationManager.FederationType;

/**
 * Factory class for retrieving the {@link FederationEvalStrategy} to be used
 * 
 * @author Andreas Schwarte
 */
public class EvaluationStrategyFactory {

	
	/**
	 * Return an instance of {@link FederationEvalStrategy} which is used for evaluating 
	 * the query. The type depends on the {@link FederationType} as well as on the 
	 * actual implementations given by the configuration, in particular this is
	 * {@link Config#getSailEvaluationStrategy()} and {@link Config#getSPARQLEvaluationStrategy()}.
	 * 
	 * @param federationType
	 * @return the {@link FederationEvalStrategy}
	 */
	public static FederationEvalStrategy getEvaluationStrategy(FederationType federationType) {
		
		switch (federationType) {
		case LOCAL:		return instantiate(Config.getConfig().getSailEvaluationStrategy());
		case REMOTE:
		case HYBRID:
		default:		return instantiate(Config.getConfig().getSPARQLEvaluationStrategy());
		}
	}
	
	private static FederationEvalStrategy instantiate(String evalStrategyClass) {
		try {
			return (FederationEvalStrategy) Class.forName(evalStrategyClass).getDeclaredConstructor().newInstance();
		} catch (InstantiationException e) {
			throw new IllegalStateException("Class " + evalStrategyClass + " could not be instantiated.", e);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Class " + evalStrategyClass + " could not be found, check whether the name is correct.", e);
		} catch (Exception e) {
			throw new IllegalStateException("Unexpected error while instantiating " + evalStrategyClass + ":", e);
		}
	}
}
