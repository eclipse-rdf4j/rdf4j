/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.evaluation.*;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class LeftJoinIterator extends LookAheadIteration<BindingSet> {
	/*-----------*
	 * Variables *
	 *-----------*/

	private final CloseableIteration<BindingSet> leftIter;
	private final QueryEvaluationStep rightEvaluationStep;

	private CloseableIteration<BindingSet> rightIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LeftJoinIterator(
			EvaluationStrategy strategy,
			LeftJoin join,
			BindingSet bindings,
			QueryEvaluationContext context) throws QueryEvaluationException {
		Set<String> scopeBindingNames = join.getBindingNames();

		leftIter = strategy.evaluate(join.getLeftArg(), bindings);

		rightIter = null;

		QueryEvaluationStep prepareRightArg = strategy.precompile(join.getRightArg(), context);
		join.setAlgorithm(this);
		var joinCondition = Optional.ofNullable(join.getCondition())
				.map(condition -> strategy.precompile(condition, context));

		rightEvaluationStep = determineRightEvaluationStep(
				join,
				prepareRightArg,
				joinCondition.orElse(null),
				scopeBindingNames);
	}

	public LeftJoinIterator(
			QueryEvaluationStep left,
			QueryEvaluationStep right,
			QueryValueEvaluationStep joinCondition,
			BindingSet bindings,
			Set<String> scopeBindingNames,
			LeftJoin leftJoin) throws QueryEvaluationException {
		this(
				left.evaluate(bindings),
				determineRightEvaluationStep(leftJoin, right, joinCondition, scopeBindingNames));
	}

	public LeftJoinIterator(CloseableIteration<BindingSet> leftIter, QueryEvaluationStep rightEvaluationStep) {
		this.leftIter = leftIter;
		this.rightIter = null;
		this.rightEvaluationStep = rightEvaluationStep;
	}

	public static CloseableIteration<BindingSet> getInstance(
			QueryEvaluationStep left,
			BindingSet bindings,
			QueryEvaluationStep rightEvaluationStep) {

		CloseableIteration<BindingSet> leftIter = left.evaluate(bindings);

		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		} else {
			return new LeftJoinIterator(leftIter, rightEvaluationStep);
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {

		try {
			CloseableIteration<BindingSet> nextRightIter = rightIter;
			while (nextRightIter == null || nextRightIter.hasNext() || leftIter.hasNext()) {
				BindingSet leftBindings = null;

				if (nextRightIter == null) {
					if (leftIter.hasNext()) {
						// Use left arg's bindings in case join fails
						leftBindings = leftIter.next();
						nextRightIter = rightIter = rightEvaluationStep.evaluate(leftBindings);
					} else {
						return null;
					}
				} else if (!nextRightIter.hasNext()) {
					// Use left arg's bindings in case join fails
					leftBindings = leftIter.next();

					nextRightIter.close();
					nextRightIter = rightIter = rightEvaluationStep.evaluate(leftBindings);
				}

				if (nextRightIter == QueryEvaluationStep.EMPTY_ITERATION) {
					rightIter = null;
					return leftBindings;
				}

				if (nextRightIter.hasNext()) {
					return nextRightIter.next();
				}

				if (leftBindings != null) {
					rightIter = null;
					// Join failed, return left arg's bindings
					return leftBindings;
				}
			}
		} catch (NoSuchElementException ignore) {
			// probably, one of the iterations has been closed concurrently in
			// handleClose()
		}

		return null;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			leftIter.close();
		} finally {
			if (rightIter != null) {
				rightIter.close();
			}
		}
	}

	public static QueryEvaluationStep determineRightEvaluationStep(
			LeftJoin join,
			QueryEvaluationStep prepareRightArg,
			QueryValueEvaluationStep joinCondition,
			Set<String> scopeBindingNames) {
		if (joinCondition == null) {
			return prepareRightArg;
		} else if (canEvaluateConditionBasedOnLeftHandSide(join)) {
			return new LeftJoinPreFilterQueryEvaluationStep(
					prepareRightArg,
					new ScopeBindingsJoinConditionEvaluator(join.getAssuredBindingNames(), joinCondition));
		} else {
			return new LeftJoinPostFilterQueryEvaluationStep(
					prepareRightArg,
					new ScopeBindingsJoinConditionEvaluator(scopeBindingNames, joinCondition));
		}
	}

	private static boolean canEvaluateConditionBasedOnLeftHandSide(LeftJoin leftJoin) {
		if (!leftJoin.hasCondition()) {
			return false;
		}

		var varNames = VarNameCollector.process(leftJoin.getCondition());
		return leftJoin.getAssuredBindingNames().containsAll(varNames);
	}
}
