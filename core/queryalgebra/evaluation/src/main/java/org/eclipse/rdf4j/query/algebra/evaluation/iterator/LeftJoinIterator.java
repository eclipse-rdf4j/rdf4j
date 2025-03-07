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

import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

public class LeftJoinIterator extends LookAheadIteration<BindingSet> {
	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The set of binding names that are "in scope" for the filter. The filter must not include bindings that are (only)
	 * included because of the depth-first evaluation strategy in the evaluation of the constraint.
	 */
	private final Set<String> scopeBindingNames;

	private final CloseableIteration<BindingSet> leftIter;

	private CloseableIteration<BindingSet> rightIter;

	private final QueryEvaluationStep prepareRightArg;

	private final QueryValueEvaluationStep joinCondition;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LeftJoinIterator(EvaluationStrategy strategy, LeftJoin join, BindingSet bindings,
			QueryEvaluationContext context)
			throws QueryEvaluationException {
		this.scopeBindingNames = join.getBindingNames();

		leftIter = strategy.evaluate(join.getLeftArg(), bindings);

		rightIter = null;

		prepareRightArg = strategy.precompile(join.getRightArg(), context);
		join.setAlgorithm(this);
		final ValueExpr condition = join.getCondition();
		if (condition == null) {
			joinCondition = null;
		} else {
			joinCondition = strategy.precompile(condition, context);
		}
	}

	public LeftJoinIterator(QueryEvaluationStep left, QueryEvaluationStep right, QueryValueEvaluationStep joinCondition,
			BindingSet bindings, Set<String> scopeBindingNamse)
			throws QueryEvaluationException {
		this.scopeBindingNames = scopeBindingNamse;

		leftIter = left.evaluate(bindings);

		// Initialize with empty iteration so that var is never null
		rightIter = null;

		prepareRightArg = right;
		this.joinCondition = joinCondition;

	}

	public LeftJoinIterator(CloseableIteration<BindingSet> leftIter, QueryEvaluationStep prepareRightArg,
			QueryValueEvaluationStep joinCondition, Set<String> scopeBindingNamse) {
		this.scopeBindingNames = scopeBindingNamse;
		this.leftIter = leftIter;
		this.rightIter = null;
		this.prepareRightArg = prepareRightArg;
		this.joinCondition = joinCondition;
	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep left,
			QueryEvaluationStep prepareRightArg, QueryValueEvaluationStep joinCondition, BindingSet bindings,
			Set<String> scopeBindingNamse) {

		CloseableIteration<BindingSet> leftIter = left.evaluate(bindings);

		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		} else {
			return new LeftJoinIterator(leftIter, prepareRightArg, joinCondition, scopeBindingNamse);
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
						nextRightIter = rightIter = prepareRightArg.evaluate(leftBindings);
					} else {
						return null;
					}

				} else if (!nextRightIter.hasNext()) {
					// Use left arg's bindings in case join fails
					leftBindings = leftIter.next();

					nextRightIter.close();
					nextRightIter = rightIter = prepareRightArg.evaluate(leftBindings);
				}

				if (nextRightIter == QueryEvaluationStep.EMPTY_ITERATION) {
					rightIter = null;
					return leftBindings;
				}

				while (nextRightIter.hasNext()) {
					BindingSet rightBindings = nextRightIter.next();

					try {
						if (joinCondition == null) {
							return rightBindings;
						} else {
							// Limit the bindings to the ones that are in scope for
							// this filter

							QueryBindingSet scopeBindings = new QueryBindingSet(scopeBindingNames.size());
							for (String scopeBindingName : scopeBindingNames) {
								Binding binding = rightBindings.getBinding(scopeBindingName);
								if (binding != null) {
									scopeBindings.addBinding(binding);
								}
							}

							if (isTrue(joinCondition, scopeBindings)) {
								return rightBindings;
							}
						}
					} catch (ValueExprEvaluationException e) {
						// Ignore, condition not evaluated successfully
					}
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

	private boolean isTrue(QueryValueEvaluationStep expr, QueryBindingSet bindings) {
		Value value = expr.evaluate(bindings);
		return QueryEvaluationUtility.getEffectiveBooleanValue(value).orElse(false);
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
}
