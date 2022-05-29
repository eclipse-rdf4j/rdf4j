/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.List;

import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;

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

	public static final BindingAssignerOptimizer BINDING_ASSIGNER = new BindingAssignerOptimizer();
	public static final BindingSetAssignmentInlinerOptimizer BINDING_SET_ASSIGNMENT_INLINER = new BindingSetAssignmentInlinerOptimizer();
	public static final CompareOptimizer COMPARE_OPTIMIZER = new CompareOptimizer();
	public static final ConjunctiveConstraintSplitterOptimizer CONJUNCTIVE_CONSTRAINT_SPLITTER = new ConjunctiveConstraintSplitterOptimizer();
	public static final DisjunctiveConstraintOptimizer DISJUNCTIVE_CONSTRAINT_OPTIMIZER = new DisjunctiveConstraintOptimizer();
	public static final SameTermFilterOptimizer SAME_TERM_FILTER_OPTIMIZER = new SameTermFilterOptimizer();
	public static final UnionScopeChangeOptimizer UNION_SCOPE_CHANGE_OPTIMIZER = new UnionScopeChangeOptimizer();
	public static final QueryModelNormalizerOptimizer QUERY_MODEL_NORMALIZER = new QueryModelNormalizerOptimizer();
	public static final ProjectionRemovalOptimizer PROJECTION_REMOVAL_OPTIMIZER = new ProjectionRemovalOptimizer();
	public static final IterativeEvaluationOptimizer ITERATIVE_EVALUATION_OPTIMIZER = new IterativeEvaluationOptimizer();
	public static final FilterOptimizer FILTER_OPTIMIZER = new FilterOptimizer();
	public static final OrderLimitOptimizer ORDER_LIMIT_OPTIMIZER = new OrderLimitOptimizer();
	public static final ParentReferenceCleaner PARENT_REFERENCE_CLEANER = new ParentReferenceCleaner();
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
		return List.of(
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
				PROJECTION_REMOVAL_OPTIMIZER, // Make sure this is after the UnionScopeChangeOptimizer
				new QueryJoinOptimizer(evaluationStatistics, strategy.isTrackResultSize()),
				ITERATIVE_EVALUATION_OPTIMIZER,
				FILTER_OPTIMIZER,
				ORDER_LIMIT_OPTIMIZER,
				PARENT_REFERENCE_CLEANER
		);
	}

}
