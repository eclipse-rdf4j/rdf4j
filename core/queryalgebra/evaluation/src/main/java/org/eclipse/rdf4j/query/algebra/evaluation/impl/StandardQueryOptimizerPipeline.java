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
public class StandardQueryOptimizerPipeline implements QueryOptimizerPipeline {

	private static final BindingAssigner BINDING_ASSIGNER = new BindingAssigner();
	private static final BindingSetAssignmentInliner BINDING_SET_ASSIGNMENT_INLINER = new BindingSetAssignmentInliner();
	private static final CompareOptimizer COMPARE_OPTIMIZER = new CompareOptimizer();
	private static final ConjunctiveConstraintSplitter CONJUNCTIVE_CONSTRAINT_SPLITTER = new ConjunctiveConstraintSplitter();
	private static final DisjunctiveConstraintOptimizer DISJUNCTIVE_CONSTRAINT_OPTIMIZER = new DisjunctiveConstraintOptimizer();
	private static final SameTermFilterOptimizer SAME_TERM_FILTER_OPTIMIZER = new SameTermFilterOptimizer();
	private static final UnionScopeChangeOptimizer UNION_SCOPE_CHANGE_OPTIMIZER = new UnionScopeChangeOptimizer();
	private static final QueryModelNormalizer QUERY_MODEL_NORMALIZER = new QueryModelNormalizer();
	private static final IterativeEvaluationOptimizer ITERATIVE_EVALUATION_OPTIMIZER = new IterativeEvaluationOptimizer();
	private static final FilterOptimizer FILTER_OPTIMIZER = new FilterOptimizer();
	private static final OrderLimitOptimizer ORDER_LIMIT_OPTIMIZER = new OrderLimitOptimizer();
	private static final ParentReferenceCleaner PARENT_REFERENCE_CLEANER = new ParentReferenceCleaner();

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
				BINDING_ASSIGNER,
				BINDING_SET_ASSIGNMENT_INLINER,
				new ConstantOptimizer(strategy),
				new RegexAsStringFunctionOptimizer(tripleSource.getValueFactory()),
				COMPARE_OPTIMIZER,
				CONJUNCTIVE_CONSTRAINT_SPLITTER,
				DISJUNCTIVE_CONSTRAINT_OPTIMIZER,
				SAME_TERM_FILTER_OPTIMIZER,
				UNION_SCOPE_CHANGE_OPTIMIZER,
				QUERY_MODEL_NORMALIZER,
				new ProjectionRemovalOptimizer(), // Make sure this is after the UnionScopeChangeOptimizer
				new QueryJoinOptimizer(evaluationStatistics),
				ITERATIVE_EVALUATION_OPTIMIZER,
				FILTER_OPTIMIZER,
				ORDER_LIMIT_OPTIMIZER,
				new VarOptimizer(),
				PARENT_REFERENCE_CLEANER);
	}

}
