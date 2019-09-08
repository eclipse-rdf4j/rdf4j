/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
