/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.query.Dataset;

/**
 * Factory for {@link EvaluationStrategy}s.
 */
public interface EvaluationStrategyFactory {

	/**
	 * Set the number of query solutions the {@link EvaluationStrategy} will keep in main memory before it attempts to
	 * sync to a temporary disk cache. If set to 0, no disk caching will occur. EvaluationStrategies that provide no
	 * disk caching functionality are free to ignore this parameter.
	 * 
	 * @param threshold the number of query solutions that the EvaluationStrategy can cache in main memory before
	 *                  attempting disk sync.
	 */
	void setQuerySolutionCacheThreshold(long threshold);

	/**
	 * Get the number of query solutions the {@link EvaluationStrategy} will keep in main memory before it attempts to
	 * sync to a temporary disk cache. If set to 0, no disk caching will occur. EvaluationStrategies that provide no
	 * disk caching functionality are free to ignore this parameter.
	 */
	long getQuerySolutionCacheThreshold();

	/**
	 * Returns the {@link EvaluationStrategy} to use to evaluate queries for the given {@link Dataset} and
	 * {@link TripleSource}.
	 * 
	 * @param dataset      the DataSet to evaluate queries against.
	 * @param tripleSource the TripleSource to evaluate queries against.
	 * @return an EvaluationStrategy.
	 */
	EvaluationStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource);

}
