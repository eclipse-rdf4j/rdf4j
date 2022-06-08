/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
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

	public static QueryEvaluationStep supply(EvaluationStrategy strategy, LeftJoin leftJoin,
			QueryEvaluationContext context) {
		QueryEvaluationStep left = strategy.precompile(leftJoin.getLeftArg(), context);
		QueryEvaluationStep right = strategy.precompile(leftJoin.getRightArg(), context);
		if (TupleExprs.containsSubquery(leftJoin.getRightArg())) {
			Set<String> leftBindingNames = leftJoin.getLeftArg().getBindingNames();
			Set<String> rightBindingNames = leftJoin.getRightArg().getBindingNames();
			Set<String> joinAttributeNames = new HashSet<>(leftBindingNames);
			joinAttributeNames.retainAll(rightBindingNames);
			String[] joinAttributes = joinAttributeNames.toArray(new String[0]);
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

	@Deprecated(forRemoval = true, since = "4.1.0")
	public LeftJoinQueryEvaluationStep(QueryEvaluationStep right, QueryValueEvaluationStep condition,
			QueryEvaluationStep left, LeftJoin leftJoin,
			org.eclipse.rdf4j.query.algebra.helpers.VarNameCollector optionalVarCollector) {
		this.right = right;
		this.condition = condition;
		this.left = left;
		this.leftJoin = leftJoin;
		// This is used to determine if the left join is well designed.

		Set<String> leftBindingNames = leftJoin.getLeftArg().getBindingNames();
		Set<String> optionalVars = optionalVarCollector.getVarNames();

		boolean optionalVarsContainLeftBindingName = false;
		for (String leftBindingName : leftBindingNames) {
			if (!optionalVarsContainLeftBindingName && optionalVars.contains(leftBindingName)) {
				optionalVars = new HashSet<>(optionalVars);
				optionalVarsContainLeftBindingName = true;
			}
			if (optionalVarsContainLeftBindingName) {
				optionalVars.remove(leftBindingName);
			}
		}

		this.optionalVars = optionalVars;

	}

	public LeftJoinQueryEvaluationStep(QueryEvaluationStep right, QueryValueEvaluationStep condition,
			QueryEvaluationStep left, LeftJoin leftJoin, Set<String> optionalVars) {
		this.right = right;
		this.condition = condition;
		this.left = left;
		this.leftJoin = leftJoin;
		// This is used to determine if the left join is well designed.

		Set<String> leftBindingNames = leftJoin.getLeftArg().getBindingNames();

		boolean optionalVarsContainLeftBindingName = false;
		for (String leftBindingName : leftBindingNames) {
			if (!optionalVarsContainLeftBindingName && optionalVars.contains(leftBindingName)) {
				optionalVars = new HashSet<>(optionalVars);
				optionalVarsContainLeftBindingName = true;
			}
			if (optionalVarsContainLeftBindingName) {
				optionalVars.remove(leftBindingName);
			}
		}

		this.optionalVars = optionalVars;

	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {

		Set<String> problemVars = new HashSet<>(optionalVars);
		problemVars.retainAll(bindings.getBindingNames());
		if (problemVars.isEmpty()) {
			// left join is "well designed"
			leftJoin.setAlgorithm(LeftJoinIterator.class.getSimpleName());
			return new LeftJoinIterator(left, right, condition, bindings, leftJoin.getBindingNames());
		} else {
			leftJoin.setAlgorithm(BadlyDesignedLeftJoinIterator.class.getSimpleName());
			return new BadlyDesignedLeftJoinIterator(left, right, condition, bindings, problemVars);
		}
	}
}
