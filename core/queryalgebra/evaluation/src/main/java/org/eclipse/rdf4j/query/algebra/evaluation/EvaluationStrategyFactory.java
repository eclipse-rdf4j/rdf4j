/**
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.query.Dataset;

/**
 * Factory for {@link EvaluationStrategy}s.
 */
public interface EvaluationStrategyFactory {

	/**
	 * Returns the {@link EvaluationStrategy} to use to evaluate queries for the given {@link Dataset} and
	 * {@link TripleSource}.
	 * 
	 * @param dataset
	 *        the DataSet to evaluate queries against.
	 * @param tripleSource
	 *        the TripleSource to evaluate queries against.
	 * @return an EvaluationStrategy.
	 */
	EvaluationStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource);

	/**
	 * Returns the {@link EvaluationStrategy} to use to evaluate queries for the given {@link Dataset} and
	 * {@link TripleSource}.
	 * 
	 * @param dataset
	 *        the DataSet to evaluate queries against.
	 * @param tripleSource
	 *        the TripleSource to evaluate queries against.
	 * @param iterationCacheSyncThreshold
	 *        the number of query solutions the {@link EvaluationStrategy} can keep in main memory before it
	 *        should sync to a temporary disk cache. If set to 0, no disk caching occurs.
	 *        {@link EvaluationStrategy} implementations that provide no disk caching are free to ignore this
	 *        parameter.
	 * @return an EvaluationStrategy.
	 */
	EvaluationStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource,
			long iterationCacheSyncThreshold);
}
