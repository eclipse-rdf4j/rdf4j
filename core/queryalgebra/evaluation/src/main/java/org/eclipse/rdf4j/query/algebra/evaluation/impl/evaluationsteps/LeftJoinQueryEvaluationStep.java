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
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.BadlyDesignedLeftJoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.LeftJoinIterator;
import org.eclipse.rdf4j.query.algebra.helpers.VarNameCollector;

public final class LeftJoinQueryEvaluationStep implements QueryEvaluationStep {
	private final QueryEvaluationStep right;
	private final QueryValueEvaluationStep condition;
	private final QueryEvaluationStep left;
	private final LeftJoin leftJoin;
	private final VarNameCollector optionalVarCollector;

	public LeftJoinQueryEvaluationStep(QueryEvaluationStep right, QueryValueEvaluationStep condition,
			QueryEvaluationStep left, LeftJoin leftJoin, VarNameCollector optionalVarCollector) {
		this.right = right;
		this.condition = condition;
		this.left = left;
		this.leftJoin = leftJoin;
		this.optionalVarCollector = optionalVarCollector;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		Set<String> problemVars = new HashSet<>(optionalVarCollector.getVarNames());
		problemVars.removeAll(leftJoin.getLeftArg().getBindingNames());
		problemVars.retainAll(bindings.getBindingNames());

		if (problemVars.isEmpty()) {
			// left join is "well designed"
			leftJoin.setAlgorithm(LeftJoinIterator.class.getSimpleName());
			return new LeftJoinIterator(left, right, condition, bindings, problemVars);
		} else {
			leftJoin.setAlgorithm(BadlyDesignedLeftJoinIterator.class.getSimpleName());
			return new BadlyDesignedLeftJoinIterator(left, right, condition, bindings, problemVars);
		}
	}
}