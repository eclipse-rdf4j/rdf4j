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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values.ScopedQueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.BadlyDesignedLeftJoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.HashJoinIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.LeftJoinIterator;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

public final class LeftJoinQueryEvaluationStep implements QueryEvaluationStep {
	private final QueryEvaluationStep right;
	private final QueryValueEvaluationStep condition;
	private final QueryEvaluationStep left;
	private final LeftJoin leftJoin;
	private final Set<String> optionalVars;
	private final QueryEvaluationStep wellDesignedRightEvaluationStep;

	public static QueryEvaluationStep supply(EvaluationStrategy strategy, LeftJoin leftJoin,
			QueryEvaluationContext context) {
		boolean runtimeTelemetryTrackingActive = strategy.isTrackResultSize() || strategy.isTrackTime();
		QueryEvaluationStep left = JoinMetricsTracking
				.wrapLeftInput(strategy.precompile(leftJoin.getLeftArg(), context), leftJoin, leftJoin.getLeftArg(),
						runtimeTelemetryTrackingActive);
		QueryEvaluationStep right = JoinMetricsTracking
				.wrapRightInput(strategy.precompile(leftJoin.getRightArg(), context), leftJoin, leftJoin.getRightArg(),
						runtimeTelemetryTrackingActive);
		if (TupleExprs.containsSubquery(leftJoin.getRightArg())) {
			Set<String> leftBindingNames = leftJoin.getLeftArg().getBindingNames();
			Set<String> rightBindingNames = leftJoin.getRightArg().getBindingNames();
			String[] joinAttributes = leftBindingNames.stream()
					.filter(rightBindingNames::contains)
					.toArray(String[]::new);
			return bs -> new HashJoinIteration(left, right, bs, true, joinAttributes, context);
		}

		// Check whether optional join is "well designed" as defined in section
		// 4.2 of "Semantics and Complexity of SPARQL", 2006, Jorge Pérez et al.
		VarNameCollector optionalVarCollector = new VarNameCollector();
		leftJoin.getRightArg().visit(optionalVarCollector);
		QueryValueEvaluationStep condition;
		if (leftJoin.hasCondition()) {
			leftJoin.getCondition().visit(optionalVarCollector);
			condition = strategy.precompile(leftJoin.getCondition(), context);
		} else {
			condition = null;
		}
		return new LeftJoinQueryEvaluationStep(right, condition, left, leftJoin, optionalVarCollector.getVarNames());
	}

	public LeftJoinQueryEvaluationStep(QueryEvaluationStep right, QueryValueEvaluationStep condition,
			QueryEvaluationStep left, LeftJoin leftJoin, Set<String> optionalVars) {
		this.right = right;
		this.condition = condition;
		this.left = left;
		this.leftJoin = leftJoin;
		// This is used to determine if the left join is well designed.

		Set<String> leftBindingNames = leftJoin.getLeftArg().getBindingNames();

		if (!leftBindingNames.isEmpty()) {
			if (leftBindingNames.containsAll(optionalVars)) {
				optionalVars = Set.of();
			} else {
				for (String leftBindingName : leftBindingNames) {
					if (optionalVars.contains(leftBindingName)) {
						optionalVars = new HashSet<>(optionalVars);
						optionalVars.removeAll(leftBindingNames);
						break;
					}
				}
			}
		}

		this.optionalVars = optionalVars;
		this.wellDesignedRightEvaluationStep = determineRightEvaluationStep(
				leftJoin,
				right,
				condition,
				leftJoin.getBindingNames());
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {

		boolean containsNone = true;
		Set<String> bindingNames = bindings.getBindingNames();
		for (String optionalVar : optionalVars) {
			if (bindingNames.contains(optionalVar)) {
				containsNone = false;
				break;
			}
		}

		if (containsNone) {
			// left join is "well designed"
			leftJoin.setAlgorithm(LeftJoinIterator.class.getSimpleName());
			return LeftJoinIterator.getInstance(left, bindings, wellDesignedRightEvaluationStep);
		} else {
			Set<String> problemVars = new HashSet<>(optionalVars);
			problemVars.retainAll(bindings.getBindingNames());

			leftJoin.setAlgorithm(BadlyDesignedLeftJoinIterator.class.getSimpleName());
			var rightEvaluationStep = determineRightEvaluationStep(leftJoin, right, condition, problemVars);
			return new BadlyDesignedLeftJoinIterator(left, bindings, problemVars, rightEvaluationStep);
		}
	}

	/**
	 * This function determines the way the right-hand side is evaluated. There are 3 options:
	 * <p>
	 * 1. No join condition: <br>
	 * The right-hand side should just be joined with the left-hand side. No filtering is applied.
	 * </p>
	 * <p>
	 * 2. The join condition can be fully evaluated by the left-hand side:
	 *
	 * <pre>
	 * SELECT * WHERE {
	 * 	?dist a dcat:Distribution .
	 *  ?dist dc:license ?license .
	 *
	 *  OPTIONAL {
	 *     	?a dcat:distribution ?dist.
	 *
	 *      FILTER(?license = <http://wiki.data.gouv.fr/wiki/Licence_Ouverte_/_Open_Licence>)
	 * 	}
	 * }
	 * </pre>
	 *
	 * In this case, pre-filtering can be applied. The right-hand side does not have to evaluated when the join
	 * condition evaluates to false.
	 * </p>
	 * <p>
	 * 3. The join condition needs right-hand side evaluation:
	 *
	 * <pre>
	 * SELECT * WHERE {
	 *  ?dist a dcat:Distribution .
	 *
	 * 	OPTIONAL {
	 *		?a dcat:distribution ?dist .
	 * 		?a dct:language $lang .
	 *
	 * 		FILTER(?lang = eu-lang:ENG)
	 *     	}
	 * }
	 * </pre>
	 *
	 * In this case, the join condition can only be evaluated after the right-hand side is evaluated (post-filtering).
	 * </p>
	 */
	public static QueryEvaluationStep determineRightEvaluationStep(
			LeftJoin join,
			QueryEvaluationStep prepareRightArg,
			QueryValueEvaluationStep joinCondition,
			Set<String> scopeBindingNames) {
		if (joinCondition == null) {
			return prepareRightArg;
		} else if (canEvaluateConditionBasedOnLeftHandSide(join)) {
			return new PreFilterQueryEvaluationStep(
					prepareRightArg,
					new ScopedQueryValueEvaluationStep(join.getAssuredBindingNames(), joinCondition),
					join);
		} else {
			return new PostFilterQueryEvaluationStep(
					prepareRightArg,
					new ScopedQueryValueEvaluationStep(scopeBindingNames, joinCondition),
					join);
		}
	}

	private static boolean canEvaluateConditionBasedOnLeftHandSide(LeftJoin leftJoin) {
		var varNames = VarNameCollector.process(leftJoin.getCondition());
		return leftJoin.getAssuredBindingNames().containsAll(varNames);
	}
}
