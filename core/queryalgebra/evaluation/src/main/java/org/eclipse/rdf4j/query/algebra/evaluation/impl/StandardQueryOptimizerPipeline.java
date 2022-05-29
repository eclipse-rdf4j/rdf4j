/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.Arrays;

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
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class StandardQueryOptimizerPipeline implements QueryOptimizerPipeline {

	private final EvaluationStatistics evaluationStatistics;
	private final TripleSource tripleSource;
	private final EvaluationStrategy strategy;

	public StandardQueryOptimizerPipeline(EvaluationStrategy strategy, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics) {
		this.strategy = strategy;
		this.tripleSource = tripleSource;
		this.evaluationStatistics = evaluationStatistics;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline#getOptimizers()
	 */
	@Override
	public Iterable<QueryOptimizer> getOptimizers() {
		return Arrays.asList(
				new BindingAssigner(),
				new BindingSetAssignmentInliner(),
				new ConstantOptimizer(strategy),
				new RegexAsStringFunctionOptimizer(tripleSource.getValueFactory()),
				new CompareOptimizer(),
				new ConjunctiveConstraintSplitter(),
				new DisjunctiveConstraintOptimizer(),
				new SameTermFilterOptimizer(),
				new UnionScopeChangeOptimizer(),
				new QueryModelNormalizer(),
				new ProjectionRemovalOptimizer(), // Make sure this is after the UnionScopeChangeOptimizer
				new QueryJoinOptimizer(evaluationStatistics),
				new IterativeEvaluationOptimizer(),
				new FilterOptimizer(),
				new OrderLimitOptimizer(),
				new ParentReferenceCleaner());
	}

}
