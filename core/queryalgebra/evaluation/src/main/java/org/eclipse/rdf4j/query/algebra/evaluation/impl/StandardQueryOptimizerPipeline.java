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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;

/**
 *
 * A standard pipeline with the default {@link QueryOptimizer}s that will be used by {@link StrictEvaluationStrategy}
 * and its subclasses, unless specifically overridden.
 *
 * @author Jeen Broekstra
 *
 * @see EvaluationStrategyFactory#setOptimizerPipeline(QueryOptimizerPipeline)
 * 
 * @deprecated since 4.1.0. Use
 *             {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.StandardQueryOptimizerPipeline} instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class StandardQueryOptimizerPipeline
		extends org.eclipse.rdf4j.query.algebra.evaluation.optimizer.StandardQueryOptimizerPipeline
		implements QueryOptimizerPipeline {

	public StandardQueryOptimizerPipeline(EvaluationStrategy strategy, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics) {
		super(strategy, tripleSource, evaluationStatistics);
	}

}
