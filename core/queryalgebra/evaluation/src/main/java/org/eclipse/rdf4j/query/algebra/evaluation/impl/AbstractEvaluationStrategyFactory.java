/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.Optional;

import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;

/**
 * Abstract base class for {@link ExtendedEvaluationStrategy}.
 *
 * @author Jeen Broekstra
 */
public abstract class AbstractEvaluationStrategyFactory implements EvaluationStrategyFactory {

	private long querySolutionCacheThreshold;

	// track the results size that each node in the query plan produces during execution
	private boolean trackResultSize;

	private QueryOptimizerPipeline pipeline;

	@Override
	public void setQuerySolutionCacheThreshold(long threshold) {
		this.querySolutionCacheThreshold = threshold;
	}

	@Override
	public long getQuerySolutionCacheThreshold() {
		return querySolutionCacheThreshold;
	}

	@Override
	public void setOptimizerPipeline(QueryOptimizerPipeline pipeline) {
		this.pipeline = pipeline;
	}

	@Override
	public Optional<QueryOptimizerPipeline> getOptimizerPipeline() {
		return Optional.ofNullable(pipeline);
	}

	@Override
	public boolean isTrackResultSize() {
		return trackResultSize;
	}

	@Override
	public void setTrackResultSize(boolean trackResultSize) {
		this.trackResultSize = trackResultSize;
	}
}
