/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.ServiceJoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.HashJoinIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.InnerMergeJoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.JoinIterator;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;

public class JoinQueryEvaluationStep implements QueryEvaluationStep {

	private final java.util.function.Function<BindingSet, CloseableIteration<BindingSet>> eval;

	public JoinQueryEvaluationStep(EvaluationStrategy strategy, Join join, QueryEvaluationContext context) {
		// efficient computation of a SERVICE join using vectored evaluation
		// TODO maybe we can create a ServiceJoin node already in the parser?
		boolean runtimeTelemetryTrackingActive = strategy.isTrackResultSize() || strategy.isTrackTime();
		QueryEvaluationStep leftPrepared = JoinMetricsTracking
				.wrapLeftInput(strategy.precompile(join.getLeftArg(), context), join, join.getLeftArg(),
						runtimeTelemetryTrackingActive);
		QueryEvaluationStep rightPrepared = JoinMetricsTracking
				.wrapRightInput(strategy.precompile(join.getRightArg(), context), join, join.getRightArg(),
						runtimeTelemetryTrackingActive);
		if (join.getRightArg() instanceof Service) {
			eval = bindings -> new ServiceJoinIterator(leftPrepared.evaluate(bindings),
					(Service) join.getRightArg(), bindings,
					strategy);
			join.setAlgorithm(ServiceJoinIterator.class.getSimpleName());
		} else if (isOutOfScopeForLeftArgBindings(join.getRightArg())) {
			String[] joinAttributes = HashJoinIteration.hashJoinAttributeNames(join);
			eval = bindings -> new HashJoinIteration(leftPrepared, rightPrepared, bindings, false,
					joinAttributes, context);
			join.setAlgorithm(HashJoinIteration.class.getSimpleName());
		} else if (join.isMergeJoin() && context.getComparator() != null) {
			eval = bindings -> InnerMergeJoinIterator.getInstance(leftPrepared, rightPrepared, bindings,
					context.getComparator(), context.getValue(join.getOrder().getName()), context);
			join.setAlgorithm(InnerMergeJoinIterator.class.getSimpleName());
		} else {
			eval = bindings -> JoinIterator.getInstance(leftPrepared, rightPrepared, bindings);
			join.setAlgorithm(JoinIterator.class.getSimpleName());
		}
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		return eval.apply(bindings);
	}

	private static boolean isOutOfScopeForLeftArgBindings(TupleExpr expr) {
		return TupleExprs.isVariableScopeChange(expr) || TupleExprs.containsSubquery(expr);
	}

}
