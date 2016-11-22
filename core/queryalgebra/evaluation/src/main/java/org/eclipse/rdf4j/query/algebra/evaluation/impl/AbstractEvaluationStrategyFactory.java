/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;

/**
 * Abstract base class for {@link ExtendedEvaluationStrategy}.
 * 
 * @author Jeen Broekstra
 */
public abstract class AbstractEvaluationStrategyFactory implements EvaluationStrategyFactory {

	private long querySolutionCacheThreshold;

	@Override
	public void setQuerySolutionCacheThreshold(long threshold) {
		this.querySolutionCacheThreshold = threshold;
	}

	@Override
	public long getQuerySolutionCacheThreshold() {
		return querySolutionCacheThreshold;
	}

}
