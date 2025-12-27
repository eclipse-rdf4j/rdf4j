/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;

/**
 * A standard pipeline with the default {@link QueryOptimizer}s that will be used by {@link StrictEvaluationStrategy}
 * and its subclasses, unless specifically overridden.
 *
 * @author Jeen Broekstra
 * @see EvaluationStrategyFactory#setOptimizerPipeline(QueryOptimizerPipeline)
 */
public class StandardQueryOptimizerPipeline implements QueryOptimizerPipeline {

	private static boolean assertsEnabled = false;

	static {
		// noinspection AssertWithSideEffects
		assert assertsEnabled = true;
	}

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
		List<QueryOptimizer> optimizers = new ArrayList<>();
		optimizers.add(BINDING_ASSIGNER);
		optimizers.add(BINDING_SET_ASSIGNMENT_INLINER);
		optimizers.add(new ConstantOptimizer(strategy));
		optimizers.add(new RegexAsStringFunctionOptimizer(tripleSource.getValueFactory()));
		optimizers.add(COMPARE_OPTIMIZER);
		optimizers.add(CONJUNCTIVE_CONSTRAINT_SPLITTER);
		optimizers.add(DISJUNCTIVE_CONSTRAINT_OPTIMIZER);
		optimizers.add(SAME_TERM_FILTER_OPTIMIZER);
		optimizers.add(UNION_SCOPE_CHANGE_OPTIMIZER);
		optimizers.add(QUERY_MODEL_NORMALIZER);
		if (isUnionOptionalEnabled() && isUnionFlattenEnabled()) {
			optimizers.add(new UnionFlatteningOptimizer());
		}
		if (isUnionOptionalEnabled() && isUnionReorderEnabled()) {
			optimizers.add(new UnionReorderOptimizer(evaluationStatistics));
		}
		optimizers.add(PROJECTION_REMOVAL_OPTIMIZER); // Make sure this is after the UnionScopeChangeOptimizer
		optimizers.add(new QueryJoinOptimizer(evaluationStatistics, strategy.isTrackResultSize(), tripleSource));
		optimizers.add(ITERATIVE_EVALUATION_OPTIMIZER);
		optimizers.add(FILTER_OPTIMIZER);
		optimizers.add(ORDER_LIMIT_OPTIMIZER);

		if (assertsEnabled) {
			List<QueryOptimizer> optimizersWithReferenceCleaner = new ArrayList<>();
			optimizersWithReferenceCleaner.add(new ParentReferenceChecker(null));
			for (QueryOptimizer optimizer : optimizers) {
				optimizersWithReferenceCleaner.add(optimizer);
				optimizersWithReferenceCleaner.add(new ParentReferenceChecker(optimizer));
			}
			optimizers = optimizersWithReferenceCleaner;
		}

		return optimizers;
	}

	private static boolean isUnionOptionalEnabled() {
		return Boolean.parseBoolean(System.getProperty("rdf4j.optimizer.unionOptional.enabled", "false"));
	}

	private static boolean isUnionFlattenEnabled() {
		return Boolean.parseBoolean(System.getProperty("rdf4j.optimizer.unionOptional.flatten.enabled", "false"));
	}

	private static boolean isUnionReorderEnabled() {
		return Boolean.parseBoolean(System.getProperty("rdf4j.optimizer.unionOptional.unionReorder.enabled", "false"));
	}

}
